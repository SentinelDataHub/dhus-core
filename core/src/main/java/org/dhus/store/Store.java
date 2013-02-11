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
package org.dhus.store;

import org.dhus.store.ingestion.IngestibleProduct;

/**
 * Store main interface gather all the Data, Meta-data, Keys Stores.
 */
public interface Store
{
   /**
    * Adds a product in this Store.
    *
    * @param inProduct the product to store
    *
    * @throws StoreException an error occurred
    */
   public void addProduct(IngestibleProduct inProduct) throws StoreException;

   /**
    * Removes a product from this Store.
    *
    * @param uuid UUID of the product to remove
    *
    * @throws StoreException an error occurred
    */
   public void deleteProduct(String uuid) throws StoreException;
}
