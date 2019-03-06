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
package fr.gael.dhus.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

import fr.gael.dhus.database.object.KeyStoreEntry;
import fr.gael.dhus.database.object.KeyStoreEntry.Key;
import fr.gael.dhus.util.TestContextLoader;

@ContextConfiguration(
      locations =
      {
         "classpath:fr/gael/dhus/spring/context-test.xml",
         "classpath:fr/gael/dhus/spring/context-security-test.xml"
      },
      loader = TestContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestKeyStoreService extends AbstractTransactionalTestNGSpringContextTests
{
   @Autowired
   private KeyStoreService keyStoreService;

   @Test
   public void testCRUD() throws InterruptedException
   {
      Key key = new Key("myStore", "key", "tag");
      KeyStoreEntry kse = new KeyStoreEntry(key, "toto", 0L);
      Key key2 = new Key("myStore", "key2", "tag");
      keyStoreService.createEntry(kse);
      Assert.assertEquals(keyStoreService.getEntry("myStore", "key", "tag").getValue(), "toto");
      Assert.assertEquals(keyStoreService.getEntry(key).getValue(), "toto");
      Assert.assertEquals(keyStoreService.getEntry(key2), null);
      Assert.assertEquals(keyStoreService.exists("myStore", "key", "tag"), true);
      Assert.assertEquals(keyStoreService.exists(key), true);
      Assert.assertEquals(keyStoreService.exists(key2), false);
      kse.setValue("toto2");
      keyStoreService.updateEntry(kse);
      Assert.assertEquals(keyStoreService.getEntry(key).getValue(), "toto2");
      keyStoreService.deleteEntry(kse);
      Assert.assertEquals(keyStoreService.exists(key), false);
   }

   @Test(expectedExceptions = {DuplicateKeyException.class})
   public void testDoubleCreate()
   {
      Key key = new Key("myStore", "key", "tag");
      KeyStoreEntry kse = new KeyStoreEntry(key, "toto", 0L);
      keyStoreService.createEntry(kse);
      Assert.assertEquals(keyStoreService.getEntry(key).getValue(), "toto");
      KeyStoreEntry kse2 = new KeyStoreEntry(key, "toto3", 0L);
      keyStoreService.createEntry(kse2);
   }
}
