/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018 GAEL Systems
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
package fr.gael.dhus.database.object.config.source;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.bind.annotation.XmlTransient;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;

/**
 * Represents a source of data.
 */
public class Source extends SourceConfiguration implements Comparable<Source>
{
   private static final int CALCULATOR_CACHE_TTL = 5; // 5 seconds
   private static final int CALCULATOR_CACHE_MAX_ENTRIES = 1_000;

   private static final String SOURCE_CACHE_PREFIX = "AverageBandwidthSource#";
   private static final int SOURCE_CACHE_TTL = 180; // 3 minutes
   private static final int SOURCE_CACHE_MAX_ENTRIES = SOURCE_CACHE_TTL * 1_000;
   private static final int MIN_CACHE_ENTRIES = 100;

   static final long UNKNOWN_BANDWIDTH = -1;

   private static void initCacheManager(Source source)
   {
      if (source.cacheManager == null)
      {
         CacheConfiguration cacheConf = new CacheConfiguration();
         cacheConf.setEternal(false);
         cacheConf.setTimeToIdleSeconds(CALCULATOR_CACHE_TTL);
         cacheConf.setTimeToLiveSeconds(CALCULATOR_CACHE_TTL);
         cacheConf.setMaxEntriesLocalHeap(CALCULATOR_CACHE_MAX_ENTRIES);
         cacheConf.setMemoryStoreEvictionPolicy("FIFO");
         cacheConf.setOverflowToOffHeap(false);
         cacheConf.setCopyOnRead(true);
         cacheConf.setCopyOnWrite(false);
         cacheConf.setLogging(true);

         Configuration conf = new Configuration();
         conf.setDefaultCacheConfiguration(cacheConf);
         conf.setDynamicConfig(true);
         conf.setName("BandwidthCalculator#" + source.getId());
         source.cacheManager = CacheManager.newInstance(conf);
      }
   }

   private static void initSourceCache(Source source)
   {
      if (source.cacheManager == null)
      {
         throw new IllegalStateException("Cache manager is not available");
      }

      String cacheName = SOURCE_CACHE_PREFIX + source.getId();
      if (source.cache == null)
      {
         CacheConfiguration conf = new CacheConfiguration();
         conf.setEternal(false);
         conf.setTimeToLiveSeconds(SOURCE_CACHE_TTL);
         conf.setTimeToIdleSeconds(SOURCE_CACHE_TTL);
         conf.setMaxEntriesLocalHeap(SOURCE_CACHE_MAX_ENTRIES);
         conf.setMemoryStoreEvictionPolicy("FIFO");
         conf.setOverflowToOffHeap(false);
         conf.setCopyOnRead(true);
         conf.setCopyOnWrite(false);
         conf.setLogging(true);
         conf.setName(cacheName);
         Cache sourceCache = new Cache(conf);
         source.cacheManager.addCache(sourceCache);
         source.cache = sourceCache;
      }
   }

   private static long computeBandwidthFromCache(Cache cache)
   {
      if (cache == null)
      {
         return UNKNOWN_BANDWIDTH;
      }

      Long transferredBytes = null;
      long totalTransferred = 0;
      long validTransfer = 0;
      long minTime = 0;
      long maxTime = 0;
      List keys = cache.getKeysWithExpiryCheck();

      if (keys.isEmpty() || keys.size() < MIN_CACHE_ENTRIES)
      {
         return UNKNOWN_BANDWIDTH;
      }

      for (Object key: keys)
      {
         try
         {
            Element element = cache.get(key);
            if (element != null)
            {
               minTime = (minTime == 0 || minTime > element.getCreationTime()) ?
                     element.getCreationTime() : minTime;
               maxTime = (maxTime == 0 || maxTime < element.getCreationTime()) ?
                     element.getCreationTime() : maxTime;

               transferredBytes = (Long) element.getObjectValue();
               totalTransferred = totalTransferred + transferredBytes;
               validTransfer = validTransfer + 1;
            }
         }
         catch (CacheException | IllegalStateException suppressed) {}
      }

      // compute average bandwidth
      long duration = maxTime - minTime;
      if (duration > 1_000)
      {
         transferredBytes = totalTransferred / (duration / 1_000);
      }
      else
      {
         transferredBytes = totalTransferred;
      }

      return transferredBytes;
   }

   @XmlTransient
   private final ConcurrentHashMap<String, BandwidthCalculator> calculatorMap;

   @XmlTransient
   private CacheManager cacheManager;

   @XmlTransient
   private Cache cache;

   /**
    * Default constructor.
    */
   public Source()
   {
      this.calculatorMap = new ConcurrentHashMap<>();
   }

   /**
    * Copy constructor.
    *
    * @param config configuration to copy
    */
   Source(SourceConfiguration config)
   {
      this();
      super.setId(config.getId());
      super.setUrl(config.getUrl());
      super.setUsername(config.getUsername());
      super.setPassword(config.getPassword());
      super.setMaxDownload(config.getMaxDownload());
   }

   private synchronized void initSource()
   {
      if (cacheManager == null || cache == null)
      {
         initCacheManager(this);
         initSourceCache(this);
      }
   }

   /**
    * Computes and returns average bandwidth of this source.
    *
    * @return the bandwidth of this source in byte per second, or -1 if it is not computable
    */
   public long getBandwidth()
   {
      return computeBandwidthFromCache(this.cache);
   }

   /**
    * Returns number of downloads active on this source.
    *
    * @return number of downloads active
    */
   public int concurrentDownload()
   {
      return calculatorMap.size();
   }

   /**
    * Generates a new bandwidth calculator.
    *
    * @param identifier
    * @return identifier of the newest calculator
    */
   public synchronized boolean generateBandwidthCalculator(String identifier)
   {
      initSource();
      if (calculatorMap.containsKey(identifier))
      {
         return false;
      }
      cacheManager.addCache(identifier);
      BandwidthCalculator calculator = new BandwidthCalculator(cacheManager.getCache(identifier));
      calculatorMap.put(identifier, calculator);
      return true;
   }

   /**
    * Populates a calculator, in order to allowing it to compute bandwidth.
    *
    * @param id calculator identifier
    * @param transferredBytes the data
    */
   public void populateBandwidthCalculator(String id, long transferredBytes)
   {
      initSource();
      if (cacheManager != null)
      {
         BandwidthCalculator calculator = calculatorMap.get(id);
         if (calculator != null)
         {
            calculator.add(transferredBytes);
            cache.put(new Element(System.nanoTime(), transferredBytes));
         }
      }
   }

   /**
    * Computes and returns bandwidth from a calculator.
    *
    * @param id calculator id
    * @return bandwidth computed from the calculator, or -1 if bandwidth cannot be computed
    */
   public long getCalculatedBandwidth(String id)
   {
      initSource();
      BandwidthCalculator calculator = calculatorMap.get(id);
      if (calculator != null)
      {
         return calculator.computeBandwidth();
      }
      return UNKNOWN_BANDWIDTH;
   }

   /**
    * Removes and releases resources of a calculator.
    *
    * @param id calculator id to remove
    */
   public void removeBandwidthCalculator(String id)
   {
      initSource();
      BandwidthCalculator calculator = calculatorMap.remove(id);
      if (calculator != null)
      {
         cacheManager.removeCache(calculator.getIdentifier());
      }
   }

   @Override
   public boolean equals(Object o)
   {
      if (this == o)
      {
         return true;
      }

      if (o == null || getClass() != o.getClass())
      {
         return false;
      }

      Source other = (Source) o;
      return id == other.id
            || (url.equals(other.url) && username.equals(other.username) && password.equals(other.password));
   }

   @Override
   public int hashCode()
   {
      return Objects.hashCode(getId());
   }

   @Override
   public int compareTo(Source o)
   {
      int order = Long.compare(getBandwidth(), o.getBandwidth());
      if (order == 0)
      {
         return Integer.compare(id, o.id);
      }
      return order;
   }

   public void close()
   {
      if (cacheManager != null)
      {
         cacheManager.removeAllCaches();
         cacheManager.shutdown();
         cacheManager = null;
         cache = null;
      }
   }

   @Override
   protected void finalize() throws Throwable
   {
      close();
      super.finalize();
   }

   /**
    * BandwidthCalculator class allows to compute a bandwidth.
    * <p>
    * This bandwidth is computed using cached sliding window.
    */
   private static class BandwidthCalculator
   {
      private final Cache cache;

      BandwidthCalculator(Cache cache)
      {
         this.cache = cache;
      }

      public String getIdentifier()
      {
         return cache.getName();
      }

      /**
       * Saves in cache the given value.
       *
       * @param transferredBytes value to add
       */
      public synchronized void add(long transferredBytes)
      {
         cache.put(new Element(System.nanoTime(), transferredBytes));
      }

      /**
       * Computes bandwidth of a download using number of downloaded bytes saved.
       *
       * @return the bandwidth of the associated download in byte per second, or -1 if it is not
       *         computable
       */
      public synchronized long computeBandwidth()
      {
         return computeBandwidthFromCache(this.cache);
      }

   }
}
