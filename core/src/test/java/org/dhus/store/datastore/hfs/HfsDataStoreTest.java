/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016,2017 GAEL Systems
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
package org.dhus.store.datastore.hfs;

import fr.gael.dhus.util.TestContextLoader;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import org.dhus.Product;
import org.dhus.store.datastore.DataStoreException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@ContextConfiguration(
   locations = "classpath:fr/gael/dhus/spring/context-test.xml",
   loader = TestContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class HfsDataStoreTest extends AbstractTransactionalTestNGSpringContextTests
{
   private static final String TEMP_DIRECTORY_PATH =
         System.getProperty("java.io.tmpdir") + File.separator + "incoming";

   private HfsDataStore hfsDataStore;

   private File tmp;

   @BeforeClass
   void init() throws IOException
   {
      HfsManager hfs = new HfsManager(TEMP_DIRECTORY_PATH, 5,10);
      hfsDataStore = new HfsDataStore("hfs-test", hfs, false, 0, -1L, 0, false);
      tmp = File.createTempFile("datastore", ".test");
      tmp.deleteOnExit();
   }

   @AfterClass
   void exit()
   {
      FileUtils.deleteQuietly(new File(hfsDataStore.getHfs().getPath()));
   }

   @Test
   public void insert()
   {
      Assert.assertNotNull(hfsDataStore);

      String id = UUID.randomUUID().toString();
      try
      {
         hfsDataStore.set(id, new HfsProduct(tmp));
         Product product = hfsDataStore.get(id);

         Assert.assertNotNull(product);
         Assert.assertTrue(product.getName().startsWith("datastore"));

         Assert.assertTrue(product.hasImpl(File.class));
         Assert.assertTrue(product.getImpl(File.class).exists());
      }
      catch (DataStoreException e)
      {
         Assert.fail("An exception occurred:", e);
      }
   }

   @Test(dependsOnMethods = {"insert"})
   public void delete()
   {
      String id = UUID.randomUUID().toString();

      try
      {
         hfsDataStore.set(id, new HfsProduct(tmp));
         Product product = hfsDataStore.get(id);

         Assert.assertNotNull(product);
         Assert.assertTrue(product.getName().startsWith("datastore"));

         Assert.assertTrue(product.hasImpl(File.class));
         Assert.assertTrue(product.getImpl(File.class).exists());

         hfsDataStore.deleteProduct(id);
         Assert.assertFalse(product.getImpl(File.class).exists());
      }
      catch (DataStoreException e)
      {
         Assert.fail("An exception occurred:", e);
      }
   }
}
