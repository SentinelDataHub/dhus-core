/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
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
package org.dhus.store;

import java.util.List;

import org.dhus.store.metadatastore.RelationalMetadataStore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;

import org.testng.Assert;
import org.testng.annotations.Test;

import fr.gael.dhus.util.TestContextLoader;

@ContextConfiguration(
      locations = "classpath:fr/gael/dhus/spring/context-test.xml",
      loader = TestContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RelationalMetadataStoreFilterTest extends AbstractTransactionalTestNGSpringContextTests
{
   @Autowired
   RelationalMetadataStore relationalMDS;

   @Test
   public void testFilter() throws StoreException
   {
      List<LoggableProduct> productUUIDs = relationalMDS.getProductUUIDs("Name eq 'prod0'", null, null, 0, 3);
      Assert.assertEquals(productUUIDs.size(), 1);
      Assert.assertEquals(productUUIDs.get(0).getIdentifier(), "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0");
   }

   @Test
   public void testOrderBy() throws StoreException
   {
      List<LoggableProduct> productUUIDs = relationalMDS.getProductUUIDs(null, "Name desc", null, 0, 3);
      Assert.assertEquals(productUUIDs.size(), 3);
      Assert.assertEquals(productUUIDs.get(0).getIdentifier(), "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa7");
      Assert.assertEquals(productUUIDs.get(2).getIdentifier(), "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa5");
   }
}
