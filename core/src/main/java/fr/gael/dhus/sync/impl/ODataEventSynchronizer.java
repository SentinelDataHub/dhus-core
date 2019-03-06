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
package fr.gael.dhus.sync.impl;

import fr.gael.dhus.database.object.Event;
import fr.gael.dhus.database.object.Event.EventCategory;
import fr.gael.dhus.database.object.config.synchronizer.EventSynchronizer;
import fr.gael.dhus.olingo.ODataClient;
import fr.gael.dhus.olingo.v1.entityset.EventEntitySet;
import fr.gael.dhus.service.EventService;
import fr.gael.dhus.service.ISynchronizerService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.sync.Synchronizer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;

import org.hibernate.exception.LockAcquisitionException;

import org.springframework.dao.CannotAcquireLockException;

/**
 * Synchronizes events through the OData event API.
 */
public class ODataEventSynchronizer extends Synchronizer
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final ISynchronizerService SYNC_SERVICE =
         ApplicationContextProvider.getBean(ISynchronizerService.class);

   private static final EventService EVENT_SERVICE =
         ApplicationContextProvider.getBean(EventService.class);

   /** An {@link ODataClient} configured to query another DHuS OData service. */
   private final ODataClient client;

   /** Credentials: username. */
   private final String serviceUser;

   /** Credentials: password. */
   private final String servicePass;

   /** Current offset in remote's event list ($skip parameter) */
   private int skip;

   /** Size of a Page (number of events to retrieve at once, $top parameter). */
   private int pageSize;

   /** Filter through events ($filter parameter) */
   private String filter;

   public ODataEventSynchronizer(EventSynchronizer eventSynchronizer)
         throws IOException, ODataException
   {
      super(eventSynchronizer);
      // Checks if required configuration is set
      String urilit = eventSynchronizer.getServiceUrl();
      serviceUser = eventSynchronizer.getServiceLogin();
      servicePass = eventSynchronizer.getServicePassword();

      if (urilit == null || urilit.isEmpty())
      {
         throw new IllegalStateException("`service_uri` is not set");
      }

      try
      {
         client = new ODataClient(urilit, serviceUser, servicePass);
      }
      catch (URISyntaxException e)
      {
         throw new IllegalStateException("`service_uri` is malformed");
      }

      Integer skip = eventSynchronizer.getSkip();
      this.skip = skip != null ? skip : 0;

      pageSize = eventSynchronizer.getPageSize();

      String filterParam = eventSynchronizer.getFilterParam();
      if (filterParam != null && !filterParam.isEmpty())
      {
         filter = filterParam;
      }
      else
      {
         filter = null;
      }
   }

   private void log(Level level, String message)
   {
      LOGGER.log(level, "EventSync#" + getId() + ' ' + message);
   }

   private void log(Level level, String message, Throwable ex)
   {
      LOGGER.log(level, "EventSync#" + getId() + ' ' + message, ex);
   }

   @Override
   public boolean synchronize() throws InterruptedException
   {
      log(Level.INFO, "Events synchronization started");
      try
      {
         Map<String, String> query_param = new HashMap<>();

         if (skip != 0)
         {
            query_param.put("$skip", String.valueOf(skip));
         }

         query_param.put("$top", String.valueOf(pageSize));
         query_param.put("$orderby", "PublicationDate desc");

         if (filter != null)
         {
            query_param.put("$filter", filter);
         }

         ODataFeed eventsFeed = client.readFeed("/Events", query_param);

         // For each entry, creates a DataBase Object
         for (ODataEntry pdt: eventsFeed.getEntries())
         {
            Map<String, Object> properties = pdt.getProperties();

            String category = (String) properties.get(EventEntitySet.CATEGORY);
            String title = (String) properties.get(EventEntitySet.TITLE);
            String description = (String) properties.get(EventEntitySet.DESCRIPTION);

            String subcategory = null;
            if (properties.containsKey(EventEntitySet.SUBCATEGORY))
            {
               subcategory = (String) properties.get(EventEntitySet.SUBCATEGORY);
            }

            Date startDate = null;
            if (properties.containsKey(EventEntitySet.START_DATE))
            {
               startDate = ((GregorianCalendar) properties.get(EventEntitySet.START_DATE)).getTime();
            }

            Date stopDate = null;
            if (properties.containsKey(EventEntitySet.STOP_DATE))
            {
               Object stopDateObject = properties.get(EventEntitySet.STOP_DATE);
               if(stopDateObject != null)
               {
                  stopDate = ((GregorianCalendar) stopDateObject).getTime();
               }
            }

            Date publicationDate = null;
            if (properties.containsKey(EventEntitySet.PUBLICATION_DATE))
            {
               Object publicationDateObject = properties.get(EventEntitySet.PUBLICATION_DATE);
               if(publicationDateObject != null)
               {
                  publicationDate = ((GregorianCalendar) publicationDateObject).getTime();
               }
            }

            String icon = null;
            if (properties.containsKey(EventEntitySet.ICON))
            {
               icon = (String) properties.get(EventEntitySet.ICON);
            }

            boolean localEvent = true;
            if (properties.containsKey(EventEntitySet.LOCAL_EVENT))
            {
               localEvent = (boolean) properties.get(EventEntitySet.LOCAL_EVENT);
            }

            boolean publicEvent = true;
            if (properties.containsKey(EventEntitySet.PUBLIC_EVENT))
            {
               publicEvent = (boolean) properties.get(EventEntitySet.PUBLIC_EVENT);
            }

            String originator = null;
            if (properties.containsKey(EventEntitySet.ORIGINATOR))
            {
               originator = (String) properties.get(EventEntitySet.ORIGINATOR);
            }

            String hubTag = null;
            if (properties.containsKey(EventEntitySet.HUB_TAG))
            {
               hubTag = (String) properties.get(EventEntitySet.HUB_TAG);
            }

            String missionTag = null;
            if (properties.containsKey(EventEntitySet.MISSION_TAG))
            {
               missionTag = (String) properties.get(EventEntitySet.MISSION_TAG);
            }

            String instrumentTag = null;
            if (properties.containsKey(EventEntitySet.INSTRUMENT_TAG))
            {
               instrumentTag = (String) properties.get(EventEntitySet.INSTRUMENT_TAG);
            }

            String externalUrl = null;
            if (properties.containsKey(EventEntitySet.EXTERNAL_URL))
            {
               externalUrl = (String) properties.get(EventEntitySet.EXTERNAL_URL);
            }

            Event event = EVENT_SERVICE.getEventByTitleAndPublicationDate(title, publicationDate);
            boolean update = true;
            if (event == null)
            {
               event = new Event();
               update = false;
            }

            event.setCategory(EventCategory.fromString(category));
            event.setSubcategory(subcategory);
            event.setTitle(title);
            event.setDescription(description);
            event.setStartDate(startDate);
            event.setStopDate(stopDate);
            event.setPublicationDate(publicationDate);
            event.setLocalEvent(localEvent);
            event.setPublicEvent(publicEvent);
            event.setIcon(icon);
            event.setOriginator(originator);
            event.setHubTag(hubTag);
            event.setMissionTag(missionTag);
            event.setInstrumentTag(instrumentTag);
            event.setExternalUrl(externalUrl);

            if (update)
            {
               log(Level.INFO, "Updating event " + event);
               EVENT_SERVICE.updateEventWithoutCredentials(event);
            }
            else
            {
               log(Level.INFO, "Creating event " + event);
               EVENT_SERVICE.createEventWithoutCredentials(event);
            }
         }

         // This is the end, resets `skip` to 0
         if (eventsFeed.getEntries().size() < pageSize)
         {
            this.skip = 0;
         }
      }
      catch (IOException | ODataException ex)
      {
         log(Level.ERROR, "OData failure", ex);
      }
      catch (LockAcquisitionException | CannotAcquireLockException e)
      {
         throw new InterruptedException(e.getMessage());
      }
      finally
      {
         SYNC_SERVICE.saveSynchronizer(this);
      }
      return false;
   }
}
