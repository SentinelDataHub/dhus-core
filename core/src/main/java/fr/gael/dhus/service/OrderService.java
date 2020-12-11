/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019,2020 GAEL Systems
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

import fr.gael.dhus.database.dao.OrderDao;
import fr.gael.dhus.database.object.Order;
import fr.gael.dhus.database.object.Order.OrderId;
import fr.gael.dhus.database.object.OrderOwner;
import fr.gael.dhus.database.object.User;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.olingo.server.api.ODataApplicationException;

import org.dhus.api.JobStatus;
import org.dhus.olingo.v2.visitor.OrderSQLVisitor;
import org.dhus.store.quota.FetchLimiterAsyncDataStore;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService
{
   @Autowired
   private OrderDao orderDao;
   
   @Autowired
   private StoreQuotaService sqService;

   /**
    * Refresh status of existing Order or creates it if necessary.
    *
    * @param productUUID   UUID of product to fetch
    * @param dataStoreName name of the DataStore that issued the fetch request
    * @param jobId         job ID to set, may be null
    * @param jobStatus     job status, it can be completed, running or failed
    * @param estimatedDate estimated completion date of the job, may be null
    * @param statusMessage message from the DataStore explaining the current status of the Order to create
    */
   @Transactional
   public void refreshOrCreateOrder(String productUUID, String dataStoreName, String jobId, JobStatus jobStatus, Date estimatedDate, String statusMessage)
   {
      // get existing order
      Order order = orderDao.getOrderByOrderId(new Order.OrderId(dataStoreName, productUUID));
      if (order != null)
      {
         order.setStatus(jobStatus);
         order.setJobId(jobId);
         order.setEstimatedTime(estimatedDate);
         order.setStatusMessage(statusMessage);
         orderDao.update(order);
      }
      else
      {
         // order does not exist, create it with a new id
         Order newOrder = new Order(dataStoreName, productUUID, jobId, jobStatus, new Date(), estimatedDate, statusMessage);
         orderDao.create(newOrder);
      }
   }

   /**
    * Set the a jobID, an estimatedDate and the RUNNING status to the order on a product whose UUID is productUUID.
    *
    * @param productUUID   of order to set running
    * @param jobId         to set on the order
    * @param estimatedDate to set on the order
    * @param statusMessage message from the DataStore explaining the current status of the Order to update
    * @return false if no order exist for the given product UUID
    */
   @Transactional
   public boolean setOrderRunning(String productUUID, String jobId, Date estimatedDate, String statusMessage)
   {
      Order order = orderDao.getOrderByProductUuid(productUUID);
      if (order != null)
      {
         order.setStatus(JobStatus.RUNNING);
         order.setJobId(jobId);
         order.setEstimatedTime(estimatedDate);
         order.setStatusMessage(statusMessage);
         orderDao.update(order);
         return true;
      }
      return false;
   }

   /**
    * Delete an order in database.
    *
    * @param productUUID product UUID of order to delete
    */
   @Transactional
   public void deleteOrderByProductUUID(String productUUID)
   {
      //Retrieve Order info to delete Store quotas
      Order order = getOrderByProductUuid(productUUID);
      if (order != null)
      {
         sqService.deleteQuotaEntry(order.getOrderId().getDataStoreName(), FetchLimiterAsyncDataStore.NAME, productUUID);
      }
      orderDao.deleteByProductUUID(productUUID);
   }

   /**
    * Get an order by product UUID.
    *
    * @param uuid product UUID of the order to get
    * @return the order or null
    */
   @Transactional(readOnly = true)
   public Order getOrderByProductUuid(String uuid)
   {
      Order order = orderDao.getOrderByProductUuid(uuid);
      if (order != null)
      {
         Hibernate.initialize(order.getOwners());
      }
      return order;
   }

   /**
    * Returns Orders of specified data store.
    *
    * @param dataStoreName a non null DataStore name
    * @return a non null, possibly empty list of orders
    */
   @Transactional(readOnly = true)
   public List<Order> getOrdersByDataStore(String dataStoreName)
   {
      return orderDao.getOrdersByDataStore(dataStoreName);
   }

   /**
    * Returns the list of product orders that are PENDING in a specific DataStore.
    *
    * @param dataStoreName
    * @return
    */
   @Transactional(readOnly = true)
   public List<Order> getPendingOrdersByDataStore(String dataStoreName)
   {
      return orderDao.getOrdersByDataStore(dataStoreName, JobStatus.PENDING);
   }

   /**
    * Returns the number of product orders whose status is RUNNING from a specific DataStore.
    *
    * @param dataStoreName a non null DataStore name
    * @return number of orders
    */
   @Transactional(readOnly = true)
   public int countRunningOrdersByDataStore(String dataStoreName)
   {
      return orderDao.countOrdersByDataStore(dataStoreName, JobStatus.RUNNING);
   }

   /**
    * Returns the number of product orders whose status is PENDING from a specific DataStore.
    *
    * @param dataStoreName a non null DataStore name
    * @return number of orders
    */
   @Transactional(readOnly = true)
   public int countPendingOrdersByDataStore(String dataStoreName)
   {
      return orderDao.countOrdersByDataStore(dataStoreName, JobStatus.PENDING);
   }

   /**
    * Creates a PENDING Order. This Order is to be considered "in queue" and has not yet been
    * submitted to a remote asynchronous data source.
    *
    * @param dataStoreName
    * @param productUuid
    * @return
    */
   @Transactional
   public Order createPendingOrder(String dataStoreName, String productUuid)
   {
      return orderDao.create(new Order(dataStoreName, productUuid, null, JobStatus.PENDING, new Date(), null, null));
   }

   /**
    * Returns a list of Orders matching the given OData expression.
    *
    * @param orderSqlVisitor a non null expression visitor
    * @return a non null, possibly empty, list of Orders
    * @throws ODataApplicationException while visiting the OData expression
    */
   @Transactional(readOnly = true)
   public List<Order> getOrders(OrderSQLVisitor orderSqlVisitor) throws ODataApplicationException
   {
      return getOrdersOfUser(orderSqlVisitor, null);
   }

   /**
    * Returns a list of Orders matching the given OData expression and owned by the given user.
    * Returns all orders if the `user` parameter is null.
    *
    * @param orderSqlVisitor a non null expression visitor
    * @param user the owner (may be null)
    * @return a non null, possibly empty, list of Orders
    * @throws ODataApplicationException while visiting the OData expression
    */
   @Transactional(readOnly = true)
   public List<Order> getOrdersOfUser(OrderSQLVisitor orderSqlVisitor, User user) throws ODataApplicationException
   {
      if (orderSqlVisitor.getTop() <= 0)
      {
         return Collections.emptyList();
      }

      String joinClause = "";
      if (user != null)
      {
         joinClause = " inner join ord.owners o with o.key.owner.uuid = '" + user.getUUID() + "' ";
      }
      String hql = prepareHQLQuery(orderSqlVisitor.getHqlPrefix() + joinClause, orderSqlVisitor.getHqlFilter(), orderSqlVisitor.getHqlOrder());

      return orderDao.executeHQLQuery(hql, orderSqlVisitor.getHqlParameters(), orderSqlVisitor.getSkip(), orderSqlVisitor.getTop());
   }

   private String prepareHQLQuery(String hqlPrefix, String hqlFilter, String hqlOrder)
   {
      StringBuilder sb = new StringBuilder(hqlPrefix);

      if (hqlFilter != null && !hqlFilter.isEmpty())
      {
         sb.append(" WHERE ").append(hqlFilter);
      }

      if (hqlOrder != null && !hqlOrder.isEmpty())
      {
         sb.append(" ORDER BY ").append(hqlOrder);
      }

      return sb.toString();
   }

   /**
    * Count Orders matching the given OData expression.
    *
    * @param orderSQLVisitor a non null expression visitor
    * @return the number of Orders matching the OData expression
    * @throws ODataApplicationException while visiting the OData expression
    */
   @Transactional(readOnly = true)
   public int countOrders(OrderSQLVisitor orderSQLVisitor) throws ODataApplicationException
   {
      return countOrdersOfUser(orderSQLVisitor, null);
   }

   /**
    * Count Orders matching the given OData expression and owned by the given user.
    * Counts all orders if the `user` parameter is null.
    *
    * @param orderSQLVisitor a non null expression visitor
    * @param user the owner (may be null)
    * @return the number of Orders matching the OData expression
    * @throws ODataApplicationException while visiting the OData expression
    */
   @Transactional(readOnly = true)
   public int countOrdersOfUser(OrderSQLVisitor orderSQLVisitor, User user) throws ODataApplicationException
   {
      String joinClause = "";
      if (user != null)
      {
         joinClause = " inner join ord.owners o with o.key.owner.uuid = '" + user.getUUID() + "' ";
      }

      StringBuilder sb = new StringBuilder(orderSQLVisitor.getFromClause()).append(joinClause);
      if (orderSQLVisitor.getHqlFilter() != null)
      {
         sb.append(" where ").append(orderSQLVisitor.getHqlFilter());
      }
      return orderDao.countHQLQuery(sb.toString(), orderSQLVisitor.getHqlParameters());
   }

   /**
    * Add a owning User to the given Order. (user requested the async Product).
    *
    * @param order a non null Order to update
    * @param owner a non null User to add to the Order
    */
   @Transactional
   public void addOwner(Order order, User owner)
   {
      order = orderDao.getOrderByOrderId(order.getOrderId());
      order.addOwner(owner);
      orderDao.update(order);
   }

   /**
    * Get an Order by its Id.
    *
    * @param orderId a non null OrderId
    * @return an Order or null if not found
    */
   @Transactional(readOnly = true)
   public Order getOrder(OrderId orderId)
   {
      return orderDao.getOrderByOrderId(orderId);
   }
}
