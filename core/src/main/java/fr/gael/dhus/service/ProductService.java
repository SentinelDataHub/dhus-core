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
package fr.gael.dhus.service;

import fr.gael.dhus.database.dao.KeyStoreEntryDao;
import fr.gael.dhus.database.dao.ProductDao;
import fr.gael.dhus.database.dao.UserDao;
import fr.gael.dhus.database.object.Collection;
import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.datastore.Destination;
import fr.gael.dhus.datastore.Ingester;
import fr.gael.dhus.datastore.exception.DataStoreAlreadyExistException;
import fr.gael.dhus.datastore.exception.DataStoreException;
import fr.gael.dhus.datastore.exception.DataStoreLocalArchiveNotExistingException;
import fr.gael.dhus.datastore.processing.fair.FairCallable;
import fr.gael.dhus.datastore.processing.fair.FairThreadPoolTaskExecutor;
import fr.gael.dhus.datastore.scanner.AsynchronousLinkedList;
import fr.gael.dhus.datastore.scanner.AsynchronousLinkedList.Event;
import fr.gael.dhus.datastore.scanner.AsynchronousLinkedList.Listener;
import fr.gael.dhus.datastore.scanner.FileScannerWrapper;
import fr.gael.dhus.datastore.scanner.Scanner;
import fr.gael.dhus.datastore.scanner.ScannerFactory;
import fr.gael.dhus.datastore.scanner.URLExt;
import fr.gael.dhus.olingo.v1.visitor.ProductSQLVisitor;
import fr.gael.dhus.search.SolrDao;
import fr.gael.dhus.spring.cache.AddProduct;
import fr.gael.dhus.spring.cache.RemoveProduct;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;

import fr.gael.drbx.cortex.DrbCortexItemClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import org.apache.commons.io.IOUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.solr.client.solrj.SolrServerException;

import org.dhus.ProductFactory;
import org.dhus.store.datastore.DataStoreService;

import org.hibernate.Hibernate;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product Service provides connected clients with a set of method
 * to interact with it.
 */
@Service
public class ProductService extends WebService
{
   private static final Logger LOGGER = LogManager.getLogger(ProductService.class);

   private static final long DAY_MILLI = 24 * 60 * 60 * 1_000;

   @Autowired
   private ProductDao productDao;

   @Autowired
   private CollectionService collectionService;

   @Autowired
   private UserDao userDao;

   @Autowired
   private FairThreadPoolTaskExecutor taskExecutor;

   @Autowired
   private ScannerFactory scannerFactory;

   /** Configuration (etc/dhus.xml). */
   @Autowired
   private ConfigurationManager cfgManager;

   @Autowired
   private SolrDao solrDao;

   @Autowired
   private KeyStoreEntryDao keyStoreEntryDao;

   @Autowired
   private DataStoreService dataStoreService;

   @Autowired
   private DeletedProductService deletedProductService;

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public Iterator<Product> systemGetProducts(String filter, Long collection_id, int skip)
   {
      return productDao.scrollFiltered(filter, collection_id, skip);
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   @Cacheable (value = "product", key = "#id")
   public Product systemGetProduct (Long id)
   {
      return productDao.read (id);
   }

   @PreAuthorize ("hasAnyRole('ROLE_DATA_MANAGER','ROLE_SEARCH')")
   @Cacheable (value = "product", key = "#id")
   public Product getProduct (Long id)
   {
      return systemGetProduct (id);
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public Product getProductNoCache (Long id)
   {
      return productDao.read (id);
   }

   @PreAuthorize ("hasAnyRole('ROLE_DATA_MANAGER','ROLE_SEARCH')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   @Cacheable (value = "products", key = "#ids")
   public List<Product> getProducts (List<Long> ids)
   {
      return productDao.read(ids);
   }


   /**
    * Gets a {@link Product} by its {@code UUID} (Protected).
    * @see #getProduct(java.lang.String)
    * @param uuid UUID unique identifier
    * @return a {@link Product} or {@code null}
    */
   @PreAuthorize ("hasAnyRole('ROLE_DATA_MANAGER','ROLE_SEARCH')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   @Cacheable (value = "product", key = "#uuid")
   public Product getProduct (String uuid)
   {
      return systemGetProduct (uuid);
   }

   /**
    * Gets a {@link Product} by its {@code UUID} (Unprotected).
    * @param uuid UUID unique identifier
    * @return a {@link Product} or {@code null}
    */
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   @Cacheable (value = "product", key = "#uuid")
   public Product systemGetProduct (String uuid)
   {
      return productDao.getProductByUuid (uuid);
   }

   @PreAuthorize ("hasRole('ROLE_DOWNLOAD')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   @Cacheable (value = "product", key = "#id")
   public Product getProductToDownload (Long id)
   {
      // TODO remove method cause duplicated and not used
      return productDao.read (id);
   }

   @PreAuthorize ("hasAnyRole('ROLE_DOWNLOAD','ROLE_SEARCH')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public InputStream getProductQuickLook (Long id)
   {
      // TODO remove method cause not used
      Product product = getProduct (id);
      if (!product.getQuicklookFlag ()) return null;
      
      try
      {
         return new FileInputStream (product.getQuicklookPath ());
      }
      catch (Exception e)
      {
         LOGGER.warn ("Cannot retrieve Quicklook from product id #" + id,e);
      }
      return null;
   }

   @PreAuthorize ("hasAnyRole('ROLE_DOWNLOAD','ROLE_SEARCH')")
   public long getProductQuickLookContentLength (Long id)
   {
      // TODO remove method cause not used
      return getProduct (id).getQuicklookSize ();
   }

   @PreAuthorize ("hasAnyRole('ROLE_DOWNLOAD','ROLE_SEARCH')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public InputStream getProductThumbnail (Long id)
   {
      // TODO remove method cause not used
      Product product = getProduct (id);
      if (!product.getThumbnailFlag ()) return null;
      try
      {
         return new FileInputStream (product.getThumbnailPath ());
      }
      catch (Exception e)
      {
         LOGGER.warn ("Cannot retrieve Thumbnail from product id #" + id,e);
      }
      return null;
   }


   @PreAuthorize ("hasAnyRole('ROLE_DOWNLOAD','ROLE_SEARCH')")
   public long getProductThumbnailContentLength (Long id)
   {
      // TODO remove method cause not used
      return getProduct (id).getThumbnailSize ();
   }

   /**
    * Returns the number of product belonging to the given Collection.
    * <p><b>This method requires roles ROLE_DATA_MANAGER | ROLE_SEARCH.</b>
    * @param filter an optionnal `where` clause (without the "where" token).
    * @param collection_uuid the `Id` of the parent collection.
    * @return number of Products.
    */
   @PreAuthorize ("hasAnyRole('ROLE_DATA_MANAGER','ROLE_SEARCH')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   @Cacheable (value = "product_count", key = "{#filter, #collection_uuid}")
   public Integer count(String filter, String collection_uuid)
   {
      return productDao.count(filter, collection_uuid);
   }

   @PreAuthorize ("hasAnyRole('ROLE_DATA_MANAGER','ROLE_SEARCH')")
   @Transactional (readOnly = true, propagation = Propagation.REQUIRED)
   @Cacheable (value = "product_count", key = "{#filter, null}")
   public Integer count(String filter)
   {
      return productDao.count(filter, null);
   }

   /**
    * Deletes references and binaries of product.
    * @param pid product ID.
    */
   @RemoveProduct
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   public Product systemDeleteProduct(Long pid, boolean storeAsDeleted, String deletionCause)
   {
      Product product = productDao.read (pid);

      if (product == null)
      {
         throw new DataStoreException ("Product #" + pid +
            " not found in the system.");
      }

      return systemDeleteProduct(product, Destination.TRASH, storeAsDeleted, deletionCause);
   }

   /**
    * Deletes a product.
    * @param product product to delete.
    * @param destination destination of product backup.
    */
   @Transactional
   public Product systemDeleteProduct(Product product, Destination destination,
         boolean storeAsDeleted, String deletionCause)
   {
      LOGGER.debug("System delete of product {} to destination {} with cause {} and storeAsDeleted={}",
            product, destination, deletionCause, storeAsDeleted);
      if (product == null)
      {
         throw new IllegalArgumentException ("Product should not be null");
      }

      if (product.getLocked ())
      {
         throw new DataStoreException ("Cannot delete product : " + product +
               ". This product is locked by the system");
      }

      long start = System.currentTimeMillis ();
      // remove from Solr index
      try
      {
         solrDao.remove(product.getId());
      }
      catch (NullPointerException | IllegalStateException e)
      {
         LOGGER.warn("Solr not running!", e);
      }
      catch (SolrServerException | IOException e)
      {
         LOGGER.warn("Product '{}' not found in Solr", product.getIdentifier(), e);
      }

      // delete associated sub-products
      if (product.getQuicklookFlag())
      {
         LOGGER.info("Deletion of QL: {}", product.getQuicklookPath());
         try
         {
            dataStoreService.delete(product.getQuicklookPath());
         }
         catch (org.dhus.store.datastore.DataStoreException e)
         {
            LOGGER.warn("Deletion of QL {} failed, remove it manually", product.getQuicklookPath(), e);
         }

      }
      if (product.getThumbnailFlag())
      {
         LOGGER.info("Deletion of TN: {}", product.getThumbnailPath());
         try
         {
            dataStoreService.delete(product.getThumbnailPath());
         }
         catch (org.dhus.store.datastore.DataStoreException e)
         {
            LOGGER.warn("Deletion of TN {} failed, remove it manually", product.getThumbnailPath(), e);
         }
      }

      // make product backup if necessary
      if (!destination.equals(Destination.NONE))
      {
         makeProductBackup(product, destination);
      }

      // delete product data
      LOGGER.debug("Trying to delete product {}", product.getUuid());
      try
      {
         dataStoreService.delete(product.getUuid());
      }
      catch (org.dhus.store.datastore.DataStoreException e)
      {
         LOGGER.warn("Deletion of {} failed, remove it manually", product.getUuid(), e);
      }

      // store product if req
      if (storeAsDeleted)
      {
         deletedProductService.storeProduct(product, deletionCause);
      }

      // remove from database
      productDao.delete(product);
      long time = System.currentTimeMillis() - start;

      LOGGER.info("Deletion of product '{}' ({} bytes) successful spent {}ms",
            product.getIdentifier(), product.getDownloadableSize(), time);
      return product;
   }

   // TODO delegate this process to DataStoreManager or DataStores
   private void makeProductBackup(Product product, Destination destination)
   {
      org.dhus.Product product_data = null;
      if (dataStoreService.exists(product.getUuid()))
      {
         try
         {
            LOGGER.debug("Product exists: {}", product.getUuid());
            product_data = dataStoreService.get(product.getUuid());
         }
         catch (org.dhus.store.datastore.DataStoreException e)
         {
            LOGGER.warn("Cannot physically retrieve product {}", product.getUuid(), e);
         }
      }
      else
      {
         LOGGER.debug("Product to generate: {}", product.getUuid());
         product_data = ProductFactory.generateProduct(product.getPath());
         LOGGER.debug("Product generated: {}", product.getUuid());
      }
      Path destination_path = null;
      if(product_data != null)
      {
         switch (destination)
         {
            case ERROR:
               String error_path =
                     cfgManager.getArchiveConfiguration().getIncomingConfiguration().getErrorPath();
               if (error_path != null && !error_path.isEmpty())
               {
                  destination_path = Paths.get(error_path, product_data.getName());
               }
               break;
            case TRASH:
               String trash_path =
                     cfgManager.getArchiveConfiguration().getEvictionConfiguration().getTrashPath();
               if (trash_path != null && !trash_path.isEmpty())
               {
                  destination_path = Paths.get(trash_path, product_data.getName());
               }
               break;
            default:
               return;
         }
      }
      if (destination_path != null)
      {
         try
         {
            // generate directory if does not exists
            Path directory = destination_path.getParent();
            if (Files.notExists(directory))
            {
               Files.createDirectory(directory);
            }

            if (product_data.hasImpl(File.class))
            {
               // copy file to its destination
               Files.copy(product_data.getImpl(File.class).toPath(), destination_path);
            }
            else
            {
               // copy file to its destination
               InputStream input = product_data.getImpl(InputStream.class);
               File destination_file = destination_path.toFile();
               if (destination_file.createNewFile())
               {
                  IOUtils.copy(input, new FileWriter(destination_file));
               }
            }
         }
         catch (IOException | RuntimeException e)
         {
            LOGGER.warn("Cannot save product {} to {} directory.",
                  product.getIdentifier(), destination_path.getParent());
         }
      }
   }

   @PreAuthorize ("hasRole('ROLE_DATA_MANAGER')")
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   @RemoveProduct
   public Product deleteProduct(Long pid, boolean storeAsDeleted, String deletionCause)
   {
      return systemDeleteProduct(pid, storeAsDeleted, deletionCause);
   }

   @PreAuthorize ("isAuthenticated ()")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   @Cacheable (value="product", key="#uuid")
   public Product getProduct (String uuid, User u)
   {
      return productDao.getProductByUuid (uuid);
   }
   
   @PreAuthorize ("isAuthenticated ()")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   @Cacheable (value="product_count", key="'all'")
   public int count ()
   {
      DetachedCriteria criteria = DetachedCriteria.forClass(Product.class);
      criteria.add(Restrictions.eq("processed", true));
      return productDao.count(criteria);
   }

   public boolean hasAccessToProduct (long user_id, long product_id)
   {
      return true;
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   @Cacheable (value = {"indexes"}, key = "#product_id")
   public List<MetadataIndex> getIndexes(Long product_id)
   {
      Product product = productDao.read (product_id);
      if (product == null) return new ArrayList<MetadataIndex> ();
      Hibernate.initialize (product.getIndexes ());
      return product.getIndexes ();
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public Product getProductWithIndexes(Long product_id)
   {
      Product product = productDao.read (product_id);
      if (product == null) return null;
      Hibernate.initialize (product.getIndexes ());
      return product;
   }

   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   @CacheEvict (value = {"indexes"}, key = "#product_id")
   public void setIndexes(Long product_id, List<MetadataIndex>indexes)
   {
      Product product = productDao.read (product_id);
      product.setIndexes (indexes);
      productDao.update (product);
   }

   /**
    * Adds a product in the database, the given product will not be queued for
    * processing nor it will be submitted to the search engine.
    * @param product a product to store in the database.
    * @return the created product.
    */
   @AddProduct
   @Transactional (readOnly = false, propagation = Propagation.REQUIRED)
   public Product addProduct(Product product)
   {
      Product final_product = this.productDao.create (product);
      return final_product;
   }

   @Transactional
   public Product addProduct (URL path, User owner, String origin)
      throws DataStoreAlreadyExistException
   {
      if (productDao.exists (path))
      {
         throw new DataStoreAlreadyExistException ("Product \"" +
            path.toExternalForm () + "\" already present in the system.");
      }

      /* **** CRITICAL SECTION *** */
      /** THIS SECTION SHALL NEVER BE STOPPED BY CNTRL-C OR OTHER SIGNALS */
      /* TODO: check if shutdownHook can protect this section */
      Product product = new Product ();
      product.setPath (path);
      product.setOrigin (origin);
      List<User> users = new ArrayList<User> ();
      
      if (owner != null)
      {
         product.setOwner (owner);
         users.add (userDao.read (owner.getUUID ()));
         product.setAuthorizedUsers (new HashSet<User> (users));
      }

      product = productDao.create (product);
      return product;
   }

   /**
    * Process given unprocessed product.
    * @param product to process.
    * @param owner user owning that product.
    * @param collections containing that product.
    * @param scanner
    * @param wrapper
    * @return A future to get notified for the end of the processing.
    *         {@code get()} will return null, see {@link ProcessingCallable#call()}.
    *         May return {@code null} if a RejectedExecutionException has been thrown.
    */
   public Future<Object> processProduct(Product product, User owner,
      List<Collection>collections, Scanner scanner,
      FileScannerWrapper wrapper)
   {
      Future<Object> future = null;
      int retry = 10;
      while (retry > 0)
      {
         try
         {
            ProcessingCallable pr = new ProcessingCallable(product, owner,
                  collections, scanner, wrapper);
            future = taskExecutor.submit(pr);
            retry = 0;
         }
         catch (RejectedExecutionException ree)
         {
            retry--;
            if (retry <= 0) throw ree;
            try
            {
               Thread.sleep (500);
            }
            catch (InterruptedException e)
            {
               LOGGER.warn ("Current thread has interrupted by another!", e);
            }
         }
      }
      return future;
   }

   /**
    * OData dedicated Services
    *
    * @param visitor OData expression visitor
    * @param uuid UUID of owning collection
    * @param skip $skip parameter
    * @param top $top parameter
    * @return List of results
    */
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   @Cacheable (value="products", key="{#visitor.getHqlQuery(), #visitor.getHqlParameters(), #uuid, #skip, #top}")
   public List<Product> getProducts (ProductSQLVisitor visitor, String uuid, int skip, int top)
   {
      String filter = getProcessedClauseAndInnerCollection(visitor.getHqlFilter(), uuid);
      StringBuilder sb = new StringBuilder(visitor.getHqlPrefix());
      sb.append("WHERE ").append(filter);

      String order = visitor.getHqlOrder();
      if (order != null)
      {
         sb.append(" ORDER BY ").append(order);
      }

      return productDao.executeHQLQuery(sb.toString(), visitor.getHqlParameters(), skip, top);
   }

   /**
    * OData dedicated Services.
    *
    * @param visitor OData expression visitor
    * @param uuid UUID of owning collection
    * @return count of results
    */
   @Transactional(readOnly = true)
   @Cacheable(value = "product_count", key = "{#visitor, #uuid}")
   public int countProducts(ProductSQLVisitor visitor, String uuid)
   {
      String filter = getProcessedClauseAndInnerCollection(visitor.getHqlFilter(), uuid);
      StringBuilder sb = new StringBuilder(visitor.getHqlPrefix());
      sb.append("WHERE ").append(filter);
      return productDao.countHQLQuery(sb.toString(), visitor.getHqlParameters());
   }

   /**
    * Adds HQL clause to retrieve only processed product, in the given collection.
    *
    * @param filter initial HQL WHERE condition.
    * @param uuid   collection uuid.
    * @return a string representation of a HQL WHERE condition.
    */
   private String getProcessedClauseAndInnerCollection(final String filter, final String uuid)
   {
      StringBuilder result;
      // retrieve only processed products
      String onlyProcessed = "processed=true";
      if (filter == null)
      {
         result = new StringBuilder(onlyProcessed);
      }
      else
      {
         result = new StringBuilder(filter);
         result.append(" AND ").append(onlyProcessed);
      }

      // retrieve only products contained in collection uuid
      if (uuid != null)
      {
         List<Long> ids = collectionService.getProductIds(uuid);
         if (!ids.isEmpty())
         {
            result.append(" AND id in (");
            Iterator<Long> it = ids.iterator();
            while (it.hasNext())
            {
               result.append(it.next());
               if (it.hasNext())
               {
                  result.append(',');
               }
            }
            result.append(')');
         }
      }

      return result.toString();
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   @Cacheable (value = "product_count", key = "{#filter, #collection?.getUUID ()}")
   public int count (Collection collection, String filter)
   {
      if (collection == null)
      {
         return this.count (filter);
      }
      return this.count(filter, collection.getUUID());
   }

   /**
    * Returns all products not contained in a collection.
    * @return a set of products.
    */
   @Transactional(readOnly = true)
   public Set<Product> getNoCollectionProducts ()
   {
      DetachedCriteria criteria = DetachedCriteria.forClass (Product.class);
      Iterator<Product> it =
            collectionService.getAllProductInCollection ().iterator ();

      HashSet<Long> cpid = new HashSet<> ();
      while (it.hasNext ())
      {
         cpid.add (it.next ().getId ());
      }

      if (!cpid.isEmpty())
      {
         criteria.add(Restrictions.not(Restrictions.in("id", cpid)));
      }
      criteria.add (Restrictions.eq ("processed", true));
      return new HashSet<> (productDao.listCriteria (criteria, 0, -1));
   }

   /**
    * Retrieve products ingested at a given date in the given collection.
    *
    * @param date       ingestion date.
    * @param collection the collection where found products.
    * @return a set of researched products.
    */
   @Transactional(readOnly = true)
   public Set<Product> getProductByIngestionDate (Date date,
         Collection collection)
   {
      Iterator<Product> it;
      Set<Product> productSet = new HashSet<> ();

      if (collection == null)
      {
         it = getNoCollectionProducts ().iterator ();
      }
      else
      {
         it = collectionService.systemGetCollection (
               collection.getUUID ()).getProducts ().iterator ();
      }

      while (it.hasNext ())
      {
         Product p = it.next ();
         if (p != null)
         {
            if (date.getTime () - p.getIngestionDate ().getTime () < DAY_MILLI)
            {
               productSet.add (p);
            }
         }
      }

      return productSet;
   }

   @Transactional(readOnly = true)
   public Product getProductIdentifier (String identifier)
   {
      return productDao.getProductByIdentifier (identifier);
   }


   @Transactional
   public void update (Product product)
   {
      productDao.update (product);
   }


   @Transactional(readOnly = true)
   public Product getProductByDonwloadName(String pname, Collection collection)
   {
      Product p =
            productDao.getProductByDownloadableFilename(pname, collection);
      if (p == null)
      {
         p = productDao.getProductByUuid(keyStoreEntryDao.getKeyBySubValue(pname));
      }
      return p;
   }

   /*
    * Reported from DefaultDataStrore
    */
   private class ProcessingCallable extends FairCallable
   {
      Product product;
      User owner;
      Scanner scanner;
      List<Collection>collections;
      FileScannerWrapper wrapper;
      
      public ProcessingCallable(Product product, User owner,
         List<Collection> collections, Scanner scanner,
         FileScannerWrapper wrapper)
      {
         super (scanner == null ? null : scanner.toString ());
         this.product = product;
         this.owner = owner;
         this.collections = collections;
         this.scanner = scanner;
         this.wrapper = wrapper;
      }

      public Object call() throws Exception
      {
         ApplicationContextProvider.getBean (Ingester.class).ingest (
               product, owner, collections, scanner, wrapper);
         return null;
      }
   }
   
   // Check if product present is the DB is still present into the repository.
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   public void checkDBProducts ()
   {
      LOGGER.info ("Syncing database with repositories...");
      Iterator<Product> products = productDao.getAllProducts ();
      while (products.hasNext ())
      {
         Product product = products.next ();
         if ( !ProductService.checkUrl (product.getPath()))
         {
            LOGGER.info ("Removing Product " + product.getPath () +
               " not found in repository.");
            products.remove ();
         }
         else
            LOGGER.info ("Product " + product.getPath () +
               " found in repository.");
      }
   }

   private static boolean checkUrl (URL url)
   {
      Objects.requireNonNull (url, "`url` parameter must not be null");

      // OData Synchronized product, DELME
      if (url.getPath ().endsWith ("$value"))
      {
         // Ignoring ...
         return true;
      }

      // Case of simple file
      try
      {
         File f = new File (url.toString ());
         if (f.exists ()) return true;
      }
      catch (Exception e)
      {
         LOGGER.debug ("url \"" + url + "\" not formatted as a file");
      }

      // Case of local URL
      try
      {
         URI local = new File (".").toURI ();
         URI uri = local.resolve (url.toURI ());
         File f = new File (uri);
         if (f.exists ()) return true;
      }
      catch (Exception e)
      {
         LOGGER.debug ("url \"" + url + "\" not a local URL");
      }

      // Case of remote URL
      try
      {
         URLConnection con = url.openConnection ();
         con.connect ();
         InputStream is = con.getInputStream ();
         is.close ();
         return true;
      }
      catch (Exception e)
      {
         LOGGER.debug ("url \"" + url + "\" not a remote URL");
      }
      // Unrecovrable case
      return false;
   }
   
   /**
    * Performs directory structure scan to retrieve relevant products, and run
    * declared processing.
    */
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   public int processArchiveSync ()
      throws DataStoreLocalArchiveNotExistingException, InterruptedException
   {
      String archivePath = cfgManager.getArchiveConfiguration ().getPath ();
      final File archive = new File(archivePath);
      if (!archive.exists ())
      {
         throw new DataStoreLocalArchiveNotExistingException (
               "Local archive \"" + archivePath + "\" does not exist.");
      }
      
      LOGGER.info ("Looking for new product in archive \"" +
               archivePath + "\".");

      final List<DrbCortexItemClass> supported =
         scannerFactory.getScannerSupport ();
      
      Scanner scanner =
         scannerFactory.getScanner (archivePath);
      scanner.setSupportedClasses (supported);
      AsynchronousLinkedList<URLExt> list = scanner.getScanList ();
      final Scanner s = scanner;

      list.addListener (new Listener<URLExt> ()
      {
         @Override
         public void addedElement (final Event<URLExt> e)
         {
            try
            {
               URL url = e.getElement ().getUrl ();
               if (getProductByOrigin (url.toString ()) != null ||
                  productDao.exists (url))
               {
                  throw new DataStoreAlreadyExistException (
                     "Already in database");
               }

               LOGGER.info ("Adding product \"" + url + "\".");
               User owner = userDao.getRootUser ();
               Product p = addProduct (url, owner, null);
               processProduct (p, owner, null, s, null);
            }
            catch (DataStoreAlreadyExistException excp)
            {
               LOGGER.info ("Product already in database : \"" +
                  e.getElement ().getUrl ().toString () + "\".");
            }
            catch (DataStoreException excp)
            {
               LOGGER.error ("Cannot add product \"" +
                  e.getElement ().toString () + "\"", excp);
            }
         }

         @Override
         public void removedElement (Event<URLExt> e)
         {
         }
      });
      return scanner.scan ();
   }


   /**
    * Remove unprocessed products
    */
   @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
   @CacheEvict (
      value = { "product_count", "product", "products" },
               allEntries = true )
   public void removeUnprocessed()
   {
      long start = System.currentTimeMillis();

      Iterator<Product> products = getUnprocessedProducts();
      while (products.hasNext())
      {
         products.next();
         products.remove();
      }

      LOGGER.debug("Cleanup incomplete processed products in " + (System.currentTimeMillis() - start) + "ms");
   }

   @Transactional (readOnly = true)
   public Iterator<Product> getUnprocessedProducts ()
   {
      return productDao.getUnprocessedProducts ();
   }

   @Transactional(readOnly = true)
   public Product getProductByOrigin (final String origin)
   {
      DetachedCriteria criteria = DetachedCriteria.forClass (Product.class);
      criteria.add (Restrictions.eq ("origin", origin));
      return productDao.uniqueResult (criteria);
   }

   /**
    * Retrieve products ordered by it date of ingestion, updated.
    * The list of product is returned prior to the passed date argument.
    * Currently processed and locked products are not returned.
    *
    * @param max_date     Maximum date to retrieve products
    *                     More recent product will not be returned
    * @param max_products Maximum products to return
    * @return a scrollable list of the products
    */
   @Transactional(readOnly = true)
   public Iterator<Product> getProductsByIngestionDate(Date max_date, int max_products)
   {
      DetachedCriteria criteria = DetachedCriteria.forClass(Product.class);
      criteria.add(Restrictions.and(
            Restrictions.lt("created", max_date),
            Restrictions.and(
                  Restrictions.eq("processed", true),
                  Restrictions.eq("locked", false))));
      criteria.addOrder(Order.asc("created"));

      return productDao.listCriteria(criteria, 0, max_products).iterator();
   }
}
