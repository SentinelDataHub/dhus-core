/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2017 GAEL Systems
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
package fr.gael.dhus.spring.security.authentication;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.authentication.WebAuthenticationDetails;

@SuppressWarnings ("serial")
public class ProxyWebAuthenticationDetails extends WebAuthenticationDetails
{
   private final String remoteAddress;
   private String proxyAddress = null;

   public ProxyWebAuthenticationDetails(HttpServletRequest request)
   {
      super (request);
      this.remoteAddress = getRemoteIp (this, request);
   }
   
   @Override
   public String getRemoteAddress()
   {
      return remoteAddress;
   }

   private void setProxy(String proxyAddress)
   {
      this.proxyAddress = proxyAddress;
   }

   public String getProxyAddress()
   {
      return proxyAddress;
   }

   public static String getRemoteIp (HttpServletRequest request)
   {
      return getRemoteIp(null, request);
   }

   public static String getRemoteIp(ProxyWebAuthenticationDetails details, HttpServletRequest request)
   {
      String ip = request.getHeader("X-Forwarded-For");
      if (ip == null)
      {
         ip = request.getRemoteAddr();
      }
      else if (details != null)
      {
         details.setProxy(request.getRemoteAddr());
      }
      return ip;
   }
   
   public static String getRemoteHost (HttpServletRequest request)
   {
      String host = request.getHeader("X-Forwarded-Host");
      if (host == null) host=request.getRemoteHost();
      return host;
   }
}
