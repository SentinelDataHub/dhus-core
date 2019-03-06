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
package fr.gael.dhus.spring.context;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;

import fr.gael.dhus.server.http.valve.NoPendingEvictionPolicy;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

@Component
public class SecurityContextProvider
{
   private static final Logger LOGGER = LogManager.getLogger(SecurityContextProvider.class);
   private static final String CACHE_MANAGER_NAME = "dhus_cache";
   private static final String CACHE_NAME = "security_context";
   private static Cache cache;

   private static Cache getCache()
   {
      if (cache == null)
      {
         cache = CacheManager.getCacheManager(CACHE_MANAGER_NAME).getCache(CACHE_NAME);

         // Override the current eviction policy to avoid removing pending elements
         cache.setMemoryStoreEvictionPolicy(new NoPendingEvictionPolicy(cache.getMemoryStoreEvictionPolicy()));
      }
      return cache;
   }

   public SecurityContextProvider() {}

   public SecurityContext getSecurityContext(String key)
   {
      if (key == null)
      {
         return null;
      }
      Element e = getCache().get(key);
      if (e == null)
      {
         return null;
      }
      return (SecurityContext) getCache().get(key).getObjectValue();
   }

   public void saveSecurityContext(String key, SecurityContext ctx)
   {
      if (key == null)
      {
         LOGGER.error("Cannot save securityContext with a null key.");
         return;
      }
      Element element = new Element(key, ctx);
      getCache().put(element);
   }

   public void logout(String key)
   {
      if (key == null)
      {
         return;
      }
      getCache().remove(key);
   }

   public void forceLogout(String userName)
   {
      if (userName == null)
      {
         return;
      }

      List<Object> keys = getCache().getKeysWithExpiryCheck();

      for (Object key: keys)
      {
         if (getCache().isKeyInCache(key))
         {
            Object value = getCache().get(key).getObjectValue();
            if (value instanceof SecurityContext)
            {
               SecurityContext securityContext = (SecurityContext) value;
               Authentication auth = securityContext.getAuthentication();
               if (auth != null && userName.equals(auth.getName()))
               {
                  securityContext.setAuthentication(null);
                  getCache().remove(key);
               }
            }
            else // if not SecurityContext, delete it !
            {
               getCache().remove(key);
            }
         }
      }
   }
}
