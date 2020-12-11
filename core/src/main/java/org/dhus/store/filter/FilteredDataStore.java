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

import fr.gael.dhus.olingo.v1.ODataExpressionParser;
import fr.gael.dhus.olingo.v1.visitor.ProductSQLVisitor;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.exception.ODataMessageException;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;

import org.dhus.Product;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.DataStoreFactory;
import org.dhus.store.datastore.DataStoreFactory.InvalidConfigurationException;
import org.dhus.store.datastore.ProductNotFoundException;
import org.dhus.store.derived.DerivedProductStore;
import org.dhus.store.quota.AbstractDataStoreDecorator;

/**
 * A DataStore decorator whose {@link #get(String)} method is tweaked to return a product only if it
 * validates a filter expression.
 *
 * @param <DST> generic type can be changed to support more complex datastores
 */
public class FilteredDataStore<DST extends DataStore> extends AbstractDataStoreDecorator<DST>
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final ProductService PRODUCT_SERVICE = ApplicationContextProvider.getBean(ProductService.class);

   private final ProductSQLVisitor visitor;

   /**
    * Creates a new filtering decorator.
    *
    * @param decorated a non null DataStore instance to decorate
    * @param visitor a non null filter expression visitor (see the OData $filter format)
    * @throws NullPointerException either the `decorated` or `filter` parameter is null
    */
   public FilteredDataStore(DST decorated, ProductSQLVisitor visitor)
   {
      super(decorated);
      Objects.requireNonNull(visitor);
      this.visitor = visitor;
   }

   public static ProductSQLVisitor makeVisitor(String filter) throws InvalidConfigurationException
   {
      try
      {
         FilterExpression filterExpression = ODataExpressionParser.getProductExpressionParser().parseFilterString(filter);
         ProductSQLVisitor productSQLVisitor = new ProductSQLVisitor(filterExpression, null);
         return productSQLVisitor;
      }
      catch (ODataMessageException | ODataApplicationException e)
      {
         throw new DataStoreFactory.InvalidConfigurationException("Cannot parse filter expression", e);
      }
   }

   @Override
   public Product get(String uuid) throws DataStoreException
   {
      fr.gael.dhus.database.object.Product product = PRODUCT_SERVICE.getFilteredProduct(uuid, visitor);

      if (product != null)
      {
         return this.decorated.get(uuid);
      }
      else
      {
         LOGGER.debug("Filtered DataStore (get): Product {} does not match filter for {}", uuid, getName());
         throw new ProductNotFoundException("Cannot get product for id " + uuid);
      }
   }

   @Override
   public boolean hasProduct(String uuid)
   {
      fr.gael.dhus.database.object.Product product = PRODUCT_SERVICE.getFilteredProduct(uuid, visitor);
      if (product != null)
      {
         return this.decorated.hasProduct(uuid);
      }
      else
      {
         // means the product doesn't match the filter
         LOGGER.debug("Filtered DataStore (hasProduct): Product {} does not match filter for {}", uuid, getName());
         return false;
      }
   }

   public static class FileteredDerivedDataStore<DPDST extends DataStore & DerivedProductStore>
         extends FilteredDataStore<DPDST>
         implements AbstractDataStoreDecorator.DataStoreDecoratorHelper
   {
      public FileteredDerivedDataStore(DPDST decorated, ProductSQLVisitor visitor)
      {
         super(decorated, visitor);
      }

      @Override
      public DerivedProductStore getDecorated()
      {
         return this.decorated;
      }

   }
}
