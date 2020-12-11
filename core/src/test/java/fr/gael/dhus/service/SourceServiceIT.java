/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018,2019 GAEL Systems
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

import fr.gael.dhus.database.object.config.source.Source;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerSource;
import fr.gael.dhus.util.TestContextLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.easymock.EasyMock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import org.testng.Assert;
import org.testng.annotations.Test;

@ContextConfiguration(locations = "classpath:fr/gael/dhus/spring/context-service-test.xml", loader = TestContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SourceServiceIT extends AbstractTestNGSpringContextTests
{
   @Autowired
   private SourceService service;

   @Test
   public void count()
   {
      Assert.assertEquals(service.count(), 3);
   }

   @Test
   public void get()
   {
      int sourceId = 1;
      String expectedUrl = "https://dhus1.com";
      String expectedUsername = "dhus1_username";
      String expectedPassword = "dhus1_password";

      Source source = service.getSource(sourceId);
      Assert.assertNotNull(source);
      Assert.assertEquals(source.getId(), sourceId);
      Assert.assertEquals(source.getUrl(), expectedUrl);
      Assert.assertEquals(source.getUsername(), expectedUsername);
      Assert.assertEquals(source.getPassword(), expectedPassword);

      sourceId = 512;
      source = service.getSource(sourceId);
      Assert.assertNull(source);

      sourceId = 1;
      SynchronizerSource synchronizerSource = new SynchronizerSource();
      synchronizerSource.setSourceId(sourceId);
      source = service.getSource(synchronizerSource);
      Assert.assertNotNull(source);
      Assert.assertEquals(source.getId(), synchronizerSource.getSourceId());
      Assert.assertEquals(source.getUrl(), expectedUrl);
      Assert.assertEquals(source.getUsername(), expectedUsername);
      Assert.assertEquals(source.getPassword(), expectedPassword);

      sourceId = 123;
      synchronizerSource.setSourceId(sourceId);
      source = service.getSource(synchronizerSource);
      Assert.assertNull(source);

      List<SynchronizerSource> synchronizerSources = new ArrayList<>(3);
      List<Source> sources = service.getSource(synchronizerSources);
      Assert.assertNotNull(sources);
      Assert.assertTrue(sources.isEmpty());

      sourceId = 3;
      synchronizerSource.setSourceId(sourceId);
      SynchronizerSource synchronizerSourceFake = new SynchronizerSource();
      synchronizerSourceFake.setSourceId(99);
      synchronizerSources.add(synchronizerSource);
      synchronizerSources.add(synchronizerSourceFake);
      sources = service.getSource(synchronizerSources);
      Assert.assertNotNull(sources);
      Assert.assertEquals(sources.size(), 1);
      source = sources.get(0);
      Assert.assertNotNull(source);
      Assert.assertEquals(source.getId(), sourceId);
   }

   @Test(dependsOnMethods = {"get"})
   public void createSource()
   {
      String expectedUrl = "https://foo-bar.net";
      String expectedUsername = "login-username";
      String expectedPassword = "login-password";

      Source source = service.createSource(expectedUrl, expectedUsername, expectedPassword, null);

      Assert.assertNotNull(source);
      Assert.assertTrue(source.getId() > 3);
      Assert.assertEquals(source.getUrl(), expectedUrl);
      Assert.assertEquals(source.getUsername(), expectedUsername);
      Assert.assertEquals(source.getPassword(), expectedPassword);
      Assert.assertEquals(source.getBandwidth(), -1L);
   }

   @Test(expectedExceptions = {IllegalArgumentException.class})
   public void createSourceNullUrlArg()
   {
      String expectedUrl = null;
      service.createSource(expectedUrl, null, null, null);
   }

   @Test(expectedExceptions = {IllegalArgumentException.class})
   public void createSourceInvalidUrlArg()
   {
      String expectedUrl = "ftp://foo-bar.net";
      service.createSource(expectedUrl, null, null, null);
   }

   @Test(dependsOnMethods = {"createSource"})
   public void updateSource()
   {
      int sourceId = 1;
      String expectedUrl = "https://foo-bar.net";

      Source source = service.getSource(sourceId);
      source.setUrl("https://foo-bar.net");
      Assert.assertTrue(service.updateSource(source));

      source = service.getSource(sourceId);
      Assert.assertEquals(source.getUrl(), expectedUrl);
   }

   @Test(dependsOnMethods = {"updateSource"})
   public void deleteSource()
   {
      int sourceId = 4;
      int expectedCount = service.count() - 1;

      Source source = new Source();
      source.setId(sourceId);

      Assert.assertTrue(service.deleteSource(source));
      Assert.assertEquals(service.count(), expectedCount);
      Assert.assertNull(service.getSource(sourceId));
      Assert.assertFalse(service.deleteSource(source));
   }

   @Test(dependsOnMethods = {"count"})
   public void list()
   {
      List<Source> sourceList = service.list();
      Assert.assertEquals(sourceList.size(), service.count());
      sourceList.forEach(Assert::assertNotNull);
   }

   @Test
   public void sortSource()
   {
      Source source0 = EasyMock.createMock(Source.class);
      source0.setId(0);
      EasyMock.expect(source0.getBandwidth()).andStubReturn(3L);
      EasyMock.expect(source0.getId()).andStubReturn(0);

      Source source1 = EasyMock.createMock(Source.class);
      source1.setId(1);
      EasyMock.expect(source1.getBandwidth()).andStubReturn(12L);
      EasyMock.expect(source1.getId()).andStubReturn(1);

      Source source2 = EasyMock.createMock(Source.class);
      source2.setId(2);
      EasyMock.expect(source2.getBandwidth()).andStubReturn(1L);
      EasyMock.expect(source2.getId()).andStubReturn(2);

      Source source3 = EasyMock.createMock(Source.class);
      source3.setId(3);
      EasyMock.expect(source3.getBandwidth()).andStubReturn(3L);
      EasyMock.expect(source3.getId()).andStubReturn(3);

      Source source4 = EasyMock.createMock(Source.class);
      source4.setId(4);
      EasyMock.expect(source4.getBandwidth()).andStubReturn(8L);
      EasyMock.expect(source4.getId()).andStubReturn(4);

      Source source5 = EasyMock.createMock(Source.class);
      source5.setId(5);
      EasyMock.expect(source5.getBandwidth()).andStubReturn(-1L);
      EasyMock.expect(source5.getId()).andStubReturn(5);

      EasyMock.replay(source0, source1, source2, source3, source4, source5);
      List<Source> sources = Arrays.asList(source0, source1, source2, source3, source4, source5);
      List<Source> sortedList = service.sortSources(sources);

      Assert.assertNotNull(sortedList);
      Assert.assertEquals(sortedList.size(), 6);
      Assert.assertEquals(sortedList.get(0).getId(), 5);
      Assert.assertEquals(sortedList.get(1).getId(), 1);
      Assert.assertEquals(sortedList.get(2).getId(), 4);
      Assert.assertEquals(sortedList.get(3).getId(), 0);
      Assert.assertEquals(sortedList.get(4).getId(), 3);
      Assert.assertEquals(sortedList.get(5).getId(), 2);
   }
}
