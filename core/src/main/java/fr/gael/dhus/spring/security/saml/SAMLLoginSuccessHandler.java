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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import fr.gael.dhus.spring.security.filter.GDPRAuthFilter;
import fr.gael.dhus.spring.security.handler.LoginSuccessHandler;

@Component(value = "samlLoginSuccessHandler")
public class SAMLLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler
{
   @Autowired
   private LoginSuccessHandler loginSuccessHandler;

   @Autowired 
   private SAMLSavedRequestCache requestCache;
   
   @Override
   public void onAuthenticationSuccess(HttpServletRequest request,
         HttpServletResponse response, Authentication authentication)
         throws ServletException, IOException
   {
      // Call same loginSuccessHandler as default case to set same cookies
      loginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

      HttpSession session = request.getSession(false);
      
      if (session == null)
      {
         super.onAuthenticationSuccess(request, response, authentication);
         return;
      }
      
      SavedRequest savedRequest = requestCache.load(session.getId());
      requestCache.remove(session.getId());

      if (savedRequest == null) 
      {
         super.onAuthenticationSuccess(request, response, authentication);
         return;
      }

      clearAuthenticationAttributes(request);

      // Use the DefaultSavedRequest URL
      String targetUrl = savedRequest.getRedirectUrl();
      getRedirectStrategy().sendRedirect(request, response, targetUrl);
   }
}
