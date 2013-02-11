package org.dhus.metrics.external.influx.database;

import java.time.Instant;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.TimeColumn;

public class InfluxCounter implements InfluxPOJOInterface
{
   @Column(name = "failure")
   private Long failure;
   
   @Column(name = "success")
   private Long success;
   
   @Column(name = "volume")
   private Long volume;
   
   @TimeColumn
   @Column(name = "time")
   private Instant time;
   
   @Column(name = "dhus_inst", tag = true)
   private String dhusInst;

   public Long getFailure()
   {
      return failure;
   }
   
   public void setFailure(Long failure)
   {
      this.failure = failure;
   }

   public Long getSuccess()
   {
      return success;
   }
   
   public void setSuccess(Long success)
   {
      this.success = success;
   }
   
   public Long getVolume()
   {
      return volume;
   }

   public void setVolume(Long volume)
   {
      this.volume = volume;
   }

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
}
