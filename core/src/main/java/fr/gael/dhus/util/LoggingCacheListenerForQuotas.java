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
package fr.gael.dhus.util;

import java.util.Properties;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Log cache events related to quota informations.
 */
public class LoggingCacheListenerForQuotas extends CacheEventListenerFactory
{
   /** Log. */
   private static final Logger LOGGER = LogManager.getLogger();

   @Override
   public CacheEventListener createCacheEventListener(Properties properties)
   {
      return new QuotaEventListener();
   }

   /** Quota event logger. */
   private static class QuotaEventListener implements CacheEventListener
   {
      @Override
      public Object clone() throws CloneNotSupportedException
      {
         LOGGER.debug("QUOTA CLONE OBTAINED");
         return super.clone();
      }

      @Override
      public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException
      {
         LOGGER.debug("QUOTA REMOVED FROM CACHE: [{}, {}]", element.getObjectKey(), element.getObjectValue());
      }

      @Override
      public void notifyElementPut(Ehcache cache, Element element) throws CacheException
      {
         LOGGER.debug("QUOTA PUT INTO CACHE: [{}, {}]", element.getObjectKey(), element.getObjectValue());
      }

      @Override
      public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException
      {
         LOGGER.debug("QUOTA UPDATED IN CACHE: [{}, {}]", element.getObjectKey(), element.getObjectValue());
      }

      @Override
      public void notifyElementExpired(Ehcache cache, Element element)
      {
         LOGGER.debug("QUOTA EXPIRED IN CACHE: [{}, {}]", element.getObjectKey(), element.getObjectValue());
      }

      @Override
      public void notifyElementEvicted(Ehcache cache, Element element)
      {
         LOGGER.debug("QUOTA EVICTED FROM CACHE: [{}, {}]", element.getObjectKey(), element.getObjectValue());
      }

      @Override
      public void notifyRemoveAll(Ehcache cache)
      {
         LOGGER.debug("REMOVED ALL QUOTA FROM THE CACHE");
      }

      @Override
      public void dispose()
      {
         LOGGER.debug("DISPOSED OF THE QUOTA LISTENER");
      }
   }
}
