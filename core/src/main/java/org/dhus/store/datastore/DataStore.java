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

import java.util.List;

import org.dhus.Product;
import org.dhus.Util;
import org.dhus.store.Store;

/**
 * A DataStore is a means to store, access, and manage the physical data of unaltered products.
 */
public interface DataStore extends Store, AutoCloseable
{
   public static final String UNALTERED_PRODUCT_TAG = "unaltered";

   /**
    * Returns the name of this DataStore.
    *
    * @return name
    */
   public String getName();

   /**
    * Returns the priority of this DataStore.
    *
    * @return the priority of this DataStore
    */
   public int getPriority();

   /**
    * Retrieve a product stored in this DataStore.
    *
    * @param uuid is the UUID that references a product
    *
    * @return product from DataStore
    *
    * @throws DataStoreException an error occurred
    */
   public Product get(String uuid) throws DataStoreException;

   /**
    * Put a product in this DataStore.
    *
    * @param uuid    the UUID that uniquely references the product
    * @param product the product to store
    *
    * @throws DataStoreException an error occurred
    */
   public void set(String uuid, Product product) throws DataStoreException;

   /**
    * Deletes the product identified by the given UUID from this DataStore.
    *
    * @param uuid the UUID that uniquely references the product to delete
    *
    * @throws ProductNotFoundException if the product is not present in this DataStore
    */
   @Override
   public void deleteProduct(String uuid) throws DataStoreException;

   /**
    * Checks if the passed id is referenced as a product in this DataStore.
    *
    * @param uuid a UUID to check
    *
    * @return true if passed id is a known reference in this DataStore
    */
   public boolean hasProduct(String uuid);

   /**
    * Checks resource accessibility.
    *
    * @param resource_location the resource location
    * @return true if the DataStore can access resource
    */
   public boolean canAccess(String resource_location);

   /**
    * Put a <i>reference</i> to a product in the DataStore (unlike {@link #set(String, Product)}
    * that physically put a product in this DataStore).
    *
    * @param uuid    key to use for added the product
    * @param product product to add
    *
    * @return true if the product is added, otherwise false
    *
    * @throws DataStoreException an error occurred
    */
   // will be removed with self-descriptive datastores
   public boolean addProductReference(String uuid, Product product) throws DataStoreException;

   /**
    * Returns a list of products known to this DataStore.
    *
    * @return return a non-null list of products
    */
   public List<String> getProductList();

   /**
    * Returns a list of products known to this DataStore.
    *
    * @param skip number of products to skip
    * @param top maximum number of product to return
    * @return return a non-null list of products
    */
   public default List<String> getProductList(int skip, int top)
   {
      List<String> productList = getProductList();
      return Util.subList(productList, skip, top);
   }

   @Override
   public default void close() throws Exception {} // Default No-Op implementation

}
