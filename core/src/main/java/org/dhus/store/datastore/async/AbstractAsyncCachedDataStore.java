/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018-2020 GAEL Systems
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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dhus.Product;
import org.dhus.ProductConstants;
import org.dhus.api.JobStatus;
import org.dhus.store.StoreException;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.ProductAlreadyExist;
import org.dhus.store.datastore.ProductNotFoundException;
import org.dhus.store.datastore.ReadOnlyDataStoreException;
import org.dhus.store.datastore.config.DataStoreRestriction;
import org.dhus.store.datastore.config.PatternReplace;
import org.dhus.store.datastore.hfs.HfsDataStore;
import org.dhus.store.datastore.hfs.HfsManager;
import org.dhus.store.ingestion.IngestibleProduct;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;

import fr.gael.dhus.database.object.Order;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.service.OrderService;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.service.SecurityService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

/**
 * An AsyncDataStore implements both the DataStore and AsyncDataSource interfaces, It uses an
 * HFSDataStore as its backing cache store.
 */
public abstract class AbstractAsyncCachedDataStore implements AsyncDataStore, MetricSet
{
   private static final Logger LOGGER = LogManager.getLogger();

   protected static final ProductService PRODUCT_SERVICE =
         ApplicationContextProvider.getBean(ProductService.class);

   private static final OrderService ORDER_SERVICE =
         ApplicationContextProvider.getBean(OrderService.class);

   /** Local HFS cache. */
   private final HfsDataStore cache;

   protected static final SecurityService SECURITY_SERVICE =
         ApplicationContextProvider.getBean(SecurityService.class);

   /* Data Store identifier. */
   private final String name;

   /* Position of this DataStore among all other DataStores. */
   private final int priority;

   /* Pattern to reformat products names coming FROM the remote data source */
   private final PatternReplace patternReplaceIn;

   /* Pattern to reformat products names sent TO the remote data source */
   private final PatternReplace patternReplaceOut;

   /* Maximum pending requests until the DHuS has to refuse a new async product orders */
   private final int maxPendingRequests;

   /* Maximum pending requests that this data store can submit to its remote */
   private final int maxRunningRequests;

   /** Metric Set. */
   private final HashMap<String, Metric> metricSet;
   private final Meter getRate = new Meter();
   private final Meter cacheHit = new Meter();
   private final Meter restoreRate = new Meter();
   private final Meter acceptedFetchRate = new Meter();
   private final Meter refusedFetchRate = new Meter();


   protected AbstractAsyncCachedDataStore(String name, int priority, String hfsLocation,
         PatternReplace patternReplaceIn, PatternReplace patternReplaceOut, Integer maxPendingRequests, Integer maxRunningRequests,
         long maximumSize, long currentSize, boolean autoEviction, String[] hashAlgorithms)
   {
      LOGGER.info("Initializing {}...", name);
      Objects.requireNonNull(hfsLocation);
      Objects.requireNonNull(name);

      this.name = name;
      this.priority = priority;

      this.patternReplaceIn = patternReplaceIn;
      this.patternReplaceOut = patternReplaceOut;

      this.maxPendingRequests = (maxPendingRequests != null) ? maxPendingRequests : 4;
      LOGGER.info("{} - Setting max pending Order queue size to: {}", name, this.maxPendingRequests);

      this.maxRunningRequests = (maxRunningRequests != null) ? maxRunningRequests : 4;
      LOGGER.info("{} - Setting max running Order queue size to: {}", name, this.maxRunningRequests);

      /* setting the HFS cache's name as this GMPDataStore's name allows the
       * cache to update this GMPDataStore's database entry (currentSize) when
       * products are inserted or evicted on a cache level */
      this.cache = new HfsDataStore(
            this.name,
            new HfsManager(hfsLocation, 10, 1024),
            DataStoreRestriction.NONE,
            0, // unused
            maximumSize,
            currentSize,
            autoEviction,
            hashAlgorithms);

      metricSet = new HashMap<>();
      metricSet.put(MetricRegistry.name(METRIC_PREFIX, getName(), "gets"), getRate);
      metricSet.put(MetricRegistry.name(METRIC_PREFIX, getName(), "cache.hits"), cacheHit);
      metricSet.put(MetricRegistry.name(METRIC_PREFIX, getName(), "restores"), restoreRate);
      metricSet.put(MetricRegistry.name(METRIC_PREFIX, getName(), "cache.size"), (Gauge<Long>)cache::getCurrentSize);
      metricSet.put(MetricRegistry.name(METRIC_PREFIX, getName(), "queue.size"), (Gauge<Integer>)this::queueSize);
      metricSet.put(MetricRegistry.name(METRIC_PREFIX, getName(), "fetches.accepted"), acceptedFetchRate);
      metricSet.put(MetricRegistry.name(METRIC_PREFIX, getName(), "fetches.refused"), refusedFetchRate);
   }

   @Override
   public Order fetch(AsyncProduct to_fetch) throws DataStoreException
   {
      String localIdentifier = to_fetch.getName();
      String uuid = (String) to_fetch.getProperty(ProductConstants.UUID);
      String username = (String) to_fetch.getProperty(ProductConstants.USERNAME); // Hack to satisfy SD-3153, property `username` is set by the FetchLimiter decorator
      Long size = (Long) to_fetch.getProperty(ProductConstants.DATA_SIZE);

      LOGGER.info("fetch request for product {}", localIdentifier);

      Order order;
      synchronized (this.name)
      {
         // return existing order if found
         Order oldOrder = getAndLogExistingOrder(uuid, localIdentifier, size);
         if (oldOrder != null)
         {
            return oldOrder;
         }

         // check max pending requests, throws exception if overquota
         enforceQueueQuota(username, localIdentifier, uuid, size);

         // create order as pending for now
         order = ORDER_SERVICE.createPendingOrder(getName(), uuid);
      }

      logOrderStatusCode(order);
      LOGGER.info("Fetch request from '{}' for product {} ({}) (~{} bytes) successfully submitted",
            username, localIdentifier, uuid, size);

      // monitoring/reporting
      acceptedFetchRate.mark();

      // Immediate fetch only if no pending orders
      if (ORDER_SERVICE.countPendingOrdersByDataStore(getName()) == 1)
      {
         try
         {
            startOrder(order);
         }
         catch (DataStoreException e)
         {
            LOGGER.info("Order is still pending, cannot fetch immediately - Cause: {}", e.getMessage());
         }
      }
      return ORDER_SERVICE.getOrder(order.getOrderId());
   }

   /**
    * Method called by {@link #fetch(AsyncProduct)} after it has applied the Out pattern on the
    * local identifier to product the remote identifier.
    *
    * @param localIdentifier local product identifier
    * @param remoteIdentifier remote product identifier
    * @param uuid product UUID
    * @param size of the product
    * @return a non null Order instance
    *
    * @throws DataStoreException could not perform fetch request
    */
   protected abstract Order internalFetch(String localIdentifier, String remoteIdentifier, String uuid, Long size)
         throws DataStoreException;

   @Override
   public Map<String, Metric> getMetrics()
   {
      return metricSet;
   }

   @Override
   public String getName()
   {
      return this.name;
   }

   @Override
   public int getPriority()
   {
      return priority;
   }

   @Override
   public boolean hasProduct(String uuid)
   {
      return cache.hasProduct(uuid);
   }

   @Override
   public boolean hasAsyncProduct(String uuid)
   {
      return Boolean.TRUE;
   }

   @Override
   public Product get(String id) throws DataStoreException
   {
      getRate.mark();
      // If is in cache ...
      if (this.cache.hasProduct(id))
      {
         cacheHit.mark();
         LOGGER.debug("get of cached product {}", id);
         return cache.get(id);
      }
      else
      {
         fr.gael.dhus.database.object.Product prod = PRODUCT_SERVICE.getProduct(id);
         if (prod == null)
         {
            throw new ProductNotFoundException();
         }

         LOGGER.debug("Get of async product {} ({})", id, prod.getIdentifier());

         AsyncProduct res = new AsyncProduct(this);
         res.setName(prod.getIdentifier());
         res.setProperty(ProductConstants.DATA_SIZE, prod.getSize());
         res.setProperty(ProductConstants.UUID, prod.getUuid());

         return res;
      }
   }

   @Override
   public void deleteProduct(String uuid) throws DataStoreException
   {
      cache.deleteProduct(uuid);
   }

   @Override
   public boolean addProductReference(String id, Product product) throws DataStoreException
   {
      return false;
   }

   @Override
   public List<String> getProductList()
   {
      return cache.getProductList();
   }

   @Override
   public List<String> getProductList(int skip, int top)
   {
      return cache.getProductList(skip, top);
   }

   /**
    * Move product to cache and restore it by updating the online, checksum and content length fields in database
    *
    * @param product a non null instance of product
    * @param uuid UUID of moved product
    * @throws DataStoreException could not move the given product to cache
    */
   protected void moveProductToCache(Product product, String uuid) throws DataStoreException
   {
      LOGGER.info("Moving product {} ({}) to cache", product.getName(), uuid);

      try
      {
         try
         {
            cache.set(uuid, product);
         }
         catch (ProductAlreadyExist ex)
         {
            // do not delete the product if it already exists
            throw ex;
         }
         catch (DataStoreException | RuntimeException ex)
         {
            cache.deleteProduct(uuid);
            throw ex;
         }

         // Set online, checksum and content length in database
         long size = (Long) product.getProperty(ProductConstants.DATA_SIZE);
         PRODUCT_SERVICE.restoreProduct(uuid, (Long) product.getProperty(ProductConstants.DATA_SIZE), ProductConstants.getChecksums(product));
         LOGGER.info("Product {} ({}) ({} bytes) successfully restored", product.getName(), uuid, size);
         restoreRate.mark();
      }
      catch (DataStoreException ex)
      {
         LOGGER.error("Cannot move product {} ({}) to cache", product.getName(), uuid, ex);
         LOGGER.error("Product {} ({}) could not be restored", product.getName(), uuid);
         throw ex;
      }
   }

   /**
    * Creates the Id to uniquely identify an Order element.
    *
    * @param uuid no null product UUID
    * @return a String to identify an Order
    */
   public final String makeOrderId(String uuid)
   {
      return getName().concat("-").concat(uuid);
   }

   @Override
   public List<Order> getOrderList()
   {
      return ORDER_SERVICE.getOrdersByDataStore(getName());
   }

   @Override
   public Order getOrder(String uuid)
   {
      return ORDER_SERVICE.getOrderByProductUuid(uuid);
   }

   /**
    * Refresh a product Order in the database.
    *
    * @see OrderService#refreshOrCreateOrder(String, String, String, org.dhus.api.JobStatus, java.util.Date, String)
    */
   protected void refreshOrder(String productUUID, String dataStoreName, String jobId, JobStatus jobStatus, Date estimatedDate, String statusMessage)
   {
      ORDER_SERVICE.refreshOrCreateOrder(productUUID, dataStoreName, jobId, jobStatus, estimatedDate, statusMessage);
   }

   protected synchronized void startOrder(Order pendingOrder) throws DataStoreException
   {
      Order order = ORDER_SERVICE.getOrder(pendingOrder.getOrderId());
      if (order.getStatus() != JobStatus.PENDING)
      {
         return;
      }
      if (maxRunning())
      {
         // max running requests reached
         // do not submit any more orders to the remote
         throw new DataStoreException("Maximum number of running order reached for this datastore: " + getName());
      }

      // get database product for required information
      fr.gael.dhus.database.object.Product orderedProduct = PRODUCT_SERVICE.systemGetProduct(pendingOrder.getOrderId().getProductUuid());
      if (orderedProduct == null)
      {
         LOGGER.warn("{} - Skipping Order '{}', product not found in database", getName(), pendingOrder.getOrderId());
         refreshOrder(pendingOrder.getOrderId().getProductUuid(), getName(), pendingOrder.getJobId(), JobStatus.FAILED, null, "Product not found");
         return;
      }

      // perform remote fetch request and refresh order
      try
      {
         // delegate to concrete implementation
         Order runningOrder = internalFetch(
               orderedProduct.getIdentifier(), // DHuS-side identifier
               doPatternReplaceOut(orderedProduct.getIdentifier()), // LTA-side identifier
               orderedProduct.getUuid(),
               orderedProduct.getSize());

         ORDER_SERVICE.setOrderRunning(orderedProduct.getUuid(), runningOrder.getJobId(), runningOrder.getEstimatedTime(), runningOrder.getStatusMessage());
      }
      catch (DataStoreException ex)
      {
         LOGGER.error("Cannot fetch async product '{}' from datastore {}, reason: {}",
               orderedProduct.getUuid(), getName(), ex.getMessage());
         refreshOrder(orderedProduct.getUuid(), getName(), pendingOrder.getJobId(), JobStatus.FAILED, null, ex.getMessage());
      }
   }

   /**
    * A task to ingest successfully fetched data.
    */
   protected abstract class IngestTask extends TimerTask
   {
      /**
       * Get the UUID from the given local product identifier.
       *
       * @param productLocalIdentifier a non null product identifier
       * @return its UUID or null if no product could be found from the given identifier
       */
      protected String getProductUUID(String productLocalIdentifier)
      {
         fr.gael.dhus.database.object.Product product = PRODUCT_SERVICE.getProducBytIdentifier(productLocalIdentifier);
         return product != null ? product.getUuid() : null;
      }

      /**
       * Does the given product exist in the cache backing this async dataStore?
       *
       * @param productUUID a non null product UUID
       * @return true if the product physically exists in the HFS cache
       */
      protected boolean existsInCache(String productUUID)
      {
         return cache.hasProduct(productUUID);
      }

      @Override
      public void run()
      {
         LOGGER.debug("{} - Starting order management routine...", getName());
         LOGGER.debug("{} - Checking pending orders", getName());
         List<Order> pendingOrders = ORDER_SERVICE.getPendingOrdersByDataStore(getName());
         for (Order pendingOrder: pendingOrders)
         {
            try
            {
               startOrder (pendingOrder);
            }
            catch (DataStoreException ex)
            {
               LOGGER.debug("{} - Maximum number of running order reached", getName());
               break;
            }
         }

         LOGGER.debug("{} - Retrieving completed orders", getName());
         int ingested;
         do
         {
            ingested = ingestCompletedFetches();
         }
         while (ingested > 0);
         LOGGER.debug("{} - Finished order management routine.", getName());
      }

      /**
       * Called by {@link #run()}.
       * This method will be invoked by the caller in loop as long as the returned value is > 0.
       *
       * @return the number of products successfully restored by this call
       */
      protected abstract int ingestCompletedFetches();
   }

   public final String doPatternReplaceIn(String productName)
   {
      String resultName = productName;
      if (patternReplaceIn != null)
      {
         resultName = productName.replaceAll(patternReplaceIn.getPattern(), patternReplaceIn.getReplacement());
      }
      return resultName;
   }

   public final String doPatternReplaceOut(String productName)
   {
      String resultName = productName;
      if (patternReplaceOut != null)
      {
         resultName = productName.replaceAll(patternReplaceOut.getPattern(), patternReplaceOut.getReplacement());
      }
      return resultName;
   }

   protected void logOrderStatusCode(Order order)
   {
      JobStatus status = order.getStatus();
      switch (status)
      {
         case PENDING:
            LOGGER.info("Requested product is pending");
            break;
         case COMPLETED:
            LOGGER.info("Requested product is available");
            break;
         case RUNNING:
            LOGGER.info("Requested product is under processing, try again later");
            break;
         default:
            LOGGER.error("Product retrieval has failed");
            break;
      }
   }

   protected void enforceQueueQuota(String username, String localIdentifier, String uuid, Long size) throws AsyncDataStoreException
   {
      if (maxPending())
      {
         LOGGER.info("Fetch request from '{}' for product {} ({}) (~{} bytes) failed due to max active order per instance reached",
               username, localIdentifier, uuid, size);

         refusedFetchRate.mark();

         throw new AsyncDataStoreException(
               "Maximum concurrent requests exceeded for this data store: " + getName(),
               "The retrieval of offline data is temporary unavailable, please try again later",
               503);
      }
   }

   private boolean maxRunning()
   {
      return ORDER_SERVICE.countRunningOrdersByDataStore(getName()) >= maxRunningRequests;
   }

   private boolean maxPending()
   {
      return ORDER_SERVICE.countPendingOrdersByDataStore(getName()) >= maxPendingRequests;
   }

   private int queueSize()
   {
      return ORDER_SERVICE.countRunningOrdersByDataStore(getName());
   }

   /**
    * Returns existing order and logs its existence/presence in the queue.
    * Required by operations.
    */
   @Override
   public Order getAndLogExistingOrder(String uuid, String localIdentifier, Long size)
   {
      Order order = ORDER_SERVICE.getOrderByProductUuid(uuid);
      if (order != null)
      {
         LOGGER.info("Order for product '{}' already exists", uuid);
         User user = SECURITY_SERVICE.getCurrentUser();
         LOGGER.info("Fetch request from '{}' for product {} ({}) (~{} bytes) already in queue", user.getUsername(), localIdentifier, uuid, size);
         logOrderStatusCode(order);
      }
      return order;
   }

    // vvvv Not implemented because this DS is read-only vvvv

   @Override
   public boolean canAccess(String resource_location)
   {
      // Is read only, should not be target of synchronisers
      return false;
   }

   @Override
   public void set(String id, Product product) throws DataStoreException
   {
      throw new ReadOnlyDataStoreException("The DataStore is read-only");
   }

   @Override
   public void addProduct(IngestibleProduct inProduct) throws StoreException
   {
      throw new ReadOnlyDataStoreException("The DataStore is read-only");
   }

   @Override
   public boolean canHandleDerivedProducts()
   {
      return false;
   }

   @Override
   public boolean canModifyReferences()
   {
      return false;
   }

   @Override
   public boolean hasKeyStore()
   {
      return cache.hasKeyStore();
   }
}
