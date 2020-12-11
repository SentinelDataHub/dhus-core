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


import static fr.gael.dhus.database.dao.TransformationDaoIT.UUID0;
import static fr.gael.dhus.database.dao.TransformationDaoIT.UUID1;
import static fr.gael.dhus.database.dao.TransformationDaoIT.UUID_OUT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import fr.gael.dhus.database.dao.TransformationDao;
import fr.gael.dhus.database.object.Transformation;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.util.TestContextLoader;

import java.util.Collections;
import java.util.List;

import org.apache.olingo.server.api.ODataApplicationException;

import org.dhus.api.JobStatus;
import org.dhus.olingo.v2.visitor.SQLVisitorParameter;
import org.dhus.olingo.v2.visitor.TransformationSQLVisitor;

import org.easymock.EasyMock;

import org.hibernate.type.StandardBasicTypes;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;

import org.testng.annotations.Test;

/**
 * Test the TransformationService.
 * All methods that purely delegates to the Transformation DAO are tested in the TransformationDaoIT.
 */
@ContextConfiguration(locations = "classpath:fr/gael/dhus/spring/context-service-test.xml", loader = TestContextLoader.class)
public class TransformationServiceIT extends AbstractTransactionalTestNGSpringContextTests implements InitializingBean
{
   @Autowired
   private TransformationService svc;

   @Autowired
   private TransformationDao dao;

   private User user0;
   private User user1;
   private TransformationSQLVisitor visitor1;
   private TransformationSQLVisitor visitor2;

   public TransformationServiceIT() {}

   @Override
   public void afterPropertiesSet() throws Exception
   {
      // Initialise test data
      user0 = new User();
      user0.setUUID("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0"); // owns all transformationss
      user0.setUsername("koko");
      user0.setPassword("koko");

      user1 = new User();
      user1.setUUID("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1"); // owns only 1 transformation
      user1.setUsername("riko");
      user1.setPassword("koko");

      // Mock #1 no filter, no transformation, top 100, no skip, no param
      visitor1 = mockVisitor(null, "trf.creationDate asc", 100, 0, Collections.<SQLVisitorParameter>emptyList());

      // Mock #2 filter ID=UUID0, no transformation, top 100, no skip, no param
      visitor2 = mockVisitor("trf.id=?1", null, 100, 0, Collections.<SQLVisitorParameter>singletonList(new SQLVisitorParameter(1, UUID0, StandardBasicTypes.STRING)));
   }

   @Test
   public void testOnTransformationSuccess()
   {
      System.out.println("onTransformationSuccess");
      String url = "file://foo.bar";
      svc.onTransformationSuccess(UUID1, url);
      dao.getHibernateTemplate().flush();
      assertEquals(countRowsInTableWhere("TRANSFORMATIONS", "UUID='" + UUID1 + "' AND STATUS='COMPLETED' AND RESULT_URL='" + url + "'"), 1);
   }

   @Test
   public void testOnTransformationFailure()
   {
      System.out.println("onTransformationFailure");
      assertEquals(countRowsInTableWhere("TRANSFORMATIONS", "STATUS='FAILED'"), 0);
      svc.onTransformationFailure(UUID1);
      dao.getHibernateTemplate().flush();
      assertEquals(countRowsInTableWhere("TRANSFORMATIONS", "UUID='" + UUID1 + "' AND STATUS='FAILED'"), 1);
   }

   @Test
   public void testRemoveTransformationByOutputProduct()
   {
      System.out.println("removeTransformationByOutputProduct");
      assertEquals(countRowsInTableWhere("TRANSFORMATIONS", "PRODUCT_OUT='" + UUID_OUT + "'"), 1);
      svc.removeTransformationByOutputProduct(UUID_OUT);
      dao.getHibernateTemplate().flush();
      assertEquals(countRowsInTableWhere("TRANSFORMATIONS", "PRODUCT_OUT='" + UUID_OUT + "'"), 0);
   }

   @Test
   public void testUpdateStatus()
   {
      System.out.println("updateStatus");
      svc.updateStatus(UUID0, JobStatus.RUNNING.toString(), "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa5", "http://yes.no", null);
      dao.getHibernateTemplate().flush();
      assertEquals(countRowsInTableWhere("TRANSFORMATIONS", "UUID='" + UUID0 + "' AND STATUS='RUNNING'"), 1);
   }

   @Test
   public void testCountPendingTransformations()
   {
      System.out.println("countPendingTransformations");
      assertEquals(svc.countPendingTransformations(), 2);
   }

   @Test
   public void testCountRunningTransformations()
   {
      System.out.println("countRunningTransformations");
      assertEquals(svc.countRunningTransformations(), 0);
   }

   @Test(expectedExceptions = NullPointerException.class)
   public void testAddUserNPE()
   {
      logger.info("testAddUserNPE");
      svc.addUser(null, null);
   }

   @Test
   public void testAddUser()
   {
      logger.info("testAddUser");

      Transformation transformation = new Transformation();
      transformation.setUuid(UUID0);

      // Add user already owning the transformation
      svc.addUser(transformation, user0); // no throw, no-op
      dao.getHibernateTemplate().flush();
      assertEquals(countRowsInTableWhere("USER_TRANSFORMATIONS", "USER_UUID='aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0'"), 3);

      // Add new user
      assertEquals(countRowsInTableWhere("USER_TRANSFORMATIONS", "USER_UUID='aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1'"), 1);
      transformation.setUuid(UUID1);
      svc.addUser(transformation, user1);
      dao.getHibernateTemplate().flush();
      assertEquals(countRowsInTableWhere("USER_TRANSFORMATIONS", "USER_UUID='aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1'"), 2);
   }

   @Test
   public void testRemoveUser()
   {
      logger.info("testRemoveUser");

      User user = new User();
      user.setUUID("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1"); // owns only 1 transformation
      user.setUsername("riko");
      user.setPassword("koko");

      Transformation transformation = new Transformation();

      // Try to remove a user that does not own the transformation
      assertEquals(countRowsInTableWhere("USER_TRANSFORMATIONS", "USER_UUID='aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1'"), 1);
      transformation.setUuid(UUID1);
      svc.removeUser(transformation, user); // no throw, no-op
      dao.getHibernateTemplate().flush();
      assertEquals(countRowsInTableWhere("USER_TRANSFORMATIONS", "USER_UUID='aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1'"), 1);

      // Remove a user owning a transformation
      transformation.setUuid(UUID0);
      svc.removeUser(transformation, user);
      dao.getHibernateTemplate().flush();
      assertEquals(countRowsInTableWhere("USER_TRANSFORMATIONS", "USER_UUID='aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1'"), 0);
   }

   @Test
   public void testGetTransformations() throws Exception
   {
      logger.info("testGetTransformations");

      List<Transformation> result = svc.getTransformations(visitor1);
      assertNotNull(result);
      assertEquals(result.size(), 3);

      result = svc.getTransformations(visitor2);
      assertNotNull(result);
      assertEquals(result.size(), 1);
   }

   /* Factory method to generate mocks of the TransformationSQLVisitor. */
   private static TransformationSQLVisitor mockVisitor(String filter, String order, int top, int skip, List<SQLVisitorParameter> params)
         throws ODataApplicationException
   {
      TransformationSQLVisitor visitor = EasyMock.createMock(TransformationSQLVisitor.class);
      EasyMock.expect(visitor.getHqlPrefix()).andReturn("select trf from Transformation trf ").anyTimes();
      EasyMock.expect(visitor.getFromClause()).andReturn("from Transformation trf").anyTimes();
      EasyMock.expect(visitor.getSelectClause()).andReturn("select trf").anyTimes();
      EasyMock.expect(visitor.getHqlFilter()).andReturn(filter).anyTimes();
      EasyMock.expect(visitor.getHqlOrder()).andReturn(order).anyTimes();
      EasyMock.expect(visitor.getTop()).andReturn(top).anyTimes();
      EasyMock.expect(visitor.getSkip()).andReturn(skip).anyTimes();
      EasyMock.expect(visitor.getHqlParameters()).andReturn(params).anyTimes();
      EasyMock.replay(visitor);
      return visitor;
   }

   @Test
   public void testGetTransformationsOfUser() throws ODataApplicationException
   {
      logger.info("getTransformationsOfUser");

      // Top=0 to return an empty list
      TransformationSQLVisitor visitor = EasyMock.createMock(TransformationSQLVisitor.class);
      EasyMock.expect(visitor.getTop()).andReturn(0);
      EasyMock.replay(visitor);
      List<Transformation> result = svc.getTransformationsOfUser(visitor, null);
      assertNotNull(result);
      assertEquals(result.size(), 0);

      // Visitor1
      result = svc.getTransformationsOfUser(visitor1, user0);
      assertNotNull(result);
      assertEquals(result.size(), 3);

      result = svc.getTransformationsOfUser(visitor1, user1);
      assertNotNull(result);
      assertEquals(result.size(), 1);

      // Visitor2
      result = svc.getTransformationsOfUser(visitor2, user0);
      assertNotNull(result);
      assertEquals(result.size(), 1);

      result = svc.getTransformationsOfUser(visitor2, user1);
      assertNotNull(result);
      assertEquals(result.size(), 1);
   }

   @Test
   public void testCountTransformations() throws Exception
   {
      logger.info("testCountTransformations");
      assertEquals(svc.countTransformations(visitor1), 3);
      assertEquals(svc.countTransformations(visitor2), 1);
   }

   @Test
   public void testCountTransformationsOfUser() throws Exception
   {
      logger.info("testCountTransformationsOfUser");

      assertEquals(svc.countTransformationsOfUser(visitor1, user0), 3);
      assertEquals(svc.countTransformationsOfUser(visitor1, user1), 1);

      assertEquals(svc.countTransformationsOfUser(visitor2, user0), 1);
      assertEquals(svc.countTransformationsOfUser(visitor2, user1), 1);
   }
}