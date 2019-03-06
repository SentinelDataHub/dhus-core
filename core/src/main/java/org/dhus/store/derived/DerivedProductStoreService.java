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
package org.dhus.store.derived;

import org.dhus.store.StoreException;
import org.dhus.store.ingestion.IngestibleProduct;

/**
 * This service handles the storage of derived product such as quicklooks or thumbnails.
 * This implementation relies on a DataStoreManager to operate.
 */
public interface DerivedProductStoreService extends DerivedProductStore
{
   /**
    * Extracts quicklook and thumbnail from IngestibleProduct.
    *
    * @param inProduct to process
    * @throws StoreException could not perform operation
    */
   public void addDefaultDerivedProducts(IngestibleProduct inProduct) throws StoreException;

   /**
    * Extracts quicklook and thumbnail from IngestibleProduct.
    *
    * @param inProduct to process
    * @throws StoreException could not perform operation
    */
   public void addDefaultDerivedProductReferences(IngestibleProduct inProduct) throws StoreException;
}
