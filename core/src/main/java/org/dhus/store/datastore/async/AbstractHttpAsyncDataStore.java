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
package org.dhus.store.datastore.async;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Objects;
import java.util.Timer;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.domain.ClientEntitySetIterator;
import org.dhus.api.JobStatus;
import org.dhus.olingo.v2.OData4Client;
import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.store.OrderException;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.StreamableProduct;
import org.dhus.store.datastore.async.AsyncDataStoreException.ProductNotFoundException;
import org.dhus.store.datastore.async.pdgs.ParallelProductDownloadManager;
import org.dhus.store.datastore.config.PatternReplace;

import fr.gael.dhus.database.object.Order;
import fr.gael.dhus.database.object.Product;

public abstract class AbstractHttpAsyncDataStore extends AbstractAsyncCachedDataStore
{
   protected static final Logger LOGGER = LogManager.getLogger();

   protected static final String PROPERTY_ID = "Id";
   protected static final String PROPERTY_ID_ONDA = "id";
   protected static final String PROPERTY_NAME = "Name";
   protected static final String PROPERTY_NAME_ONDA = "name";
   protected static final String ORDER_ENTITY_SET_NAME = "Orders";
   protected static final String PROPERTY_ONLINE = "Online";
   protected static final String PROPERTY_OFFLINE = "offline";
   private static final String STATUS_RUNNING_ONDA = "RUNNING";
   private static final String STATUS_COMPLETED_ONDA = "COMPLETED";
   private static final String STATUS_FAILED_ONDA = "FAILED";
   private static final String STATUS_UNKNWON_ONDA = "UNKNWON";
   private static final String STATUS_COMPLETED = "completed";
   private static final String STATUS_IN_PROGRESS = "in_progress";
   private static final String STATUS_FAILED = "failed";
   private static final String STATUS_CANCELLED = "cancelled";
   private static final String STATUS_QUEUED = "queued";

   protected final OData4Client client;
   protected final ParallelProductDownloadManager parallelProductDownloadManager;

   //TODO is this the right place????
   /* Ingest Job. */
   protected Timer timer;

   // Optional order
   private boolean order;

   public AbstractHttpAsyncDataStore(String name, int priority, boolean isManager,
         PatternReplace patternReplaceIn, PatternReplace patternReplaceOut,
         Integer maxPendingRequests, Integer maxRunningRequests, long maximumSize, long currentSize,
         boolean autoEviction, String urlService, String login, String password, long interval,
         int maxConcurrentsDownloads, String[] hashAlgorithms, DataStore cache, boolean order)
         throws URISyntaxException, IOException, InterruptedException
   {
      super(name, priority, patternReplaceIn, patternReplaceOut, maxPendingRequests, maxRunningRequests,
            maximumSize, currentSize, autoEviction, hashAlgorithms, cache);

      Objects.requireNonNull(urlService, "A required UrlService is missing");

      client = new OData4Client(urlService, login, password);

      parallelProductDownloadManager = new ParallelProductDownloadManager(maxConcurrentsDownloads);
      
      this.order = order;
   }

   @Override
   public Order fetch(AsyncProduct to_fetch) throws DataStoreException
   {
      if (order == false)
      {
         throw new OrderException("The product " + to_fetch.getName() + " cannot be ordered");
      }
      else
      {
         return super.fetch(to_fetch);
      }
   }
   
   @Override
   protected Order internalFetch(String localIdentifier, String remoteIdentifier, String uuid,
         Long size) throws DataStoreException
   {
      ClientEntitySetIterator<ClientEntitySet, ClientEntity> entities = readEntitySet(localIdentifier);

      while (entities.hasNext())
      {
         ClientEntity productEntity = entities.next();
         if (productEntity != null)
         {
            String uuidProprty = getUuidProperty(productEntity);
            String nameProperty = getProductNameProperty(productEntity);
            String proprtyOnline = getOnlineProperty(productEntity);

            String productUuid = AbstractHttpJob.getPropertyValue(productEntity, uuidProprty).toString();
            String productName = AbstractHttpJob.getPropertyValue(productEntity, nameProperty).toString();
            boolean online = Boolean.parseBoolean(AbstractHttpJob.getPropertyValue(productEntity, proprtyOnline).toString());

            if ((PROPERTY_ONLINE.equals(proprtyOnline) && online == true)
                  || (PROPERTY_OFFLINE.equals(proprtyOnline) && online == false))
            {
               Order order = new Order(
                     getName(),
                     uuid,
                     productUuid,
                     JobStatus.RUNNING,
                     new Date(),
                     null,
                     "Job running");

               downloadProduct(uuid, productUuid, productUuid, productName, null, null);
               return order;
            }
            else
            {
               String actionName = getActionName();
               ClientEntity orderEntity = client.performAction(UUID.fromString(productUuid),
                     ProductModel.ENTITY_SET_NAME, actionName);

               if (orderEntity != null)
               {
                  return internalHttpFetch(orderEntity, productUuid, productName, uuid);
               }
            }
         }
      }
      return null;
   }

   @Override
   public org.dhus.Product get(String id) throws DataStoreException
   {
      if (order == false)
      {
         throw new ProductNotFoundException("The requested Product for id "+ id +" is not found");
      }
      else
      {
         return super.get(id);
      }
   }
   
   protected abstract Order internalHttpFetch(ClientEntity entity, String remoteUuid, String identifier, String localUuid);

   protected abstract String getOnlineProperty(ClientEntity productEntity);

   protected abstract String getUuidProperty(ClientEntity productEntity);

   protected abstract String getProductNameProperty(ClientEntity productEntity);

   protected abstract ClientEntitySetIterator<ClientEntitySet, ClientEntity> readEntitySet(String filter);

   protected abstract String getActionName();

   protected JobStatus getStatusJob(String status)
   {
      if (STATUS_IN_PROGRESS.equals(status) || STATUS_RUNNING_ONDA.equals(status) || STATUS_QUEUED.equals(status))
      {
         return JobStatus.RUNNING;
      }
      else if (STATUS_CANCELLED.equals(status) || STATUS_FAILED.equals(status)
            || STATUS_FAILED_ONDA.equals(status) || STATUS_UNKNWON_ONDA.equals(status))
      {
         return JobStatus.FAILED;
      }
      else
      {
         return JobStatus.COMPLETED;
      }
   }

   protected StreamableProduct getStreamableProduct(String remoteProductUuid, String remoteIdentifier)
   {
      InputStream stream = client.downloadEntityMedia(ProductModel.ENTITY_SET_NAME, UUID.fromString(remoteProductUuid));

      StreamableProduct streamableproduct = new StreamableProduct(stream);
      streamableproduct.setName(remoteIdentifier);
      return streamableproduct;
   }

   protected String getLocalIdentifier(String remoteIdentifier)
   {
      if (remoteIdentifier.endsWith(".zip") || remoteIdentifier.endsWith(".tar"))
      {
         return remoteIdentifier.substring(0, remoteIdentifier.length() - 4);
      }
      return remoteIdentifier;
   }

   protected void downloadProduct(String localUuid, String jobId, String remoteUuid, String remoteIdentifier,
         Date estimatedDate, String statusMessage)
   {
      StreamableProduct streamable = getStreamableProduct(remoteUuid, remoteIdentifier);

      // put the product in a queue to download it
      parallelProductDownloadManager.submit(streamable, () ->
      {
         try
         {
            moveProductToCache(streamable, localUuid);

            // refresh database order
            refreshOrder(localUuid, getName(), jobId, JobStatus.COMPLETED, estimatedDate,
                  statusMessage != null ? statusMessage : "Job completed");
         }
         catch (DataStoreException e)
         {
            LOGGER.error("Failed to move product {} to cache", remoteIdentifier, e);
            // refresh database order
            refreshOrder(localUuid, getName(), jobId, JobStatus.FAILED, estimatedDate,
                  statusMessage != null ? statusMessage : "Job failed");
         }
      });
      LOGGER.info("Product download {} successfully queued", streamable.getName());
   }

   protected int runJob(String jobId, String remoteIdentifier, String status, String remoteUuid,
         Date estimatedDate, String statusMessage)
   {
      int res = 0;
      String localIdentifier = getLocalIdentifier(remoteIdentifier);
      Product product = PRODUCT_SERVICE.getProducBytIdentifier(localIdentifier);
      if (product == null)
      {
         LOGGER.error("Product {} not found in the database", localIdentifier);
      }
      else
      {
         String uuid = product.getUuid();

         if (STATUS_COMPLETED.equals(status) || STATUS_COMPLETED_ONDA.equals(status))
         {
            // make sure the product is not already in the cache or
            // already queued for download
            if (!existsInCache(uuid) && !parallelProductDownloadManager.isAlreadyQueued(localIdentifier))
            {
               // download product
               downloadProduct(uuid, jobId, remoteUuid, remoteIdentifier, estimatedDate, statusMessage);
            }
            else
            {
               LOGGER.warn(
                     "Job for product {} ({}) found, but product already downloaded or downloading, skipping",
                     uuid, localIdentifier);
            }
            res++;
         }
         else
         {
            // refresh database order
            refreshOrder(
                  uuid,
                  getName(),
                  jobId,
                  getStatusJob(status),
                  estimatedDate,
                  statusMessage);
            if (STATUS_IN_PROGRESS.equals(status) || STATUS_RUNNING_ONDA.equals(status))
            {
               LOGGER.debug("the requested product {} is being processed",
                     localIdentifier);
            }
            else
            {
               LOGGER.error("product {} retrieval has failed", localIdentifier);
            }
         }
      }
      LOGGER.debug("Started restore of {} products in {}", res, getName());

      // check running downloads
      parallelProductDownloadManager.checkProductDownloads();
      return res;
   }

   @Override
   public void close() throws Exception
   {
      if (this.timer != null)
      {
         try
         {
            this.timer.cancel();
         }
         catch (RuntimeException suppressed) {}
      }
      parallelProductDownloadManager.shutdownNow();
   }
}
