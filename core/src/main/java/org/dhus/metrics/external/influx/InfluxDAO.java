package org.dhus.metrics.external.influx;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.dhus.metrics.embed.MetricTypes;
import org.dhus.metrics.external.influx.database.ProdSyncDailyCounterPOJO;
import org.dhus.metrics.external.influx.database.ProdSyncDailyTimersBySourcePOJO;
import org.dhus.metrics.external.influx.database.ProdSyncGlobalQueuedDownloadsPOJO;
import org.dhus.metrics.external.influx.database.ProdSyncHourlyCounterBySourcePOJO;
import org.dhus.metrics.external.influx.database.ProdSyncHourlyCounterPOJO;
import org.dhus.metrics.external.influx.database.ProdSyncHourlyTimersBySourcePOJO;
import org.dhus.metrics.external.influx.database.ProdSyncMonthlyCounterBySourcePOJO;
import org.dhus.metrics.external.influx.database.ProdSyncMonthlyCounterPOJO;
import org.dhus.metrics.external.influx.database.ProdSyncMonthlyTimersBySourcePOJO;
import org.dhus.metrics.external.influx.database.ProdSyncCounterBySourcePOJO;
import org.dhus.metrics.external.influx.database.ProdSyncCounterPOJO;
import org.dhus.metrics.external.influx.database.ProdSyncDailyCounterBySourcePOJO;
import org.dhus.metrics.external.influx.database.ProdSyncQueuedDownloadsPOJO;
import org.dhus.metrics.external.influx.database.ProdSyncTimelinessCreationPOJO;
import org.dhus.metrics.external.influx.database.ProdSyncTimelinessIngestionPOJO;
import org.dhus.metrics.external.influx.database.ProdSyncTimelinessPOJO;
import org.dhus.metrics.external.influx.database.ProdSyncTimerBySourcePOJO;
import org.dhus.metrics.external.influx.database.ProdSyncTimerPOJO;
import org.dhus.olingo.v2.visitor.InfluxMetricsVisitor;
import org.dhus.olingo.v2.visitor.MetricSQLVisitor;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.impl.InfluxDBResultMapper;
import org.influxdb.dto.QueryResult;

/**
 * This class describes the queries uses for each measurement of 
 * @author rosine
 *
 */
public class InfluxDAO
{
   /** Log. */
   private final Logger LOGGER = LogManager.getLogger();
   
   private InfluxDB client;
   
   //Queries for data into prod_sync_counters
   
   public static String CREATE_HOURLY_CONTINUOUS_QUERY_PROD_SYNC_COUNTERS = "SELECT sum(failure) as failure, sum(success) as success, sum(volume) as volume into \"hourly_measures\" "
         + "from prod_sync_counters where productType <> '' group by time(1h), platformSerialId, platformShortName, productType, dhus_inst";
   
   public static String CREATE_DAILY_CONTINUOUS_QUERY_PROD_SYNC_COUNTERS = "SELECT sum(failure) as failure, sum(success) as success, sum(volume) as volume into \"daily_measures\" "
         + "from prod_sync_counters where productType <> '' group by time(1d), platformSerialId, platformShortName, productType, dhus_inst";
   
   public static String CREATE_MONTHLY_CONTINUOUS_QUERY_PROD_SYNC_COUNTERS = "SELECT sum(failure) as failure, sum(success) as success, sum(volume) as volume into \"monthly_measures\" "
         + "from prod_sync_counters where productType <> '' group by time(30d), platformSerialId, platformShortName, productType, dhus_inst";
   
   //Continuous queries by source id and sync (prod_sync_counters_by_sync)
   public static String CREATE_HOURLY_CONTINUOUS_QUERY_PROD_SYNC_COUNTERS_BY_SOURCE = "SELECT sum(failure) as failure, sum(success) as success, sum(volume) as volume into \"hourly_measures_by_source\" "
         + "from prod_sync_counters_by_sync where productType <> '' group by time(1h), platformSerialId, platformShortName, productType, dhus_inst, sourceid, syncid";
   
   public static String CREATE_DAILY_CONTINUOUS_QUERY_PROD_SYNC_COUNTERS_BY_SOURCE = "SELECT sum(failure) as failure, sum(success) as success, sum(volume) as volume into \"daily_measures_by_source\" "
         + "from prod_sync_counters_by_sync where productType <> '' group by time(1d), platformSerialId, platformShortName, productType, dhus_inst, sourceid, syncid";
   
   public static String CREATE_MONTHLY_CONTINUOUS_QUERY_PROD_SYNC_COUNTERS_BY_SOURCE = "SELECT sum(failure) as failure, sum(success) as success, sum(volume) as volume into \"monthly_measures_by_source\" "
         + "from prod_sync_counters_by_sync where productType <> '' group by time(30d), platformSerialId, platformShortName, productType, dhus_inst, sourceid, syncid";
   
   //Continuous queries into prod_sync_timers_by_sync
   public static String CREATE_HOURLY_CONTINUOUS_QUERY_PROD_SYNC_TIMERS_BY_SOURCE ="SELECT mean(\"50-percentile\") as \"50-percentile\", mean(\"75-percentile\") as \"75-percentile\", mean(\"95-percentile\") as \"95-percentile\","
         + " mean(\"99-percentile\") as \"99-percentile\", mean(\"999-percentile\") as \"999-percentile\", max(count) as count, mean(\"fifteen-minute\") as \"fifteen-minute\", mean(\"five-minute\") as \"five-minute\","
         + " mean(max) as max, mean(\"mean-minute\") as \"mean-minute\", mean(min) as min, mean(\"one-minute\") as \"one-minute\", max(\"run-count\") as \"run-count\", mean(\"std-dev\") as \"std-dev\" INTO \"hourly_timers_by_source\" "
         + "FROM prod_sync_timers_by_sync GROUP BY time(1h), platformSerialId, platformShortName, productType, dhus_inst, sourceid, syncid";
   
   public static String CREATE_DAILY_CONTINUOUS_QUERY_PROD_SYNC_TIMERS_BY_SOURCE ="SELECT mean(\"50-percentile\") as \"50-percentile\", mean(\"75-percentile\") as \"75-percentile\", mean(\"95-percentile\") as \"95-percentile\","
         + " mean(\"99-percentile\") as \"99-percentile\", mean(\"999-percentile\") as \"999-percentile\", max(count) as count, mean(\"fifteen-minute\") as \"fifteen-minute\", mean(\"five-minute\") as \"five-minute\", mean(max) as max,"
         + " mean(\"mean-minute\") as \"mean-minute\", mean(min) as min, mean(\"one-minute\") as \"one-minute\", max(\"run-count\") as \"run-count\", mean(\"std-dev\") as \"std-dev\" INTO \"daily_timers_by_source\" "
         + "FROM prod_sync_timers_by_sync GROUP BY time(1d), platformSerialId, platformShortName, productType, dhus_inst, sourceid, syncid";
   
   public static String CREATE_MONTHLY_CONTINUOUS_QUERY_PROD_SYNC_TIMERS_BY_SOURCE ="SELECT mean(\"50-percentile\") as \"50-percentile\", mean(\"75-percentile\") as \"75-percentile\", mean(\"95-percentile\") as \"95-percentile\","
         + " mean(\"99-percentile\") as \"99-percentile\", mean(\"999-percentile\") as \"999-percentile\", max(count) as count, mean(\"fifteen-minute\") as \"fifteen-minute\", mean(\"five-minute\") as \"five-minute\", mean(max) as max,"
         + " mean(\"mean-minute\") as \"mean-minute\", mean(min) as min, mean(\"one-minute\") as \"one-minute\", max(\"run-count\") as \"run-count\", mean(\"std-dev\") as \"std-dev\" INTO \"monthly_timers_by_source\" "
         + "FROM prod_sync_timers_by_sync GROUP BY time(30d), platformSerialId, platformShortName, productType, dhus_inst, sourceid, syncid";
   
   // Select requests
   private static final String SELECT_HOURLY_MEASURES_PROD_SYNC = "SELECT time, dhus_inst, failure, success, volume, "
         + "productType, platformSerialId, platformShortName FROM hourly_measures";
   
   private static final String SELECT_DAILY_MEASURES_PROD_SYNC = "SELECT time, dhus_inst, failure, success, volume, "
         + "productType, platformSerialId, platformShortName FROM daily_measures";
   
   private static final String SELECT_MONTHLY_MEASURES_PROD_SYNC = "SELECT time, dhus_inst, failure, success, volume, "
         + "productType, platformSerialId, platformShortName FROM monthly_measures";
   
   private static final String SELECT_HOURLY_MEASURES_PROD_SYNC_BY_SOURCE = "SELECT time, dhus_inst, failure, success, volume, "
         + "productType, platformSerialId, platformShortName, syncid, sourceid FROM hourly_measures_by_source";
   
   private static final String SELECT_DAILY_MEASURES_PROD_SYNC_BY_SOURCE = "SELECT time, dhus_inst, failure, success, volume, "
         + "productType, platformSerialId, platformShortName, syncid, sourceid FROM daily_measures_by_source";
   
   private static final String SELECT_MONTHLY_MEASURES_PROD_SYNC_BY_SOURCE = "SELECT time, dhus_inst, failure, success, volume, "
         + "productType, platformSerialId, platformShortName, syncid, sourceid FROM monthly_measures_by_source";
   
   private static final String SELECT_PROD_SYNC_TIMER = "SELECT * from prod_sync_timer";
   
   private static final String SELECT_PROD_SYNC_QUEUED_DOWNLOADS_BY_SYNC = "SELECT * from prod_sync_queued_downloads";
   
   private static final String SELECT_PROD_SYNC_QUEUED_DOWNLOADS = "SELECT * from prod_sync_total_queued_downloads";
   
   private static final String SELECT_PROD_SYNC_TIMELINESS_CREATION_BY_SYNC = "SELECT * from prod_sync_timeliness_creation_by_sync";
   
   private static final String SELECT_PROD_SYNC_TIMELINESS_INGESTION_BY_SYNC = "SELECT * from prod_sync_timeliness_ingestion_by_sync";
   
   private static final String SELECT_DAILY_PROD_SYNC_TIMERS_BY_SOURCE = "SELECT * FROM daily_timers_by_source ";
   
   private static final String SELECT_HOURLY_PROD_SYNC_TIMERS_BY_SOURCE = "SELECT * FROM hourly_timers_by_source ";
   
   private static final String SELECT_MONTHLY_PROD_SYNC_TIMERS_BY_SOURCE = "SELECT * FROM monthly_timers_by_source ";
   
   private Integer nbMetrics;
   
   private Instant lastupdateNbMetrics;
   
   public InfluxDAO(InfluxDB client)
   {
      Objects.requireNonNull(client,"InfluxDB client cannot be null");
      this.client = client;
   }
   
   public List<InfluxMeasurement> getAllMetrics(ExpressionVisitor<Object> sqlVisitor)
   {
      if (sqlVisitor == null || sqlVisitor instanceof MetricSQLVisitor)
      {
         List<InfluxMeasurement> list = new ArrayList<InfluxMeasurement>();
         list.addAll(getProdSyncCounters((InfluxMetricsVisitor)sqlVisitor));
         list.addAll(getProdSyncTimer((InfluxMetricsVisitor)sqlVisitor));
         list.addAll(getProdSyncQueuedDownloads((InfluxMetricsVisitor)sqlVisitor));
         list.addAll(getProdSyncQueuedDownloadsBySync((InfluxMetricsVisitor)sqlVisitor));
         list.addAll(getProdSyncTimelinessBySync((InfluxMetricsVisitor)sqlVisitor));
         list.addAll(getProdSyncTimersBySource((InfluxMetricsVisitor)sqlVisitor));
         nbMetrics = list.size();
         lastupdateNbMetrics = Instant.now();

         return list;
      }
      return null;
   }
   
   private List<InfluxMeasurement> getProdSyncTimersBySource(InfluxMetricsVisitor sqlVisitor)
   {
      List<InfluxMeasurement> list = new ArrayList<InfluxMeasurement>();
      list.addAll(getProdSyncTimersByProductBySync(SELECT_DAILY_PROD_SYNC_TIMERS_BY_SOURCE, "daily", sqlVisitor));
      list.addAll(getProdSyncTimersByProductBySync(SELECT_MONTHLY_PROD_SYNC_TIMERS_BY_SOURCE, "monthly", sqlVisitor));
      list.addAll(getProdSyncTimersByProductBySync(SELECT_HOURLY_PROD_SYNC_TIMERS_BY_SOURCE, "hourly", sqlVisitor));
      return list;
   }
   
   private List<InfluxMeasurement> getProdSyncCounters(InfluxMetricsVisitor visitor)
   {
      List<InfluxMeasurement> list = new ArrayList<InfluxMeasurement>();
      list.addAll(getProdSyncCountersByProduct(SELECT_HOURLY_MEASURES_PROD_SYNC, "hourly", visitor));
      list.addAll(getProdSyncCountersByProduct(SELECT_DAILY_MEASURES_PROD_SYNC, "daily", visitor));
      list.addAll(getProdSyncCountersByProduct(SELECT_MONTHLY_MEASURES_PROD_SYNC, "monthly", visitor));
      list.addAll(getProdSyncCountersByProductBySync(SELECT_HOURLY_MEASURES_PROD_SYNC_BY_SOURCE, "hourly", visitor));
      list.addAll(getProdSyncCountersByProductBySync(SELECT_DAILY_MEASURES_PROD_SYNC_BY_SOURCE, "daily", visitor));
      list.addAll(getProdSyncCountersByProductBySync(SELECT_MONTHLY_MEASURES_PROD_SYNC_BY_SOURCE, "monthly", visitor));
      return list;
   }

   private <T> List<T> executeRequest(String query, final Class<T> clazz)
   {
      final BlockingQueue<QueryResult> queue = new LinkedBlockingQueue<>();
      client.query(new Query(query), 10, queue::add);
      try
      {
         QueryResult result = queue.poll(10, TimeUnit.SECONDS);
         if (result.hasError() && !"DONE".equalsIgnoreCase(result.getError()))
         {
            LOGGER.warn("Cannot access influxDB measurement due to " + result.getError());
         }
         else
         {
            InfluxDBResultMapper resultMapper = new InfluxDBResultMapper(); // thread-safe - can be reused
            return resultMapper.toPOJO(result, clazz);
         }
      }
      catch (InterruptedException e)
      {
         LOGGER.error("An exception occured ", e);
      }
      return null;
   }
   
   private List<InfluxMeasurement> getProdSyncCountersByProduct(String query, String aggregationType, InfluxMetricsVisitor visitor)
   {
      List<ProdSyncCounterPOJO> measures = new ArrayList<ProdSyncCounterPOJO>();
      query = completeRequest(query, visitor);
      if ("hourly".equalsIgnoreCase(aggregationType))
       {
         measures.addAll(executeRequest(query, ProdSyncHourlyCounterPOJO.class));
       }
      else if ("daily".equalsIgnoreCase(aggregationType))
      {
         measures.addAll(executeRequest(query,ProdSyncDailyCounterPOJO.class));
      }
      else if ("monthly".equalsIgnoreCase(aggregationType))
      {
         measures.addAll(executeRequest(query,ProdSyncMonthlyCounterPOJO.class));
      }
      List<InfluxMeasurement> list = new ArrayList<InfluxMeasurement>();
      if (measures != null)
       {
          //TODO: Check for asynchronous reading of the list
          measures.forEach(measure -> {
             buildAProdSyncCounterInfluxMeasurement(measure, aggregationType, list);
          });
       }
      return list;
   }

   private String completeRequest(String query, InfluxMetricsVisitor visitor)
   {
      //Build where clause of the query
      if (visitor != null)
      {
         String filter = visitor.getSqlFilter();
         if (filter != null)
         {
            StringBuilder sb = new StringBuilder(query);
            sb.append(" WHERE ").append(filter);
            LOGGER.info("The request with filter {}", sb.toString());
            return sb.toString();
         }
      }
      return query;
   }
   
   // aggregationType can be hourly, daily, monthly
   private void buildAProdSyncCounterInfluxMeasurement(ProdSyncCounterPOJO value, 
         String aggregationType, List<InfluxMeasurement> list)
   {
      String namePrefix = "prod_sync.global.counters." + aggregationType + "."
      + value.getProductType() + "." + value.getPlatformShortName() + "." 
            + value.getPlatformSerialId();
      
      InfluxMeasurement measure = new InfluxMeasurement();
      measure.setName(namePrefix+ ".failure");
      //Value failure
      measure.setCount(value.getFailure());
      measure.setType(MetricTypes.COUNTER);
      measure.setDate(Timestamp.from(value.getTime()));
      
      list.add(measure);
      
      measure = new InfluxMeasurement();
      measure.setName(namePrefix+ ".success");
      //Value failure
      measure.setCount(value.getSuccess());
      measure.setType(MetricTypes.COUNTER);
      measure.setDate(Timestamp.from(value.getTime()));
      
      list.add(measure);
      
      measure = new InfluxMeasurement();
      measure.setName(namePrefix+ ".volume");
      //Value failure
      measure.setCount(value.getVolume());
      measure.setType(MetricTypes.COUNTER);
      measure.setDate(Timestamp.from(value.getTime()));
      list.add(measure);
      
      return ;
   }
   
   private List<InfluxMeasurement> getProdSyncCountersByProductBySync(String query, String aggregationType, InfluxMetricsVisitor visitor)
   {
      List<ProdSyncCounterBySourcePOJO> measures = new ArrayList<ProdSyncCounterBySourcePOJO>();
      //Build where clause of the query
      query = completeRequest(query, visitor);
      if ("hourly".equalsIgnoreCase(aggregationType))
       {
         measures.addAll(executeRequest(query, ProdSyncHourlyCounterBySourcePOJO.class));
       }
      else if ("daily".equalsIgnoreCase(aggregationType))
      {
         measures.addAll(executeRequest(query,ProdSyncDailyCounterBySourcePOJO.class));
      }
      else if ("monthly".equalsIgnoreCase(aggregationType))
      {
         measures.addAll(executeRequest(query,ProdSyncMonthlyCounterBySourcePOJO.class));
      }
      List<InfluxMeasurement> list = new ArrayList<InfluxMeasurement>();
      if (measures != null)
       {
          //TODO: Check for asynchronous reading of the list
          measures.forEach(measure -> {
             buildAProdSyncCounterBySourceInfluxMeasurement(measure, aggregationType, list);
          });
       }
      return list;
   }
   
// aggregationType can be hourly, daily, monthly
   private void buildAProdSyncCounterBySourceInfluxMeasurement(ProdSyncCounterBySourcePOJO value, 
         String aggregationType, List<InfluxMeasurement> list)
   {
      StringBuilder sb = new StringBuilder("prod_sync.sync");
      sb.append(value.getSyncId()).append(".source");
      if (value.getSourceId() != null)
      {
         sb.append(value.getSourceId());
      }
      sb.append(".counters.").append(aggregationType).append(".")
      .append(value.getProductType()).append(".").append(value.getPlatformShortName())
      .append(".").append(value.getPlatformSerialId());
      String namePrefix = sb.toString();
      
      InfluxMeasurement measure = new InfluxMeasurement();
      measure.setName(namePrefix+ ".failure");
      //Value failure
      measure.setCount(value.getFailure());
      measure.setType(MetricTypes.COUNTER);
      measure.setDate(Timestamp.from(value.getTime()));
      
      list.add(measure);
      
      measure = new InfluxMeasurement();
      measure.setName(namePrefix+ ".success");
      //Value failure
      measure.setCount(value.getSuccess());
      measure.setType(MetricTypes.COUNTER);
      measure.setDate(Timestamp.from(value.getTime()));
      
      list.add(measure);
      
      measure = new InfluxMeasurement();
      measure.setName(namePrefix+ ".volume");
      //Value failure
      measure.setCount(value.getVolume());
      measure.setType(MetricTypes.COUNTER);
      measure.setDate(Timestamp.from(value.getTime()));
      list.add(measure);
      
      return ;
   }
   
   private List<InfluxMeasurement> getProdSyncTimer(InfluxMetricsVisitor sqlVisitor)
   {
      String query = completeRequest(SELECT_PROD_SYNC_TIMER, sqlVisitor);
      List<ProdSyncTimerPOJO> measures = executeRequest(query, ProdSyncTimerPOJO.class);
      List<InfluxMeasurement> list = new ArrayList<InfluxMeasurement>();
      if (measures != null)
      {
         // TODO: Check for asynchronous reading of the list
         measures.forEach(measure -> {
            InfluxMeasurement influxMeasurement = buildAProdSyncTimerInfluxMeasurement(measure);
            list.add(influxMeasurement);
         });
      }
      return list;
   }

   private InfluxMeasurement buildAProdSyncTimerInfluxMeasurement(ProdSyncTimerPOJO measure)
   {
      String name = "prod_sync.sync" + measure.getSyncId() + ".timer";

      InfluxMeasurement res = new InfluxMeasurement();
      res.setName(name);
      // Value failure
      res.setCount(measure.getCount());
      res.setType(MetricTypes.TIMER);
      res.setDate(Timestamp.from(measure.getTime()));
      res.set_75thPercentile(measure.get_75percentile());
      res.set_95thPercentile(measure.get_95percentile());
      res.set_99thPercentile(measure.get_99percentile());
      res.set_999thPercentile(measure.get_99percentile());
      res.setMedian(measure.get_50percentile());
      res.setFiveMinuteRate(measure.getFiveMinute());
      res.setOneMinuteRate(measure.getOneMinute());
      res.setFifteenMinuteRate(measure.getFifteenMinute());
      res.setMean(measure.getMean());
      res.setStandardDeviation(measure.getStdDev());
      res.setMeanRate(measure.getMeanMinute());
      res.setMax(measure.getMax());
      res.setMin(measure.getMin());
      
      return res;
   }
   
   private List<InfluxMeasurement> getProdSyncTimersByProductBySync(String query, String aggregationType, InfluxMetricsVisitor sqlVisitor)
   {
      List<ProdSyncTimerBySourcePOJO> measures = new ArrayList<ProdSyncTimerBySourcePOJO>();
      query = completeRequest(query, sqlVisitor);
      if ("hourly".equalsIgnoreCase(aggregationType))
       {
         measures.addAll(executeRequest(query, ProdSyncHourlyTimersBySourcePOJO.class));
       }
      else if ("daily".equalsIgnoreCase(aggregationType))
      {
         measures.addAll(executeRequest(query,ProdSyncDailyTimersBySourcePOJO.class));
      }
      else if ("monthly".equalsIgnoreCase(aggregationType))
      {
         measures.addAll(executeRequest(query,ProdSyncMonthlyTimersBySourcePOJO.class));
      }
      List<InfluxMeasurement> list = new ArrayList<InfluxMeasurement>();
      if (measures != null)
       {
          //TODO: Check for asynchronous reading of the list
          measures.forEach(measure -> {
             buildAProdSyncTimerBySourceInfluxMeasurement(measure, aggregationType, list);
          });
       }
      return list;
   }
   
   private void buildAProdSyncTimerBySourceInfluxMeasurement(ProdSyncTimerBySourcePOJO measure, String aggregationType,
         List<InfluxMeasurement> list)
   {
      StringBuilder sb = new StringBuilder("prod_sync.sync")
            .append(measure.getSyncId() != null ? measure.getSyncId() : "").append(".source");
      if (measure.getSourceId() != null)
      {
         sb.append(measure.getSourceId());
      }
      sb.append(".timers.").append(aggregationType);
      if (measure.getProductType() != null)
      {
         sb.append(".").append(measure.getProductType());
      }
      if (measure.getPlatformShortName() != null)
      {
         sb.append(".").append(measure.getPlatformShortName());
      }
      if (measure.getPlatformSerialId() != null)
      {
         sb.append(".").append(measure.getPlatformSerialId());
      }
      sb.append(".transferRate");

      InfluxMeasurement res = new InfluxMeasurement();
      res.setName(sb.toString());
      // Value failure
      res.setCount(measure.getCount());
      res.setType(MetricTypes.TIMER);
      res.setDate(Timestamp.from(measure.getTime()));
      res.set_75thPercentile(measure.get_75percentile());
      res.set_95thPercentile(measure.get_95percentile());
      res.set_99thPercentile(measure.get_99percentile());
      res.set_999thPercentile(measure.get_99percentile());
      res.setMedian(measure.get_50percentile());
      res.setFiveMinuteRate(measure.getFiveMinute());
      res.setOneMinuteRate(measure.getOneMinute());
      res.setFifteenMinuteRate(measure.getFifteenMinute());
      res.setMean(measure.getMean());
      res.setStandardDeviation(measure.getStdDev());
      res.setMeanRate(measure.getMeanMinute());
      res.setMax(measure.getMax());
      res.setMin(measure.getMin());

      list.add(res);
   }

   private List<InfluxMeasurement> getProdSyncQueuedDownloadsBySync(InfluxMetricsVisitor sqlVisitor)
   {
      String query = completeRequest(SELECT_PROD_SYNC_QUEUED_DOWNLOADS_BY_SYNC, sqlVisitor);
      List<ProdSyncQueuedDownloadsPOJO> measures = executeRequest(query, ProdSyncQueuedDownloadsPOJO.class);
      List<InfluxMeasurement> list = new ArrayList<InfluxMeasurement>();
      
      if (measures != null)
      {
         // TODO: Check for asynchronous reading of the list
         measures.forEach(measure -> {
            InfluxMeasurement influxMeasurement = buildInfluxMeasurementFromPOJO(measure);
            list.add(influxMeasurement);
         });
      }
      return list;
   }

   private InfluxMeasurement buildInfluxMeasurementFromPOJO(ProdSyncQueuedDownloadsPOJO measure)
   {
      String name = "prod_sync.sync" + measure.getSyncId() + ".gauges.queued_downloads";

      InfluxMeasurement res = new InfluxMeasurement();
      res.setName(name);
      // Value failure
      res.setValue(measure.getQueuedDownloads().toString());
      res.setType(MetricTypes.GAUGE);
      res.setDate(Timestamp.from(measure.getTime()));
      return res;
   }
   
   private List<InfluxMeasurement> getProdSyncQueuedDownloads(InfluxMetricsVisitor sqlVisitor)
   {
      String query = completeRequest(SELECT_PROD_SYNC_QUEUED_DOWNLOADS, sqlVisitor);
      List<ProdSyncGlobalQueuedDownloadsPOJO> measures = executeRequest(query, ProdSyncGlobalQueuedDownloadsPOJO.class);
      List<InfluxMeasurement> list = new ArrayList<InfluxMeasurement>();
      
      if (measures != null)
      {
         // TODO: Check for asynchronous reading of the list
         measures.forEach(measure -> {
            InfluxMeasurement influxMeasurement = buildInfluxMeasurementFromPOJO(measure);
            list.add(influxMeasurement);
         });
      }
      return list;
   }

   private InfluxMeasurement buildInfluxMeasurementFromPOJO(ProdSyncGlobalQueuedDownloadsPOJO measure)
   {
      String name = "prod_sync.global.gauges.queued_downloads";

      InfluxMeasurement res = new InfluxMeasurement();
      res.setName(name);
      // Value failure
      res.setValue(measure.getQueuedDownloads().toString());
      res.setType(MetricTypes.GAUGE);
      res.setDate(Timestamp.from(measure.getTime()));
      return res;
   }
   
   private List<InfluxMeasurement> getProdSyncTimelinessBySync(InfluxMetricsVisitor sqlVisitor)
   {
      List<InfluxMeasurement> list = new ArrayList<InfluxMeasurement>();
      list.addAll(getProdSyncTimeliness(sqlVisitor));
      return list;
   }

   private Collection<? extends InfluxMeasurement> getProdSyncTimeliness(InfluxMetricsVisitor sqlVisitor)
   {
      String query = completeRequest(SELECT_PROD_SYNC_TIMELINESS_CREATION_BY_SYNC, sqlVisitor);
      List<ProdSyncTimelinessPOJO> measures = new ArrayList<ProdSyncTimelinessPOJO>();
      measures.addAll(executeRequest(query, ProdSyncTimelinessCreationPOJO.class));
      
      query = completeRequest(SELECT_PROD_SYNC_TIMELINESS_INGESTION_BY_SYNC, sqlVisitor);
      measures.addAll(executeRequest(query, ProdSyncTimelinessIngestionPOJO.class));
      List<InfluxMeasurement> list = new ArrayList<InfluxMeasurement>();
      if (measures != null)
      {
         // TODO: Check for asynchronous reading of the list
         measures.forEach(measure -> {
            InfluxMeasurement influxMeasurement = buildAProdSyncTimelinessInfluxMeasurement(measure);
            list.add(influxMeasurement);
         });
      }
      return list;
   }

   private InfluxMeasurement buildAProdSyncTimelinessInfluxMeasurement(ProdSyncTimelinessPOJO measure)
   {
      StringBuilder sb = new StringBuilder("prod_sync.sync").append(measure.getSyncId())
            .append(".source");
      if (measure.getSourceId() != null)
      {
         sb.append(measure.getSourceId());
      }
      sb.append(".timeliness.");
      if (measure instanceof ProdSyncTimelinessCreationPOJO)
      {
         sb.append("creation");
      }
      else
      {
         sb.append("ingestion");
      }

      InfluxMeasurement res = new InfluxMeasurement();
      res.setName(sb.toString());
      // Value failure
      res.setType(MetricTypes.HISTOGRAM);
      res.setDate(Timestamp.from(measure.getTime()));
      res.set_75thPercentile(measure.get_75percentile());
      res.set_95thPercentile(measure.get_95percentile());
      res.set_99thPercentile(measure.get_99percentile());
      res.set_999thPercentile(measure.get_99percentile());
      res.setMedian(measure.get_50percentile());
      res.setMean(measure.getMean());
      res.setStandardDeviation(measure.getStdDev());
      res.setMax(measure.getMax());
      res.setMin(measure.getMin());
      
      return res;
   }

   public int countMetrics(ExpressionVisitor<Object> sqlVisitor)
   {
      if (sqlVisitor == null || sqlVisitor instanceof InfluxMetricsVisitor)
      {
         // If we update nbMetrics more than 1 min before
         if (nbMetrics == null
               || (lastupdateNbMetrics != null && lastupdateNbMetrics.plusMillis(1000).isBefore(Instant.now())))
         {
            List<InfluxMeasurement> metrics = getAllMetrics(sqlVisitor);
            if (metrics != null)
            {
               nbMetrics = metrics.size();
            }
            else
            {
               nbMetrics = 0;
            }
            lastupdateNbMetrics = Instant.now();
         }
      }
      else
      {
         LOGGER.warn("Could not check  metrics");
         return 0;
      }
      return nbMetrics;
   }

   public Long getProdSyncSuccessCounter(Long syncId, Long sourceId, Timestamp time)
   {
      Objects.requireNonNull(syncId,"SyncId could not be null");
      Objects.requireNonNull(sourceId,"SourceId could not be null");
      Instant endTime = Instant.now();
      if (time == null)
      {
         //If not time provided, choose the last minute
         time = Timestamp.from(endTime.minusMillis(1000*60));
      }
      Instant startTime = time.toInstant();
      
      StringBuilder sb = new StringBuilder("SELECT * FROM prod_sync_counters_by_sync WHERE sourceid = ");
      sb.append(sourceId).append(" AND syncid = ").append(syncId).append(" AND time >= '").append(startTime)
      .append("' AND time <= '").append(endTime).append("'");
      LOGGER.debug("Influx query: {}", sb.toString());
      
      List<ProdSyncCounterBySourcePOJO> results = executeRequest(sb.toString(), ProdSyncCounterBySourcePOJO.class);
      if (results != null && results.size() > 0)
      {
         return results.get(0).getSuccess();
      }
      return null;
   }

   public Long getProdSyncFailureCounter(Long syncId, Long sourceId, Timestamp time)
   {
      Objects.requireNonNull(syncId,"SyncId could not be null");
      Objects.requireNonNull(sourceId,"SourceId could not be null");
      Instant endTime = Instant.now();
      if (time == null)
      {
         //If not time provided, choose the last minute
         time = Timestamp.from(endTime.minusMillis(1000*60));
      }
      Instant startTime = time.toInstant();
      
      StringBuilder sb = new StringBuilder("SELECT * FROM prod_sync_counters_by_sync WHERE sourceid = ");
      sb.append(sourceId).append(" AND syncid = ").append(syncId).append(" AND time >= '").append(startTime)
      .append("' AND time <= '").append(endTime).append("'");
      LOGGER.debug("Influx query: {}", sb.toString());
      
      List<ProdSyncCounterBySourcePOJO> results = executeRequest(sb.toString(), ProdSyncCounterBySourcePOJO.class);
      if (results != null && results.size() > 0)
      {
         return results.get(0).getFailure();
      }
      return null;
   }

   public Double getProdSyncFiveMinutesRateTimer(Long syncId, Long sourceId, Timestamp time)
   {
      Objects.requireNonNull(syncId,"SyncId could not be null");
      Objects.requireNonNull(sourceId,"SourceId could not be null");
      Instant endTime = Instant.now();
      if (time == null)
      {
         //If not time provided, choose the last minute
         time = Timestamp.from(endTime.minusMillis(1000*60));
      }
      Instant startTime = time.toInstant();
      
      StringBuilder sb = new StringBuilder("SELECT * FROM prod_sync_timers_by_sync WHERE sourceid = ");
      sb.append(sourceId).append(" AND syncid = ").append(syncId).append(" AND time >= '").append(startTime)
      .append("' AND time <= '").append(endTime).append("'");
      LOGGER.debug("Influx query: {}", sb.toString());
      List<ProdSyncTimerBySourcePOJO> results = executeRequest(sb.toString(), ProdSyncTimerBySourcePOJO.class);
      
      if (results != null && results.size() > 0)
      {
         return results.get(0).getFiveMinute();
      }
      
      return null;
   }
}
