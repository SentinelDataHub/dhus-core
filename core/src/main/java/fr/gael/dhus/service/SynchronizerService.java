/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2015-2019 GAEL Systems
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

import fr.gael.dhus.database.object.config.synchronizer.EventSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.SmartProductSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerConfiguration;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerManager;
import fr.gael.dhus.database.object.config.synchronizer.UserSynchronizer;
import fr.gael.dhus.database.object.config.system.ExecutorConfiguration;
import fr.gael.dhus.service.exception.InvokeSynchronizerException;
import fr.gael.dhus.sync.Executor;
import fr.gael.dhus.sync.MetaExecutor;
import fr.gael.dhus.sync.ProductSynchronizerUtils;
import fr.gael.dhus.sync.Synchronizer;
import fr.gael.dhus.sync.SynchronizerStatus;
import fr.gael.dhus.sync.impl.ODataEventSynchronizer;
import fr.gael.dhus.sync.impl.ODataProductSynchronizer;
import fr.gael.dhus.sync.impl.ODataUserSynchronizer;
import fr.gael.dhus.sync.smart.IngestionPageFactory;
import fr.gael.dhus.sync.smart.ODataSmartProductSynchronizer;
import fr.gael.dhus.system.config.ConfigurationManager;
import fr.gael.dhus.util.XmlProvider;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.odata2.api.exception.ODataException;

import org.quartz.CronExpression;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Service;

/**
 * Manages the {@link Executor}, {@link Synchronizer} and
 * {@link SynchronizerConf}.
 */
@Service
public class SynchronizerService implements ISynchronizerService
{
   /** Log. */
   private static final Logger LOGGER = LogManager.getLogger();

   /** Configuration (etc/dhus.xml). */
   @Autowired
   private ConfigurationManager cfgManager;

   @Autowired
   private SecurityService secu;

   @Autowired
   private ISourceService sourceService;

   @Autowired
   private ProductSynchronizerUtils productSyncUtils;

   @Autowired
   private IngestionPageFactory pageFactory;

   /** An instance of {@link Executor}, running the synchronization. */
   private final Executor executor = MetaExecutor.getInstance();

   @Override
   public Status getStatus()
   {
      return executor.isRunning() ? Status.RUNNING : Status.STOPPED;
   }

   @Override
   public void startSynchronization()
   {
      if (!executor.isRunning())
      {
         for (SynchronizerConfiguration sc: cfgManager.getSynchronizerManager().getActiveSynchronizers())
         {
            try
            {
               Synchronizer sync = instanciate(sc);
               executor.addSynchronizer(sync);
            }
            catch (InvokeSynchronizerException ex)
            {
               LOGGER.error("Failed to invoke a Synchronizer", ex);
            }
         }
         executor.start(true); // FIXME: true or false?
      }
   }

   @Override
   public void stopSynchronization()
   {
      executor.stop();
      executor.removeAllSynchronizers();
   }

   @Override
   @SuppressWarnings("unchecked")
   public <T extends SynchronizerConfiguration>
         T getSynchronizerConfById(long id, Class<T> requiredType)
   {
      SynchronizerConfiguration sc = cfgManager.getSynchronizerManager().get(id);
      if (requiredType.isInstance(sc))
      {
         return (T) sc;
      }
      return null;
   }

   @Override
   public Iterator<SynchronizerConfiguration> getSynchronizerConfs()
   {
      return cfgManager.getSynchronizerManager().getSynchronizers().iterator();
   }

   @SuppressWarnings("unchecked")
   @Override
   public <T extends SynchronizerConfiguration>
       Iterator<T> getSynchronizerConfs(Class<T> type)
   {
      List<SynchronizerConfiguration> synchronizers =
            cfgManager.getSynchronizerManager().getSynchronizers();
      List<T> res = new ArrayList<>();
      for (SynchronizerConfiguration sync: synchronizers)
      {
         if (type.isInstance(sync))
         {
            res.add((T) sync);
         }
      }
      return res.iterator();
   }

   @Override
   public int count()
   {
      return cfgManager.getSynchronizerManager().count();
   }

   @Override
   public <T extends SynchronizerConfiguration>
       T createSynchronizer(String label, String cronExpression, Class<T> type)
         throws ParseException, ReflectiveOperationException
   {
      T sc = type.getDeclaredConstructor().newInstance();
      sc.setLabel(label);
      CronExpression.validateExpression(cronExpression);
      sc.setSchedule(cronExpression);
      sc.setCreated(XmlProvider.getCalendarNow());
      sc.setModified(sc.getCreated());
      cfgManager.getSynchronizerManager().create(sc, true);
      LOGGER.info("Synchronizer#{} created by user {}",
            sc.getId(), secu.getCurrentUser().getUsername());
      return sc;
   }

   @Override
   public void removeSynchronizer(long id)
   {
      SynchronizerManager manager = cfgManager.getSynchronizerManager();
      SynchronizerConfiguration sc = manager.get(id);
      if (sc != null)
      {
         try
         {
            if (sc.isActive())
            {
               executor.removeSynchronizer(sc);
            }
         }
         finally
         {
            manager.delete(sc);
            LOGGER.info("Synchronizer#{} deleted by user {}",
                  sc.getId(), secu.getCurrentUser().getUsername());
         }
      }
   }

   @Override
   public void activateSynchronizer(long id) throws InvokeSynchronizerException
   {
      SynchronizerManager manager = cfgManager.getSynchronizerManager();
      SynchronizerConfiguration sc = manager.get(id);
      if (sc != null)
      {
         boolean wasActive = true;
         if (!sc.isActive())
         {
            sc.setActive(true);
            wasActive = false;
         }
         try
         {
            Synchronizer s = instanciate(sc);
            executor.addSynchronizer(s);
         }
         catch (InvokeSynchronizerException ex)
         {
            sc.setActive(false);
            if (wasActive)
            {
               manager.update(sc);
            }
            throw ex;
         }
         if (!wasActive)
         {
            manager.update(sc);
            LOGGER.info("Synchronizer#{} started by user {}",
                  sc.getId(), secu.getCurrentUser().getUsername());
         }
      }
   }

   @Override
   public void deactivateSynchronizer(long id)
   {
      SynchronizerManager manager = cfgManager.getSynchronizerManager();
      SynchronizerConfiguration sc = manager.get(id);
      if (sc != null && sc.isActive())
      {
         try
         {
            // Removes the synchronizer from the Executor
            Synchronizer s = executor.removeSynchronizer(sc);
            if (s != null && s.getSynchronizerConf() != null)
            {
               sc = s.getSynchronizerConf();
            }
            LOGGER.info("Synchronizer#{} stopped by user {}",
                  sc.getId(), secu.getCurrentUser().getUsername());
         }
         finally
         {
            sc.setActive(false);
            manager.update(sc);
         }
      }
   }

   @Override
   public void enableBatchMode(boolean enable)
   {
      executor.enableBatchMode(enable);
   }

   @Override
   public boolean isBatchModeEnabled()
   {
      return executor.isBatchModeEnabled();
   }

   @Override
   public void saveSynchronizerConf(SynchronizerConfiguration sc)
         throws InvokeSynchronizerException
   {
      long id = sc.getId();
      sc.setModified(XmlProvider.getCalendarNow());
      cfgManager.getSynchronizerManager().update(sc);
      if (sc.isActive())
      {
         activateSynchronizer(id);
      }
      LOGGER.info("Synchronizer#{} modified by user {}",
            sc.getId(), secu.getCurrentUser().getUsername());
   }

   @Override
   public void saveSynchronizer(Synchronizer s)
   {
      cfgManager.getSynchronizerManager().update(s.getSynchronizerConf());
   }

   @Override
   public SynchronizerStatus getStatus(SynchronizerConfiguration sc)
   {
      if (!this.executor.isRunning())
      {
         return new SynchronizerStatus(SynchronizerStatus.Status.STOPPED,
               new Date(0L), "Executor is not running");
      }

      if (!sc.isActive())
      {
         return SynchronizerStatus.makeStoppedStatus(
               sc.getModified().toGregorianCalendar().getTime());
      }

      SynchronizerStatus ss = this.executor.getSynchronizerStatus(sc);
      if (ss == null)
      {
         return SynchronizerStatus.makeUnknownStatus();
      }

      return ss;
   }

   /**
    * Creates a new Synchronizer instance of the class returned from {@link SynchronizerConf#getType()}.
    *
    * @throws InvokeSynchronizerException
    */
   private Synchronizer instanciate(SynchronizerConfiguration sc) throws InvokeSynchronizerException
   {
      try
      {
         if (sc instanceof UserSynchronizer)
         {
            return new ODataUserSynchronizer((UserSynchronizer) sc);
         }
         else if (sc instanceof ProductSynchronizer)
         {
            return new ODataProductSynchronizer((ProductSynchronizer) sc);
         }
         else if (sc instanceof EventSynchronizer)
         {
            return new ODataEventSynchronizer((EventSynchronizer) sc);
         }
         else
         {
            return new ODataSmartProductSynchronizer(
                  sourceService, productSyncUtils, pageFactory, (SmartProductSynchronizer) sc);
         }
      }
      catch (com.vividsolutions.jts.io.ParseException | IOException | ODataException | RuntimeException e)
      {
         throw new InvokeSynchronizerException("Cannot invoke Synchronizer: " + e.getMessage(), e);
      }
   }

   /**
    * Spring ContextClosedEvent listener to terminate the {@link Executor}.
    * <b>YOU MUST NOT CALL THIS METHOD!</b>
    *
    * @param event
    */
   @Override
   public void onApplicationEvent(ContextClosedEvent event)
   {
      LOGGER.debug("Synchronizer: event {} received", event);
      if (event == null)
      {
         return;
      }
      // Terminates the Executor
      LOGGER.info("Synchronization: Executor is terminating");
      executor.removeAllSynchronizers();
      executor.terminate();
   }

   @Override
   public void init()
   {
      // Starts the Executor if not started yet
      if (!this.executor.isRunning())
      {
         ExecutorConfiguration cfg = this.cfgManager.getExecutorConfiguration();
         if (cfg.isEnabled())
         {
            this.executor.enableBatchMode(cfg.isBatchModeEnabled());
            startSynchronization(); // Adds every active synchronizer and
            // starts
            LOGGER.info("Synchronization: Starting the Executor (batchmode: {})",
                  cfg.isBatchModeEnabled() ? "on" : "off");
         }
      }
   }

}
