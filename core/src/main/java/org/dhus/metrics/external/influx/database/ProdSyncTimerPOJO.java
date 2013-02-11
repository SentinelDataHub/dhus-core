package org.dhus.metrics.external.influx.database;

import java.time.Instant;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;
import org.influxdb.annotation.TimeColumn;

@Measurement(name="prod_sync_timer")
public class ProdSyncTimerPOJO
{
   @TimeColumn
   @Column(name = "time")
   private Instant time;
   
   @Column(name = "syncid")
   private String syncId;
   
   @Column(name = "50-percentile")
   private Double _50percentile;
   
   @Column(name = "75-percentile")
   private Double _75percentile;
   
   @Column(name = "95-percentile")
   private Double _95percentile;
   
   @Column(name = "99-percentile")
   private Double _99percentile;
   
   @Column(name = "999-percentile")
   private Double _999percentile;
   
   @Column(name = "count")
   private Long count;
   
   @Column(name = "dhus_inst")
   private String dhusInst;
   
   @Column(name = "fifteen-minute")
   private Double fifteenMinute;
   
   @Column(name = "five-minute")
   private Double fiveMinute;
   
   @Column(name = "max")
   private Double max;
   
   @Column(name = "mean")
   private Double mean;
   
   @Column(name = "mean-minute")
   private Double meanMinute;
   
   @Column(name = "min")
   private Double min;
   
   @Column(name = "one-minute")
   private Double oneMinute;
   
   @Column(name = "run-count")
   private Long runCount;
   
   @Column(name = "std-dev")
   private Double stdDev;

   public Instant getTime()
   {
      return time;
   }

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

   public Double get_50percentile()
   {
      return _50percentile;
   }

   public void set_50percentile(Double _50percentile)
   {
      this._50percentile = _50percentile;
   }

   public Double get_75percentile()
   {
      return _75percentile;
   }

   public void set_75percentile(Double _75percentile)
   {
      this._75percentile = _75percentile;
   }

   public Double get_95percentile()
   {
      return _95percentile;
   }

   public void set_95percentile(Double _95percentile)
   {
      this._95percentile = _95percentile;
   }

   public Double get_99percentile()
   {
      return _99percentile;
   }

   public void set_99percentile(Double _99percentile)
   {
      this._99percentile = _99percentile;
   }

   public Double get_999percentile()
   {
      return _999percentile;
   }

   public void set_999percentile(Double _999percentile)
   {
      this._999percentile = _999percentile;
   }

   public Long getCount()
   {
      return count;
   }

   public void setCount(Long count)
   {
      this.count = count;
   }

   public String getDhusInst()
   {
      return dhusInst;
   }

   public void setDhusInst(String dhusInst)
   {
      this.dhusInst = dhusInst;
   }

   public Double getFifteenMinute()
   {
      return fifteenMinute;
   }

   public void setFifteenMinute(Double fifteenMinute)
   {
      this.fifteenMinute = fifteenMinute;
   }

   public Double getFiveMinute()
   {
      return fiveMinute;
   }

   public void setFiveMinute(Double fiveMinute)
   {
      this.fiveMinute = fiveMinute;
   }

   public Double getMax()
   {
      return max;
   }

   public void setMax(Double max)
   {
      this.max = max;
   }

   public Double getMean()
   {
      return mean;
   }

   public void setMean(Double mean)
   {
      this.mean = mean;
   }

   public Double getMeanMinute()
   {
      return meanMinute;
   }

   public void setMeanMinute(Double meanMinute)
   {
      this.meanMinute = meanMinute;
   }

   public Double getMin()
   {
      return min;
   }

   public void setMin(Double min)
   {
      this.min = min;
   }

   public Double getOneMinute()
   {
      return oneMinute;
   }

   public void setOneMinute(Double oneMinute)
   {
      this.oneMinute = oneMinute;
   }

   public Long getRunCount()
   {
      return runCount;
   }

   public void setRunCount(Long runCount)
   {
      this.runCount = runCount;
   }

   public Double getStdDev()
   {
      return stdDev;
   }

   public void setStdDev(Double stdDev)
   {
      this.stdDev = stdDev;
   }
   
   
}
