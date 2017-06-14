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

import java.util.Map;

import org.dhus.store.Store;

/**
 * The Key store aims to stores key/value pairs.
 * This interface allow to propose a set of implementation based on various technologies.
 */
public interface KeyStore extends Store
{
   /**
    * Put a key into the Store
    *
    * @param key   the key identifier
    * @param value the value related to the key
    * @see Map#put(Object, Object)
    */
   public void put(String key, String value);

   /**
    * Find a value into the key store.
    *
    * @param key the key to be retrieved
    * @return the value referenced by this key
    * @see Map#get(Object)
    */
   public String get(String key);

   /**
    * Remove a key/value couple, then returns the value if retrieved.
    *
    * @param key that references the value
    * @return removed value of null if not retrieved
    * @see Map#remove(Object)
    */
   public String remove(String key);

   /**
    * Checks if the passed key exists in this KeyStore.
    *
    * @param key
    *
    * @return true if given Key exists.
    */
   public boolean exists(String key);
}
