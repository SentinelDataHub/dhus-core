/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016 GAEL Systems
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
import org.dhus.store.Store;

/**
 * DataStore Interface aims to define data of products.
 */
public interface DataStore extends Store
{
   /**
    * Returns the name of this DataStore.
    * @return name
    */
   public String getName();

   /**
    * Retrieve a product stored in this DataStore.
    *
    * @param id is the UUID that references a product
    *
    * @return product from DataStore
    *
    * @throws DataStoreException an error occurred
    */
   public Product get(String id) throws DataStoreException;

   /**
    * Put a product in this DataStore.
    *
    * @param id      the UUID that uniquely references the product
    * @param product the product to store
    *
    * @throws DataStoreException an error occurred
    */
   public void set(String id, Product product) throws DataStoreException;

   /**
    * Sets the product into this DataStore and removes the sources.
    *
    * @param id      the UUID references this product
    * @param product the product to move
    *
    * @throws DataStoreException an error occurred
    */
   public void move(String id, Product product) throws DataStoreException;

   /**
    * Removes a product from this DataStore.
    *
    * @param id  UUID of the product to remove
    *
    * @throws DataStoreException an error occurred
    */
   public void delete(String id) throws DataStoreException;

   /**
    * Checks if the passed id is referenced as a product in this DataStore.
    *
    * @param id a UUID to check
    *
    * @return true if passed id is a known reference in this DataStore
    */
   public boolean exists(String id);

   /**
    * Checks resource accessibility.
    *
    * @param resource_location the resource location
    *
    * @return true if the DataStore can access resource
    */
   public boolean canAccess(String resource_location);

   /**
    * Put a <i>reference</i> to a product in the DataStore (unlike {@link #set(String, Product)}
    * that physically put a product in this DataStore).
    *
    * @param id key to use for added the product
    * @param product product to add
    *
    * @return true if the product is added, otherwise false
    *
    * @throws DataStoreException an error occurred
    */
   public boolean addProductReference(String id, Product product) throws DataStoreException;
}
