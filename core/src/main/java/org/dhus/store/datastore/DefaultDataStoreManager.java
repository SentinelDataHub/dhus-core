/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016-2020 GAEL Systems
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dhus.Product;
import org.dhus.ProductConstants;
import org.dhus.store.StoreException;
import org.dhus.store.datastore.async.AsyncDataStore;
import org.dhus.store.datastore.config.DataStoreRestriction;
import org.dhus.store.datastore.hfs.HfsDataStore;
import org.dhus.store.derived.DerivedProductStore;
import org.dhus.store.derived.DerivedProductStoreService;
import org.dhus.store.ingestion.IngestibleProduct;
import org.springframework.beans.factory.annotation.Autowired;

import fr.gael.dhus.database.object.KeyStoreEntry;
import fr.gael.dhus.datastore.Destination;
import fr.gael.dhus.service.KeyStoreService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;

/**
 * This class implements the default DataStore container that is constructed according to aggregated DataStores.
 * The execution of this DataStore commands will automatically be propagated to the handled DataStores.
 */
// TODO reorganize methods for better readability
public class DefaultDataStoreManager implements DataStoreManager, DerivedProductStoreService
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final String DATA_STORE_NAME = "DefaultDataStoreService";

   private final SortedSet<DataStore> datastores = new ConcurrentSkipListSet<>(
         (DataStore ds1, DataStore ds2) -> DataStores.compare(ds1, ds2));

   @Autowired
   private KeyStoreService keyStoreService;

   @Override
   public String getName()
   {
      return DATA_STORE_NAME;
   }

   @Override
   public Product get(String uuid) throws DataStoreException
   {
      LOGGER.debug("Finding product {} in data stores...", uuid);
      List<DataStore> datastoreList = listForUuid(uuid, true, true);
      for (DataStore datastore : datastoreList)
      {
         try
         {
            return datastore.get(uuid);
         }
         catch (DataStoreException e)
         {
            continue;
         }
      }
      LOGGER.debug("Product {} not found in any data store", uuid);

      throw new ProductNotFoundException("Cannot retrieve product for id " + uuid);
   }

   // TODO remove this method?
   // This method MUST be transactional: in case of failure all the DataStores that have been set
   // in this context must be rolled back.
   @Override
   public void set(String uuid, Product product) throws DataStoreException
   {
      for (DataStore datastore : datastores)
      {
         try
         {
            long duration = System.currentTimeMillis();
            datastore.set(uuid, product);
            duration = System.currentTimeMillis() - duration;
            LOGGER.info("Product [{}:{}] stored in {} datastore in {}ms",
                  product.getName(), product.getProperty(ProductConstants.DATA_SIZE),
                  datastore.getName(), duration);
         }
         catch (ReadOnlyDataStoreException e)
         {
            continue;
         }
         catch (RuntimeException e)
         {
            throw new DataStoreException(e);
         }
      }
   }

   @Override
   public void addProduct(IngestibleProduct inProduct, String targetDataStore) throws StoreException
   {
      if (targetDataStore == null)
      {
         LOGGER.info("No target data store, inserting product {} in all data stores", inProduct.getUuid());
      }
      else
      {
         LOGGER.info("Inserting product {} in target data store {}", inProduct.getUuid(), targetDataStore);
      }

      List<Throwable> throwables = new ArrayList<>();
      int count = 0;
      for (DataStore datastore: datastores)
      {
         // only insert product in target data store
         // except when targetDataStore is null, in which case insert everywhere
         if (targetDataStore != null && !datastore.getName().equals(targetDataStore))
         {
            continue;
         }

         if (Thread.interrupted())
         {
            Thread.currentThread().interrupt();
            throw new StoreException("Product insertion interrupted");
         }

         try
         {
            long duration = System.currentTimeMillis();
            datastore.addProduct(inProduct);
            LOGGER.debug("Added product {} to data store '{}'", inProduct.getUuid(), datastore.getName());

            count++;

            duration = System.currentTimeMillis() - duration;
            LOGGER.info("Product {} of UUID {} and size {} stored in {} datastore in {}ms",
                  inProduct.getName(), inProduct.getUuid(),
                  inProduct.getProperty(ProductConstants.DATA_SIZE), datastore.getName(), duration);
         }
         catch (ReadOnlyDataStoreException e)
         {
            continue;
         }
         catch (StoreException | RuntimeException e)
         {
            throwables.add(e);
         }
      }
      LOGGER.info("Product {} inserted in {} data stores", inProduct.getUuid(), count);
      DataStores.throwErrors(throwables, "addProduct", inProduct.getUuid());
   }

   @Override
   public void deleteProduct(String uuid) throws DataStoreException
   {
      deleteProduct(uuid, Destination.NONE, false);
   }

   @Override
   public void deleteProduct(String uuid, Destination destination, boolean safeMode) throws DataStoreException
   {
      LOGGER.info("Deleting product {} from all DataStores - safe : {}", uuid, safeMode);
      
      List<Throwable> throwables = new ArrayList<>();
      boolean deleted = false;
      int count = 0;
      List<DataStore> datastoreList;

      if (safeMode)
      {
         // get list of datastores in safeMode
         SortedSet<DataStore> dataStores = getDataStoresInSafeMode(uuid);
         datastoreList = dataStores.stream().collect(Collectors.toList());
         // attempt to move product to destination
         if (dataStores.size() == 0)
         {
            throw new ProductNotFoundException();
         }
         makeBackupProduct(uuid, destination, null, dataStores);
      }
      else
      {
         // get list of dataStores
         datastoreList = listForUuid(uuid, false, false);
         // attempt to move product to destination
         makeBackupProduct(uuid, destination, null, null);
      }
      for (DataStore datastore : datastoreList)
      {
         try
         {
            datastore.deleteProduct(uuid);
            LOGGER.debug("Deleted product {} from data store '{}'", uuid, datastore.getName());

            deleted = true;
            count++;
         }
         catch (ReadOnlyDataStoreException | ProductNotFoundException e)
         {
            LOGGER.debug("Cannot remove product {} from data store '{}': {}", uuid, datastore.getName(), e.getMessage());
            continue;
         }
         catch (DataStoreException | RuntimeException e)
         {
            throwables.add(e);
         }
      }
      if(!deleted)
      {
         throw new ProductNotFoundException();
      }

      LOGGER.debug("Removed product {} from {} data stores", uuid, count);
      DataStores.throwErrors(throwables, "deleteProduct", uuid);
   }

   /**
    * 
    * @param uuid UUID of product to backup
    * @param destination backup destination
    * @param dataStoreName target dataStore from where the product will be removed
    * @param allowedDatastores allowed datastores to be used to backup the product
    */
   private void makeBackupProduct(String uuid, Destination destination, String dataStoreName, Set<DataStore> allowedDatastores)
   {
      if (destination.equals(Destination.NONE))
      {
         return;
      }
      ConfigurationManager configManager = ApplicationContextProvider.getBean(ConfigurationManager.class);
      if (destination.equals(Destination.ERROR) && configManager.getErrorPath() == null
       || destination.equals(Destination.TRASH) && configManager.getTrashPath() == null)
      {
         return;
      }

      LOGGER.info("Moving product {} data to destination {}", uuid, destination);
      Exception exception = new ProductNotFoundException();
      
      if (dataStoreName != null)
      {
         LOGGER.info("Try to backup product {} from datastore {}", uuid, dataStoreName);
         DataStore datastore = getDataStoreByName(dataStoreName);
         exception = processHfsDataStore(uuid, destination, exception, datastore);
         if (exception == null)
         {
            return;
         }
         exception = processOtherDataStore(uuid, destination, exception, datastore);
         if (exception == null)
         {
            return;
         }
      }
      else
      {
         // we assume the optimal way to make a backup product during a deletion
         // is to use an HFSDataStore that isn't read-only
         LOGGER.info("Try to backup product {} from {} datastores", uuid, ((allowedDatastores == null || allowedDatastores.size() == 0) ? "ALL" : "some"));         
         for (DataStore dataStore : ((allowedDatastores == null || allowedDatastores.size() == 0) ? datastores : allowedDatastores))
         {
            exception = processHfsDataStore(uuid, destination, exception, dataStore);
            if (exception == null)
            {
               return;
            }
         }
         // no suitable HFSDataStore was found, use any datastore that has the product
         for (DataStore dataStore : datastores)
         {
            exception = processOtherDataStore(uuid, destination, exception, dataStore);
            if (exception == null)
            {
               return;
            }
         }
      }
      LOGGER.warn("Cannot retrieve data of product {}: {}", uuid, exception.getMessage());
   }

   private Exception processOtherDataStore(String uuid, Destination destination, Exception exception,
         DataStore dataStore)
   {
      if (dataStore.hasProduct(uuid))
      {
         try
         {
            copyProduct(dataStore, uuid, destination);
            LOGGER.info("The product uuid {} copied to destination {}", uuid, destination.name());
            return null;
         }
         catch (DataStoreException | IOException e)
         {
            exception = e;
         }
      }
      return exception;
   }

   private Exception processHfsDataStore(String uuid, Destination destination, Exception exception, DataStore dataStore)
   {
      if (dataStore instanceof HfsDataStore && dataStore.hasProduct(uuid))
      {
         HfsDataStore hfsStore = (HfsDataStore) dataStore;
         if (hfsStore.getRestriction() == DataStoreRestriction.NONE)
         {
            try
            {
               moveProduct((HfsDataStore) dataStore, uuid, destination);
               LOGGER.info("The product uuid {} moved to {}", uuid, destination.name());
               return null;
            }
            catch (DataStoreException | IOException e)
            {
               exception = e;
            }
         }
      }
      return exception;
   }

   private void moveProduct(HfsDataStore dataStore, String uuid, Destination destination) throws DataStoreException, IOException
   {
      Product productData = dataStore.get(uuid);
      Path destinationPath = prepareDestinationPath(productData, destination);

      if (destinationPath != null && productData.hasImpl(File.class))
      {
         Files.move(productData.getImpl(File.class).toPath(), destinationPath);
      }
      else
      {
         throw new DataStoreException(String.format("Cannot move product %s to destination %s", uuid, destination));
      }
      productData.closeProduct();
   }

   private void copyProduct(DataStore dataStore, String uuid, Destination destination) throws IOException, DataStoreException
   {
      Product productData = dataStore.get(uuid);
      Path destinationPath = prepareDestinationPath(productData, destination);

      if (destinationPath != null && productData.hasImpl(File.class))
      {
         Files.copy(productData.getImpl(File.class).toPath(), destinationPath);
      }
      else if (destinationPath != null && productData.hasImpl(InputStream.class))
      {
         InputStream input = productData.getImpl(InputStream.class);
         File destinationFile = destinationPath.toFile();
         if (destinationFile.createNewFile())
         {
            IOUtils.copy(input, new FileOutputStream(destinationFile));
         }
      }
      else
      {
         throw new DataStoreException(String.format("Cannot copy product %s to destination %s", uuid, destination));
      }
      productData.closeProduct();
   }

   /** May return null. */
   private Path prepareDestinationPath(Product productData, Destination destination) throws IOException
   {
      ConfigurationManager configManager = ApplicationContextProvider.getBean(ConfigurationManager.class);

      Path destinationPath = null;
      switch (destination)
      {
         case ERROR:
            String errorPath = configManager.getErrorPath();
            if (errorPath != null && !errorPath.isEmpty())
            {
               destinationPath = Paths.get(errorPath, productData.getName());
            }
            break;
         case TRASH:
            String trashPath = configManager.getTrashPath();
            if (trashPath != null && !trashPath.isEmpty())
            {
               destinationPath = Paths.get(trashPath, productData.getName());
            }
            break;
         case NONE:
         default:
            return null;
      }

      if (destinationPath != null)
      {
         Path directory = destinationPath.getParent();
         if (Files.notExists(directory))
         {
            Files.createDirectory(directory);
         }
      }

      return destinationPath;
   }

   @Override
   public boolean hasProduct(String uuid)
   {
      // get list of products
      List<DataStore> datastoreList = listForUuid(uuid, false, false);
      // at least one datastore has the product if not empty
      return !datastoreList.isEmpty();
   }

   @Override
   public void add(DataStore datastore)
   {
      if (getDataStoreByName(datastore.getName()) != null)
      {
         LOGGER.warn("DataStore already present in the manager, not adding");
      }
      else
      {
         datastores.add(datastore);
      }
   }

   @Override
   public DataStore remove(String datastoreName)
   {
      Optional<DataStore> toRemove = datastores.stream()
            .filter((t) -> t.getName().equals(datastoreName))
            .findFirst();

      if (toRemove.isPresent())
      {
         datastores.remove(toRemove.get());
         return toRemove.get();
      }

      return null;
   }

   /**
    * Returns the list of DataStores managed by this service.
    * The list is a copy and will NOT reflect modifications such as adding or removing DataStores
    * to the service.
    */
   @Override
   public List<DataStore> list()
   {
      return new ArrayList<>(datastores);
   }

   @Override
   public boolean addProductReference(String uuid, Product product) throws DataStoreException
   {
      LOGGER.debug("Adding product {} reference in all suitable data stores...", uuid);
      List<Throwable> throwables = new ArrayList<>();
      boolean added = false;
      int count = 0;
      for (DataStore datastore: datastores)
      {
         try
         {
            if (datastore.addProductReference(uuid, product))
            {
               added = true;
               LOGGER.debug("Added product {} reference to data store '{}'", uuid, datastore.getName());

               count++;
            }
         }
         catch (ReadOnlyDataStoreException e)
         {
            continue;
         }
         catch (DataStoreException | RuntimeException e)
         {
            throwables.add(e);
         }
      }

      LOGGER.debug("Added product {} reference in {} data stores", uuid, count);
      DataStores.throwErrors(throwables, "addProductReference", uuid);
      return added;
   }

   @Override
   public boolean canAccess(String resource_location)
   {
      for (DataStore dataStore: datastores)
      {
         if (dataStore.canAccess(resource_location))
         {
            return true;
         }
      }
      return false;
   }

   @Override
   public int getPriority()
   {
      return 0;
   }

   @Override
   public boolean canModifyReferences()
   {
      return true;
   }

   @Override
   public void addDerivedProduct(String uuid, String tag, Product product) throws StoreException
   {
      addDerivedProduct(uuid, tag, product, null);
   }

   @Override
   public void addDerivedProduct(String uuid, String tag, Product product, String targetDataStore) throws StoreException
   {
      List<Throwable> throwables = new ArrayList<>();
      for (DataStore datastore: datastores)
      {
         // only insert product in target data store
         // except when targetDataStore is null, in which case insert everywhere
         if (targetDataStore != null && !datastore.getName().equals(targetDataStore))
         {
            continue;
         }

         if (datastore.canHandleDerivedProducts ())
         {
            try
            {
               ((DerivedProductStore) datastore).addDerivedProduct(uuid, tag, product);
            }
            catch (ReadOnlyDataStoreException suppressed)
            {
               continue;
            }
            catch (StoreException | RuntimeException e)
            {
               throwables.add(e);
            }
         }
      }
      DataStores.throwErrors(throwables, "addDerivedProduct", uuid);
   }

   @Override
   public void addDefaultDerivedProducts(IngestibleProduct inProduct, String targetDataStore) throws StoreException
   {
      LOGGER.info("Retrieving derived products from product {}", inProduct.getUuid());

      if (inProduct.getQuicklook() != null)
      {
         addDerivedProduct(inProduct.getUuid(), DerivedProductStore.QUICKLOOK_TAG, inProduct.getQuicklook(), targetDataStore);
      }
      if (inProduct.getThumbnail() != null)
      {
         addDerivedProduct(inProduct.getUuid(), DerivedProductStore.THUMBNAIL_TAG, inProduct.getThumbnail(), targetDataStore);
      }

      LOGGER.info("Derived products retrieved from product {}", inProduct.getUuid());
   }

   @Override
   public void addDefaultDerivedProductReferences(IngestibleProduct inProduct) throws StoreException
   {
      LOGGER.info("Retrieving derived product references from product {}", inProduct.getUuid());
      if (inProduct.getQuicklook() != null)
      {
         addDerivedProductReference(inProduct.getUuid(), DerivedProductStore.QUICKLOOK_TAG, inProduct.getQuicklook());
      }
      if (inProduct.getThumbnail() != null)
      {
         addDerivedProductReference(inProduct.getUuid(), DerivedProductStore.THUMBNAIL_TAG, inProduct.getThumbnail());
      }
      LOGGER.info("Derived product references retrieved from product {}", inProduct.getUuid());
   }

   @Override
   public boolean addDerivedProductReference(String uuid, String tag, Product product) throws StoreException
   {
      List<Throwable> throwables = new ArrayList<>();
      boolean added = false;
      for (DataStore datastore: datastores)
      {
         if (datastore.canHandleDerivedProducts())
         {
            try
            {
               if (((DerivedProductStore) datastore).addDerivedProductReference(uuid, tag, product))
               {
                  added = true;
               }
            }
            catch (ReadOnlyDataStoreException suppressed)
            {
               continue;
            }
            catch (StoreException | RuntimeException e)
            {
               throwables.add(e);
            }
         }
      }
      DataStores.throwErrors(throwables, "addDerivedProductReference", uuid);
      return added;
   }

   @Override
   public void deleteDerivedProduct(String uuid, String tag) throws StoreException
   {
      List<Throwable> throwables = new ArrayList<>();

      // get list from keystores
      List<DataStore> datastoreList = listForUuidDerived(uuid, tag);

      for (DataStore datastore: datastoreList)
      {
         try
         {
            ((DerivedProductStore) datastore).deleteDerivedProduct(uuid, tag);
         }
         catch (ReadOnlyDataStoreException | ProductNotFoundException e)
         {
            continue;
         }
         catch (StoreException | RuntimeException e)
         {
            throwables.add(e);
         }
      }
      DataStores.throwErrors(throwables, "deleteDerivedProduct", uuid);
   }

   @Override
   public void deleteDerivedProducts(String uuid) throws StoreException
   {
      LOGGER.info("Deleting derived products of {}", uuid);
      List<Throwable> throwables = new ArrayList<>();

      try
      {
         deleteDerivedProduct(uuid, DerivedProductStore.QUICKLOOK_TAG);
      }
      catch (Exception e)
      {
         throwables.add(e);
      }

      try
      {
         deleteDerivedProduct(uuid, DerivedProductStore.THUMBNAIL_TAG);
      }
      catch (Exception e)
      {
         throwables.add(e);
      }

      DataStores.throwErrors(throwables, "deleteDerivedProducts", uuid);
   }

   @Override
   public DataStoreProduct getDerivedProduct(String uuid, String tag) throws StoreException
   {
      // get list from keystores
      List<DataStore> datastoreList = listForUuidDerived(uuid, tag);

      for (DataStore datastore: datastoreList)
      {
         DerivedProductStore derivedProductStore = (DerivedProductStore) datastore;
         return derivedProductStore.getDerivedProduct(uuid, tag);
      }
      throw new ProductNotFoundException();
   }

   @Override
   public boolean hasDerivedProduct(String uuid, String tag)
   {
      // get list from keystores
      List<DataStore> datastoreList = listForUuidDerived(uuid, tag);

      for (DataStore datastore: datastoreList)
      {
         if (((DerivedProductStore) datastore).hasDerivedProduct(uuid, tag))
         {
            return true;
         }
      }
      return false;
   }

   @Override
   public List<String> getProductList()
   {
      return datastores
         .stream()
         // for each datastore, get a list of UUIDs
         .map(datastore -> datastore.getProductList())
         // combine lists of uuids into one
         .reduce(new ArrayList<>(), (totalList, list) -> {
            totalList.addAll(list);
            return totalList;
         })
         // remove duplicates using distinct
         .stream().distinct()
         // collect into a list that can be returned
         .collect(Collectors.toList());
   }

   @Override
   public void deleteProductFromDataStore(String uuid, String dataStoreName, Destination destination, Boolean safeMode)
         throws DataStoreException
   {
      DataStore dataStore = getDataStoreByName(dataStoreName);

      if (dataStore == null)
      {
         throw new DataStoreException("Unknown DataStore: " + dataStoreName);
      }

      // check deletion safety if in safe mode
      if (safeMode && !safeToDelete(dataStoreName, uuid))
      {
         throw new UnsafeDeletionException();
      }

      makeBackupProduct(uuid, destination, dataStoreName, null);

      // delete the product
      dataStore.deleteProduct(uuid);
   }

   private boolean safeToDelete(String dataStoreName, String uuid)
   {
      for (DataStore datastore: datastores)
      {
         if (!datastore.getName().equals(dataStoreName) && datastore.hasProduct(uuid))
         {
            return true;
         }
      }
      return false;
   }

   private SortedSet<DataStore> getDataStoresInSafeMode(String uuid)
   {
      List<DataStore> datastoreList = listForUuid(uuid, false, false);
      // The product will be kept in the lowest priority DataStore
      SortedSet<DataStore> dataStores = new ConcurrentSkipListSet<>(datastores.comparator().reversed());
      dataStores.addAll(datastoreList);

      for (DataStore dataStore : dataStores)
      {
         if (dataStore.hasProduct(uuid))
         {
            dataStores.remove(dataStore);
            return dataStores;
         }
      }
      return dataStores;
   }

   @Override
   public Map<String, String> getResourceLocations(String uuid) throws DataStoreException
   {
      // get list
      List<DataStore> datastoreList = listForUuid(uuid, true, false);

      Map<String, String> locations = new HashMap<>();
      for (DataStore datastore: datastoreList)
      {
         locations.put(datastore.getName(), ((DataStoreProduct) datastore.get(uuid)).getResourceLocation());
      }
      return locations;
   }

   @Override
   public int getDefaultDerivedProdutCount(String uuid)
   {
      int count = 0;
      if (hasDerivedProduct(uuid, DerivedProductStore.QUICKLOOK_TAG))
      {
         count++;
      }
      if (hasDerivedProduct(uuid, DerivedProductStore.THUMBNAIL_TAG))
      {
         count++;
      }
      return count;
   }

   @Override
   public boolean canHandleDerivedProducts()
   {
      return true;
   }

   @Override
   public boolean hasKeyStore()
   {
      return false;
   }

   @Override
   public DataStore getDataStoreByName(String name)
   {
      for (DataStore datastore : list())
      {
         if (datastore instanceof AsyncDataStore)
         {
            DataStore cache = ((AsyncDataStore) datastore).getCache();
            if (cache.getName().equals(name))
            {
               return cache;
            }
         }

         if (datastore.getName().equals(name))
         {
            return datastore;
         }
      }
      return null;
   }

   private List<DataStore> listForUuid(String uuid, boolean includeAsync, boolean order)
   {
      Set<String> datastoreNames = keyStoreService.listUnalteredForUuid(uuid).stream()
            .map(KeyStoreEntry::getKeyStore)
            .collect(Collectors.toSet());

      // return data stores found in keystores
      // and data stores that do no have a keystore
      // and async data stores
      return datastores.stream()
            .filter(datastore -> {
               // standard case, data store with keystore
               if (datastoreNames.contains(datastore.getName()))
               {
                  return true;
               }
               // case of data store without keystore, usually a remote data store
               if (!datastore.hasKeyStore() && datastore.hasProduct(uuid))
               {
                  return true;
               }
               // case of async data stores if included
               if (includeAsync
                     && AsyncDataStore.class.isAssignableFrom(datastore.getClass()) && order == true) 
                     {
                       return true;
                     }               
               else if (includeAsync
                     && AsyncDataStore.class.isAssignableFrom(datastore.getClass())
                     && (AsyncDataStore.class.cast(datastore).hasProduct(uuid))) 
               {
                  LOGGER.debug("Found async product in {}", datastore.getName());
                  // do NOT put async products in registry
                  return true;
               }
               return false;
            })
            .collect(Collectors.toList());
   }

   private List<DataStore> listForUuidDerived(String uuid, String tag)
   {
      Set<String> datastoreNames = keyStoreService.listDerivedForUuid(uuid, tag).stream()
            .map(KeyStoreEntry::getKeyStore)
            .collect(Collectors.toSet());

      // return data stores found in keystores
      // and data stores that do no have a keystore
      // do not use registry
      return datastores.stream()
            .filter(datastore -> {
               // only check derived product stores
               if (!datastore.canHandleDerivedProducts())
               {
                  return false;
               }

               // standard case, data store with keystore
               if (datastoreNames.contains(datastore.getName()))
               {
                  LOGGER.debug("Found local derived product in {}", datastore.getName());
                  return true;
               }
               // case of data store without keystore, usually a remote data store
               if (!datastore.hasKeyStore() && ((DerivedProductStore) datastore).hasDerivedProduct(uuid, tag))
               {
                  LOGGER.debug("Found remote derived product in {}", datastore.getName());
                  return true;
               }
               return false;
            })
            .collect(Collectors.toList());
   }

   @Override
   public Iterator<String> getScrollableProductResults()
   {
      return null;
   }
}
