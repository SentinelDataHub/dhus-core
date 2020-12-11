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

/**
 * Metric types.
 *
 * @see Metric
 */
public enum MetricTypes
{
   COUNTER(1, "Counter"),
   HISTOGRAM(2, "Histogram"),
   GAUGE(3, "Gauge"),
   METER(4, "Meter"),
   TIMER(5, "Timer");

   public final int type;
   public final String name;

   private MetricTypes(int type, String name)
   {
      this.type = type;
      this.name = name;
   }

   // Used by the DAO, do not remove
   public int getType()
   {
      return type;
   }

   public String getTypeAsString()
   {
      return Integer.toString(type);
   }

   @Override
   public String toString()
   {
      return this.name;
   }

   /**
    * Returns the Metric type associated with the given name.
    * Note that the name is not the one returned by the {@link #name()} method but is the member {@link #name}.
    *
    * @param name a number that matched any {@link #name} value
    * @return an instance or null
    */
   public static MetricTypes fromName(String name)
   {
      if (name == null || name.isEmpty())
      {
         return null;
      }
      if (name.equals(COUNTER.name))
      {
         return COUNTER;
      }
      if (name.equals(HISTOGRAM.name))
      {
         return HISTOGRAM;
      }
      if (name.equals(GAUGE.name))
      {
         return GAUGE;
      }
      if (name.equals(METER.name))
      {
         return METER;
      }
      if (name.equals(TIMER.name))
      {
         return TIMER;
      }
      return null;
   }

}
