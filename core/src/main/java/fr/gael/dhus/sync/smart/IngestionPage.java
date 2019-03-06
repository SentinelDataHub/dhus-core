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
package fr.gael.dhus.sync.smart;

import fr.gael.dhus.sync.impl.ODataProductSynchronizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.store.ParallelProductSetter;
import org.dhus.store.StoreService;
import org.dhus.store.ingestion.IngestibleODataProduct;

/**
 * Manages downloads of a {@link ODataProductSynchronizer}.
 */
public class IngestionPage implements AutoCloseable
{
   private static final Logger LOGGER = LogManager.getLogger();

   /** Global set of all queued downloads, to avoid simultaneous downloads of the same product. */
   private static final Set<DownloadTask> GLOBAL_DOWNLOADS = Collections.synchronizedSet(new HashSet<>());

   private static class DownloadTask implements Comparable<DownloadTask>
   {
      /** Not null. */
      final IngestibleODataProduct product;

      Future<?> productDownload;

      DownloadTask(IngestibleODataProduct prod)
      {
         this.product = prod;
      }

      /** Returns true if ingestion is done whatever the status of this ingestion. */
      boolean isDone()
      {
         return productDownload == null || productDownload.isDone();
      }

      void setProductDownload(Future<?> productDownload)
      {
         this.productDownload = productDownload;
      }

      @Override
      public boolean equals(Object obj)
      {
         if (obj == null || !DownloadTask.class.isAssignableFrom(obj.getClass()))
         {
            return false;
         }
         DownloadTask other = DownloadTask.class.cast(obj);
         return other.product.getUuid().equals(this.product.getUuid());
      }

      @Override
      public int hashCode()
      {
         return UUID.fromString(this.product.getUuid()).hashCode();
      }

      @Override
      public int compareTo(DownloadTask o)
      {
         // Ordering by creation date is important because it is the pivot between already synced
         // products and to be synced products. Used to order elements in the `localDownloads` set.
         int result = product.getCreationDate().compareTo(o.product.getCreationDate());

         // In case of two products have the same creation date.
         if (result == 0)
         {
            result = product.getUuid().compareTo(o.product.getUuid());
         }
         return result;
      }
   }

   class PageStatus
   {
      private final List<IngestibleODataProduct> completed;
      private final List<IngestibleODataProduct> interrupted;
      private final List<IngestibleODataProduct> failed;

      private int pending;

      private PageStatus()
      {
         this.completed = new ArrayList<>();
         this.interrupted = new ArrayList<>();
         this.failed = new ArrayList<>();
         clearStatus();
      }

      private void clearStatus()
      {
         completed.clear();
         failed.clear();
         interrupted.clear();
         pending = 0;
      }

      private void updateStatus()
      {
         clearStatus();
         for (DownloadTask task: IngestionPage.this.localDownloads)
         {
            if (task.isDone())
            {
               try
               {
                  task.productDownload.get();
                  completed.add(task.product);
               }
               catch (InterruptedException | CancellationException e)
               {
                  interrupted.add(task.product);
               }
               catch (ExecutionException e)
               {
                  failed.add(task.product);
               }
            }
            else
            {
               pending = pending + 1;
            }
         }
      }

      public int pendingIngestion()
      {
         return pending;
      }

      public int getPagePercentage()
      {
         return ((completed.size() + failed.size()) * 100) / IngestionPage.this.size();
      }

      public List<IngestibleODataProduct> getCompleted()
      {
         return completed;
      }

      public List<IngestibleODataProduct> getFailed()
      {
         return failed;
      }

      public List<IngestibleODataProduct> getInterrupted()
      {
         return interrupted;
      }
   }

   /**
    * A set of queued downloads managed a synchronizer. An implementation of SortedSet is
    * required to avoid skipping failing downloads.
    */
   private final Set<DownloadTask> localDownloads = new TreeSet<>();
   private final ParallelProductSetter taskExecutor;
   private final List<IngestibleODataProduct> products;
   private final StoreService storeService;
   private final List<String> targetCollections;
   private final ArrayList<IngestibleODataProduct> alreadyPending;
   private final PageStatus status;

   IngestionPage(StoreService storeService, List<IngestibleODataProduct> productList, List<String> targetCollections)
   {
      int pageSize = productList.size();

      this.taskExecutor = new ParallelProductSetter(pageSize, pageSize, 0, TimeUnit.SECONDS);
      this.storeService = storeService;
      this.products = productList;
      this.targetCollections = targetCollections;
      this.alreadyPending = new ArrayList<>();
      this.status = new PageStatus();

      startIngestions();
   }

   private void startIngestions()
   {
      for (IngestibleODataProduct product: products)
      {
         DownloadTask ingestionTask = new DownloadTask(product);
         if (GLOBAL_DOWNLOADS.contains(ingestionTask))
         {
            alreadyPending.add(product);
            LOGGER.debug("Product {} synchronization is already pending", product.getUuid());
         }
         else
         {
            ingestionTask.setProductDownload(taskExecutor.submitProduct(storeService, product, targetCollections));
            GLOBAL_DOWNLOADS.add(ingestionTask);
            localDownloads.add(ingestionTask);
            LOGGER.debug("Starting synchronization of product {}", product.getUuid());
         }
      }
   }

   /**
    * Returns true if all tasks are done.
    *
    * @return true if all tasks are finished, otherwise false
    */
   public boolean isDone()
   {
      return localDownloads.stream().allMatch(task -> task.isDone());
   }

   /**
    * Returns number of task submitted in this page.
    *
    * @return number of task submitted in this page
    */
   public int size()
   {
      return localDownloads.size();
   }

   /**
    * Returns a date to resume the synchronization avoiding any gap.
    * <p>
    * The 'creationDate' is a product property, this property representing a date.
    * If all tasks are done successfully then this method returns the greatest 'creationDate',
    * else the smallest 'creationDate' of failed tasks.
    *
    * @return a date, or {@code null} if all tasks are not done
    */
   public GregorianCalendar lastValidCreationDate()
   {
      if (isDone())
      {
         GregorianCalendar date = new GregorianCalendar();
         date.setTimeInMillis(0);
         for (DownloadTask task: localDownloads)
         {
            try
            {
               task.productDownload.get();
               date.setTime(task.product.getCreationDate());
            }
            catch (InterruptedException | CancellationException | ExecutionException e)
            {
               break;
            }
         }
         return date;
      }
      return null;
   }

   public PageStatus getStatus()
   {
      status.updateStatus();
      return status;
   }

   @Override
   public void close() throws Exception
   {
      for (DownloadTask task: localDownloads)
      {
         try
         {
            task.product.close();
            task.productDownload.cancel(true);
            task.productDownload.get();
         }
         catch (IOException | CancellationException | ExecutionException suppressed) {}
         GLOBAL_DOWNLOADS.remove(task);
      }
      taskExecutor.shutdownNow();
   }
}
