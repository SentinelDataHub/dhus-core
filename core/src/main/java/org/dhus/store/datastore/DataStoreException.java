/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016,2017 GAEL Systems
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
package org.dhus.store.datastore;

import org.dhus.store.StoreException;

/**
 * Exception thrown by DataStore operations.
 */
public class DataStoreException extends StoreException
{
   private static final long serialVersionUID = 1L;

   public DataStoreException()
   {
      super();
   }

   public DataStoreException(String message)
   {
      super(message);
   }

   public DataStoreException(Throwable e)
   {
      super(e);
   }

   public DataStoreException(String message, Throwable e)
   {
      super(message, e);
   }

}
