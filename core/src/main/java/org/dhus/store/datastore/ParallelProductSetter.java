/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
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
package org.dhus.store.datastore;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.dhus.Product;

/**
 * Uses a thread pool to set products into datastores.
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

   public Future<?> submit(DataStore target, String id, Product prod)
   {
      return this.threadPool.submit(new AddTask(id, target, prod));
   }

   /**
    * Calls `shutdownNow` on the {@link ThreadPoolExecutor} backing this manager.
    * @see ThreadPoolExecutor#shutdownNow()
    */
   public void shutdownNow()
   {
      this.threadPool.shutdownNow();
   }

   /** Add task used by #submit(). */
   private static class AddTask implements Callable<Void>
   {
      private final String id;
      private final DataStore target;
      private final Product toAdd;

      public AddTask(String id, DataStore target, Product to_add)
      {
         this.id = id;
         this.toAdd = to_add;
         this.target = target;
      }

      @Override
      public Void call() throws DataStoreException
      {
         this.target.set(this.id, this.toAdd);
         return null;
      }
   }

   /** Creates only daemon threads. */
   private static class DaemonThreadFactory implements ThreadFactory
   {
      @Override
      public Thread newThread(Runnable r)
      {
         Thread thread = new Thread(r, "DataStore Transfer");
         thread.setDaemon(true);
         return thread;
      }
   }
}
