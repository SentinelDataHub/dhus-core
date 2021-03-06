/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2019 GAEL Systems
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
package org.dhus.store.datastore.openstack;

import fr.gael.dhus.util.TestContextLoader;

import org.dhus.Product;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.ReadOnlyDataStoreException;
import org.dhus.store.datastore.config.DataStoreRestriction;
import org.easymock.EasyMock;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@ContextConfiguration(locations = "classpath:fr/gael/dhus/spring/context-datastore-test.xml", loader = TestContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OpenStackDataStoreIT extends AbstractTestNGSpringContextTests
{

   private OpenStackDataStore openstack;

   @BeforeClass
   void init()
   {
      openstack = new OpenStackDataStore(
            "name",
            "provider",
            "identity",
            "credential",
            "url",
            "container",
            "region",
            DataStoreRestriction.READ_ONLY,
            5, -1, 0, false, null);
   }

   // test set(uuid, Product)
   @Test(expectedExceptions = ReadOnlyDataStoreException.class)
   public void testSet() throws DataStoreException
   {
      Product product = EasyMock.createMock(Product.class);
      openstack.set("6b95f536-34e9-415e-9e3d-cd5a2f6429df", product);
   }

}
