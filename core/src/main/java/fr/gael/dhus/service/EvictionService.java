/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013-2019 GAEL Systems
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
import org.dhus.store.datastore.DataStoreManager;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import fr.gael.dhus.database.object.DeletedProduct;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.config.eviction.Eviction;
import fr.gael.dhus.database.object.config.eviction.EvictionBaseDate;
import fr.gael.dhus.database.object.config.eviction.EvictionManager;
import fr.gael.dhus.database.object.config.eviction.EvictionStatusEnum;
import fr.gael.dhus.datastore.Destination;
import fr.gael.dhus.service.eviction.EvictionScheduler;
import fr.gael.dhus.system.config.ConfigurationManager;

@Service
public class EvictionService extends WebService
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

   @Autowired
   private ConfigurationManager cfgManager;

   /** DataStore Service called to delete data. */
   @Autowired
   private StoreService storeService;
   
   @Autowired
   private DataStoreManager dataStoreManager;

   /* Monitoring. */
   @Autowired
   private MetricRegistry metricRegistry;

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
   private boolean queueEvict(String evictionName)
   {
      Eviction configuredEviction = getEviction(evictionName);
      // check if eviction still exists
      if (configuredEviction == null)
      {
         LOGGER.warn("Eviction '{}' not found", evictionName);
         return false;
      }
      // do not queue the same eviction twice
      else if (configuredEviction.getStatus().equals(EvictionStatusEnum.QUEUED))
      {
         LOGGER.info("Eviction '{}' is already queued", evictionName);
         return false;
      }

      // TODO forbid queuing already STARTED evictions?
      // mark eviction as queued
      configuredEviction.setStatus(EvictionStatusEnum.QUEUED);
      evictionManager.save();
      LOGGER.info("Eviction '{}' has been queued", evictionName);
      return true;
   }

   /**
    * Called by OData v2 action
    * Using defined targetDataStore and safeMode, ignoring default defined in specified Eviction.
    */
   public void doEvict(String evictionName, String targetDataStore, Boolean safeMode)
   {
      if (queueEvict(evictionName))
      {
         // queue eviction in the executor
         executor.execute(() -> performTimedEviction(evictionName, targetDataStore, Long.MAX_VALUE, safeMode));
      }
   }

   /**
    * Called by Eviction Job
    * Using default targetDataStore and safeMode defined in specified Eviction.
    */
   public void doEvict(String evictionName)
   {
      if (queueEvict(evictionName))
      {
         Eviction eviction = getEviction(evictionName);
         // queue eviction in the executor
         executor.execute(() -> performTimedEviction(evictionName, eviction.getTargetDataStore(), Long.MAX_VALUE, eviction.isSafeMode()));
      }
   }

   /**
    * Run customizable automatic eviction.
    * Using defined dataStoreName as targetDatastore and forcing safeMode to false.
    * Ignoring default values defined in specified Eviction.
    * 
    * @param evictionName  name of the eviction that is in the configuration of the DataStore
    * @param dataStoreName name of the DataStore
    * @param dataSize      size to evict (in bytes) from the DataStore
    */
   public void evictAtLeast(String evictionName, String dataStoreName, long dataSize)
   {
      if (queueEvict(evictionName))
      {
         // queue eviction in the executor
         executor.execute(() -> performTimedEviction(evictionName, dataStoreName, dataSize, false));
      }
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
         String metric = MetricRegistry.name("eviction", "default", dataStoreName, "timer");
         try (Timer.Context ctx = metricRegistry.timer(metric).time())
         {
            storeService.evictAtLeast(sizeToEvict, dataStoreName);
         }
         catch (StoreException e)
         {
            LOGGER.error("Error during eviction in DataStore {}", dataStoreName, e);
         }
      });
   }

   private long performTimedEviction(String evictionName, String dataStoreName, long dataSize, boolean safeMode)
   {
      Eviction eviction = getEviction(evictionName);
      // skip eviction if it has been deleted
      if (eviction == null)
      {
         LOGGER.warn("Eviction '{}' has been deleted, skipping", eviction);
         return 0L;
      }
      if (dataStoreName != null && dataStoreManager.getDataStoreByName(dataStoreName) == null)
      {
         LOGGER.error("DataStore '{}' is unknown. Cannot perform an eviction on it.", dataStoreName);

         // mark eviction as STOPPED
         eviction.setStatus(EvictionStatusEnum.STOPPED);
         evictionManager.save();
         return 0L;
      }

      String metric = MetricRegistry.name("eviction",
            evictionName,
            dataStoreName,
            eviction.isSoftEviction()? "soft": null,
            safeMode? "safe": null,
            "timer");
      try (Timer.Context ctx = metricRegistry.timer(metric).time())
      {
         return performEviction(eviction, dataStoreName, dataSize, safeMode);
      }
   }

   private long performEviction(Eviction eviction, String dataStoreName, long dataSize, boolean safeMode)
   {
      long evictAtLeast = 0L;
      try
      {
         if (eviction.getStatus().equals(EvictionStatusEnum.CANCELED))
         {
            // skip eviction, nothing to do
            LOGGER.info("Eviction '{}' has been cancelled, skipping", eviction.getName());
         }
         else
         {
            // If a trashPath is present in the conf (dhus.xml), evicted products will be saved in the
            // trash folder before being removed
            String trashPath = cfgManager.getTrashPath();
            final Destination destination = (trashPath != null && !"".equals(trashPath)) ? Destination.TRASH : Destination.NONE;

            // mark eviction as STARTED
            eviction.setStatus(EvictionStatusEnum.STARTED);
            evictionManager.save();

            String filter = eviction.getBaseDate() == EvictionBaseDate.CREATION_DATE ? 
                buildFilterBasedOnCreationDate(eviction.getFilter(), getKeepPeriod(eviction.computeKeepPeriod())) :
                buildFilter(eviction.getFilter(), getKeepPeriod(eviction.computeKeepPeriod()));
            
            // eviction targetting a specific datastore
            if (dataStoreName != null) // && dataSize > 0 ?
            {
               evictAtLeast = storeService.evictAtLeast(
                     dataSize,
                     dataStoreName,
                     filter,
                     eviction.getOrderBy(),
                     eviction.getTargetCollection(),
                     eviction.getMaxEvictedProducts(),
                     eviction.isSoftEviction(),
                     destination,
                     DeletedProduct.AUTO_EVICTION,
                     safeMode);
            }
            // regular eviction started by scheduler or odata action
            else
            {
               storeService.evictProducts(
                     filter,
                     eviction.getOrderBy(),
                     eviction.getTargetCollection(),
                     eviction.getMaxEvictedProducts(),
                     eviction.isSoftEviction(),
                     destination,
                     DeletedProduct.AUTO_EVICTION,
                     safeMode);
            }
         }
      }
      catch (StoreException e)
      {
         LOGGER.warn("Error during eviction '{}'", eviction.getName(), e);
      }
      finally
      {
         // mark eviction as STOPPED
         if (eviction.getStatus().equals(EvictionStatusEnum.STARTED))
         {
            eviction.setStatus(EvictionStatusEnum.STOPPED);
         }
         evictionManager.save();
      }
      return evictAtLeast;
   }

   /**
    * Add the minimal keeping period for the evicted products to the OData filter.
    *
    * @param filter          optional additional filter
    * @param maxModificationDate threshold date to filter products
    * @return an OData filter
    */
   private String buildFilter(String filter, Date maxModificationDate)
   {
      if (filter == null && maxModificationDate == null)
      {
         return null;
      }
      if (maxModificationDate == null)
      {
         return filter;
      }

      String maxDate = DATE_FORMATTER.format(maxModificationDate);
      String newFilter = "ModificationDate lt datetime'" + maxDate + "'";
      if (filter != null && !filter.trim().isEmpty())
      {
         newFilter += " and (" + filter + ")";
      }
      return newFilter;
   }
   
   /**
    * Add the minimal keeping period for the evicted products to the OData filter.
    *
    * @param filter          optional additional filter
    * @param maxCreationDate threshold date to filter products
    * @return an OData filter
    */
   private String buildFilterBasedOnCreationDate(String filter, Date maxCreationDate)
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
            Date evictDate = new Date(product.getUpdated().getTime() + eviction.computeKeepPeriod());
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
