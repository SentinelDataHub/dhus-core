/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013-2017 GAEL Systems
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

import fr.gael.dhus.util.TestContextLoader;

@ContextConfiguration(
      locations =
      {
         "classpath:fr/gael/dhus/spring/context-test.xml",
         "classpath:fr/gael/dhus/spring/context-security-test.xml"
      },
      loader = TestContextLoader.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestConfigurationManager extends AbstractTransactionalTestNGSpringContextTests
{
   @Autowired
   private ConfigurationManager configurationManager;

   private final String varFolder = "local_dhus_test/";

   @Test
   public void testgetCleanDatabaseCronConfiguration()
   {
      Assert.assertNotNull(configurationManager.getCleanDatabaseCronConfiguration());
      Assert.assertEquals(configurationManager.getCleanDatabaseCronConfiguration().getSchedule(), "0 0 1 ? * *");
      Assert.assertEquals(configurationManager.getCleanDatabaseCronConfiguration().getTempUsersConfiguration().getKeepPeriod(), 10);
   }

   @Test
   public void testgetDumpDatabaseCronConfiguration()
   {
      Assert.assertNotNull(configurationManager.getDumpDatabaseCronConfiguration());
      Assert.assertEquals(configurationManager.getDumpDatabaseCronConfiguration().getSchedule(), "0 0 3 ? * *");
   }

   @Test
   public void testgetCleanDatabaseDumpCronConfiguration()
   {
      Assert.assertNotNull(configurationManager.getCleanDatabaseDumpCronConfiguration());
      Assert.assertEquals(configurationManager.getCleanDatabaseDumpCronConfiguration().getSchedule(), "0 0 4 ? * *");
      Assert.assertEquals(configurationManager.getCleanDatabaseDumpCronConfiguration().getKeep(), 10);
   }

   @Test
   public void testgetFileScannersCronConfiguration()
   {
      Assert.assertNotNull(configurationManager.getFileScannersCronConfiguration());
      Assert.assertEquals(configurationManager.getFileScannersCronConfiguration().getSchedule(), "0 0 22 ? * *");
   }

   @Test
   public void testgetSearchesCronConfiguration()
   {
      Assert.assertNotNull(configurationManager.getSearchesCronConfiguration());
      Assert.assertEquals(configurationManager.getSearchesCronConfiguration().getSchedule(), "0 0 5 ? * *");
   }

   @Test
   public void testgetSendLogsCronConfiguration()
   {
      Assert.assertNotNull(configurationManager.getSendLogsCronConfiguration());
      Assert.assertEquals(configurationManager.getSendLogsCronConfiguration().getSchedule(), "0 55 23 ? * *");
      Assert.assertEquals(configurationManager.getSendLogsCronConfiguration().getAddresses(), "dhus@gael.fr");
   }

   @Test
   public void testgetMailConfiguration()
   {
      Assert.assertNotNull(configurationManager.getMailConfiguration());
      Assert.assertEquals(configurationManager.getMailConfiguration().isOnUserCreate(), false);
      Assert.assertEquals(configurationManager.getMailConfiguration().isOnUserDelete(), false);
      Assert.assertEquals(configurationManager.getMailConfiguration().isOnUserUpdate(), false);
      Assert.assertEquals(configurationManager.getMailConfiguration().getServerConfiguration().getSmtp(), "smtp.gael.fr");
      Assert.assertEquals(configurationManager.getMailConfiguration().getServerConfiguration().getPort(), 587);
      Assert.assertEquals(configurationManager.getMailConfiguration().getServerConfiguration().isTls(), false);
      Assert.assertEquals(configurationManager.getMailConfiguration().getServerConfiguration().getUsername(), "dhus@gael.fr");
      Assert.assertEquals(configurationManager.getMailConfiguration().getServerConfiguration().getPassword(), "password");
      Assert.assertEquals(configurationManager.getMailConfiguration().getServerConfiguration().getReplyTo(), "dhus@gael.fr");
      Assert.assertEquals(configurationManager.getMailConfiguration().getServerConfiguration().getMailFromConfiguration().getAddress(), "dhus@gael.fr");
      Assert.assertEquals(configurationManager.getMailConfiguration().getServerConfiguration().getMailFromConfiguration().getName(), "DHuS Support Team");
      Assert.assertEquals(configurationManager.getMailConfiguration().getServerConfiguration().getReplyTo(), "dhus@gael.fr");
   }

   @Test
   public void getProductConfiguration()
   {
      Assert.assertNotNull(configurationManager.getProductConfiguration());
      Assert.assertEquals(configurationManager.getProductConfiguration().getQuicklookConfiguration().getHeight(), 512);
      Assert.assertEquals(configurationManager.getProductConfiguration().getQuicklookConfiguration().getWidth(), 512);
      Assert.assertEquals(configurationManager.getProductConfiguration().getQuicklookConfiguration().isCutting(), false);
      Assert.assertEquals(configurationManager.getProductConfiguration().getThumbnailConfiguration().getHeight(), 64);
      Assert.assertEquals(configurationManager.getProductConfiguration().getThumbnailConfiguration().getWidth(), 64);
      Assert.assertEquals(configurationManager.getProductConfiguration().getThumbnailConfiguration().isCutting(), false);
   }

   @Test
   public void testgetSolrConfiguration()
   {
      Assert.assertNotNull(configurationManager.getSolrConfiguration());
      Assert.assertEquals(configurationManager.getSolrConfiguration().getPath(), varFolder + "solr");
      Assert.assertEquals(configurationManager.getSolrConfiguration().getCore(), "dhus");
      Assert.assertEquals(configurationManager.getSolrConfiguration().getSchemaPath(), "");
      Assert.assertEquals(configurationManager.getSolrConfiguration().getSynonymPath(), "");
   }

   @Test
   public void testgetOdataConfiguration()
   {
      Assert.assertNotNull(configurationManager.getOdataConfiguration());
      Assert.assertEquals(configurationManager.getOdataConfiguration().getDefaultTop(), 50);
   }

   @Test
   public void testgetGeonameConfiguration()
   {
      Assert.assertNotNull(configurationManager.getGeonameConfiguration());
      Assert.assertEquals(configurationManager.getGeonameConfiguration().getUsername(), "username");
   }

   @Test
   public void testgetGeocoderConfiguration()
   {
      Assert.assertNotNull(configurationManager.getGeocoderConfiguration());
      Assert.assertEquals(configurationManager.getGeocoderConfiguration().getUrl(), "http://nominatim.openstreetmap.org");
   }

   @Test
   public void testgetNominatimConfiguration()
   {
      Assert.assertNotNull(configurationManager.getNominatimConfiguration());
      Assert.assertEquals(configurationManager.getNominatimConfiguration().isBoundingBox(), false);
      Assert.assertEquals(configurationManager.getNominatimConfiguration().getMaxPointNumber(), 50);
   }

   @Test
   public void testgetTomcatConfiguration()
   {
      Assert.assertNotNull(configurationManager.getTomcatConfiguration());
      Assert.assertEquals(configurationManager.getTomcatConfiguration().getPath(), varFolder + "tomcat");
   }

   @Test
   public void testgetSupportConfiguration()
   {
      Assert.assertNotNull(configurationManager.getSupportConfiguration());
      Assert.assertEquals(configurationManager.getSupportConfiguration().getName(), "DHuS Support Team");
      Assert.assertEquals(configurationManager.getSupportConfiguration().getMail(), "dhus@gael.fr");
      Assert.assertEquals(configurationManager.getSupportConfiguration().getRegistrationMail(), "dhus@gael.fr");
   }

   @Test
   public void testgetAdministratorConfiguration()
   {
      Assert.assertNotNull(configurationManager.getAdministratorConfiguration());
      Assert.assertEquals(configurationManager.getAdministratorConfiguration().getName(), "root");
      Assert.assertEquals(configurationManager.getAdministratorConfiguration().getPassword(), "password");
   }

   @Test
   public void testgetNameConfiguration()
   {
      Assert.assertNotNull(configurationManager.getNameConfiguration());
      Assert.assertEquals(configurationManager.getNameConfiguration().getLongName(), "Data Hub Service");
      Assert.assertEquals(configurationManager.getNameConfiguration().getShortName(), "DHuS");
   }

   @Test
   public void testgetProcessingConfiguration()
   {
      Assert.assertNotNull(configurationManager.getProcessingConfiguration());
      Assert.assertEquals(configurationManager.getProcessingConfiguration().getCorePoolSize(), 1);
   }

   @Test
   public void testgetDatabaseConfiguration()
   {
      Assert.assertNotNull(configurationManager.getDatabaseConfiguration());
      Assert.assertNotNull(configurationManager.getDatabaseConfiguration().getDumpPath());
      Assert.assertNotNull(configurationManager.getDatabaseConfiguration().getHibernateDialect());
      Assert.assertNotNull(configurationManager.getDatabaseConfiguration().getJDBCDriver());
      Assert.assertNotNull(configurationManager.getDatabaseConfiguration().getJDBCUrl());
   }
}
