/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2015,2016,2017 GAEL Systems
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
package fr.gael.dhus.spring.security.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;

import fr.gael.dhus.spring.context.SecurityContextProvider;
import fr.gael.dhus.spring.security.CookieKey;
import fr.gael.dhus.spring.security.authentication.ProxyWebAuthenticationDetails;

@Component
public class LogoutSuccessHandler implements
   org.springframework.security.web.authentication.logout.LogoutSuccessHandler
{
   private static final Logger LOGGER = LogManager.getLogger(LogoutSuccessHandler.class);

   @Autowired
   private SecurityContextProvider securityContextProvider;

   @Override
   public void onLogoutSuccess (HttpServletRequest request,
      HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException
   {

      Cookie[] cookies = request.getCookies();
      String integriyCookie = null;
      if (cookies != null)
      {
         for (Cookie cookie: cookies)
         {
            if (CookieKey.AUTHENTICATION_COOKIE_NAME.equals(cookie.getName())
                  || CookieKey.INTEGRITY_COOKIE_NAME.equals(cookie.getName()))
            {
               if(CookieKey.INTEGRITY_COOKIE_NAME.equals(cookie.getName()))
               {
                  integriyCookie = cookie.getValue();
               }
               cookie.setMaxAge(0);
               response.addCookie(cookie);
            }
         }
      }

      String ip = ProxyWebAuthenticationDetails.getRemoteIp(request);
      String name = authentication==null?"unknown":authentication.getName ();

      if ("unknown".equals(name))
      {
         SecurityContext securityContext = securityContextProvider.getSecurityContext(integriyCookie);
         if (securityContext != null)
         {
            Authentication auth = securityContext.getAuthentication();
            name = auth == null ? "unknown" : auth.getName();
         }
      }
      LOGGER.info ("Connection closed by '" + name + "' from " + ip);
      securityContextProvider.logout(integriyCookie);

      request.getSession ().invalidate ();
   }
}
