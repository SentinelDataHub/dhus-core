/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018-2020 GAEL Systems
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
package org.dhus;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class Util
{
   /**
    * Returns a subList.
    * Does not throw OutOfBounbsException if skip or top is too high.
    *
    * @param <E> type of element in list
    * @param list to partition
    * @param skip or first index
    * @param top maximum number of element in the sublist to return
    * @return a non null sublist (may be empty)
    */
   public static <E> List<E> subList(List<E> list, int skip, int top)
   {
      // skipped everything
      if(top == 0 || skip >= list.size())
      {
         return Collections.emptyList();
      }

      // avoid OOB exception
      int lastIndex = skip + top;
      if(lastIndex > list.size())
      {
         lastIndex = list.size();
      }

      return list.subList(skip, lastIndex);
   }

   /**
    * Escapes regex special characters from the input string, so it can be matched by a Matcher.
    * <p>
    * Escaped characters: { } ( ) [ ] . + * ? ^ $ |
    *
    * @param str input string
    * @return the input string whose special characters have been escaped
    */
   public static String escapeSpecialRegexChars(String str)
   {
      Pattern regexChars = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");
      return regexChars.matcher(str).replaceAll("\\\\$0");
   }
}
