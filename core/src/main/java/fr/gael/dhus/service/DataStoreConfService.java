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

import fr.gael.dhus.database.dao.DataStoreConfigurationDao;
import fr.gael.dhus.database.object.DataStoreConfiguration;

import java.util.List;

import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataStoreConfService
{
   @Autowired
   private DataStoreConfigurationDao dataStoreDao;

   @Transactional(readOnly = true)
   public DataStore getDataStore(Long id)
   {
      return DataStoreFactory.createDataStore(dataStoreDao.read(id));
   }

   @Transactional(readOnly = true)
   public DataStore getDataStoreByName(String name)
   {
      return DataStoreFactory.createDataStore(
            dataStoreDao.getDataStoreConfigurationByName(name));
   }

   @Transactional(readOnly = true)
   public DataStoreConfiguration getDataStoreConfigurationByName(String name)
   {
      return dataStoreDao.getDataStoreConfigurationByName(name);
   }

   @Transactional(readOnly = false)
   public void create(DataStoreConfiguration dataStoreConf)
   {
      dataStoreDao.create(dataStoreConf);
   }

   @Transactional
   public void delete(DataStoreConfiguration dataStoreConf)
   {
      dataStoreDao.delete(dataStoreConf);
   }

   @Transactional
   public void update(DataStoreConfiguration dataStoreConf)
   {
      dataStoreDao.update(dataStoreConf);
   }

   @Transactional(readOnly = true)
   public List<DataStoreConfiguration> getDataStoreConfigurations()
   {
      return dataStoreDao.readAll();
   }

   @Transactional(readOnly = true)
   public boolean dataStoreExists(String dataStoreName)
   {
      return getDataStoreConfigurationByName(dataStoreName) != null;
   }
}
