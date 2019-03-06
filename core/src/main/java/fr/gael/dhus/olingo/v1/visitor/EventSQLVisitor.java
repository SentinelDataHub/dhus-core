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
package fr.gael.dhus.olingo.v1.visitor;

import org.apache.olingo.odata2.api.edm.EdmTyped;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.uri.expression.ExceptionVisitExpression;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;
import org.apache.olingo.odata2.api.uri.expression.PropertyExpression;

import fr.gael.dhus.database.object.Event;
import fr.gael.dhus.olingo.v1.SQLVisitor;
import fr.gael.dhus.olingo.v1.entityset.EventEntitySet;

public class EventSQLVisitor extends SQLVisitor
{

   public EventSQLVisitor(FilterExpression filter, OrderByExpression order)
         throws ExceptionVisitExpression, ODataApplicationException
   {
      super(Event.class, filter, order);
   }

   @Override
   public Object visitProperty(PropertyExpression property_expression,
         String uri_literal, EdmTyped edm_property)
   {
      if (edm_property == null)
      {
         throw new IllegalArgumentException(
               "Property not found: " + uri_literal);
      }

      switch (uri_literal)
      {
         case EventEntitySet.ID:
            return new Member("id");
         case EventEntitySet.CATEGORY:
            return new Member("category");
         case EventEntitySet.SUBCATEGORY:
            return new Member("subcategory");
         case EventEntitySet.TITLE:
            return new Member("title");
         case EventEntitySet.DESCRIPTION:
            return new Member("description");
         case EventEntitySet.START_DATE:
            return new Member("startDate");
         case EventEntitySet.STOP_DATE:
            return new Member("stopDate");
         case EventEntitySet.PUBLICATION_DATE:
            return new Member("publicationDate");
         case EventEntitySet.ICON:
            return new Member("icon");
         case EventEntitySet.LOCAL_EVENT:
            return new Member("localEvent");
         case EventEntitySet.PUBLIC_EVENT:
            return new Member("publicEvent");
         case EventEntitySet.ORIGINATOR:
            return new Member("originator");
         case EventEntitySet.HUB_TAG:
            return new Member("hubTag");
         case EventEntitySet.MISSION_TAG:
            return new Member("missionTag");
         case EventEntitySet.INSTRUMENT_TAG:
            return new Member("instrumentTag");
         case EventEntitySet.EXTERNAL_URL:
            return new Member("externalUrl");
         default:
            throw new IllegalArgumentException(
                  "Property not supported: " + uri_literal);
      }
   }

}
