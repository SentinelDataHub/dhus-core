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
import org.dhus.store.StoreException;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.async.AsyncDataSource;
import org.dhus.store.datastore.async.AsyncProduct;

/**
 * Caps the number of fetch requests per user on an AsyncDataSource.
 *
 * @param <AsyncDataStore> Type of async datastore
 */
public final class FetchLimiterAsyncDataSource <AsyncDataStore extends AsyncDataSource & DataStore>
      extends AbstractDataStoreDecorator<AsyncDataStore> implements AsyncDataSource
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String NAME = "ASYNC_FETCH_LIMITER";

   private final StoreQuotaService storeQuotaService;
   private final SecurityService securityService;
   private final int maxFetchs;
   private final long timeInMilliseconds;

   /**
    * Create new instance
    *
    * @param decorated           an asynchronous data store implementation,
    *                            eg: {@link org.dhus.store.datastore.async.gmp.GmpDataStore}
    * @param securityService     used to fetch user's informations
    * @param storeQuotaService   used to persist quota entries
    * @param maxFetchs           Maximum number of fetch requests a user may issue
    * @param timeInMilliseconds  expiration time of quota entries in milliseconds
    */
   public FetchLimiterAsyncDataSource(AsyncDataStore decorated, SecurityService securityService,
         StoreQuotaService storeQuotaService, int maxFetchs, long timeInMilliseconds)
   {
      super(decorated);
      Objects.requireNonNull(securityService);
      Objects.requireNonNull(storeQuotaService);
      this.securityService = securityService;
      this.storeQuotaService = storeQuotaService;
      this.maxFetchs = maxFetchs;
      this.timeInMilliseconds = timeInMilliseconds;
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
   public void fetch(AsyncProduct toFetch) throws DataStoreException
   {
      User user = securityService.getCurrentUser();
      String username = user != null ? user.getUsername() : null;
      UUID uuid = getUsersUUID(user);
      if (uuid != null)
      {
         UpdateUserCallback param1 = new UpdateUserCallback(uuid);
         toFetch.setProperty("username", username);
         FetchCallbacks param234 = new FetchCallbacks(toFetch, uuid, username);
         try
         {
            // Update of quota entries + check + insertion of new quota entry, all done in a transaction
            storeQuotaService.performQuotaCappedOperation(param1, param234, param234, param234);
         }
         catch (StoreException ex)
         {
            if (DataStoreException.class.isAssignableFrom(ex.getClass()))
            {
               throw DataStoreException.class.cast(ex);
            }
            throw new DataStoreException(ex);
         }
      }
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
      public void asyncFetchData() throws DataStoreException
      {
         fetch(embedded);
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
            if (System.currentTimeMillis() - lycos.getDatetime() > timeInMilliseconds)
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
         if (!svc.hasQuotaEntry(decorated.getName(), NAME, userUUID, toFetch.getName()))
         {
            int count = svc.countQuotaEntries(decorated.getName(), NAME, userUUID);

            if (count >= maxFetchs) // Quota reached
            {
               QuotaException ex = new ParallelFetchResquestQuotaException(username, maxFetchs);
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
         decorated.fetch(toFetch);
      }

      /* Updates quotas if `perform()` succeeded */
      @Override
      public void update(StoreQuotaService svc)
      {
         svc.insertQuotaEntry(decorated.getName(), NAME, userUUID, toFetch.getName(), System.currentTimeMillis());
      }

   }

}
