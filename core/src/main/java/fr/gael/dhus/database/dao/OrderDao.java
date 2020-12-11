/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019 GAEL Systems
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
import fr.gael.dhus.database.object.Order;
import fr.gael.dhus.database.object.Order.OrderId;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.dhus.api.JobStatus;

import org.hibernate.query.Query;
import org.hibernate.type.StandardBasicTypes;

import org.springframework.stereotype.Repository;

@Repository
public class OrderDao extends HibernateDao<Order, String>
{
   @SuppressWarnings("unchecked")
   public List<Order> getOrderList()
   {
      return getHibernateTemplate().execute(session ->
      {
         String hql = "SELECT o FROM Order o";
         Query query = session.createQuery(hql);
         return (List<Order>) query.list();
      });
   }

   private Order makeHqlRequest(String field, String hql)
   {
      return getHibernateTemplate().execute(session ->
      {
         Query query = session.createQuery(hql);
         query.setParameter(1, field, StandardBasicTypes.STRING);
         List<?> list = query.list();
         return (Order) (list.isEmpty() ? null : list.get(0));
      });
   }

   public Order getOrderByProductUuid(final String uuid)
   {
      String hql = "FROM Order WHERE product_uuid=?1";
      return makeHqlRequest(uuid, hql);
   }

   public Order getOrderByOrderId(final OrderId id)
   {
      String hql = "FROM Order WHERE product_uuid=?1 AND datastore_name=?2";
      return getHibernateTemplate().execute(session ->
      {
          Query query = session.createQuery(hql);
          query.setParameter(1, id.getProductUuid(), StandardBasicTypes.STRING);
          query.setParameter(2, id.getDataStoreName(), StandardBasicTypes.STRING);
          List<?> list = query.list();
          return (Order) (list.isEmpty() ? null : list.get(0));
       });
   }

   public void deleteByProductUUID(String prodUUID)
   {
      getHibernateTemplate().execute((session) ->
      {
         Query query = session.createQuery("DELETE FROM Order where product_uuid=?1");
         query.setParameter(1, prodUUID);
         query.executeUpdate();
         return null;
      });
   }

   @SuppressWarnings("unchecked")
   public List<Order> getOrdersByDataStore(String dataStoreName)
   {
      String hql = "FROM Order WHERE datastore_name=?1";

      return getHibernateTemplate().execute(session ->
      {
         Query query = session.createQuery(hql);
         query.setParameter(1, dataStoreName, StandardBasicTypes.STRING);
         return query.list();
      });
   }

   @SuppressWarnings("unchecked")
   public List<Order> getOrdersByDataStore(String dataStoreName, JobStatus...statuses)
   {
      return getHibernateTemplate().execute(session ->
      {
         Query query = session.createQuery("FROM Order WHERE datastore_name=?1" + makeStatusPredicate(1, statuses));
         query.setParameter(1, dataStoreName, StandardBasicTypes.STRING);

         // assign status values
         int paramIndex = 1;
         for (JobStatus status: statuses)
         {
            query.setParameter(++paramIndex, status.toString(), StandardBasicTypes.STRING);
         }

         return query.list();
      });
   }

   public int countOrdersByDataStore(String dataStoreName, JobStatus... statuses)
   {
      return ((Long) getHibernateTemplate().execute(session ->
      {
         Query query = session.createQuery("SELECT COUNT(*) FROM Order WHERE datastore_name=?1" + makeStatusPredicate(1, statuses));
         query.setParameter(1, dataStoreName, StandardBasicTypes.STRING);

         // assign status values
         int paramIndex = 1;
         for (JobStatus status: statuses)
         {
            query.setParameter(++paramIndex, status.toString(), StandardBasicTypes.STRING);
         }
         return query.uniqueResult();
      })).intValue();

   }

   private String makeStatusPredicate(int shift, JobStatus... statuses)
   {
      // make an HQL string with enough "status=?#" statements and put OR operators between them
      Optional<String> predicate = IntStream.range(1 + shift, statuses.length + 1 + shift)
            .<String>mapToObj(index -> "status=?" + index)
            .reduce((left, right) -> left + " OR " + right);
      if (predicate.isPresent())
      {
         return " AND (" + predicate.get() + ")";
      }
      else
      {
         return "";
      }
   }
}
