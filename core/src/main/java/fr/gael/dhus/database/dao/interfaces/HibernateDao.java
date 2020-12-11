/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013-2020 GAEL Systems
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
package fr.gael.dhus.database.dao.interfaces;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import javax.swing.event.EventListenerList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.olingo.v2.visitor.SQLVisitorParameter;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.query.Query;
import org.hibernate.query.NativeQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.orm.hibernate5.support.HibernateDaoSupport;

/**
 * Hibernate DAO Implementation, containing minimal CRUD operations.
 *
 * @param <T>  Object concerned by this DAO
 * @param <PK> Primary Key of this Object
 */
public class HibernateDao<T, PK extends Serializable>
      extends HibernateDaoSupport
      implements GenericDao<T, PK>, Pageable<T>
{
   private final Logger LOGGER = LogManager.getLogger();

   private final EventListenerList listeners = new EventListenerList();

   protected Class<T> entityClass;

   @SuppressWarnings("unchecked")
   public HibernateDao()
   {
      ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
      this.entityClass = (Class<T>) genericSuperclass.getActualTypeArguments()[0];
   }

   @SuppressWarnings("unchecked")
   @Override
   public T create(T t)
   {
      T sent = t;
      long start = System.currentTimeMillis();
      PK id = (PK) getHibernateTemplate().save(t);
      t = getHibernateTemplate().get((Class<T>) t.getClass(), id);
      long end = System.currentTimeMillis();
      LOGGER.info("Create/save {}({}) spent {}ms", entityClass.getSimpleName(), id, (end - start));

      fireCreatedEvent(new DaoEvent<>(sent));
      return t;
   }

   @Override
   public T read(PK id)
   {
      long start = System.currentTimeMillis();
      T ret = getHibernateTemplate().get(entityClass, id);
      long end = System.currentTimeMillis();
      LOGGER.debug("Read {}({}) spent {}ms", entityClass.getSimpleName(), id, (end - start));
      return ret;
   }

   @Override
   public void update(T t)
   {
      long start = System.currentTimeMillis();
      getHibernateTemplate().update(t);
      long end = System.currentTimeMillis();
      LOGGER.info("Update {}({}) spent {}ms", entityClass.getSimpleName(), t.toString(), (end - start));

      fireUpdatedEvent(new DaoEvent<>(t));
   }

   /**
    * Merge the provided object into the current session.
    * This could be useful when one session handle the same object twice.
    *
    * @param t the entity to merge
    */
   public void merge(T t)
   {
      long start = System.currentTimeMillis();
      getHibernateTemplate().getSessionFactory().getCurrentSession().merge(t);
      long end = System.currentTimeMillis();
      LOGGER.info("Merge {} spent {}ms", entityClass.getSimpleName(), (end - start));
   }

   @Override
   public void delete(T t)
   {
      long start = System.currentTimeMillis();
      getHibernateTemplate().delete(t);
      long end = System.currentTimeMillis();
      LOGGER.info("Delete {}({}) spent {}ms", entityClass.getSimpleName(), t.toString(), (end - start));

      fireDeletedEvent(new DaoEvent<>(t));
   }

   /**
    * Remove all the element from the db of this T instance.
    */
   public void deleteAll()
   {
      for (T entity: readAll())
      {
         delete(entity);
      }
   }

   /**
    * Retrieve the first element of the results.
    *
    * @param query_string
    * @return
    */
   @SuppressWarnings("unchecked")
   public T first(String query_string)
   {
      return getHibernateTemplate().<T>execute((Session session) -> {
         return (T) session.createQuery(query_string).setMaxResults(1).getSingleResult();
      });
   }

   /**
    * Returns all Objects in a List.
    *
    * @return List containing all Objects
    */
   @SuppressWarnings("unchecked")
   @Override
   public List<T> readAll()
   {
      return find("FROM " + entityClass.getName());
   }

   /**
    * Count objects in table.
    *
    * @return Objects count
    */
   public int count()
   {
      // FIXME return type should be long
      return (int) DataAccessUtils.longResult(getHibernateTemplate().execute((Session session) ->
      {
         return session.createQuery("SELECT count (*) FROM " + entityClass.getName()).list();
      }));
   }

   @SuppressWarnings("rawtypes")
   public List find(String query_string) throws DataAccessException
   {
      long start = System.currentTimeMillis();

      List ret = getHibernateTemplate().<List>execute((Session session) -> {
         return session.createQuery(query_string).list();
      });

      long end = System.currentTimeMillis();
      LOGGER.debug("Query \"{}\" spent {}ms", query_string, (end - start));
      return ret;
   }

   public void addListener(DaoListener<T> listener)
   {
      listeners.add(DaoListener.class, listener);
   }

   public void removeListener(DaoListener<T> listener)
   {
      listeners.remove(DaoListener.class, listener);
   }

   @SuppressWarnings("unchecked")
   public DaoListener<T>[] getListeners()
   {
      return listeners.getListeners(DaoListener.class);
   }

   protected void fireCreatedEvent(DaoEvent<T> e)
   {
      for (DaoListener<T> listener: getListeners())
      {
         listener.created(e);
      }
   }

   protected void fireUpdatedEvent(DaoEvent<T> e)
   {
      for (DaoListener<T> listener: getListeners())
      {
         listener.updated(e);
      }
   }

   protected void fireDeletedEvent(DaoEvent<T> e)
   {
      for (DaoListener<T> listener: getListeners())
      {
         listener.deleted(e);
      }
   }

   @Autowired
   public void init(SessionFactory session_factory)
   {
      setSessionFactory(session_factory);
   }

   public void printCurrentSessions()
   {
      int num_session = countOpenSessions();
      LOGGER.info("{} open sessions:", countOpenSessions());
      int index = 0;
      while (index < num_session)
      {
         LOGGER.info("   SESSION_ID       {}", getSystemByName("SESSION_ID", index));
         LOGGER.info("   CONNECTED        {}", getSystemByName("CONNECTED", index));
         LOGGER.info("   SCHEMA           {}", getSystemByName("SCHEMA", index));
         LOGGER.info("   WAITING_FOR_THIS {}", getSystemByName("WAITING_FOR_THIS", index));
         LOGGER.info("   THIS_WAITING_FOR {}", getSystemByName("THIS_WAITING_FOR", index));
         LOGGER.info("   LATCH_COUNT      {}", getSystemByName("LATCH_COUNT", index));
         LOGGER.info("   STATEMENT        {}", getSystemByName("CURRENT_STATEMENT", index));
         LOGGER.info("");
         index++;
      }
   }

   @SuppressWarnings("rawtypes")
   private int countOpenSessions()
   {
      return DataAccessUtils.intResult(getHibernateTemplate().execute(new HibernateCallback<List>()
      {
         @Override
         public List doInHibernate(Session session) throws HibernateException
         {
            String sql = "SELECT count (*) FROM INFORMATION_SCHEMA.SYSTEM_SESSIONS";
            NativeQuery query = session.createNativeQuery(sql);
            return query.list();
         }
      }));
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   private String getSystemByName(final String name, final int index)
   {
      return DataAccessUtils.uniqueResult(
            getHibernateTemplate().execute(new HibernateCallback<List>()
      {
         @Override
         public List doInHibernate(Session session) throws HibernateException
         {
            String sql =
                  "SELECT " + name
                  + " FROM INFORMATION_SCHEMA.SYSTEM_SESSIONS"
                  + " LIMIT  1 OFFSET " + index;
            NativeQuery query = session.createNativeQuery(sql);
            return query.list();
         }
      })).toString();
   }

   /**
    * Returns a paged list of database entities.
    *
    * @param query the passed query to retrieve the list
    * @param skip  the number of elements to skip in the list (0=no skip)
    * @param top   number of element to be retained in the list
    * @throws ClassCastException if query does not returns entity list of type T
    * @see org.hibernate.Query
    */
   @Override
   public List<T> getPage(final String query, final int skip, final int top)
   {
      return getHibernateTemplate().execute(new HibernateCallback<List<T>>()
      {
         // List must be instance of List<T> otherwise ClassCast
         @SuppressWarnings("unchecked")
         @Override
         public List<T> doInHibernate(Session session) throws HibernateException
         {
            Query hql_query = session.createQuery(query);
            hql_query.setFirstResult(skip);
            hql_query.setMaxResults(top);
            return hql_query.list();
         }
      });
   }

   @SuppressWarnings("unchecked")
   public List<T> listCriteria(DetachedCriteria detached, int skip, int top)
   {
      SessionFactory factory = getSessionFactory();
      Session session = factory.getCurrentSession();

      Criteria criteria = detached.getExecutableCriteria(session);

      if (skip > 0)
      {
         criteria.setFirstResult(skip);
      }
      if (top > 0)
      {
         criteria.setMaxResults(top);
      }
      return criteria.list();
   }

   @SuppressWarnings("unchecked")
   public T uniqueResult(DetachedCriteria criteria)
   {
      Criteria excrit = criteria.getExecutableCriteria(getSessionFactory().getCurrentSession());
      excrit.setMaxResults(1);
      List<?> res = excrit.list();
      return res.isEmpty() ? null : (T) res.get(0);
   }

   public int count(DetachedCriteria detached)
   {
      Session session = getSessionFactory().getCurrentSession();
      Criteria criteria = detached.getExecutableCriteria(session);
      criteria.setProjection(Projections.rowCount());
      Object result = criteria.uniqueResult();
      return ((Number) result).intValue();
   }

   @SuppressWarnings("unchecked")
   public List<T> executeHQLQuery(final String hql,
         final List<SQLVisitorParameter> parameters, final int skip, final int top)
   {
      Session session = getSessionFactory().getCurrentSession();
      Query query = session.createQuery(hql);

      if (!parameters.isEmpty())
      {
         parameters.forEach((t) -> query.setParameter(t.getPosition(), t.getValue(), t.getType()));
      }
      if (skip > 0)
      {
         query.setFirstResult(skip);
      }
      if (top > 0)
      {
         query.setMaxResults(top);
      }
      query.setReadOnly(true);
      return query.list();
   }

   public int countHQLQuery(String hql, List<SQLVisitorParameter> parameters)
   {
      String hqlQuery = "SELECT COUNT(*) " + hql;
      Session session = getSessionFactory().getCurrentSession();
      Query query = session.createQuery(hqlQuery);
      parameters.forEach((t) -> query.setParameter(t.getPosition(), t.getValue(), t.getType()));
      query.setReadOnly(true);

      Object obj = query.uniqueResult();
      int result;
      if (obj instanceof Long)
      {
         result = ((Long) obj).intValue();
      }
      else
      {
         result = (int) obj;
      }
      return result;
   }
}
