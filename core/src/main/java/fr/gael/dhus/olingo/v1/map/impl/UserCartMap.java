/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016,2017 GAEL Systems
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
import fr.gael.dhus.olingo.v1.entity.Product;
import fr.gael.dhus.olingo.v1.map.AbstractDelegatingMap;
import fr.gael.dhus.olingo.v1.map.SubMap;
import fr.gael.dhus.olingo.v1.map.SubMapBuilder;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.util.functional.IteratorAdapter;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.uri.expression.ExceptionVisitExpression;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;
import org.apache.olingo.odata2.core.exception.ODataRuntimeException;

/**
 * A map view on a User's cart.
 *
 * @see AbstractDelegatingMap
 */
public class UserCartMap extends AbstractDelegatingMap<String, Product>
      implements SubMap<String, Product>
{
   /** To filter product carts. */
   private final OlingoManager olingoManager =
         ApplicationContextProvider.getBean(OlingoManager.class);

   /** User ID. */
   private final String user_uuid;
   private final FilterExpression filter;
   private final OrderByExpression orderBy;
   private final int skip;
   private int top;

   /**
    * Creates a Map on products from the given user's cart.
    * @param user_uuid a User ID.
    */
   public UserCartMap(String user_uuid)
   {
      this(user_uuid, null, null, 0, -1);
   }

   public UserCartMap(String user_uuid, FilterExpression filter, OrderByExpression order,
         int skip, int top)
   {
      this.user_uuid = user_uuid;
      this.filter = filter;
      this.orderBy = order;
      this.skip = skip;
      this.top = top;
   }

   @Override
   protected Product serviceGet(String key)
   {
      try
      {
         List<fr.gael.dhus.database.object.Product> cart =
               olingoManager.getProductCart(user_uuid, filter, orderBy, skip, top);
         if (!cart.isEmpty())
         {
            for (fr.gael.dhus.database.object.Product prod: cart)
            {
               if (prod.getUuid().equals(key))
               {
                  return Product.generateProduct(prod);
               }
            }
         }
         return null;
      }
      catch (ExceptionVisitExpression | ODataApplicationException ex)
      {
         throw new ODataRuntimeException(ex);
      }
   }

   @Override
   protected Iterator<Product> serviceIterator()
   {
      try {
         List<fr.gael.dhus.database.object.Product> cart =
               olingoManager.getProductCart(user_uuid, filter, orderBy, skip, top);
         final Iterator<fr.gael.dhus.database.object.Product> it = cart.iterator();
         return new IteratorAdapter<>(it, Product::generateProduct);
      }
      catch (ExceptionVisitExpression | ODataApplicationException ex)
      {
         throw new ODataRuntimeException(ex);
      }
   }

   @Override
   protected int serviceCount()
   {
      try
      {
         return olingoManager.getCartProductCount(user_uuid, filter);
      }
      catch (ExceptionVisitExpression | ODataApplicationException ex)
      {
         throw new ODataRuntimeException(ex);
      }
   }

   @Override
   public SubMapBuilder<String, Product> getSubMapBuilder()
   {
      return new SubMapBuilder<String, Product>()
      {
         @Override
         public Map<String, Product> build()
         {
            return new UserCartMap(user_uuid, filter, orderBy, skip, top);
         }
      };
   }

}
