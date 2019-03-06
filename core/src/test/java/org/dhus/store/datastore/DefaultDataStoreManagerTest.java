/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016,2017,2018 GAEL Systems
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
package org.dhus.store.datastore;

import fr.gael.dhus.util.TestContextLoader;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import org.dhus.Product;
import org.dhus.store.StoreException;
import org.dhus.store.datastore.config.DataStoreConf;
import org.dhus.store.datastore.config.HfsDataStoreConf;
import org.dhus.store.datastore.hfs.HfsProduct;
import org.dhus.store.datastore.hfs.HfsDataStore;
import org.dhus.store.datastore.hfs.HfsManager;
import org.dhus.store.keystore.VolatileKeyStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@ContextConfiguration(
   locations = "classpath:fr/gael/dhus/spring/context-test.xml",
   loader = TestContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DefaultDataStoreManagerTest extends AbstractTransactionalTestNGSpringContextTests
{
   private static final String TEMP_DIRECTORY_PATH =
         System.getProperty("java.io.tmpdir") + File.separator + "incoming_new";

   @Autowired
   DataStoreManager dss;

   @Test
   public void add()
   {
      HfsDataStore ds = new HfsDataStore("test", new HfsManager(TEMP_DIRECTORY_PATH, 5, 100), false, 0, -1L, 0, false);
      ds.setKeystore(new VolatileKeyStore());
      dss.add(ds);
      // One DataStore is already declared in the spring context
      Assert.assertEquals(dss.list().size(), 2);
      dss.remove("test");
   }

   @Test
   public void remove()
   {
      HfsDataStore ds = new HfsDataStore("test", new HfsManager(TEMP_DIRECTORY_PATH, 5, 100), false, 0, -1L, 0, false);
      ds.setKeystore(new VolatileKeyStore());
      dss.add(ds);
      Assert.assertEquals(dss.list().size(), 2);
      dss.remove("test");
      Assert.assertEquals(dss.list().size(), 1);
   }

   // Tests the DataStore service capability
   File tmp;
   String id = UUID.randomUUID().toString();

   @BeforeClass
   void init() throws IOException
   {
      HfsDataStoreConf hfs_conf = new HfsDataStoreConf();
      hfs_conf.setName("hfs_test");
      hfs_conf.setReadOnly(false);
      hfs_conf.setPath(TEMP_DIRECTORY_PATH);
      hfs_conf.setMaxFileNo(5);
      hfs_conf.setMaximumSize(-1L);
      hfs_conf.setAutoEviction(false);

      this.dss = new DefaultDataStoreManager(Collections.<DataStoreConf>singletonList(hfs_conf));

      tmp = File.createTempFile("datastore", ".test");
      tmp.deleteOnExit();
   }

   // FIXME database related exceptions occur during the following tests
   // these exceptions do not occur during normal execution
   @Test(enabled=false)
   public void set_get() throws StoreException
   {
      dss.set(id, new HfsProduct (tmp));
      Product product = dss.get(id);
      Assert.assertNotNull(product);
      dss.deleteProduct(id);
   }

   @Test(enabled=false)
   public void delete() throws StoreException
   {
      dss.deleteProduct(id);
      Assert.assertFalse(dss.hasProduct(id));
   }
}
