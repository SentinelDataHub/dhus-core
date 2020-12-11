/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016-2019 GAEL Systems
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

import org.dhus.store.datastore.config.DataStoreRestriction;
import org.dhus.store.datastore.hfs.HfsDataStore;
import org.dhus.store.datastore.hfs.HfsManager;
import org.dhus.store.keystore.VolatileKeyStore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;

import org.testng.Assert;
import org.testng.annotations.Test;

@ContextConfiguration(locations = "classpath:fr/gael/dhus/spring/context-datastore-test.xml", loader = TestContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DefaultDataStoreManagerIT extends AbstractTransactionalTestNGSpringContextTests
{
   private static final String TEMP_DIRECTORY_PATH =
         System.getProperty("java.io.tmpdir") + File.separator + "incoming_new";

   @Autowired
   DataStoreManager dss;

   @Test
   public void addAndRemove()
   {
      HfsDataStore ds = new HfsDataStore("test", new HfsManager(TEMP_DIRECTORY_PATH, 5, 100), DataStoreRestriction.NONE, 0, -1L, 0, false, null);
      ds.setKeystore(new VolatileKeyStore());
      dss.add(ds);
      Assert.assertEquals(dss.list().size(), 1);
      dss.remove("test");
      Assert.assertEquals(dss.list().size(), 0);
   }
}
