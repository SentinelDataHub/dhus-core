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
package org.dhus.store.datastore.openstack;

import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class OpenStackLocation
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String STR_PATTERN = "%s;%s;%s";

   /** Data separator */
   public static final String SEPARATOR = ";";

   private final String region;
   private final String container;
   private final String objectName;

   public OpenStackLocation(String to_parse)
   {
      String[] values = to_parse.split(Pattern.quote(SEPARATOR));
      if (values.length != 3)
      {
         LOGGER.error("product resource location error: {}", to_parse);
         throw new IllegalArgumentException("product resource location error: " + to_parse);
      }
      this.region = values[0];
      this.container = values[1];
      this.objectName = values[2];
   }

   public OpenStackLocation(String region, String container, String object_name)
   {
      this.region = Objects.requireNonNull(region);
      this.container = Objects.requireNonNull(container);
      this.objectName = Objects.requireNonNull(object_name);
   }

   public String getRegion()
   {
      return this.region;
   }

   public String getContainer()
   {
      return this.container;
   }

   public String getObjectName()
   {
      return this.objectName;
   }

   public String toResourceLocation()
   {
      return new StringBuilder(region).append(SEPARATOR).append(container)
            .append(SEPARATOR).append(objectName).toString();
   }

   @Override
   public String toString()
   {
      return String.format(STR_PATTERN, region, container, objectName);
   }
}
