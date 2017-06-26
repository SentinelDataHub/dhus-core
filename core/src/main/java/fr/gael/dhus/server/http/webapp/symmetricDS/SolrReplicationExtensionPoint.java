/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016 GAEL Systems
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
package fr.gael.dhus.server.http.webapp.symmetricDS;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.concurrent.Callable;

import fr.gael.dhus.database.object.DataStoreConfiguration;
import fr.gael.dhus.database.object.HfsDataStoreConf;
import fr.gael.dhus.database.object.OpenstackDataStoreConf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.solr.client.solrj.SolrServerException;

import org.dhus.store.datastore.DataStoreFactory;
import org.dhus.store.datastore.DataStoreService;
import org.jumpmind.db.model.Table;
import org.jumpmind.symmetric.io.data.CsvData;
import org.jumpmind.symmetric.io.data.DataContext;
import org.jumpmind.symmetric.io.data.DataEventType;
import org.jumpmind.symmetric.io.data.writer.DatabaseWriterFilterAdapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.datastore.processing.fair.FairThreadPoolTaskExecutor;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.service.SearchService;
import fr.gael.dhus.service.UserService;

@Component
public class SolrReplicationExtensionPoint extends DatabaseWriterFilterAdapter
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final boolean EXIT_ON_FAILURE =
         Boolean.valueOf(System.getProperty("dhus.replication.exitonfailure", "true"));

   @Autowired
   private SearchService searchService;

   @Autowired
   private ProductService productService;

   @Autowired
   private UserService userService;

   @Autowired
   private CacheManager cacheManager;

   @Autowired
   private DataStoreService dataStoreService;

   @Autowired
   private FairThreadPoolTaskExecutor taskExecutor;

   private final LinkedList<Long> newProducts = new LinkedList<>();
   private final LinkedList<Long> productsToIndex = new LinkedList<>();
   private final LinkedList<Long> productsIndexationList = new LinkedList<>();
   private final LinkedList<String> usersToCacheUpdate = new LinkedList<> ();

   private boolean clearProductsCache = false;

   private static String lastTable = "";

   @Override
   public void afterWrite(DataContext context, Table table, CsvData data)
   {
      if (LOGGER.isDebugEnabled () && table.getName () != lastTable)
      {
         LOGGER.debug ("Adding "+table.getName ());
         lastTable = table.getName ();
      }
      if (table.getName ().equalsIgnoreCase ("USERS") &&
         (data.getDataEventType () == DataEventType.INSERT ||
            data.getDataEventType () == DataEventType.UPDATE))
      {
         int idIdx = table.getColumnIndex("UUID");
         String[] rowData = data.getParsedData(CsvData.ROW_DATA);
         String id = rowData[idIdx];
         if (!usersToCacheUpdate.contains(id))
         {
            usersToCacheUpdate.add(id);
         }
         if (data.getDataEventType () == DataEventType.INSERT)
         {
            int loginIdx = table.getColumnIndex("LOGIN");
            LOGGER.info("User '{}' was replicated", rowData[loginIdx]);
         }
      }

      if (table.getName().equalsIgnoreCase("USER_RESTRICTIONS") ||
            table.getName().equalsIgnoreCase("USER_ROLES"))
      {
         int idIdx = table.getColumnIndex("USER_UUID");
         String[] rowData;
         if ((data.getDataEventType() == DataEventType.INSERT ||
               data.getDataEventType() == DataEventType.UPDATE))
         {
            rowData = data.getParsedData(CsvData.ROW_DATA);
         }
         else
         {
            rowData = data.getParsedData(CsvData.OLD_DATA);
         }
         String id = rowData[idIdx];
         if (!usersToCacheUpdate.contains(id))
         {
            usersToCacheUpdate.add(id);
         }
      }

      if (table.getName().equalsIgnoreCase("METADATA_INDEXES") && (
            data.getDataEventType() == DataEventType.INSERT ||
            data.getDataEventType() == DataEventType.UPDATE))
      {
         int idIdx = table.getColumnIndex("PRODUCT_ID");
         String[] rowData = data.getParsedData(CsvData.ROW_DATA);
         Long id = new Long(rowData[idIdx]);
         if (!productsToIndex.contains(id))
         {
            productsToIndex.add(id);
         }
      }

      if (table.getName().equalsIgnoreCase("PRODUCTS") &&
            data.getDataEventType() == DataEventType.UPDATE)
      {
         int idIdx = table.getColumnIndex("ID");
         String[] rowData = data.getParsedData(CsvData.ROW_DATA);
         Long id = new Long(rowData[idIdx]);
         if (!productsToIndex.contains(id))
         {
            productsToIndex.add(id);
         }
         int identifierIdx = table.getColumnIndex("IDENTIFIER");
         LOGGER.info("Product '{}' was replicated", rowData[identifierIdx]);
      }

      if (table.getName().equalsIgnoreCase("PRODUCTS") &&
            data.getDataEventType() == DataEventType.INSERT)
      {
         int idIdx = table.getColumnIndex("ID");
         String[] rowData = data.getParsedData(CsvData.ROW_DATA);
         Long id = new Long(rowData[idIdx]);
         newProducts.add(id);
         int identifierIdx = table.getColumnIndex("IDENTIFIER");
         String identifier = rowData[identifierIdx];
         if (identifier != null)
         {
            LOGGER.info("Product '{}' was replicated", identifier);
         }
      }

      if (table.getName().equalsIgnoreCase("COLLECTION_PRODUCT"))
      {
         int idIdx = table.getColumnIndex("PRODUCTS_ID");
         String[] rowData = data.getDataEventType() == DataEventType.DELETE ?
               data.getParsedData(CsvData.OLD_DATA) : data.getParsedData(CsvData.ROW_DATA);
         Long id = new Long(rowData[idIdx]);
         if (!productsIndexationList.contains(id))
         {
            productsIndexationList.add(id);
         }
      }

      if (table.getName().equalsIgnoreCase("DATASTORE_CONF")
            && data.getDataEventType().equals(DataEventType.INSERT))
      {
         int typeIndex = table.getColumnIndex("TYPE");
         String[] raw = data.getParsedData(CsvData.ROW_DATA);

         DataStoreConfiguration configuration;
         try
         {
            if (raw[typeIndex].equalsIgnoreCase("hfs"))
            {
               HfsDataStoreConf subConf = new HfsDataStoreConf();
               subConf.setPath(raw[table.getColumnIndex("PATH")]);
               subConf.setMaxFileDepth(Integer.valueOf(raw[table.getColumnIndex("MAX_FILE_DEPTH")]));
               configuration = subConf;
            }
            else
            {
               OpenstackDataStoreConf subConf = new OpenstackDataStoreConf();
               subConf.setProvider(raw[table.getColumnIndex("PROVIDER")]);
               subConf.setUrl(new URL(raw[table.getColumnIndex("URL")]));
               subConf.setIdentity(raw[table.getColumnIndex("IDENTITY")]);
               subConf.setCredential(raw[table.getColumnIndex("CREDENTIAL")]);
               subConf.setRegion(raw[table.getColumnIndex("REGION")]);
               subConf.setContainer(raw[table.getColumnIndex("CONTAINER")]);
               configuration = subConf;
            }
            configuration.setId(Long.parseLong(raw[table.getColumnIndex("ID")]));
            configuration.setName(raw[table.getColumnIndex("NAME")]);
            configuration.setReadOnly(Boolean.valueOf(raw[table.getColumnIndex("READ_ONLY")]));
            dataStoreService.add(DataStoreFactory.createDataStore(configuration));
         }
         catch (MalformedURLException e)
         {
            LOGGER.warn("Cannot replicate datastore: {}", raw[table.getColumnIndex("NAME")], e);
         }
      }
   }

   @Override
   public boolean beforeWrite(DataContext context, Table table, CsvData data)
   {
      if ((table.getName().equalsIgnoreCase("PRODUCTS") &&
            data.getDataEventType() == DataEventType.DELETE))
      {
         int idIdx = table.getColumnIndex("ID");
         String[] rowData = data.getParsedData(CsvData.OLD_DATA);
         String id = rowData[idIdx];

         Product product = productService.getProductNoCache (Long.parseLong (id));
         searchService.remove(product);

         Cache cache = cacheManager.getCache ("product");
         if (cache != null)
         {
            synchronized (cache)
            {
               if (cache.get (product.getUuid ()) != null)
               {
                  cache.evict (product.getUuid ());
               }
               if (cache.get (product.getId ()) != null)
               {
                  cache.evict (product.getId ());
               }
            }
         }

         cache = cacheManager.getCache ("product_count");
         if (cache != null)
         {
            synchronized (cache)
            {
               Integer old_value = cache.get ("all", Integer.class);
               // removing all other keys of cache
               cache.clear ();
               if (old_value != null)
               {
                  cache.put ("all", (old_value - 1));
               }
            }
         }

         clearProductsCache = true;
      }

      if ((table.getName().equalsIgnoreCase("USERS") &&
            data.getDataEventType() == DataEventType.DELETE))
      {
         int idIdx = table.getColumnIndex("UUID");
         String[] rowData = data.getParsedData(CsvData.OLD_DATA);
         String uId = rowData[idIdx];

         User user = userService.getUserNoCache (uId);
         if (user != null)
         {
            Cache cache = cacheManager.getCache ("user");
            if (cache != null)
            {
               synchronized (cache)
               {
                  if (cache.get (user.getUUID ()) != null)
                  {
                     cache.evict (user.getUUID ());
                  }
               }
            }
            cache = cacheManager.getCache ("userByName");
            if (cache != null)
            {
               synchronized (cache)
               {
                  if (cache.get (user.getUsername ()) != null)
                  {
                     cache.evict (user.getUsername ());
                  }
               }
            }
         }
      }
      return true;
   }

   @Override
   public void batchCommitted(DataContext context)
   {
      String uId;
      while ((uId = usersToCacheUpdate.poll()) != null)
      {
         User user = userService.getUserNoCache (uId);
         if (user == null)
         {
            continue;
         }

         Cache cache = cacheManager.getCache ("user");
         if (cache != null)
         {
            synchronized (cache)
            {
               if (cache.get (user.getUUID ()) != null)
               {
                  cache.put (user.getUUID (), user);
               }
            }
         }

         cache = cacheManager.getCache ("userByName");
         if (cache != null)
         {
            synchronized (cache)
            {
               if (cache.get (user.getUsername ()) != null)
               {
                  cache.put (user.getUsername (), user);
               }
            }
         }
      }

      if (!newProducts.isEmpty() || !productsIndexationList.isEmpty() ||
            !productsToIndex.isEmpty() || clearProductsCache)
      {
         Cache cache = cacheManager.getCache ("products");
         if (cache != null)
         {
            cache.clear ();
         }
         clearProductsCache = false;
      }

      Long pId;
      while ((pId = productsToIndex.poll()) != null)
      {
         Product product = productService.getProductWithIndexes(pId);
         if (product == null)
         {
            continue;
         }

         // Add products to index them in // thread
         if (!productsIndexationList.contains(pId))
         {
            productsIndexationList.add(pId);
         }

         Cache cache = cacheManager.getCache ("product");
         if (cache != null)
         {
            synchronized (cache)
            {
               if (cache.get (product.getUuid ()) != null)
               {
                  cache.put (product.getUuid (), product);
               }
               if (cache.get (product.getId ()) != null)
               {
                  cache.put (product.getId (), product);
               }
            }
         }
      }

      while ((pId = newProducts.poll()) != null)
      {
         Cache cache = cacheManager.getCache ("product_count");
         if (cache != null)
         {
            synchronized (cache)
            {
               Integer old_value = cache.get ("all", Integer.class);
               // removing all other keys of cache
               cache.clear ();
               if (old_value != null)
               {
                  cache.put ("all", (old_value + 1));
               }
            }
         }
      }

      if (!productsIndexationList.isEmpty ())
      {         
         taskExecutor.submit(new Callable<Void>()
         {
            @Override
            public Void call() throws Exception
            {
               Long pId;
               while ((pId = productsIndexationList.poll()) != null)
               {
                  Product product = productService.getProductWithIndexes(pId);
                  if (product == null)
                  {
                     continue;
                  }
                  try
                  {
                     searchService.index(product);
                  }
                  catch (IOException | SolrServerException e)
                  {
                     replicationFailure(e);
                  }
               }
               return null;
            }
         });
      }
   }

   /**
    * Applies the behaviour when a error occurred during a replication.
    * The behaviour is defined by the property 'dhus.replication.exitonfailure'.
    *
    * @param e the replication exception
    */
   private void replicationFailure(Exception e)
   {
      if (EXIT_ON_FAILURE)
      {
         // shutdown system
         LOGGER.fatal("Replication failure", e);
         System.exit(1);
      }
      LOGGER.error("Replication failure", e);
      throw new RuntimeException(e);
   }

   public void setSearchService(SearchService searchService)
   {
      this.searchService = searchService;
   }
}
