/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2016,2017 GAEL Systems
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
package fr.gael.dhus.spring.cache;

import fr.gael.dhus.database.object.Product;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@Aspect
public class CacheAspectDefinition
{
   private static final String PRODUCT_CACHE_NAME = "product";
   private static final String PRODUCTS_CACHE_NAME = "products";
   private static final String PRODUCT_COUNT_CACHE_NAME = "product_count";
   private static final String PRODUCT_COUNT_TOTAL_KEY = "all";
   private static final String INDEXES_CACHE_NAME = "indexes";

   @Autowired
   private CacheManager cacheManager;

   public CacheManager getCacheManager ()
   {
      return cacheManager;
   }

   public void setCacheManager (CacheManager cacheManager)
   {
      this.cacheManager = cacheManager;
   }

   @AfterReturning ("@annotation(fr.gael.dhus.spring.cache.IncrementCache)")
   public void updateCache (JoinPoint joinPoint)
   {
      IncrementCache annotation = ((MethodSignature) joinPoint.getSignature ())
            .getMethod ().getAnnotation (IncrementCache.class);

      Cache cache = getCacheManager().getCache(annotation.name());
      if (cache != null)
      {
         synchronized (cache)
         {
            Integer old_value = cache.get (annotation.key (), Integer.class);
            cache.clear ();
            if (old_value == null)
            {
               return;
            }
            cache.put (annotation.key (), (old_value + annotation.value ()));
         }
      }
   }

   /**
    * Updates caches after a product insertion.
    *
    * @param retObject object returned by the annotated method.
    */
   @AfterReturning(
         pointcut = "@annotation(fr.gael.dhus.spring.cache.AddProduct)",
         returning = "retObject")
   public void addProduct(Object retObject)
   {
      if (retObject != null && retObject instanceof Product)
      {
         Product p = (Product) retObject;

         // add the given product in cache
         Cache cache = getCacheManager().getCache(PRODUCT_CACHE_NAME);
         synchronized (cache)
         {
            cache.evict(p.getId());
            cache.evict(p.getUuid());
            cache.put(p.getId(), p);
            cache.put(p.getUuid(), p);
         }

         incrementProductCount(cacheManager);

         // clear 'products' cache
         getCacheManager().getCache(PRODUCTS_CACHE_NAME).clear();
      }
   }

   /* FIXME This method was made public static to fix cached product count
    * inconsistencies in fr.gael.dhus.datastore.Ingester ingest method
    * 
    * put this code back into addProduct above once the ingestion process
    * has been fully incorporated into the Stores architecture
    */
   @Deprecated
   public static void incrementProductCount(CacheManager cacheManager)
   {
      // increment global 'product_count' and clear others key
      Cache cache = cacheManager.getCache(PRODUCT_COUNT_CACHE_NAME);
      synchronized (cache)
      {
         Integer oldValue = cache.get(PRODUCT_COUNT_TOTAL_KEY, Integer.class);
         if (oldValue != null)
         {
            cache.clear();
            cache.put(PRODUCT_COUNT_TOTAL_KEY, (oldValue + 1));
         }
      }
   }

   /**
    * Updates caches after a product deletion.
    *
    * @param retObject object returned by the annotated method.
    */
   @AfterReturning(
         pointcut = "@annotation(fr.gael.dhus.spring.cache.RemoveProduct)",
         returning = "retObject")
   public void removeProduct(Object retObject)
   {
      if (retObject != null && retObject instanceof Product)
      {
         Product p = (Product) retObject;

         // remove the given product from cache
         Cache cache = getCacheManager().getCache(PRODUCT_CACHE_NAME);
         synchronized (cache)
         {
            cache.evict(p.getId());
            cache.evict(p.getUuid());
         }

         // clear 'indexes' cache of the given product
         cache = getCacheManager().getCache(INDEXES_CACHE_NAME);
         cache.evict(p.getId());

         // decrement global 'product_count' and clear others key
         cache = getCacheManager().getCache(PRODUCT_COUNT_CACHE_NAME);
         synchronized (cache)
         {
            // save old value of all processed products
            Integer oldValue = cache.get(PRODUCT_COUNT_TOTAL_KEY, Integer.class);

            // clear cache
            cache.clear();

            // update value for all processed products if necessary
            if (oldValue != null)
            {
               cache.put(PRODUCT_COUNT_TOTAL_KEY, (oldValue - 1));
            }
         }

         // clear 'products' cache
         getCacheManager().getCache(PRODUCTS_CACHE_NAME).clear();
      }
   }
}
