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

import fr.gael.dhus.database.object.DeletedProduct;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.datastore.Destination;
import fr.gael.dhus.datastore.scanner.Scanner;
import fr.gael.dhus.datastore.scanner.ScannerStatus;
import fr.gael.dhus.service.TransformationService;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.ProductConstants;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.DataStoreManager;
import org.dhus.store.datastore.DataStoreProduct;
import org.dhus.store.datastore.DataStores;
import org.dhus.store.datastore.ProductNotFoundException;
import org.dhus.store.datastore.UnsafeDeletionException;
import org.dhus.store.derived.DerivedProductStoreService;
import org.dhus.store.ingestion.IngestibleProduct;
import org.dhus.store.ingestion.IngestibleRawProduct;
import org.dhus.store.metadatastore.MetadataStoreService;

import org.springframework.beans.factory.annotation.Autowired;

public class StoreService implements Store
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final int FILTERED_DELETION_PAGE_SIZE = 1000;

   public static final String STORE_SERVICE_NAME = "StoreService";

   private final AtomicBoolean evictionMustStop = new AtomicBoolean(false);

   private final Set<String> runningRepairsUuids = Collections.<String>synchronizedSet(new HashSet<>());

   @Autowired
   private DataStoreManager dataStoreService;

   @Autowired
   private DerivedProductStoreService derivedProductStoreService;

   @Autowired
   private MetadataStoreService metadataStoreService;

   @Autowired
   private TransformationService transformationService;

   @Override
   public void deleteProduct(String uuid) throws StoreException
   {
      deleteProduct(uuid, Destination.NONE, true, null);
   }

   /**
    * Deletes a product from all stores and stores it as deleted.
    *
    * @param uuid
    * @param destination
    * @param cause
    * @throws StoreException
    */
   public void deleteProduct(String uuid, Destination destination, String cause) throws StoreException
   {
      deleteProduct(uuid, destination, true, cause);
   }

   /**
    * Deletes a product from all stores.
    *
    * @param uuid of product to delete
    * @param destination to move the product's data to
    * @param storeAsDeleted {@code true} to keep this product as deleted
    * @param cause of the deletion
    * @throws StoreException could not perform operation
    */
   public void deleteProduct(String uuid, Destination destination, boolean storeAsDeleted, String cause)
         throws StoreException
   {
      // start global deletion and time it
      LOGGER.info("Deleting product {} globally", uuid);
      long start = System.currentTimeMillis();

      List<Throwable> throwables = new LinkedList<>();

      // physical product
      try
      {
         softDeleteProduct(destination, uuid);
      }
      catch (ProductNotFoundException suppressed) {}
      catch (DataStoreException e)
      {
         throwables.add(e);
      }

      // derived products
      try
      {
         derivedProductStoreService.deleteDerivedProducts(uuid);
      }
      catch (ProductNotFoundException suppressed) {}
      catch (StoreException e)
      {
         throwables.add(e);
      }

      // product metadata
      try
      {
         metadataStoreService.deleteProduct(uuid, storeAsDeleted, cause);
      }
      catch (ProductNotFoundException suppressed) {}
      catch (StoreException e)
      {
         throwables.add(e);
      }

      // delete associated transformation if necessary
      transformationService.removeTransformationByOutputProduct(uuid);

      // throw errors if any
      DataStores.throwErrors(throwables, "storeServiceDelete", uuid);

      // report time spent
      LOGGER.info("Product {} deleted globally in {}ms", uuid, System.currentTimeMillis() - start);
   }

   public void stopCurrentEviction()
   {
      evictionMustStop.set(true);
   }

   private List<LoggableProduct> listProductUUIDs(String filter, String orderBy, String collectionName,
         boolean online, int skip, int top) throws StoreException
   {
      if (online)
      {
         return metadataStoreService.getOnlineProductUUIDs(filter, orderBy, collectionName, skip, top);
      }
      else
      {
         return metadataStoreService.getProductUUIDs(filter, orderBy, collectionName, skip, top);
      }
   }

   /**
    * Automatic eviction.
    *
    * @param sizeToEvict   in bytes
    * @param dataStoreName name of the DataStore
    *
    * @throws StoreException
    * @throws DataStoreException
    */
   public void evictAtLeast(long sizeToEvict, String dataStoreName) throws DataStoreException, StoreException
   {
      internalEvictAtLeast(sizeToEvict, dataStoreName, null, null, null, Integer.MAX_VALUE, true, Destination.TRASH, DeletedProduct.AUTO_EVICTION, false);
   }

   /**
    * Remove data from a DataStore using a configured eviction.
    *
    * @param sizeToEvict        in bytes
    * @param dataStoreName      name of the DataStore
    * @param filter             OData formatted filter ($filter parameter)
    * @param orderBy            OData formatted sort ($order parameter)
    * @param collectionName     collectionName exclude product not in that collection
    * @param maxDeletedProducts maximum number of product to evict
    * @param softEviction       softDeletion softDeletion {@code true} to exclude derived products (QuickLooks & Thumbnails)
    * @param destination        to move deleted data to
    * @param cause              the cause of the deletion
    *
    * @return total evicted size
    *
    * @throws StoreException
    */
   public long evictAtLeast(long sizeToEvict, String dataStoreName, String filter, String orderBy,
         String collectionName, int maxDeletedProducts, boolean softEviction, Destination destination, String cause, Boolean safeMode)
         throws StoreException
   {
      return internalEvictAtLeast(sizeToEvict, dataStoreName, filter, orderBy, collectionName, maxDeletedProducts, softEviction, destination, cause, safeMode);
   }

   // TODO merge with deleteProducts?
   private long internalEvictAtLeast(long sizeToEvict, String dataStoreName, String filter, String orderBy,
         String collectionName, int maxDeletedProducts, boolean softEviction, Destination destination, String cause, Boolean safeMode)
         throws StoreException, DataStoreException
   {
      // to ensure a previous stop will not prevent this start
      evictionMustStop.set(false);

      LOGGER.info("Attempting to evict maximum {} products or maximum {} bytes from DataStore '{}'",
                  maxDeletedProducts, sizeToEvict, dataStoreName);

      long totalEvictedSize = 0;
      int productsDeleted = 0;

      // retrieve datastore product iterator
      DataStoreProductIterator productIterator =
            new DataStoreProductIterator(filter, orderBy, collectionName, dataStoreName);

      // eviction stops if enough space is freed
      // or many enough products have been deleted
      // or if user requested a stop
      while (!evictionMustStop.get() && totalEvictedSize < sizeToEvict && productsDeleted < maxDeletedProducts)
      {
         if (!productIterator.hasNext())
         {
            // deleted as many products as possible
            LOGGER.info("No products left to delete in DataStore '{}'", dataStoreName);
            break;
         }
         else
         {
            LoggableProduct logProd = productIterator.next();

            try
            {
               long start = System.currentTimeMillis();

               if (softEviction)
               {
                  softDeleteProduct(logProd.getUuid(), dataStoreName, destination, safeMode);
                  LOGGER.info("Product {} ({}) ({} bytes) has been successfully soft evicted from datastore {}",
                        logProd.getIdentifier(), logProd.getUuid(), logProd.getSize(), dataStoreName);
               }
               else
               {
                  deleteProduct(logProd.getUuid(), destination, cause);
               }

               // product successfully deleted
               totalEvictedSize += logProd.getSize();
               productsDeleted++;

               // sysma logs
               long totalTime = System.currentTimeMillis() - start;
               LOGGER.info("Evicted {} ({} bytes, {} bytes compressed) spent {}ms (from datastore {})",
                     logProd.getIdentifier(),
                     logProd.getSize(),
                     logProd.getSize(),
                     totalTime,
                     dataStoreName);
            }
            catch (ProductNotFoundException ex)
            {
               LOGGER.info("Product {} not found in DataStore '{}', skipping...", logProd, dataStoreName);
               continue;
            }
            catch (UnsafeDeletionException e)
            {
               LOGGER.info("Cannot safely delete product {} from DataStore {}, this product isn't present in any other DataStore", logProd, dataStoreName);
               continue;
            }
         }
      }
      LOGGER.info("{} bytes evicted from DataStore '{}'", totalEvictedSize, dataStoreName);
      return totalEvictedSize;
   }

   /**
    * Versatile method to delete product in DataStores.
    * This method is exclusively used by Eviction and can be stopped via an OData Action.
    *
    * @param filter OData formatted filter ($filter param)
    * @param orderBy OData formatted sort ($order param)
    * @param collectionName exclude product not in this collection
    * @param maxDeletedProducts maximum amount of product to delete
    * @param softDeletion {@code true} to exclude derived products (quicklooks & thumbnails)
    * @param destination to move deleted data to
    * @param cause the cause of the deletion
    * @throws StoreException could not perform operation
    */
   // TODO merge with internalEvictAtLeast?
   public void evictProducts(String filter, String orderBy, String collectionName,
         int maxDeletedProducts, boolean softDeletion, Destination destination, String cause)
         throws StoreException
   {
      // to ensure a previous stop will not prevent this start
      evictionMustStop.set(false);

      int productsDeleted = 0;

      LOGGER.info("Attempting to delete {} products...", maxDeletedProducts);
      long start = System.currentTimeMillis();

      // get product iterator and start deletion loop
      ProductIterator productIterator = new ProductIterator(filter, orderBy, collectionName, softDeletion);
      while (productsDeleted < maxDeletedProducts && productIterator.hasNext())
      {
         if (evictionMustStop.get())
         {
            break;
         }

         LoggableProduct logProd = productIterator.next();

         // attempt product deletion
         try
         {
            long deleteStart = System.currentTimeMillis();
            if (softDeletion)
            {
               softDeleteProduct(destination, logProd.getUuid());
               LOGGER.info("Product {} ({}) ({} bytes) has been successfully soft evicted from all datastore",
                     logProd.getIdentifier(), logProd.getUuid(), logProd.getSize());
            }
            else
            {
               // delete product, metadata, and derived products
               deleteProduct(logProd.getUuid(), destination, cause);
            }
            // increment product deletion counter if successful
            productsDeleted++;

            // print sysma's dumb logs
            long deleteTime = System.currentTimeMillis() - deleteStart;
            switch (cause)
            {
               case DeletedProduct.AUTO_EVICTION:
                  LOGGER.info("Evicted {} ({} bytes, {} bytes compressed) spent {}ms",
                        logProd.getIdentifier(),
                        logProd.getSize(),
                        logProd.getSize(),
                        deleteTime);
                  break;
               default:
                  LOGGER.info("Deletion of product '{}' ({} bytes) successful spent {}ms",
                        logProd.getIdentifier(),
                        logProd.getSize(),
                        deleteTime);
                  break;
            }
         }
         catch (ProductNotFoundException ex)
         {
            continue;
         }
         catch (StoreException ex)
         {
            LOGGER.warn("Cannot delete product {}: {}", logProd, ex.getMessage());
         }
         catch (RuntimeException ex)
         {
            LOGGER.error("Cannot delete product {}", logProd, ex);
         }
      }
      LOGGER.info("Successfully deleted {} products in {}ms", productsDeleted, System.currentTimeMillis() - start);
   }

   private void softDeleteProduct(Destination destination, String uuid) throws DataStoreException
   {
      // only delete physical product
      dataStoreService.deleteProduct(uuid, destination);

      if (!dataStoreService.hasProduct(uuid))
      {
         metadataStoreService.setProductOffline(uuid);
      }
   }

   public void addProductReference(IngestibleProduct inProduct, List<String> targetCollectionsNames)
         throws StoreException
   {
      LOGGER.info("Ingesting product reference and metadata of UUID {} and identifier {}",
            inProduct.getUuid(), inProduct.getIdentifier());
      long start = System.currentTimeMillis();

      try
      {
         // FIXME why does this one return a boolean instead of throwing an exception?
         if (!dataStoreService.addProductReference(inProduct.getUuid(), inProduct))
         {
            throw new StoreException("Failed to create product " + inProduct.getUuid() + " reference");
         }
         derivedProductStoreService.addDefaultDerivedProductReferences(inProduct);
         metadataStoreService.addProduct(inProduct, targetCollectionsNames);
      }
      catch (StoreException e)
      {
         // delete product in case of error
         deleteProduct(inProduct.getUuid(), Destination.NONE, false, null);
         throw e;
      }
      finally
      {
         try
         {
            inProduct.close();
         }
         catch (IOException e)
         {
            LOGGER.warn("Couldn't close product {} after ingestion", inProduct.getUuid());
         }
      }

      LOGGER.info("Product of UUID {} and identifier {} successfully ingested in {}ms",
            inProduct.getUuid(), inProduct.getIdentifier(),
            System.currentTimeMillis() - start);
   }

   @Override
   public void addProduct(IngestibleProduct inProduct) throws StoreException
   {
      addProduct(inProduct, null /* all data stores */, Collections.<String>emptyList(), false);
   }

   public void addProduct(IngestibleProduct inProduct, List<String> targetCollectionsNames,
         Scanner scanner, ScannerStatus scannerWrapper) throws StoreException
   {
      // notify ingestion cancellation if scanner is stopped
      if (scanner.isStopped())
      {
         scannerWrapper.cancelProcess(inProduct.getOrigin());
         return;
      }

      // notify start of ingestion
      scannerWrapper.startIngestion();

      try
      {
         addProduct(inProduct, null /* all data stores */, targetCollectionsNames, scanner.getConfig().isSourceRemove());
      }
      catch (StoreException e)
      {
         // notify error
         scannerWrapper.error(inProduct.getOrigin(), e);
         throw e;
      }
      catch (RuntimeException e)
      {
         // notify runtime error
         LOGGER.error("Unexpected RuntimeException caught while adding a product in datastores", e);
         scannerWrapper.error(inProduct.getOrigin(), e);
         throw e;
      }

      // notify ingestion success
      scannerWrapper.endIngestion();
   }

   public void addProduct(IngestibleProduct inProduct, List<String> targetCollectionsNames, boolean sourceRemove)
         throws StoreException
   {
      addProduct(inProduct, null, targetCollectionsNames, sourceRemove);
   }

   public void addProduct(IngestibleProduct inProduct, String targetDataStore, List<String> targetCollectionsNames, boolean sourceRemove)
         throws StoreException
   {
      if (metadataStoreService.hasProduct(inProduct.getUuid()))
      {
         LOGGER.error("A product of UUID {} already exists", inProduct.getUuid());
         throw new StoreException.ProductAlreadyExistsException(inProduct.getUuid());
      }

      // TODO improve transactionality
      LOGGER.info("Inserting product of UUID {} and identifier {}", inProduct.getUuid(), inProduct.getIdentifier());
      inProduct.startTimer();

      try
      {
         dataStoreService.addProduct(inProduct, targetDataStore);
         derivedProductStoreService.addDefaultDerivedProducts(inProduct);
         metadataStoreService.addProduct(inProduct, targetCollectionsNames);
      }
      catch (StoreException | RuntimeException | Error ex)
      {
         LOGGER.error("Could not insert product UUID={}, Identifier='{}' in stores, reverting...",
               inProduct.getUuid(), inProduct.getIdentifier(), ex);
         try
         {
            deleteProduct(inProduct.getUuid(), Destination.ERROR, false, null);
         }
         catch (StoreException | RuntimeException de)
         {
            LOGGER.warn("An error occured during deletion of partially ingested product: {}", de.getMessage());
         }
         if (StoreException.class.isAssignableFrom(ex.getClass()))
         {
            throw StoreException.class.cast(ex);
         }
         throw new StoreException(ex);
      }
      finally
      {
         // cleanup resources
         try
         {
            if (sourceRemove)
            {
               boolean removed = inProduct.removeSource();
               if(!removed)
               {
                  LOGGER.warn("Couldn't remove source of product {} at {}", inProduct.getUuid(), inProduct.getOrigin());
               }
            }

            inProduct.close();
         }
         catch (IOException e)
         {
            LOGGER.warn("Couldn't cleanup resources of product {} after ingestion", inProduct.getUuid());
         }
      }

      inProduct.stopTimer();
      LOGGER.info("Product of UUID {} and identifier {} successfully inserted in {}ms",
            inProduct.getUuid(), inProduct.getIdentifier(), inProduct.getIngestionTimeMillis());

      // sysma logs
      if(inProduct instanceof IngestibleRawProduct)
      {
         LOGGER.info("Ingestion processing complete for product {} ({} bytes, {} bytes compressed) in {}ms.",
               inProduct.getOrigin(),
               inProduct.getProperty(ProductConstants.DATA_SIZE),
               inProduct.getProperty(ProductConstants.DATA_SIZE),
               inProduct.getIngestionTimeMillis());
      }
   }

   public void restoreProduct(String uuid, Long size, Map<String, String> checksums)
         throws ProductNotFoundException
   {
      metadataStoreService.restoreProduct(uuid, size, checksums);
   }

   private void softDeleteProduct(String uuid, String dataStoreName, Destination destination, Boolean safeMode)
         throws DataStoreException
   {
      dataStoreService.deleteProductFromDataStore(uuid, dataStoreName, destination, safeMode);

      if (!dataStoreService.hasProduct(uuid))
      {
         metadataStoreService.setProductOffline(uuid);
      }
   }

   public DataStoreProduct getPhysicalProduct(String uuid) throws DataStoreException
   {
      return dataStoreService.get(uuid).getImpl(DataStoreProduct.class);
   }

   /**
    * Repair a product.
    *
    * @param uuid of product to repair
    *
    * @throws ProductNotFoundException no product for the given UUID
    * @throws StoreException a repair is already running for the given product
    */
   public void repairProduct(String uuid) throws StoreException
   {
      if (runningRepairsUuids.contains(uuid))
      {
         throw new StoreException("Repair is already running for product: " + uuid);
      }

      runningRepairsUuids.add(uuid);
      LOGGER.debug("Added product {} to running repairs", uuid);

      try
      {
         if (!dataStoreService.hasProduct(uuid))
         {
            throw new ProductNotFoundException();
         }

         LOGGER.info("Repairing product {}", uuid);
         long start = System.currentTimeMillis();

         Product toRepair = metadataStoreService.getDatabaseProduct(uuid);

         IngestibleProduct ingestibleProduct = IngestibleRawProduct.fromDataStoreProduct(
               uuid, (DataStoreProduct) dataStoreService.get(uuid), toRepair);

         metadataStoreService.repairProduct(ingestibleProduct);

         // TODO update quicklook/thumbnail as well?
         long time = System.currentTimeMillis() - start;
         LOGGER.info("Product {} successfully repaired in {} milliseconds", uuid, time);
      }
      finally
      {
         runningRepairsUuids.remove(uuid);
         LOGGER.debug("Removed product {} from running repairs", uuid);
      }
   }

   /**
    * Repair a set of products.
    *
    * @param filter OData filter expression
    * @param orderBy OData orderby expression
    * @param maxRepairedProducts maximum number of products to repair
    * @param skip number of products to skip in the list of products to repair
    *
    * @return number of repaired products
    * @throws StoreException Cannot repair products, may be caused by a malformed filter expression
    */
   public int repairProducts(String filter, String orderBy, int maxRepairedProducts, int skip) throws StoreException
   {
      LOGGER.info("Attempting to repair {} products after skipping {}", maxRepairedProducts, skip);
      long start = System.currentTimeMillis();

      int repairedProducts = 0;
      ProductIterator productIterator = new ProductIterator(filter, orderBy, null, true, skip);
      while (repairedProducts < maxRepairedProducts && productIterator.hasNext())
      {
         LoggableProduct product = productIterator.next();
         try
         {
            repairProduct(product.getUuid());
            repairedProducts++;
         }
         catch (StoreException | RuntimeException e)
         {
            LOGGER.warn("Could not repair product {}", product, e);
            continue;
         }
      }

      long time = System.currentTimeMillis() - start;
      LOGGER.info("Successfully repaired {} products in {} milliseconds", repairedProducts, time);
      return repairedProducts;
   }

   /**
    * Paginated product iterator.
    */
   private class ProductIterator implements Iterator<LoggableProduct>
   {
      private final String filter;
      private final String orderBy;
      private final String collectionName;
      private final boolean online;

      private int skip = 0;
      private Iterator<LoggableProduct> productPage = Collections.emptyIterator();

      public ProductIterator(String filter, String orderBy, String collectionName, boolean online, int skip) throws StoreException
      {
         this.filter = filter;
         this.orderBy = orderBy;
         this.collectionName = collectionName;
         this.online = online;
         this.skip = skip;

         // fetch next page so that filter can be validated
         fetchNextPage();
      }

      public ProductIterator(String filter, String orderBy, String collectionName, boolean online) throws StoreException
      {
         this.filter = filter;
         this.orderBy = orderBy;
         this.collectionName = collectionName;
         this.online = online;

         // fetch next page so that filter can be validated
         fetchNextPage();
      }

      @Override
      public boolean hasNext()
      {
         if (productPage.hasNext())
         {
            return true;
         }
         else
         {
            try
            {
               fetchNextPage();
               return productPage.hasNext();
            }
            catch (StoreException e)
            {
               LOGGER.error("Could not fetch next page of products", e);
               return false;
            }
         }
      }

      private void fetchNextPage() throws StoreException
      {
         List<LoggableProduct> products = listProductUUIDs(
               filter, orderBy, collectionName, online, skip, FILTERED_DELETION_PAGE_SIZE);

         skip += FILTERED_DELETION_PAGE_SIZE;

         // no products left
         if (products.isEmpty())
         {
            LOGGER.debug("No products left for filter {}", filter);
         }

         productPage = products.iterator();
      }

      @Override
      public LoggableProduct next()
      {
         return productPage.next();
      }

   }

   /**
    * Specialized iterator to combine the listing of products from a DataStore
    * and metadata-based filtering.
    * <p>
    * FIXME: when used by eviction code, products are skipped because the number of
    * deleted KeyStore entries not subtracted from the skip value.
    * TODO add a goBack(int) method?
    */
   private class DataStoreProductIterator implements Iterator<LoggableProduct>
   {
      private final String filter;
      private final String orderBy;
      private final String collectionName;
      private final String dataStoreName;

      private int skip = 0;
      private Iterator<LoggableProduct> productPage = Collections.emptyIterator();

      private DataStoreProductIterator(String filter, String orderBy, String collectionName,
            String dataStoreName) throws StoreException
      {
         this.filter = filter;
         this.orderBy = orderBy;
         this.collectionName = collectionName;
         this.dataStoreName = dataStoreName;

         // fetch next page so that filter can be validated
         fetchNextPage();
      }

      @Override
      public boolean hasNext()
      {
         if (productPage.hasNext())
         {
            return true;
         }
         else
         {
            try
            {
               fetchNextPage();
               return productPage.hasNext();
            }
            catch (StoreException e)
            {
               LOGGER.error("Could not fetch next page of products", e);
               return false;
            }
         }
      }

      private void fetchNextPage() throws StoreException
      {
         List<String> dataStoreProductUUIDs;
         List<LoggableProduct> productUUIDsWithin = Collections.emptyList();
         do
         {
            // get page from datastore
            dataStoreProductUUIDs = dataStoreService
                  .getDataStoreByName(dataStoreName)
                  .getProductList(skip, FILTERED_DELETION_PAGE_SIZE);

            skip += FILTERED_DELETION_PAGE_SIZE;

            // page empty, no product left in datastore, return
            if (dataStoreProductUUIDs.isEmpty())
            {
               LOGGER.debug("<Targetted Eviction> No product left in DataStore: {}", dataStoreName);
               productPage = Collections.emptyIterator();
               return;
            }
            // filter page using metadatastore service
            else
            {
               productUUIDsWithin = metadataStoreService
                     .getProductUUIDsWithin(filter, orderBy, collectionName, dataStoreProductUUIDs);
            }
         } while (productUUIDsWithin.isEmpty());

         productPage = productUUIDsWithin.iterator();
      }

      @Override
      public LoggableProduct next()
      {
         return productPage.next();
      }
   }
}
