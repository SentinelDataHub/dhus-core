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
package fr.gael.dhus.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.gael.dhus.database.dao.KeyStoreEntryDao;
import fr.gael.dhus.database.object.KeyStoreEntry;
import fr.gael.dhus.database.object.KeyStoreEntry.Key;

@Service
public class KeyStoreService
{
   @Autowired
   private KeyStoreEntryDao keyStoreEntryDao;

   /**
    * Create a keyStore entry.
    *
    * @param entry to create
    */
   public void createEntry(KeyStoreEntry entry)
   {
      keyStoreEntryDao.create(entry);
   }

   /**
    * Return KeyStoreEntry corresponding to given couple (keyStoreName, entryKey).
    *
    * @param keyStoreName
    * @param entryKey
    * @return
    */
   public KeyStoreEntry getEntry(String keyStoreName, String entryKey)
   {
      return getEntry(new Key(keyStoreName, entryKey));
   }

   /**
    * Return KeyStoreEntry corresponding to given key.
    *
    * @param key
    * @return
    */
   public KeyStoreEntry getEntry(Key key)
   {
      return keyStoreEntryDao.read(key);
   }

   /**
    * Update an existing entry, or create it if it does not exist.
    *
    * @param entry
    */
   public void updateEntry(KeyStoreEntry entry)
   {
      KeyStoreEntry ks = keyStoreEntryDao.read(entry.getKey());
      if (ks != null)
      {
         ks.setValue(entry.getValue());
         keyStoreEntryDao.update(ks);
      }
      else
      {
         keyStoreEntryDao.create(entry);
      }
   }

   /**
    * Delete given entry.
    *
    * @param entry
    */
   public void deleteEntry(KeyStoreEntry entry)
   {
      keyStoreEntryDao.delete(entry);
   }

   /**
    * Test if an entry exist for given couple (keyStoreName, entryKey).
    *
    * @param keyStoreName
    * @param entryKey
    * @return
    */
   public boolean exists(String keyStoreName, String entryKey)
   {
      return exists(new Key(keyStoreName, entryKey));
   }

   /**
    * Test if an entry exist for given key.
    *
    * @param key
    * @return
    */
   public boolean exists(Key key)
   {
      return keyStoreEntryDao.read(key) != null;
   }
}
