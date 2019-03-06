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
package fr.gael.dhus.service;

import fr.gael.dhus.database.dao.StoreQuotaDao;
import fr.gael.dhus.database.object.StoreQuota;

import java.util.List;
import java.util.UUID;

import org.dhus.store.StoreException;
import org.dhus.store.quota.QuotaException;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quotas on store service.
 */
@Service
public class StoreQuotaService
{
   @Autowired
   private StoreQuotaDao storeQuotaDao;

   /**
    * @param storeName of quota entry to insert
    * @param quotaName of quota entry to insert
    * @param user of quota entry to insert
    * @param identifier of quota entry to insert
    * @param datetime of quota entry to insert
    */
   public void insertQuotaEntry(String storeName, String quotaName, UUID user, String identifier, long datetime)
   {
      StoreQuota entry = new StoreQuota();
      entry.setStoreName(storeName);
      entry.setQuotaName(quotaName);
      entry.setIdentifier(identifier);
      entry.setUserUUID(user.toString());
      entry.setDatetime(datetime);
      storeQuotaDao.create(entry);
   }

   /**
    * @param storeName of quota entries to return
    * @param quotaName of quota entries to return
    * @param user of quota entries to return
    * @return a List of quota entries
    */
   @Transactional(readOnly = true)
   public List<StoreQuota> getQuotaEntries(String storeName, String quotaName, UUID user)
   {
      DetachedCriteria query = DetachedCriteria.forClass(StoreQuota.class);
      query.add(Restrictions.eq("key.storeName", storeName))
           .add(Restrictions.eq("key.quotaName", quotaName))
           .add(Restrictions.eq("key.userUUID", user.toString()));
      return storeQuotaDao.listCriteria(query, 0, 0);
   }

   /**
    * @param storeName of quota entries to count
    * @param quotaName of quota entries to count
    * @param user of quota entries to count
    * @return count of quota entries
    */
   @Transactional(readOnly = true)
   public int countQuotaEntries(String storeName, String quotaName, UUID user)
   {
      DetachedCriteria query = DetachedCriteria.forClass(StoreQuota.class);
      query.add(Restrictions.eq("key.storeName", storeName))
           .add(Restrictions.eq("key.quotaName", quotaName))
           .add(Restrictions.eq("key.userUUID", user.toString()));
      return storeQuotaDao.count(query);
   }

   /**
    * @param storeName of quota entry to find
    * @param quotaName of quota entry to find
    * @param user of quota entry to find
    * @param identifier of quota entry to find
    * @return true if quota entry exists in database
    */
   public boolean hasQuotaEntry(String storeName, String quotaName, UUID user, String identifier)
   {
      DetachedCriteria query = DetachedCriteria.forClass(StoreQuota.class);
      query.add(Restrictions.eq("key.storeName", storeName))
           .add(Restrictions.eq("key.quotaName", quotaName))
           .add(Restrictions.eq("key.userUUID", user.toString()))
           .add(Restrictions.eq("key.identifier", identifier));
      return !(storeQuotaDao.listCriteria(query, 0, 1).isEmpty());
   }

   /**
    * @param quotaEntry to remove
    */
   @Transactional
   public void deleteQuotaEntry(StoreQuota quotaEntry)
   {
      storeQuotaDao.delete(quotaEntry);
   }

   /**
    * Uses callbacks, so everything happens in a transaction.
    *
    * @param preUpdate callback that is run first
    * @param preCheck condition to execute {@code operation.perform()} and {@code postUpdate.update()}
    * @param operation perform a quota capped operation
    * @param postUpdate update run after {@code operation.perform()} if it did not throw an exception
    * @throws StoreException thrown by {@code operation.perform()} and {@code preCheck.check()},
    *                        rollbacks the transaction
    */
   @Transactional
   public void performQuotaCappedOperation(Callback.Update preUpdate, Callback.Check preCheck,
         Callback.Perform operation, Callback.Update postUpdate)
         throws StoreException
   {
      if (preUpdate != null)
      {
         preUpdate.update(this);
      }

      if (preCheck.check(this))
      {
         operation.perform();

         if (postUpdate != null)
         {
            postUpdate.update(this);
         }
      }
   }

   /**
    * @see #performQuotaCappedOperation(Callback.Update, Callback.Check, Callback.perform, Callback.Update)
    */
   public interface Callback
   {
      /**
       * Updates quota informations.
       */
      interface Update
      {
         void update(StoreQuotaService svc);
      }

      /**
       * Checks if the quota capped operation shall be run.
       */
      interface Check
      {
         boolean check(StoreQuotaService svc) throws QuotaException;
      }

      /**
       * The quota capped operation.
       */
      interface Perform
      {
         void perform() throws StoreException;
      }
   }
}
