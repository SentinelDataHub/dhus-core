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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.dhus.store.ingestion.IngestibleProduct;

/**
 * Uses a thread pool to set products into stores.
 */
public class ParallelProductSetter
{
   /** Thread pool. */
   private final ThreadPoolExecutor threadPool;

   public ParallelProductSetter(String threadName, int core_pool_size, int max_pool_size, long keep_alive, TimeUnit time_unit)
   {
      BlockingQueue<Runnable> work_queue = new LinkedBlockingDeque<>();
      this.threadPool = new ThreadPoolExecutor(core_pool_size, max_pool_size, keep_alive, time_unit,
            work_queue, new DaemonThreadFactory(threadName));
   }

   public Future<?> submitProduct(StoreService targetStore, IngestibleProduct product, List<String> targetCollections, boolean sourceRemove)
   {
      return this.threadPool.submit(AddTask.get(targetStore, product, null, targetCollections, sourceRemove));
   }

   public Future<?> submitProduct(StoreService targetStore, IngestibleProduct product,
         String targetDataStore, List<String> targetCollections, boolean sourceRemove)
   {
      return this.threadPool.submit(AddTask.get(targetStore, product, targetDataStore, targetCollections, sourceRemove));
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

      public static AddTask get(StoreService target, IngestibleProduct product, String targetDataStore,
            List<String> targetCollections, boolean sourceRemove)
      {
         return new AddTask(target, product, targetDataStore, targetCollections, sourceRemove);
      }

      public AddTask(StoreService target, IngestibleProduct product, String targetDataStore,
            List<String> targetCollections, boolean sourceRemove)
      {
         this.productToAdd = product;
         this.targetStore = target;
         this.targetDataStore = targetDataStore;
         this.targetCollections = targetCollections;
         this.sourceRemove = sourceRemove;
      }

      @Override
      public Void call() throws StoreException
      {
         targetStore.addProduct(productToAdd, targetDataStore, targetCollections, sourceRemove);
         return null;
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
