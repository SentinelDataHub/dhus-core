/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016-2020 GAEL Systems
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
import fr.gael.dhus.database.object.KeyStoreEntry;
import fr.gael.dhus.database.object.KeyStoreEntry.Key;

import java.util.List;

import org.dhus.store.datastore.DataStore;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.stereotype.Repository;

@Repository
public class KeyStoreEntryDao extends HibernateDao<KeyStoreEntry, Key>
{
   /**
    * Retrieves a key from a KeyStore by a substring of its associated value.
    * @param sub_value substring of associate value
    * @return the key of searched value
    */
   public String getKeyBySubValue(final String sub_value)
   {
      return getHibernateTemplate().execute(new HibernateCallback<String>()
      {
         @Override
         public String doInHibernate(Session session) throws HibernateException
         {
            Criteria criteria = session.createCriteria(entityClass);
            criteria.add(Restrictions.like("value", "%" + sub_value + "%"));
            List results = criteria.list();
            if (results.isEmpty())
            {
               return null;
            }
            return ((KeyStoreEntry) results.get(0)).getEntryKey();
         }
      });
   }

   /**
    * Retrieves entries of a keyStore, oldest first.
    * @param keyStoreName the name of the keyStore
    * @return a ScrollableResult of entries
    */
   public ScrollableResults readOldestEntries(final String keyStoreName)
   {
      return getHibernateTemplate().execute(new HibernateCallback<ScrollableResults>()
      {
         @Override
         public ScrollableResults doInHibernate(Session session)
               throws HibernateException
         {
            Criteria criteria = session.createCriteria(entityClass);
            criteria.add(Restrictions.eq("key.keyStore", keyStoreName));
            criteria.addOrder(Order.asc("insertionDate"));
            return criteria.scroll(ScrollMode.FORWARD_ONLY);
         }
      });
   }

   public List<KeyStoreEntry> getByUuid(final String uuid)
   {
      return getHibernateTemplate().execute(new HibernateCallback<List<KeyStoreEntry>>()
      {
         @Override
         @SuppressWarnings("unchecked")
         public List<KeyStoreEntry> doInHibernate(Session session) throws HibernateException
         {
            Criteria criteria = session.createCriteria(entityClass);
            criteria.add(Restrictions.eq("key.entryKey", uuid));
            return criteria.list();
         }
      });
   }

   // TODO other methods should be rewritten using this approach instead of Criteria
   public List<KeyStoreEntry> listForUuidAndTag(final String uuid, String tag)
   {
      String hql = "FROM KeyStoreEntry WHERE entrykey=?1 AND tag=?2";
      return getHibernateTemplate().execute(new HibernateCallback<List<KeyStoreEntry>>()
      {
         @Override
         @SuppressWarnings("unchecked")
         public List<KeyStoreEntry> doInHibernate(Session session) throws HibernateException
         {
            Query<KeyStoreEntry> query = session.createQuery(hql);
            query.setParameter(1, uuid);
            query.setParameter(2, tag);
            return query.list();
         }
      });
   }

   public List<KeyStoreEntry> getUnalteredProductEntries(final String keyStoreName, Integer skip, Integer top)
   {
      return getHibernateTemplate().execute(new HibernateCallback<List<KeyStoreEntry>>()
      {
         @Override
         @SuppressWarnings("unchecked")
         public List<KeyStoreEntry> doInHibernate(Session session) throws HibernateException
         {
            Criteria criteria = session.createCriteria(entityClass);
            criteria.add(Restrictions.eq("key.keyStore", keyStoreName));
            criteria.add(Restrictions.eq("key.tag", DataStore.UNALTERED_PRODUCT_TAG));

            if(skip != null)
            {
               criteria.setFirstResult(skip);
            }
            if (top != null)
            {
               criteria.setMaxResults(top);
            }

            return criteria.list();
         }
      });
   }
}
