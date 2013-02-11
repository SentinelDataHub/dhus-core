/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2018 GAEL Systems
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
package fr.gael.dhus.olingo.v1.visitor;

import fr.gael.dhus.database.object.ProductCart;
import fr.gael.dhus.olingo.v1.Model;
import fr.gael.dhus.olingo.v1.SQLVisitor;
import fr.gael.dhus.olingo.v1.entityset.ItemEntitySet;
import fr.gael.dhus.olingo.v1.entityset.NodeEntitySet;
import fr.gael.dhus.olingo.v1.entityset.ProductEntitySet;

import org.apache.olingo.odata2.api.edm.EdmTyped;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.uri.expression.ExceptionVisitExpression;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;
import org.apache.olingo.odata2.api.uri.expression.PropertyExpression;

/**
 * Visitor for Filter and OrderBy expression on user carts.
 */
public class CartSQLVisitor extends SQLVisitor
{
   /**
    * Creates a new visitor for given filter and order expressions.
    *
    * @param filter a non null filter
    * @param order a non null order
    * @throws ExceptionVisitExpression could not visit given expression
    * @throws ODataApplicationException another exception occurred
    */
   public CartSQLVisitor(FilterExpression filter, OrderByExpression order)
         throws ExceptionVisitExpression, ODataApplicationException
   {
      super(ProductCart.class, filter, order);
   }

   @Override
   public Object visitProperty(PropertyExpression property_expression, String uri_literal, EdmTyped edm_property)
   {
      if (edm_property == null)
      {
         throw new IllegalArgumentException("Property not found: " + uri_literal);
      }

      Member member = null;
      switch (uri_literal)
      {
         case ItemEntitySet.ID:
         {
            member = new Member("products.uuid");
            break;
         }
         case ItemEntitySet.NAME:
         {
            member = new Member("products.identifier");
            break;
         }
         case ItemEntitySet.CONTENT_LENGTH:
         {
            member = new Member("products.download.size");
            break;
         }
         case ProductEntitySet.CREATION_DATE:
         {
            member = new Member("products.created");
            break;
         }
         case ProductEntitySet.INGESTION_DATE:
         {
            member = new Member("products.ingestionDate");
            break;
         }
         case ProductEntitySet.CONTENT_GEOMETRY:
         {
            member = new Member("products.footPrint");
            break;
         }
         // Not used really, but needed to be here.
         case ProductEntitySet.CONTENT_DATE:
         {
            break; // return null
         }
         case Model.TIME_RANGE_START:
         {
            member = new Member("products.contentStart");
            break;
         }
         case Model.TIME_RANGE_END:
         {
            member = new Member("products.contentEnd");
            break;
         }

         case ProductEntitySet.ONLINE :
         {
            member = new Member("products.online");
            break;
         }

         // non filterable properties
         case ProductEntitySet.EVICTION_DATE:
         case ItemEntitySet.CONTENT_TYPE:
         case NodeEntitySet.VALUE:
         case NodeEntitySet.CHILDREN_NUMBER:
         {
            throw new IllegalArgumentException("Property \"" + uri_literal + "\" is not filterable");
         }

         // Unsupported or invalid properties
         default:
         {
            throw new IllegalArgumentException("Property not supported: " + uri_literal);
         }
      }
      return member;
   }

}
