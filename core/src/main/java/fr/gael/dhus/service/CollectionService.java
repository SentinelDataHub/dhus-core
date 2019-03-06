/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013-2018 GAEL Systems
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

import fr.gael.dhus.database.dao.CollectionDao;
import fr.gael.dhus.database.dao.ProductDao;
import fr.gael.dhus.database.object.Collection;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.olingo.v1.visitor.CollectionSQLVisitor;
import fr.gael.dhus.service.exception.CollectionNameExistingException;
import fr.gael.dhus.service.exception.RequiredFieldMissingException;
import java.io.IOException;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Collection Service provides connected clients with a set of method to
 * interact with it.
 */
@Service
public class CollectionService extends WebService
{
   private static final Logger LOGGER = LogManager.getLogger(CollectionService.class);

   @Autowired
   private CollectionDao collectionDao;

   @Autowired
   private ProductDao productDao;

   @Autowired
   private SecurityService securityService;

   @Autowired
   private SearchService searchService;

   @PreAuthorize ("hasRole('ROLE_DATA_MANAGER')")
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   public Collection createCollection(Collection collection) throws
         RequiredFieldMissingException, CollectionNameExistingException
   {
      checkRequiredFields(collection);
      checkName(collection);
      return collectionDao.create(collection);
   }

   @PreAuthorize ("hasRole('ROLE_DATA_MANAGER')")
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   public void updateCollection(Collection collection) throws
         RequiredFieldMissingException
   {
      checkRequiredFields(collection);
      Collection c = collectionDao.read (collection.getUUID ());

      String old_name = c.getName();
      String new_name = collection.getName();
      c.setName(new_name);
      c.setDescription (collection.getDescription ());

      collectionDao.update(c);

      if (!new_name.equals(old_name))
      {
         for (Product product: c.getProducts())
         {
            try
            {
               searchService.index(product);
            }
            catch (IOException | SolrServerException | RuntimeException e)
            {
               throw new RuntimeException("Cannot update Solr index", e);
            }
         }
      }
   }

   @PreAuthorize ("hasRole('ROLE_DATA_MANAGER')")
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   public void deleteCollection(String uuid)
   {
      Collection collection = collectionDao.read (uuid);
      LOGGER.info("Removing collection {}", collection.getName());
      Set<Product> products = collection.getProducts();
      Iterator<Product> iterator = products.iterator();
      // delete collection before reindexing its old products.
      collectionDao.delete(collection);
      while (iterator.hasNext())
      {
         try
         {
            Product product = iterator.next();
            iterator.remove();
            searchService.index(product);
         }
         catch (IOException | SolrServerException | RuntimeException e)
         {
            throw new RuntimeException("Cannot update Solr index", e);
         }
      }
   }

   @PreAuthorize ("hasAnyRole('ROLE_DATA_MANAGER','ROLE_SEARCH')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public Collection getCollection (String uuid)
   {
      return systemGetCollection (uuid);
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public List<Collection> getCollections (Product product)
   {
      return collectionDao.getCollectionsOfProduct(product.getId());
   }

   @PreAuthorize ("hasRole('ROLE_DATA_MANAGER')")
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   @CacheEvict (value = "products", allEntries = true)
   public void removeProducts (String uuid, Long[] pids)
   {
      collectionDao.removeProducts (uuid, pids, null);
      long start = new Date ().getTime ();
      for (Long pid: pids)
      {
         try
         {
            searchService.index(productDao.read(pid));
         }
         catch (Exception e)
         {
            throw new RuntimeException("Cannot update Solr index", e);
         }
      }
      long end = new Date ().getTime ();
      LOGGER.info("[SOLR] Remove " + pids.length +
         " product(s) from collection spent " + (end-start) + "ms" );
   }

   @PreAuthorize ("hasRole('ROLE_DATA_MANAGER')")
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   @CacheEvict (value = "products", allEntries = true)
   public void addProducts (String uuid, Long[] pids)
   {
      for (int i = 0; i < pids.length; i++)
      {
         systemAddProduct (uuid, pids[i], true);
      }
   }

   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   @CacheEvict (value = "products", allEntries = true)
   public void systemAddProduct (String uuid, Long pid, boolean followRights)
   {
      Collection collection = collectionDao.read (uuid);
      Product product = productDao.read (pid);

      this.addProductInCollection(collection, product);
      try
      {
         searchService.index(product);
      }
      catch (Exception e)
      {
         throw new RuntimeException("Cannot update Solr index", e);
      }
   }

   @Transactional
   @CacheEvict (value = "products", allEntries = true)
   public void addProductInCollection(Collection collection, Product product)
   {
      if (collection == null)
      {
         LOGGER.error("Cannot add product '{}' in a null collection.", product.getUuid());
         return;
      }
      Collection c = collectionDao.read(collection.getUUID());
      if (!c.getProducts().contains(product))
      {
         c.getProducts().add(product);
         collectionDao.update(c);
      }
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public List<Long> getProductIds(String uuid)
   {
      User user = securityService.getCurrentUser();
      return collectionDao.getProductIds (uuid, user);
   }

   @PreAuthorize ("hasAnyRole('ROLE_DATA_MANAGER','ROLE_SEARCH')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public Integer count ()
   {
      return collectionDao.count();
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   private void checkName (Collection collection)
      throws RequiredFieldMissingException, CollectionNameExistingException
   {
      final String toCheck = collection.getName ();
      if (toCheck == null)
      {
         throw new RequiredFieldMissingException (
            "At least one required field is empty.");
      }

      Iterator<Collection> it = collectionDao.getAllCollections ();
      while (it.hasNext ())
      {
         final String name = it.next ().getName ();
         if (toCheck.equals (name))
         {
            throw new CollectionNameExistingException ("Collection name '" +
                  collection.getName () + "' is already used.");
         }
      }
   }

   private void checkRequiredFields(Collection collection) throws
         RequiredFieldMissingException
   {
      if (collection.getName () == null || collection.getName ().trim ()
            .isEmpty ())
      {
         throw new RequiredFieldMissingException (
               "At least one required field is empty.");
      }
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public Product getProduct (String uuid, String collection_uuid, User u)
   {
      Product p = productDao.getProductByUuid(uuid);
      if (collectionDao.contains(collection_uuid, p.getId()))
         return p;
      return null;
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public List<Product> getProducts(String uuid)
   {
      Collection collection = collectionDao.read (uuid);
      if (collection == null)
      {
         return Collections.emptyList ();
      }

      List<Product> products = new LinkedList<> ();
      Iterator<Long> it = getProductIds (collection.getUUID ()).iterator ();
      while (it.hasNext ())
      {
         Long pid = it.next ();
         if (pid != null)
         {
            Product p = productDao.read (pid);
            if (p != null)
            {
               products.add (p);
            }
         }
      }

      return products;
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public String getCollectionUUIDByName(String collection_name)
   {
      return collectionDao.getCollectionUUIDByName(collection_name);
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public boolean containsProduct (String uuid, Long pid)
   {
      if (uuid == null) return false;
      return collectionDao.contains (uuid, pid);
   }

   @Transactional(readOnly=true)
   public List<Collection> getCollectionsOfProduct(Long id)
   {
      return collectionDao.getCollectionsOfProduct(id);
   }

   @Transactional (readOnly=true)
   public List<Collection> getCollectionsOfProduct(Product p)
   {
      return collectionDao.getCollectionsOfProduct(p.getId());
   }

   /**
    * Retrieves collections higher authorized collection of the given user in
    * function of the given criteria.
    *
    * @param visitor visitor contains filter and order of required collection.
    * @param user
    * @param skip     number of skipped valid results.
    * @param top      max of valid results.
    * @return a list of {@link Collection}
    */
   @Transactional(readOnly = true)
   public List<Collection> getHigherCollections (CollectionSQLVisitor visitor,
         User user, int skip, int top)
   {
      if (user.getRoles().contains(Role.DATA_MANAGER))
      {
         return collectionDao.executeHQLQuery(visitor.getHqlQuery(), visitor.getHqlParameters(), skip, top);
      }
      else
      {
         StringBuilder hql = new StringBuilder(visitor.getHqlPrefix());
         if (visitor.getHqlFilter() != null)
         {
            hql.append("WHERE ").append(visitor.getHqlFilter());
         }
         if (visitor.getHqlOrder() != null)
         {
            hql.append(" ORDER BY ").append(visitor.getHqlOrder());
         }

         return collectionDao.executeHQLQuery(hql.toString(), visitor.getHqlParameters(), skip, top);
      }
   }

   @Transactional(readOnly = true)
   public int countHigherCollections(CollectionSQLVisitor visitor, User user)
   {
      return getHigherCollections(visitor, user, 0, 0).size();
   }

   /**
    * Retrieves all collections.
    *
    * @return all collections
    */
   @Transactional(readOnly = true)
   public Set<Collection> getCollections()
   {
      return new HashSet<>(collectionDao.readAll());
   }

   @PreAuthorize("hasRole('ROLE_DATA_MANAGER')")
   @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
   public List<String> getCollectionsUUID()
   {
      return collectionDao.getCollectionsUUID();
   }

   /**
    * Retrieves a collection by its name.
    *
    * @param name collection name
    * @return the named collection or null
    */
   @Transactional(readOnly = true)
   public Collection getCollectionByName(String name)
   {
      if (name == null)
      {
         return null;
      }
      return collectionDao.getByName(name);
   }

   /**
    * Retrieves all products contained in a collection.
    * @return a set of products.
    */
   @Transactional(readOnly = true)
   public Set<Product> getAllProductInCollection ()
   {
      Set<Product> products = new HashSet<> ();
      Iterator<Collection> it = collectionDao.getAllCollections ();
      while (it.hasNext ())
      {
         products.addAll (it.next ().getProducts ());
      }
      return products;
   }

   public Collection systemGetCollection (String id)
   {
      return collectionDao.read (id);
   }

   public Collection systemGetCollectionByName(String name)
   {
      return collectionDao.getByName(name);
   }
}
