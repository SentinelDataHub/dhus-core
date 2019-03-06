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
package org.dhus.test.olingo.scenario;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.dhus.test.olingo.OlingoTestException;
import org.dhus.test.olingo.TestManager;
import org.dhus.test.olingo.Utils;
import org.dhus.test.olingo.operations.ODataOperator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
@FunctionalTest
public class EventsCRUDScenario implements TestScenario
{  
   public static final String ENTITY_SET = "Events";
   
   // properties
   public static final String ID = "Id";
   public static final String CATEGORY = "Category";
   public static final String SUBCATEGORY = "Subcategory";
   public static final String TITLE = "Title";
   public static final String DESCRIPTION = "Description";
   public static final String STARTDATE = "StartDate";
   public static final String STOPDATE = "StopDate";
   public static final String PUBLICATIONDATE = "PublicationDate";
   public static final String ICON = "Icon";
   public static final String ORIGINATOR = "Originator";
   public static final String HUBTAG = "HubTag";
   public static final String MISSIONTAG = "MissionTag";
   public static final String INSTRUMENTTAG = "InstrumentTag";
   public static final String EXTERNALURL = "ExternalUrl";
   public static final String LOCALEVENT = "LocalEvent";
   public static final String PUBLICVENT = "PublicEvent";

   // property values
   public static final Long ID_VALUE = 0L;
   public static final String CATEGORY_VALUE = "Other";
   public static final String SUBCATEGORY_VALUE = "Test";
   public static final String TITLE_VALUE = "My Event";
   public static final String DESCRIPTION_VALUE = "Description of the event";
   public static final String STARTDATE_VALUE = "2017-04-28T09:00:00";
   public static final String STOPDATE_VALUE = "2017-04-29T09:00:00";
   public static final String PUBLICATIONDATE_VALUE = "2017-04-28T00:00:00";
   public static final String ICON_VALUE = "/path/to/icon";
   public static final String ORIGINATOR_VALUE = "Origin";
   public static final String HUBTAG_VALUE = "HT01";
   public static final String MISSIONTAG_VALUE = "MT01";
   public static final String INSTRUMENTTAG_VALUE = "IT01";
   public static final String EXTERNALURL_VALUE = "http://my.domain.com";
   public static final Boolean LOCALEVENT_VALUE = true;
   public static final Boolean PUBLICEVENT_VALUE = false;
   
   // alternate property values
   public static final String TITLE_ALT_VALUE = "My Event 2";
   public static final String MISSIONTAG_ALT_VALUE = "MT02";
   public static final String INSTRUMENTTAG_ALT_VALUE = "IT02";
   
   @Autowired
   private ODataOperator odataOperator;
   
   private final Map<String, Object> referenceProperties;
   private final Map<String, Object> alternateProperties;

   public EventsCRUDScenario() throws ParseException
   {
      this.referenceProperties = Utils.makeEntryMap(listReferenceKeys(), listReferenceValues());
      this.alternateProperties = Utils.makeEntryMap(listReferenceKeys(), listAlternateValues());
   }
   
   @Override
   public void execute() throws OlingoTestException
   {
      try
      {
         createEvent();
         readEvents();
         readEvent();
         updateEvent();
         deleteEvent();
      }
      catch (Exception e)
      {
         throw new OlingoTestException(e);
      }
   }

   private void createEvent() throws Exception, OlingoTestException
   {
      // create event with references
      ODataEntry createdEntry =  this.odataOperator.createEntry(ENTITY_SET, this.referenceProperties);
   
      if (createdEntry == null)
      {
         throw new OlingoTestException("Failed to create Event entity.");
      }
      TestManager.logScenarioInfo("Event entity created.");
      
      // update references with generated ID
      Long eventId = (Long) createdEntry.getProperties().get(ID);
      referenceProperties.put(ID, eventId);
      alternateProperties.put(ID, eventId);
      
      // check property values
      if(!Utils.validateProperties(referenceProperties, createdEntry.getProperties()))
      {
         throw new OlingoTestException("Invalid created Event entity.");
      } 
   }

   private void readEvents() throws IOException, ODataException, OlingoTestException
   {
      ODataFeed eventsFeed = odataOperator.readFeed(ENTITY_SET);
   
      if(eventsFeed == null || eventsFeed.getEntries().isEmpty())
      {
         throw new OlingoTestException("Failed to read Event entity collection.");
      }
      TestManager.logScenarioInfo("Event entity collection read.");
   }
   
   private void readEvent() throws IOException, ODataException, OlingoTestException
   {
      // get ID retrieved at creation
      Long eventId = (Long) referenceProperties.get(ID);
      
      // read event entry
      ODataEntry entry = odataOperator.readEntry(ENTITY_SET, String.valueOf(eventId));
      
      if (entry == null)
      {
         throw new OlingoTestException("Failed to read Event entity.");
      }
      TestManager.logScenarioInfo("Event entity read.");
      
      // check property values
      if(!Utils.validateProperties(referenceProperties, entry.getProperties()))
      {
         throw new OlingoTestException("Invalid read Event entity.");
      }
   }

   private void updateEvent() throws Exception
   {
      // get ID retrieved at creation
      Long eventId = (Long) alternateProperties.get(ID);
      
      // update event with alternate properties
      int statusCode = odataOperator.updateEntry(ENTITY_SET, String.valueOf(eventId), alternateProperties);
      
      if (statusCode != ODataOperator.HTTP_NO_CONTENT)
      {
         throw new OlingoTestException("Failed to update Event entity.");
      }
      TestManager.logScenarioInfo("Event entity updated.");
      
      // check updated property values
      ODataEntry updatedEntry = odataOperator.readEntry(ENTITY_SET, String.valueOf(eventId));
      if(updatedEntry == null || !Utils.validateProperties(alternateProperties, updatedEntry.getProperties()))
      {
         throw new OlingoTestException("Invalid updated Event entity.");
      }
   }
   
   private void deleteEvent() throws IOException, OlingoTestException, ODataException
   {
      // get ID retrieved at creation
      Long eventId = (Long) referenceProperties.get(ID);
      
      // delete event
      int statusCode = odataOperator.deleteEntry(ENTITY_SET, String.valueOf(eventId));
      
      // check deletion success
      if(statusCode != ODataOperator.HTTP_NO_CONTENT || odataOperator.readEntry(ENTITY_SET, String.valueOf(eventId)) != null)
      {
         throw new OlingoTestException("Failed to delete Event entity.");
      }
      TestManager.logScenarioInfo("Event entity deleted.");
   }

   private static List<String> listReferenceKeys()
   {
      return Arrays.asList(
            ID,
            CATEGORY,
            SUBCATEGORY,
            TITLE,
            DESCRIPTION,
            STARTDATE,
            STOPDATE,
            PUBLICATIONDATE,
            ICON,
            ORIGINATOR,
            HUBTAG,
            MISSIONTAG,
            INSTRUMENTTAG,
            EXTERNALURL,
            LOCALEVENT,
            PUBLICVENT);
   }

   private static List<Object> listReferenceValues() throws ParseException
   {
      return Arrays.<Object>asList(
            ID_VALUE,
            CATEGORY_VALUE,
            SUBCATEGORY_VALUE,
            TITLE_VALUE,
            DESCRIPTION_VALUE,
            Utils.parseToGregorianCalendar(STARTDATE_VALUE),
            Utils.parseToGregorianCalendar(STOPDATE_VALUE),
            Utils.parseToGregorianCalendar(PUBLICATIONDATE_VALUE),
            ICON_VALUE,
            ORIGINATOR_VALUE,
            HUBTAG_VALUE,
            MISSIONTAG_VALUE,
            INSTRUMENTTAG_VALUE,
            EXTERNALURL_VALUE,
            LOCALEVENT_VALUE,
            PUBLICEVENT_VALUE);
   }

   private static List<Object> listAlternateValues() throws ParseException
   {
      return Arrays.<Object>asList(
            ID_VALUE,
            CATEGORY_VALUE,
            SUBCATEGORY_VALUE,
            TITLE_ALT_VALUE,
            DESCRIPTION_VALUE,
            Utils.parseToGregorianCalendar(STARTDATE_VALUE),
            Utils.parseToGregorianCalendar(STOPDATE_VALUE),
            Utils.parseToGregorianCalendar(PUBLICATIONDATE_VALUE),
            ICON_VALUE,
            ORIGINATOR_VALUE,
            HUBTAG_VALUE,
            MISSIONTAG_ALT_VALUE,
            INSTRUMENTTAG_ALT_VALUE,
            EXTERNALURL_VALUE,
            LOCALEVENT_VALUE,
            PUBLICEVENT_VALUE);
   }
}
