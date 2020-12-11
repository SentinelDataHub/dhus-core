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
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.commons.api.data.Entity;

import org.dhus.olingo.v2.visitor.MetricSQLVisitor;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;

/**
 * Metrics Service, to store metrics in the embedded metrics database.
 *
 * @see Reporter
 */
public class MetricsService
{
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
                           + "max BIGINT, "
                           + "min BIGINT, "
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

   /**
    * Get a metric by its key.
    *
    * @param name of the metric
    * @param time report time
    * @return an OData Entity or {@code null}
    * @throws NullPointerException if either `name` or `time` is null
    */
   public Entity getMetricAsEntity(String name, Timestamp time)
   {
      Objects.requireNonNull(name, "parameter `name` cannot be null");
      Objects.requireNonNull(time, "parameter `time` cannot be null");

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
   public List<Entity> listMetricsAsEntities(MetricSQLVisitor sqlVisitor)
   {
      Objects.requireNonNull(sqlVisitor, "Parameter `sqlVisitor` cannot be null");

      String whereClause = sqlVisitor.getSqlFilter();
      String orderByClause = sqlVisitor.getSqlOrder();

      StringBuilder queryBuilder = new StringBuilder("SELECT * FROM metrics");
      if (whereClause != null && !whereClause.isEmpty())
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

      try (Handle handle = jdbi.open())
      {
         return handle.createQuery(queryBuilder.toString())
               .map(rowMapper)
               .collect(Collectors.toList());
      }
   }

   /**
    * Count metrics.
    *
    * @return number of metrics
    */
   public int countMetrics()
   {
      try (Handle handle = jdbi.open())
      {
         MetricsDao dao = handle.attach(MetricsDao.class);
         return dao.countMetrics();
      }
   }
}
