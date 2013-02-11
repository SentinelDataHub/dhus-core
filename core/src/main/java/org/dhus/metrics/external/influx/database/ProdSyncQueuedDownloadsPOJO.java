package org.dhus.metrics.external.influx.database;

import java.time.Instant;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;
import org.influxdb.annotation.TimeColumn;

@Measurement(name="prod_sync_queued_downloads")
public class ProdSyncQueuedDownloadsPOJO implements InfluxPOJOInterface
{
   @TimeColumn
   @Column(name = "time")
   private Instant time;
   
   @Column(name = "syncid")
   private String syncId;
   
   @Column(name = "dhus_inst")
   private String dhusInst;
   
   @Column (name = "queued_downloads")
   private Long queuedDownloads;

   @Override
   public Instant getTime()
   {
      return time;
   }

   @Override
   public void setTime(Instant time)
   {
      this.time = time;
   }

   public String getSyncId()
   {
      return syncId;
   }

   public void setSyncId(String syncId)
   {
      this.syncId = syncId;
   }

   public String getDhusInst()
   {
      return dhusInst;
   }

   public void setDhusInst(String dhusInst)
   {
      this.dhusInst = dhusInst;
   }

   public Long getQueuedDownloads()
   {
      return queuedDownloads;
   }

   public void setQueuedDownloads(Long queuedDownloads)
   {
      this.queuedDownloads = queuedDownloads;
   }
   
   
}
