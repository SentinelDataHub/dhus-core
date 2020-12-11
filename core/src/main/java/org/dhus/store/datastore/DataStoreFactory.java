/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2019 GAEL Systems
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

import com.codahale.metrics.MetricRegistry;

import fr.gael.dhus.olingo.v1.visitor.ProductSQLVisitor;
import fr.gael.dhus.service.SecurityService;
import fr.gael.dhus.service.StoreQuotaService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.io.IOException;
import java.net.URISyntaxException;

import org.dhus.store.datastore.async.AsyncDataStore;
import org.dhus.store.datastore.async.gmp.GmpDataStore;
import org.dhus.store.datastore.async.pdgs.ParamPdgsDataStore;
import org.dhus.store.datastore.async.pdgs.PdgsDataStore;
import org.dhus.store.datastore.config.DataStoreConf;
import org.dhus.store.datastore.config.DataStoreRestriction;
import org.dhus.store.datastore.config.GmpDataStoreConf;
import org.dhus.store.datastore.config.HfsDataStoreConf;
import org.dhus.store.datastore.config.NamedDataStoreConf;
import org.dhus.store.datastore.config.OpenStackDataStoreConf;
import org.dhus.store.datastore.config.ParamPdgsDataStoreConf;
import org.dhus.store.datastore.config.PdgsDataStoreConf;
import org.dhus.store.datastore.config.RemoteDhusDataStoreConf;
import org.dhus.store.datastore.hfs.HfsDataStore;
import org.dhus.store.datastore.hfs.HfsManager;
import org.dhus.store.datastore.openstack.OpenStackDataStore;
import org.dhus.store.datastore.remotedhus.RemoteDhusDataStore;
import org.dhus.store.filter.FilteredAsyncDataStore;
import org.dhus.store.filter.FilteredDataStore;
import org.dhus.store.quota.FetchLimiterAsyncDataStore;

final public class DataStoreFactory
{
   private static final SecurityService SECURITY_SERVICE = ApplicationContextProvider.getBean(SecurityService.class);
   private static final StoreQuotaService STORE_QUOTA_SERVICE = ApplicationContextProvider.getBean(StoreQuotaService.class);
   private static final ConfigurationManager CFG_MGR = ApplicationContextProvider.getBean(ConfigurationManager.class);

   /** Metric Registry, for monitoring purposes. */
   private static final MetricRegistry METRIC_REGISTRY = ApplicationContextProvider.getBean(MetricRegistry.class);

   private static String[] getHashAlgorithms()
   {
      String res = CFG_MGR.getDownloadConfiguration().getChecksumAlgorithms();
      if (res == null || res.trim().isEmpty())
      {
         return null;
      }
      return res.split(",");
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   public static DataStore createDataStore(DataStoreConf configuration) throws InvalidConfigurationException
   {
      DataStoreRestriction restriction = configuration.getRestriction();
      int priority = configuration.getPriority();
      long maximumSize = configuration.getMaximumSize();
      long currentSize = configuration.getCurrentSize();
      boolean autoEviction = configuration.isAutoEviction();
      String filter = configuration.getFilter();
      ProductSQLVisitor visitor = null;

      // synchronous data stores
      if (configuration instanceof NamedDataStoreConf)
      {
         NamedDataStoreConf n_conf = NamedDataStoreConf.class.cast(configuration);
         HfsDataStore hfs;
         String name = n_conf.getName();

         if (filter != null)
         {
            visitor = FilteredDataStore.makeVisitor(filter);
         }

         if (configuration instanceof HfsDataStoreConf)
         {
            HfsDataStoreConf conf = (HfsDataStoreConf) configuration;

            hfs = new HfsDataStore(
                  name,
                  new HfsManager(conf.getPath(), conf.getMaxFileNo(), conf.getMaxItems()),
                  restriction,
                  priority,
                  maximumSize,
                  currentSize,
                  autoEviction,
                  getHashAlgorithms());

            if (visitor != null)
            {
               return new FilteredDataStore(hfs, visitor);
            }
            return hfs;
         }

         if (configuration instanceof OpenStackDataStoreConf)
         {
            OpenStackDataStoreConf conf = (OpenStackDataStoreConf) configuration;
            OpenStackDataStore openStackDataStore;
            openStackDataStore = new OpenStackDataStore(
                  name,
                  conf.getProvider(),
                  conf.getIdentity(),
                  conf.getCredential(),
                  conf.getUrl(),
                  conf.getContainer(),
                  conf.getRegion(),
                  restriction,
                  priority,
                  maximumSize,
                  currentSize,
                  autoEviction,
                  getHashAlgorithms());
            if (filter != null)
            {
               return new FilteredDataStore(openStackDataStore, visitor);
            }
            return openStackDataStore;
         }

         if (configuration instanceof RemoteDhusDataStoreConf)
         {
            RemoteDhusDataStoreConf dhusDataStoreConf = (RemoteDhusDataStoreConf) configuration;

            // always read only
            // no auto eviction or size management supported
            RemoteDhusDataStore remoteDhusDataStore;
            remoteDhusDataStore = new RemoteDhusDataStore(
                  dhusDataStoreConf.getName(),
                  dhusDataStoreConf.getServiceUrl(),
                  dhusDataStoreConf.getLogin(),
                  dhusDataStoreConf.getPassword(),
                  dhusDataStoreConf.getPriority(),
                  dhusDataStoreConf.getAliveInterval(),
                  dhusDataStoreConf.getRestriction());

            if (filter != null)
            {
               return new FilteredDataStore.FileteredDerivedDataStore<>(remoteDhusDataStore, visitor);
            }

            return remoteDhusDataStore;
         }

         // asynchronous data stores
         if (configuration instanceof GmpDataStoreConf)
         {
            GmpDataStoreConf conf = GmpDataStoreConf.class.cast(configuration);
            GmpDataStore gmpDs;
            try
            {
               gmpDs = GmpDataStore.make(conf, getHashAlgorithms());
               METRIC_REGISTRY.registerAll(gmpDs);
            }
            catch (DataStoreException e)
            {
               throw new InvalidConfigurationException(e.getMessage(), e);
            }
            AsyncDataStore asyncDataStore = gmpDs;
            return asyncDecorator(asyncDataStore, conf.getMaxParallelFetchRequestsPerUser(), visitor);
         }

         // checked BEFORE PdgsDataStoreConf since MetadataPdgsDataStoreConf extends it
         if(configuration instanceof ParamPdgsDataStoreConf)
         {
            ParamPdgsDataStoreConf paramPdgsConf = (ParamPdgsDataStoreConf) configuration;
            ParamPdgsDataStore paramPdgsDataStore;
            try
            {
               paramPdgsDataStore = new ParamPdgsDataStore(
                     paramPdgsConf.getName(),
                     paramPdgsConf.getPriority(),
                     paramPdgsConf.isIsMaster(),
                     paramPdgsConf.getHfsLocation(),
                     paramPdgsConf.getPatternReplaceIn(),
                     paramPdgsConf.getPatternReplaceOut(),
                     paramPdgsConf.getMaxPendingRequests(),
                     paramPdgsConf.getMaxRunningRequests(),
                     paramPdgsConf.getMaximumSize(),
                     paramPdgsConf.getCurrentSize(),
                     paramPdgsConf.isAutoEviction(),
                     paramPdgsConf.getServiceUrl(),
                     paramPdgsConf.getLogin(),
                     paramPdgsConf.getPassword(),
                     paramPdgsConf.getInterval(),
                     paramPdgsConf.getMaxConcurrentsDownloads(),
                     getHashAlgorithms(),
                     paramPdgsConf.getUrlParamPattern(),
                     paramPdgsConf.getProductNamePattern());
            }
            catch (URISyntaxException | IOException e)
            {
               throw new InvalidConfigurationException(e.getMessage(), e);
            }
            return asyncDecorator(paramPdgsDataStore, paramPdgsConf.getMaxParallelFetchRequestsPerUser(), visitor);
         }

         // checked AFTER MetadataPdgsDataStoreConf since it extends PdgsDataStoreConf
         if (configuration instanceof PdgsDataStoreConf)
         {
            PdgsDataStoreConf pdgsDataStoreConf = (PdgsDataStoreConf) configuration;
            PdgsDataStore pdgsDataStore;
            try
            {
               pdgsDataStore = new PdgsDataStore(
                     pdgsDataStoreConf.getName(),
                     pdgsDataStoreConf.getPriority(),
                     pdgsDataStoreConf.isIsMaster(),
                     pdgsDataStoreConf.getHfsLocation(),
                     pdgsDataStoreConf.getPatternReplaceIn(),
                     pdgsDataStoreConf.getPatternReplaceOut(),
                     pdgsDataStoreConf.getMaxPendingRequests(),
                     pdgsDataStoreConf.getMaxRunningRequests(),
                     pdgsDataStoreConf.getMaximumSize(),
                     pdgsDataStoreConf.getCurrentSize(),
                     pdgsDataStoreConf.isAutoEviction(),
                     pdgsDataStoreConf.getServiceUrl(),
                     pdgsDataStoreConf.getLogin(),
                     pdgsDataStoreConf.getPassword(),
                     pdgsDataStoreConf.getInterval(),
                     pdgsDataStoreConf.getMaxConcurrentsDownloads(),
                     getHashAlgorithms());
               METRIC_REGISTRY.registerAll(pdgsDataStore);
            }
            catch (URISyntaxException | IOException e)
            {
               throw new InvalidConfigurationException(e.getMessage(), e);
            }
            AsyncDataStore asyncDataStore = pdgsDataStore;
            return asyncDecorator(asyncDataStore, pdgsDataStoreConf.getMaxParallelFetchRequestsPerUser(), visitor);
         }
      }
      throw new InvalidConfigurationException("Invalid or unsupported dataStore configuration");
   }

   private static AsyncDataStore asyncDecorator(AsyncDataStore asyncDataStore, Integer maxQueryPerUser, ProductSQLVisitor visitor)
         throws InvalidConfigurationException
   {
      if (maxQueryPerUser != null)
      {
         asyncDataStore = new FetchLimiterAsyncDataStore(
               asyncDataStore,
               SECURITY_SERVICE,
               STORE_QUOTA_SERVICE,
               maxQueryPerUser);
      }
      if (visitor != null)
      {
         asyncDataStore = new FilteredAsyncDataStore(asyncDataStore, visitor);
      }
      return asyncDataStore;
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
