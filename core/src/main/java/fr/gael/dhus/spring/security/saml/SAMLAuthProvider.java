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
package fr.gael.dhus.spring.security.saml;

import fr.gael.dhus.database.object.User;
import fr.gael.dhus.database.object.restriction.AccessRestriction;
import fr.gael.dhus.service.UserService;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class SAMLAuthProvider extends SAMLAuthenticationProvider
{
   private static final Logger LOGGER = LogManager.getLogger();

   @Autowired
   private UserService userService;

   @Autowired
   private IDPManager idpManager;

   protected final String errorMessage = "There was an error with your "
         + "login/password combination. Please try again.";

   @Override
   public Authentication authenticate(Authentication authentication)
         throws AuthenticationException
   {
      Authentication auth = super.authenticate(authentication);

      // Need to be validated by SAML before attached to a DHuS account.
      if (!auth.isAuthenticated())
      {
         return auth;
      }
      ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      HttpServletRequest request = attributes.getRequest();

      String proxy = null;
      String ip = request.getHeader("X-Forwarded-For");
      if (ip == null)
      {
         ip = request.getRemoteAddr();
      }
      else
      {
         proxy = " (proxy: " + request.getRemoteAddr() + ")";
      }

      LOGGER.info("Connection attempted by '{}' from {}",
            authentication.getName(), (proxy != null ? ip + proxy : ip));

      String username = idpManager.getIdpName() + "~" + auth.getPrincipal();
      User user = userService.getUserNoCheck(username);
      if (user == null)
      {
         User u = new User();
         u.setUsername(username);
         u.generatePassword();
         user = userService.systemCreateSSOUser(u);
      }
      else
      {
         for (AccessRestriction restriction: user.getRestrictions())
         {
            LOGGER.warn("Connection refused for '{}' from {} : account is locked ({})",
                  username, ip, restriction.getBlockingReason());
            throw new LockedException(restriction.getBlockingReason());
         }
      }

      LOGGER.info("Connection success for '{}' from {}", username, ip);
      return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
   }
}
