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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fr.gael.dhus.database.object.KeyStoreEntry;

/**
 * The Key store aims to stores key-tag/value pairs.
 * This interface allow to propose a set of implementation based on various technologies.
 */
public interface KeyStore
{
   /**
    * Put a key into the Store
    *
    * @param key the key identifier
    * @param tag the tag that categorizes the value
    * @param value the value related to the key
    * @see Map#put(Object, Object)
    */
   public void put(String key, String tag, String value);

   /**
    * Find a value into the key store.
    *
    * @param key the key to be retrieved
    * @param tag the tag that categorizes the value
    * @return the value referenced by this key
    * @see Map#get(Object)
    */
   public String get(String key, String tag);

   /**
    * Remove a key/value couple, then returns the value if retrieved.
    *
    * @param key that references the value
    * @param tag the tag that categorizes the value
    * @return removed value of null if not retrieved
    * @see Map#remove(Object)
    */
   public String remove(String key, String tag);

   /**
    * Checks if the passed key exists in this KeyStore.
    *
    * @param key that references the value
    * @param tag the tag that categorizes the value
    *
    * @return true if given Key exists.
    */
   public boolean exists(String key, String tag);

   /**
    * Returns an Iterator over the entries in this KeyStore, starting from
    * the oldest.
    *
    * @return an Iterator of KeyStoreEntry
    */
   public Iterator<KeyStoreEntry> getOldestEntries();

   /**
    * Returns all the entries in this KeyStore associated with the key uuid.
    *
    * @param uuid the key associated to entries
    * @return a list of KeyStoreEntry
    */
   public List<KeyStoreEntry> getEntriesByUuid(String uuid);

   /**
    * Returns a non-null list of KeyStore Entries referencing unaltered products.
    *
    * @return list of KeyStoreEntriy
    */
   public List<KeyStoreEntry> getUnalteredProductEntries();

   /**
    * Returns a non-null list of KeyStore Entries referencing unaltered products.
    *
    * @param skip number of entries to skip (not added to the returned list)
    * @param top maximum number of entries to return (size of returned list it at most `top`)
    * @return list of KeyStoreEntriy
    */
   public List<KeyStoreEntry> getUnalteredProductEntries(int skip, int top);
}
