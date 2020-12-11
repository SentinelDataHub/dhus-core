/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2014-2016,2019 GAEL Systems
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

import fr.gael.dhus.database.object.User;
import fr.gael.dhus.database.object.User.PasswordEncryption;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

// TODO: migrate to https://www.baeldung.com/spring-security-5-password-storage
public class CustomAuthProvider extends DaoAuthenticationProvider
{
   private static final Logger LOGGER = LogManager.getLogger(CustomAuthProvider.class);

   @Override
   protected void additionalAuthenticationChecks (UserDetails user_details,
      UsernamePasswordAuthenticationToken authentication)
      throws AuthenticationException
   {
      User u = (User) user_details;
      if (u.getPasswordEncryption () != PasswordEncryption.NONE)
      {
         try
         {
            super.setPasswordEncoder (new MessageDigestPasswordEncoder (u
               .getPasswordEncryption ().getAlgorithmKey ()));
         }
         catch (Exception e)
         {
            LOGGER.warn("Algorithm " +
               u.getPasswordEncryption ().getAlgorithmKey () +
               " was not found. Trying with no encryption.");
         }
      }
      else
      {
         super.setPasswordEncoder(NoOpPasswordEncoder.getInstance());
      }
      super.additionalAuthenticationChecks (user_details, authentication);
   }
}
