/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
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
package fr.gael.dhus.olingo.v1;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for URI parsing.
 */
public class UriParsingUtils
{
   public static final Pattern RESSOURCE_PATH_EXTRACTOR = Pattern.compile("odata/v1(/.*)$");

   /**
    * Extracts and returns the resource path (part of the URI that comes after the service URL).
    *
    * @param link from which the resource path is extracted
    * @return the resource path
    * @throws ExpectedException given link is not an URI or does not contain a path
    */
   public static String extractResourcePath(String link) throws ExpectedException
   {
      link = link.trim();
      try
      {
         link = (new URI(link)).getPath();
         if (link == null || link.isEmpty())
         {
            throw new ExpectedException("Invalid link, path is empty");
         }
         // Gets the OData resource path
         Matcher matcher = RESSOURCE_PATH_EXTRACTOR.matcher(link);
         if (matcher.find())
         {
            link = matcher.group(1);
         }
         else
         {
            throw new ExpectedException("Invalid link, path is malformed");
         }
      }
      catch (URISyntaxException e)
      {
         throw new ExpectedException(e.getMessage());
      }

      return link;
   }
}
