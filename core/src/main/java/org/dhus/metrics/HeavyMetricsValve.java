/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019 GAEL Systems
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
package org.dhus.metrics;

import com.codahale.metrics.MetricRegistry;

import fr.gael.dhus.server.http.valve.AccessValve;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.spring.context.SecurityContextProvider;
import fr.gael.dhus.spring.security.CookieKey;

import java.io.IOException;

import javax.servlet.http.Cookie;

import org.apache.catalina.connector.Request;

import org.springframework.security.core.context.SecurityContext;

/**
 * Valve to compute heavy metrics, adds a user dimension to the metrics computed by the more basic
 * {@link MetricsValve}.
 * <p>
 * Removing that valve from the pipeline does not affect other components.
 * <p>
 * <strong>Do not use both this valve and the basic {@link MetricsValve}</strong>.
 */
public class HeavyMetricsValve extends MetricsValve
{
   private static final SecurityContextProvider SEC_CTX_PROVIDER =
         ApplicationContextProvider.getBean(SecurityContextProvider.class);

   @Override
   protected String getMetricPrefix(Request request)
   {
      return MetricRegistry.name(super.getMetricPrefix(request), getUserName(request));
   }

   private String getUserName(Request request)
   {
      Cookie integrityCookie = CookieKey.getIntegrityCookie(request.getCookies());
      String requestHeader = request.getHeader("Authorization");

      if (integrityCookie != null)
      {
         String integrity = integrityCookie.getValue();
         if (integrity != null && !integrity.isEmpty())
         {
            SecurityContext ctx = SEC_CTX_PROVIDER.getSecurityContext(integrity);
            if ((ctx != null) && (ctx.getAuthentication() != null))
            {
               return ctx.getAuthentication().getName();
            }
         }
      }
      if (requestHeader != null)
      {
         try
         {
            String[] basicAuth = AccessValve.extractAndDecodeHeader(requestHeader);
            if (basicAuth != null)
            {
               return basicAuth[0];
            }
         }
         catch (IOException suppressed) {}
      }
      return "~none"; // the tilde is a forbidden character for a username: no clash with a username
   }

}
