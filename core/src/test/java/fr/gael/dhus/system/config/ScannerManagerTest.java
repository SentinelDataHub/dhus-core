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
package fr.gael.dhus.system.config;

import fr.gael.dhus.database.object.config.scanner.ScannerInfo;
import fr.gael.dhus.database.object.config.scanner.ScannerManager;
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

@ContextConfiguration(
      locations = "classpath:fr/gael/dhus/spring/context-test.xml",
      loader = TestContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ScannerManagerTest extends AbstractTestNGSpringContextTests
{
   ScannerManager scannerManager;

   @BeforeClass
   public void init() throws JAXBException, IOException, SAXException
   {
      scannerManager = ApplicationContextProvider.getBean(ConfigurationManager.class).getScannerManager();
   }

   @Test
   public void getScannersTest()
   {
      List<ScannerInfo> scanners = scannerManager.getScanners();
      Assert.assertEquals(scannerManager.count(), 1);
      Assert.assertEquals(scanners.size(), 1);
      Assert.assertEquals(scanners.get(0).getId(), 3L);
      Assert.assertEquals(scanners.get(0).getUrl(), "file://toto");
      Assert.assertEquals(scanners.get(0).getUsername(), "abc");
      Assert.assertEquals(scanners.get(0).getPassword(), "def");
      Assert.assertEquals(scanners.get(0).getPattern(), "S1A*");
      Assert.assertEquals(scanners.get(0).isActive(), false);
      Assert.assertEquals(scanners.get(0).getCollections().getCollection().size(), 2);
      Assert.assertEquals(scanners.get(0).getCollections().getCollection().get(1), "col2");
      Assert.assertEquals(((ScannerInfo) scanners.get(0)).getStatus(), ScannerManager.STATUS_ADDED);
      Assert.assertEquals(((ScannerInfo) scanners.get(0)).getStatusMessage(), "");
   }

   @Test(dependsOnMethods = "getScannersTest")
   public void createTest()
   {
      ScannerInfo scanner2 = scannerManager.get(4L);
      Assert.assertNull(scanner2);
      ScannerInfo scannerInfo = scannerManager.create("file://tata", ScannerManager.STATUS_OK, "Ok", false, "toto", "", null, false);
      Assert.assertEquals(scannerInfo.getId(), 4L);
      scanner2 = scannerManager.get(4L);
      Assert.assertNotNull(scanner2);
      Assert.assertEquals(scanner2.getId(), 4L);
      Assert.assertEquals(((ScannerInfo) scanner2).getStatus(), ScannerManager.STATUS_OK);
   }

   @Test(dependsOnMethods = {"createTest"})
   public void updateTest()
   {
      ScannerInfo scanner = scannerManager.get(4L);
      scanner.setStatus(ScannerManager.STATUS_ERROR);
      scanner.setPattern("PLEIADES");
      scannerManager.update(scanner, true);

      ScannerInfo scanner2 = scannerManager.get(4L);
      Assert.assertEquals(((ScannerInfo) scanner2).getStatus(), ScannerManager.STATUS_ERROR);
      Assert.assertEquals(scanner2.getPattern(), "PLEIADES");
   }

   @Test(dependsOnMethods = {"createTest", "updateTest"})
   public void deleteCollectionReferencesTest()
   {
      scannerManager.deleteCollectionReferences("col1");
      ScannerInfo scanner = scannerManager.get(3L);
      Assert.assertEquals(scanner.getCollections().getCollection().size(), 1);
      Assert.assertEquals(scanner.getCollections().getCollection().get(0), "col2");
      scanner.getCollections().getCollection().add(0, "col1");
      scannerManager.update(scanner, true);
      Assert.assertEquals(scanner.getCollections().getCollection().size(), 2);
   }

   @Test(dependsOnMethods = {"createTest"})
   public void activeTest()
   {
      scannerManager.setScannerActive(4L, true);
      ScannerInfo scanner = scannerManager.get(4L);
      Assert.assertEquals(scanner.isActive(), true);
      List<ScannerInfo> scanners = scannerManager.getActiveScanners();
      Assert.assertEquals(scanners.size(), 1);
      Assert.assertEquals(scanners.get(0).getId(), 4L);
   }

   @Test(dependsOnMethods = {"deleteCollectionReferencesTest", "updateTest", "activeTest", "createTest"})
   public void deleteTest()
   {
      ScannerInfo scanner = scannerManager.get(4L);
      Assert.assertNotNull(scanner);
      scannerManager.delete(4L);
      scanner = scannerManager.get(4L);
      Assert.assertNull(scanner);
   }

}
