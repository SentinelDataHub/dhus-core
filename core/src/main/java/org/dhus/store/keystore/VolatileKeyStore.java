/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016 GAEL Systems
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
package org.dhus.store.keystore;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class VolatileKeyStore implements KeyStore
{
   private static final Logger LOGGER = LogManager.getLogger(VolatileKeyStore.class);

   /** The map to store products keys is referenced by the given product Id. */
   private final Map<String, String> map = new LinkedHashMap<>();

   /**
    * Put a key into the store.
    * if key already exists in map, it will be override.
    *
    * @param key   the reference key.
    * @param value the value to be store.
    */
   @Override
   public void put(String key, String value)
   {
      if (map.containsKey(key))
      {
         LOGGER.warn("Key {}:{} already in the key store, replaced by {}:{}.",
               key, map.get(key), key, value);
      }
      map.put(key, value);
   }

   @Override
   public String get(String key)
   {
      return map.get(key);
   }

   @Override
   public String remove(String key)
   {
      return map.remove(key);
   }

   @Override
   public boolean exists(String id)
   {
      return map.containsKey(id);
   }

}
