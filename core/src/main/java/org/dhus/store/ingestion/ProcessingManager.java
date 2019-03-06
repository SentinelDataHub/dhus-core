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
package org.dhus.store.ingestion;

import fr.gael.dhus.datastore.processing.fair.FairCallable;
import fr.gael.dhus.datastore.processing.fair.FairThreadPoolTaskExecutor;
import fr.gael.dhus.datastore.scanner.FileScannerWrapper;
import fr.gael.dhus.datastore.scanner.Scanner;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
         List<String> collectionNames, Scanner scanner, FileScannerWrapper wrapper)
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

   /*
    * Reported from DefaultDataStrore
    */
   private static class ScannerProcessingCallable extends FairCallable
   {
      private final IngestibleProduct product;
      private final Scanner scanner;
      private final List<String> collectionNames;
      private final FileScannerWrapper wrapper;

      public ScannerProcessingCallable(IngestibleProduct product, List<String> collectionNames,
            Scanner scanner, FileScannerWrapper wrapper)
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
         STORE_SERVICE.addProduct(product, collectionNames, scanner, wrapper);
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
         STORE_SERVICE.addProduct(product, collectionNames, removeSource);
         return null;
      }
   }
}
