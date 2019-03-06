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

import fr.gael.dhus.database.object.Collection;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.olingo.v1.ODataExpressionParser;
import fr.gael.dhus.olingo.v1.visitor.ProductSQLVisitor;
import fr.gael.dhus.service.CollectionService;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.exception.ODataMessageException;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;

import org.dhus.ProductConstants;
import org.dhus.store.LoggableProduct;
import org.dhus.store.StoreException;
import org.dhus.store.datastore.ProductNotFoundException;
import org.dhus.store.ingestion.IngestibleProduct;
import org.dhus.store.ingestion.MetadataExtractionException;

import org.springframework.beans.factory.annotation.Autowired;

public class RelationalMetadataStore implements MetadataStore
{
   /** Used in database.object.Product getQuicklookFlag and getThumbnailFlag */
   public static final String DEPRECATED_DERIVED_PRODUCT_PATH = "deprecated";

   private static final Logger LOGGER = LogManager.getLogger();

   @Autowired
   private ProductService productService;

   @Autowired
   private CollectionService collectionService;

   public static final String NAME = "RelationalMetadataStore";

   // this method must be "atomic", the product database entry must be complete before being added to the database
   @Override
   public void addProduct(IngestibleProduct inProduct, List<String> targetCollectionNames) throws MetadataExtractionException
   {
      LOGGER.info("Creating database entry for product of UUID {}", inProduct.getUuid());
      Product product = new Product();//productService.addProduct(rawProduct.getURL(), null);
      product.setOrigin(inProduct.getOrigin());

      product.setUuid(inProduct.getUuid());
      product.setIdentifier(inProduct.getIdentifier());
      product.setItemClass(inProduct.getItemClass());

      product.setIngestionDate(inProduct.getIngestionDate());
      // creation date is set by ProductDao
      product.setUpdated(inProduct.getIngestionDate());

      product.setIndexes(inProduct.getMetadataIndexes());
      product.setContentStart(inProduct.getContentStart());
      product.setContentEnd(inProduct.getContentEnd());
      product.setFootPrint(inProduct.getFootprint());

      // public extended properties
      product.setSize((Long) inProduct.getProperty(ProductConstants.DATA_SIZE));

      Product.Download productDownload = new Product.Download();
      productDownload.setType("application/octet-stream");
      productDownload.setChecksums(getChecksums(inProduct));
      product.setDownload(productDownload);

      // derived product attributes
      if (inProduct.getQuicklook() != null)
      {
         product.setQuicklookSize((Long) inProduct.getQuicklook().getProperty(ProductConstants.DATA_SIZE));
      }

      if (inProduct.getThumbnail() != null)
      {
         product.setThumbnailSize((Long) inProduct.getThumbnail().getProperty(ProductConstants.DATA_SIZE));
      }

      // TODO add a condition here when offline product
      // synchronization becomes possible
      product.setOnline(true);

      Product finalProduct = productService.addProduct(product);

      // TODO make product service add product to collections and optimize it
      for (String collectionName: targetCollectionNames)
      {
         LOGGER.info("Adding product of UUID {} to collection '{}'", inProduct.getUuid(), collectionName);
         Collection collection = collectionService.getCollectionByName(collectionName);
         collectionService.addProductInCollection(collection, finalProduct);
      }

      // must be done after the product is created
      inProduct.setProperty(ProductConstants.DATABASE_ID, finalProduct.getId());
      LOGGER.info("Creating database entry successfully created", inProduct.getUuid());
   }

   @Override
   public void addProduct(IngestibleProduct inProduct) throws StoreException
   {
      addProduct(inProduct, Collections.<String>emptyList());
   }

   /**
    * Retrieves checksum of the given product.
    *
    * @param inProduct the product
    * @return a map containing checksum of product, or a empty map if no checksum cannot be extracted
    */
   private static Map<String, String> getChecksums(IngestibleProduct inProduct) // TODO move to utility class
   {
      Map<String, String> checksum = new HashMap<>();
      String[] algorithms = ApplicationContextProvider.getBean(ConfigurationManager.class)
            .getDownloadConfiguration()
            .getChecksumAlgorithms()
            .split(",");

      for (String algorithm: algorithms)
      {
         String checksumKey = ProductConstants.checksum(algorithm);
         String checksumValue = (String) inProduct.getProperty(checksumKey);
         if (checksumValue != null)
         {
            checksum.put(algorithm, checksumValue);
         }
      }
      return checksum;
   }

   public void deleteProduct(String uuid, boolean storeAsDeleted, String cause) throws StoreException
   {
      productService.deleteByUuid(uuid, storeAsDeleted, cause);
   }

   public List<LoggableProduct> getProductUUIDs(String filter, String orderBy, String collectionName, int skip, int top)
         throws StoreException
   {
      return internalGetProductUUIDs(filter, orderBy, collectionName, skip, top);
   }

   public List<LoggableProduct> getOnlineProductUUIDs(String filter, String orderBy, String collectionName, int skip, int top)
         throws StoreException
   {
      String newFilter;
      if (filter == null)
      {
         newFilter = "Online eq true";
      }
      else
      {
         newFilter = filter + " and Online eq true";
      }
      return internalGetProductUUIDs(newFilter, orderBy, collectionName, skip, top);
   }

   public List<LoggableProduct> getProductUUIDsWithin(String filter, String orderBy,
         String collectionName, List<String> dataStoreProductUUIDs) throws StoreException
   {
      if (dataStoreProductUUIDs.isEmpty())
      {
         return Collections.emptyList(); // no products in that datastore
      }

      // add list of products to the filter
      if (filter != null)
      {
         filter += " and";
      }
      else
      {
         filter = "";
      }

      // build uuid list with quotes and commas
      String quotedUuidsWithCommas = dataStoreProductUUIDs.stream()
            .map(uuid -> "Id eq '" + uuid + "'")
            .collect(Collectors.joining(" or "));

      // add list to the filter
      filter += "(" + quotedUuidsWithCommas + ")";

      return internalGetProductUUIDs(filter, orderBy, collectionName, 0, Integer.MAX_VALUE);
   }

   private List<LoggableProduct> internalGetProductUUIDs(String filter, String orderBy, String collectionName, int skip, int top)
         throws StoreException
   {
      ODataExpressionParser productExpressionParser = ODataExpressionParser.getProductExpressionParser();
      try
      {
         FilterExpression filterExpression = null;
         if (filter != null)
         {
            filterExpression = productExpressionParser.parseFilterString(filter);
         }

         OrderByExpression orderByExpression = null;
         if (orderBy != null)
         {
            orderByExpression = productExpressionParser.parseOrderByString(orderBy);
         }

         ProductSQLVisitor visitor = new ProductSQLVisitor(filterExpression, orderByExpression);
         return productService.getProductUUIDs(visitor, collectionName, skip, top);
      }
      catch (ODataApplicationException | ODataMessageException e)
      {
         throw new StoreException("Invalid OData filter: " + filter, e);
      }
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
   public boolean hasProduct(String uuid)
   {
      return productService.systemGetProduct(uuid) != null;
   }

   @Override
   public org.dhus.Product getProduct(String uuid)
   {
      throw new UnsupportedOperationException();
   }

   public void restoreProduct(String uuid, Long size, Map<String, String> checksums) throws ProductNotFoundException
   {
      productService.restoreProduct(uuid, size, checksums);
   }

   public void setProductOffline(String uuid) throws ProductNotFoundException
   {
      productService.setProductOffline(uuid);
   }

   public long getProductSize(String uuid) throws ProductNotFoundException
   {
      long productSize = 0;
      Product product = productService.systemGetProduct(uuid);
      if (product != null)
      {
         productSize = product.getSize();
      }
      else
      {
         throw new ProductNotFoundException();
      }
      return productSize;
   }
}
