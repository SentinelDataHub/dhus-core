/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016,2017 GAEL Systems
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

import org.dhus.Product;
import org.dhus.store.keystore.KeyStore;
import org.dhus.store.keystore.PersistentKeyStore;

import java.util.Objects;
import java.util.Properties;

/**
 * Abstract class contains globals methods for all DataStores.
 */
public abstract class AbstractDataStore implements DataStore
{
   private final String name;
   private final boolean readOnly;
   private KeyStore keystore;

   /**
    * Data stores properties configuration.
    * These properties are controlled by the sub-implementation to customize
    * the product manipulation within the stores.
    * The list may contains
    *
    *    transfer.compute.digests=md5,sha1,sha256 (comma separated list)
    *    transfer.compression.ratio=[1-9]
    *
    * Specific sub-implementation my also define their own dedicated settings.
    * The DataStores implementation may or may not implement each settings
    * according to their capabilities.
    */
   private final Properties properties = new Properties();

   protected AbstractDataStore(String name, boolean readOnly)
   {
      this.name = Objects.requireNonNull(name);
      this.readOnly = readOnly;
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
   public final boolean isReadOnly()
   {
      return readOnly;
   }

   /**
    * Retrieves a product resource location from the given id key.
    *
    * @param key id key of product
    *
    * @return a string representation of the product location, or null if it is not referenced
    */
   protected final String getResource(String key)
   {
      return keystore.get(key);
   }

   /**
    * References a new product.
    *
    * @param key      product key
    * @param resource product resource location
    *
    * @throws ProductAlreadyExist if the product key is already used
    */
   protected final void putResource(String key, String resource) throws ProductAlreadyExist
   {
      if (keystore.exists(key))
      {
         throw new ProductAlreadyExist();
      }
      keystore.put(key, resource);
   }

   protected final void removeResource(String key) throws ProductNotFoundException
   {
      if (keystore.exists(key))
      {
         keystore.remove(key);
      }
   }

   @Override
   public final boolean addProductReference(String id, Product product) throws DataStoreException
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

      if (onAddProductReference(resource))
      {
         keystore.put(id, resource);
         return true;
      }
      return false;
   }

   @Override
   public final boolean exists(String id)
   {
      return keystore.exists(id);
   }

   /**
    * Performs additional actions after a method call
    * {@link AbstractDataStore#addProductReference(String, Product)}.
    *
    * @param resource a product resource location
    */
   protected abstract boolean onAddProductReference(String resource);

   public void setKeystore(KeyStore keystore)
   {
      this.keystore = keystore;
   }

   /**
    * A reference to the internal properties set.
    * @return the properties set.
    */
   public Properties getProperties()
   {
      return properties;
   }

   /**
    * Sets the property value for the given key. If the key already exists,
    * it is overwritten.
    * @param key
    * @param value
    */
   public void setProperty(String key, String value)
   {
      this.properties.setProperty(key, value);
   }

   /**
    * Remove the key/value association from these properties for the given key.
    * @param key
    */
   public void removeProperty(String key)
   {
      this.properties.remove(key);
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
         return (other.isReadOnly() == readOnly) && (other.getName().equals(name));
      }
      return false;
   }

   @Override
   public int hashCode()
   {
      return name.hashCode() + Boolean.valueOf(readOnly).hashCode();
   }
}
