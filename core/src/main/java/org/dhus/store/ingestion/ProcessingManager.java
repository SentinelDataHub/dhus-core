/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2019 GAEL Systems
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
package org.dhus.store.ingestion;

import static org.dhus.metrics.Utils.ItemClass;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import fr.gael.dhus.datastore.processing.fair.FairCallable;
import fr.gael.dhus.datastore.processing.fair.FairThreadPoolTaskExecutor;
import fr.gael.dhus.datastore.scanner.Scanner;
import fr.gael.dhus.datastore.scanner.ScannerStatus;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.ProductConstants;
import org.dhus.store.StoreException;
import org.dhus.store.StoreService;

/**
 *
 */
public class ProcessingManager
{
   private static final FairThreadPoolTaskExecutor TASK_EXECUTOR =
         ApplicationContextProvider.getBean(FairThreadPoolTaskExecutor.class);

   private static final StoreService STORE_SERVICE =
         ApplicationContextProvider.getBean(StoreService.class);

   /** Metric Registry, for monitoring purposes. */
   private static final MetricRegistry METRIC_REGISTRY =
         ApplicationContextProvider.getBean(MetricRegistry.class);

   private static final Logger LOGGER = LogManager.getLogger();

   public static Future<Object> processProduct(IngestibleProduct product,
         List<String> collectionNames, boolean removeSource)
   {
      return process(new ProcessingCallable(product, collectionNames, removeSource));
   }

   /**
    * Process given unprocessed product.
    *
    * @param product to process
    * @param collectionNames to reference this product
    * @param scanner that scanned this product
    * @param wrapper to notify of ingestion events
    * @return A future to get notified for the end of the processing.
    *         May return {@code null} if a RejectedExecutionException has been thrown.
    *         {@code get()} will return null, see {@link ScannerProcessingCallable#call()}.
    */
   public static Future<Object> processProduct(IngestibleProduct product,
         List<String> collectionNames, Scanner scanner, ScannerStatus wrapper)
   {
      return process(new ScannerProcessingCallable(product, collectionNames, scanner, wrapper));
   }

   private static Future<Object> process(FairCallable processingCallable)
   {
      Future<Object> future = null;
      int retry = 10;
      while (retry > 0)
      {
         try
         {
            future = TASK_EXECUTOR.submit(processingCallable);
            retry = 0;
         }
         catch (RejectedExecutionException ree)
         {
            retry--;
            if (retry <= 0)
            {
               throw ree;
            }
            try
            {
               Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
               LOGGER.warn("Current thread was interrupted by another", e);
            }
         }
      }
      return future;
   }

   /**
    * Returns the size in bytes of the given product's data.
    * @param product not null
    * @return the size or {@code null}
    */
   @SuppressWarnings("unchecked")
   public static Long getVolume(IngestibleProduct product)
   {
      return (Long) product.getProperty(ProductConstants.DATA_SIZE);
   }

   /*
    * Reported from DefaultDataStrore
    */
   private static class ScannerProcessingCallable extends FairCallable
   {
      private final IngestibleProduct product;
      private final Scanner scanner;
      private final List<String> collectionNames;
      private final ScannerStatus wrapper;

      public ScannerProcessingCallable(IngestibleProduct product, List<String> collectionNames,
            Scanner scanner, ScannerStatus wrapper)
      {
         super (scanner.toString ());
         this.product = product;
         this.collectionNames = collectionNames;
         this.scanner = scanner;
         this.wrapper = wrapper;
      }

      @Override
      public Object call() throws StoreException
      {
         String metricPrefix = "ingestion." + ItemClass.toMetricNamePart(product.getItemClass(), ItemClass.Precision.FINE);
         try (Timer.Context context = METRIC_REGISTRY.timer(metricPrefix + ".timer").time())
         {
            STORE_SERVICE.addProduct(product, collectionNames, scanner, wrapper);
            METRIC_REGISTRY.counter(metricPrefix + ".counters.success").inc();
            Long size = getVolume(product);
            if (size != null)
            {
               METRIC_REGISTRY.counter(metricPrefix + ".counters.volume").inc(size);
            }
         }
         catch (Throwable t)
         {
            METRIC_REGISTRY.counter(metricPrefix + ".counters.failure").inc();
            throw t;
         }
         return null;
      }
   }

   private static class ProcessingCallable extends FairCallable
   {
      private final IngestibleProduct product;
      private final List<String> collectionNames;
      private final boolean removeSource;

      public ProcessingCallable(IngestibleProduct product, List<String> collectionNames, boolean removeSource)
      {
         super(null);
         this.product = product;
         this.collectionNames = collectionNames;
         this.removeSource = removeSource;
      }

      @Override
      public Object call() throws Exception
      {
         String metricPrefix = "ingestion." + ItemClass.toMetricNamePart(product.getItemClass(), ItemClass.Precision.FINE);
         try (Timer.Context context = METRIC_REGISTRY.timer(metricPrefix + ".timer").time())
         {
            STORE_SERVICE.addProduct(product, collectionNames, removeSource);
            METRIC_REGISTRY.counter(metricPrefix + ".counters.success").inc();
            Long size = getVolume(product);
            if (size != null)
            {
               METRIC_REGISTRY.counter(metricPrefix + ".counters.volume").inc(size);
            }
         }
         catch (Throwable t)
         {
            METRIC_REGISTRY.counter(metricPrefix + ".counters.failure").inc();
            throw t;
         }
         return null;
      }
   }
}
