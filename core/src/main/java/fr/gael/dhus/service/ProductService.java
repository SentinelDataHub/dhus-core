/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013-2019 GAEL Systems
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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import fr.gael.dhus.database.dao.ProductDao;
import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.datastore.exception.DataStoreException;
import fr.gael.dhus.olingo.v1.visitor.ProductSQLVisitor;
import fr.gael.dhus.spring.cache.AddProduct;
import fr.gael.dhus.spring.cache.RemoveProduct;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.server.api.ODataApplicationException;

import org.dhus.metrics.Utils;
import org.dhus.olingo.v2.visitor.SQLVisitorParameter;
import org.dhus.store.LoggableProduct;
import org.dhus.store.datastore.ProductNotFoundException;

import org.hibernate.Hibernate;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product Service provides connected clients with a set of method
 * to interact with it.
 */
@Service
public class ProductService extends WebService
{
   private static final Logger LOGGER = LogManager.getLogger();

   @Autowired
   private ProductDao productDao;

   @Autowired
   private CollectionService collectionService;

   @Autowired
   private DeletedProductService deletedProductService;

   @Autowired
   private MetricRegistry metricRegistry;

   @Transactional(readOnly = true)
   public Iterator<Product> systemGetProducts(String filter, Long collection_id, int skip)
   {
      return productDao.scrollFiltered(filter, collection_id, skip);
   }

   /**
    * Do not use.
    *
    * @param id database ID
    * @return Product
    * @deprecated use {@link #systemGetProduct(String) instead
    */
   @Transactional(readOnly = true)
   @Deprecated
   public Product systemGetProduct(Long id)
   {
      LOGGER.warn("Call to deprecated method systemGetProduct(long), use systemGetProduct(String uuid) instead");
      return productDao.read(id);
   }

   /**
    * Gets a {@link Product} by its {@code UUID} (Protected).
    *
    * @see #getProduct(java.lang.String)
    * @param uuid UUID unique identifier
    * @return a {@link Product} or {@code null}
    */
   @PreAuthorize("hasAnyRole('ROLE_DATA_MANAGER','ROLE_SEARCH')")
   @Transactional(readOnly = true)
   @Cacheable(value = "product", key = "#uuid")
   public Product getProduct(String uuid)
   {
      return systemGetProduct(uuid);
   }

   @Transactional(readOnly = true)
   public Product getProductWithIndexes(Long product_id)
   {
      Product product = productDao.read(product_id);
      if (product == null)
      {
         return null;
      }
      Hibernate.initialize(product.getIndexes());
      return product;
   }

   /**
    * Gets a {@link Product} by its {@code UUID} (Unprotected).
    * Returns null if that {@link Product} does not exist.
    *
    * @param uuid UUID unique identifier
    * @return a {@link Product} or {@code null}
    */
   @Transactional(readOnly = true)
   public Product systemGetProduct(String uuid)
   {
      return productDao.getProductByUuid(uuid);
   }

   @PreAuthorize("isAuthenticated()")
   @Transactional(readOnly = true)
   @Cacheable(value = "product_count", key = "'all'")
   public int count()
   {
      DetachedCriteria criteria = DetachedCriteria.forClass(Product.class);
      return productDao.count(criteria);
   }

   @Transactional(readOnly = true)
   @Cacheable(value = "product_count", key = "'all'")
   public int unprotectedCount()
   {
      return count();
   }

   @Transactional(readOnly = true)
   @Cacheable(value = {"indexes"}, key = "#uuid")
   public List<MetadataIndex> getIndexes(String uuid)
   {
      Product product = productDao.getProductByUuid(uuid);
      if (product == null)
      {
         return Collections.emptyList();
      }
      Hibernate.initialize(product.getIndexes());
      return product.getIndexes();
   }

   /**
    * Adds a product in the database, the given product will not be queued for
    * processing nor it will be submitted to the search engine.
    *
    * @param product a product to store in the database.
    * @return the created product.
    */
   @AddProduct
   @Transactional
   public Product addProduct(Product product)
   {
      Product final_product = this.productDao.create(product);
      return final_product;
   }

   /**
    * OData dedicated Services
    *
    * @param visitor        OData expression visitor
    * @param collectionUuid UUID of owning collection
    * @param skip           $skip parameter
    * @param top            $top parameter
    * @return List of results
    */
   // FIXME duplicated method, SQLVisitor v1 and v2 should share the same interface
   @Transactional(readOnly = true)
   @Cacheable(value = "products", key = "{#visitor.getHqlQuery(), #visitor.getHqlParameters(), #collectionUuid, #skip, #top}")
   public List<Product> getProducts(ProductSQLVisitor visitor, String collectionUuid, int skip, int top)
   {
      return internalGetProducts(visitor.getHqlPrefix(), visitor.getHqlFilter(), visitor.getHqlOrder(),
            visitor.getHqlParameters(), collectionUuid, skip, top);
   }

   // FIXME duplicated method, SQLVisitor v1 and v2 should share the same interface
   @Transactional(readOnly = true)
   public List<Product> getProducts(org.dhus.olingo.v2.visitor.ProductSQLVisitor visitor, String collectionUuid) throws ODataApplicationException
   {
      return internalGetProducts(visitor.getHqlPrefix(), visitor.getHqlFilter(), visitor.getHqlOrder(),
            visitor.getHqlParameters(), collectionUuid, visitor.getSkip(), visitor.getTop());
   }

   private List<Product> internalGetProducts(String hqlPrefix, String hqlFilter, String hqlOrder,
         List<SQLVisitorParameter> hqlParameters, String collectionUuid, int skip, int top)
   {
      if (top <= 0)
      {
         return Collections.emptyList();
      }

      String hql = prepareHQLQuery(hqlPrefix, hqlFilter, hqlOrder, collectionUuid);
      if (hql == null)
      {
         return Collections.emptyList();
      }

      return productDao.executeHQLQuery(hql, hqlParameters, skip, top);
   }

   /**
    * Returns a list of UUIDs of products that match the ProductSQLVisitor
    * {@code visitor} and belong to the collection of UUID {@code collectionUuid}.
    * Skips the first {@code skip} products, and returns at most {@code top} products.
    *
    * @param visitor        OData expression visitor
    * @param collectionName Name of owning collection
    * @param skip           $skip parameter
    * @param top            $top parameter
    * @return a list of String UUIDs
    */
   @Transactional(readOnly = true)
   public List<LoggableProduct> getProductUUIDs(ProductSQLVisitor visitor, String collectionName, int skip, int top)
   {
      String collectionUUID = collectionService.getCollectionUUIDByName(collectionName);
      // No collection exist for given collection name, return an empty list
      if (collectionName != null && !collectionName.isEmpty() && collectionUUID == null)
      {
         LOGGER.error("No collection with name {} exists", collectionName);
         return Collections.<LoggableProduct>emptyList();
      }
      // Collection is empty, return an empty list (because method getClauseAndInnerCollection does not handle this case)
      if (collectionUUID != null && collectionService.getProductIds(collectionUUID).isEmpty())
      {
         LOGGER.info("No products in the collection {}", collectionName);
         return Collections.<LoggableProduct>emptyList();
      }

      String hql = prepareHQLQuery(visitor.getHqlPrefix(), visitor.getHqlFilter(), visitor.getHqlOrder(), collectionUUID);
      if (hql == null)
      {
         return Collections.emptyList();
      }
      return productDao.getProductUUIDs(hql, visitor.getHqlParameters(), skip, top);
   }

   private String prepareHQLQuery(String hqlPrefix, String hqlFilter, String hqlOrder, String collectionUuid)
   {
      StringBuilder sb = new StringBuilder(hqlPrefix);//visitor.getHqlPrefix());

      if (collectionUuid == null) // normal product navigation
      {
         if (hqlFilter != null)//visitor.getHqlFilter() != null)
         {
            sb.append(" WHERE ").append(hqlFilter);//visitor.getHqlFilter());
         }
      }
      else // navigation from collection to product
      {
         String filter = appendInnerCollection(hqlFilter, collectionUuid);
         if (filter != null)
         {
            sb.append(" WHERE ").append(filter);
         }
         else
         {
            return null; // return null here to terminate and return empty collection
         }
      }

      if (hqlOrder != null)
      {
         sb.append(" ORDER BY ").append(hqlOrder);
      }

      return sb.toString();
   }

   /**
    * OData dedicated Services.
    *
    * @param visitor        OData expression visitor
    * @param collectionUuid UUID of owning collection
    * @return count of results
    */
   // FIXME duplicated method, SQLVisitor v1 and v2 should share the same interface
   @Transactional(readOnly = true)
   @Cacheable(value = "product_count", key = "{#visitor, #collectionUuid}")
   public int countProducts(ProductSQLVisitor visitor, String collectionUuid)
   {
      return internalCountProducts(visitor.getHqlPrefix(), visitor.getHqlFilter(), collectionUuid, visitor.getHqlParameters());
   }

   /**
    * OData dedicated Services.
    *
    * @param visitor        OData expression visitor
    * @param collectionUuid UUID of owning collection
    * @return count of results
    */
   // FIXME duplicated method, SQLVisitor v1 and v2 should share the same interface
   @Transactional(readOnly = true)
   @Cacheable(value = "product_count", key = "{#visitor, #collectionUuid}")
   public int countProducts(org.dhus.olingo.v2.visitor.ProductSQLVisitor visitor, String collectionUuid) throws ODataApplicationException
   {
      return internalCountProducts(visitor.getHqlPrefix(), visitor.getHqlFilter(), collectionUuid, visitor.getHqlParameters());
   }

   private int internalCountProducts(String hqlPrefix, String hqlFilter, String collectionUuid, List<SQLVisitorParameter> hqlParameters)
   {
      StringBuilder sb = new StringBuilder(hqlPrefix);

      if (null == collectionUuid)  // normal product navigation
      {
         if (hqlFilter != null)
         {
            sb.append(" WHERE ").append(hqlFilter);
         }
      }
      else // navigation from collection to product
      {
         String filter = appendInnerCollection(hqlFilter, collectionUuid);
         if (filter != null)
         {
            sb.append(" WHERE ").append(filter);
         }
         else
         {
            return 0; // return 0 here to terminate.
         }
      }

      return productDao.countHQLQuery(sb.toString(), hqlParameters);
   }

   /**
    * Adds HQL clause to retrieve only processed product, in the given collection.
    *
    * @param filter         initial HQL WHERE condition.
    * @param collectionUuid collection UUID
    * @return a string representation of a HQL WHERE condition.
    */
   private String appendInnerCollection(final String filter, final String collectionUuid)
   {
      // retrieve only products contained in collection uuid
      if (null == collectionUuid)
      {
         return null;
      }

      StringBuilder result = new StringBuilder(filter != null ? filter : "");
      List<Long> ids = collectionService.getProductIds(collectionUuid);
      if (!ids.isEmpty())
      {
         if (filter != null)
         {
            result.append(" AND ");
         }
         result.append("id in (");
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

         return result.toString();
      }
      else
      {
         return null;
      }
   }

   /**
    * Returns all products not contained in a collection.
    *
    * @return a set of products.
    */
   @Transactional(readOnly = true)
   public Set<Product> getNoCollectionProducts()
   {
      DetachedCriteria criteria = DetachedCriteria.forClass(Product.class);
      Iterator<Product> it = collectionService.getAllProductInCollection().iterator();

      HashSet<Long> cpid = new HashSet<>();
      while (it.hasNext())
      {
         cpid.add(it.next().getId());
      }

      if (!cpid.isEmpty())
      {
         criteria.add(Restrictions.not(Restrictions.in("id", cpid)));
      }
      return new HashSet<>(productDao.listCriteria(criteria, 0, -1));
   }

   @Transactional(readOnly = true)
   public Product getProducBytIdentifier(String identifier)
   {
      return productDao.getProductByIdentifier(identifier);
   }

   @Transactional(readOnly = true)
   public Product getProductByOrigin(final String origin)
   {
      DetachedCriteria criteria = DetachedCriteria.forClass(Product.class);
      criteria.add(Restrictions.eq("origin", origin));
      return productDao.uniqueResult(criteria);
   }

   /**
    * @param uuid           the UUID of a Product
    * @param storeAsDeleted whether the Product should be stored as deleted
    * @param cause          the cause of the Deletion
    * @return the deleted Product
    * @throws ProductNotFoundException if the Product doesn't exist
    */
   @RemoveProduct
   @Transactional
   public Product deleteByUuid(String uuid, boolean storeAsDeleted, String cause)
         throws ProductNotFoundException
   {
      Product product = systemGetProduct(uuid);
      if (product != null)
      {
         if (product.getLocked())
         {
            throw new DataStoreException(String.format("Cannot delete product %s: This product is locked by the system", uuid));
         }
         if (storeAsDeleted)
         {
            if (storeAsDeleted)
            {
               LOGGER.info("Storing product {} in DeletedProducts", uuid);
               deletedProductService.storeProduct(product, cause);
            }
         }
         LOGGER.info("Deleting product {} from database", uuid);
         productDao.delete(product);
         return product;
      }
      else
      {
         throw new ProductNotFoundException(String.format("Cannot delete product %s", uuid));
      }
   }

   /**
    * Set a product offline.
    *
    * @param uuid of the product to set offline
    * @throws ProductNotFoundException no product identified by the given UUID
    */
   @Transactional
   @Caching(evict =
   {
      @CacheEvict(value = "product", key = "#uuid"),
      @CacheEvict(value = "products", allEntries = true)
   })
   public void setProductOffline(String uuid) throws ProductNotFoundException
   {
      Product product = productDao.getProductByUuid(uuid);
      if (product != null)
      {
         product.setUpdated(new Date());
         product.setOnline(false);
         productDao.update(product);
      }
      else
      {
         throw new ProductNotFoundException();
      }
   }

   /**
    * Set a product modification date.
    *
    * @param uuid of the product
    */
   @Transactional
   @Caching(evict =
   {
      @CacheEvict(value = "product", key = "#uuid"),
      @CacheEvict(value = "products", allEntries = true)
   })
   public void updateModificationDate(String uuid) throws ProductNotFoundException
   {
      Product product = productDao.getProductByUuid(uuid);
      if (product != null)
      {
         product.setUpdated(new Date());
         productDao.update(product);
      }
      else
      {
         throw new ProductNotFoundException();
      }
   }

   /**
    * Restore a product (from offline to online).
    *
    * @param uuid      of the product to restore
    * @param size      of product's data
    * @param checksums of the product's data
    * @throws ProductNotFoundException no product identified by the given UUID
    */
   @Transactional
   @Caching(evict =
   {
      @CacheEvict(value = "product", key = "#uuid"),
      @CacheEvict(value = "products", allEntries = true)
   })
   public void restoreProduct(String uuid, Long size, Map<String, String> checksums)
         throws ProductNotFoundException
   {
      Product product = productDao.getProductByUuid(uuid);
      if (product != null)
      {
         product.setUpdated(new Date());
         product.setOnline(true);
         product.setSize(size);
         product.getDownload().setChecksums(checksums);
         productDao.update(product);
      }
      else
      {
         throw new ProductNotFoundException();
      }
   }

   @Transactional
   @Caching(evict =
   {
      @CacheEvict(value = "product", key = "#uuid"),
      @CacheEvict(value = "indexes", key = "#uuid"),
      @CacheEvict(value = "products", allEntries = true)
   })
   public Product repairProduct(String uuid, String itemClass, List<MetadataIndex> metadataIndexes, String footprint)
         throws ProductNotFoundException
   {
      String metricName = "repair." + Utils.ItemClass.toMetricNamePart(itemClass, null);
      try (Timer.Context ctx = metricRegistry.timer(metricName).time())
      {
         // update existing product
         Product product = productDao.getProductByUuid(uuid);
         if (product != null)
         {
            product.setUpdated(new Date());
            product.setItemClass(itemClass);
            product.setIndexes(metadataIndexes);
            product.setFootPrint(footprint);

            productDao.update(product);
         }
         else
         {
            throw new ProductNotFoundException();
         }

         // return updated product
         return productDao.getProductByUuid(uuid);
      }
   }

   /**
    * Get a Product by its UUID if it validates the given visitor.
    *
    * @param uuid of product to get
    * @param productSQLVisitor '$filter' expression visitor
    * @return an instance of product or {@code null} if it does not exist or does not validates the filter
    */
   @Transactional(readOnly = true)
   public Product getFilteredProduct(String uuid, ProductSQLVisitor productSQLVisitor)
   {
     return productDao.getFilteredProductByUuid(uuid, productSQLVisitor.getHqlFilter(), productSQLVisitor.getHqlParameters());
   }
}
