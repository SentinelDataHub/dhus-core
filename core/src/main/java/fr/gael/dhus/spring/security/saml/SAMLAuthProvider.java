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

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.providers.ExpiringUsernameAuthenticationToken;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import fr.gael.dhus.database.object.User;
import fr.gael.dhus.service.UserService;

public class SAMLAuthProvider extends SAMLAuthenticationProvider
{
   private static final Logger LOGGER = LogManager.getLogger();

   @Autowired
   private UserService userService;

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

      String uuid = ((User)auth.getDetails()).getUUID();
      String username = SAMLUtil.hash(auth.getPrincipal().toString());
      LOGGER.info("Connection attempted by user '{}' from {}",
            username, (proxy != null ? ip + proxy : ip));

      LOGGER.info("*temp* KEYCLOAK Information - UUID : " + ((User)auth.getDetails()).getUUID() +" - Roles : "+ auth.getAuthorities());
      // remove previous log and use information in following User creation/storage or update
      User user = userService.getUserNoCheck(username);
      if (user == null)
      {
         User u = new User();
         u.setUsername(username);
         u.setUUID(uuid);
         u.setRoles(((User)auth.getDetails()).getRoles());
         u.generatePassword();
         user = userService.systemCreateSSOUser(u);
      }
      else
      {
         user.setRoles(((User)auth.getDetails()).getRoles());
         userService.systemUpdateSSOUser(user);
      }
      user.setExtendedRoles(((User)auth.getDetails()).getExtendedRoles());

      Date tokenExpiration = ((ExpiringUsernameAuthenticationToken) auth).getTokenExpiration();
      
      LOGGER.info("Connection success for user '{}' from {} - token expires on {}", user.getUsername(), ip, tokenExpiration);
      return new ExpiringUsernameAuthenticationToken(((ExpiringUsernameAuthenticationToken)auth).getTokenExpiration(), user, null, user.getAuthorities());
   }
}
