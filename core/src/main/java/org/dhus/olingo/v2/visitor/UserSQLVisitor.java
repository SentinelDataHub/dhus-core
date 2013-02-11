/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2020 GAEL Systems
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
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.dhus.olingo.v2.datamodel.UserModel;

public class UserSQLVisitor  extends SQLVisitor
{
   private static final long serialVersionUID = 1L;

   public UserSQLVisitor(FilterOption filter, OrderByOption order, TopOption topOption, SkipOption skipOption)
   {
      super("", "from User", filter, order, topOption, skipOption);
   }
   @Override
   public Object visitMember(Member member)
         throws ExpressionVisitException, ODataApplicationException
   {
      final List<UriResource> uriResourceParts = member.getResourcePath().getUriResourceParts();

      if (uriResourceParts.size() == 1 && uriResourceParts.get(0) instanceof UriResourcePrimitiveProperty)
      {
         String segmentVal = ((UriResourcePrimitiveProperty) uriResourceParts.get(0)).getSegmentValue();
         // switch over the properties to identify the exact property
         switch (segmentVal)
         {
            case UserModel.PROPERTY_USERNAME:
            {
               return "username";
            }
            case UserModel.PROPERTY_COUNTRY:
            {
               return "country";
            }
            case UserModel.PROPERTY_EMAIL:
            {
               return "email";
            }
            case UserModel.PROPERTY_FIRSTNAME:
            {
               return "firstname";
            }
            case UserModel.PROPERTY_LASTNAME:
            {
               return "lastname";
            }
            case UserModel.PROPERTY_ADDRESS:
            {
               return "address";
            }
            case UserModel.PROPERTY_PHONE:
            {
               return "phone";
            }
            case UserModel.PROPERTY_DOMAIN:
            {
               return "domain";
            }
            case UserModel.PROPERTY_SUBDOMAIN:
            {
               return "subDomain";
            }
            case UserModel.PROPERTY_USAGE:
            {
               return "usage";
            }
            case UserModel.PROPERTY_SUBUSAGE:
            {
               return "subUsage";
            }
            case UserModel.PROPERTY_CREATED:
            {
               return "created";
            }
            default:
            {
               throw new ODataApplicationException("Property not supported: " + segmentVal,
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
