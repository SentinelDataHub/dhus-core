/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019 GAEL Systems
 *
 * This file is part of DHuS software sources.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.dhus.metrics.embed;

import static org.dhus.olingo.v2.web.DHuSODataServlet.CONTAINER_NAME;
import static org.dhus.olingo.v2.web.DHuSODataServlet.NAMESPACE;
import static org.testng.Assert.*;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import fr.gael.odata.engine.model.EdmProvider;

import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.SystemQueryOptionKind;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.core.uri.parser.Parser;
import org.apache.olingo.server.core.uri.parser.UriParserException;
import org.apache.olingo.server.core.uri.validator.UriValidationException;

import org.dhus.olingo.v2.datamodel.MetricModel;
import org.dhus.olingo.v2.datamodel.enumeration.MetricTypeEnum;
import org.dhus.olingo.v2.visitor.MetricSQLVisitor;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * MetricsService integration tests.
 *
 * Tests the MetricsService, the Metric database object, the MetricsDAO, and the MetricSQLVisitor
 */
@ContextConfiguration(locations = { "classpath:fr/gael/dhus/spring/context-config-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MetricsServiceIT extends AbstractTestNGSpringContextTests
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final String TIMER = "foobar.timer";
   private static final String COUNTER = "foobar.counter";
   private static final String METER = "foobar.meter";
   private static final String HISTOGRAM = "foobar.histogram";
   private static final String GAUGE = "foobar.gauge";
   private static final String PATH = "/Metrics";
   private static final String BASE_URI = "http://localhost/odata/v2";

   private MetricsService svc;
   private Parser parser;
   private LocalDateTime reportTime;

   @BeforeClass
   public void setup()
   {
      Jdbi jdbi = Jdbi.create("jdbc:hsqldb:mem:.");
      jdbi.installPlugin(new SqlObjectPlugin());
      svc = new MetricsService(jdbi, 1, "SECONDS");

      // Create URI parser to test the MetricSQLVisitor and the MetricsService
      EdmProvider.Builder builder = new EdmProvider.Builder(NAMESPACE, CONTAINER_NAME);
      builder.addModels(new MetricModel())
             .addEnums(new MetricTypeEnum());
      OData odata = OData.newInstance();
      Edm edm = odata.createServiceMetadata(builder.build(), Collections.emptyList()).getEdm();
      parser = new Parser(edm, odata);
   }

   /* Test invalid parameters */

   @Test(groups = "invalid")
   public void testInvalidParameters()
   {
      assertThrows(NullPointerException.class, () -> svc.getMetricAsEntity("", null));
      assertThrows(NullPointerException.class, () -> svc.getMetricAsEntity(null, new Timestamp(0l)));
      assertThrows(NullPointerException.class, () -> svc.getMetricAsEntity(null, null));

      assertThrows(NullPointerException.class, () -> svc.listMetricsAsEntities(null));
   }

   /* Test service with zero metrics reported. */

   @Test(groups = "zero")
   public void testZeroMetrics()
   {
      assertEquals(svc.countMetrics(), 0);

      assertNull(svc.getMetricAsEntity(TIMER, new Timestamp(System.currentTimeMillis())));

      List<?> entities = svc.listMetricsAsEntities();
      assertNotNull(entities);
      assertEquals(entities.size(), 0);

      entities = svc.listMetricsAsEntities(new MetricSQLVisitor(null, null, null, null));
      assertNotNull(entities);
      assertEquals(entities.size(), 0);
   }

   /* Test service with some metrics reported. */

   @BeforeGroups(value = "nonzero", dependsOnGroups = "zero")
   public void storeMetrics()
   {
      // DO NOT CHANGE these metrics or their values, used by testFilterMethods()
      MetricRegistry reg = new MetricRegistry();
      try (Timer.Context ctx = reg.timer(TIMER).time())
      {
         reg.counter(COUNTER).inc(10);
         reg.meter(METER).mark(100);
         reg.histogram(HISTOGRAM).update(1);
         reg.histogram(HISTOGRAM).update(4); // mean=2.5, stddev=1.5
         reg.register(GAUGE, (Gauge<String>) () -> "  VALUE");
      }

      svc.report(reg.getGauges(), reg.getCounters(), reg.getHistograms(), reg.getMeters(), reg.getTimers());

      // Store the timestamp of the report() we just performed
      Timestamp timestamp = Timestamp.class.cast(svc.listMetricsAsEntities().get(0).getProperty(MetricModel.TIMESTAMP).getValue());
      reportTime = timestamp.toLocalDateTime();
   }

   @Test(groups = "nonzero", dependsOnGroups = "zero")
   public void testMetrics()
   {
      assertEquals(svc.countMetrics(), 5);

      List<Entity> entities = svc.listMetricsAsEntities();
      assertNotNull(entities);
      assertEquals(entities.size(), 5);

      MetricSQLVisitor visitor;

      // $top=1
      visitor = new MetricSQLVisitor(null, null, (lambdaTopOption)() -> 1, null);
      entities = svc.listMetricsAsEntities(visitor);
      assertNotNull(entities);
      assertEquals(entities.size(), 1);
      Entity first = entities.get(0);
      assertNotNull(first);

      // $skip=1
      visitor = new MetricSQLVisitor(null, null, null, (lambdaSkipOption)() -> 1);
      entities = svc.listMetricsAsEntities(visitor);
      assertNotNull(entities);
      assertEquals(entities.size(), 4);
      Entity second = entities.get(0);
      assertNotEquals(
            first.getProperty(MetricModel.NAME).getValue(),
            second.getProperty(MetricModel.NAME).getValue());

      // Access entity using a primary key
      String value = String.class.cast(first.getProperty(MetricModel.NAME).getValue());
      Timestamp timestamp = Timestamp.class.cast(first.getProperty(MetricModel.TIMESTAMP).getValue());
      assertNotNull(svc.getMetricAsEntity(value, timestamp));
   }

   @DataProvider(name = "typeProvider")
   public String[][] typeProvider()
   {
      return new String[][] {
         { MetricTypes.TIMER.name, TIMER },
         { MetricTypes.COUNTER.name, COUNTER },
         { MetricTypes.METER.name, METER },
         { MetricTypes.HISTOGRAM.name, HISTOGRAM },
         { MetricTypes.GAUGE.name, GAUGE }
      };
   }

   private MetricSQLVisitor mkVisitor(String filter) throws UriParserException, UriValidationException
   {
      UriInfo uriInfo = parser.parseUri(PATH, filter, null, BASE_URI);
      return new MetricSQLVisitor(uriInfo.getFilterOption(), null, null, null);
   }

   private void checkHasEntityNamed(String filter, String entityName) throws UriParserException, UriValidationException
   {
      List<Entity> entities = svc.listMetricsAsEntities(mkVisitor(filter));
      assertNotNull(entities);
      assertEquals(entities.size(), 1);
      Entity entity = entities.get(0);
      assertNotNull(entity);
      assertEquals(entity.getProperty(MetricModel.NAME).getValue(), entityName);
   }

   @Test(groups = "nonzero", dependsOnGroups = "zero", dataProvider = "typeProvider")
   public void testFilteringByType(String type, String name) throws UriParserException, UriValidationException
   {
      LOGGER.info("testFilteringByType: $filter=Type eq OData.DHuS.MetricType.{} -> {}", type, name);
      checkHasEntityNamed("$filter=Type eq OData.DHuS.MetricType'" + type + "'", name);
   }

   @DataProvider(name = "filterMethodProvider")
   public String[][] filterMethodProvider()
   {
      return new String[][] {
         { "endswith(Name, 'counter')", COUNTER },
         { "startswith(Name, 'foobar.ti')", TIMER },
         { "indexof(Name, 'meter') gt 0", METER },
         { "contains(Name, 'histo')", HISTOGRAM },
         { "substring(Name, 8) eq 'gauge'", GAUGE },
         { "substring(Name, 8, 3) eq 'gau'", GAUGE },
         { "length(Name) eq 16", HISTOGRAM },
         { "concat(Name, '.test') eq 'foobar.meter.test'", METER },
         { "toupper(Name) eq 'FOOBAR.TIMER'", TIMER },
         { "tolower(Gauge) eq '  value'", GAUGE },
         { "trim(Gauge) eq 'VALUE'", GAUGE }
      };
   }

   @Test(groups = "nonzero", dependsOnGroups = "zero", dataProvider = "filterMethodProvider")
   public void testFilterMethods(String filter, String name) throws UriParserException, UriValidationException
   {
      LOGGER.info("testFilterMethods: $filter={} -> {}", filter, name);
      checkHasEntityNamed("$filter=" + filter, name);
   }

   @DataProvider(name = "dateMethodProvider")
   @SuppressWarnings("unchecked")
   public Function<LocalDateTime, String[]>[] dateMethodProvider()
   {
      // Using functions as we need the reporting timestamp of the metrics to build our filters

      // Cannot create a parameterised array, using a cast seems to be the only way here...
      Function<LocalDateTime, String[]>[] res;
      res = (Function<LocalDateTime, String[]>[]) Array.newInstance(Function.class, 13);

      res[0] = (LocalDateTime ldt) -> {
         int year = ldt.getYear();
         return new String[] { "year(Timestamp) eq " + year , "year(Timestamp) lt " + year };
      };
      res[1] = (LocalDateTime ldt) -> {
         int month = ldt.getMonthValue();
         return new String[] { "month(Timestamp) eq " + month , "month(Timestamp) ne " + month };
      };
      res[2] = (LocalDateTime ldt) -> {
         int day = ldt.getDayOfMonth();
         return new String[] { "day(Timestamp) eq " + day , "day(Timestamp) ne " + day };
      };
      res[3] = (LocalDateTime ldt) -> {
         int hour =ldt.getHour();
         return new String[] { "hour(Timestamp) eq " + hour , "hour(Timestamp) ne " + hour };
      };
      res[4] = (LocalDateTime ldt) -> {
         int minute = ldt.getMinute();
         return new String[] { "minute(Timestamp) eq " + minute , "minute(Timestamp) ne " + minute };
      };
      res[5] = (LocalDateTime ldt) -> {
         int second = ldt.getSecond();
         return new String[] { "second(Timestamp) eq " + second , "second(Timestamp) ne " + second };
      };
      res[6] = (LocalDateTime ldt) -> {
         String fractionalSeconds = ldt.format(DateTimeFormatter.ofPattern("ss.SSS"));
         return new String[] { "fractionalseconds(Timestamp) eq " + fractionalSeconds , "fractionalseconds(Timestamp) ne " + fractionalSeconds };
      };
      res[7] = (LocalDateTime ldt) -> {
         String time = ldt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
         return new String[] { "time(Timestamp) eq " + time , "time(Timestamp) ne " + time };
      };
      res[8] = (LocalDateTime ldt) -> {
         String date = ldt.toLocalDate().toString();
         return new String[] { "date(Timestamp) eq " + date , "date(Timestamp) ne " + date };
      };
      res[9] = (LocalDateTime ldt) -> {
         String dateTime = ldt.minusSeconds(10).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + 'Z';
         return new String[] { "Timestamp gt " + dateTime, "Timestamp le " + dateTime };
      };
      res[10] = (LocalDateTime ldt) -> {
         OffsetDateTime odt = ldt.atOffset(ZoneOffset.UTC);
         String right = odt.withOffsetSameInstant(ZoneOffset.ofHours(3)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
         String wrong = odt.withOffsetSameLocal(ZoneOffset.ofHours(3)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
         return new String[] { "Timestamp eq " + right, "Timestamp eq " + wrong };
      };
      res[11] = (LocalDateTime) -> {
         return new String[] { "Timestamp le now()", "Timestamp ge now()" };
      };
      res[12] = (LocalDateTime) ->
      {
         return new String[] { "Timestamp add duration'P1D' gt now()", "Timestamp lt now() sub duration'PT12H'" };
      };
      return res;
   }

   @Test(groups = "nonzero", dependsOnGroups = "zero", dataProvider = "dateMethodProvider")
   public void testDateMethods(Function<LocalDateTime, String[]> filterSupplier) throws UriParserException, UriValidationException
   {
      String[] filters = filterSupplier.apply(reportTime);
      String filterOK = filters[0];
      String filterNOK = filters[1];

      LOGGER.info("testDateMethods: OK $filter={}    NOK $filter={}", filterOK, filterNOK);
      List<Entity> entities = svc.listMetricsAsEntities(mkVisitor("$filter=" + filterOK));
      assertNotNull(entities);
      assertEquals(entities.size(), 5);

      entities = svc.listMetricsAsEntities(mkVisitor("$filter=" + filterNOK));
      assertNotNull(entities);
      assertEquals(entities.size(), 0);
   }

   @DataProvider(name = "mathMethodProvider")
   public String[][] mathMethodProvider()
   {
      return new String[][] {
         { "Count sub 10 eq 0", COUNTER },
         { "Count add 10 eq 20", COUNTER },
         { "Count mul 10 eq 100", COUNTER },
         { "Count div 5 eq 2", COUNTER },
         //{ "Count mod 2 eq 0", COUNTER }, // HSQLDB does not support the SQL mod (%) operator
         { "ceiling(StandardDeviation) eq 2", HISTOGRAM },
         { "floor(Mean) eq 2", HISTOGRAM },
         { "round(StandardDeviation) eq 2", HISTOGRAM },
      };
   }

   @Test(groups = "nonzero", dependsOnGroups = "zero", dataProvider = "mathMethodProvider")
   public void testMathMethods(String filter, String name) throws UriParserException, UriValidationException
   {
      LOGGER.info("testMathMethods: $filter={} -> {}", filter, name);
      checkHasEntityNamed("$filter=" + filter, name);
   }

   @DataProvider(name = "filterProvider")
   public String[][] filterProvider()
   {
      return new String[][] {
         { "Gauge ne null", GAUGE },
         { "Count eq null", GAUGE },
         { "Gauge ne ''", GAUGE },
         { "not (Gauge eq null)", GAUGE },
         { "Minimum eq 1 and Maximum eq 4", HISTOGRAM },
         { "Minimum eq 1 or Maximum eq 4", HISTOGRAM }
      };
   }

   @Test(groups = "nonzero", dependsOnGroups = "zero", dataProvider = "filterProvider")
   public void testComplexFilters(String filter, String name) throws UriParserException, UriValidationException
   {
      LOGGER.info("testComplexFilters: $filter={} -> {}", filter, name);
      checkHasEntityNamed("$filter=" + filter, name);
   }

   /* Test sliding window, rerun group zero. */

   @BeforeGroups(value = "cleaned", dependsOnGroups = "nonzero")
   public void cleanMetrics() throws InterruptedException
   {
      Thread.sleep(2000);
      svc.applySlidingWindow(); // Should remove all metrics
   }

   @Test(groups = "cleaned", dependsOnGroups = "nonzero")
   public void testCleanedMetrics()
   {
      testZeroMetrics();
   }

   /* Regression tests. */

   /* Issue: Cannot filter by Name if Name contains an underscore:

      org.jdbi.v3.core.statement.UnableToCreateStatementException:
        Error rendering SQL template:
        'SELECT * FROM metrics WHERE name LIKE 'prod\_sync%' ESCAPE '\'  LIMIT 0,50'
        [statement:"SELECT * FROM metrics WHERE name LIKE 'prod\_sync%' ESCAPE '\'  LIMIT 0,50", rewritten:"null", parsed:"null", arguments:{ positional:{}, named:{}, finder:[]}]

      Reason: Not possible to use '\' as the escape character with JDBI3
   */
   @Test(groups = "regression", dependsOnGroups = "cleaned")
   public void testMetricNameContainingUnderscores() throws UriParserException, UriValidationException
   {
      String name = "prod_sync.global.gauges.queued_downloads";
      MetricRegistry reg = new MetricRegistry();
      reg.register(name, (Gauge)() -> 0);
      svc.report(reg.getGauges(), reg.getCounters(), reg.getHistograms(), reg.getMeters(), reg.getTimers());

      checkHasEntityNamed("$filter=startswith(Name, 'prod_sync')", name);
      checkHasEntityNamed("$filter=endswith(Name, 'queued_downloads')", name);
      checkHasEntityNamed("$filter=contains(Name, '_sync')", name);
   }

   /* Helper classes */

   @FunctionalInterface
   private static interface lambdaTopOption extends TopOption
   {
      @Override
      public default String getText()
      {
         return String.valueOf(getValue());
      }

      @Override
      public default SystemQueryOptionKind getKind()
      {
         return null;
      }

      @Override
      public default String getName()
      {
         return null;
      }
   }

   @FunctionalInterface
   private static interface lambdaSkipOption extends SkipOption
   {
      @Override
      public default String getText()
      {
         return String.valueOf(getValue());
      }

      @Override
      public default SystemQueryOptionKind getKind()
      {
         return null;
      }

      @Override
      public default String getName()
      {
         return null;
      }
   }

}
