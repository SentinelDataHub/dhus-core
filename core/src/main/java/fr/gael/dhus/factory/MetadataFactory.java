/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018 GAEL Systems
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
package fr.gael.dhus.factory;

import fr.gael.dhus.database.object.MetadataDefinition;
import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.service.MetadataService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Metadata instances factory.
 */
public class MetadataFactory
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final MetadataService METADATA_SERVICE = ApplicationContextProvider.getBean(MetadataService.class);

   private MetadataFactory() {}

   public static MetadataIndex createMetadataIndex(String name, String type, String category, String queryable, String value)
   {
      MetadataDefinition metadataDefinition = METADATA_SERVICE.getMetadataDefinition(name, type, category, queryable);
      if (metadataDefinition == null)
      {
         metadataDefinition = new MetadataDefinition(name, type, category, queryable);
         LOGGER.info("MetadataDefinition created : {}", metadataDefinition);
      }
      return new MetadataIndex(metadataDefinition, value);
   }

   /**
    * DHuS Ingestion Date metadata.
    */
   public static class IngestionDate
   {
      public static final String NAME = "Ingestion Date";
      public static final String TYPE = "text/date+iso8601";
      public static final String CATEGORY = "product";
      public static final String QUERYABLE = "ingestionDate";
      public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

      private IngestionDate() {}

      public static MetadataIndex create(Date date)
      {
         return createMetadataIndex(NAME, TYPE, CATEGORY, QUERYABLE, DATE_FORMAT.format(date));
      }
   }

   /**
    * DHuS Identifier metadata.
    */
   public static class Identifier
   {
      public static final String NAME = "Identifier";
      public static final String TYPE = "text/plain";
      public static final String CATEGORY = "summary";
      public static final String QUERYABLE = "identifier";

      private Identifier() {}

      public static MetadataIndex create(String identifier)
      {
         return createMetadataIndex(NAME, TYPE, CATEGORY, QUERYABLE, identifier);
      }
   }
}
