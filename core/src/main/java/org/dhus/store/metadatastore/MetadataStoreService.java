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
package org.dhus.store.metadatastore;

import fr.gael.dhus.service.OrderService;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.Product;
import org.dhus.store.LoggableProduct;
import org.dhus.store.StoreException;
import org.dhus.store.datastore.DataStores;
import org.dhus.store.datastore.ProductNotFoundException;
import org.dhus.store.ingestion.IngestibleProduct;

import org.springframework.beans.factory.annotation.Autowired;

public class MetadataStoreService implements MetadataStore
{
   private static final Logger LOGGER = LogManager.getLogger(MetadataStoreService.class);

   public static final String METADATA_STORE_SERVICE = "MetadataStoreService";

   @Autowired
   private RelationalMetadataStore relationalMetadataStore;

   @Autowired
   private SolrMetadataStore solrMetadataStore;

   @Autowired
   private OrderService orderService;

   @Override
   public void addProduct(IngestibleProduct inProduct, List<String> targetCollectionNames) throws StoreException
   {
      LOGGER.info("Retrieving metadata from product {}", inProduct.getUuid());
      relationalMetadataStore.addProduct(inProduct, targetCollectionNames);
      // database ID is required in Solr
      // so we need to have the product inserted in the database first
      solrMetadataStore.addProduct(inProduct, targetCollectionNames);
      LOGGER.info("Metadata retrieved from product {}", inProduct.getUuid());
   }

   @Override
   public void addProduct(IngestibleProduct inProduct) throws StoreException
   {
      addProduct(inProduct, Collections.<String>emptyList());
   }

   @Override
   public void repairProduct(IngestibleProduct inProduct) throws StoreException
   {
      relationalMetadataStore.repairProduct(inProduct);
      solrMetadataStore.repairProduct(inProduct);
   }

   public void deleteProduct(String uuid, boolean storeAsDeleted, String cause) throws StoreException
   {
      LOGGER.info("Deleting product {} from MetadataStores", uuid);
      List<Throwable> throwables = new LinkedList<>();

      try
      {
         relationalMetadataStore.deleteProduct(uuid, storeAsDeleted, cause);
         deleteOrder(uuid);
      }
      catch (StoreException | RuntimeException e)
      {
         throwables.add(e);
      }
      try
      {
         solrMetadataStore.deleteProduct(uuid);
      }
      catch (StoreException | RuntimeException e)
      {
         throwables.add(e);
      }
      DataStores.throwErrors(throwables, "deleteProductMetadata", uuid);
   }

   @Override
   public void deleteProduct(String uuid) throws StoreException
   {
      deleteProduct(uuid, false, null);
   }

   @Override
   public boolean hasProduct(String uuid) throws StoreException
   {
      // should we use && instead of || ?
      return relationalMetadataStore.hasProduct(uuid) || solrMetadataStore.hasProduct(uuid);
   }

   @Override
   public Product getProduct(String uuid) throws StoreException
   {
      // TODO
      throw new UnsupportedOperationException();
   }

   public fr.gael.dhus.database.object.Product getDatabaseProduct(String uuid)
   {
      return relationalMetadataStore.getDatabaseProduct(uuid);
   }

   public List<LoggableProduct> getProductUUIDs(String filter, String orderBy, String collectionName, int skip, int top)
         throws StoreException
   {
      return relationalMetadataStore.getProductUUIDs(filter, orderBy, collectionName, skip, top);
   }

   public List<LoggableProduct> getOnlineProductUUIDs(String filter, String orderBy, String collectionName, int skip, int top)
         throws StoreException
   {
      return relationalMetadataStore.getOnlineProductUUIDs(filter, orderBy, collectionName, skip, top);
   }

   public List<LoggableProduct> getProductUUIDsWithin(String filter, String orderBy,
         String collectionName, List<String> productUUIDs) throws StoreException
   {
      return relationalMetadataStore.getProductUUIDsWithin(filter, orderBy, collectionName, productUUIDs);
   }

   public void restoreProduct(String uuid, Long size, Map<String, String> checksums)
         throws ProductNotFoundException
   {
      relationalMetadataStore.restoreProduct(uuid, size, checksums);
   }

   public void setProductOffline(String uuid) throws ProductNotFoundException
   {
      relationalMetadataStore.setProductOffline(uuid);
      deleteOrder(uuid);
   }

   public long getProductSize(String uuid) throws ProductNotFoundException
   {
      return relationalMetadataStore.getProductSize(uuid);
   }

   private void deleteOrder(String uuid)
   {
      orderService.deleteOrderByProductUUID(uuid);
   }
}
