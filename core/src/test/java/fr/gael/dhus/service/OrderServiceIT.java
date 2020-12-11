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

import static fr.gael.dhus.database.dao.OrderDaoIT.*;

import static org.testng.Assert.*;

import fr.gael.dhus.database.dao.OrderDao;
import fr.gael.dhus.database.object.Order;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.util.TestContextLoader;
import java.time.Instant;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.olingo.server.api.ODataApplicationException;

import org.dhus.api.JobStatus;
import org.dhus.olingo.v2.visitor.OrderSQLVisitor;
import org.dhus.olingo.v2.visitor.SQLVisitorParameter;

import org.easymock.EasyMock;

import org.hibernate.type.StandardBasicTypes;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;

import org.testng.annotations.Test;

/**
 * Test the OrderService. All methods that purely delegates to the Order DAO are tested in the OrderDaoIT.
 */
@ContextConfiguration(locations = "classpath:fr/gael/dhus/spring/context-service-test.xml", loader = TestContextLoader.class)
public class OrderServiceIT extends AbstractTransactionalTestNGSpringContextTests
      implements InitializingBean
{
   @Autowired
   private OrderService svc;

   @Autowired
   private OrderDao dao;

   // Test data
   private User user0;
   private User user1;
   private OrderSQLVisitor visitor1;
   private OrderSQLVisitor visitor2;

   public OrderServiceIT() {}

   @Override
   public void afterPropertiesSet() throws Exception
   {
      // Initialise test data
      user0 = new User();
      user0.setUUID(UUID0); // owns all orders
      user0.setUsername("koko");
      user0.setPassword("koko");

      user1 = new User();
      user1.setUUID(UUID1); // owns only 1 order
      user1.setUsername("riko");
      user1.setPassword("koko");

      // Mock #1 no filter, no order, top 100, no skip, no param
      visitor1 = mockVisitor(null, "ord.submissionTime asc", 100, 0, Collections.<SQLVisitorParameter>emptyList());

      // Mock #2 filter STATUS=COMPLETE, no order, top 100, no skip, one param
      visitor2 = mockVisitor("ord.status=?1", null, 100, 0, Collections.<SQLVisitorParameter>singletonList(new SQLVisitorParameter(1, "COMPLETED", StandardBasicTypes.STRING)));
   }

   @Test
   public void testRefreshOrCreateOrder()
   {
      logger.info("testRefreshOrCreateOrder");
      // Update an existing JobID
      Date date = Date.from(Instant.EPOCH);
      svc.refreshOrCreateOrder(UUID0, STORE_NAME, "barbaz", JobStatus.FAILED, date, "product retrieval has failed");
      dao.getHibernateTemplate().flush();
      assertEquals(countRowsInTable("ORDERS"), 3);
      assertEquals(countRowsInTableWhere("ORDERS", "STATUS='FAILED'"), 1);
      assertEquals(countRowsInTableWhere("ORDERS", "JOB_ID='barbaz'"), 1);
      assertEquals(countRowsInTableWhere("ORDERS", "ESTIMATED_TIME='1970-01-01 00:00:00'"), 1);
      assertEquals(countRowsInTableWhere("ORDERS", "STATUS_MESSAGE='product retrieval has failed'"), 1);

      // Create a new order (unknown JobID)
      String extraUUID = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa4";
      svc.refreshOrCreateOrder(extraUUID, STORE_NAME, "foobar", JobStatus.RUNNING, null, "request is under processing");
      dao.getHibernateTemplate().flush();
      assertEquals(countRowsInTable("ORDERS"), 4);
      assertEquals(countRowsInTableWhere("ORDERS", "STATUS='RUNNING'"), 1);
      assertEquals(countRowsInTableWhere("ORDERS", "JOB_ID='foobar'"), 1);
      assertEquals(countRowsInTableWhere("ORDERS", "STATUS_MESSAGE='request is under processing'"), 1);
   }

   @Test
   public void testSetOrderRunning()
   {
      logger.info("testSetOrderRunning");
      // Calling using a non-existing product uuid to return false
      assertEquals(false, svc.setOrderRunning("not a product UUID", null, null, null));

      // Existing product uuid
      boolean result = svc.setOrderRunning(UUID2, "", null, "request is under processing");
      dao.getHibernateTemplate().flush();
      assertEquals(result, true);
      assertEquals(countRowsInTableWhere("ORDERS", "STATUS='RUNNING'"), 1);
      assertEquals(countRowsInTableWhere("ORDERS", "STATUS_MESSAGE='request is under processing'"), 1);
   }

   @Test
   public void testCreatePendingOrder()
   {
      logger.info("testCreatePendingOrder");
      String extraUUID = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa4";
      Order result = svc.createPendingOrder(STORE_NAME, extraUUID);
      dao.getHibernateTemplate().flush();
      assertEquals(result.getStatus(), JobStatus.PENDING);
      assertNull(result.getJobId());
      assertEquals(result.getOrderId().getDataStoreName(), STORE_NAME);
      assertEquals(result.getOrderId().getProductUuid(), extraUUID);
      assertEquals(countRowsInTableWhere("ORDERS", "PRODUCT_UUID='" + extraUUID + "'"), 1);
   }

   /* Factory method to generate mocks of the OrderSQLVisitor. */
   private static OrderSQLVisitor mockVisitor(String filter, String order, int top, int skip, List<SQLVisitorParameter> params)
         throws ODataApplicationException
   {
      OrderSQLVisitor visitor = EasyMock.createMock(OrderSQLVisitor.class);
      EasyMock.expect(visitor.getHqlPrefix()).andReturn("select ord from Order ord ").anyTimes();
      EasyMock.expect(visitor.getFromClause()).andReturn("from Order ord").anyTimes();
      EasyMock.expect(visitor.getSelectClause()).andReturn("select ord").anyTimes();
      EasyMock.expect(visitor.getHqlFilter()).andReturn(filter).anyTimes();
      EasyMock.expect(visitor.getHqlOrder()).andReturn(order).anyTimes();
      EasyMock.expect(visitor.getTop()).andReturn(top).anyTimes();
      EasyMock.expect(visitor.getSkip()).andReturn(skip).anyTimes();
      EasyMock.expect(visitor.getHqlParameters()).andReturn(params).anyTimes();
      EasyMock.replay(visitor);
      return visitor;
   }

   @Test
   public void testGetOrders() throws Exception
   {
      logger.info("testGetOrders");

      List<Order> result = svc.getOrders(visitor1);
      assertNotNull(result);
      assertEquals(result.size(), 3);

      result = svc.getOrders(visitor2);
      assertNotNull(result);
      assertEquals(result.size(), 2);
   }

   @Test
   public void testGetOrdersOfUser() throws Exception
   {
      logger.info("testGetOrdersOfUser");

      // Top=0 to return an empty list
      OrderSQLVisitor visitor = EasyMock.createMock(OrderSQLVisitor.class);
      EasyMock.expect(visitor.getTop()).andReturn(0);
      EasyMock.replay(visitor);
      List<Order> result = svc.getOrdersOfUser(visitor, null);
      assertNotNull(result);
      assertEquals(result.size(), 0);

      // Visitor1
      result = svc.getOrdersOfUser(visitor1, user0);
      assertNotNull(result);
      assertEquals(result.size(), 3);

      result = svc.getOrdersOfUser(visitor1, user1);
      assertNotNull(result);
      assertEquals(result.size(), 1);

      // Visitor2
      result = svc.getOrdersOfUser(visitor2, user0);
      assertNotNull(result);
      assertEquals(result.size(), 2);

      result = svc.getOrdersOfUser(visitor2, user1);
      assertNotNull(result);
      assertEquals(result.size(), 1);

      logger.info("testHasOwner");
      assertTrue(result.get(0).hasOwner(user1));
   }

   @Test
   public void testCountOrders() throws Exception
   {
      logger.info("testCountOrders");
      assertEquals(svc.countOrders(visitor1), 3);
      assertEquals(svc.countOrders(visitor2), 2);
   }

   @Test
   public void testCountOrdersOfUser() throws Exception
   {
      logger.info("testCountOrdersOfUser");

      assertEquals(svc.countOrdersOfUser(visitor1, user0), 3);
      assertEquals(svc.countOrdersOfUser(visitor1, user1), 1);

      assertEquals(svc.countOrdersOfUser(visitor2, user0), 2);
      assertEquals(svc.countOrdersOfUser(visitor2, user1), 1);
   }

   @Test
   public void testAddOwner()
   {
      logger.info("testAddOwner");
      Order order = svc.getOrderByProductUuid(UUID1);

      // Add user already owning the order
      svc.addOwner(order, user0); // no throw, no-op
      dao.getHibernateTemplate().flush();
      assertEquals(countRowsInTableWhere("ORDER_OWNERS", "USER_UUID='" + UUID0 + "'"), 3);

      // Add new owner
      svc.addOwner(order, user1);
      dao.getHibernateTemplate().flush();
      assertEquals(countRowsInTableWhere("ORDER_OWNERS", "USER_UUID='" + UUID1 + "'"), 2);
   }

   // Regression: the inner join returns a list of Object[] if the select clause is missing.
   @Test
   public void testRegressionReturnTypeOfGetOrdersOfUser() throws Exception
   {
      OrderSQLVisitor visitor = new OrderSQLVisitor(null, null, null, null);
      List<Order> result = svc.getOrdersOfUser(visitor, user0);
      assertTrue(Order.class.isAssignableFrom(result.get(0).getClass()));
   }

}
