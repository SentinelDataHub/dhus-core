/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2020 GAEL Systems
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
package org.dhus.store;

import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dhus.ProductConstants;
import org.dhus.store.ingestion.IngestibleODataProduct;
import org.dhus.store.ingestion.IngestibleProduct;
import org.dhus.store.ingestion.ProcessingManager;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import fr.gael.dhus.sync.impl.ProductInformationUtils;

/**
 * Uses a thread pool to set products into stores.
 */
public class ParallelProductSetter
{
   /** Thread pool. */
   private final ThreadPoolExecutor threadPool;
   
   /** Log. */
   private static final Logger LOGGER = LogManager.getLogger(ParallelProductSetter.class);

   public ParallelProductSetter(String threadName, int core_pool_size, int max_pool_size, long keep_alive, TimeUnit time_unit)
   {
      BlockingQueue<Runnable> work_queue = new LinkedBlockingDeque<>();
      this.threadPool = new ThreadPoolExecutor(core_pool_size, max_pool_size, keep_alive, time_unit,
            work_queue, new DaemonThreadFactory(threadName));
   }

   public Future<?> submitProduct(StoreService targetStore, IngestibleProduct product, List<String> targetCollections, boolean sourceRemove
         , Long syncId, String sourceUrl, Long sourceId, MetricRegistry metricRegistry)
   {
      return this.threadPool.submit(AddTask.get(targetStore, product, null, targetCollections, sourceRemove, syncId, sourceUrl, sourceId, metricRegistry));
   }

   //Only called by transformationManager
   public Future<?> submitProduct(StoreService targetStore, IngestibleProduct product,
         String targetDataStore, List<String> targetCollections, boolean sourceRemove)
   {
      return this.threadPool.submit(AddTask.get(targetStore, product, targetDataStore, targetCollections, sourceRemove, null,null,null, null));
   }

   /**
    * Shutdown and waits (maximum 1 hour), let tasks terminate gracefully.
    * /!\ To be used by other scenarios not using the DownloadableProduct.
    */
   public void shutdownBlockingIO()
   {
      try
      {
         this.threadPool.shutdown();
         this.threadPool.awaitTermination(1, TimeUnit.HOURS);
      }
      catch (InterruptedException suppressed) {}
   }

   /**
    * Shutdown and waits (max 20 seconds), let tasks terminate gracefully.
    * /!\ To be used by the sync with copy scenario (products must be DownloadableProducts.
    * the timeout is short because IO operations have already been interrupted by the close method of the ODataProdSync.
    */
   public void shutdownNonBlockingIO()
   {
      try
      {
         this.threadPool.shutdown();
         this.threadPool.awaitTermination(20, TimeUnit.SECONDS);
      }
      catch (InterruptedException suppressed) {}
   }

   /** Add task used by #submitProduct(). */
   private static class AddTask implements Callable<Void>
   {
      private final StoreService targetStore;
      private final IngestibleProduct productToAdd;
      private final String targetDataStore;
      private final List<String> targetCollections;
      private final boolean sourceRemove;
      private final Long sourceId;
      private final Long syncId;
      private final String sourceUrl;
      private final MetricRegistry metricRegistry;

      public static AddTask get(StoreService target, IngestibleProduct product, String targetDataStore,
            List<String> targetCollections, boolean sourceRemove, Long syncId, String sourceUrl, Long sourceId, MetricRegistry metricRegistry)
      {
         return new AddTask(target, product, targetDataStore, targetCollections, sourceRemove, syncId, sourceUrl, sourceId, metricRegistry);
      }

      public AddTask(StoreService target, IngestibleProduct product, String targetDataStore,
            List<String> targetCollections, boolean sourceRemove, Long syncId, String sourceUrl, Long sourceId, MetricRegistry metricRegistry)
      {
         this.productToAdd = product;
         this.targetStore = target;
         this.targetDataStore = targetDataStore;
         this.targetCollections = targetCollections;
         this.sourceRemove = sourceRemove;
         this.sourceId = sourceId;
         this.syncId = syncId;
         this.sourceUrl = sourceUrl;
         this.metricRegistry = metricRegistry;
      }

      @Override
      public Void call() throws StoreException
      {
         if (productToAdd instanceof IngestibleODataProduct)
         {
            IngestibleODataProduct odataProduct = (IngestibleODataProduct) productToAdd;

            Timer timerPerSyncPerProduct = null;
            Timer timerPerSync = null;
            Timer.Context ctxTimerPerSyncPerProduct = null;
            Timer.Context ctxTimerPerSync = null;

            String genericMetricPrefix = ProductInformationUtils.parseGeneralMetricNameFromEntry(odataProduct);
            String perSyncPerProductCountersMetricPrefix = ProductInformationUtils.parsePerSourceMetricNameFromEntry(odataProduct, syncId, sourceId, "counters", true);
            String perSyncCountersMetricPrefix = ProductInformationUtils.parsePerSourceMetricNameFromEntry(odataProduct, syncId, sourceId, "counters", false);
            Long size = ProcessingManager.getVolume(productToAdd);
            String perSyncPerProductTimersMetricPrefix = ProductInformationUtils.parsePerSourceMetricNameFromEntry(odataProduct, syncId, sourceId, "timers", true);
            String perSyncTimersMetricPrefix = ProductInformationUtils.parsePerSourceMetricNameFromEntry(odataProduct, syncId, sourceId, "timers", false);
            String perSyncPerProductMeterMetricPrefix = ProductInformationUtils.parsePerSourceMetricNameFromEntry(odataProduct, syncId, sourceId, "meters", true);
            String perSyncMeterMetricPrefix = ProductInformationUtils.parsePerSourceMetricNameFromEntry(odataProduct, syncId, sourceId, "meters", false);
            String perSyncPerProductHistMetricPrefix = ProductInformationUtils.parsePerSourceMetricNameFromEntry(odataProduct, syncId, sourceId, "histogram", true);
            String perSyncHistMetricPrefix = ProductInformationUtils.parsePerSourceMetricNameFromEntry(odataProduct, syncId, sourceId, "histogram", false);

            LOGGER.debug("*** Metric timer: {}", perSyncPerProductTimersMetricPrefix);
            LOGGER.debug("*** Metric timer per sync: {}", perSyncTimersMetricPrefix);
            LOGGER.debug("Start synchronization for the product {} ",productToAdd.getIdentifier());

            String suffixTransferSize = ".transferSize";
            String suffixTransferRate = ".transferRate";
            if (metricRegistry != null)
            {
               timerPerSyncPerProduct = metricRegistry.timer(perSyncPerProductTimersMetricPrefix);
               timerPerSync = metricRegistry.timer(perSyncTimersMetricPrefix);
               ctxTimerPerSyncPerProduct = timerPerSyncPerProduct.time();
               ctxTimerPerSync = timerPerSync.time();
            }

            // Add product into the datastore
            long timeStart = System.currentTimeMillis();
            targetStore.addProduct(odataProduct, targetDataStore, targetCollections, sourceRemove);
            long timeEnd = System.currentTimeMillis();

            if (metricRegistry != null)
            {
               Meter meterMetric = metricRegistry.meter(perSyncMeterMetricPrefix + suffixTransferSize);
               Meter meterMetricPerProduct = metricRegistry.meter(perSyncPerProductMeterMetricPrefix + suffixTransferSize);
               String successSuffix = ".success";
               metricRegistry.meter(genericMetricPrefix + successSuffix).mark();
               metricRegistry.meter(perSyncPerProductCountersMetricPrefix + successSuffix).mark(1L);
               metricRegistry.meter(perSyncCountersMetricPrefix + successSuffix).mark(1L);
               if (size != null)
               {
                  
                  long rate = (size / (timeEnd-timeStart))*1000; // convert from milliseconds to seconds
                  LOGGER.debug("Transfert rate calculation - timeStart: {} - timeEnd: {} - Size: {} "
                        + " - TransferRate: {} (bytes/s) ", timeStart, timeEnd, size, rate);
                  //meters are used to register the volume transfered during the process
                  meterMetric.mark(size);
                  meterMetricPerProduct.mark(size);
                  String volumeSuffix = ".volume";
                  metricRegistry.meter(genericMetricPrefix + volumeSuffix).mark(size);
                  metricRegistry.meter(perSyncPerProductCountersMetricPrefix + volumeSuffix).mark(size);
                  metricRegistry.meter(perSyncCountersMetricPrefix + volumeSuffix).mark(size);
                  // Add to have histogram on transfert Rate 
                  metricRegistry.histogram(perSyncHistMetricPrefix + suffixTransferRate).update(rate);
                  metricRegistry.histogram(perSyncPerProductHistMetricPrefix + suffixTransferRate).update(rate);
               }
               reportTimelinessMetrics(odataProduct);

               // stop metrics timers
               ctxTimerPerSyncPerProduct.stop();
               ctxTimerPerSync.stop();
            }
         LOGGER.info("Synchronizer#{} Product {} ({} bytes compressed) successfully synchronized from {}", syncId, productToAdd.getIdentifier(),
               odataProduct.getProperty(ProductConstants.DATA_SIZE), sourceUrl);
         }
         else
         {
            targetStore.addProduct(productToAdd, targetDataStore, targetCollections, sourceRemove);
         }
         return null;
      }

      /** Report metrics on product timeliness. 
       * @param odataProduct */
      private void reportTimelinessMetrics(IngestibleODataProduct odataProduct)
      {
         String prefix = "prod_sync.sync" + syncId + ".source"+ sourceId +".timeliness";
         Date creationDateOnDatastore = (Date) odataProduct.getProperty("creationDate");
         metricRegistry.histogram(prefix + ".creation")
               .update(creationDateOnDatastore.getTime() - odataProduct.getCreationDate().getTime());
         long now = System.currentTimeMillis();
         metricRegistry.histogram(prefix + ".ingestion")
               .update(now - productToAdd.getIngestionDate().getTime());
         LOGGER.debug("Timeliness - CreationDate: {} - IngestionDate: {}",  odataProduct.getCreationDate(),
               productToAdd.getIngestionDate() );
      }
      
   }
   

   /** Creates only daemon threads. */
   private static class DaemonThreadFactory implements ThreadFactory
   {
      private final String threadName;
      private long threadNumber = 0;

      public DaemonThreadFactory(String threadName)
      {
         this.threadName = threadName;
      }

      @Override
      public Thread newThread(Runnable r)
      {
         Thread thread = new Thread(r, threadName+"#"+threadNumber++);
         thread.setDaemon(true);
         return thread;
      }
   }
}
