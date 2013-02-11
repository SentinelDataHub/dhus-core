package org.dhus.metrics.external.influx.database;

import java.time.Instant;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;
import org.influxdb.annotation.TimeColumn;

@Measurement(name="prod_sync_total_queued_downloads")
public class ProdSyncGlobalQueuedDownloadsPOJO implements InfluxPOJOInterface
{
   @TimeColumn
   @Column(name = "time")
   private Instant time;
   
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

   @Override
   public String getDhusInst()
   {
      return dhusInst;
   }

   @Override
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
