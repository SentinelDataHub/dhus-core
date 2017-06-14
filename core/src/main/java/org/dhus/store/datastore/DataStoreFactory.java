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
package org.dhus.store.datastore;

import fr.gael.dhus.database.object.DataStoreConfiguration;
import fr.gael.dhus.database.object.OpenstackDataStoreConf;
import org.dhus.store.datastore.config.DataStoreConf;
import org.dhus.store.datastore.config.HfsDataStoreConf;
import org.dhus.store.datastore.config.OpenStackDataStoreConf;
import org.dhus.store.datastore.hfs.HfsDataStore;
import org.dhus.store.datastore.hfs.HfsManager;
import org.dhus.store.datastore.hfs.OldIncomingDataStore;
import org.dhus.store.datastore.openstack.OpenStackDataStore;

final public class DataStoreFactory
{
   public static DataStore createDataStore(DataStoreConf configuration)
   {
      String name = configuration.getName();
      boolean read_only = configuration.isReadOnly();

      if (configuration instanceof HfsDataStoreConf)
      {
         HfsDataStoreConf conf = (HfsDataStoreConf) configuration;
         String path = conf.getPath();
         int file_no = conf.getMaxFileNo();

         return new HfsDataStore(name, new HfsManager(path, file_no), read_only);
      }

      if (configuration instanceof OpenStackDataStoreConf)
      {
         OpenStackDataStoreConf conf = (OpenStackDataStoreConf) configuration;
         String provider = conf.getProvider();
         String id = conf.getIdentity();
         String pwd = conf.getCredential();
         String url = conf.getUrl();
         String container = conf.getContainer();
         String region = conf.getRegion();
         return new OpenStackDataStore(name, provider, id, pwd, url, container, region, read_only);
      }

      throw new IllegalArgumentException("Invalid or unsupported dataStore configuration");
   }

   public static DataStore getOldIncomingDataStore()
   {
      return new OldIncomingDataStore();
   }

   /**
    * Creates a datastore from the given configuration.
    *
    * @param configuration datastore configuration
    * @return a datastore or null any error happened
    */
   public static DataStore createDataStore(DataStoreConfiguration configuration)
   {
      if (configuration instanceof fr.gael.dhus.database.object.HfsDataStoreConf)
      {
         fr.gael.dhus.database.object.HfsDataStoreConf conf =
               (fr.gael.dhus.database.object.HfsDataStoreConf) configuration;
         return new HfsDataStore(conf.getName(), new HfsManager(conf.getPath(),
               conf.getMaxFileDepth()), conf.isReadOnly());
      }
      else if (configuration instanceof OpenstackDataStoreConf)
      {
         OpenstackDataStoreConf conf = (OpenstackDataStoreConf) configuration;
         return new OpenStackDataStore(conf.getName(), conf.getProvider(),
               conf.getIdentity(), conf.getCredential(),
               conf.getUrl().toString(), conf.getContainer(),
               conf.getRegion(), conf.isReadOnly());
      }
      return null;
   }
}
