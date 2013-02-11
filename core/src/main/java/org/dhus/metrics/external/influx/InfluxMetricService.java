package org.dhus.metrics.external.influx;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.dhus.metrics.external.MetricsServiceInterface;
import org.dhus.olingo.v2.datamodel.MetricModel;
import org.dhus.olingo.v2.visitor.InfluxMetricsVisitor;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;

public class InfluxMetricService implements MetricsServiceInterface
{
   /** Log. */
   private final Logger LOGGER = LogManager.getLogger();
   
   private String url;
   
   private String username;
   
   private String password;
   
   private String database;
   
   private InfluxDB client;

//   private Set<String> tableNames;
   
   private InfluxDAO influxDao;
   
   // The database is mandarory before using metrics API
   public InfluxMetricService(String url, String username, String password, String database)
   {
      Objects.requireNonNull(url,"InfluxDB url cannot be null");
      Objects.requireNonNull(username,"InfluxDB username cannot be null");
      Objects.requireNonNull(password,"InfluxDB password cannot be null");
      Objects.requireNonNull(database,"InfluxDB database cannot be null");
      this.url = url;
      this.username = username;
      this.password = password;
      this.database = database;
      
      //Create the client
      client = InfluxDBFactory.connect(this.url, this.username, this.password);
      client.setDatabase(this.database);
      createContinuousQuery();
      
      influxDao = new InfluxDAO(client);
      
      //To close the client
      Runtime.getRuntime().addShutdownHook(new Thread(client::close));
      LOGGER.info("InfluxDB Metric Service is started");
   }
   
   //Continuous queries will be used to have hourly, daily or monthly information
   //See: https://docs.influxdata.com/influxdb/v1.8/query_language/continuous_queries/#advanced-syntax
   private void createContinuousQuery()
   {
      List<String> queries = new ArrayList<String>();
      
      queries.add(buildCreateContinuousQueryRequest(
            InfluxDAO.CREATE_HOURLY_CONTINUOUS_QUERY_PROD_SYNC_COUNTERS, "cq_hourly_prod_sync"));
      
      queries.add(buildCreateContinuousQueryRequest(
            InfluxDAO.CREATE_DAILY_CONTINUOUS_QUERY_PROD_SYNC_COUNTERS, "cq_daily_prod_sync"));
      
      queries.add(buildCreateContinuousQueryRequest(
            InfluxDAO.CREATE_MONTHLY_CONTINUOUS_QUERY_PROD_SYNC_COUNTERS, "cq_monthly_prod_sync"));
      
      queries.add(buildCreateContinuousQueryRequest(
            InfluxDAO.CREATE_HOURLY_CONTINUOUS_QUERY_PROD_SYNC_COUNTERS_BY_SOURCE, "cq_hourly_prod_sync_by_source"));
      
      queries.add(buildCreateContinuousQueryRequest(
            InfluxDAO.CREATE_DAILY_CONTINUOUS_QUERY_PROD_SYNC_COUNTERS_BY_SOURCE, "cq_daily_prod_sync_by_source"));
      
      queries.add(buildCreateContinuousQueryRequest(
            InfluxDAO.CREATE_MONTHLY_CONTINUOUS_QUERY_PROD_SYNC_COUNTERS_BY_SOURCE, "cq_monthly_prod_sync_by_source"));
      
      queries.add(buildCreateContinuousQueryRequest(
            InfluxDAO.CREATE_HOURLY_CONTINUOUS_QUERY_PROD_SYNC_TIMERS_BY_SOURCE, "cq_hourly_prod_sync_timer_by_source"));
      
      queries.add(buildCreateContinuousQueryRequest(
            InfluxDAO.CREATE_DAILY_CONTINUOUS_QUERY_PROD_SYNC_TIMERS_BY_SOURCE, "cq_daily_prod_sync_timer_by_source"));
      
      queries.add(buildCreateContinuousQueryRequest(
            InfluxDAO.CREATE_MONTHLY_CONTINUOUS_QUERY_PROD_SYNC_TIMERS_BY_SOURCE, "cq_monthly_prod_sync_timer_by_source"));
      
      queries.forEach(query ->
      {
         //Execute the request
         LOGGER.debug("Create continuous query: {}", query);
         client.query(new Query(query));
      });
      LOGGER.info("Continuous queries created");
   }
   
   private String buildCreateContinuousQueryRequest(String query, String cqName)
   {
      StringBuilder sb = new StringBuilder();
      sb.append("CREATE CONTINUOUS QUERY \"").append(cqName).append("\" ON ")
      .append(this.database).append(" BEGIN ");
      sb.append(query).append(" END");
      return sb.toString();
   }

   public InfluxMetricService(String url, String token)
   {
      this(url, token, null, null);
   }

   // The purpose is to delete metrics older than windowWidth
   @Override
   public void applySlidingWindow()
   {
     //TODO No need to apply it because InfluxDB has its own retention policy
   }

   @Override
   public int countMetrics(FilterOption filterOption)
   {
      if (filterOption != null)
      {
         InfluxMetricsVisitor visitor = new InfluxMetricsVisitor(filterOption, null, null, null);
         return influxDao.countMetrics(visitor);
      }
      return influxDao.countMetrics(null);
   }

   @Override
   public List<Entity> listMetricsAsEntities(ExpressionVisitor<Object> sqlVisitor)
   {
      return transformToEntity(influxDao.getAllMetrics(sqlVisitor));
   }

   private List<Entity> transformToEntity(List<InfluxMeasurement> measures)
   {
      List<Entity> entities = new ArrayList<Entity>();
      if (measures != null && !measures.isEmpty())
      {
         BlockingQueue<InfluxMeasurement> queue = new LinkedBlockingQueue<>();
         queue.addAll(measures);
         queue.forEach(measure -> {
            Entity entity = transformInfluxMeasurementToEntity(measure);
            entities.add(entity);
         });

      }
      return entities;
   }

   private Entity transformInfluxMeasurementToEntity(InfluxMeasurement measure)
   {
      Entity entity = new Entity();
      entity.addProperty(new Property(null, MetricModel.NAME, ValueType.PRIMITIVE, measure.getName()));
      entity.addProperty(new Property(null, MetricModel.TIMESTAMP, ValueType.PRIMITIVE, measure.getDate()));
      entity.addProperty(new Property(null, MetricModel.TYPE, ValueType.ENUM, measure.getType().type));
      entity.addProperty(new Property(null, MetricModel.COUNT, ValueType.PRIMITIVE, measure.getCount()));
      entity.addProperty(new Property(null, MetricModel.GAUGE, ValueType.PRIMITIVE, measure.getValue()));
      entity.addProperty(new Property(null, MetricModel.MINIMUM, ValueType.PRIMITIVE, measure.getMin()));
      entity.addProperty(new Property(null, MetricModel.MAXIMUM, ValueType.PRIMITIVE, measure.getMax()));
      entity.addProperty(new Property(null, MetricModel.MEAN, ValueType.PRIMITIVE, measure.getMean()));
      entity.addProperty(new Property(null, MetricModel.MEDIAN, ValueType.PRIMITIVE, measure.getMedian()));
      entity.addProperty(new Property(null, MetricModel.STANDARDDEVIATION, ValueType.PRIMITIVE, measure.getStandardDeviation()));
      entity.addProperty(new Property(null, MetricModel.SEVENTYFIFTHPERCENTILE, ValueType.PRIMITIVE, measure.get_75thPercentile()));
      entity.addProperty(new Property(null, MetricModel.NINETYFIFTHPERCENTILE, ValueType.PRIMITIVE, measure.get_95thPercentile()));
      entity.addProperty(new Property(null, MetricModel.NINETYEIGHTHPERCENTILE, ValueType.PRIMITIVE, measure.get_98thPercentile()));
      entity.addProperty(new Property(null, MetricModel.NINETYNINTHPERCENTILE, ValueType.PRIMITIVE, measure.get_99thPercentile()));
      entity.addProperty(new Property(null, MetricModel.NINETYNINTHNINEPERCENTILE, ValueType.PRIMITIVE, measure.get_999thPercentile()));
      entity.addProperty(new Property(null, MetricModel.MEANRATE, ValueType.PRIMITIVE, measure.getMeanRate()));
      entity.addProperty(new Property(null, MetricModel.ONEMINUTERATE, ValueType.PRIMITIVE, measure.getOneMinuteRate()));
      entity.addProperty(new Property(null, MetricModel.FIVEMINUTESRATE, ValueType.PRIMITIVE, measure.getFiveMinuteRate()));
      entity.addProperty(new Property(null, MetricModel.FIFTEENMINUTESRATE, ValueType.PRIMITIVE, measure.getFifteenMinuteRate()));
      return entity;
   }

   @Override
   public Entity getMetricFromStorage(String name, Timestamp time)
   {
      return null;
   }
   

   @Override
   public List<Entity> listMetricsAsEntities()
   {
      return listMetricsAsEntities(null);
   }
   
   /**
    * List prod sync counter metrics
    */
   @Override
   public Long getProdSyncSuccessCountersBySync(Long syncId, Long sourceId, Timestamp time)
   {
      return influxDao.getProdSyncSuccessCounter(syncId, sourceId, time);
   }
   
   @Override
   public Long getProdSyncFailureCountersBySync(Long syncId, Long sourceId, Timestamp time)
   {
      return influxDao.getProdSyncFailureCounter(syncId, sourceId, time);
   }
   
   @Override
   public Double getProdSyncTransferSizeFiveMinutesRateBySync(Long syncId, Long sourceId, Timestamp time)
   {
      return influxDao.getProdSyncFiveMinutesRateTimer(syncId, sourceId, time);
   }

}
