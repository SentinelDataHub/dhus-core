/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018,2019 GAEL Systems
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
package org.dhus.olingo.v2.visitor;

import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

import org.dhus.olingo.v2.datamodel.ItemModel;
import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.olingo.v2.datamodel.complex.TimeRangeComplexType;

public class ProductSQLVisitor extends SQLVisitor
{
   private static final long serialVersionUID = 1L;

   public ProductSQLVisitor(FilterOption filter, OrderByOption order, TopOption topOption, SkipOption skipOption)
   {
      super("", "from Product", filter, order, topOption, skipOption);
   }

   @Override
   public Object visitMember(Member member) throws ExpressionVisitException, ODataApplicationException
   {
      final List<UriResource> uriResourceParts = member.getResourcePath().getUriResourceParts();

      // Adressing a property
      if (uriResourceParts.size() == 1 && uriResourceParts.get(0) instanceof UriResourcePrimitiveProperty)
      {
         String segmentVal = ((UriResourcePrimitiveProperty) uriResourceParts.get(0)).getSegmentValue();

         switch (segmentVal)
         {
            case ItemModel.PROPERTY_ID:
            {
               return "uuid";
            }
            case ItemModel.PROPERTY_NAME:
            {
               return "identifier";
            }
            case ItemModel.PROPERTY_CONTENTLENGTH:
            {
               return "size";
            }
            case ProductModel.PROPERTY_CREATIONDATE:
            {
               return "created";
            }
            case ProductModel.PROPERTY_INGESTIONDATE:
            {
               return "ingestiondate";
            }
            case ProductModel.PROPERTY_MODIFICATIONDATE:
            {
               return "updated";
            }
            case ProductModel.PROPERTY_ONLINE:
            {
               return "online";
            }
            case ProductModel.PROPERTY_ONDEMAND:
            {
               return "onDemand";
            }
            default:
            {
               throw new ODataApplicationException("Property not supported: " + segmentVal,
                     HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
            }
         }
      }
      // Adressing a complex property
      else if (uriResourceParts.size() == 2
            && uriResourceParts.get(0) instanceof UriResourceComplexProperty
            && uriResourceParts.get(1) instanceof UriResourcePrimitiveProperty)
      {
         UriResource cplex = uriResourceParts.get(0);
         String segmentVal = ((UriResourcePrimitiveProperty) uriResourceParts.get(1)).getSegmentValue();

         switch (cplex.getSegmentValue())
         {
            case ProductModel.PROPERTY_CONTENTDATE:
            {
               switch (segmentVal)
               {
                  case TimeRangeComplexType.PROPERTY_START:
                  {
                     return "contentStart";
                  }
                  case TimeRangeComplexType.PROPERTY_END:
                  {
                     return "contentEnd";
                  }
                  default:
                  {
                     throw new ODataApplicationException("Property not supported: " + segmentVal,
                           HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
                  }
               }
            }
            default:
            {
               throw new ODataApplicationException("Complex property not supported: " + segmentVal,
                     HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
            }
         }
      }
      else
      {
         throw new ODataApplicationException("Non-primitive properties are not supported in filter expressions",
               HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
      }
   }
}
