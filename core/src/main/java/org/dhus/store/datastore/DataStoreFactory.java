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
package org.dhus.store.datastore;

import fr.gael.dhus.service.SecurityService;
import fr.gael.dhus.service.StoreQuotaService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.olingo.odata2.api.exception.ODataException;
import org.dhus.store.datastore.async.gmp.GmpDataStore;
import org.dhus.store.datastore.config.DataStoreConf;
import org.dhus.store.datastore.config.RemoteDhusDataStoreConf;
import org.dhus.store.datastore.config.GmpDataStoreConf;
import org.dhus.store.datastore.config.HfsDataStoreConf;
import org.dhus.store.datastore.config.NamedDataStoreConf;
import org.dhus.store.datastore.config.OpenStackDataStoreConf;
import org.dhus.store.datastore.hfs.HfsDataStore;
import org.dhus.store.datastore.hfs.HfsManager;
import org.dhus.store.datastore.openstack.OpenStackDataStore;
import org.dhus.store.datastore.remotedhus.RemoteDhusDataStore;
import org.dhus.store.quota.FetchLimiterAsyncDataSource;

final public class DataStoreFactory
{
   public static DataStore createDataStore(DataStoreConf configuration) throws InvalidConfigurationException
   {
      boolean read_only = configuration.isReadOnly();
      int priority = configuration.getPriority();
      long maximumSize = configuration.getMaximumSize();
      long currentSize = configuration.getCurrentSize();
      boolean autoEviction = configuration.isAutoEviction();

      if (configuration instanceof NamedDataStoreConf)
      {
         NamedDataStoreConf n_conf = NamedDataStoreConf.class.cast(configuration);
         String name = n_conf.getName();

         if (configuration instanceof HfsDataStoreConf)
         {
            HfsDataStoreConf conf = (HfsDataStoreConf) configuration;

            return new HfsDataStore(
                  name,
                  new HfsManager(conf.getPath(), conf.getMaxFileNo(), conf.getMaxItems()),
                  read_only,
                  priority,
                  maximumSize,
                  currentSize,
                  autoEviction);
         }

         if (configuration instanceof OpenStackDataStoreConf)
         {
            OpenStackDataStoreConf conf = (OpenStackDataStoreConf) configuration;
            return new OpenStackDataStore(
                  name,
                  conf.getProvider(),
                  conf.getIdentity(),
                  conf.getCredential(),
                  conf.getUrl(),
                  conf.getContainer(),
                  conf.getRegion(),
                  read_only,
                  priority,
                  maximumSize,
                  currentSize,
                  autoEviction);
         }

         if (configuration instanceof GmpDataStoreConf)
         {
            GmpDataStoreConf conf = GmpDataStoreConf.class.cast(configuration);
            GmpDataStore gmpDs;
            try
            {
               gmpDs = GmpDataStore.make(conf);
            }
            catch (DataStoreException e)
            {
               throw new InvalidConfigurationException(e.getMessage(), e);
            }
            if (conf.getQuotas() != null)
            {
               SecurityService secuSvc = ApplicationContextProvider.getBean(SecurityService.class);
               StoreQuotaService quotaSvc = ApplicationContextProvider.getBean(StoreQuotaService.class);
               int maxQPU = conf.getQuotas().getMaxQueryPerUser();
               long timespan = conf.getQuotas().getTimespan();
               return new FetchLimiterAsyncDataSource<GmpDataStore>(gmpDs, secuSvc, quotaSvc, maxQPU, timespan);
            }
            return gmpDs;
         }

         if(configuration instanceof RemoteDhusDataStoreConf)
         {
            RemoteDhusDataStoreConf dhusDataStoreConf = (RemoteDhusDataStoreConf) configuration;
            try
            {
               // always read only
               // no auto eviction or size management supported
               return new RemoteDhusDataStore(
                     dhusDataStoreConf.getName(),
                     dhusDataStoreConf.getServiceUrl(),
                     dhusDataStoreConf.getLogin(),
                     dhusDataStoreConf.getPassword(),
                     dhusDataStoreConf.getPriority());
            }
            catch (URISyntaxException | IOException | ODataException e)
            {
               throw new InvalidConfigurationException(e.getMessage(), e);
            }
         }
      }
      throw new InvalidConfigurationException("Invalid or unsupported dataStore configuration");
   }

   public static class InvalidConfigurationException extends Exception
   {
      private static final long serialVersionUID = 1L;

      public InvalidConfigurationException(String msg)
      {
         super(msg);
      }

      public InvalidConfigurationException(String msg, Throwable e)
      {
         super(msg, e);
      }
   }
}
