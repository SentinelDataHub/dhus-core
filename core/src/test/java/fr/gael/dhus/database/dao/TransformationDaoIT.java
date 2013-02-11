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

import static org.testng.Assert.*;

import fr.gael.dhus.database.object.Transformation;
import fr.gael.dhus.util.TestContextLoader;

import java.util.List;

import org.dhus.api.JobStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;

import org.testng.annotations.Test;

/**
 * Test the Transformation DAO. 3 transformations created by the import.sql script.
 */
@ContextConfiguration(locations = "classpath:fr/gael/dhus/spring/context-dao-test.xml", loader = TestContextLoader.class)
public class TransformationDaoIT extends AbstractTransactionalTestNGSpringContextTests
{
   public static final String UUID0 = "ttttttttttttttttttttttttttttttt0";
   public static final String UUID1 = "ttttttttttttttttttttttttttttttt1";
   public static final String UUID2 = "ttttttttttttttttttttttttttttttt2";
   public static final String UUID_OUT = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa4";

   public static final String TRANSFORMER0 = "transformer0";
   public static final String TRANSFORMER1 = "transformer1";

    @Autowired
    private TransformationDao dao;

   public TransformationDaoIT() {}

   @Test
   public void testFindTransformationsOf()
   {
      logger.info("findTransformationsOf");
      List<Transformation> res;

      // has 1
      res = dao.findTransformationsOf(TRANSFORMER0);
      assertNotNull(res);
      assertEquals(res.size(), 1);
      assertEquals(res.get(0).getUuid(), UUID0);

      // has 2
      res = dao.findTransformationsOf(TRANSFORMER1);
      assertNotNull(res);
      assertEquals(res.size(), 2);
      assertEquals(res.get(0).getUuid(), UUID1);
      assertEquals(res.get(1).getUuid(), UUID2);

      // empty res
      res = dao.findTransformationsOf("foobar");
      assertNotNull(res);
      assertEquals(res.size(), 0);
   }

   @Test
   public void testFindTransformationOf()
   {
      logger.info("findTransformationOf");
      Transformation res;

      res = dao.findTransformationOf(TRANSFORMER0, UUID0);
      assertNotNull(res);
      assertEquals(res.getUuid(), UUID0);
      assertEquals(res.getTransformer(), TRANSFORMER0);

      assertNull(dao.findTransformationOf(TRANSFORMER0, UUID1));
      assertNull(dao.findTransformationOf(TRANSFORMER1, UUID0));
   }

   @Test
   public void testFindTransformation()
   {
      logger.info("findTransformation");
      Transformation res;

      res = dao.findTransformation(TRANSFORMER0, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0", 0);
      assertNotNull(res);
      assertEquals(res.getUuid(), UUID0);
      assertEquals(res.getTransformer(), TRANSFORMER0);

      assertNull(dao.findTransformation(TRANSFORMER0, UUID0, 42));
      assertNull(dao.findTransformation(TRANSFORMER1, UUID0, 0));
      assertNull(dao.findTransformation(TRANSFORMER0, UUID1, 0));
   }

   @Test
   public void testFindTransformationByOutput()
   {
      logger.info("findTransformationByOutput");
      Transformation res;

      res = dao.findTransformationByOutput(UUID_OUT);
      assertNotNull(res);
      assertEquals(res.getUuid(), UUID2);
      assertEquals(res.getProductIn(), "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2");
      assertEquals(res.getTransformer(), TRANSFORMER1);
      assertEquals(res.getProductOut(), UUID_OUT);

      assertNull(dao.findTransformationByOutput(UUID0));
   }

   @Test
   public void testGetTransformationsByStatus()
   {
      logger.info("getTransformationsByStatus");
      List<Transformation> res;

      // empty res
      res = dao.getTransformationsByStatus(JobStatus.FAILED.toString());
      assertNotNull(res);
      assertEquals(res.size(), 0);

      // has 1
      res = dao.getTransformationsByStatus(JobStatus.COMPLETED.toString());
      assertNotNull(res);
      assertEquals(res.size(), 1);
      assertEquals(res.get(0).getUuid(), UUID2);

      // has 2
      res = dao.getTransformationsByStatus(JobStatus.PENDING.toString());
      assertNotNull(res);
      assertEquals(res.size(), 2);
      assertEquals(res.get(0).getUuid(), UUID0);
      assertEquals(res.get(1).getUuid(), UUID1);
   }

   @Test
   public void testCountTransformationsByStatus()
   {
      logger.info("countTransformationsByStatus");
      assertEquals(dao.countTransformationsByStatus(JobStatus.FAILED.toString()), 0);
      assertEquals(dao.countTransformationsByStatus(JobStatus.COMPLETED.toString()), 1);
      assertEquals(dao.countTransformationsByStatus(JobStatus.PENDING.toString()), 2);
   }

   @Test
   public void testDeleteTransformationByUuid()
   {
      logger.info("deleteTransformationByUuid");
      assertEquals(countRowsInTable("TRANSFORMATIONS"), 3);
      dao.deleteTransformationByUuid(UUID0);
      dao.getHibernateTemplate().flush();
      assertEquals(countRowsInTable("TRANSFORMATIONS"), 2);
   }

   @Test
   public void testfindTransformationByUuid()
   {
      System.out.println("findTransformationByUuid");
      Transformation transformation = dao.findTransformationByUuid(UUID0);
      assertNotNull(transformation);
      assertEquals(transformation.getTransformer(), TRANSFORMER0);
   }

   @Test
   public void testgetTransformationsOfUser()
   {
      logger.info("getTransformationOfUser");
      List<Transformation> result = dao.getTransformationsOfUser("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0");
      assertNotNull(result);
      assertEquals(result.size(), 3);
   }
}
