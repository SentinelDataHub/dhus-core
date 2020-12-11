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

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import java.util.Collection;
import java.util.stream.Collectors;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheCache;
import org.springframework.cache.ehcache.EhCacheCacheManager;

/**
 * Instrumented EhCache cache manager.
 * This class is a bean referenced in file dhus-core-cache.xml
 */
public final class InstrumentedEhCacheCacheManager extends EhCacheCacheManager
{
   /** Metric Registry (may be null). */
   @Autowired(required=false)
   private MetricRegistry metricRegistry;

   /**
    * Called by Spring at context creation.
    *
    * @param cacheManager must not be null
    */
   public InstrumentedEhCacheCacheManager(CacheManager cacheManager)
   {
      super(cacheManager);
   }

   @Override
   protected Collection<org.springframework.cache.Cache> loadCaches()
   {
      if (metricRegistry != null)
      {
         return super.loadCaches().stream()
               .<EhCacheCache>map(EhCacheCache.class::cast)
               .<InstrumentedCacheDecotrator>map(InstrumentedCacheDecotrator::new)
               .collect(Collectors.toList());
      }
      return super.loadCaches();
   }

   @Override
   protected org.springframework.cache.Cache getMissingCache(String name)
   {
      EhCacheCache res = EhCacheCache.class.cast(super.getMissingCache(name));
      if (metricRegistry != null)
      {
         res = new InstrumentedCacheDecotrator(res);
      }
      return res;
   }

   /** Adapted from com.codahale.metrics.ehcache.InstrumentedEhcache */
   private class InstrumentedCacheDecotrator extends EhCacheCache {

      private final EhCacheCache decorated;
      private final Timer getTimer, putTimer;

      public InstrumentedCacheDecotrator(final EhCacheCache decorated)
      {
         super(decorated.getNativeCache());

         this.decorated = decorated;
         Ehcache cache = decorated.getNativeCache();
         final String prefix = name("dhus.cache", cache.getName());

         metricRegistry.register(name(prefix, "hits"),
               (Gauge<Long>) () -> cache.getStatistics().cacheHitCount());

         metricRegistry.register(name(prefix, "in-memory-hits"),
               (Gauge<Long>) () -> cache.getStatistics().localHeapHitCount());

         metricRegistry.register(name(prefix, "off-heap-hits"),
               (Gauge<Long>) () -> cache.getStatistics().localOffHeapHitCount());

         metricRegistry.register(name(prefix, "on-disk-hits"),
               (Gauge<Long>) () -> cache.getStatistics().localDiskHitCount());

         metricRegistry.register(name(prefix, "misses"),
               (Gauge<Long>) () -> cache.getStatistics().cacheMissCount());

         metricRegistry.register(name(prefix, "in-memory-misses"),
               (Gauge<Long>) () -> cache.getStatistics().localHeapMissCount());

         metricRegistry.register(name(prefix, "off-heap-misses"),
               (Gauge<Long>) () -> cache.getStatistics().localOffHeapMissCount());

         metricRegistry.register(name(prefix, "on-disk-misses"),
               (Gauge<Long>) () -> cache.getStatistics().localDiskMissCount());

         metricRegistry.register(name(prefix, "objects"),
               (Gauge<Long>) () -> cache.getStatistics().getSize());

         metricRegistry.register(name(prefix, "in-memory-objects"),
               (Gauge<Long>) () -> cache.getStatistics().getLocalHeapSize());

         metricRegistry.register(name(prefix, "off-heap-objects"),
               (Gauge<Long>) () -> cache.getStatistics().getLocalOffHeapSize());

         metricRegistry.register(name(prefix, "on-disk-objects"),
               (Gauge<Long>) () -> cache.getStatistics().getLocalDiskSize());

         metricRegistry.register(name(prefix, "mean-get-time"),
               (Gauge<Double>) () -> cache.getStatistics().cacheGetOperation().latency().average().value());

         metricRegistry.register(name(prefix, "mean-search-time"),
               (Gauge<Double>) () -> cache.getStatistics().cacheSearchOperation().latency().average().value());

         metricRegistry.register(name(prefix, "eviction-count"),
               (Gauge<Long>) () -> cache.getStatistics().cacheEvictionOperation().count().value());

         metricRegistry.register(name(prefix, "searches-per-second"),
               (Gauge<Double>) () -> cache.getStatistics().cacheSearchOperation().rate().value());

         metricRegistry.register(name(prefix, "writer-queue-size"),
               (Gauge<Long>) () -> cache.getStatistics().getWriterQueueLength());

         this.getTimer = metricRegistry.timer(name(prefix, "gets"));
         this.putTimer = metricRegistry.timer(name(prefix, "puts"));
      }

      @Override
      public ValueWrapper get(Object key)
      {
         final Timer.Context ctx = getTimer.time();
         try
         {
            return decorated.get(key);
         }
         finally
         {
            ctx.stop();
         }
      }

      @Override
      public <T> T get(Object key, Class<T> type)
      {
         final Timer.Context ctx = getTimer.time();
         try
         {
            return decorated.get(key, type);
         }
         finally
         {
            ctx.stop();
         }
      }

      @Override
      public void put(Object key, Object value)
      {
         final Timer.Context ctx = putTimer.time();
         try
         {
            decorated.put(key, value);
         }
         finally
         {
            ctx.stop();
         }
      }

      @Override
      public ValueWrapper putIfAbsent(Object key, Object value)
      {
         final Timer.Context ctx = putTimer.time();
         try
         {
            return decorated.putIfAbsent(key, value);
         }
         finally
         {
            ctx.stop();
         }
      }

   }

}
