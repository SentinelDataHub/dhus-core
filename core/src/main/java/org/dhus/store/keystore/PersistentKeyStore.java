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
import fr.gael.dhus.service.KeyStoreService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PersistentKeyStore implements KeyStore
{
   private static final Logger LOGGER = LogManager.getLogger();

   private final KeyStoreService keyStoreService;

   /** Name of this persistent key store. */
   private final String name;

   /**
    * Constructs a persistent KeyStore based on the inner DHuS database service.
    *
    * @param name the name of the KeyStore.
    */
   public PersistentKeyStore(String name)
   {
      this.name = name;
      this.keyStoreService = ApplicationContextProvider.getBean(KeyStoreService.class);
   }

   public String getName()
   {
      return name;
   }

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
      KeyStoreEntry kse = keyStoreService.getEntry(name, key, tag);
      if (kse != null)
      {
         LOGGER.warn("Key {}:{} already in the key store, replaced by {}:{}.",
               key, kse.getValue(), key, value);
         kse.setValue(value);
         kse.setInsertionDate(System.currentTimeMillis());
         keyStoreService.updateEntry(kse);
      }
      else
      {
         kse = new KeyStoreEntry(name, key, tag, value, System.currentTimeMillis());
         keyStoreService.createEntry(kse);
      }
   }

   @Override
   public String get(String key, String tag)
   {
      KeyStoreEntry kse = keyStoreService.getEntry(name, key, tag);
      return kse != null ? kse.getValue() : null;
   }

   @Override
   public String remove(String key, String tag)
   {
      KeyStoreEntry kse = keyStoreService.getEntry(name, key, tag);
      if (kse == null)
      {
         return null;
      }
      keyStoreService.deleteEntry(kse);
      return kse.getValue();
   }

   @Override
   public boolean exists(String key, String tag)
   {
      return keyStoreService.exists(name, key, tag);
   }

   @Override
   public Iterator<KeyStoreEntry> getOldestEntries()
   {
      return keyStoreService.getOldestEntries(name);
   }

   @Override
   public List<KeyStoreEntry> getEntriesByUuid(String uuid)
   {
      return keyStoreService.getByUuid(uuid);
   }

   @Override
   public List<KeyStoreEntry> getUnalteredProductEntries()
   {
      return keyStoreService.getUnalteredProductEntries(name);
   }

   @Override
   public List<KeyStoreEntry> getUnalteredProductEntries(int skip, int top)
   {
      return keyStoreService.getUnalteredProductEntries(name, skip, top);
   }
}
