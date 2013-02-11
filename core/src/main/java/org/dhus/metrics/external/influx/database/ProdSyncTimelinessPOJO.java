package org.dhus.metrics.external.influx.database;

import org.influxdb.annotation.Column;

public class ProdSyncTimelinessPOJO extends InfluxTimeliness
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
