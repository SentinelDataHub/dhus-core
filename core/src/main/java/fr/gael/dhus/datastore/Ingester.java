/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2016,2017 GAEL Systems
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

package fr.gael.dhus.datastore;

import fr.gael.dhus.database.object.Collection;
import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.datastore.processing.ProcessingManager;
import fr.gael.dhus.datastore.scanner.FileScannerWrapper;
import fr.gael.dhus.datastore.scanner.Scanner;
import fr.gael.dhus.service.CollectionService;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.service.SearchService;
import fr.gael.dhus.spring.cache.AddProduct;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.ProductConstants;
import org.dhus.ProductFactory;
import org.dhus.store.datastore.DataStoreService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Allows to perform a product ingestion.
 */
@Component
public class Ingester
{
   private static final Logger LOGGER = LogManager.getLogger(Ingester.class);
   private static final String PROPERTY_INGESTIONDATE = "ingestionDate";

   @Autowired
   private ProductService productService;

   @Autowired
   private DataStoreService dataStoreService;

   @Autowired
   private ProcessingManager processingManager;

   @Autowired
   private CollectionService collectionService;

   @Autowired
   private SearchService searchService;

   @Autowired
   private ConfigurationManager config;

   @AddProduct
   public Product ingest(Product product, User user,
         List<Collection> collections, Scanner scanner,
         FileScannerWrapper wrapper)
   {
      if (scanner != null && scanner.isStopped ())
      {
         if (wrapper != null)
         {
            wrapper.cancelProcess(product);
         }
         productService.systemDeleteProduct(product, Destination.NONE, false, null);
         return null;
      }
      try
      {
         if (wrapper != null)
         {
            wrapper.startIngestion ();
         }

         long processing_start = System.currentTimeMillis();
         processingManager.process (product);
         // ingest physical product
         org.dhus.Product p = ProductFactory.generateProduct(product.getPath());
         LOGGER.info("Store product {} into datastores.", product.getIdentifier());
         if (config.getFileScannersCronConfiguration().isSourceRemove())
         {
            dataStoreService.move(product.getUuid(), p);
         }
         else
         {
            dataStoreService.set(product.getUuid(), p);
         }
         // retrieve download size
         Long size = (Long) p.getProperty(ProductConstants.DATA_SIZE);
         product.setSize(size);
         product.setDownloadableSize(size);
         // retrieve checksum
         product.getDownload().setChecksums(retrieveChecksumProduct (p));

         // cleanup and update product
         product.setPath(null);
         product.setDownloadablePath(null);

         Date ingestion_date = new Date();
         SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
         product.getIndexes().add(
               new MetadataIndex("Ingestion Date", null, "product", PROPERTY_INGESTIONDATE, df.format(ingestion_date)));
         product.setIngestionDate(ingestion_date);
         product.setCreated(ingestion_date);

         product.setUpdated(ingestion_date);
         product.setProcessed(true);
         productService.update(product);

         // add product to Solr
         searchService.index (product);
         if (collections != null)
         {
            for (Collection c : collections)
            {
               collectionService.systemAddProduct (
                     c.getUUID (), product.getId (), true);
            }
         }

         long processing_end = System.currentTimeMillis();
         LOGGER.info ("Ingestion processing complete for product {} ({} bytes, {} bytes compressed) in {}ms.",
               product.getOrigin(), product.getSize(), product.getDownloadableSize(),
               (processing_end - processing_start));

         if (wrapper != null)
         {
            wrapper.endIngestion ();
         }

         return product;
      }
      catch (Throwable excp)
      {
         LOGGER.warn("Unrecoverable error happen during ingestion of {} (removing from database)",
               product.getOrigin(), excp);
         try
         {
            productService.systemDeleteProduct(product, Destination.ERROR, false, null);
         }
         catch (Exception e)
         {
            LOGGER.error (
                  "Unable to remove product after ingestion failure", e);
         }
         if (wrapper != null)
         {
            wrapper.error (product, excp);
         }
         return null;
      }
   }

   /**
    * Retrieves checksum of the given product.
    *
    * @param product the product.
    * @return a map containing checksum of product, or a empty map if no
    * checksum cannot be extracted.
    */
   private Map<String, String> retrieveChecksumProduct(org.dhus.Product product)
   {
      Map<String, String> checksum = new HashMap<>();
      String[] algorithms = config.getDownloadConfiguration().getChecksumAlgorithms().split(",");

      for (String algorithm: algorithms)
      {
         String key = ProductConstants.CHECKSUM_PREFIX + '.' + algorithm;
         if (product.getPropertyNames().contains(key))
         {
            checksum.put(algorithm, (String) product.getProperty(key));
         }
      }
      return checksum;
   }

}
