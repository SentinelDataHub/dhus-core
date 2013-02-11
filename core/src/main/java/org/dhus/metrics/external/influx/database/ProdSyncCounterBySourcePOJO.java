package org.dhus.metrics.external.influx.database;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

@Measurement(name="prod_sync_counters_by_sync")
public class ProdSyncCounterBySourcePOJO extends ProdSyncCounterPOJO
{
   @Column(name = "sourceid", tag = true)
   private String sourceId;
   
   @Column(name = "syncid", tag = true)
   private String syncId;

   public String getSourceId()
   {
      return sourceId;
   }

   public void setSourceId(String sourceId)
   {
      this.sourceId = sourceId;
   }

   public String getSyncId()
   {
      return syncId;
   }

   public void setSyncId(String syncId)
   {
      this.syncId = syncId;
   }
   
}
