package org.dhus.metrics.external.influx;

import java.sql.Timestamp;

import org.dhus.metrics.embed.MetricTypes;

//DAO for the Influx Measurement

public class InfluxMeasurement
{

   private String name;
   
   private MetricTypes type;
   
   private Timestamp date;
   
   //For Counter
   private Long count;
   
   //For Gauge
   private String value;
   
   //For Histogram
   private Double max;
   
   private Double min;
   
   private Double mean;
   
   private Double median;
   
   private Double standardDeviation;
   
   private Double _75thPercentile;
   
   private Double _95thPercentile;
   
   private Double _98thPercentile;
   
   private Double _99thPercentile;
   
   private Double _999thPercentile;
   
   //For Meter
   private Double meanRate;
   
   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public MetricTypes getType()
   {
      return type;
   }

   public void setType(MetricTypes type)
   {
      this.type = type;
   }

   public Timestamp getDate()
   {
      return date;
   }

   public void setDate(Timestamp date)
   {
      this.date = date;
   }

   public Long getCount()
   {
      return count;
   }

   public void setCount(Long count)
   {
      this.count = count;
   }

   public String getValue()
   {
      return value;
   }

   public void setValue(String value)
   {
      this.value = value;
   }

   public Double getMax()
   {
      return max;
   }

   public void setMax(Double max)
   {
      this.max = max;
   }

   public Double getMin()
   {
      return min;
   }

   public void setMin(Double min)
   {
      this.min = min;
   }

   public Double getMean()
   {
      return mean;
   }

   public void setMean(Double mean)
   {
      this.mean = mean;
   }

   public Double getMedian()
   {
      return median;
   }

   public void setMedian(Double median)
   {
      this.median = median;
   }

   public Double getStandardDeviation()
   {
      return standardDeviation;
   }

   public void setStandardDeviation(Double standardDeviation)
   {
      this.standardDeviation = standardDeviation;
   }

   public Double get_75thPercentile()
   {
      return _75thPercentile;
   }

   public void set_75thPercentile(Double _75thPercentile)
   {
      this._75thPercentile = _75thPercentile;
   }

   public Double get_95thPercentile()
   {
      return _95thPercentile;
   }

   public void set_95thPercentile(Double _95thPercentile)
   {
      this._95thPercentile = _95thPercentile;
   }

   public Double get_98thPercentile()
   {
      return _98thPercentile;
   }

   public void set_98thPercentile(Double _98thPercentile)
   {
      this._98thPercentile = _98thPercentile;
   }

   public Double get_99thPercentile()
   {
      return _99thPercentile;
   }

   public void set_99thPercentile(Double _99thPercentile)
   {
      this._99thPercentile = _99thPercentile;
   }

   public Double get_999thPercentile()
   {
      return _999thPercentile;
   }

   public void set_999thPercentile(Double _999thPercentile)
   {
      this._999thPercentile = _999thPercentile;
   }

   public Double getMeanRate()
   {
      return meanRate;
   }

   public void setMeanRate(Double meanRate)
   {
      this.meanRate = meanRate;
   }

   public Double getOneMinuteRate()
   {
      return oneMinuteRate;
   }

   public void setOneMinuteRate(Double oneMinuteRate)
   {
      this.oneMinuteRate = oneMinuteRate;
   }

   public Double getFiveMinuteRate()
   {
      return fiveMinuteRate;
   }

   public void setFiveMinuteRate(Double fiveMinuteRate)
   {
      this.fiveMinuteRate = fiveMinuteRate;
   }

   public Double getFifteenMinuteRate()
   {
      return fifteenMinuteRate;
   }

   public void setFifteenMinuteRate(Double fifteenMinuteRate)
   {
      this.fifteenMinuteRate = fifteenMinuteRate;
   }

   private Double oneMinuteRate;
   
   private Double fiveMinuteRate;
   
   private Double fifteenMinuteRate;
   
   //For Timer use Histogram + Meter
}
