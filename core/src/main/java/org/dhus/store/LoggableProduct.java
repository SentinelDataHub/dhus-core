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
package org.dhus.store;

import com.google.common.base.Objects;

public class LoggableProduct
{
   private final String uuid;
   private final String identifier;
   private final long size;

   /**
    * Creates new instance. All parameters may be null.
    *
    * @param uuid       of a product
    * @param identifier of a product
    * @param size       in bytes of a product
    */
   public LoggableProduct(String uuid, String identifier, long size)
   {
      this.uuid = uuid;
      this.identifier = identifier;
      this.size = size;
   }

   /**
    * @return the uuid
    */
   public String getUuid()
   {
      return uuid;
   }

   /**
    * @return the identifier
    */
   public String getIdentifier()
   {
      return identifier;
   }

   /**
    * @return the size
    */
   public long getSize()
   {
      return size;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj != null && obj instanceof LoggableProduct)
      {
         LoggableProduct logProd = (LoggableProduct) obj;
         return Objects.equal(logProd.uuid, uuid)
               && Objects.equal(logProd.identifier, identifier)
               && Objects.equal(logProd.size, size);
      }

      return false;
   }

   @Override
   public int hashCode()
   {
      return uuid.hashCode();
   }

   @Override
   public String toString()
   {
      return String.format("%s (%s)", identifier, uuid);
   }

}
