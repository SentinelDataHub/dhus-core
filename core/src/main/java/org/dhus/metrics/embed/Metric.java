/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019 GAEL Systems
 *
 * This file is part of DHuS software sources.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.dhus.metrics.embed;

import com.codahale.metrics.Counting;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Snapshot;

import java.beans.ConstructorProperties;
import java.sql.Timestamp;
import java.util.Date;

import org.jdbi.v3.core.mapper.Nested;

/**
 * Mapped Object.
 *
 * @see MetricsDao
 * @see MetricsService
 */
public final class Metric
{
   private final Timestamp date;
   private final String name;
   private final MetricTypes type;
   private final Counter counter;
   private final Gauge gauge;
   private final Histogram histogram;
   private final Meter meter;

   @ConstructorProperties(
         {
            "date", "name", "type",
            "count",
            "value",
            "max", "min", "mean", "median", "std_dev", "h_75thpercentile", "h_95thpercentile", "h_98thpercentile", "h_99thpercentile", "h_999thpercentile",
            "mean_rate", "m_1m_rate", "m_5m_rate", "m_15m_rate"
         }
   )
   public Metric(Timestamp date, String name, MetricTypes type,
         Long count,
         String value,
         Long max, Long min, Double mean, Double median, Double standardDeviation, Double _75thPercentile, Double _95thPercentile, Double _98thPercentile, Double _99thPercentile, Double _999thPercentile,
         Double meanRate, Double OneMinuteRate, Double FiveMinuteRate, Double FifteenMinuteRate)
   {
      this.date = date;
      this.name = name;
      this.type = type;
      this.counter = new Counter(count);
      this.gauge = new Gauge(value);
      this.histogram = new Histogram(max, min, mean, median, standardDeviation, _75thPercentile, _95thPercentile, _98thPercentile, _99thPercentile, _999thPercentile);
      this.meter = new Meter(meanRate, OneMinuteRate, FiveMinuteRate, FifteenMinuteRate);
   }

   public Metric(Timestamp date, String name, MetricTypes type, Counter counter, Gauge gauge, Histogram histogram, Meter meter)
   {
      this.date = date;
      this.name = name;
      this.type = type;
      this.counter =   counter == null ?   new Counter() :   counter;
      this.gauge =     gauge == null ?     new Gauge() :     gauge;
      this.histogram = histogram == null ? new Histogram() : histogram;
      this.meter =     meter == null ?     new Meter() :     meter;
   }

   public Metric(String name, com.codahale.metrics.Counter counter)
   {
      this(null, name, MetricTypes.COUNTER, new CounterFromCounting(counter), null, null, null);
   }

   public Metric(String name, com.codahale.metrics.Gauge gauge)
   {
      this(null, name, MetricTypes.GAUGE, null, new Gauge(gauge.getValue()), null, null);
   }

   public Metric(String name, com.codahale.metrics.Histogram histogram)
   {
      this(null, name, MetricTypes.HISTOGRAM, new CounterFromCounting(histogram), null, new HistogramFromSnapshot(histogram.getSnapshot()), null);
   }

   public Metric(String name, com.codahale.metrics.Meter meter)
   {
      this(null, name, MetricTypes.METER, new CounterFromCounting(meter), null, null, new MeterFromMetered(meter));
   }

   public Metric(String name, com.codahale.metrics.Timer timer)
   {
      this(null, name, MetricTypes.TIMER, new CounterFromCounting(timer), null, new HistogramFromSnapshot(timer.getSnapshot()), new MeterFromMetered(timer));
   }

   public String getName()
   {
      return name;
   }

   public long getDateAsLong()
   {
      return date.getTime();
   }

   public Date getDate()
   {
      return new Date(date.getTime());
   }

   public Timestamp getTimestamp()
   {
      return this.date;
   }

   public MetricTypes getType()
   {
      return this.type;
   }

   @Nested
   public Counter getCounter()
   {
      return counter;
   }

   @Nested
   public Gauge getGauge()
   {
      return gauge;
   }

   @Nested
   public Histogram getHistogram()
   {
      return histogram;
   }

   @Nested
   public Meter getMeter()
   {
      return meter;
   }

   @Override
   public String toString()
   {
      StringBuilder sb = new StringBuilder();
      sb.append(date == null ? "null" : date.toString());
      sb.append(String.format(" (%9s) ", type.toString()));
      sb.append(name);
      if (counter != null && counter.count != null)
      {
         sb.append(' ').append(counter.toString());
      }
      if (gauge != null && gauge.value != null)
      {
         sb.append(' ').append(gauge.toString());
      }
      if (histogram != null && histogram.max != null)
      {
         sb.append(' ').append(histogram.toString());
      }
      if (meter != null && meter.meanRate != null)
      {
         sb.append(' ').append(meter.toString());
      }
      return sb.toString();
   }

   /*
         Nested Types
   */

   public static class Counter
   {
      private final Long count;

      public Counter()
      {
         this.count = null;
      }

      public Counter(Long count)
      {
         this.count = count;
      }

      public Long getCount()
      {
         return count;
      }

      @Override
      public String toString()
      {
         return "Count=" + getCount();
      }
   }
   private static class CounterFromCounting extends Counter
   {
      private final Counting adapted;

      public CounterFromCounting(Counting adapted)
      {
         this.adapted = adapted;
      }

      @Override
      public Long getCount()
      {
         return adapted.getCount();
      }
   }

   public static class Gauge
   {
      private final String value;

      public Gauge()
      {
         this.value = null;
      }

      public Gauge(Object value)
      {
         this.value = value == null ? null : value.toString();
      }

      public Gauge(String value)
      {
         this.value = value;
      }

      public String getValue()
      {
         return value;
      }

      @Override
      public String toString()
      {
         return "Value=" + getValue();
      }
   }

   public static class Histogram
   {
      private final Long max;
      private final Long min;
      private final Double mean;
      private final Double median;
      private final Double standardDeviation;
      private final Double _75thPercentile;
      private final Double _95thPercentile;
      private final Double _98thPercentile;
      private final Double _99thPercentile;
      private final Double _999thPercentile;

      public Histogram()
      {
         this.max = null;
         this.min = null;
         this.mean = null;
         this.median = null;
         this.standardDeviation = null;
         this._75thPercentile = null;
         this._95thPercentile = null;
         this._98thPercentile = null;
         this._99thPercentile = null;
         this._999thPercentile = null;
      }

      public Histogram(Long max, Long min, Double mean, Double median, Double standardDeviation,
            Double _75thPercentile, Double _95thPercentile, Double _98thPercentile, Double _99thPercentile, Double _999thPercentile)
      {
         this.max = max;
         this.min = min;
         this.mean = mean;
         this.median = median;
         this.standardDeviation = standardDeviation;
         this._75thPercentile = _75thPercentile;
         this._95thPercentile = _95thPercentile;
         this._98thPercentile = _98thPercentile;
         this._99thPercentile = _99thPercentile;
         this._999thPercentile = _999thPercentile;
      }

      public Long getMax()
      {
         return max;
      }

      public Long getMin()
      {
         return min;
      }

      public Double getMean()
      {
         return mean;
      }

      public Double getMedian()
      {
         return median;
      }

      public Double getStandardDeviation()
      {
         return standardDeviation;
      }

      public Double get75thPercentile()
      {
         return _75thPercentile;
      }

      public Double get95thPercentile()
      {
         return _95thPercentile;
      }

      public Double get98thPercentile()
      {
         return _98thPercentile;
      }

      public Double get99thPercentile()
      {
         return _99thPercentile;
      }

      public Double get999thPercentile()
      {
         return _999thPercentile;
      }

      @Override
      public String toString()
      {
         StringBuilder sb = new StringBuilder();
         sb.append("Max=").append(getMax()).append(", ");
         sb.append("Min=").append(getMin()).append(", ");
         sb.append("Mean=").append(getMean()).append(", ");
         sb.append("Median=").append(getMedian()).append(", ");
         sb.append("StandardDeviation=").append(getStandardDeviation()).append(", ");
         sb.append("75thPercentile=").append(get75thPercentile()).append(", ");
         sb.append("95thPercentile=").append(get95thPercentile()).append(", ");
         sb.append("98thPercentile=").append(get98thPercentile()).append(", ");
         sb.append("99thPercentile=").append(get99thPercentile()).append(", ");
         sb.append("999thPercentile=").append(get999thPercentile());
         return sb.toString();
      }
   }
   private static class HistogramFromSnapshot extends Histogram
   {
      private final Snapshot adapted;

      public HistogramFromSnapshot(Snapshot adapted)
      {
         this.adapted = adapted;
      }

      @Override
      public Long getMax()
      {
         return adapted.getMax();
      }

      @Override
      public Long getMin()
      {
         return adapted.getMin();
      }

      @Override
      public Double getMean()
      {
         return adapted.getMean();
      }

      @Override
      public Double getMedian()
      {
         return adapted.getMedian();
      }

      @Override
      public Double getStandardDeviation()
      {
         return adapted.getStdDev();
      }

      @Override
      public Double get75thPercentile()
      {
         return adapted.get75thPercentile();
      }

      @Override
      public Double get95thPercentile()
      {
         return adapted.get95thPercentile();
      }

      @Override
      public Double get98thPercentile()
      {
         return adapted.get98thPercentile();
      }

      @Override
      public Double get99thPercentile()
      {
         return adapted.get99thPercentile();
      }

      @Override
      public Double get999thPercentile()
      {
         return adapted.get999thPercentile();
      }
   }

   public static class Meter
   {
      private final Double meanRate;
      private final Double oneMinuteRate;
      private final Double fiveMinuteRate;
      private final Double fifteenMinuteRate;

      public Meter()
      {
         this.meanRate = null;
         this.oneMinuteRate = null;
         this.fiveMinuteRate = null;
         this.fifteenMinuteRate = null;
      }

      @ConstructorProperties({"mean_rate", "m_1m_rate", "m_5m_rate", "m_15m_rate"})
      public Meter(Double meanRate, Double oneMinuteRate, Double fiveMinuteRate, Double fifteenMinuteRate)
      {
         this.meanRate = meanRate;
         this.oneMinuteRate = oneMinuteRate;
         this.fiveMinuteRate = fiveMinuteRate;
         this.fifteenMinuteRate = fifteenMinuteRate;
      }

      public Double getMeanRate()
      {
         return meanRate;
      }

      public Double getOneMinuteRate()
      {
         return oneMinuteRate;
      }

      public Double getFiveMinuteRate()
      {
         return fiveMinuteRate;
      }

      public Double getFifteenMinuteRate()
      {
         return fifteenMinuteRate;
      }

      @Override
      public String toString()
      {
         StringBuilder sb = new StringBuilder();
         sb.append("MeanRate=").append(getMeanRate()).append(", ");
         sb.append("OneMinuteRate=").append(getOneMinuteRate()).append(", ");
         sb.append("FiveMinuteRate=").append(getFiveMinuteRate()).append(", ");
         sb.append("FifteenMinuteRate=").append(getFifteenMinuteRate());
         return sb.toString();
      }
   }
   private static class MeterFromMetered extends Meter
   {
      private final Metered adapted;

      public MeterFromMetered(Metered adapted)
      {
         this.adapted = adapted;
      }

      @Override
      public Double getMeanRate()
      {
         return adapted.getMeanRate();
      }

      @Override
      public Double getOneMinuteRate()
      {
         return adapted.getOneMinuteRate();
      }

      @Override
      public Double getFiveMinuteRate()
      {
         return adapted.getFiveMinuteRate();
      }

      @Override
      public Double getFifteenMinuteRate()
      {
         return adapted.getFifteenMinuteRate();
      }
   }
}
