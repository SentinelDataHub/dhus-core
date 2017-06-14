/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2016,2017 GAEL Systems
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

import fr.gael.dhus.database.object.User;
import fr.gael.dhus.olingo.v1.SQLVisitor;
import fr.gael.dhus.olingo.v1.entityset.UserEntitySet;

import org.apache.olingo.odata2.api.edm.EdmTyped;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.uri.expression.ExceptionVisitExpression;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;
import org.apache.olingo.odata2.api.uri.expression.PropertyExpression;

public class UserSQLVisitor extends SQLVisitor
{

   public UserSQLVisitor(FilterExpression filter, OrderByExpression order)
         throws ExceptionVisitExpression, ODataApplicationException
   {
      super(User.class, filter, order);
   }

   @Override
   public Object visitProperty(PropertyExpression property_expression,
         String uri_literal, EdmTyped edm_property)
   {
      if (edm_property == null)
      {
         throw new IllegalArgumentException("Property not found: " + uri_literal);
      }

      switch (uri_literal)
      {
         case UserEntitySet.USERNAME:
            return new Member("username");

         case UserEntitySet.COUNTRY:
            return new Member("country");

         case UserEntitySet.EMAIL:
            return new Member("email");

         case UserEntitySet.FIRSTNAME:
            return new Member("firstname");

         case UserEntitySet.LASTNAME:
            return new Member("lastname");

         case UserEntitySet.ADDRESS:
            return new Member("address");

         case UserEntitySet.PHONE:
            return new Member("phone");

         case UserEntitySet.DOMAIN:
            return new Member("domain");

         case UserEntitySet.SUBDOMAIN:
            return new Member("subDomain");

         case UserEntitySet.USAGE:
            return new Member("usage");

         case UserEntitySet.SUBUSAGE:
            return new Member("subUsage");

         case UserEntitySet.CREATED:
            return new Member("created");

         default:
            throw new IllegalArgumentException("Property not supported: " + uri_literal);
      }
   }
}
