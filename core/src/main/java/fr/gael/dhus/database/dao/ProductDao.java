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
package fr.gael.dhus.database.dao;

import fr.gael.dhus.database.dao.interfaces.HibernateDao;
import fr.gael.dhus.database.object.Collection;
import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.User;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dhus.olingo.v2.visitor.SQLVisitorParameter;
import org.dhus.store.LoggableProduct;

import org.hibernate.FetchMode;
import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;

import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Product Data Access Object provides interface to Product Table into the
 * database.
 */
@Repository
public class ProductDao extends HibernateDao<Product, Long>
{
   private static final Logger LOGGER = LogManager.getLogger(ProductDao.class);

   @Autowired
   private CollectionDao collectionDao;

   @Autowired
   private ProductCartDao productCartDao;

   @Autowired
   private UserDao userDao;

   private final static Integer MAX_PRODUCT_PAGE_SIZE =
         Integer.getInteger("max.product.page.size", -1);

   /**
    * Checks if the passed number as a number of product is acceptable
    * according to the current configuration
    *
    * @param n the number of product to retrieve.
    * @throws UnsupportedOperationException if the passed number cannot be handled.
    */
   static void checkProductNumber(int n)
   {
      if (MAX_PRODUCT_PAGE_SIZE < 0)
      {
         return;
      }
      if (n > MAX_PRODUCT_PAGE_SIZE)
      {
         throw new UnsupportedOperationException(
               "Product page size exceeds the authorized size ("
               + MAX_PRODUCT_PAGE_SIZE + ").");
      }
   }

   /**
    * Paginated scroll.
    * @param filter substring of Product.Identifier, may be null.
    * @param collection_id may be null.
    * @param skip number of rows to skip from the result set.
    * @return result set as a paginated iterator.
    */
   public Iterator<Product> scrollFiltered(String filter, final Long collection_id, int skip)
   {
      StringBuilder sb = new StringBuilder("SELECT p ");
      boolean whereApplied = false;
      if (collection_id != null)
      {
         sb.append("FROM Collection c LEFT OUTER JOIN c.products p ");
         sb.append("WHERE c.id=").append(collection_id);
         whereApplied = true;
      }
      else
      {
         sb.append("FROM ").append(entityClass.getName()).append(" p ");
      }

      if (filter != null && !filter.isEmpty())
      {
         sb.append(whereApplied?" AND ":" WHERE ").append(" p.identifier LIKE '%").append(filter.toUpperCase()).append("%' ");
      }

      return new PagedIterator<>(this, sb.toString(), skip);
   }

    @Override
    public void deleteAll()
    {
       Iterator<Product> it = getAllProducts ();
       while (it.hasNext ())
       {
          it.next ();
          it.remove ();
       }
    }

   @Override
   public void delete (Product product)
   {
      Product p = read (product.getId ());
      List<Collection>cls = collectionDao.getCollectionsOfProduct (p.getId ());
      // Remove collection references
      // Must use rootUser to remove every reference of this product
      // (or maybe a new non usable user ?)
      User user = userDao.getRootUser();
      if (cls!=null)
      {
         for (Collection c: cls)
         {
            LOGGER.info("deconnect product from collection " + c.getName ());
            collectionDao.removeProduct (c.getUUID (), p.getId (), user);
         }
      }
      // Remove references
      productCartDao.deleteProductReferences(p);
      setIndexes (p.getId (), null);

      super.delete (p);
   }

   /**
    * Manage replacing existing lazy index into persistent structure.
    * @param product the product to modify.
    * @param indexes the index to set.
    */
   public void setIndexes(Product product, List<MetadataIndex>indexes)
   {
      product.setIndexes (indexes);
      update (product);
   }
   public void setIndexes(Long id, List<MetadataIndex>indexes)
   {
      setIndexes (read(id), indexes);
   }

   /**
    * Returns a Product of UUID uuid, or null if it does not exist.
    *
    * @param uuid the UUID of a Product
    * @return the Product of UUID uuid, or null if it does not exist
    */
   public Product getProductByUuid(final String uuid)
   {
      return getHibernateTemplate().execute(session -> {
         Query query = session.createQuery("FROM Product WHERE uuid=?1");
         query.setParameter(1, uuid, StandardBasicTypes.STRING);
         List<?> list = query.list();
         return (Product) (list.isEmpty() ? null : list.get(0));
      });
   }

   public Iterator<Product> getAllProducts ()
   {
      String query = "FROM " + entityClass.getName ();
      return new PagedIterator<> (this, query);
   }

   public Product getFilteredProductByUuid(final String uuid, String filter, List<SQLVisitorParameter> parameters)
   {
      // TODO merge this method and getProductByUuid
      return getHibernateTemplate().execute(session ->
      {
         Query query = session.createQuery("FROM Product WHERE uuid=:uuid AND " + filter);
         query.setParameter ("uuid", uuid, StandardBasicTypes.STRING);
         parameters.forEach((t) -> query.setParameter(t.getPosition(), t.getValue(), t.getType()));
         List<?> list = query.list();
         return (Product) (list.isEmpty() ? null : list.get(0));
      });
   }

   public Product getProductByIdentifier(final String identifier)
   {
      return getHibernateTemplate().execute(session -> {
         Query query = session.createQuery("FROM Product WHERE identifier=?1");
         query.setParameter(1, identifier, StandardBasicTypes.STRING);
         List<?> list = query.list();
         return (Product) (list.isEmpty() ? null : list.get(0));
      });
   }

   @Override
   public synchronized Product create(Product p)
   {
      // Call the generation of uuid if null.
      p.getUuid();
      p.setCreated(new Date());
      p.setUpdated(p.getCreated());
      p.setOnline(true);
      p.setOnDemand(p.isOnDemand());
      return super.create(p);
   }

   @Override
   public List<Product> listCriteria(DetachedCriteria detached, int skip, int top)
   {
      detached.setFetchMode("download.checksums", FetchMode.SELECT);
      return super.listCriteria(detached, skip, top);
   }

   @Override
   public List<Product> executeHQLQuery(final String hql,
         final List<SQLVisitorParameter> parameters, final int skip, final int top)
   {
      checkProductNumber (top);
      return super.executeHQLQuery (hql, parameters, skip, top);
   }

   /**
    * Returns a list of LoggableProducts that match the given HQL String and HQL parameters.
    * LoggableProduct contain the UUID, Identifier and Size of a Product.
    * Skips the first {@code skip} products, and returns at most {@code top} products.
    *
    * @param hql and HQL query String
    * @param hqlParameters the parameters of the HQL query
    * @param skip the number of UUIDs to skip
    * @param top the maximum number of UUIDs to return
    * @return a list LoggableProducts
    */
   @SuppressWarnings("unchecked")
   public List<LoggableProduct> getProductUUIDs(String hql, List<SQLVisitorParameter> hqlParameters, int skip, int top)
   {
      Session session = getSessionFactory().getCurrentSession();
      Query query = session.createQuery("SELECT uuid, identifier, size "+hql);
      hqlParameters.forEach((t) -> query.setParameter(t.getPosition(), t.getValue(), t.getType()));

      if (skip > -1)
      {
         query.setFirstResult(skip);
      }
      if (top > -1)
      {
         query.setMaxResults(top);
      }
      query.setReadOnly(true);

      return ((List<Object[]>) query.list())
            .stream()
            .map(row -> new LoggableProduct(
                  (String) row[0], // uuid
                  (String) row[1], // identifier
                  (Long) row[2]))  // size
            .collect(Collectors.toList());
   }

}
