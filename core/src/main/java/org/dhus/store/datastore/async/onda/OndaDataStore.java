/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2020 GAEL Systems
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
package org.dhus.store.datastore.async.onda;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.domain.ClientEntitySetIterator;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.dhus.Product;
import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.DataStoreFactory.InvalidConfigurationException;
import org.dhus.store.datastore.async.AbstractHttpAsyncDataStore;
import org.dhus.store.datastore.config.ObjectStorageCredentialConf;
import org.dhus.store.datastore.config.OndaDataStoreConf;
import org.dhus.store.datastore.openstack.OpenStackLocation;
import org.dhus.store.datastore.openstack.OpenStackObject;
import org.dhus.store.datastore.openstack.OpenStackProduct;
import org.dhus.store.keystore.KeyStore;
import org.dhus.store.keystore.PersistentKeyStore;
import org.quartz.SchedulerException;

import fr.gael.dhus.database.object.KeyStoreEntry;
import fr.gael.dhus.database.object.Order;
import fr.gael.dhus.database.object.config.cron.Cron;
import fr.gael.drb.impl.swift.DrbSwiftObject;

public class OndaDataStore extends AbstractHttpAsyncDataStore
{
   private static final String ORDER_ACTION_NAME = "Ens.Order";

   private static final OndaDataStoreScannerScheduler JOB_SCHEDULER = new OndaDataStoreScannerScheduler();

   private final String provider;
   private final String identity;
   private final String credential;
   private final String url;

   private KeyStore keystore;
   private String scannerId;

   public OndaDataStore(OndaDataStoreConf ondaDataStoreConf, String[] hashAlgorithms, DataStore cache)
         throws URISyntaxException, IOException, InterruptedException, InvalidConfigurationException
   {
      super(ondaDataStoreConf.getName(),
            ondaDataStoreConf.getPriority(),
            ondaDataStoreConf.isIsMaster(),
            ondaDataStoreConf.getPatternReplaceIn(),
            ondaDataStoreConf.getPatternReplaceOut(),
            ondaDataStoreConf.getMaxPendingRequests(),
            ondaDataStoreConf.getMaxRunningRequests(),
            ondaDataStoreConf.getMaximumSize(),
            ondaDataStoreConf.getCurrentSize(),
            ondaDataStoreConf.isAutoEviction(),            
            ondaDataStoreConf.getServiceUrl(),
            ondaDataStoreConf.getLogin(),
            ondaDataStoreConf.getPassword(),
            ondaDataStoreConf.getInterval(),
            ondaDataStoreConf.getMaxConcurrentsDownloads(), hashAlgorithms, cache,
            ondaDataStoreConf.isOrder());
       
      scannerId = getName()+"-"+UUID.randomUUID();
      
      boolean isManager = ondaDataStoreConf.isIsMaster();
      Cron scannerCron = ondaDataStoreConf.getOndaScanner() == null ? null : ondaDataStoreConf.getOndaScanner().getCron();
      
      LOGGER.info("New ONDA DataStore, name={} url={}", getName(), ondaDataStoreConf.getServiceUrl());

      LOGGER.info("This DHuS instance {} the ONDA manager", isManager ? "is" : "isn't");

      if (isManager)
      {
         try
         {
            // Start ONDA Scanner only on master node
            if (scannerCron != null && scannerCron.isActive())
            {
               LOGGER.info("Onda scanner cron scheduled for " + ondaDataStoreConf.getName());
               JOB_SCHEDULER.scheduleScanner(scannerId, ondaDataStoreConf);
               if (!JOB_SCHEDULER.isStarted())
               {
                  JOB_SCHEDULER.start();
               }
            }
         }
         catch (SchedulerException | ParseException e)
         {
            throw new InvalidConfigurationException(e.getMessage(), e);
         }

         if (ondaDataStoreConf.isOrder())
         {
            timer = new Timer("ONDA ingest job", true);
            timer.schedule(new RunJob(), 60000, ondaDataStoreConf.getInterval());
         }
         else
         {
            timer = null;
         }
      }
      else
      {
         timer = null;
      }

      ObjectStorageCredentialConf cred = ondaDataStoreConf.getObjectStorageCredential();
      if (cred == null)
      {
         this.provider = null;
         this.identity = null;
         this.credential = null;
         this.url = null;
      }
      else
      {
         this.provider = cred.getProvider();
         this.identity = cred.getIdentity();
         this.credential = cred.getCredential();
         this.url = cred.getUrl();
      }

      this.keystore = new PersistentKeyStore(ondaDataStoreConf.getName());
   }

   @Override
   protected Order internalHttpFetch(ClientEntity entity, String remoteUuid, String identifier,
         String localUuid)
   {
      ONDAJob ondaJob = new ONDAJob(entity, remoteUuid, identifier);
      return new Order(
            getName(),
            localUuid,
            ondaJob.getJobId(),
            getStatusJob(ondaJob.getStatus()),
            new Date(),
            null,
            ondaJob.getStatusMessage());
   }

   @Override
   protected String getOnlineProperty(ClientEntity productEntity)
   {
      ClientProperty property = productEntity.getProperty(PROPERTY_OFFLINE);
      return property.getName();
   }

   @Override
   protected String getUuidProperty(ClientEntity productEntity)
   {
      ClientProperty property = productEntity.getProperty(PROPERTY_ID_ONDA);
      return property.getName();
   }

   @Override
   protected String getProductNameProperty(ClientEntity productEntity)
   {
      ClientProperty property = productEntity.getProperty(PROPERTY_NAME_ONDA);
      return property.getName();
   }

   @Override
   protected ClientEntitySetIterator<ClientEntitySet, ClientEntity> readEntitySet(String identifier)
   {
      String search = "\"name:" + identifier + "*\"";
      return client.readEntitySet(ProductModel.ENTITY_SET_NAME, null, search);
   }

   @Override
   protected String getActionName()
   {
      return ORDER_ACTION_NAME;
   }

   @Override
   public boolean hasProduct(String uuid)
   {
      boolean existInObjectStorage = keystore.exists(uuid, UNALTERED_PRODUCT_TAG);
      if (existInObjectStorage)
      {
         return true;
      }
      else
      {
         return super.hasProduct(uuid);
      }
   }

   @Override
   public void deleteProduct(String uuid) throws DataStoreException
   {
      boolean existInObjectStorage = keystore.exists(uuid, UNALTERED_PRODUCT_TAG);
      if (existInObjectStorage)
      {
         keystore.remove(uuid, UNALTERED_PRODUCT_TAG);
      }
      else
      {
         super.deleteProduct(uuid);
      }
   }

   @Override
   public Product get(String id) throws DataStoreException
   {
      boolean existInObjectStorage = keystore.exists(id, UNALTERED_PRODUCT_TAG);
      if (existInObjectStorage)
      {
         String productLocation = keystore.get(id, UNALTERED_PRODUCT_TAG);
         OpenStackObject openStackObject = new OpenStackObject(provider, identity, credential, url);
         DrbSwiftObject swiftObject = openStackObject.getOpenStackObject();
         OpenStackLocation location = new OpenStackLocation(productLocation);
         return new OpenStackProduct(swiftObject, location, id);
      }
      else
      {
         return super.get(id);
      }
   }

   private final class RunJob extends IngestTask
   {
      @Override
      protected int ingestCompletedFetches()
      {
         List<Order> orderList = ORDER_SERVICE.getRunningOrdersByDataStore(getName());

         for (Order order : orderList)
         {
            ClientEntity orderEntity = client.readSingleEntity(ORDER_ENTITY_SET_NAME, order.getJobId());
            ClientEntity productEntity = client.readSingleEntity(ProductModel.ENTITY_SET_NAME,
                  UUID.fromString(order.getJobId()));
            String productName = ONDAJob.getPropertyValue(productEntity, PROPERTY_NAME_ONDA).toString();
            ONDAJob ondaJob = new ONDAJob(orderEntity, order.getJobId(), productName);

            return runJob(
                  ondaJob.getJobId(),
                  ondaJob.getProductName(),
                  ondaJob.getStatus(),
                  ondaJob.getJobId(),
                  ondaJob.getEstimatedDate(),
                  ondaJob.getStatusMessage());
         }
         return 0;
      }
   }

   @Override
   public void close() throws Exception
   {
      if (JOB_SCHEDULER.isStarted())
      {
         JOB_SCHEDULER.unscheduleScanner(scannerId);
      }
      super.close();
   }

   @Override
   public Iterator<String> getScrollableProductResults()
   {
      final Iterator<KeyStoreEntry> iter = keystore.getUnalteredScrollableProductEntries();
      return new Iterator<String>()
      {
         @Override
         public String next()
         {
            return iter.next().getEntryKey();
         }
         @Override
         public boolean hasNext()
         {
            return iter.hasNext();
         }
      };
   }

   @Override
   public List<String> getProductList()
   {
      List<KeyStoreEntry> entryList = keystore.getUnalteredProductEntries();
      if (!entryList.isEmpty())
      {
         return toKeyList(entryList);
      }
      else
      {
         return super.getProductList();
      }
   }

   @Override
   public List<String> getProductList(int skip, int top)
   {
      List<KeyStoreEntry> entryList = keystore.getUnalteredProductEntries(skip, top);
      if (!entryList.isEmpty())
      {
         return toKeyList(entryList);
      }
      else
      {
         return super.getProductList(skip, top);
      }
   }

   private List<String> toKeyList(List<KeyStoreEntry> keyList)
   {
      // map keystore entries to uuids
      return keyList.stream()
            .map(keyStoreEntry -> keyStoreEntry.getEntryKey())
            .collect(Collectors.toList());
   }   
}