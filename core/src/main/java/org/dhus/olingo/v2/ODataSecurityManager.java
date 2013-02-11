/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018,2019 GAEL Systems
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
package org.dhus.olingo.v2;

import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.service.SecurityService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.util.Locale;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;

public class ODataSecurityManager
{
   private static final SecurityService SECURITY_SERVICE =
         ApplicationContextProvider.getBean(SecurityService.class);

   public static void checkPermission(Role role) throws ODataApplicationException
   {
      if (!SECURITY_SERVICE.getCurrentUser().getRoles().contains(role))
      {
         throw new ODataApplicationException("Insufficient permission to perform this operation",
               HttpStatusCode.UNAUTHORIZED.getStatusCode(), Locale.ENGLISH);
      }
   }

   public static boolean hasPermission(Role role)
   {
      return SECURITY_SERVICE.getCurrentUser().getRoles().contains(role);
   }

   public static User getCurrentUser()
   {
      return SECURITY_SERVICE.getCurrentUser();
   }
}
