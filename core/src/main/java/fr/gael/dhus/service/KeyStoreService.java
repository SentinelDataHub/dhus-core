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
package fr.gael.dhus.service;

import java.util.Iterator;
import java.util.List;

import org.hibernate.ScrollableResults;
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
   public KeyStoreEntry getEntry(String keyStoreName, String entryKey, String tag)
   {
      return getEntry(new Key(keyStoreName, entryKey, tag));
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
   public boolean exists(String keyStoreName, String entryKey, String tag)
   {
      return exists(new Key(keyStoreName, entryKey, tag));
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

   /**
    * Retrieves entries of a keyStore, oldest first.
    * @param keyStoreName the name of the keyStore
    * @return an Iterator of KeyStoreEntry
    */
   public Iterator<KeyStoreEntry> getOldestEntries(String keyStoreName)
   {
      final ScrollableResults entries = keyStoreEntryDao.readOldestEntries(keyStoreName);
      return new Iterator<KeyStoreEntry>()
      {
         @Override
         public boolean hasNext()
         {
            return entries.next();
         }

         @Override
         public KeyStoreEntry next()
         {
            return (KeyStoreEntry) entries.get(0);
         }

         @Override
         public void remove()
         {
            throw new UnsupportedOperationException("Remove not supported.");
         }
      };
   }

   public List<KeyStoreEntry> getByUuid(String uuid)
   {
      return keyStoreEntryDao.getByUuid(uuid);
   }

   /**
    * Returns a non-null list of unaltered products KeyStoreEntries known to given KeyStore.
    *
    * @param keyStoreName name of KeyStore to query
    * @return list of KeyStoreEntry
    */
   public List<KeyStoreEntry> getUnalteredProductEntries(String keyStoreName)
   {
      return keyStoreEntryDao.getUnalteredProductEntries(keyStoreName, null, null);
   }

   /**
    * Returns a non-null list of unaltered products KeyStoreEntries known to given KeyStore.
    *
    * @param keyStoreName name of KeyStore to query
    * @param skip number of entries to skip (not added to the returned list)
    * @param top maximum number of entries to return (size of returned list it at most `top`)
    * @return list of KeyStoreEntry
    */
   public List<KeyStoreEntry> getUnalteredProductEntries(String keyStoreName, int skip, int top)
   {
      return keyStoreEntryDao.getUnalteredProductEntries(keyStoreName, skip, top);
   }
}
