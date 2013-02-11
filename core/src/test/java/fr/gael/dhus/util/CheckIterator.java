/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2014,2015,2017 GAEL Systems
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
package fr.gael.dhus.util;

import java.util.Iterator;

public class CheckIterator
{

   /**
    * Checks number of elements within a iterator.
    *
    * @param it iterator to check
    * @param n  number of elements found
    * @return true if n equals elements number in iterator, otherwise false.
    */
   public static boolean checkElementNumber(Iterator<?> it, int n)
   {
      int i = 0;
      while (it.hasNext())
      {
         if (it.next() == null)
         {
            return false;
         }
         i++;
      }
      return (i == n);
   }
}
