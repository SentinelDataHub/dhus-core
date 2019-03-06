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
package org.dhus.store.metadatastore;

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

   @Override
   public void addProduct(IngestibleProduct inProduct, List<String> targetCollectionNames) throws StoreException
   {
      LOGGER.info("Retrieving metadata from product {}", inProduct.getUuid());
      if (!relationalMetadataStore.isReadOnly())
      {
         relationalMetadataStore.addProduct(inProduct, targetCollectionNames);

         // database ID is required in Solr
         // so we need to have the product inserted in the database first
         if (!solrMetadataStore.isReadOnly())
         {
            solrMetadataStore.addProduct(inProduct, targetCollectionNames);
         }
      }
      LOGGER.info("Metadata retrieved from product {}", inProduct.getUuid());
   }

   @Override
   public void addProduct(IngestibleProduct inProduct) throws StoreException
   {
      addProduct(inProduct, Collections.<String>emptyList());
   }

   public void deleteProduct(String uuid, boolean storeAsDeleted, String cause) throws StoreException
   {
      LOGGER.info("Deleting product {} from MetadataStores", uuid);
      List<Throwable> throwables = new LinkedList<>();

      if (!relationalMetadataStore.isReadOnly())
      {
         try
         {
            relationalMetadataStore.deleteProduct(uuid, storeAsDeleted, cause);
         }
         catch (Exception e)
         {
            throwables.add(e);
         }
      }
      if (!solrMetadataStore.isReadOnly())
      {
         try
         {
            solrMetadataStore.deleteProduct(uuid);
         }
         catch (Exception e)
         {
            throwables.add(e);
         }
      }
      DataStores.throwErrors(throwables, "deleteProductMetadata", uuid);
   }

   @Override
   public void deleteProduct(String uuid) throws StoreException
   {
      deleteProduct(uuid, false, null);
   }

   @Override
   public boolean isReadOnly()
   {
      return false;
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
   }

   public long getProductSize(String uuid) throws ProductNotFoundException
   {
      return relationalMetadataStore.getProductSize(uuid);
   }
}
