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
package org.dhus.store.datastore.async.lta;

import fr.gael.dhus.database.object.Order;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.UUID;

import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.domain.ClientEntitySetIterator;
import org.apache.olingo.client.api.domain.ClientProperty;

import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.async.AbstractHttpAsyncDataStore;
import org.dhus.store.datastore.config.PatternReplace;

public class LtaDataStore extends AbstractHttpAsyncDataStore
{
   private static final String ORDER_ACTION_NAME = "OData.CSC.Order";

   /**
    * Create a LTA DateStore.
    *
    * @param name                    of this DataStore
    * @param priority                DataStores are ordered
    * @param isManager               true to enable the ingest job in this instance of the DHuS (only one instance per cluster)
    * @param patternReplaceIn        transform PDGS identifiers to DHuS identifiers
    * @param patternReplaceOut       transform DHuS identifiers to PDGS identifiers
    * @param maxPendingRequests      maximum number of pending orders at the same time
    * @param maxRunningRequests      maximum number of running orders at the same time
    * @param maximumSize             maximum size in bytes of the local cache DataStore
    * @param currentSize             overall size of the local HFS cache DataStore (disk usage)
    * @param autoEviction            true to activate auto-eviction based on disk usage on the local HFS cache DataStore
    * @param urlService              URL to connect to the LTA Service
    * @param login                   user to log to LTA Service
    * @param password                password to log to LTA Service
    * @param interval                interval
    * @param maxConcurrentsDownloads maximum number of product download occurring in parallel
    * @param hashAlgorithms          to compute on restore
    * @param cache                   local cache, can be HFS or OpenStack
    *
    * @throws URISyntaxException   could not create LTADataStore
    * @throws IOException          could not create LTA repo location directory
    * @throws InterruptedException could not initialize OData client
    */
   public LtaDataStore(String name, int priority, boolean isManager,
         PatternReplace patternReplaceIn, PatternReplace patternReplaceOut,
         Integer maxPendingRequests, Integer maxRunningRequests, long maximumSize, long currentSize,
         boolean autoEviction, String urlService, String login, String password, long interval,
         int maxConcurrentsDownloads, String[] hashAlgorithms, DataStore cache, boolean order)
         throws URISyntaxException, IOException, InterruptedException
   {
      super(name, priority, isManager, patternReplaceIn, patternReplaceOut, maxPendingRequests,
            maxRunningRequests, maximumSize, currentSize, autoEviction, urlService, login, password,
            interval, maxConcurrentsDownloads, hashAlgorithms, cache, order);
      
      LOGGER.info("New LTA DataStore, name={} url={}", getName(), urlService);

      LOGGER.info("This DHuS instance {} the LTA manager", isManager ? "is" : "isn't");

      if (isManager)
      {
         timer = new Timer("LTA ingest job", true);
         timer.schedule(new RunJob(), 60_000, interval);
      }
      else
      {
         timer = null;
      }
   }

   @Override
   protected Order internalHttpFetch(ClientEntity entity, String remoteUuid, String identifier,
         String LocalUuid)
   {
      LTAJob ltaJob = new LTAJob(entity, remoteUuid, identifier);
      return new Order(
            getName(),
            LocalUuid,
            ltaJob.getJobId(),
            getStatusJob(ltaJob.getStatus()),
            new Date(),
            ltaJob.getEstimatedDate(),
            ltaJob.getStatusMessage());
   }

   @Override
   protected String getOnlineProperty(ClientEntity productEntity)
   {
      ClientProperty property = productEntity.getProperty(PROPERTY_ONLINE);
      return property.getName();
   }

   @Override
   protected String getUuidProperty(ClientEntity productEntity)
   {
      ClientProperty property = productEntity.getProperty(PROPERTY_ID);
      return property.getName();
   }

   @Override
   protected String getProductNameProperty(ClientEntity productEntity)
   {
      ClientProperty property = productEntity.getProperty(PROPERTY_NAME);
      return property.getName();
   }

   @Override
   protected ClientEntitySetIterator<ClientEntitySet, ClientEntity> readEntitySet(String identifier)
   {
      String filter = "startswith(Name,'" + identifier + "')";
      return client.readEntitySet(ProductModel.ENTITY_SET_NAME, filter, null);
   }

   @Override
   protected String getActionName()
   {
      return ORDER_ACTION_NAME;
   }

   private List<LTAJob> getAllJobs()
   {
      List<LTAJob> ltaJobList = new ArrayList<>();
      ClientEntitySetIterator<ClientEntitySet, ClientEntity> entitySetIt = client.readEntitySet(ORDER_ENTITY_SET_NAME, null, null);
      while (entitySetIt.hasNext())
      {
         ClientEntity entity = entitySetIt.next();
         String key = LTAJob.getPropertyValue(entity, PROPERTY_ID).toString();
         ClientEntity productEntity = client.navigationLinkEntity(UUID.fromString(key), ORDER_ENTITY_SET_NAME, ProductModel.ENTITY_TYPE_NAME);

         String productUuid = LTAJob.getPropertyValue(productEntity, PROPERTY_ID).toString();
         String productName = LTAJob.getPropertyValue(productEntity, PROPERTY_NAME).toString();

         LTAJob ltaJob = new LTAJob(entity, productUuid, productName);
         ltaJobList.add(ltaJob);
      }
      return ltaJobList;
   }

   private final class RunJob extends IngestTask
   {
      @Override
      protected int ingestCompletedFetches()
      {
         List<LTAJob> ltaJobsList = getAllJobs();

         for (LTAJob ltaJob : ltaJobsList)
         {
            Order order = ORDER_SERVICE.getOrderByJobId(ltaJob.getJobId());
            if (order == null)
            {
               continue;
            }
           return runJob(
                 ltaJob.getJobId(),
                 ltaJob.getProductName(),
                 ltaJob.getStatus(),
                 ltaJob.getProductUuid(),
                 ltaJob.getEstimatedDate(),
                 ltaJob.getStatusMessage());
         }
         return 0;
      }
   }
}
