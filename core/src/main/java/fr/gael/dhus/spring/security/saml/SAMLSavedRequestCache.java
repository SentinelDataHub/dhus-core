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
package fr.gael.dhus.spring.security.saml;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

@Component
public class SAMLSavedRequestCache
{
   private static final Logger LOGGER = LogManager.getLogger(SAMLSavedRequestCache.class);
   private static final String CACHE_MANAGER_NAME = "dhus_cache";
   private static final String CACHE_NAME = "saml_saved_requests";
   private static Cache cache;

   private static Cache getCache()
   {
      if (cache == null)
      {
         cache = CacheManager.getCacheManager(CACHE_MANAGER_NAME).getCache(CACHE_NAME);
      }
      return cache;
   }

   public SAMLSavedRequestCache() {}

   public SavedRequest load(String key)
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
      return (SavedRequest) getCache().get(key).getObjectValue();
   }

   public void save(String key, SavedRequest request)
   {
      if (key == null)
      {
         LOGGER.error("Cannot save request with a null key.");
         return;
      }
      Element element = new Element(key, request);
      getCache().put(element);
   }

   public void remove(String key)
   {
      if (key == null)
      {
         return;
      }
      getCache().remove(key);
   }
}
