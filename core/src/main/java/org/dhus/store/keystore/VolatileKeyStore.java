/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016-2018 GAEL Systems
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

import fr.gael.dhus.database.object.KeyStoreEntry;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.Util;
import org.dhus.store.datastore.DataStore;

public class VolatileKeyStore implements KeyStore
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final String VOLATILE_KEYSTORE_NAME = "VolatileKeyStore";

   /** The map to store products keys is referenced by the given product Id. */
   private final Map<VolatileKey, String> map = new LinkedHashMap<>();

   /**
    * Put a key into the store.
    * if key already exists in map, it will be override.
    *
    * @param key   the reference key.
    * @param value the value to be store.
    */
   @Override
   public void put(String key, String tag, String value)
   {
      if (map.containsKey(new VolatileKey(key, tag)));
      {
         LOGGER.warn("Key {}:{} already in the key store, replaced by {}:{}.",
               new VolatileKey(key, tag), map.get(new VolatileKey(key, tag)), new VolatileKey(key, tag), value);
      }
      map.put(new VolatileKey(key, tag) , value);
   }

   @Override
   public String get(String key, String tag)
   {
      return map.get(new VolatileKey(key, tag));
   }

   @Override
   public String remove(String key, String tag)
   {
      return map.remove(new VolatileKey(key, tag));
   }

   @Override
   public boolean exists(String key, String tag)
   {
      return map.containsKey(new VolatileKey(key, tag));
   }

   @Override
   public Iterator<KeyStoreEntry> getOldestEntries()
   {
      return map.entrySet().stream()
            .map(entry -> new KeyStoreEntry(VOLATILE_KEYSTORE_NAME,
                  entry.getKey().getKey(), entry.getKey().getTag(),
                  entry.getValue(), 0L))
            .collect(Collectors.toList()).iterator();
   }

   @Override
   public List<KeyStoreEntry> getEntriesByUuid(String uuid)
   {
      return map.entrySet().stream()
            .filter(entry -> entry.getKey().getKey().equals(uuid))
            .map(entry -> new KeyStoreEntry(VOLATILE_KEYSTORE_NAME,
                  entry.getKey().getKey(), entry.getKey().getTag(),
                  entry.getValue(), 0L))
            .collect(Collectors.toList());
   }

   @Override
   public List<KeyStoreEntry> getUnalteredProductEntries()
   {
      return map.entrySet().stream()
            .filter(entry -> entry.getKey().getTag().equals(DataStore.UNALTERED_PRODUCT_TAG))
            .map(entry -> new KeyStoreEntry(VOLATILE_KEYSTORE_NAME,
                  entry.getKey().getKey(), entry.getKey().getTag(),
                  entry.getValue(), 0L))
            .collect(Collectors.toList());
   }

   @Override
   public List<KeyStoreEntry> getUnalteredProductEntries(int skip, int top)
   {
      List<KeyStoreEntry> entries = getUnalteredProductEntries();
      return Util.subList(entries, skip, top);
   }

   public static class VolatileKey
   {
      private final String key;
      private final String tag;

      public String getKey()
      {
         return key;
      }

      public String getTag()
      {
         return tag;
      }

      private VolatileKey(String key, String tag)
      {
         this.key = key;
         this.tag = tag;
      }

      @Override
      public boolean equals(Object obj)
      {
         if (obj == null)
         {
            return false;
         }
         if (this == obj)
         {
            return true;
         }
         if (obj instanceof VolatileKey)
         {
            VolatileKey other = (VolatileKey) obj;
            return other.key.equals(key) && other.tag.equals(tag);
         }
         return false;
      }

      @Override
      public int hashCode()
      {
         return key.concat(tag).hashCode();
      }
   }
}
