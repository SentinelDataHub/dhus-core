/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2018 GAEL Systems
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
package org.dhus.store.datastore.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationException;
import fr.gael.dhus.system.config.ConfigurationManager;

/**
 * Manages DataStores defined in configuration file (dhus.xml).
 */
public class DataStoreManager extends DataStores
{
   private static final Logger LOGGER = LogManager.getLogger();

   /**
    * Returns a data store configuration having the given name.
    * This method is able to return the only data store configuration having no name: the OldIncomingDataStore.
    *
    * @param name  of data store configuration to return
    * @return data store configuration or null if not found
    */
   public DataStoreConf get(String name)
   {
      for (DataStoreConf ds: getDataStore())
      {
         if (ds != null && ds instanceof NamedDataStoreConf
               && ((NamedDataStoreConf) ds).getName().equals(name))
         {
            return ds;
         }
      }
      return null;
   }

   /**
    * Returns a data store configuration having the given name.
    *
    * @param name of data store configuration to return
    * @return data store configuration or null if not found
    */
   public NamedDataStoreConf getNamed(String name)
   {
      for (DataStoreConf dataStoreConf: getDataStore())
      {
         if (dataStoreConf != null
               && dataStoreConf instanceof NamedDataStoreConf
               && ((NamedDataStoreConf) dataStoreConf).getName().equals(name))
         {
            return (NamedDataStoreConf) dataStoreConf;
         }
      }
      return null;
   }

   /**
    * Inserts given data store configuration entry in the main configuration, then saves that configuration
    * to disk in the dhus.xml file.
    *
    * @param namedDsc data store configuration to add
    * @throws UnavailableNameException a data store with that name already exists
    */
   public void createNamed(NamedDataStoreConf namedDsc) throws UnavailableNameException
   {
      if (getNamed(namedDsc.getName()) == null)
      {
         getDataStore().add(namedDsc);
         save();
      }
      else
      {
         throw new UnavailableNameException("The DataStore name '" + namedDsc.getName() + "' is unavailable");
      }
   }

   /**
    * Create a DataStoreConf entry in the configuration and optionally saves the configuration.
    *
    * @param dc to create
    * @param save {@code true} to save (writes the configuration in the dhus.xml file)
    * @return dc for API compatibility
    * @deprecated use {@link #createNamed(org.dhus.store.datastore.config.NamedDataStoreConf)} instead.
    */
   @Deprecated
   public DataStoreConf create(DataStoreConf dc, boolean save)
   {
      getDataStore().add(dc);
      if (save)
      {
         save();
      }
      return dc;
   }

   /**
    * Remove given data store from configuration.
    *
    * @param ds to remove
    */
   public synchronized void delete(DataStoreConf ds)
   {
      if (ds != null)
      {
         getDataStore().remove(ds);
         save();
      }
   }

   /**
    * Updates an existing DataStore in configuration.
    *
    * @param update scanner to update.
    */
   public synchronized void update(final NamedDataStoreConf update)
   {
      Objects.requireNonNull(update);

      NamedDataStoreConf namedDsc = getNamed(update.getName());
      if(namedDsc != null)
      {
         getDataStore().remove(namedDsc);
         create(update, true);
      }
   }

   /**
    * Merges and returns the result of both {@link #getUnnamedDataStoreConfigurations()} and
    * {@link #getNamedDataStoreConfigurations()}.
    * <p>Ignores duplicate DataStores based on their name.
    *
    * @return a list of all data store configurations
    */
   public List<DataStoreConf> getAllDataStoreConfigurations()
   {
      List<DataStoreConf> allDataStoreConfs = new ArrayList<>();
      allDataStoreConfs.addAll(getUnnamedDataStoreConfigurations());
      allDataStoreConfs.addAll(getNamedDataStoreConfigurations());
      return allDataStoreConfs;
   }

   /**
    * Ignores duplicate DataStores based on their name.
    *
    * @return a list of named data store configurations
    */
   public List<NamedDataStoreConf> getNamedDataStoreConfigurations()
   {
      Set<String> dataStoreNames = new HashSet<>();
      List<NamedDataStoreConf> namedDataStoreConfs = new ArrayList<>();
      for (DataStoreConf dataStoreConf: getDataStore())
      {
         // only return named datastores
         if (dataStoreConf instanceof NamedDataStoreConf)
         {
            // ignore duplicate datastores based on their name
            NamedDataStoreConf namedDsc = (NamedDataStoreConf) dataStoreConf;
            if (!dataStoreNames.contains(namedDsc.getName()))
            {
               namedDataStoreConfs.add(namedDsc);
               dataStoreNames.add(namedDsc.getName());
            }
            else
            {
               LOGGER.warn("Found duplicate DataStoreConfiguration of name: {}", namedDsc.getName());
            }
         }
      }
      return namedDataStoreConfs;
   }

   /**
    * @return a list of all unnamed data stores
    */
   public List<DataStoreConf> getUnnamedDataStoreConfigurations()
   {
      List<DataStoreConf> unnamedDataStoreConfs = new ArrayList<>();
      for (DataStoreConf dataStoreConf: getDataStore())
      {
         if(!(dataStoreConf instanceof NamedDataStoreConf))
         {
            unnamedDataStoreConfs.add(dataStoreConf);
         }
      }
      return unnamedDataStoreConfs;
   }

   public synchronized long varyCurrentSize(String dataStoreName, long amount)
   {
      NamedDataStoreConf dataStoreConf = getNamed(dataStoreName);
      if (dataStoreConf != null)
      {
         dataStoreConf.setCurrentSize(dataStoreConf.getCurrentSize() + amount);
         save();
         return dataStoreConf.getCurrentSize();
      }
      else
      {
         LOGGER.warn("DataStore not found: {}", dataStoreName);
         return 0;
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

   /**
    * Thrown by {@link #createNamed(org.dhus.store.datastore.config.NamedDataStoreConf)} if a data
    * store with specified name already exists.
    */
   public static class UnavailableNameException extends Exception
   {
      private static final long serialVersionUID = 1L;

      public UnavailableNameException(String msg)
      {
         super(msg);
      }
   }
}
