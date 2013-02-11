/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2014-2017,2019 GAEL Systems
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
package fr.gael.dhus.database.dao.interfaces;

import org.springframework.stereotype.Service;

public class DaoUtils
{
   public static final int DEFAULT_ELEMENTS_PER_PAGE = 3;

   /**
    * Hide utility class constructor
    */
   private DaoUtils ()
   {

   }
   
   /**
    * Escape quote marks in the given string by doubling it (' -> '').
    * @param s String to secure.
    * @return a string with escaped quotes.
    */
   public static String secureString (String s)
   {
      if (s==null) return null;
      return s.replace ("'", "''");
   }
}
