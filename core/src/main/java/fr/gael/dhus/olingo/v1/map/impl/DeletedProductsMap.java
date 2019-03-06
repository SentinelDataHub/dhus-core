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
package fr.gael.dhus.olingo.v1.map.impl;

import fr.gael.dhus.olingo.v1.OlingoManager;
import fr.gael.dhus.olingo.v1.entity.DeletedProduct;
import fr.gael.dhus.olingo.v1.map.AbstractDelegatingMap;
import fr.gael.dhus.olingo.v1.map.SubMap;
import fr.gael.dhus.olingo.v1.map.SubMapBuilder;
import fr.gael.dhus.service.DeletedProductService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.util.functional.IteratorAdapter;

import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;
import org.apache.olingo.odata2.core.exception.ODataRuntimeException;

/**
 * This is a map view on ALL products.
 *
 * @see AbstractDelegatingMap
 */
public class DeletedProductsMap extends AbstractDelegatingMap<String, DeletedProduct>
      implements SubMap<String, DeletedProduct>
{
   private static final Logger LOGGER = LogManager.getLogger();
   private final OlingoManager olingoManager = ApplicationContextProvider
         .getBean(OlingoManager.class);
   private final DeletedProductService deletedProductService = ApplicationContextProvider
         .getBean(DeletedProductService.class);
   private final FilterExpression filter;
   private final OrderByExpression orderBy;
   private final int skip;
   private int top;

   /**
    * Creates a new map view.
    */
   public DeletedProductsMap()
   {
      this(null, null, 0, -1);
   }

   /** Private constructor used by {@link DeletedProductsMap#getSubMapBuilder()}. */
   private DeletedProductsMap(FilterExpression filter, OrderByExpression order,
         int skip, int top)
   {
      this.filter = filter;
      this.orderBy = order;
      this.skip = skip;
      this.top = top;
   }

   @Override
   protected Iterator<DeletedProduct> serviceIterator()
   {
      try
      {
         Iterator<fr.gael.dhus.database.object.DeletedProduct> it =
               olingoManager.getDeletedProducts(filter, orderBy, skip, top).iterator();

         return new IteratorAdapter<>(it, DeletedProduct::new);
      }
      catch (Exception e)
      {
         throw new ODataRuntimeException(e);
      }
   }

   @Override
   protected int serviceCount()
   {
      try
      {
         return olingoManager.getDeletedProductsNumber(filter);
      }
      catch (Exception e)
      {
         LOGGER.error("Error when getting Products number", e);
      }
      return -1;
   }

   @Override
   protected DeletedProduct serviceGet(String key)
   {
      fr.gael.dhus.database.object.DeletedProduct p = deletedProductService.getProduct(key);
      if (p == null)
      {
         return null;
      }
      return new DeletedProduct(p);
   }

   @Override
   public SubMapBuilder<String, DeletedProduct> getSubMapBuilder()
   {
      return new SubMapBuilder<String, DeletedProduct>()
      {
         @Override
         public Map<String, DeletedProduct> build()
         {
            return new DeletedProductsMap(filter, orderBy, skip, top);
         }
      };
   }
}
