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

import fr.gael.dhus.database.object.DeletedProduct;
import fr.gael.dhus.olingo.v1.Model;
import fr.gael.dhus.olingo.v1.SQLVisitor;
import fr.gael.dhus.olingo.v1.entityset.DeletedProductEntitySet;

import org.apache.olingo.odata2.api.edm.EdmTyped;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.uri.expression.ExceptionVisitExpression;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;
import org.apache.olingo.odata2.api.uri.expression.PropertyExpression;

public class DeletedProductSQLVisitor extends SQLVisitor
{
   public DeletedProductSQLVisitor(FilterExpression filter, OrderByExpression order)
         throws ExceptionVisitExpression, ODataApplicationException
   {
      super(DeletedProduct.class, filter, order);
   }

   @Override
   public Object visitProperty(PropertyExpression property_expression,
         String uri_literal, EdmTyped edm_property)
   {
      if (edm_property == null)
      {
         throw new IllegalArgumentException("Property not found: " + uri_literal);
      }

      Member member = null;
      switch (uri_literal)
      {
         case DeletedProductEntitySet.ID:
         {
            member = new Member("uuid");
            break;
         }
         case DeletedProductEntitySet.NAME:
         {
            member = new Member("identifier");
            break;
         }
         case DeletedProductEntitySet.CREATION_DATE:
         {
            member = new Member("created");
            break;
         }
         case DeletedProductEntitySet.FOOTPRINT:
         {
            member = new Member("footPrint");
            break;
         }
         case DeletedProductEntitySet.SIZE:
         {
            member = new Member("size");
            break;
         }
         case DeletedProductEntitySet.INGESTION_DATE:
         {
            member = new Member("ingestionDate");
            break;
         }
         // Not used really, but needed to be here.
         case DeletedProductEntitySet.CONTENT_DATE:
         {
            break; // return null
         }
         case Model.TIME_RANGE_START:
         {
            member = new Member("contentStart");
            break;
         }
         case Model.TIME_RANGE_END:
         {
            member = new Member("contentEnd");
            break;
         }
         case DeletedProductEntitySet.DELETION_DATE:
         {
            member = new Member("deletionDate");
            break;
         }
         case DeletedProductEntitySet.DELETION_CAUSE:
         {
            member = new Member("deletionCause");
            break;
         }

         // non filterable properties
         case DeletedProductEntitySet.CHECKSUM:
         {
            throw new IllegalArgumentException("Property \"" + uri_literal + "\" is not filterable.");
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
