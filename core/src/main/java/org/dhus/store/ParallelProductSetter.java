/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2018 GAEL Systems
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

   public ParallelProductSetter(int core_pool_size, int max_pool_size, long keep_alive, TimeUnit time_unit)
   {
      BlockingQueue<Runnable> work_queue = new LinkedBlockingDeque<>();
      this.threadPool = new ThreadPoolExecutor(core_pool_size, max_pool_size, keep_alive, time_unit,
            work_queue, new DaemonThreadFactory());
   }

   public Future<?> submitProduct(StoreService targetStore, IngestibleProduct product, List<String> targetCollections)
   {
      return this.threadPool.submit(AddTask.get(targetStore, product, targetCollections));
   }

   /**
    * Calls `shutdownNow` on the {@link ThreadPoolExecutor} backing this manager.
    * @see ThreadPoolExecutor#shutdownNow()
    */
   public void shutdownNow()
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
      private final List<String> targetCollections;

      public static AddTask get(StoreService target, IngestibleProduct product, List<String> targetCollections)
      {
         return new AddTask(target, product, targetCollections);
      }

      public AddTask(StoreService target, IngestibleProduct product, List<String> targetCollections)
      {
         this.productToAdd = product;
         this.targetStore = target;
         this.targetCollections = targetCollections;
      }

      @Override
      public Void call() throws StoreException
      {
         targetStore.addProduct(productToAdd, targetCollections, false);
         return null;
      }
   }

   /** Creates only daemon threads. */
   private static class DaemonThreadFactory implements ThreadFactory
   {
      @Override
      public Thread newThread(Runnable r)
      {
         Thread thread = new Thread(r, "sync-ingestion");
         thread.setDaemon(true);
         return thread;
      }
   }
}
