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
package org.dhus.store.datastore.async;

import org.dhus.store.datastore.DataStoreException;

/**
 * An asynchronous DataStore should implement this interface to allow fetching of data.
 */
public interface AsyncDataSource
{
   /**
    * Returns {@code true} if this data store can fetch a remote product for given UUID.
    * {@link org.dhus.store.datastore.DataStore#hasProduct(String)} does not consider remote data.
    *
    * @param uuid of remote product
    * @return true is remote product exists for given UUID
    */
   public boolean hasAsyncProduct(String uuid);

   /**
    * Ask this data source to fetch the given product asynchronously.
    * This method is non-blocking.
    *
    * @param to_fetch product to fetch
    * @throws DataStoreException could not perform fetch
    */
   public void fetch(AsyncProduct to_fetch) throws DataStoreException;
}
