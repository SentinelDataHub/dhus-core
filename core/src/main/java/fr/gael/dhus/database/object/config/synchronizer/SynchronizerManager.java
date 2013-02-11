/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2019 GAEL Systems
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
package fr.gael.dhus.database.object.config.synchronizer;

import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationException;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages synchronisers defined in configuration file (dhus.xml).
 */
public class SynchronizerManager extends Synchronizers
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static long nextId = -1;

   public List<SynchronizerConfiguration> getSynchronizers()
   {
      return Collections.<SynchronizerConfiguration>unmodifiableList(getSynchronizer());
   }

   private long getNextId()
   {
      if (nextId < 0)
      {
         for (SynchronizerConfiguration sync: getSynchronizer())
         {
            if (sync.getId() > nextId)
            {
               nextId = sync.getId();
            }
         }
      }
      nextId += 1;
      return nextId;
   }

   public SynchronizerConfiguration create(SynchronizerConfiguration sc, boolean save)
   {
      synchronized(this)
      {
         sc.setId(getNextId());
         getSynchronizer().add(sc);
         if (save)
         {
            save();
         }
         LOGGER.info("Synchronizer#{} created", sc.getId());
         return sc;
      }
   }

   /**
    * Updates a existing synchroniser in configuration.
    *
    * @param update synchroniser to update
    */
   public void update(final SynchronizerConfiguration update)
   {
      synchronized(this)
      {
         if (update == null)
         {
            return;
         }

         for (int i = 0; i < getSynchronizer().size(); i++)
         {
            SynchronizerConfiguration sync = getSynchronizer().get(i);
            if (sync != null && sync.getId() == update.getId())
            {
               getSynchronizer().set(i, update);
               break;
            }
         }
         save();
      }
   }

   public int count()
   {
      return getSynchronizer().size();
   }

   public List<SynchronizerConfiguration> getActiveSynchronizers()
   {
      List<SynchronizerConfiguration> result = new ArrayList<>();
      for (SynchronizerConfiguration sync: getSynchronizer())
      {
         if (sync != null && sync.isActive())
         {
            result.add(sync);
         }
      }
      return result;
   }

   public SynchronizerConfiguration get(Long id)
   {
      for (SynchronizerConfiguration sync: getSynchronizer())
      {
         if (sync != null && sync.getId() == id)
         {
            return (SynchronizerConfiguration) sync;
         }
      }
      return null;
   }
   
   /**
    * Get a synchronize by its label
    * @param label
    * @return
    */
   public SynchronizerConfiguration get(String label)
   {
      for (SynchronizerConfiguration sync: getSynchronizer())
      {
         if (sync != null && sync.getLabel().equalsIgnoreCase(label))
         {
            return (SynchronizerConfiguration) sync;
         }
      }
      return null;
   }

   public void delete(SynchronizerConfiguration syncConf)
   {
      synchronized(this)
      {
         if (syncConf != null)
         {
            getSynchronizer().remove(syncConf);
            save();
         }
      }
   }

   private void save()
   {
      ConfigurationManager cfg = ApplicationContextProvider.getBean(ConfigurationManager.class);
      try
      {
         cfg.saveConfiguration();
      }
      catch (ConfigurationException e)
      {
         LOGGER.error("There was an error while saving configuration", e);
      }
   }
}
