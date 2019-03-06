/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018 GAEL Systems
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
package fr.gael.dhus.system.config;

import fr.gael.dhus.database.object.config.source.Source;
import fr.gael.dhus.sync.smart.SourceManager;
import fr.gael.dhus.util.TestContextLoader;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@ContextConfiguration(
      locations = "classpath:fr/gael/dhus/spring/context-test.xml",
      loader = TestContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SourceManagerTest extends AbstractTestNGSpringContextTests
{
   @Autowired
   private ConfigurationManager configManager;

   private SourceManager sourceManager;

   @BeforeClass
   public void setUp()
   {
      sourceManager = configManager.getSourceManager();
   }

   @Test
   public void listTest()
   {
      List<Source> sources = sourceManager.list();
      Assert.assertEquals(sources.size(), 3);

      Source source = sources.get(1);
      Assert.assertEquals(source.getId(), 2);
      Assert.assertEquals(source.getUrl(), "https://dhus2.com");
      Assert.assertEquals(source.getUsername(), "dhus2_username");
      Assert.assertEquals(source.getPassword(), "dhus2_password");
      Assert.assertEquals(source.getMaxDownload().intValue(), 5);
   }

   @Test
   public void getTest()
   {
      int sourceId = 2;

      Source source = sourceManager.get(sourceId);
      Assert.assertNotNull(source);
      Assert.assertEquals(source.getId(), sourceId);

      sourceId = 25;
      source = sourceManager.get(sourceId);
      Assert.assertNull(source);
   }

   @Test
   public void countTest()
   {
      Assert.assertEquals(sourceManager.count(), 3);
   }

   @Test(dependsOnMethods = {"countTest", "listTest"})
   public void createSourceTest()
   {
      String url = "http://url-test.com";
      String username = "login";
      String password = "password";
      int maxDownload = 3;
      int count = sourceManager.list().size();
      int sourceId = 4;

      Source source = sourceManager.create(url, username, password, maxDownload);
      Assert.assertNotNull(source);
      Assert.assertEquals(source.getId(), sourceId);
      Assert.assertEquals(sourceManager.list().size(), (count + 1));

      source = sourceManager.get(sourceId);
      Assert.assertNotNull(source);
      Assert.assertEquals(source.getId(), sourceId);
      Assert.assertEquals(source.getUrl(), url);
      Assert.assertEquals(source.getUsername(), username);
      Assert.assertEquals(source.getPassword(), password);
      Assert.assertEquals(source.getMaxDownload().intValue(), maxDownload);
   }

   @Test(dependsOnMethods = {"getTest", "createSourceTest"})
   public void updateSourceTest()
   {
      int sourceId = 4;
      String expectedUrl = "https://url-test-updated/dhus";
      Source source = sourceManager.get(sourceId);
      int expectedId = source.getId();
      int expectedMaxDownload = 20;
      String expectedUsername = source.getUsername();
      String expectedPassword = source.getPassword();

      source.setUrl(expectedUrl);
      source.setMaxDownload(expectedMaxDownload);
      sourceManager.updateSource(source);

      source = sourceManager.get(sourceId);
      Assert.assertNotNull(source);
      Assert.assertEquals(source.getId(), expectedId);
      Assert.assertEquals(source.getUrl(), expectedUrl);
      Assert.assertEquals(source.getUsername(), expectedUsername);
      Assert.assertEquals(source.getPassword(), expectedPassword);
      Assert.assertEquals(source.getMaxDownload().intValue(), expectedMaxDownload);
   }

   @Test(dependsOnMethods = {"countTest", "createSourceTest", "updateSourceTest"})
   public void deleteSource()
   {
      int sourceId = 4;
      int count = sourceManager.list().size();

      sourceManager.removeSource(sourceId);

      Assert.assertEquals(sourceManager.list().size(), (count - 1));
      Assert.assertNull(sourceManager.get(sourceId));
   }
}
