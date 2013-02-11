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

import static org.testng.Assert.assertEquals;

import fr.gael.dhus.database.object.config.scanner.FileScannerConf;
import fr.gael.dhus.database.object.config.scanner.FtpScannerConf;
import fr.gael.dhus.database.object.config.scanner.ScannerConfiguration;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.util.TestContextLoader;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBException;

import org.dhus.scanner.config.ScannerConfigurationManager;
import org.dhus.scanner.config.ScannerInfo;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.xml.sax.SAXException;

@ContextConfiguration(locations = "classpath:fr/gael/dhus/spring/context-config-test.xml", loader = TestContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ScannerConfigurationManagerIT extends AbstractTestNGSpringContextTests
{
   private ScannerConfigurationManager scannerConfigurationManager;

   @BeforeClass
   public void init() throws JAXBException, IOException, SAXException
   {
      scannerConfigurationManager = ApplicationContextProvider.getBean(ConfigurationManager.class).getScannerManager();
   }

   @AfterMethod
   public void cleanup()
   {
      List<Long> ids = scannerConfigurationManager.getScanners().stream().map(ScannerConfiguration::getId).collect(Collectors.toList());
      ids.forEach(i -> scannerConfigurationManager.delete(i));
   }

   @Test
   public void createTest()
   {
      ScannerInfo scannerInfo = createScanner();

      ScannerInfo scanner2 = scannerConfigurationManager.get(scannerInfo.getId());
      Assert.assertNotNull(scanner2);
      assertEquals(scanner2.getId(), scannerInfo.getId());
   }

   @Test
   public void updateTest()
   {
      ScannerInfo scannerInfo = createScanner();

      scannerInfo.setPattern("PLEIADES");
      scannerConfigurationManager.update(scannerInfo, true);

      ScannerInfo scanner2 = scannerConfigurationManager.get(scannerInfo.getId());
      assertEquals(scanner2.getPattern(), "PLEIADES");
   }

   @Test
   public void deleteCollectionReferencesTest()
   {
      // Prepare
      ScannerInfo scannerInfo = createScanner();
      ScannerConfiguration.Collections collections = new ScannerConfiguration.Collections();
      collections.getCollection().add("col1");
      collections.getCollection().add("col2");
      scannerInfo.setCollections(collections);
      scannerConfigurationManager.update(scannerInfo, true);

      // Do
      scannerConfigurationManager.deleteCollectionReferences("col1");

      // Verify
      ScannerInfo scanner = scannerConfigurationManager.get(scannerInfo.getId());
      assertEquals(scanner.getCollections().getCollection().size(), 1);
      assertEquals(scanner.getCollections().getCollection().get(0), "col2");
      scanner.getCollections().getCollection().add(0, "col1");
      scannerConfigurationManager.update(scanner, true);
      assertEquals(scanner.getCollections().getCollection().size(), 2);
   }

   @Test
   public void deleteTest()
   {
      // prepare
      ScannerInfo scannerInfo = createScanner();
      Assert.assertNotNull(scannerInfo);
      // do
      scannerConfigurationManager.delete(scannerInfo.getId());

      // verify
      scannerInfo = scannerConfigurationManager.get(scannerInfo.getId());
      Assert.assertNull(scannerInfo);
   }

   @Test
   public void createFtpScanner()
   {
      ScannerInfo si = new FtpScannerConf();
      si.setUsername("username");
      si.setPassword("password");
      si.setPattern("pattern");

      // do
      si = scannerConfigurationManager.create(si, true);

      // verify
      si = scannerConfigurationManager.get(si.getId());
      assertEquals(si.getClass().getSimpleName(), "FtpScannerConf");
   }

   private ScannerInfo createScanner()
   {
      ScannerInfo conf = new FileScannerConf();
      conf.setId((long) Math.floor(Math.random() * 100));
      conf.setUrl("/data");
      conf.setPattern("*.odata");

      return scannerConfigurationManager.create(conf);
   }
}
