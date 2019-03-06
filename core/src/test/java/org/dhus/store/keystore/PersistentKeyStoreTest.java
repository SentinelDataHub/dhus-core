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
package org.dhus.store.keystore;

import fr.gael.dhus.util.TestContextLoader;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import org.testng.Assert;
import org.testng.annotations.Test;

@ContextConfiguration(
      locations = "classpath:fr/gael/dhus/spring/context-test.xml",
      loader = TestContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PersistentKeyStoreTest extends AbstractTestNGSpringContextTests
{
   @Test
   public void globalTest()
   {
      PersistentKeyStore pks = new PersistentKeyStore("test-persistent");
      pks.put("myKey", "tag", "value");
      Assert.assertEquals(pks.exists("myKey", "tag"), true);
      Assert.assertEquals(pks.get("myKey", "tag"), "value");
      pks.remove("myKey", "tag");
      Assert.assertEquals(pks.exists("myKey", "tag"), false);
   }

}
