/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2019 GAEL Systems
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
package fr.gael.dhus.service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.gael.dhus.database.dao.EventDao;
import fr.gael.dhus.database.object.Event;
import fr.gael.dhus.database.object.Event.EventCategory;
import fr.gael.dhus.olingo.v1.visitor.EventSQLVisitor;

@Service
public class EventService extends WebService
{

   @Autowired
   private EventDao eventDao;

   public Event getEventById(Long id)
   {
      return eventDao.read(id);
   }

   public Event getEventById(String id)
   {
      return eventDao.read(Long.parseLong(id));
   }

   @Transactional(readOnly = true)
   public Event getEventByMainFields(EventCategory category, String subcategory,
         String title, Date startDate, Date stopDate)
   {
      return eventDao.read(category, subcategory, title, startDate, stopDate);
   }

   @Transactional(readOnly = true)
   public Event getEventByTitleAndPublicationDate(String title,
         Date publicationDate)
   {
      return eventDao.read(title, publicationDate);
   }

   @Transactional(readOnly = true)
   public List<Event> getEvents(EventSQLVisitor visitor, int skip, int top)
   {
      return eventDao.executeHQLQuery(visitor.getHqlQuery(),
            visitor.getHqlParameters(), skip, top);
   }

   @Transactional(readOnly = true)
   public int countEvents(EventSQLVisitor visitor)
   {
      return eventDao.countHQLQuery(visitor.getHqlQuery(),
            visitor.getHqlParameters());
   }

   @PreAuthorize("hasRole('ROLE_EVENT_MANAGER')")
   @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
   public void delete(Event event)
   {
      eventDao.delete(event);
   }

   @PreAuthorize("hasRole('ROLE_EVENT_MANAGER')")
   @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
   public void delete(Long id)
   {
      eventDao.delete(getEventById(id));
   }

   @PreAuthorize("hasRole('ROLE_EVENT_MANAGER')")
   @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
   public Event create(String category, String subcategory, String title,
         String description, Date startDate, Date stopDate, Date publicationDate,
         String icon, boolean localEvent, boolean publicEvent, String originator,
         String hubTag, String missionTag, String instrumentTag,
         String externalUrl)
   {
      Event event = new Event();

      /* Required fields without default values */
      event.setCategory(
            EventCategory.valueOf(Objects.requireNonNull(category)));
      event.setTitle(Objects.requireNonNull(title));
      event.setDescription(Objects.requireNonNull(description));
      event.setStartDate(Objects.requireNonNull(startDate));

      if ("Other".equals(category)
            && (subcategory == null || subcategory.isEmpty()))
      {
         throw new IllegalArgumentException(
               "Subcategory must be specified if the Category is 'Other'");
      }
      event.setSubcategory(subcategory);

      /* publicationDate is the current date by default */
      if (publicationDate != null)
      {
         event.setPublicationDate(publicationDate);
      }
      event.setStopDate(stopDate);
      event.setIcon(icon);
      event.setLocalEvent(localEvent);
      event.setPublicEvent(publicEvent);
      event.setOriginator(originator);
      event.setHubTag(hubTag);
      event.setMissionTag(missionTag);
      event.setInstrumentTag(instrumentTag);
      event.setExternalUrl(externalUrl);
      eventDao.create(event);
      return event;
   }

   @PreAuthorize("hasRole('ROLE_EVENT_MANAGER')")
   @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
   public void updateEvent(Event event)
   {
      eventDao.update(event);
   }

   @PreAuthorize("hasRole('ROLE_EVENT_MANAGER')")
   @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
   public void createEvent(Event event)
   {
      eventDao.create(event);
   }

   @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
   public void updateEventWithoutCredentials(Event event)
   {
      eventDao.update(event);
   }

   @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
   public void createEventWithoutCredentials(Event event)
   {
      eventDao.create(event);
   }

}
