/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018 GAEL Systems
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

import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.olingo.v1.FunctionalVisitor;
import fr.gael.dhus.olingo.v1.Model;
import fr.gael.dhus.olingo.v1.entityset.ItemEntitySet;
import fr.gael.dhus.olingo.v1.entityset.NodeEntitySet;
import fr.gael.dhus.olingo.v1.entityset.ProductEntitySet;

import org.apache.commons.collections4.Transformer;

import org.apache.olingo.odata2.api.edm.EdmTyped;
import org.apache.olingo.odata2.api.uri.expression.PropertyExpression;

/**
 * Functional Visitor used by the filtered eviction mechanism.
 */
public class ProductFunctionalVisitor extends FunctionalVisitor
{
   @Override
   public Object visitProperty(PropertyExpression pe, String uri_literal, EdmTyped prop)
   {
      Transformer<Product, ? extends Object> res = null;
      switch (uri_literal)
      {
         case ItemEntitySet.ID:
         {
            res = (prod) -> prod.getId();
            break;
         }
         case ItemEntitySet.NAME:
         {
            res = (prod) -> prod.getIdentifier();
            break;
         }
         case ItemEntitySet.CONTENT_LENGTH:
         {
            res = (prod) -> prod.getSize();
            break;
         }
         case ProductEntitySet.CREATION_DATE:
         {
            res = (prod) -> prod.getCreated();
            break;
         }
         case ProductEntitySet.INGESTION_DATE:
         {
            res = (prod) -> prod.getIngestionDate();
            break;
         }
         case ProductEntitySet.CONTENT_GEOMETRY:
         {
            res = (prod) -> prod.getFootPrint();
            break;
         }
         // Not used really, but needed to be here.
         case ProductEntitySet.CONTENT_DATE:
         {
            break; // return null
         }
         case Model.TIME_RANGE_START:
         {
            res = (prod) -> prod.getContentStart();
            break;
         }
         case Model.TIME_RANGE_END:
         {
            res = (prod) -> prod.getContentEnd();
            break;
         }
         case ProductEntitySet.ONLINE:
         {
            res = (prod) -> prod.isOnline();
            break;
         }
         case ItemEntitySet.CONTENT_TYPE:
         {
            res = (prod) -> prod.getDownloadableType();
            break;
         }

         // non filterable properties
         case ProductEntitySet.EVICTION_DATE:
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
      if (res == null)
      {
         return null;
      }
      return ExecutableExpressionTree.Node.createLeave(res);
   }

}
