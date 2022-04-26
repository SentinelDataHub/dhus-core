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
package fr.gael.dhus.database.dao;

import static org.testng.Assert.*;

import fr.gael.dhus.database.object.Order;
import fr.gael.dhus.util.TestContextLoader;

import java.util.List;

import org.dhus.api.JobStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;

import org.testng.annotations.Test;

/**
 * Test the Order DAO, 3 orders created by the import.sql script.
 */
@ContextConfiguration(locations = "classpath:fr/gael/dhus/spring/context-dao-test.xml", loader = TestContextLoader.class)
public class OrderDaoIT extends AbstractTransactionalTestNGSpringContextTests
{
   public static final String UUID0 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0";
   public static final String UUID1 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1";
   public static final String UUID2 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2";
   public static final String STORE_NAME = "storename";

   @Autowired
   OrderDao dao;

   public OrderDaoIT() {}

   @Test
   public void testGetOrderList()
   {
      List<Order> orderlist = dao.getOrderList();
      assertNotNull(orderlist);
      assertEquals(orderlist.size(), 3);
   }

   @Test
   public void testGetOrderByProductUuid()
   {
      Order order0 = dao.getOrderByProductUuid(UUID0);
      Order order1 = dao.getOrderByProductUuid(UUID1);
      Order order2 = dao.getOrderByProductUuid(UUID2);

      assertNotNull(order0);
      assertNotNull(order1);
      assertNotNull(order2);

      assertEquals(order0.getOrderId().getProductUuid(), UUID0);
      assertEquals(order1.getOrderId().getProductUuid(), UUID1);
      assertEquals(order2.getOrderId().getProductUuid(), UUID2);
   }

   @Test
   public void testGetOrderByOrderId()
   {
      Order.OrderId orderId0 = new Order.OrderId(STORE_NAME, UUID0);
      Order.OrderId orderId1 = new Order.OrderId(STORE_NAME, UUID1);
      Order.OrderId orderId2 = new Order.OrderId(STORE_NAME, UUID2);

      Order order0 = dao.getOrderByOrderId(orderId0);
      Order order1 = dao.getOrderByOrderId(orderId1);
      Order order2 = dao.getOrderByOrderId(orderId2);

      assertNotNull(order0);
      assertNotNull(order1);
      assertNotNull(order2);

      assertEquals(order0.getOrderId(), orderId0);
      assertEquals(order1.getOrderId(), orderId1);
      assertEquals(order2.getOrderId(), orderId2);
   }

   @Test
   public void testDeleteByProductUUID()
   {
      assertEquals(countRowsInTableWhere("ORDERS", "PRODUCT_UUID='" + UUID0 + "'"), 1);
      dao.deleteByProductUUID(UUID0);
      assertEquals(countRowsInTableWhere("ORDERS", "PRODUCT_UUID='" + UUID0 + "'"), 0);
   }

   @Test
   public void testGetOrdersByDataStoreName()
   {
      List<Order> orders = dao.getOrdersByDataStore(STORE_NAME);
      assertEquals(orders.size(), 3);
      orders.forEach(order -> assertEquals(order.getOrderId().getDataStoreName(), STORE_NAME));
   }

   @Test
   public void testGetOrdersByDataStoreNameAndStatus()
   {
      // One status
      List<Order> completedOrders = dao.getOrdersByDataStore(STORE_NAME, JobStatus.COMPLETED);
      List<Order>   pendingOrders = dao.getOrdersByDataStore(STORE_NAME, JobStatus.PENDING);
      List<Order>    failedOrders = dao.getOrdersByDataStore(STORE_NAME, JobStatus.FAILED);

      assertEquals(completedOrders.size(), 2);
      assertEquals(  pendingOrders.size(), 1);
      assertEquals(   failedOrders.size(), 0);

      String order0UUID = completedOrders.get(0).getOrderId().getProductUuid();
      String order1UUID = completedOrders.get(1).getOrderId().getProductUuid();
      assertTrue(order0UUID.equals(UUID0) || order0UUID.equals(UUID1));
      assertTrue(order1UUID.equals(UUID0) || order1UUID.equals(UUID1));

      assertEquals(pendingOrders.get(0).getOrderId().getProductUuid(), UUID2);

      // Multiple statuses
      List<Order> allOrders1 = dao.getOrdersByDataStore(STORE_NAME, JobStatus.COMPLETED, JobStatus.PENDING);
      List<Order> allOrders2 = dao.getOrdersByDataStore(STORE_NAME, JobStatus.COMPLETED, JobStatus.PENDING, JobStatus.FAILED, JobStatus.PAUSED, JobStatus.RUNNING, JobStatus.UNKNOWN);
      assertEqualsNoOrder(allOrders1.toArray(), allOrders2.toArray());

      List<Order> onlyPending1 = dao.getOrdersByDataStore(STORE_NAME, JobStatus.PENDING, JobStatus.RUNNING, JobStatus.UNKNOWN);
      List<Order> onlyPending2 = dao.getOrdersByDataStore(STORE_NAME, JobStatus.PENDING, JobStatus.FAILED, JobStatus.PAUSED);
      assertEquals(onlyPending1, onlyPending2);

      assertEquals(0, dao.getOrdersByDataStore(STORE_NAME, JobStatus.FAILED, JobStatus.PAUSED, JobStatus.RUNNING, JobStatus.UNKNOWN).size());
   }

   @Test
   public void testCountOrdersByDataStore()
   {
      // One status
      assertEquals(dao.countOrdersByDataStore(STORE_NAME, JobStatus.COMPLETED), 2);
      assertEquals(dao.countOrdersByDataStore(STORE_NAME, JobStatus.PENDING), 1);
      assertEquals(dao.countOrdersByDataStore(STORE_NAME, JobStatus.FAILED), 0);

      // Multiple statuses
      assertEquals(dao.countOrdersByDataStore(STORE_NAME, JobStatus.FAILED, JobStatus.PAUSED, JobStatus.RUNNING, JobStatus.UNKNOWN), 0);
      assertEquals(dao.countOrdersByDataStore(STORE_NAME, JobStatus.COMPLETED, JobStatus.PENDING), 3);
      assertEquals(dao.countOrdersByDataStore(STORE_NAME, JobStatus.COMPLETED, JobStatus.PENDING, JobStatus.FAILED, JobStatus.PAUSED, JobStatus.RUNNING, JobStatus.UNKNOWN), 3);
      assertEquals(dao.countOrdersByDataStore(STORE_NAME, JobStatus.PENDING, JobStatus.FAILED, JobStatus.PAUSED, JobStatus.RUNNING, JobStatus.UNKNOWN), 1);
   }

   @Test
   public void testGetOrderByJobId()
   {
      assertNull(dao.getOrderByJobId("invalid"));

      Order order = dao.getOrderByJobId("foo");
      assertNotNull(order);
      assertEquals(order.getJobId(), "foo");
      assertEquals(order.getOrderId().getProductUuid(), UUID0);
   }

}
