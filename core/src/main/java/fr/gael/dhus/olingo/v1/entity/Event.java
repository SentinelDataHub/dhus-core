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
package fr.gael.dhus.olingo.v1.entity;

import fr.gael.dhus.database.object.Event.EventCategory;
import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.olingo.Security;
import fr.gael.dhus.olingo.v1.ExpectedException.NotAllowedException;
import fr.gael.dhus.olingo.v1.entityset.EventEntitySet;
import fr.gael.dhus.service.EventService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataException;

public class Event extends AbstractEntity
{
   private static final EventService EVENT_SERVICE =
         ApplicationContextProvider.getBean(EventService.class);

   private fr.gael.dhus.database.object.Event event;

   public Event(fr.gael.dhus.database.object.Event event)
   {
      this.event = event;
   }

   public Event(long id) throws ODataException
   {
      this(EVENT_SERVICE.getEventById(id));
   }

   @Override
   public Map<String, Object> toEntityResponse(String self_url)
   {
      Map<String, Object> entityResponse = new HashMap<>();

      entityResponse.put(EventEntitySet.ID, event.getId());
      entityResponse.put(EventEntitySet.CATEGORY, event.getCategory());
      entityResponse.put(EventEntitySet.SUBCATEGORY, event.getSubcategory());
      entityResponse.put(EventEntitySet.TITLE, event.getTitle());
      entityResponse.put(EventEntitySet.DESCRIPTION, event.getDescription());
      entityResponse.put(EventEntitySet.START_DATE, event.getStartDate());
      entityResponse.put(EventEntitySet.STOP_DATE, event.getStopDate());
      entityResponse.put(EventEntitySet.PUBLICATION_DATE, event.getPublicationDate());
      entityResponse.put(EventEntitySet.ICON, event.getIcon());
      entityResponse.put(EventEntitySet.LOCAL_EVENT, event.isLocalEvent());
      entityResponse.put(EventEntitySet.PUBLIC_EVENT, event.isPublicEvent());
      entityResponse.put(EventEntitySet.ORIGINATOR, event.getOriginator());
      entityResponse.put(EventEntitySet.HUB_TAG, event.getHubTag());
      entityResponse.put(EventEntitySet.MISSION_TAG, event.getMissionTag());
      entityResponse.put(EventEntitySet.INSTRUMENT_TAG, event.getInstrumentTag());
      entityResponse.put(EventEntitySet.EXTERNAL_URL, event.getExternalUrl());

      return entityResponse;
   }

   @Override
   public Object getProperty(String propName) throws ODataException
   {
      switch (propName)
      {
         case EventEntitySet.ID:
            return event.getId();
         case EventEntitySet.CATEGORY:
            return event.getCategory();
         case EventEntitySet.SUBCATEGORY:
            return event.getSubcategory();
         case EventEntitySet.TITLE:
            return event.getTitle();
         case EventEntitySet.DESCRIPTION:
            return event.getDescription();
         case EventEntitySet.START_DATE:
            return event.getStartDate();
         case EventEntitySet.STOP_DATE:
            return event.getStopDate();
         case EventEntitySet.PUBLICATION_DATE:
            return event.getPublicationDate();
         case EventEntitySet.ICON:
            return event.getIcon();
         case EventEntitySet.LOCAL_EVENT:
            return event.isLocalEvent();
         case EventEntitySet.PUBLIC_EVENT:
            return event.isPublicEvent();
         case EventEntitySet.ORIGINATOR:
            return event.getOriginator();
         case EventEntitySet.HUB_TAG:
            return event.getHubTag();
         case EventEntitySet.MISSION_TAG:
            return event.getMissionTag();
         case EventEntitySet.INSTRUMENT_TAG:
            return event.getInstrumentTag();
         case EventEntitySet.EXTERNAL_URL:
            return event.getExternalUrl();
         default:
            throw new ODataException("Property '" + propName + "' not found.");
      }
   }

   public static void delete(long key) throws NotAllowedException
   {
      if (Security.currentUserHasRole(Role.EVENT_MANAGER))
      {
         EVENT_SERVICE.delete(key);
      }
      else
      {
         throw new NotAllowedException();
      }
   }

   public static Event create(ODataEntry entry) throws ODataException
   {
      Map<String, Object> properties = entry.getProperties();

      /* Required fields without default values */
      String category = (String) properties.get(EventEntitySet.CATEGORY);
      if (category == null || category.isEmpty())
      {
         throw new ODataException("Event category required");
      }

      String title = (String) properties.get(EventEntitySet.TITLE);
      if (title == null || title.isEmpty())
      {
         throw new ODataException("Event title required");
      }

      String description = (String) properties.get(EventEntitySet.DESCRIPTION);
      if (description == null || description.isEmpty())
      {
         throw new ODataException("Event description required");
      }

      Object startDateObject = properties.get(EventEntitySet.START_DATE);
      if (startDateObject == null)
      {
         throw new ODataException("Event startDate required");
      }
      Date startDate = ((GregorianCalendar) startDateObject).getTime();

      /* Optionnal fields */
      String subcategory = null;
      Date stopDate = null;
      Date publicationDate = null;
      String icon = null;
      boolean localEvent = true;
      boolean publicEvent = true;
      String originator = null;
      String hubTag = null;
      String missionTag = null;
      String instrumentTag = null;
      String externalUrl = null;

      if (properties.containsKey(EventEntitySet.SUBCATEGORY))
      {
         subcategory = (String) properties.get(EventEntitySet.SUBCATEGORY);
      }

      if (properties.containsKey(EventEntitySet.STOP_DATE))
      {
         Object stopDateObject = properties.get(EventEntitySet.STOP_DATE);
         if(stopDateObject != null)
         {
            stopDate = ((GregorianCalendar) stopDateObject).getTime();
         }
      }

      if (properties.containsKey(EventEntitySet.PUBLICATION_DATE))
      {
         Object publicationDateObject = properties.get(EventEntitySet.PUBLICATION_DATE);
         if(publicationDateObject != null)
         {
            publicationDate = ((GregorianCalendar) publicationDateObject).getTime();
         }
      }

      if (properties.containsKey(EventEntitySet.ICON))
      {
         icon = (String) properties.get(EventEntitySet.ICON);
      }

      if (properties.containsKey(EventEntitySet.LOCAL_EVENT))
      {
         localEvent = (boolean) properties.get(EventEntitySet.LOCAL_EVENT);
      }

      if (properties.containsKey(EventEntitySet.PUBLIC_EVENT))
      {
         publicEvent = (boolean) properties.get(EventEntitySet.PUBLIC_EVENT);
      }

      if (properties.containsKey(EventEntitySet.ORIGINATOR))
      {
         originator = (String) properties.get(EventEntitySet.ORIGINATOR);
      }

      if (properties.containsKey(EventEntitySet.HUB_TAG))
      {
         hubTag = (String) properties.get(EventEntitySet.HUB_TAG);
      }

      if (properties.containsKey(EventEntitySet.MISSION_TAG))
      {
         missionTag = (String) properties.get(EventEntitySet.MISSION_TAG);
      }

      if (properties.containsKey(EventEntitySet.INSTRUMENT_TAG))
      {
         instrumentTag = (String) properties.get(EventEntitySet.INSTRUMENT_TAG);
      }

      if (properties.containsKey(EventEntitySet.EXTERNAL_URL))
      {
         externalUrl = (String) properties.get(EventEntitySet.EXTERNAL_URL);
      }

      fr.gael.dhus.database.object.Event event =
            EVENT_SERVICE.create(category, subcategory, title, description, startDate, stopDate,
                  publicationDate, icon, localEvent, publicEvent, originator, hubTag, missionTag,
                  instrumentTag, externalUrl);

      return new Event(event);
   }

   @Override
   public void updateFromEntry(ODataEntry entry) throws ODataException
   {
      Map<String, Object> properties = entry.getProperties();
      if (properties.containsKey(EventEntitySet.CATEGORY))
      {
         event.setCategory(EventCategory.fromString((String) properties.get(EventEntitySet.CATEGORY)));
      }

      if (properties.containsKey(EventEntitySet.SUBCATEGORY))
      {
         String subcategory = (String) properties.get(EventEntitySet.SUBCATEGORY);
         if (event.getCategory().equals(EventCategory.OTHER) && subcategory.isEmpty())
         {
            throw new ODataException("Subcategory must be specified if the Category is 'Other'");
         }
         event.setSubcategory(subcategory);
      }

      if (properties.containsKey(EventEntitySet.TITLE))
      {
         String title = (String) properties.get(EventEntitySet.TITLE);
         if (title.isEmpty())
         {
            throw new ODataException("Event title required");
         }
         event.setTitle(title);
      }

      if (properties.containsKey(EventEntitySet.DESCRIPTION))
      {
         String description = (String) properties.get(EventEntitySet.DESCRIPTION);
         if (description.isEmpty())
         {
            throw new ODataException("Event description required");
         }
         event.setDescription(description);
      }

      if (properties.containsKey(EventEntitySet.START_DATE))
      {
         event.setStartDate(
               ((GregorianCalendar) properties.get(EventEntitySet.START_DATE)).getTime());
      }

      if (properties.containsKey(EventEntitySet.STOP_DATE))
      {
         event.setStopDate(
               ((GregorianCalendar) properties.get(EventEntitySet.STOP_DATE)).getTime());
      }

      if (properties.containsKey(EventEntitySet.PUBLICATION_DATE))
      {
         event.setPublicationDate(
               ((GregorianCalendar) properties.get(EventEntitySet.PUBLICATION_DATE)).getTime());
      }

      if (properties.containsKey(EventEntitySet.ICON))
      {
         event.setIcon((String) properties.get(EventEntitySet.ICON));
      }

      if (properties.containsKey(EventEntitySet.LOCAL_EVENT))
      {
         event.setLocalEvent((boolean) properties.get(EventEntitySet.LOCAL_EVENT));
      }

      if (properties.containsKey(EventEntitySet.PUBLIC_EVENT))
      {
         event.setPublicEvent((boolean) properties.get(EventEntitySet.PUBLIC_EVENT));
      }

      if (properties.containsKey(EventEntitySet.ORIGINATOR))
      {
         event.setOriginator((String) properties.get(EventEntitySet.ORIGINATOR));
      }

      if (properties.containsKey(EventEntitySet.HUB_TAG))
      {
         event.setHubTag((String) properties.get(EventEntitySet.HUB_TAG));
      }

      if (properties.containsKey(EventEntitySet.MISSION_TAG))
      {
         event.setMissionTag((String) properties.get(EventEntitySet.MISSION_TAG));
      }

      if (properties.containsKey(EventEntitySet.INSTRUMENT_TAG))
      {
         event.setInstrumentTag((String) properties.get(EventEntitySet.INSTRUMENT_TAG));
      }

      if (properties.containsKey(EventEntitySet.EXTERNAL_URL))
      {
         event.setExternalUrl((String) properties.get(EventEntitySet.EXTERNAL_URL));
      }

      EVENT_SERVICE.updateEvent(event);
   }

}
