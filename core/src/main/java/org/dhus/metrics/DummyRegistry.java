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
package org.dhus.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentMap;

/**
 * Dummy metric registry, most methods are no-op.
 * This registry is used when monitoring is disabled.
 */
public class DummyRegistry extends MetricRegistry
{
   private final Timer timer = new Timer(new DummyReservoir());
   private final Counter counter = new Counter();
   private final Histogram histogram = new Histogram(new DummyReservoir());
   private final Meter meter = new Meter();

   @Override
   public void addListener(MetricRegistryListener listener)
   {
      // NOOP
   }

   @Override
   protected ConcurrentMap<String, Metric> buildMap()
   {
      return null;
   }

   @Override
   public Counter counter(String name)
   {
      return counter;
   }

   @Override
   public SortedMap<String, Counter> getCounters()
   {
      return null;
   }

   @Override
   public SortedMap<String, Counter> getCounters(MetricFilter filter)
   {
      return null;
   }

   @Override
   public SortedMap<String, Gauge> getGauges()
   {
      return null;
   }

   @Override
   public SortedMap<String, Gauge> getGauges(MetricFilter filter)
   {
      return null;
   }

   @Override
   public SortedMap<String, Histogram> getHistograms()
   {
      return null;
   }

   @Override
   public SortedMap<String, Histogram> getHistograms(MetricFilter filter)
   {
      return null;
   }

   @Override
   public SortedMap<String, Meter> getMeters()
   {
      return null;
   }

   @Override
   public SortedMap<String, Meter> getMeters(MetricFilter filter)
   {
      return null;
   }

   @Override
   public Map<String, Metric> getMetrics()
   {
      return null;
   }

   @Override
   public SortedSet<String> getNames()
   {
      return null;
   }

   @Override
   public SortedMap<String, Timer> getTimers()
   {
      return null;
   }

   @Override
   public SortedMap<String, Timer> getTimers(MetricFilter filter)
   {
      return null;
   }

   @Override
   public Histogram histogram(String name)
   {
      return histogram;
   }

   @Override
   public Meter meter(String name)
   {
      return meter;
   }

   @Override
   public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException
   {
      return metric;
   }

   @Override
   public void registerAll(MetricSet metrics) throws IllegalArgumentException
   {
      // NOOP
   }

   @Override
   public boolean remove(String name)
   {
      return true;
   }

   @Override
   public void removeListener(MetricRegistryListener listener)
   {
      // NOOP
   }

   @Override
   public void removeMatching(MetricFilter filter)
   {
      // NOOP
   }

   @Override
   public Timer timer(String name)
   {
      return timer;
   }

   private static class DummyReservoir implements Reservoir
   {
      @Override
      public int size()
      {
         return 0;
      }

      @Override
      public void update(long value)
      {
         // NOOP
      }

      @Override
      public Snapshot getSnapshot()
      {
         return null;
      }

   }

}
