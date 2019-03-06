/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016-2019 GAEL Systems
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

import fr.gael.dhus.datastore.Destination;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.Product;
import org.dhus.ProductConstants;
import org.dhus.store.StoreException;
import org.dhus.store.datastore.DataStoreFactory.InvalidConfigurationException;
import org.dhus.store.datastore.async.AsyncDataSource;
import org.dhus.store.datastore.config.DataStoreConf;
import org.dhus.store.datastore.hfs.HfsDataStore;
import org.dhus.store.derived.DerivedProductStore;
import org.dhus.store.derived.DerivedProductStoreService;
import org.dhus.store.ingestion.IngestibleProduct;

/**
 * This class implements the default DataStore container that is constructed according to aggregated DataStores.
 * The execution of this DataStore commands will automatically be propagated to the handled DataStores.
 */
public class DefaultDataStoreManager implements DataStoreManager, DerivedProductStoreService
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final String DATA_STORE_NAME = "DefaultDataStoreService";

   /** Default constructor needed for Spring to construct this bean. */
   @SuppressWarnings("unused")
   private DefaultDataStoreManager() {}

   private final SortedSet<DataStore> datastores = new ConcurrentSkipListSet<>(
      new Comparator<DataStore>()
      {
         @Override
         public int compare(DataStore ds1, DataStore ds2)
         {
            return DataStores.compare(ds1, ds2);
         }
      }
   );

   /**
    * Construct a DataStore from a list of DataStoreConf.
    *
    * @param confs
    */
   public DefaultDataStoreManager(List<DataStoreConf> confs)
   {
      for (DataStoreConf conf : confs)
      {
         try
         {
            this.datastores.add(DataStoreFactory.createDataStore(conf));
         }
         catch (InvalidConfigurationException e)
         {
            LOGGER.warn("Invalid configuration for DataStore '{}'", conf);
         }
      }
   }

   @Override
   public String getName()
   {
      return DATA_STORE_NAME;
   }

   @Override
   public Product get(String id) throws DataStoreException
   {
      Iterator<DataStore> iterator = datastores.iterator();
      while (iterator.hasNext())
      {
         DataStore datastore = iterator.next();
         try
         {
            if (datastore.hasProduct(id))
            {
               return datastore.get(id);
            }
            if (AsyncDataSource.class.isAssignableFrom(datastore.getClass()) // Async Data Store
             && AsyncDataSource.class.cast(datastore).hasAsyncProduct(id))
            {
               return datastore.get(id);
            }
         }
         catch (DataStoreException e)
         {
            continue;
         }
      }
      throw new ProductNotFoundException("Cannot retrieve product for id " + id);
   }

   // TODO removed this method?
   // This method MUST be transactional: in case of failure all the DataStores that have been set
   // in this context must be rolled back.
   @Override
   public void set(String uuid, Product product) throws DataStoreException
   {
      for (DataStore datastore:datastores)
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

   // TODO merge addProduct() and set()
   @Override
   public void addProduct(IngestibleProduct inProduct) throws StoreException
   {
      LOGGER.info("Inserting product {} in DataStores", inProduct.getUuid());
      List<Throwable> throwables = new ArrayList<>();
      for (DataStore datastore: datastores)
      {
         if (Thread.interrupted())
         {
            Thread.currentThread().interrupt();
            throw new StoreException("Product insertion interrupted");
         }

         try
         {
            long duration = System.currentTimeMillis();
            datastore.addProduct(inProduct);
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
      DataStores.throwErrors(throwables, "addProduct", inProduct.getUuid());
      LOGGER.info("Product {} inserted", inProduct.getUuid());
   }

   @Override
   public void deleteProduct(String uuid, Destination destination) throws DataStoreException
   {
      // attempt to move product to destination
      makeBackupProduct(uuid, destination);

      LOGGER.info("Deleting product {} from all DataStores", uuid);
      List<Throwable> throwables = new ArrayList<>();
      boolean deleted = false;
      for (DataStore datastore: datastores)
      {
         try
         {
            datastore.deleteProduct(uuid);
            deleted = true;
         }
         catch (ReadOnlyDataStoreException | ProductNotFoundException e)
         {
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

      DataStores.throwErrors(throwables, "deleteProduct", uuid);
   }

   @Override
   public void deleteProduct(String uuid) throws DataStoreException
   {
      deleteProduct(uuid, Destination.NONE);
   }

   private void makeBackupProduct(String uuid, Destination destination)
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
      // we assume the optimal way to make a backup product during a deletion
      // is to use an HFSDataStore that isn't read-only
      for (DataStore dataStore: datastores)
      {
         if (!dataStore.isReadOnly() && dataStore instanceof HfsDataStore
               && dataStore.hasProduct(uuid))
         {
            try
            {
               moveProduct((HfsDataStore) dataStore, uuid, destination);
               return;
            }
            catch (DataStoreException | IOException e)
            {
               exception = e;
            }
         }
      }
      // no suitable HFSDataStore was found, use any datastore that has the product
      for (DataStore dataStore: datastores)
      {
         if (dataStore.hasProduct(uuid))
         {
            try
            {
               copyProduct(dataStore, uuid, destination);
               return;
            }
            catch (DataStoreException | IOException e)
            {
               exception = e;
            }
         }
      }
      LOGGER.warn("Cannot retrieve data of product {}: {}", uuid, exception.getMessage());
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
      Iterator<DataStore> iterator = datastores.iterator();
      while (iterator.hasNext())
      {
         DataStore datastore = iterator.next();
         if (datastore.hasProduct(uuid))
         {
            return true;
         }
      }
      return false;
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
      List<Throwable> throwables = new ArrayList<>();
      boolean added = false;
      for (DataStore dataStore: datastores)
      {
         try
         {
            if (dataStore.addProductReference(uuid, product))
            {
               added = true;
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
   public boolean isReadOnly()
   {
      return false;
   }

   @Override
   public void addDerivedProduct(String uuid, String tag, Product product) throws StoreException
   {
      List<Throwable> throwables = new ArrayList<>();
      for (DataStore datastore: datastores)
      {
         if (!datastore.isReadOnly() && datastore instanceof DerivedProductStore)
         {
            try
            {
               ((DerivedProductStore) datastore).addDerivedProduct(uuid, tag, product);
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
   public void addDefaultDerivedProducts(IngestibleProduct inProduct) throws StoreException
   {
      LOGGER.info("Retrieving derived products from product {}", inProduct.getUuid());

      if (inProduct.getQuicklook() != null)
      {
         addDerivedProduct(inProduct.getUuid(), DerivedProductStore.QUICKLOOK_TAG, inProduct.getQuicklook());
      }
      if (inProduct.getThumbnail() != null)
      {
         addDerivedProduct(inProduct.getUuid(), DerivedProductStore.THUMBNAIL_TAG, inProduct.getThumbnail());
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
         if(datastore instanceof DerivedProductStore)
         {
            try
            {
               if (((DerivedProductStore) datastore).addDerivedProductReference(uuid, tag, product))
               {
                  added = true;
               }
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
      for (DataStore datastore: datastores)
      {
         if (!datastore.isReadOnly() && datastore instanceof DerivedProductStore)
         {
            try
            {
               ((DerivedProductStore) datastore).deleteDerivedProduct(uuid, tag);
            }
            catch (StoreException | RuntimeException e)
            {
               throwables.add(e);
            }
         }
      }
      DataStores.throwErrors(throwables, "deleteDerivedProduct", uuid);
   }

   @Override
   public void deleteDerivedProducts(String uuid) throws StoreException
   {
      LOGGER.info("Deleting derived products of {}", uuid);
      List<Throwable> throwables = new ArrayList<>();
      for (DataStore datastore: datastores)
      {
         if (!datastore.isReadOnly() && datastore instanceof DerivedProductStore)
         {
            try
            {
               ((DerivedProductStore) datastore).deleteDerivedProducts(uuid);
            }
            catch (StoreException | RuntimeException e)
            {
               throwables.add(e);
            }
         }
      }
      DataStores.throwErrors(throwables, "deleteDerivedProducts", uuid);
   }

   @Override
   public Product getDerivedProduct(String uuid, String tag) throws StoreException
   {
      for (DataStore datastore: datastores)
      {
         if (datastore instanceof DerivedProductStore)
         {
            DerivedProductStore derivedProductStore = (DerivedProductStore) datastore;
            if (derivedProductStore.hasDerivedProduct(uuid, tag))
            {
               return derivedProductStore.getDerivedProduct(uuid, tag);
            }
         }
      }
      throw new ProductNotFoundException();
   }

   @Override
   public boolean hasDerivedProduct(String uuid, String tag)
   {
      for (DataStore datastore: datastores)
      {
         if (datastore instanceof DerivedProductStore
               && ((DerivedProductStore) datastore).hasDerivedProduct(uuid, tag))
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

      makeBackupProduct(uuid, destination);

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
}
