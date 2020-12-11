/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2019 GAEL Systems
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
package org.dhus.store.quota;

import static org.dhus.store.quota.QuotaException.ParallelFetchResquestQuotaException;

import fr.gael.dhus.database.object.Order;
import fr.gael.dhus.database.object.StoreQuota;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.service.SecurityService;
import fr.gael.dhus.service.StoreQuotaService;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.Product;
import org.dhus.ProductConstants;
import org.dhus.api.JobStatus;
import org.dhus.store.StoreException;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.async.AsyncDataStore;
import org.dhus.store.datastore.async.AsyncProduct;

/**
 * Caps the number of fetch requests running in parallel per user on an AsyncDataStore.
 */
public final class FetchLimiterAsyncDataStore extends AbstractDataStoreDecorator<AsyncDataStore> implements AsyncDataStore
{
   private static final Logger LOGGER = LogManager.getLogger();
   public static final String NAME = "ASYNC_FETCH_LIMITER";

   private final StoreQuotaService storeQuotaService;
   private final SecurityService securityService;
   private final int maxFetchsInParallel;

   /**
    * Create new instance
    *
    * @param decorated           an asynchronous data store implementation,
    *                            eg: {@link org.dhus.store.datastore.async.gmp.GmpDataStore}
    * @param securityService     used to fetch user's informations
    * @param storeQuotaService   used to persist quota entries
    * @param maxFetchsInParallel Maximum number of fetch requests a user may issue in parallel
    */
   public FetchLimiterAsyncDataStore(AsyncDataStore decorated, SecurityService securityService,
         StoreQuotaService storeQuotaService, int maxFetchsInParallel)
   {
      super(decorated);
      Objects.requireNonNull(securityService);
      Objects.requireNonNull(storeQuotaService);
      this.securityService = securityService;
      this.storeQuotaService = storeQuotaService;
      this.maxFetchsInParallel = maxFetchsInParallel;
   }

   @Override
   public void close() throws Exception
   {
      this.decorated.close();
   }

   /** Returns the UUID of the given user. */
   private UUID getUsersUUID(User user)
   {
      if (user != null)
      {
         String uuidStr = user.getUUID();
         if (uuidStr != null && !uuidStr.isEmpty())
         {
            return UUID.fromString(uuidStr);
         }
      }
      return null;
   }

   @Override
   public Order fetch(AsyncProduct toFetch) throws DataStoreException
   {
      User user = securityService.getCurrentUser();
      String username = user != null ? user.getUsername() : null;
      UUID userUuid = getUsersUUID(user);
      if (userUuid != null)
      {
         // DHuS-side identifier
         String localIdentifier = toFetch.getName();

         String productUuid = (String) toFetch.getProperty(ProductConstants.UUID);
         Long size = (Long) toFetch.getProperty(ProductConstants.DATA_SIZE);

         // return existing order if exists
         Order oldOrder = decorated.getAndLogExistingOrder(productUuid, localIdentifier, size);
         if (oldOrder != null)
         {
            return oldOrder;
         }

         UpdateUserCallback param1 = new UpdateUserCallback(userUuid);
         toFetch.setProperty("username", username);
         FetchCallbacks param234 = new FetchCallbacks(toFetch, userUuid, username);
         try
         {
            // synchronized block added to avoid an issue with Hibernate's caching (StaleStateException)
            synchronized (userUuid)
            {
               // Update of quota entries + check + insertion of new quota entry, all done in a transaction
               storeQuotaService.performQuotaCappedOperation(param1, param234, param234, param234);
            }
            if (param234.order != null)
            {
               return param234.order;
            }
            // The decorated AsyncDS does not conform to the AsyncDataStore interface
            throw new DataStoreException("Decorated AsyncDS: Fetch operation should not return null");
         }
         catch (DataStoreException ex)
         {
            throw ex;
         }
         catch (StoreException ex)
         {
            throw new DataStoreException(ex);
         }
      }
      throw new DataStoreException("Cannot porform quota capped operation due to current user not set");
   }

   @Override
   public Product get(String uuid) throws DataStoreException
   {
      Product res = this.decorated.get(uuid);
      if (AsyncProduct.class.isAssignableFrom(res.getClass()))
      {
         res = new AsyncProductDecorator(AsyncProduct.class.cast(res));
      }
      return res;
   }

   @Override
   public boolean hasAsyncProduct(String uuid)
   {
      return this.decorated.hasAsyncProduct(uuid);
   }

   @Override
   public List<String> getProductList()
   {
      return decorated.getProductList();
   }

   @Override
   public List<String> getProductList(int skip, int top)
   {
      return decorated.getProductList(skip, top);
   }

   @Override
   public List<Order> getOrderList()
   {
      return decorated.getOrderList();
   }

   @Override
   public Order getOrder(String uuid)
   {
      return decorated.getOrder(uuid);
   }

   /** Quota aware async product. */
   private class AsyncProductDecorator extends AsyncProduct
   {
      private final AsyncProduct embedded;

      private AsyncProductDecorator(AsyncProduct embedded)
      {
         super(null);
         Objects.requireNonNull(embedded);
         this.embedded = embedded;
      }

      @Override
      public Order asyncFetchData() throws DataStoreException
      {
         return fetch(embedded);
      }

      @Override
      public String getName()
      {
         return embedded.getName();
      }

   }

   /* That callback deletes outdated quota entries of given user */
   private class UpdateUserCallback implements StoreQuotaService.Callback.Update
   {
      private final UUID userUUID;

      public UpdateUserCallback(UUID userUUID)
      {
         this.userUUID = userUUID;
      }

      @Override
      public void update(StoreQuotaService svc)
      {
         List<StoreQuota> quotaEntries = svc.getQuotaEntries(decorated.getName(), NAME, userUUID);
         Iterator<StoreQuota> it = quotaEntries.iterator();
         while (it.hasNext())
         {
            StoreQuota lycos = it.next();
            Order order = decorated.getOrder(lycos.getIdentifier());
            if (order == null || (order != null && (order.getStatus() == JobStatus.COMPLETED || order.getStatus() == JobStatus.FAILED)))
            {
               svc.deleteQuotaEntry(lycos);
            }
         }
      }

   }

   /* Those callbacks do a check, a quota capped operation, and then update quota entries of given user */
   private class FetchCallbacks
         implements StoreQuotaService.Callback.Check, StoreQuotaService.Callback.Perform, StoreQuotaService.Callback.Update
   {
      private final AsyncProduct toFetch;
      private final UUID userUUID;
      private final String username;
      private Order order = null; // the order of a successful fetch request

      public FetchCallbacks(AsyncProduct productToFetch, UUID userUUID, String username)
      {
         this.toFetch = productToFetch;
         this.userUUID = userUUID;
         this.username = username;
      }

      /* Returns `true` if can call fetch() */
      @Override
      public boolean check(StoreQuotaService svc) throws QuotaException
      {
         // Check if fetch request already issued
         if (!svc.hasQuotaEntry(decorated.getName(), NAME, userUUID, (String) toFetch.getProperty(ProductConstants.UUID)))
         {
            int count = svc.countQuotaEntries(decorated.getName(), NAME, userUUID);

            if (count >= maxFetchsInParallel) // Quota reached
            {
               QuotaException ex = new ParallelFetchResquestQuotaException(username, maxFetchsInParallel, toFetch);
               LOGGER.info(ex.getMessage());
               throw ex;
            }
            return true;
         }
         return false;
      }

      /* Quota capped operation */
      @Override
      public void perform() throws StoreException
      {
         this.order = decorated.fetch(toFetch);
      }

      /* Updates quotas if `perform()` succeeded */
      @Override
      public void update(StoreQuotaService svc)
      {
         svc.insertQuotaEntry(decorated.getName(), NAME, userUUID, (String) toFetch.getProperty(ProductConstants.UUID), System.currentTimeMillis());
      }
   }

   @Override
   public Order getAndLogExistingOrder(String uuid, String localIdentifier, Long size)
   {
      return decorated.getAndLogExistingOrder(uuid, localIdentifier, size);
   }
}
