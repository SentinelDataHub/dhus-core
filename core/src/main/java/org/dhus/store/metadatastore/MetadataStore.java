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
package org.dhus.store.metadatastore;

import java.util.List;

import org.dhus.Product;
import org.dhus.store.Store;
import org.dhus.store.StoreException;
import org.dhus.store.ingestion.IngestibleProduct;

/**
 * A MetadataStore is a means to store, access, and manage the metadata of products.
 */
public interface MetadataStore extends Store
{
   /**
    * Returns whether or not this Store has a Product referenced under uuid.
    *
    * @param uuid the uuid of a Product
    * @return true is found
    * @throws StoreException an error occurred
    */
   public boolean hasProduct(String uuid) throws StoreException;

   /**
    * Returns a Product referenced under uuid by this Store.
    *
    * @param uuid the uuid of a Product
    * @return requested product or null if not found
    * @throws StoreException an error occurred
    */
   public Product getProduct(String uuid) throws StoreException;

   /**
    * @param inProduct
    * @param targetCollectionNames
    * @throws StoreException
    */
   public void addProduct(IngestibleProduct inProduct, List<String> targetCollectionNames) throws StoreException;
}
