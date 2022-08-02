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

import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.spring.context.SecurityContextProvider;
import fr.gael.dhus.spring.security.CookieKey;

import javax.servlet.http.HttpSessionEvent;

import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SessionDestroyedCleaner extends HttpSessionEventPublisher
{
   @Override
   public void sessionDestroyed(final HttpSessionEvent event)
   {
      SecurityContextProvider securityContextProvider = ApplicationContextProvider.getBean(SecurityContextProvider.class);
      securityContextProvider.logout((String) event.getSession().getAttribute(CookieKey.INTEGRITY_ATTRIBUTE_NAME));
      super.sessionDestroyed(event);
   }
}
