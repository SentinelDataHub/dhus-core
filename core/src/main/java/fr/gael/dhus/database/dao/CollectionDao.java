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
package fr.gael.dhus.database.dao;

import fr.gael.dhus.database.dao.interfaces.CollectionProductListener;
import fr.gael.dhus.database.dao.interfaces.DaoEvent;
import fr.gael.dhus.database.dao.interfaces.DaoListener;
import fr.gael.dhus.database.dao.interfaces.HibernateDao;
import fr.gael.dhus.database.object.Collection;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.type.StandardBasicTypes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author pidancier
 */
@Repository
public class CollectionDao extends HibernateDao<Collection, String>
{
   final public static String HIDDEN_PREFIX = "#.";
   final public static String COLLECTION_ROOT_NAME = HIDDEN_PREFIX + "root";
   private static final Logger LOGGER = LogManager.getLogger(CollectionDao.class);

   @Autowired
   private ProductDao productDao;

   @Autowired
   private ConfigurationManager cfgManager;

   /**
    * Delete the given collection and move all products contains in it are not
    * deleted.
    * @param collection collection to delete.
    */
   @Override
   public void delete (final Collection collection)
   {
      // remove references
      cfgManager.getScannerManager().deleteCollectionReferences(collection.getName());
      // delete collection
      super.delete (collection);
   }

   /**
    * Deletes all collections.
    */
   @Override
   public void deleteAll ()
   {
      // delete all collection without the root collection
      for (Collection collection : readAll ())
      {
         delete (collection);
      }
   }

   /**
    * Checks if the collection contains the passed product.
    *
    * @param cid the collection to check.
    * @param pid the product to retrieve in collection.
    * @return true if the product is included in the collection, false
    *         otherwise.
    */
   public boolean contains (final String cid, final Long pid)
   {
      Collection collection = read(cid);
      Hibernate.initialize (collection.getProducts());
      return collection.getProducts().contains(productDao.read(pid));
   }

   /**
    * Remove a product from a collection. The product should stay in the
    * database.
    *
    * @param cid the collection id where remove product.
    * @param pid the product id to remove.
    * @param user unused parameter.
    */
   public void removeProduct (final String cid, final Long pid, User user)
   {
      Collection collection = read(cid);
      if (collection == null)
      {
         LOGGER.warn("Unknown collection #" + cid);
         return;
      }
      Product product = productDao.read(pid);
      if (product == null)
      {
         LOGGER.warn("Unknown product #" + pid);
         return;
      }

      Hibernate.initialize (collection.getProducts());
      collection.getProducts().remove(product);
      update(collection);

      fireProductRemoved (new DaoEvent<> (collection), product);
   }

   /**
    * Remove a product from a collection. The product should stay in the
    * database.
    * @param cid the collection id where remove product.
    * @param pids the product id to remove.
    * @param user unused parameter.
    */
   public void removeProducts (final String cid, final Long[] pids, User user)
   {
      for (Long pid : pids)
         removeProduct (cid, pid, user);
   }

   // Not filtered by user, only called by ProductDao.delete, which must delete
   // all product references
   @SuppressWarnings ("unchecked")
   public List<Collection> getCollectionsOfProduct (final Long product_id)
   {
      return getHibernateTemplate().execute(session -> {
         String hql = "select c from Collection c "
                    + "left outer join c.products p where p.id = ? ORDER BY c.name";
         Query query = session.createQuery(hql);
         query.setFetchSize(10_000);
         query.setParameter(0, product_id, StandardBasicTypes.LONG);
         return (List<Collection>) query.list();
      });
   }

   /**
    * Retrieves all product id from a given collection
    * @param collection_uuid
    * @param user unused parameter.
    * @return
    */
   public List<Long> getProductIds (final String collection_uuid, User user)
   {
      if (collection_uuid == null)
      {
         return Collections.emptyList ();
      }

      List<Long> result = new ArrayList<> ();
      Iterator<Product> it = read (collection_uuid).getProducts ().iterator ();
      while (it.hasNext ())
      {
         Product product = it.next ();
         if (product != null)
         {
            result.add (product.getId ());
         }
      }
      return result;
   }

   void fireProductAdded (DaoEvent<Collection> e, Product p)
   {
      e.addParameter ("product", p);

      for (DaoListener<?> listener : getListeners ())
      {
         if (listener instanceof CollectionProductListener)
            ((CollectionProductListener) listener).productAdded (e);
      }
   }

   void fireProductRemoved (DaoEvent<Collection> e, Product p)
   {
      e.addParameter ("product", p);

      for (DaoListener<?> listener : getListeners ())
      {
         if (listener instanceof CollectionProductListener)
            ((CollectionProductListener) listener).productRemoved (e);
      }
   }

   /**
    * @return UUIDs of all collections
    */
   @SuppressWarnings("unchecked")
   public List<String> getCollectionsUUID()
   {
      return (List<String>) find("select uuid FROM " + entityClass.getName());
   }

   public String getCollectionUUIDByName(final String collection_name)
   {
      return getHibernateTemplate().execute(session -> {
         String hql = "SELECT uuid FROM " + entityClass.getName() + " WHERE name=?";
         Query query = session.createQuery(hql);
         query.setReadOnly(true);
         query.setParameter(0, collection_name, StandardBasicTypes.STRING);
         return (String) query.uniqueResult();
      });
   }

   public Collection getByName(final String name)
   {
      Collection collection = getHibernateTemplate().execute(session -> {
         String hql = "From Collection c where c.name=?";
         Query query = session.createQuery(hql);
         query.setParameter(0, name, StandardBasicTypes.STRING);
         return (Collection) query.uniqueResult();
      });

      // First without `lower()` to save time
      if (collection == null)
      {
         collection = getHibernateTemplate().execute(session -> {
            String hql = "From Collection c where LOWER(c.name)=?";
            Query query = session.createQuery(hql);
            query.setParameter(0, name.toLowerCase(), StandardBasicTypes.STRING);
            List list = query.list();

            return (Collection) (list.isEmpty() ? null : list.get(0));
         });
      }
      return collection;
   }

   public Iterator<Collection> getAllCollections ()
   {
      String query = "FROM " + entityClass.getName ();
      return new PagedIterator<> (this, query);
   }
}
