/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2018 GAEL Systems
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

import fr.gael.dhus.system.config.ConfigurationManager;

import java.util.List;

import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreFactory;
import org.dhus.store.datastore.DataStoreFactory.InvalidConfigurationException;
import org.dhus.store.datastore.config.DataStoreConf;
import org.dhus.store.datastore.config.DataStoreManager.UnavailableNameException;
import org.dhus.store.datastore.config.NamedDataStoreConf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataStoreService
{
   @Autowired
   private ConfigurationManager cfgManager;

   public DataStore getDataStoreByName(String name) throws InvalidConfigurationException
   {
      return DataStoreFactory.createDataStore(cfgManager.getDataStoreManager().get(name));
   }

   @Deprecated
   public DataStoreConf getDataStoreConfigurationByName(String name)
   {
      return cfgManager.getDataStoreManager().get(name);
   }

   public NamedDataStoreConf getNamedDataStore(String name)
   {
      return cfgManager.getDataStoreManager().getNamed(name);
   }

   public void createNamed(NamedDataStoreConf namedDataStoreConf) throws UnavailableNameException
   {
      cfgManager.getDataStoreManager().createNamed(namedDataStoreConf);
   }

   public void delete(DataStoreConf dataStoreConf)
   {
      cfgManager.getDataStoreManager().delete(dataStoreConf);
   }

   public void update(NamedDataStoreConf dataStoreConf)
   {
      cfgManager.getDataStoreManager().update((NamedDataStoreConf) dataStoreConf);
   }

   public List<NamedDataStoreConf> getNamedDataStoreConfigurations()
   {
      return cfgManager.getDataStoreManager().getNamedDataStoreConfigurations();
   }

   public List<DataStoreConf> getUnnamedDataStoreConfigurations()
   {
      return cfgManager.getDataStoreManager().getUnnamedDataStoreConfigurations();
   }

   public List<DataStoreConf> getAllDataStoreConfigurations()
   {
      return cfgManager.getDataStoreManager().getAllDataStoreConfigurations();
   }

   public boolean dataStoreExists(String dataStoreName)
   {
      return getDataStoreConfigurationByName(dataStoreName) != null;
   }

   public long varyCurrentSize(String dataStoreName, long amount)
   {
      return cfgManager.getDataStoreManager().varyCurrentSize(dataStoreName, amount);
   }
}
