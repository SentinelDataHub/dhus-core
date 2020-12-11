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

import java.sql.Timestamp;
import java.util.List;

import org.apache.olingo.commons.api.data.Entity;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * DAO to manage metrics in the storage.
 *
 * @see MetricsService
 */
public interface MetricsDao
{
   /**
    * Insert many metrics.
    *
    * @param time should be NOW()
    * @param metric list of mapped object
    */
   @SqlBatch("INSERT INTO metrics(date, name, type, count, value, max, min, mean, median, std_dev, h_75thpercentile, h_95thpercentile, h_98thpercentile, h_99thpercentile, h_999thpercentile, mean_rate, m_1m_rate, m_5m_rate, m_15m_rate) "
           + "VALUES (:dateparam, :name, :type.type, "
           + ":counter.count, "
           + ":gauge.value, "
           + ":histogram.max, :histogram.min, :histogram.mean, :histogram.median, :histogram.standardDeviation, :histogram.75thPercentile, :histogram.95thPercentile, :histogram.98thPercentile, :histogram.99thPercentile, :histogram.999thPercentile, "
           + ":meter.meanRate, :meter.oneMinuteRate, :meter.fiveMinuteRate, :meter.fifteenMinuteRate)")
   void bulkInsertMetrics(@Bind("dateparam") Timestamp time, @BindBean List<Metric> metric);

   /**
    * Count all metrics.
    *
    * @return Total number of metrics
    */
   @SqlQuery("SELECT COUNT(1) FROM metrics")
   int countMetrics();

   /**
    * List all metrics.
    *
    * @return list of all metrics, sorted by date
    */
   @SqlQuery("SELECT * FROM metrics")
   @RegisterRowMapper(MetricEntityMapper.class)
   List<Entity> listMetricsAsEntities();

   /**
    * Clear entries that are older than the given threshold date.
    *
    * @param threshold date
    * @return number of removed rows
    */
   @SqlUpdate("DELETE FROM metrics WHERE date <= ?")
   int clearOldEntries(Timestamp threshold);

   /**
    * Get a metric by its PK.
    *
    * @param name of the metric
    * @param time report time
    * @return an OData Entity or null if no result returned
    */
   @SqlQuery("SELECT * FROM metrics WHERE name = :name AND date = :time")
   @RegisterRowMapper(MetricEntityMapper.class)
   Entity getMetricAsEntity(@Bind("name") String name, @Bind("time") Timestamp time);
}
