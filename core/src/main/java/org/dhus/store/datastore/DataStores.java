/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2018 GAEL Systems
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

import java.util.List;

/**
 * Utility class for Store objects.
 */
public class DataStores
{
   // FIXME make DataStore comparable and add a default implementation of compareTo to the interface (requires java 8)
   /**
    * Compares two Store objects based on their priority and name.
    *
    * @param ds1
    * @param ds2
    * @return
    */
   public static int compare(DataStore ds1, DataStore ds2)
   {
      int res = ds1.getPriority() - ds2.getPriority();
      if (res == 0)
      {
         return ds1.getName().compareTo(ds2.getName());
      }
      else
      {
         return res;
      }
   }

   public static void throwErrors(List<Throwable> throwableList, String operation, String uuid) throws DataStoreException
   {
      if (throwableList.isEmpty())
      {
         return;
      }

      if (throwableList.size() == 1)
      {
         Throwable throwable = throwableList.get(0);
         if (DataStoreException.class.isAssignableFrom(throwable.getClass()))
         {
            throw DataStoreException.class.cast(throwable);
         }
         else
         {
            throw new DataStoreException(throwable);
         }
      }

      StringBuilder sb = new StringBuilder("Error while performing operation ");
      sb.append(operation).append(" with product ").append(uuid).append(": ");
      for (Throwable throwable: throwableList)
      {
         sb.append('[').append(throwable.getMessage()).append(']');
      }

      throw new DataStoreException(sb.toString());
   }
}
