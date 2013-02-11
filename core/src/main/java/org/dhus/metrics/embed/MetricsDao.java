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
   
   /*
    * List all hourly synchronizer metrics
    */
   @SqlQuery("select max(count) as count, name, type from metrics " + 
         "where name like 'prod_sync%counters%' " + 
         "and date >= now() - '1' HOUR " + 
         "group by name, type")
   @RegisterRowMapper(HourlyMetricCounterEntityMapper.class)
   List<Entity> listMetricsHourlyAggregationAsEntities();
   
   /*
    * count all hourly synchronizer metrics
    */
   @SqlQuery("select count(1) from (select max(count) as count, name, type from metrics " + 
         "where name like 'prod_sync%counters%' " + 
         "and date >= now() - '1' HOUR " + 
         "group by name, type)")
   int countMetricsHourlyAggregationAsEntities();
   
   /*
    * List all daily synchronizer metrics
    */
   @SqlQuery("select max(count) as count, name, type from metrics " + 
         "where name like 'prod_sync%counters%' " + 
         "and date >= now() - '1' DAY " + 
         "group by name, type")
   @RegisterRowMapper(DailyMetricCounterEntityMapper.class)
   List<Entity> listMetricsDailyAggregationAsEntities();
   
   /*
    * Count all daily synchronizer metrics
    */
   @SqlQuery("select count(1) from (select max(count) as count, name, type from metrics " + 
         "where name like 'prod_sync%counters%' " + 
         "and date >= now() - '1' DAY " + 
         "group by name, type)")
   int countMetricsDailyAggregationAsEntities();
   
   /*
    * List all monthly synchronizer metrics
    */
   @SqlQuery("select max(count) as count, name, type from metrics " + 
         "where name like 'prod_sync%counters%' " + 
         "and date >= now() - '30' DAY " + 
         "group by name, type")
   @RegisterRowMapper(MonthlyMetricCounterEntityMapper.class)
   List<Entity> listMetricsMonthlyAggregationAsEntities();
   
   /*
    * Count all monthly synchronizer metrics
    */
   @SqlQuery("select count(1) from (select max(count) as count, name, type from metrics " + 
         "where name like 'prod_sync%counters%' " + 
         "and date >= now() - '30' DAY " + 
         "group by name, type)")
   int countMetricsMonthlyAggregationAsEntities();
   
   /**
    * TIMERS
    */
   /*
    * List all hourly synchronizer metrics
    */
   @SqlQuery("select name, type, avg(max) as max, avg(min) as min, avg(mean) as mean, avg(median) as median, avg(std_dev) as std_dev, avg(h_75thpercentile) as h_75thpercentile, " + 
         "avg(h_95thpercentile) as h_95thpercentile, avg(h_98thpercentile) as h_98thpercentile, avg(h_99thpercentile) as h_99thpercentile, avg(h_999thpercentile) as h_999thpercentile, avg(mean_rate) as mean_rate, " + 
         "avg(m_1m_rate) as m_1m_rate, avg(m_5m_rate) as m_5m_rate,avg(m_15m_rate) as m_15m_rate, max(count) as count  from metrics " + 
         " where name like 'prod_sync%timers%' and date >= now() - '1' HOUR group by name, type")
   @RegisterRowMapper(HourlyMetricTimersEntityMapper.class)
   List<Entity> listTimersMetricsHourlyAggregationAsEntities();
   
   /*
    * count all hourly synchronizer metrics
    */
   @SqlQuery("select count(1) from (select name, type, avg(max) as max, avg(min) as min, avg(mean) as mean, avg(median) as median, avg(std_dev) as std_dev, avg(h_75thpercentile) as h_75thpercentile, " + 
         "avg(h_95thpercentile) as h_95thpercentile, avg(h_98thpercentile) as h_98thpercentile, avg(h_99thpercentile) as h_99thpercentile, avg(h_999thpercentile) as h_999thpercentile, avg(mean_rate) as mean_rate, " + 
         "avg(m_1m_rate) as m_1m_rate, avg(m_5m_rate) as m_5m_rate,avg(m_15m_rate) as m_15m_rate, max(count) as count  from metrics " + 
         " where name like 'prod_sync%timers%' and date >= now() - '1' HOUR group by name, type)")
   int countTimersMetricsHourlyAggregationAsEntities();
   
   /*
    * List all daily synchronizer metrics
    */
   @SqlQuery("select name, type, avg(max) as max, avg(min) as min, avg(mean) as mean, avg(median) as median, avg(std_dev) as std_dev, avg(h_75thpercentile) as h_75thpercentile, " + 
         "avg(h_95thpercentile) as h_95thpercentile, avg(h_98thpercentile) as h_98thpercentile, avg(h_99thpercentile) as h_99thpercentile, avg(h_999thpercentile) as h_999thpercentile, avg(mean_rate) as mean_rate, " + 
         "avg(m_1m_rate) as m_1m_rate, avg(m_5m_rate) as m_5m_rate,avg(m_15m_rate) as m_15m_rate, max(count) as count  from metrics " + 
         " where name like 'prod_sync%timers%' and date >= now() - '1' DAY group by name, type")
   @RegisterRowMapper(DailyMetricTimersEntityMapper.class)
   List<Entity> listTimersMetricsDailyAggregationAsEntities();
   
   /*
    * Count all daily synchronizer metrics
    */
   @SqlQuery("select count(1) from (select name, type, avg(max) as max, avg(min) as min, avg(mean) as mean, avg(median) as median, avg(std_dev) as std_dev, avg(h_75thpercentile) as h_75thpercentile, " + 
         "avg(h_95thpercentile) as h_95thpercentile, avg(h_98thpercentile) as h_98thpercentile, avg(h_99thpercentile) as h_99thpercentile, avg(h_999thpercentile) as h_999thpercentile, avg(mean_rate) as mean_rate, " + 
         "avg(m_1m_rate) as m_1m_rate, avg(m_5m_rate) as m_5m_rate,avg(m_15m_rate) as m_15m_rate, max(count) as count  from metrics " + 
         " where name like 'prod_sync%timers%' and date >= now() - '1' DAY group by name, type)")
   int countTimersMetricsDailyAggregationAsEntities();
   
   /*
    * List all monthly synchronizer metrics
    */
   @SqlQuery("select name, type, avg(max) as max, avg(min) as min, avg(mean) as mean, avg(median) as median, avg(std_dev) as std_dev, avg(h_75thpercentile) as h_75thpercentile, " + 
         "avg(h_95thpercentile) as h_95thpercentile, avg(h_98thpercentile) as h_98thpercentile, avg(h_99thpercentile) as h_99thpercentile, avg(h_999thpercentile) as h_999thpercentile, avg(mean_rate) as mean_rate, " + 
         "avg(m_1m_rate) as m_1m_rate, avg(m_5m_rate) as m_5m_rate,avg(m_15m_rate) as m_15m_rate, max(count) as count  from metrics " + 
         " where name like 'prod_sync%timers%' and date >= now() - '30' DAY group by name, type")
   @RegisterRowMapper(MonthlyMetricTimersEntityMapper.class)
   List<Entity> listTimersMetricsMonthlyAggregationAsEntities();
   
   /*
    * Count all monthly synchronizer metrics
    */
   @SqlQuery("select count(1) from (select name, type, avg(max) as max, avg(min) as min, avg(mean) as mean, avg(median) as median, avg(std_dev) as std_dev, avg(h_75thpercentile) as h_75thpercentile, " + 
         "avg(h_95thpercentile) as h_95thpercentile, avg(h_98thpercentile) as h_98thpercentile, avg(h_99thpercentile) as h_99thpercentile, avg(h_999thpercentile) as h_999thpercentile, avg(mean_rate) as mean_rate, " + 
         "avg(m_1m_rate) as m_1m_rate, avg(m_5m_rate) as m_5m_rate,avg(m_15m_rate) as m_15m_rate, max(count) as count  from metrics " + 
         " where name like 'prod_sync%timers%' and date >= now() - '30' DAY group by name, type)")
   int countTimersMetricsMonthlyAggregationAsEntities();
   
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
   
   @SqlQuery("SELECT * FROM metrics WHERE name = :name AND date <= timestamp(:time) order by date desc limit 1")
   @RegisterRowMapper(MetricEntityMapper.class)
   Entity getLastMetricWithMetricName(@Bind("name") String name, @Bind("time") Timestamp time);
   
   
   //By select the max count during this time period, we will obtain the last information because the counter is incremented
   @SqlQuery("SELECT count FROM metrics WHERE name = :name AND date <= timestamp(:time) AND date >= timestamp(:timeEnd) order by date desc limit 1")
   Long getCounter(@Bind("name") String metricName, @Bind("time") Timestamp time,
         @Bind("timeEnd") Timestamp timeEnd);
   
   @SqlQuery("SELECT count FROM metrics WHERE name = :name AND date <= timestamp(:time) order by date desc limit 1")
   Long getLastCounter(@Bind("name") String metricName, @Bind("time") Timestamp time);
   
   @SqlQuery("SELECT m_5m_rate FROM metrics WHERE name = :name AND date <= timestamp(:time) order by date desc limit 1")
   Double getLastFiveMinutesRateForCounter(@Bind("name") String metricName, @Bind("time") Timestamp time);
   
   @SqlQuery("SELECT m_15m_rate FROM metrics WHERE name = :name AND date <= timestamp(:time) order by date desc limit 1")
   Double getLastFifteenMinutesRateForCounter(@Bind("name") String metricName, @Bind("time") Timestamp time);
   
   @SqlQuery("SELECT m_5m_rate FROM metrics WHERE name = :name AND date <= timestamp(:time) AND date >= timestamp(:timeEnd) order by date desc limit 1")
   Double getTimerFiveMinutesTransferRate(@Bind("name") String metricName, @Bind("time") Timestamp time,
         @Bind("timeEnd") Timestamp timeEnd);
   
   @SqlQuery("SELECT m_5m_rate FROM metrics WHERE name = :name AND date <= timestamp(:time) order by date desc limit 1")
   Double getLastTimerFiveMinutesTransferRate(@Bind("name") String metricName, @Bind("time") Timestamp time);
   
   @SqlQuery("SELECT m_15m_rate FROM metrics WHERE name = :name AND date <= timestamp(:time) order by date desc limit 1")
   Double getLastTimerFifteenMinutesTransferRate(@Bind("name") String metricName, @Bind("time") Timestamp time);
   
}
