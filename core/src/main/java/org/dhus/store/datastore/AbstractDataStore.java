/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016,2017,2018 GAEL Systems
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

import fr.gael.dhus.database.object.KeyStoreEntry;
import fr.gael.dhus.service.DataStoreService;
import fr.gael.dhus.service.EvictionService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.Product;
import org.dhus.store.StoreException;
import org.dhus.store.datastore.config.DataStoreConf;
import org.dhus.store.derived.DerivedProductStore;
import org.dhus.store.ingestion.IngestibleProduct;
import org.dhus.store.keystore.KeyStore;
import org.dhus.store.keystore.PersistentKeyStore;

/**
 * Abstract class contains globals methods for all DataStores.
 */
public abstract class AbstractDataStore implements DataStore, DerivedProductStore
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final DataStoreService DS_SERVICE =
         ApplicationContextProvider.getBean(DataStoreService.class);

   private static final EvictionService EVICTION_SERVICE =
         ApplicationContextProvider.getBean(EvictionService.class);

   private final String name;
   private final boolean readOnly;
   private final int priority;
   private final long maximumSize;
   protected final boolean autoEviction;
   private final AtomicLong currentSize;

   private KeyStore keystore;

   /**
    * Data stores properties configuration.
    * These properties are controlled by the sub-implementation to customize the product manipulation within the stores.
    * The list may contains:
    *
    *    transfer.compute.digests=md5,sha1,sha256 (comma separated list)
    *    transfer.compression.ratio=[1-9]
    *
    * Specific sub-implementation my also define their own dedicated settings.
    * The DataStores implementation may or may not implement each settings according to their capabilities.
    */
   private final Properties properties = new Properties();

   /**
    * Creates new instance.
    *
    * @param name         of this DataStore
    * @param readOnly     if it is read-only
    * @param priority     position of this DataStore
    * @param maximumSize  maximum size in bytes of this DataStore
    * @param currentSize  overall size of this DataStore (disk usage)
    * @param autoEviction true to activate auto-eviction based on disk usage
    */
   protected AbstractDataStore(String name, boolean readOnly, int priority, long maximumSize, long currentSize, boolean autoEviction)
   {
      this.name = Objects.requireNonNull(name);
      this.readOnly = readOnly;
      this.priority = priority;
      this.maximumSize = maximumSize;
      this.currentSize = new AtomicLong(currentSize);
      this.autoEviction = autoEviction;
      this.keystore = new PersistentKeyStore(name);
   }

   @Override
   public final String getName()
   {
      return name;
   }

   /**
    * Returns read only status.
    *
    * @return true, if this DataStore is on read only
    */
   @Override
   public final boolean isReadOnly()
   {
      return readOnly;
   }

   @Override
   public int getPriority()
   {
      return priority;
   }

   /**
    * The maximum size not to be overtaken. Used by the auto-eviction mechanism.
    *
    * @see #getCurrentSize()
    * @return maximum size in bytes
    */
   public long getMaximumSize()
   {
      return maximumSize;
   }

   /**
    * Returns the overall size of this datastore (sum of the size of all products).
    * Used by the auto-eviction mechanism.
    *
    * @see #getMaximumSize()
    * @return size in bytes
    */
   public long getCurrentSize()
   {
      return currentSize.get();
   }

   public KeyStore getKeystore()
   {
      return keystore;
   }

   public void setKeystore(KeyStore keystore)
   {
      this.keystore = keystore;
   }

   /**
    * A reference to the internal properties set.
    *
    * @return the properties set
    */
   public Properties getProperties()
   {
      return properties;
   }

   /**
    * Sets the property value for the given key. If the key already exists, it is overwritten.
    *
    * @param key
    * @param value
    */
   public void setProperty(String key, String value)
   {
      this.properties.setProperty(key, value);
   }

   /**
    * Remove the key/value association from these properties for the given key.
    *
    * @param key
    */
   public void removeProperty(String key)
   {
      this.properties.remove(key);
   }

   @Override
   public final Product getDerivedProduct(String uuid, String tag) throws DataStoreException
   {
      return internalGetProduct(uuid, tag);
   }

   @Override
   public final Product get(String uuid) throws DataStoreException
   {
      return internalGetProduct(uuid, UNALTERED_PRODUCT_TAG);
   }

   /**
    * Used by {@link #get(String)} and {@link #getDerivedProduct(String, String)}.
    *
    * @param uuid to get
    * @param tag the tag under which the derived product is referenced
    * @return a product
    * @throws DataStoreException
    */
   protected abstract Product internalGetProduct(String uuid, String tag) throws DataStoreException;

   /**
    * Retrieves a product resource location from the given id key.
    *
    * @param uuid uuid of product
    * @param tag
    *
    * @return a string representation of the product location, or null if it is not referenced
    */
   protected final String getResource(String uuid, String tag)
   {
      return keystore.get(uuid, tag);
   }

   @Override
   public final void set(String uuid, Product product) throws DataStoreException
   {
      internalAddProduct(uuid, UNALTERED_PRODUCT_TAG, product);
   }

   @Override
   public final void addDerivedProduct(String uuid, String tag, Product product) throws StoreException
   {
      internalAddProduct(uuid, tag, product);
   }

   /**
    * Used by {@link #set(String, org.dhus.Product)} and {@link #addDerivedProduct(String, String, org.dhus.Product)}.
    *
    * @param uuid    the UUID of the original product
    * @param tag     the tag under which the derived product is referenced
    * @param product the derived product
    * @throws DataStoreException
    */
   protected abstract void internalAddProduct(String uuid, String tag, Product product) throws DataStoreException;

   /**
    * References a new product.
    *
    * @param uuid     product key
    * @param tag      the tag under which the derived product is referenced
    * @param resource product resource location
    *
    * @throws ProductAlreadyExist if the product key is already used
    */
   protected final void putResource(String uuid, String tag, String resource) throws ProductAlreadyExist
   {
      if (keystore.exists(uuid, tag))
      {
         throw new ProductAlreadyExist();
      }
      keystore.put(uuid, tag, resource);
   }

   // TODO merge addProduct() and set()
   @Override
   public void addProduct(IngestibleProduct inProduct) throws StoreException
   {
      set(inProduct.getUuid(), inProduct);
   }

   @Override
   public final void deleteProduct(String uuid) throws DataStoreException
   {
      if (!internalHasProduct(uuid, UNALTERED_PRODUCT_TAG))
      {
         throw new ProductNotFoundException();
      }

      String resourceLocation = getResource(uuid, UNALTERED_PRODUCT_TAG);
      removeResource(uuid, UNALTERED_PRODUCT_TAG);

      if (!isReadOnly())
      {
         internalDeleteProduct(resourceLocation);
      }
   }

   @Override
   public final void deleteDerivedProduct(String uuid, String tag) throws DataStoreException
   {
      if (!internalHasProduct(uuid, tag))
      {
         throw new ProductNotFoundException();
      }

      String resourceLocation =  getResource(uuid, tag);
      removeResource(uuid, tag);

      if (!isReadOnly())
      {
         internalDeleteProduct(resourceLocation);
      }
   }

   /**
    * Used by {@link #deleteProduct(String)} and {@link #deleteDerivedProduct(String, String)}.
    *
    * @param resourceLocation location
    * @throws DataStoreException
    */
   protected abstract void internalDeleteProduct(String resourceLocation) throws DataStoreException;

   @Override
   public final void deleteDerivedProducts(String uuid) throws DataStoreException
   {
      List<KeyStoreEntry> entries = keystore.getEntriesByUuid(uuid);
      for (KeyStoreEntry entry: entries)
      {
         // only delete derived products
         if (!entry.getTag().equals(UNALTERED_PRODUCT_TAG))
         {
            try
            {
               deleteDerivedProduct(entry.getEntryKey(), entry.getTag());
            }
            catch (DataStoreException e)
            {
               LOGGER.warn("Cannot delete derived product {} ({}) from {} DataStore",
                     entry.getEntryKey(), entry.getTag(), getName());
            }
         }
      }
   }

   /**
    * Remove from KeyStore (product is no longer known to this DataStore, but is not deleted).
    *
    * @param uuid of product to remove
    * @param tag  the tag under which the derived product is referenced
    * @throws ProductNotFoundException if product is not found
    */
   protected final void removeResource(String uuid, String tag) throws ProductNotFoundException
   {
      if (keystore.exists(uuid, tag))
      {
         keystore.remove(uuid, tag);
      }
   }

   @Override
   public final boolean hasProduct(String uuid)
   {
      return internalHasProduct(uuid, UNALTERED_PRODUCT_TAG);
   }

   @Override
   public final boolean hasDerivedProduct(String uuid, String tag)
   {
      return internalHasProduct(uuid, tag);
   }

   /**
    * Used by {@link #hasProduct(String)} and {@link #hasDerivedProduct(String, String)}.
    *
    * @param uuid the UUID of the product
    * @param tag  the tag under which the derived product is referenced
    * @return true is product is found in the keystore
    */
   protected final boolean internalHasProduct(String uuid, String tag)
   {
      return keystore.exists(uuid, tag);
   }

   @Override
   public final boolean addProductReference(String uuid, Product product) throws DataStoreException
   {
      return internalAddProductReference(uuid, UNALTERED_PRODUCT_TAG, product);
   }

   @Override
   public final boolean addDerivedProductReference(String uuid, String tag, Product product) throws StoreException
   {
      return internalAddProductReference(uuid, tag, product);
   }

   /**
    * Used by {@link #addProductReference(String, org.dhus.Product)}
    * and {@link #addDerivedProductReference(String, String, org.dhus.Product)}.
    *
    * @param uuid    the UUID of the product
    * @param tag     the tag under which the derived product is referenced
    * @param product to reference in this datastore
    * @return true if reference successfully added
    */
   protected final boolean internalAddProductReference(String uuid, String tag, Product product)
   {
      // check product has resource location
      if (!product.hasImpl(DataStoreProduct.class))
      {
         // throw new IllegalArgumentException ();
         return false;
      }

      String resource = product.getImpl(DataStoreProduct.class).getResourceLocation();
      if (!canAccess(resource))
      {
         // throws new ProductNotFoundException ();
         return false;
      }

      keystore.put(uuid, tag, resource);
      return true;
   }

   /**
    * Starts an eviction if this DataStore is running out of space.
    *
    * @param dataSize the size of the data about to be inserted
    */
   protected final void onInsertEviction(long dataSize)
   {
      // is this condition true in our case?
      long sizeAfterInsert = currentSize.get() + dataSize;
      if (autoEviction && maximumSize >= 0 && sizeAfterInsert > maximumSize)
      {
         String evictionName = getConfiguredEviction();
         long sizeToEvict = sizeAfterInsert - maximumSize;
         if (evictionName == null)
         {
            // call default automatic eviction
            EVICTION_SERVICE.evictAtLeast(getName(), sizeToEvict);
         }
         else
         {
            // call customized automatic eviction
            EVICTION_SERVICE.evictAtLeast(evictionName, getName(), sizeToEvict);
         }
      }
   }

   /**
    * Returns the size in bytes of product identified by given id.
    *
    * @param id of product to inquiry for its size
    * @return size in byte
    * @throws DataStoreException on error
    */
   protected abstract long getProductSize(String id) throws DataStoreException;

   /**
    * A DataStore must know its overall size to implement eviction based on disk usage.
    * <p>This implementation has a global size class field to store this information.
    * <p>This method must be called to update this field.
    *
    * @see #decreaseCurrentSize(long)
    * @see #getCurrentSize()
    *
    * @param amount to increase
    */
   protected synchronized final void increaseCurrentSize(long amount)
   {
      currentSize.set(DS_SERVICE.varyCurrentSize(name, amount));
      LOGGER.debug("DataStore {} size increased of {} bytes, new total: {} bytes", name, amount, currentSize.get());
   }

   /**
    * @see #increaseCurrentSize(long)
    * @see #getCurrentSize()
    *
    * @param amount to decrease
    */
   protected synchronized final void decreaseCurrentSize(long amount)
   {
      currentSize.set(DS_SERVICE.varyCurrentSize(name, -amount));
      LOGGER.debug("DataStore {} size decreased of {} bytes, new total: {} bytes", name, amount, currentSize.get());
   }

   @Override
   public String toString()
   {
      return this.name;
   }

   @Override
   public boolean equals(final Object obj)
   {
      if (obj == null)
      {
         return false;
      }
      if (this == obj)
      {
         return true;
      }
      if (obj instanceof AbstractDataStore)
      {
         AbstractDataStore other = (AbstractDataStore) obj;
         return other.isReadOnly() == readOnly
               && other.getName().equals(name)
               && other.priority == priority;
      }
      return false;
   }

   @Override
   public int hashCode()
   {
      return name.hashCode() + Boolean.valueOf(readOnly).hashCode() + priority;
   }

   @Override
   public List<String> getProductList()
   {
      List<KeyStoreEntry> entryList = keystore.getUnalteredProductEntries();
      return toKeyList(entryList);
   }

   @Override
   public List<String> getProductList(int skip, int top)
   {
      List<KeyStoreEntry> entryList = keystore.getUnalteredProductEntries(skip, top);
      return toKeyList(entryList);
   }

   private List<String> toKeyList(List<KeyStoreEntry> keyList)
   {
      // map keystore entries to uuids
      return keyList.stream()
            .map(keyStoreEntry -> keyStoreEntry.getEntryKey())
            .collect(Collectors.toList());
   }

   public String getConfiguredEviction()
   {
      DataStoreConf dataStore = DS_SERVICE.getNamedDataStore(name);
      return dataStore.getEvictionName();
   }
}
