package org.dhus.metrics.external;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;

public interface MetricsServiceInterface
{

   /**
    * Remove old metrics using the sliding window configuration from monitoring.xml.
    */
   public void applySlidingWindow();
   
   /**
    * Count metrics.
    *
    * @return number of metrics
    */
   public default int countMetrics()
   {
      return 0;
   }

   public default int countMetrics(FilterOption filterOption)
   {
      return 0;
   }
   
   /**
    * List metrics, filtered, ordered and limited.
    *
    * @param sqlVisitor non null Olingo SQL visitor
    * @return a list of OData entities, may be empty
    */
   public List<Entity> listMetricsAsEntities(ExpressionVisitor<Object> sqlVisitor);
   
   
   /**
    * Get a metric by its key.
    *
    * @param name of the metric
    * @param time report time
    * @return an OData Entity or {@code null}
    * @throws NullPointerException if either `name` or `time` is null
    */
   public default Entity getMetricAsEntity(String name, Timestamp time)
   {
      Objects.requireNonNull(name, "parameter `name` cannot be null");
      Objects.requireNonNull(time, "parameter `time` cannot be null");

      return getMetricFromStorage(name, time);
   }

   public Entity getMetricFromStorage(String name, Timestamp time);
   
   /**
    * List metrics.
    *
    * @return a list of Metric instances, may be empty
    */
   public List<Entity> listMetricsAsEntities();
   
   /**
    * List prod sync counter metrics
    */
   public default Long getProdSyncSuccessCountersBySync(Long syncId, Long sourceId, Timestamp time)
   {
      return null;
   }
   
   public default Long getProdSyncFailureCountersBySync(Long syncId, Long sourceId, Timestamp time)
   {
      return null;
   }
   
   public default Double getProdSyncTransferSizeFiveMinutesRateBySync(Long syncId, Long sourceId, Timestamp time)
   {
      return null;
   }

   public default Double getProdSyncSuccessFifteenMinutesRateBySync(Long syncId, Long sourceId, Timestamp time)
   {
      return null;
   }
   
   public default Double getProdSyncFailureFifteenMinutesRateBySync(Long syncId, Long sourceId, Timestamp time)
   {
      return null;
   }
   
}
