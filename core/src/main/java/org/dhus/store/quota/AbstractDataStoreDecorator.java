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
package org.dhus.store.quota;

import java.util.List;
import java.util.Objects;

import org.dhus.Product;
import org.dhus.store.StoreException;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.DataStoreProduct;
import org.dhus.store.derived.DerivedProductStore;
import org.dhus.store.ingestion.IngestibleProduct;

/**
 * A base class to make DataStore decorators.
 *
 * @param <DST> generic type can be changed to support more complex datastores
 */
public abstract class AbstractDataStoreDecorator<DST extends DataStore> implements DataStore
{
   protected final DST decorated;

   /**
    * Creates a new decorator.
    *
    * @param decorated a non null DataStore instance
    * @throws NullPointerException if the `decorated` parameter is null
    */
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
   public boolean canModifyReferences()
   {
      return this.decorated.canModifyReferences();
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

   @Override
   public List<String> getProductList()
   {
      return this.decorated.getProductList();
   }

   @Override
   public void close() throws Exception
   {
      this.decorated.close();
   }

   @Override
   public boolean canHandleDerivedProducts()
   {
      return this.decorated.canHandleDerivedProducts();
   }

   @Override
   public boolean hasKeyStore()
   {
      return this.decorated.hasKeyStore();
   }

   /**
    * Add the `DerivedProductStore` trait to your DataStore decorator using the 'mixins' paradigm.
    * See a usage in class {@link AbstractDataStoreDecorator.DerivedDataStore } below.
    */
   public static interface DataStoreDecoratorHelper extends DerivedProductStore
   {
      /**
       * The only method to implement per the 'mixins' pattern is this getter.
       *
       * @return the decorated object
       */
      DerivedProductStore getDecorated();

      @Override
      default void addDerivedProduct(String uuid, String tag, Product product) throws StoreException
      {
         getDecorated().addDerivedProduct(uuid, tag, product);
      }

      @Override
      default void deleteDerivedProduct(String uuid, String tag) throws StoreException
      {
         getDecorated().deleteDerivedProduct(uuid, tag);
      }

      @Override
      default void deleteDerivedProducts(String uuid) throws StoreException
      {
         getDecorated().deleteDerivedProducts(uuid);
      }

      @Override
      default DataStoreProduct getDerivedProduct(String uuid, String tag) throws StoreException
      {
         return getDecorated().getDerivedProduct(uuid, tag);
      }

      @Override
      default boolean hasDerivedProduct(String uuid, String tag)
      {
         return getDecorated().hasDerivedProduct(uuid, tag);
      }

      @Override
      default boolean addDerivedProductReference(String uuid, String tag, Product product) throws StoreException
      {
         return getDecorated().addDerivedProductReference(uuid, tag, product);
      }

      @Override
      default void deleteDerivedProductReference(String uuid, String tag) throws StoreException
      {
         getDecorated().deleteDerivedProductReference(uuid, tag);
      }
   }

   /**
    * Subclass to add the DerivedProductStore interface and decorate Stores supporting Derived Product.
    *
    * @param <DPDST> implements both then DataStore and DerivedProductStore interfaces
    */
   public static abstract class DerivedDataStore<DPDST extends DataStore & DerivedProductStore>
         extends AbstractDataStoreDecorator<DPDST>
         implements DataStoreDecoratorHelper
   {
      public DerivedDataStore(DPDST decorated)
      {
         super(decorated);
      }

      @Override
      public DerivedProductStore getDecorated()
      {
         return decorated;
      }

   }

}
