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
package org.dhus.store.datastore.async;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientProperty;

public abstract class AbstractHttpJob
{
   private final String jobId;
   private final String status;
   private final String statusMessage;
   private final String productUuid;
   private final String productName;

   public static final String PROPERTY_ID = "Id";
   public static final String PROPERTY_STATUS = "Status";
   public static final String PROPERTY_STATUS_MESSAGE = "StatusMessage";

   private static final Logger LOGGER = LogManager.getLogger();
   public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'");

   public AbstractHttpJob(ClientEntity orderEntity, String productUuid, String productName)
   {
      this.jobId = getPropertyValue(orderEntity, PROPERTY_ID).toString();
      this.status = getPropertyValue(orderEntity, PROPERTY_STATUS).toString();
      this.statusMessage = getPropertyValue(orderEntity, PROPERTY_STATUS_MESSAGE).toString();
      this.productUuid = productUuid;
      this.productName = productName;
   }

   public String getJobId()
   {
      return jobId;
   }

   public String getStatus()
   {
      return status;
   }

   public String getStatusMessage()
   {
      return statusMessage;
   }

   public String getProductUuid()
   {
      return productUuid;
   }

   public String getProductName()
   {
      return productName;
   }

   public static Object getPropertyValue(ClientEntity entity, String propertyName)
   {
      ClientProperty property = entity.getProperty(propertyName);
      return property == null ? null : property.getValue();
   }

   protected static Date parseDate(String propertyValue)
   {
      try
      {
         return DATE_FORMATTER.parse(propertyValue);
      }
      catch (ParseException e)
      {
         LOGGER.error("Cannot parse date property");
         return null;
      }
   }
}
