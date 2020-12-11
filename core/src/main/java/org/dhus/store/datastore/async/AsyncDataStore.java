/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2019 GAEL Systems
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

import fr.gael.dhus.database.object.Order;

import java.util.List;

import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreException;

/**
 * An asynchronous DataStore should implement this interface to allow fetching of data.
 */
public interface AsyncDataStore extends DataStore
{
   /** Prefix for metrics reported by async datastores. */
   static final String METRIC_PREFIX = "datastore.async";

   /**
    * Returns {@code true} if this data store can fetch a remote product for given UUID.
    * {@link org.dhus.store.datastore.DataStore#hasProduct(String)} does not consider remote data.
    *
    * @param uuid of remote product
    * @return true is remote product exists for given UUID
    */
   boolean hasAsyncProduct(String uuid);

   /**
    * Ask this data store to fetch the given product asynchronously.
    * This method is non-blocking.
    *
    * @param to_fetch product to fetch
    * @return a non null Order instance
    * @throws DataStoreException could not perform fetch
    */
   Order fetch(AsyncProduct to_fetch) throws DataStoreException;

   /**
    * Returns a list of all Orders produced by the fetch method on this DataSource.
    *
    * @return a non null, possibly empty list of Order
    */
   List<Order> getOrderList();

   /**
    * Return the Order corresponding to the given identifier. May return null if none found.
    *
    * @param uuid product UUID (non null)
    * @return the Order corresponding to the given identifier or null
    */
   Order getOrder(String uuid);

   /**
    * Returns existing order and logs its existence/presence in the queue.
    * May return null if none found. Required by operations.
    *
    * @param uuid product UUID (non null)
    * @param localIdentifier logged (may be null)
    * @param size logged (may be null)
    * @return the Order corresponding to the given product UUID or null
    */
   Order getAndLogExistingOrder(String uuid, String localIdentifier, Long size);
}
