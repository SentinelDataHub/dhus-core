/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013-2018 GAEL Systems
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

import fr.gael.dhus.database.object.DeletedProduct;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.config.eviction.Eviction;
import fr.gael.dhus.database.object.config.eviction.EvictionManager;
import fr.gael.dhus.database.object.config.eviction.EvictionStatusEnum;
import fr.gael.dhus.datastore.Destination;
import fr.gael.dhus.service.eviction.EvictionScheduler;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.store.StoreException;
import org.dhus.store.StoreService;

import org.quartz.SchedulerException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvictionService extends WebService
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

   @Autowired
   private ConfigurationManager cfgManager;

   @Autowired
   /** Service called to perform evictions * */
   private StoreService storeService;

   /** Manages eviction objects and their configuration * */
   private EvictionManager evictionManager;

   /** Single thread executor used to queue eviction tasks * */
   private Executor executor;

   private EvictionScheduler scheduler;

   @PostConstruct
   private void init() throws SchedulerException
   {
      evictionManager = cfgManager.getEvictionManager();
      executor = Executors.newSingleThreadExecutor();
      scheduler = new EvictionScheduler();

      for (Eviction eviction: getEvictions())
      {
         if (eviction.getCron() != null && eviction.getCron().isActive())
         {
            try
            {
               scheduler.scheduleEviction(eviction);
            }
            catch (SchedulerException ex)
            {
               LOGGER.warn("Could not schedule eviction '{}'", eviction.getName());
            }
         }
      }
      scheduler.start();
   }

   public Eviction getEviction(String evictionName)
   {
      return evictionManager.getEvictionByName(evictionName);
   }

   /**
    * Run the eviction identified by the name passed in argument.
    *
    * @param evictionName the name of the eviction to run
    */
   private void queueEvict(String evictionName)
   {
      Eviction configuredEviction = getEviction(evictionName);
      // check if eviction still exists
      if (configuredEviction == null)
      {
         LOGGER.warn("Eviction '{}' not found", evictionName);
         return;
      }
      // do not queue the same eviction twice
      else if (configuredEviction.getStatus().equals(EvictionStatusEnum.QUEUED))
      {
         LOGGER.info("Eviction '{}' is already queued", evictionName);
         return;
      }

      // TODO forbid queuing already STARTED evictions?
      // mark eviction as queued
      configuredEviction.setStatus(EvictionStatusEnum.QUEUED);
      evictionManager.save();
   }

   public void doEvict(String evictionName, String targetDataStore, Boolean safeMode)
   {
      queueEvict(evictionName);
      // queue eviction in the executor
      executor.execute(() -> performEviction(evictionName, targetDataStore, Long.MAX_VALUE, safeMode));
   }

   public void doEvict(String evictionName)
   {
      queueEvict(evictionName);
      // queue eviction in the executor
      executor.execute(() -> performEviction(evictionName, null, Long.MAX_VALUE, false));
   }

   /**
    * Run customizable automatic eviction.
    *
    * @param evictionName  name of the eviction that is in the configuration of the DataStore
    * @param dataStoreName name of the DataStore
    * @param dataSize      size to evict (in bytes) from the DataStore
    */
   public void evictAtLeast(String evictionName, String dataStoreName, long dataSize)
   {
      queueEvict(evictionName);
      // queue eviction in the executor
      executor.execute(() -> performEviction(evictionName, dataStoreName, dataSize, false));
   }

   /**
    * Run automatic Eviction.
    *
    * @param dataStoreName name of the DataStore
    * @param sizeToEvict   size to evict in the DataStore
    */
   public void evictAtLeast(String dataStoreName, long sizeToEvict)
   {
      executor.execute(() -> {
         try
         {
            storeService.evictAtLeast(sizeToEvict, dataStoreName);
         }
         catch (StoreException e)
         {
            LOGGER.error("Error during eviction in DataStore {}", dataStoreName, e);
         }
      });
   }

   private long performEviction(String evictionName, String dataStoreName, long dataSize, boolean safeMode)
   {
      // this method MUST read the eviction configuration in order to handle
      // cases where the eviction has been deleted
      Eviction effectiveEviction = getEviction(evictionName);

      // skip eviction if it has been deleted
      if (effectiveEviction == null)
      {
         LOGGER.warn("Eviction '{}' has been deleted, skipping", evictionName);
         return 0L;
      }
      long evictAtLeast = 0L;
      try
      {
         if (effectiveEviction.getStatus().equals(EvictionStatusEnum.CANCELED))
         {
            // skip eviction, nothing to do
            LOGGER.info("Eviction '{}' has been cancelled, skipping", effectiveEviction.getName());
         }
         else
         {
            // If a trashPath is present in the conf (dhus.xml), evicted products will be saved in the
            // trash folder before being removed
            String trashPath = cfgManager.getTrashPath();
            final Destination destination =
                  (trashPath != null && !"".equals(trashPath)) ? Destination.TRASH : Destination.NONE;

            // mark eviction as STARTED
            effectiveEviction.setStatus(EvictionStatusEnum.STARTED);
            evictionManager.save();

            // eviction triggered by datastore's autoeviction
            if (dataStoreName != null) // && dataSize > 0 ?
            {
               evictAtLeast = storeService.evictAtLeast(
                     dataSize,
                     dataStoreName,
                     buildFilter(effectiveEviction.getFilter(), getKeepPeriod(effectiveEviction.computeKeepPeriod())),
                     effectiveEviction.getOrderBy(),
                     effectiveEviction.getTargetCollection(),
                     effectiveEviction.getMaxEvictedProducts(),
                     effectiveEviction.isSoftEviction(),
                     destination,
                     DeletedProduct.AUTO_EVICTION,
                     safeMode);
            }
            // regular eviction started by scheduler or odata action
            else
            {
               storeService.evictProducts(
                     buildFilter(effectiveEviction.getFilter(), getKeepPeriod(effectiveEviction.computeKeepPeriod())),
                     effectiveEviction.getOrderBy(),
                     effectiveEviction.getTargetCollection(),
                     effectiveEviction.getMaxEvictedProducts(),
                     effectiveEviction.isSoftEviction(),
                     destination,
                     DeletedProduct.AUTO_EVICTION);
            }
         }
      }
      catch (StoreException e)
      {
         LOGGER.warn("Error during eviction '{}': {}", effectiveEviction.getName(), e.getMessage());
      }
      finally
      {
         // mark eviction as STOPPED
         effectiveEviction.setStatus(EvictionStatusEnum.STOPPED);
         evictionManager.save();
      }
      return evictAtLeast;
   }

   /**
    * Add the minimal keeping period for the evicted products to the OData filter.
    *
    * @param filter          optional additional filter
    * @param maxCreationDate threshold date to filter products
    * @return an OData filter
    */
   private String buildFilter(String filter, Date maxCreationDate)
   {
      if (filter == null && maxCreationDate == null)
      {
         return null;
      }
      if (maxCreationDate == null)
      {
         return filter;
      }

      String maxDate = DATE_FORMATTER.format(maxCreationDate);
      String newFilter = "CreationDate lt datetime'" + maxDate + "'";
      if (filter != null && !filter.trim().isEmpty())
      {
         newFilter += " and " + filter;
      }
      return newFilter;
   }

   /**
    * Computes the date <i>span</i> ms ago.
    *
    * @param ms span in ms
    * @return a date representation of date <i>days</i> ago.
    */
   public Date getKeepPeriod(long ms)
   {
      Date date = new Date();
      date = new Date(date.getTime() - ms);

      LOGGER.info("Eviction KeepPeriod: {}", date);
      return date;
   }

   @PreAuthorize("isAuthenticated()")
   @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
   public Date getEvictionDate(Product product)
   {
      Date res = null;
      for (Eviction eviction: getEvictions())
      {
         // The cron must exist and be active, and the given product must validate the filter
         if (eviction.getCron() != null && eviction.getCron().isActive() && eviction.filter(product))
         {
            // Compute eviction date, sets the result reference if it is sooner
            Date evictDate = new Date(product.getCreated().getTime() + eviction.computeKeepPeriod());
            if (res == null || res.after(evictDate))
            {
               res = evictDate;
            }
         }
      }
      return res;
   }

   /**
    * Update an eviction an run it if the previous status was STOPPED and the new one is STARTED.
    *
    * @param eviction the eviction to update
    * @param previousStatus the previous status of the eviction to update
    */
   public void updateEviction(Eviction eviction, EvictionStatusEnum previousStatus)
   {
      evictionManager.save();

      try
      {
         if (eviction.getCron() != null && eviction.getCron().isActive())
         {
            scheduler.scheduleEviction(eviction);
         }
         else
         {
            scheduler.unscheduleEviction(eviction);
         }
      }
      catch (SchedulerException ex)
      {
         LOGGER.warn("Could not unschedule eviction '{}'", eviction.getName());
      }
   }

   /**
    * Return all the evictions in the configuration file.
    *
    * @return a list of evictions
    */
   public List<Eviction> getEvictions()
   {
      return evictionManager.getEvictions();
   }

   /**
    * Create and save an eviction in the configuration file.
    *
    * @param eviction the eviction to create
    */
   public void create(Eviction eviction)
   {
      evictionManager.create(eviction);

      if (eviction.getCron() != null && eviction.getCron().isActive())
      {
         try
         {
            scheduler.scheduleEviction(eviction);
         }
         catch (SchedulerException ex)
         {
            LOGGER.warn("Could not schedule eviction '{}'", eviction.getName());
         }
      }
   }

   /**
    * Delete an eviction in the configuration file.
    *
    * @param eviction the eviction to delete
    */
   public void delete(Eviction eviction)
   {
      try
      {
         scheduler.unscheduleEviction(eviction);
      }
      catch (SchedulerException ex)
      {
         LOGGER.warn("Could not unschedule eviction '{}'", eviction.getName());
      }
      evictionManager.delete(eviction);
   }

   /**
    * Delete an eviction in the configuration file.
    *
    * @param evictionName the name of the eviction to delete
    */
   public void delete(String evictionName)
   {
      evictionManager.delete(getEviction(evictionName));
   }

   public boolean cancelEviction(String evictionName)
   {
      Eviction eviction = getEviction(evictionName);
      if (eviction == null)
      {
         LOGGER.warn("Eviction '{}' not found", evictionName);
         return false;
      }

      if (eviction.getStatus().equals(EvictionStatusEnum.QUEUED))
      {
         eviction.setStatus(EvictionStatusEnum.CANCELED);
         evictionManager.save();
         return true;
      }
      return false;
   }

   public void stopCurrentEviction()
   {
      storeService.stopCurrentEviction();
   }
}
