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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.dhus.metrics.external.MetricsServiceInterface;
import org.dhus.olingo.v2.datamodel.MetricModel;
import org.dhus.olingo.v2.visitor.MetricSQLVisitor;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;

/**
 * Metrics Service, to store metrics in the embedded metrics database.
 *
 * @see Reporter
 */
public class MetricsService implements MetricsServiceInterface
{

   private static final int SECONDS_TO_SUBTRACT = 5*60;

   /** Log. */
   private final Logger LOGGER = LogManager.getLogger();

   /** ObjectMapper. */
   private final Jdbi jdbi;

   /** Width (in `windowUnit`) of the sliding window. */
   private final Integer windowWidth;

   /** Unit for `windowWidth`. */
   private final TimeUnit windowUnit;

   /** Row mapper to directly map to OData entities. */
   private final RowMapper<Entity> rowMapper = new MetricEntityMapper();
   
   private Long nbMetrics;
   
   private Instant lastUpdateNbMetrics;
   
   /**
    * Create new instance, used by Spring.
    *
    * @param jdbi data source
    * @param windowWidth size of the sliding window in `timeUnit`, may be null
    * @param timeUnit DAYS, HOURS, MINUTES, SECONDS, ..., may be null
    */
   MetricsService(Jdbi jdbi, Integer windowWidth, String timeUnit)
   {
      Objects.requireNonNull(jdbi, "JDBI bean not defined, cannot instanciate the Metrics service");

      try (Handle handle = jdbi.open())
      {
         // Using IF NOT EXISTS in case DB is on disk
         handle.execute("CREATE TABLE IF NOT EXISTS metrics ("
                                 /* Common rows */
                           + "date TIMESTAMP(3) NOT NULL, "
                           + "name VARCHAR(255) NOT NULL, "
                           + "type INT NOT NULL, "
                                 /* Counters & Meters & Timers */
                           + "count BIGINT, "
                                 /* Gauges */
                           + "value VARCHAR(255), "
                                 /* Histograms & Timers */
                           + "max DOUBLE, "
                           + "min DOUBLE, "
                           + "mean DOUBLE, "
                           + "median DOUBLE, "
                           + "std_dev DOUBLE, "
                           + "h_75thpercentile DOUBLE, "
                           + "h_95thpercentile DOUBLE, "
                           + "h_98thpercentile DOUBLE, "
                           + "h_99thpercentile DOUBLE, "
                           + "h_999thpercentile DOUBLE, "
                                 /* Meter & Timers */
                           + "mean_rate DOUBLE, "
                           + "m_1m_rate DOUBLE, "
                           + "m_5m_rate DOUBLE, "
                           + "m_15m_rate DOUBLE, "
                           + "PRIMARY KEY (date, name))");
         handle.execute("CREATE INDEX IF NOT EXISTS idx_by_name ON metrics (name)");
      }

      this.jdbi = jdbi;

      if (windowWidth == null || windowWidth.equals(0))
      {
         LOGGER.warn("WindowWidth parameter not set, sliding window disabled, may produce OutOfMemoryError and/or JavaHeapSpaceError");
      }
      else if (windowWidth < 0)
      {
         LOGGER.error("WindowWidth ({}) cannot be lower than zero", windowWidth);
         throw new IllegalArgumentException("Illegal value (below zero) for the WindowWidth parameter");
      }
      this.windowWidth = windowWidth;

      TimeUnit tu = TimeUnit.MINUTES;
      if (timeUnit != null && !timeUnit.isEmpty())
      {
         try
         {
            tu = TimeUnit.valueOf(timeUnit);
         }
         catch (IllegalArgumentException ex)
         {
            LOGGER.warn("TimeUnit '{}' is invalid, defaulting to MINUTES", timeUnit);
         }
      }
      this.windowUnit = tu;

      LOGGER.info("Embedded metric storage started");
   }

   /**
    * Create instance, configured with no sliding window. Used by Spring.
    *
    * @param jdbi data source
    */
   MetricsService(Jdbi jdbi)
   {
      this(jdbi, null, null);
   }

   /**
    * Remove old metrics using the sliding window configuration from monitoring.xml.
    */
   @Override
   public void applySlidingWindow()
   {
      if (windowWidth != null)
      {
         Timestamp threshold = new Timestamp(System.currentTimeMillis() - this.windowUnit.toMillis(this.windowWidth));
         try (Handle handle = jdbi.open())
         {
            handle.useTransaction((transactHandle) ->
            {
               MetricsDao dao = transactHandle.attach(MetricsDao.class);
               int deletedEntries = dao.clearOldEntries(threshold);
               LOGGER.debug("{} out of window metrics removed", deletedEntries);
            });
         }
      }
   }

   public void report(SortedMap<String, Gauge> gauges,
                      SortedMap<String, Counter> counters,
                      SortedMap<String, Histogram> histograms,
                      SortedMap<String, Meter> meters,
                      SortedMap<String, Timer> timers)
   {
      Timestamp timestamp = new Timestamp(System.currentTimeMillis());
     
      if (!(gauges.isEmpty() && counters.isEmpty() && histograms.isEmpty() && meters.isEmpty() && timers.isEmpty()))
      {
         List<Metric> mappedGauges = gauges.entrySet().stream().map((t) -> new Metric(t.getKey(), t.getValue())).collect(Collectors.toList());
         List<Metric> mappedCounters = counters.entrySet().stream().map((t) -> new Metric(t.getKey(), t.getValue())).collect(Collectors.toList());
         List<Metric> mappedHistograms = histograms.entrySet().stream().map((t) -> new Metric(t.getKey(), t.getValue())).collect(Collectors.toList());
         List<Metric> mappedMeters = meters.entrySet().stream().map((t) -> new Metric(t.getKey(), t.getValue())).collect(Collectors.toList());
         List<Metric> mappedTimers = timers.entrySet().stream().map((t) -> new Metric(t.getKey(), t.getValue())).collect(Collectors.toList());

         try (Handle handle = jdbi.open())
         {
            handle.useTransaction((transactHandle) ->
            {
               MetricsDao dao = transactHandle.attach(MetricsDao.class);
               dao.bulkInsertMetrics(timestamp, mappedCounters);
               dao.bulkInsertMetrics(timestamp, mappedGauges);
               dao.bulkInsertMetrics(timestamp, mappedHistograms);
               dao.bulkInsertMetrics(timestamp, mappedMeters);
               dao.bulkInsertMetrics(timestamp, mappedTimers);
            });
         }
      }
   }

   
   @Override
   public Entity getMetricFromStorage(String name, Timestamp time)
   {
      try (Handle handle = jdbi.open())
      {
         MetricsDao dao = handle.attach(MetricsDao.class);
         return dao.getMetricAsEntity(name, time);
      }
   }

   /**
    * List metrics.
    *
    * @return a list of Metric instances, may be empty
    */
   @Override
   public List<Entity> listMetricsAsEntities()
   {
      try (Handle handle = jdbi.open())
      {
         MetricsDao dao = handle.attach(MetricsDao.class);
         return dao.listMetricsAsEntities();
      }
   }

   /**
    * List metrics, filtered, ordered and limited.
    *
    * @param sqlVisitor non null Olingo SQL visitor
    * @return a list of OData entities, may be empty
    */
   public List<Entity> listMetricsAsEntities(ExpressionVisitor<Object> visitor)
   {
      if (visitor == null || !(visitor instanceof MetricSQLVisitor))
      {
         LOGGER.warn("Could not check metrics");
         return null;
      }
      List<Entity> metrics = null;

      String metricPrefixes = System.getProperty("dhus.metrics.filter", null);
      String[] prefixList = null;
      if (metricPrefixes != null)
      {         
         prefixList = metricPrefixes.split("\\,");
      }

      StringBuilder queryBuilder = buildQuery(visitor, prefixList);

      try (Handle handle = jdbi.open())
      {
         metrics = handle.createQuery(queryBuilder.toString()).map(rowMapper)
               .collect(Collectors.toList());
      }

      // In case of CSV Reporter, metrics == null.
      // Need to access values in a different way
      if (metrics == null)
      {
         LOGGER.info("For metrics check CSV");
      }

      if (metricPrefixes != null && !metricPrefixes.contains("prod_sync"))
      {
         return metrics;
      }
      else
      {
         metrics.addAll(listAggregationTypeMetricsAsEntities((MetricSQLVisitor) visitor));
         return metrics;
      }
   }

   private StringBuilder buildQuery(ExpressionVisitor<Object> visitor, String[] prefixList)
   {
      MetricSQLVisitor sqlVisitor = (MetricSQLVisitor) visitor;
      Objects.requireNonNull(sqlVisitor, "Parameter `sqlVisitor` cannot be null");

      String whereClause = sqlVisitor.getSqlFilter();
      String orderByClause = sqlVisitor.getSqlOrder();

      StringBuilder queryBuilder = new StringBuilder();
      
      queryBuilder.append("SELECT * FROM metrics ");

      if (prefixList != null)
      {         
         int size = prefixList.length;        
         queryBuilder.append(" WHERE (");

         for (int i = 0; i < size-1; i++)
         {
            String prefix = prefixList[i];
            queryBuilder.append("name like '" + prefix + "%'").append(" OR ");
         }         
         queryBuilder.append("name like '" + prefixList[size-1] + "%')");

         if (whereClause != null && !whereClause.isEmpty())
         {
            queryBuilder.append(" AND ").append(whereClause);
         }
      }
      else if (whereClause != null && !whereClause.isEmpty())
      {
         queryBuilder.append(" WHERE ").append(whereClause);
      }
 
      if (orderByClause != null && !orderByClause.isEmpty())
      {
         queryBuilder.append(" ORDER BY ").append(orderByClause);
      }
      queryBuilder.append(" LIMIT ")
            .append(sqlVisitor.getSkip())
            .append(',')
            .append(sqlVisitor.getTop());
      return queryBuilder;
   }
   
   public List<Entity> listAggregationTypeMetricsAsEntities(MetricSQLVisitor visitor)
   {
      List<Entity> metrics = new ArrayList<Entity>();
      String whereClause = visitor.getSqlFilter();
      LOGGER.debug("Aggregated metrics filter: {}", whereClause);
      HourlyMetricCounterEntityMapper hourlyCounterMapper = new HourlyMetricCounterEntityMapper();
      DailyMetricCounterEntityMapper dailyCounterMapper = new DailyMetricCounterEntityMapper();
      MonthlyMetricCounterEntityMapper monthlyCounterMapper = new MonthlyMetricCounterEntityMapper();
      HourlyMetricTimersEntityMapper hourlyTimerMapper = new HourlyMetricTimersEntityMapper();
      DailyMetricTimersEntityMapper dailyTimerMapper = new DailyMetricTimersEntityMapper();
      MonthlyMetricTimersEntityMapper monthlyTimerMapper = new MonthlyMetricTimersEntityMapper();
      
      if (whereClause == null || whereClause.toUpperCase().contains("name like '%hourly%'".toUpperCase()))
      {
         metrics.addAll(getAggregationMetrics(null, hourlyCounterMapper, MetricsRequests.HOURLY_COUNTER_REQ));
         metrics.addAll(getAggregationMetrics(null, hourlyTimerMapper, MetricsRequests.HOURLY_TIMER_REQ));
      }
      else
      {
         metrics.addAll(getAggregationMetrics(whereClause, hourlyCounterMapper, MetricsRequests.HOURLY_COUNTER_REQ));
         metrics.addAll(getAggregationMetrics(whereClause, hourlyTimerMapper, MetricsRequests.HOURLY_TIMER_REQ));
      }
      if (whereClause == null || whereClause.toUpperCase().contains("name like '%daily%'".toUpperCase()))
      {
         metrics.addAll(getAggregationMetrics(null, dailyCounterMapper, MetricsRequests.DAILY_COUNTER_REQ));
         metrics.addAll(getAggregationMetrics(null, dailyTimerMapper, MetricsRequests.DAILY_TIMER_REQ));
      }
      else
      {
         metrics.addAll(getAggregationMetrics(whereClause, dailyCounterMapper, MetricsRequests.DAILY_COUNTER_REQ));
         metrics.addAll(getAggregationMetrics(whereClause, dailyTimerMapper, MetricsRequests.DAILY_TIMER_REQ));
      }
      if (whereClause == null || whereClause.toUpperCase().contains("name like '%monthly%'".toUpperCase()))
      {
         metrics.addAll(getAggregationMetrics(null, monthlyCounterMapper, MetricsRequests.MONTHLY_COUNTER_REQ));
         metrics.addAll(getAggregationMetrics(null, monthlyTimerMapper, MetricsRequests.MONTHLY_TIMER_REQ));
      }
      else
      {
         metrics.addAll(getAggregationMetrics(whereClause, monthlyCounterMapper, MetricsRequests.MONTHLY_COUNTER_REQ));
         metrics.addAll(getAggregationMetrics(whereClause, monthlyTimerMapper, MetricsRequests.MONTHLY_TIMER_REQ));
      }
      
      return metrics;
   }

   private List<Entity> getAggregationMetrics(String whereClause, RowMapper<Entity> rowMapper, String request)
   {
      List<Entity> metrics = new ArrayList<Entity>();
      try (Handle handle = jdbi.open())
      {
         StringBuilder queryBuilder = new StringBuilder();
         queryBuilder.append(request);

         if (whereClause != null && whereClause != "")
         {
            queryBuilder.append(" AND ").append(whereClause);
         }

         queryBuilder.append(MetricsRequests.GROUP_BY_CLAUSE);
         LOGGER.debug("The request is: {}", queryBuilder.toString());
         metrics.addAll(handle.createQuery(queryBuilder.toString())
               .map(rowMapper)
               .collect(Collectors.toList()));
      }
      return metrics;
   }
   
   @Override
   public int countMetrics(FilterOption filterOption)
   {
      // TODO Auto-generated method stub
      MetricSQLVisitor visitor = new MetricSQLVisitor(filterOption, null, null, null);
      return countMetrics(visitor).intValue();
   }

   /**
    * Count metrics.
    *
    * @return number of metrics
    */
   private Long countMetrics(MetricSQLVisitor visitor)
   {
      String whereClause = visitor.getSqlFilter();
      if (nbMetrics == null
            || (lastUpdateNbMetrics != null && lastUpdateNbMetrics.plusMillis(1000).isBefore(Instant.now())))
      {
         nbMetrics = countAllMetricsMatchingRequest(MetricsRequests.SELECT_COUNT, whereClause);
         if (whereClause == null || whereClause.toUpperCase().contains("name like '%hourly%'".toUpperCase()))
         {
            nbMetrics += countAggregatedMetricsMatchingRequest(MetricsRequests.HOURLY_COUNTER_REQ, null);
            nbMetrics += countAggregatedMetricsMatchingRequest(MetricsRequests.HOURLY_TIMER_REQ, null);
         }
         else
         {
            nbMetrics += countAggregatedMetricsMatchingRequest(MetricsRequests.HOURLY_COUNTER_REQ, whereClause);
            nbMetrics += countAggregatedMetricsMatchingRequest(MetricsRequests.HOURLY_TIMER_REQ, whereClause);
         }
         if (whereClause == null || whereClause.toUpperCase().contains("name like '%daily%'".toUpperCase()))
         {
            nbMetrics += countAggregatedMetricsMatchingRequest(MetricsRequests.DAILY_COUNTER_REQ, null);
            nbMetrics += countAggregatedMetricsMatchingRequest(MetricsRequests.DAILY_TIMER_REQ, null);
         }
         else
         {
            nbMetrics += countAggregatedMetricsMatchingRequest(MetricsRequests.DAILY_COUNTER_REQ, whereClause);
            nbMetrics += countAggregatedMetricsMatchingRequest(MetricsRequests.DAILY_TIMER_REQ, whereClause);
         }
         if (whereClause == null || whereClause.toUpperCase().contains("name like '%monthly%'".toUpperCase()))
         {
            nbMetrics += countAggregatedMetricsMatchingRequest(MetricsRequests.MONTHLY_COUNTER_REQ, null);
            nbMetrics += countAggregatedMetricsMatchingRequest(MetricsRequests.MONTHLY_TIMER_REQ, null);
         }
         else
         {
            nbMetrics += countAggregatedMetricsMatchingRequest(MetricsRequests.MONTHLY_COUNTER_REQ, whereClause);
            nbMetrics += countAggregatedMetricsMatchingRequest(MetricsRequests.MONTHLY_TIMER_REQ, whereClause);
         }
      }
      lastUpdateNbMetrics = Instant.now();
      return nbMetrics;
   }
   
   private Long countAggregatedMetricsMatchingRequest(String request, String whereClause)
   {
      Long nb = null;
      try (Handle handle = jdbi.open())
      {
         StringBuilder queryBuilder = new StringBuilder();

         queryBuilder.append(MetricsRequests.COUNT_REQ).append(request);
         if (whereClause != null && whereClause != "")
         {
            queryBuilder.append(" AND ").append(whereClause);
         }
         queryBuilder.append(MetricsRequests.GROUP_BY_CLAUSE).append(MetricsRequests.END_COUNT_REQ);
         LOGGER.debug("The request is: {}", queryBuilder.toString());
         nb = handle.createQuery(queryBuilder.toString()).mapTo(Long.class).first();
      }
      return nb;
   }
   
   private Long countAllMetricsMatchingRequest(String request, String whereClause)
   {
      Long nb = null;
      try (Handle handle = jdbi.open())
      {

         StringBuilder queryBuilder = new StringBuilder();

         queryBuilder.append(MetricsRequests.SELECT_COUNT);
         if (whereClause != null && whereClause != "")
         {
            queryBuilder.append(" WHERE ").append(whereClause);
         }
         LOGGER.debug("The request is: {}", queryBuilder.toString());
         nb = handle.createQuery(queryBuilder.toString()).mapTo(Long.class).first();
      }
      return nb;
   }
   
   /**
    * List prod sync counter metrics
    * We use these methods to define the best source for the synchronizers
    * We check the last five minutes metrics
    */
   @Override
   public Long getProdSyncSuccessCountersBySync(Long syncId, Long sourceId, Timestamp time)
   {
      String metricName = formatMetricName("counters", syncId, sourceId, "success");
      try (Handle handle = jdbi.open())
      {
         MetricsDao dao = handle.attach(MetricsDao.class);
         Long successCounter = dao.getLastCounter(metricName, time);
         LOGGER.debug("Metric name {} - Succes counter {}", metricName, successCounter);         
         return successCounter;
      }
   }
   
   @Override
   public Double getProdSyncSuccessFifteenMinutesRateBySync(Long syncId, Long sourceId, Timestamp time)
   {
      String metricName = formatMetricName("counters", syncId, sourceId, "success");
      try (Handle handle = jdbi.open())
      {
         MetricsDao dao = handle.attach(MetricsDao.class);
         Entity entity = dao.getLastMetricWithMetricName(metricName, time);
         displayDebugLogsForEntity(metricName, entity);
         Double successRate = extractFifteenMinuteRate(entity);
         LOGGER.debug("Metric name {} - Succes counter {}", metricName, successRate);         
         return successRate;
      }
   }

   private Double extractFifteenMinuteRate(Entity entity)
   {
      if (entity == null)
      {
         return null;
      }
      Property propertyFifteenMinRate = entity.getProperty(MetricModel.FIFTEENMINUTESRATE);
      Double fifteenMinuteRate = null;
      if (propertyFifteenMinRate != null)
      {
          fifteenMinuteRate = (Double)propertyFifteenMinRate.getValue();
      }
      return fifteenMinuteRate;
   }
   
   @Override
   public Long getProdSyncFailureCountersBySync(Long syncId, Long sourceId, Timestamp time)
   {
      String metricName = formatMetricName("counters", syncId, sourceId, "failure");
      try (Handle handle = jdbi.open())
      {
         MetricsDao dao = handle.attach(MetricsDao.class);
         Long failureCounter = dao.getLastCounter(metricName, time);
         LOGGER.debug("Metric name {} - Failure counter {}", metricName, failureCounter);
         return failureCounter;
      }
   }
   
   @Override
   public Double getProdSyncFailureFifteenMinutesRateBySync(Long syncId, Long sourceId, Timestamp time)
   {
      String metricName = formatMetricName("counters", syncId, sourceId, "failure");
      try (Handle handle = jdbi.open())
      {
         MetricsDao dao = handle.attach(MetricsDao.class);
         Entity entity = dao.getLastMetricWithMetricName(metricName, time);
         displayDebugLogsForEntity(metricName,entity);
         Double failureRate = extractFifteenMinuteRate(entity);
         LOGGER.debug("Metric name {} - Succes counter {}", metricName, failureRate);         
         return failureRate;
      }
   }
   
   private void displayDebugLogsForEntity(String metricName, Entity entity)
   {
      if (entity != null)
      {
         LOGGER.debug("For metric '{}' we have {}", metricName, entity.toString());
      }
   }

   @Override
   public Double getProdSyncTransferSizeFiveMinutesRateBySync(Long syncId, Long sourceId, Timestamp time)
   {
      String metricName = formatMetricName("meters", syncId, sourceId, "transferSize");
      try (Handle handle = jdbi.open())
      {
         MetricsDao dao = handle.attach(MetricsDao.class);
         Double meterFifteenMinutesRate = dao.getLastTimerFifteenMinutesTransferRate(metricName, time);
         LOGGER.debug("Metric name {} - Timer fifteen minutes Rate {}", metricName, meterFifteenMinutesRate);
         return meterFifteenMinutesRate;
      }
   }
   
   private String formatMetricName(String type, Long syncId, Long sourceId, String label)
   {
      StringBuilder sb = new StringBuilder("prod_sync.sync").append(syncId)
            .append(".source");
      if (sourceId != null)
      {
         sb.append(sourceId);
      }
      sb.append(".").append(type).append(".").append(label);
      return sb.toString();
   }
}
