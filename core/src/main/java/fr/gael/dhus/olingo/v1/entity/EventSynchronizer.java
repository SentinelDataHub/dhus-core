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

import static fr.gael.dhus.olingo.v1.entityset.EventSynchronizerEntitySet.CREATION_DATE;
import static fr.gael.dhus.olingo.v1.entityset.EventSynchronizerEntitySet.FILTER_PARAM;
import static fr.gael.dhus.olingo.v1.entityset.EventSynchronizerEntitySet.ID;
import static fr.gael.dhus.olingo.v1.entityset.EventSynchronizerEntitySet.LABEL;
import static fr.gael.dhus.olingo.v1.entityset.EventSynchronizerEntitySet.MODIFICATION_DATE;
import static fr.gael.dhus.olingo.v1.entityset.EventSynchronizerEntitySet.PAGE_SIZE;
import static fr.gael.dhus.olingo.v1.entityset.EventSynchronizerEntitySet.REQUEST;
import static fr.gael.dhus.olingo.v1.entityset.EventSynchronizerEntitySet.SCHEDULE;
import static fr.gael.dhus.olingo.v1.entityset.EventSynchronizerEntitySet.SERVICE_LOGIN;
import static fr.gael.dhus.olingo.v1.entityset.EventSynchronizerEntitySet.SERVICE_PASSWORD;
import static fr.gael.dhus.olingo.v1.entityset.EventSynchronizerEntitySet.SERVICE_URL;
import static fr.gael.dhus.olingo.v1.entityset.EventSynchronizerEntitySet.STATUS;
import static fr.gael.dhus.olingo.v1.entityset.EventSynchronizerEntitySet.STATUS_DATE;
import static fr.gael.dhus.olingo.v1.entityset.EventSynchronizerEntitySet.STATUS_MESSAGE;

import fr.gael.dhus.olingo.v1.ExpectedException;
import fr.gael.dhus.olingo.v1.ExpectedException.IncompleteDocException;
import fr.gael.dhus.olingo.v1.ExpectedException.InvalidValueException;
import fr.gael.dhus.service.ISynchronizerService;
import fr.gael.dhus.service.exception.InvokeSynchronizerException;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.sync.SynchronizerStatus;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataException;

import org.quartz.CronExpression;

public final class EventSynchronizer extends AbstractEntity
{
   /** Configuration Object. */
   private final fr.gael.dhus.database.object.config.synchronizer.EventSynchronizer syncConf;

   /** Synchronizer Service, the underlying service. */
   private static final ISynchronizerService SYNC_SERVICE =
         ApplicationContextProvider.getBean(ISynchronizerService.class);


   /**
    * Creates a new EventSynchronizer from its ID.
    *
    * @param sync_id synchronizer's ID.
    * @throws NullPointerException if no synchronizer has the given ID
    * @throws ODataException if synchronizer #sync_id is not an instance of ODataEventSynchronizer
    */
   public EventSynchronizer(long sync_id) throws ODataException
   {
      this(SYNC_SERVICE.getSynchronizerConfById(sync_id,
            fr.gael.dhus.database.object.config.synchronizer.EventSynchronizer.class));
   }

   /**
    * Creates a new EventSynchronizer from a database object.
    *
    * @param eventSynchronizer database object
    * @throws ODataException if `sync_conf` is not an instance of ODataEventSynchronizer
    */
   public EventSynchronizer(fr.gael.dhus.database.object.config.synchronizer.EventSynchronizer eventSynchronizer)
         throws ODataException
   {
      Objects.requireNonNull(eventSynchronizer);
      this.syncConf = eventSynchronizer;
   }

   /**
    * Creates an new EventSynchronizer from the given ODataEntry.
    *
    * @param odata_entry created by a POST request on the OData interface
    * @throws ODataException if the given entry is malformed
    */
   public EventSynchronizer(ODataEntry odata_entry) throws ODataException
   {
      Map<String, Object> props = odata_entry.getProperties();

      String label = (String) props.get(LABEL);
      String schedule = (String) props.get(SCHEDULE);
      String request = (String) props.get(REQUEST);
      String service_url = (String) props.get(SERVICE_URL);

      if (schedule == null || schedule.isEmpty() || service_url == null || service_url.isEmpty())
      {
         throw new IncompleteDocException();
      }

      if (request != null && !request.equals("start") && !request.equals("stop"))
      {
         throw new InvalidValueException(REQUEST, request);
      }

      try
      {
         this.syncConf = SYNC_SERVICE.createSynchronizer(label, schedule,
               fr.gael.dhus.database.object.config.synchronizer.EventSynchronizer.class);
         setDefaults(odata_entry);
         updateFromEntry(odata_entry);
      }
      catch (ParseException | ReflectiveOperationException e)
      {
         throw new ExpectedException(e.getMessage());
      }
   }

   /**
    * Called by {@link #EventSynchronizer(ODataEntry)}, sets the default values for null properties
    * from the create document.
    *
    * @param odataEntry create OData document
    */
   private void setDefaults(ODataEntry odataEntry)
   {
      Map<String, Object> props = odataEntry.getProperties();

      Integer page_size = (Integer) props.get(PAGE_SIZE);
      if (page_size == null)
      {
         this.syncConf.setPageSize(500);
      }
   }

   @Override
   public void updateFromEntry(ODataEntry entry) throws ODataException
   {
      Map<String, Object> props = entry.getProperties();

      String schedule = (String) props.remove(SCHEDULE);
      String request = (String) props.remove(REQUEST);
      String service_url = (String) props.remove(SERVICE_URL);
      String filterParam = (String) props.remove(FILTER_PARAM);
      String label = (String) props.remove(LABEL);
      String serviceLogin = (String) props.remove(SERVICE_LOGIN);
      String servicePassword = (String) props.remove(SERVICE_PASSWORD);
      Integer page_size = (Integer) props.remove(PAGE_SIZE);

      // To avoid any side effects
      SYNC_SERVICE.deactivateSynchronizer(this.syncConf.getId());

      if (schedule != null && !schedule.isEmpty())
      {
         try
         {
            CronExpression.validateExpression(schedule);
            this.syncConf.setSchedule(schedule);
         }
         catch (ParseException ex)
         {
            throw new ExpectedException(ex.getMessage());
         }
      }

      if (request != null && !request.isEmpty())
      {
         if (request.equals("start"))
         {
            this.syncConf.setActive(true);
         }
         else if (request.equals("stop"))
         {
            this.syncConf.setActive(false);
         }
         else
         {
            throw new InvalidValueException(REQUEST, request);
         }
      }

      if (service_url != null && !service_url.isEmpty())
      {
         this.syncConf.setServiceUrl(service_url);
      }

      if (label != null && !label.isEmpty())
      {
         this.syncConf.setLabel(label);
      }

      if (serviceLogin != null && !serviceLogin.isEmpty())
      {
         this.syncConf.setServiceLogin(serviceLogin);
      }

      if (servicePassword != null && !servicePassword.isEmpty())
      {
         this.syncConf.setServicePassword(servicePassword);
      }

      if (filterParam != null && !filterParam.isEmpty())
      {
         this.syncConf.setFilterParam(filterParam);
      }

      if (page_size != null)
      {
         this.syncConf.setPageSize(page_size);
      }

      try
      {
         SYNC_SERVICE.saveSynchronizerConf(this.syncConf);
      }
      catch (InvokeSynchronizerException e)
      {
         throw new ODataException(e);
      }
   }

   @Override
   public Map<String, Object> toEntityResponse(String root_url)
   {
      SynchronizerStatus ss = SYNC_SERVICE.getStatus(this.syncConf);
      Map<String, Object> res = new HashMap<>();

      res.put(ID, this.syncConf.getId());
      res.put(LABEL, this.syncConf.getLabel());
      res.put(SCHEDULE, this.syncConf.getSchedule());
      res.put(REQUEST, this.syncConf.isActive() ? "start" : "stop");
      res.put(STATUS, ss.status.toString());
      res.put(STATUS_DATE, ss.since);
      res.put(STATUS_MESSAGE, ss.message);
      res.put(CREATION_DATE, this.syncConf.getCreated().toGregorianCalendar());
      res.put(MODIFICATION_DATE, this.syncConf.getModified().toGregorianCalendar());
      res.put(SERVICE_URL, this.syncConf.getServiceUrl());
      res.put(SERVICE_LOGIN, this.syncConf.getServiceLogin());
      res.put(SERVICE_PASSWORD, "***");
      res.put(FILTER_PARAM, this.syncConf.getFilterParam());
      res.put(PAGE_SIZE, this.syncConf.getPageSize());

      return res;
   }

   @Override
   public Object getProperty(String prop_name) throws ODataException
   {
      SynchronizerStatus ss = SYNC_SERVICE.getStatus(this.syncConf);
      Object res;

      switch (prop_name)
      {
         case ID:
            res = this.syncConf.getId();
            break;
         case LABEL:
            res = this.syncConf.getLabel();
            break;
         case SCHEDULE:
            res = this.syncConf.getSchedule();
            break;
         case REQUEST:
            res = this.syncConf.isActive() ? "start" : "stop";
            break;
         case STATUS:
            res = ss.status.toString();
            break;
         case STATUS_DATE:
            res = ss.since;
            break;
         case STATUS_MESSAGE:
            res = ss.message;
            break;
         case CREATION_DATE:
            res = this.syncConf.getCreated().toGregorianCalendar();
            break;
         case MODIFICATION_DATE:
            res = this.syncConf.getModified().toGregorianCalendar();
            break;
         case SERVICE_URL:
            res = this.syncConf.getServiceUrl();
            break;
         case SERVICE_LOGIN:
            res = this.syncConf.getServiceLogin();
            break;
         case SERVICE_PASSWORD:
            res = this.syncConf.getServicePassword();
            break;
         case FILTER_PARAM:
            res = this.syncConf.getFilterParam();
            break;
         case PAGE_SIZE:
            res = this.syncConf.getPageSize();
            break;

         default:
            throw new ODataException("Unknown property: " + prop_name);
      }

      return res;
   }

}
