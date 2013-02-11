package org.dhus.metrics.external.influx.database;

import java.time.Instant;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.TimeColumn;

//For Histogram
public class InfluxTimeliness implements InfluxPOJOInterface
{
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
   
   @Column(name = "mean")
   private Double mean;
   
   @Column(name = "min")
   private Double min;
   
   @Column(name = "max")
   private Double max;
   
   @Column(name = "std-dev")
   private Double stdDev;
   
   @Column(name = "run-count")
   private Long runCount;
   
   @TimeColumn
   @Column(name = "time")
   private Instant time;
   
   @Column(name = "dhus_inst", tag = true)
   private String dhusInst;

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

   public Double getMean()
   {
      return mean;
   }

   public void setMean(Double mean)
   {
      this.mean = mean;
   }

   public Double getMin()
   {
      return min;
   }

   public void setMin(Double min)
   {
      this.min = min;
   }

   public Double getMax()
   {
      return max;
   }

   public void setMax(Double max)
   {
      this.max = max;
   }

   public Double getStdDev()
   {
      return stdDev;
   }

   public void setStdDev(Double standardDev)
   {
      this.stdDev = standardDev;
   }
   
}
