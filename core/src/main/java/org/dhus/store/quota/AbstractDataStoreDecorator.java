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
package org.dhus.store.quota;

import java.util.Objects;

import org.dhus.Product;
import org.dhus.store.StoreException;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.ingestion.IngestibleProduct;

/**
 * A base class to make DataStore decorators.
 *
 * @param <DST> generic type can be changed to support more complex datastores
 */
public abstract class AbstractDataStoreDecorator<DST extends DataStore> implements DataStore
{
   protected final DST decorated;

   public AbstractDataStoreDecorator(DST decorated)
   {
      Objects.requireNonNull(decorated);
      this.decorated = decorated;
   }

   @Override
   public String getName()
   {
      return this.decorated.getName();
   }

   @Override
   public int getPriority()
   {
      return this.decorated.getPriority();
   }

   @Override
   public boolean isReadOnly()
   {
      return this.decorated.isReadOnly();
   }

   @Override
   public Product get(String uuid) throws DataStoreException
   {
      return this.decorated.get(uuid);
   }

   @Override
   public void addProduct(IngestibleProduct inProduct) throws StoreException
   {
      this.decorated.addProduct(inProduct);
   }

   @Override
   public void set(String uuid, Product product) throws DataStoreException
   {
      this.decorated.set(uuid, product);
   }

   @Override
   public void deleteProduct(String uuid) throws DataStoreException
   {
      this.decorated.deleteProduct(uuid);
   }

   @Override
   public boolean hasProduct(String uuid)
   {
      return this.decorated.hasProduct(uuid);
   }

   @Override
   public boolean canAccess(String resource_location)
   {
      return this.decorated.canAccess(resource_location);
   }

   @Override
   public boolean addProductReference(String uuid, Product product) throws DataStoreException
   {
      return this.decorated.addProductReference(uuid, product);
   }
}
