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
package org.dhus.store.datastore.async.pdgs;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.store.datastore.StreamableProduct;

/**
 * Manages parallel product downloads.
 * This class is not thread safe.
 */
public class ParallelProductDownloadManager
{
   private static final Logger LOGGER = LogManager.getLogger();

   private final ThreadPoolExecutor threadPool;

   private final LinkedList<ProductDownload> runningDownloads = new LinkedList<>();

   public ParallelProductDownloadManager(int maxPoolSize)
   {
      this.threadPool = new ThreadPoolExecutor(
            maxPoolSize, // initial download pool size
            maxPoolSize, // maximum download pool size
            0, TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(),
            new DaemonThreadFactory());
   }

   /**
    * Submits a product download task to this manager.
    * This method is not thread safe.
    *
    * @param streamableProduct to download (must not be null)
    * @param task performing the product download (must not be null)
    * @return a non null future
    */
   public Future<?> submit(StreamableProduct streamableProduct, Runnable task)
   {
      Future<?> f = this.threadPool.submit(task);
      ProductDownload downloadProduct = new ProductDownload(streamableProduct, f);
      runningDownloads.add(downloadProduct);
      return f;
   }

   public boolean isAlreadyQueued(String productName)
   {
      return runningDownloads.stream()
            .filter(download -> download.getStreamableProduct().getName().equals(productName))
            .findFirst().isPresent();
   }

   /**
    * Calls `shutdownNow` on the {@link ThreadPoolExecutor} backing this manager.
    *
    * @see ThreadPoolExecutor#shutdownNow()
    */
   public void shutdownNow()
   {
      this.runningDownloads.clear();
      try
      {
         this.threadPool.shutdown();
         this.threadPool.awaitTermination(20, TimeUnit.SECONDS);
      }
      catch (InterruptedException suppressed) {}
   }

   /**
    * Checks and manages running product downloads and.
    * This method is not thread safe.
    */
   public void checkProductDownloads()
   {
      Iterator<ProductDownload> it = runningDownloads.iterator();
      while (it.hasNext())
      {
         ProductDownload task = it.next();
         if (task.isDone())
         {
            try
            {
               task.getDownloadTask().get();
               LOGGER.info("Product {} successfully downloaded", task.getStreamableProduct().getName());
            }
            catch (InterruptedException | ExecutionException e)
            {
               LOGGER.error("Product {} failed to download", task.getStreamableProduct().getName(), e);
            }

            // remove from running downloads list
            it.remove();
            // FIXME should tasks be removed as soon as they are done instead of waiting for a call to this method?
         }
      }
      LOGGER.debug("ParallelProductDownloadManager: {} downloads running", runningDownloads.size());
   }

   /** Creates only daemon threads. */
   private static class DaemonThreadFactory implements ThreadFactory
   {
      @Override
      public Thread newThread(Runnable r)
      {
         Thread thread = new Thread(r, "download-product");
         thread.setDaemon(true);
         return thread;
      }
   }

   private static final class ProductDownload
   {
      private final StreamableProduct streamableProduct;
      private final Future<?> downloadTask;

      public ProductDownload(StreamableProduct streamableProduct, Future<?> downloadTask)
      {
         this.streamableProduct = streamableProduct;
         this.downloadTask = downloadTask;
      }

      public boolean isDone()
      {
         return downloadTask.isDone();
      }

      @Override
      public boolean equals(Object obj)
      {
         if (obj == null || !ProductDownload.class.isAssignableFrom(obj.getClass()))
         {
            return false;
         }
         ProductDownload other = ProductDownload.class.cast(obj);
         return other.streamableProduct.getName().equals(this.streamableProduct.getName());
      }

      @Override
      public int hashCode()
      {
         return this.streamableProduct.getName().hashCode();
      }

      public Future<?> getDownloadTask()
      {
         return downloadTask;
      }

      public StreamableProduct getStreamableProduct()
      {
         return streamableProduct;
      }
   }
}
