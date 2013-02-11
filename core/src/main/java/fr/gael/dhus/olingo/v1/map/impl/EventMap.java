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

import java.util.Iterator;
import java.util.Map;

import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.uri.expression.ExceptionVisitExpression;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;
import org.apache.olingo.odata2.core.exception.ODataRuntimeException;

import fr.gael.dhus.olingo.v1.entity.Event;
import fr.gael.dhus.olingo.v1.map.AbstractDelegatingMap;
import fr.gael.dhus.olingo.v1.map.SubMap;
import fr.gael.dhus.olingo.v1.map.SubMapBuilder;
import fr.gael.dhus.olingo.v1.visitor.EventSQLVisitor;
import fr.gael.dhus.service.EventService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.util.functional.IteratorAdapter;

public class EventMap extends AbstractDelegatingMap<String, Event>
      implements SubMap<String, Event>
{
   private final EventService eventService =
      ApplicationContextProvider.getBean(EventService.class);

   private final FilterExpression filter;
   private final OrderByExpression orderBy;
   private final int skip;
   private int top;

   public EventMap()
   {
      this(null, null, 0, -1);
   }

   public EventMap(FilterExpression filter, OrderByExpression order, int skip,
         int top)
   {
      this.filter = filter;
      this.orderBy = order;
      this.skip = skip;
      this.top = top;
   }

   @Override
   protected Event serviceGet(String key)
   {
      fr.gael.dhus.database.object.Event eventObject =
         eventService.getEventById(key);
      if (eventObject != null)
      {
         return new Event(eventObject);
      }
      return null;
   }

   @Override
   protected Iterator<Event> serviceIterator()
   {
      try
      {
         EventSQLVisitor visitor = new EventSQLVisitor(filter, orderBy);
         Iterator<fr.gael.dhus.database.object.Event> it =
            eventService.getEvents(visitor, skip, top).iterator();
         return new IteratorAdapter<>(it, Event::new);
      }
      catch (ExceptionVisitExpression | ODataApplicationException e)
      {
         throw new ODataRuntimeException("Unable to iterate over events", e);
      }
   }

   @Override
   protected int serviceCount()
   {
      EventSQLVisitor visitor;
      try
      {
         visitor = new EventSQLVisitor(filter, orderBy);
         return eventService.countEvents(visitor);
      }
      catch (ExceptionVisitExpression | ODataApplicationException e)
      {
         throw new ODataRuntimeException("Unable to count events", e);
      }
   }

   @Override
   public SubMapBuilder<String, Event> getSubMapBuilder()
   {
      return new SubMapBuilder<String, Event>()
      {
         @Override
         public Map<String, Event> build()
         {
            return new EventMap(filter, orderBy, skip, top);
         }
      };
   }

}
