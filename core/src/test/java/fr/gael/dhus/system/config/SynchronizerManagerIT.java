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
package fr.gael.dhus.system.config;

import fr.gael.dhus.database.object.config.synchronizer.EventSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.SmartProductSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerConfiguration;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerManager;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerSource;
import fr.gael.dhus.database.object.config.synchronizer.UserSynchronizer;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.util.TestContextLoader;

import java.io.IOException;
import java.util.List;
import javax.xml.bind.JAXBException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.xml.sax.SAXException;

@ContextConfiguration(locations = "classpath:fr/gael/dhus/spring/context-config-test.xml", loader = TestContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SynchronizerManagerIT extends AbstractTestNGSpringContextTests
{
   private final long expectedCount = 5;
   private final long expectedNextSyncId = 5;
   private SynchronizerManager syncManager;

   @BeforeClass
   public void init() throws JAXBException, IOException, SAXException
   {
      syncManager = ApplicationContextProvider.getBean(ConfigurationManager.class).getSynchronizerManager();
   }

   @Test
   public void getSynchronizersTest()
   {
      List<SynchronizerConfiguration> scanners = syncManager.getSynchronizers();

      Assert.assertEquals(syncManager.count(), expectedCount);
      Assert.assertEquals(scanners.size(), expectedCount);

      Assert.assertTrue(scanners.get(0) instanceof EventSynchronizer);
      Assert.assertEquals(scanners.get(0).getId(), 0L);
      Assert.assertEquals(scanners.get(0).getLabel(), "event1");
      Assert.assertEquals(scanners.get(0).getServiceUrl(), "http://toto");
      Assert.assertEquals(scanners.get(0).getServiceLogin(), "abc");
      Assert.assertEquals(scanners.get(0).getServicePassword(), "def");
      Assert.assertEquals(scanners.get(0).getSchedule(), "*/5 * * ? * *");
      Assert.assertEquals(scanners.get(0).isActive(), false);
      Assert.assertEquals(scanners.get(0).getCreated().getHour(), 9);
      Assert.assertEquals(scanners.get(0).getCreated().getYear(), 2002);
      Assert.assertEquals(scanners.get(0).getPageSize(), 80);
      Assert.assertEquals(((EventSynchronizer) scanners.get(0)).getFilterParam(), "Evt2*");
      Assert.assertEquals(((EventSynchronizer) scanners.get(0)).getSkip(), Integer.valueOf(10));

      Assert.assertTrue(scanners.get(2) instanceof UserSynchronizer);
      Assert.assertEquals(scanners.get(2).getId(), 2L);
      Assert.assertEquals(scanners.get(2).getLabel(), "user1");
      Assert.assertEquals(scanners.get(2).getServiceUrl(), "http://toto");
      Assert.assertEquals(scanners.get(2).getServiceLogin(), "abc");
      Assert.assertEquals(scanners.get(2).getServicePassword(), "def");
      Assert.assertEquals(scanners.get(2).getSchedule(), "*/5 * * ? * *");
      Assert.assertEquals(scanners.get(2).isActive(), false);
      Assert.assertEquals(scanners.get(2).getCreated().getHour(), 9);
      Assert.assertEquals(scanners.get(2).getCreated().getYear(), 2002);
      Assert.assertEquals(scanners.get(2).getPageSize(), 3);
      Assert.assertEquals(((UserSynchronizer) scanners.get(2)).isForce(), Boolean.FALSE);
      Assert.assertEquals(((UserSynchronizer) scanners.get(2)).getSkip(), Integer.valueOf(0));

      Assert.assertTrue(scanners.get(3) instanceof ProductSynchronizer);
      ProductSynchronizer productSync = ((ProductSynchronizer) scanners.get(3));
      Assert.assertEquals(productSync.getId(), 3L);
      Assert.assertEquals(productSync.getLabel(), "product1");
      Assert.assertEquals(productSync.getServiceUrl(), "http://toto");
      Assert.assertEquals(productSync.getServiceLogin(), "abc");
      Assert.assertEquals(productSync.getServicePassword(), "def");
      Assert.assertEquals(productSync.getSchedule(), "*/5 * * ? * *");
      Assert.assertEquals(productSync.isActive(), false);
      Assert.assertEquals(productSync.getCreated().getHour(), 9);
      Assert.assertEquals(productSync.getCreated().getYear(), 2002);
      Assert.assertEquals(productSync.getPageSize(), 3);
      Assert.assertEquals(productSync.getTargetCollection(), "target");
      Assert.assertEquals(productSync.getFilterParam(), "S2A*");
      Assert.assertEquals(productSync.getGeofilterOp(), "Op");
      Assert.assertEquals(productSync.getGeofilterShape(), "Paris");

      // Smart Synchronizer
      Assert.assertTrue(scanners.get(4) instanceof SmartProductSynchronizer);
      SmartProductSynchronizer smartProductSync = ((SmartProductSynchronizer) scanners.get(4));
      List<SynchronizerSource> sources = smartProductSync.getSources().getSource();
      Assert.assertEquals(smartProductSync.getThreshold().longValue(), 50);
      Assert.assertEquals(smartProductSync.getTimeout().longValue(), 120);
      Assert.assertEquals(smartProductSync.getAttempts().intValue(), 3);
      Assert.assertEquals(sources.size(), 2);
      SynchronizerSource syncSource;
      syncSource = sources.get(0);
      Assert.assertEquals(syncSource.getSourceId(), 0);
      Assert.assertEquals(
            syncSource.getLastCreated().toGregorianCalendar().getTimeInMillis(), 1510617600000L);
      Assert.assertEquals(syncSource.getSourceCollection(), "Sentinel-1");
      syncSource = sources.get(1);
      Assert.assertEquals(syncSource.getSourceId(), 1);
      Assert.assertEquals(syncSource.getLastCreated().toGregorianCalendar().getTimeInMillis(), 0L);
      Assert.assertNull(syncSource.getSourceCollection());
   }

   @Test(dependsOnMethods = "getSynchronizersTest")
   public void createTest()
   {
      SynchronizerConfiguration sync = syncManager.get(expectedNextSyncId);
      Assert.assertNull(sync);
      SynchronizerConfiguration sync2 = new EventSynchronizer();
      sync2 = syncManager.create(sync2, false);
      Assert.assertEquals(sync2.getId(), expectedNextSyncId);
      sync = syncManager.get(expectedNextSyncId);
      Assert.assertNotNull(sync);
      Assert.assertEquals(sync.getId(), expectedNextSyncId);
   }

   @Test(dependsOnMethods = {"createTest"})
   public void updateTest()
   {
      SynchronizerConfiguration sync = syncManager.get(expectedNextSyncId);
      sync.setLabel("event3");
      ((EventSynchronizer) sync).setSkip(20);
      syncManager.update(sync);

      SynchronizerConfiguration sync2 = syncManager.get(expectedNextSyncId);
      Assert.assertEquals(((EventSynchronizer) sync2).getSkip(), Integer.valueOf(20));
      Assert.assertEquals(sync2.getLabel(), "event3");
   }

   @Test(dependsOnMethods = {"createTest"})
   public void activeTest()
   {
      List<SynchronizerConfiguration> active = syncManager.getActiveSynchronizers();
      Assert.assertEquals(active.size(), 0);
      SynchronizerConfiguration sync = syncManager.get(expectedNextSyncId);
      sync.setActive(true);
      syncManager.update(sync);
      SynchronizerConfiguration sync2 = syncManager.get(expectedNextSyncId);

      Assert.assertEquals(sync2.isActive(), true);
      List<SynchronizerConfiguration> syncs = syncManager.getActiveSynchronizers();
      Assert.assertEquals(syncs.size(), 1);
      Assert.assertEquals(syncs.get(0).getId(), expectedNextSyncId);
   }

   @Test(dependsOnMethods = {"updateTest", "activeTest", "createTest"})
   public void deleteTest()
   {
      SynchronizerConfiguration sync = syncManager.get(expectedNextSyncId);
      Assert.assertNotNull(sync);
      syncManager.delete(sync);
      sync = syncManager.get(expectedNextSyncId);
      Assert.assertNull(sync);
   }

}
