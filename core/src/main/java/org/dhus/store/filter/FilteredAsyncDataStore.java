/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019 GAEL Systems
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
package org.dhus.store.filter;

import fr.gael.dhus.database.object.Order;
import fr.gael.dhus.olingo.v1.visitor.ProductSQLVisitor;

import java.util.List;

import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.DataStoreFactory.InvalidConfigurationException;
import org.dhus.store.datastore.async.AsyncDataStore;
import org.dhus.store.datastore.async.AsyncProduct;

/**
 * An async flavour of the {@link FilteredDataStore}.
 */
public final class FilteredAsyncDataStore extends FilteredDataStore<AsyncDataStore> implements AsyncDataStore
{

   public FilteredAsyncDataStore(AsyncDataStore decorated, ProductSQLVisitor visitor)
         throws InvalidConfigurationException
   {
      super(decorated, visitor);
   }

   @Override
   public boolean hasAsyncProduct(String uuid)
   {
      return decorated.hasAsyncProduct(uuid);
   }

   @Override
   public Order fetch(AsyncProduct to_fetch) throws DataStoreException
   {
      return decorated.fetch(to_fetch);
   }

   @Override
   public List<Order> getOrderList()
   {
      return decorated.getOrderList();
   }

   @Override
   public Order getOrder(String uuid)
   {
      return decorated.getOrder(uuid);
   }

   @Override
   public Order getAndLogExistingOrder(String uuid, String localIdentifier, Long size)
   {
      return decorated.getAndLogExistingOrder(uuid, localIdentifier, size);
   }
}
