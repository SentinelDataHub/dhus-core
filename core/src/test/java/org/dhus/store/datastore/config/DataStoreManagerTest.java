/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2018 GAEL Systems
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
package org.dhus.store.datastore.config;

import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;
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

@ContextConfiguration(locations = "classpath:fr/gael/dhus/spring/context-test.xml", loader = TestContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DataStoreManagerTest extends AbstractTestNGSpringContextTests
{
   DataStoreManager dsManager;

   @BeforeClass
   public void init() throws JAXBException, IOException, SAXException
   {
      dsManager = ApplicationContextProvider.getBean(ConfigurationManager.class).getDataStoreManager();
   }

   @Test
   public void getDataStoresTest()
   {
      List<DataStoreConf> dataStores = dsManager.getAllDataStoreConfigurations();

      Assert.assertEquals(dataStores.size(), 3);

      Assert.assertTrue(dataStores.get(0) instanceof HfsDataStoreConf);
      Assert.assertEquals(dataStores.get(0).isReadOnly(), false);
      Assert.assertEquals(dataStores.get(0).getPriority(), 75);
      Assert.assertEquals(((HfsDataStoreConf) dataStores.get(0)).getName(), "hfs");
      Assert.assertEquals(((HfsDataStoreConf) dataStores.get(0)).getPath(), "/toto");
      Assert.assertEquals(((HfsDataStoreConf) dataStores.get(0)).getMaxFileNo(), new Integer(5));

      Assert.assertTrue(dataStores.get(1) instanceof OpenStackDataStoreConf);
      Assert.assertEquals(dataStores.get(1).isReadOnly(), false);
      Assert.assertEquals(dataStores.get(1).getPriority(), 75);
      Assert.assertEquals(((OpenStackDataStoreConf) dataStores.get(1)).getName(), "openstack");
      Assert.assertEquals(((OpenStackDataStoreConf) dataStores.get(1)).getProvider(), "provider");
      Assert.assertEquals(((OpenStackDataStoreConf) dataStores.get(1)).getIdentity(), "id");
      Assert.assertEquals(((OpenStackDataStoreConf) dataStores.get(1)).getCredential(), "cred");
      Assert.assertEquals(((OpenStackDataStoreConf) dataStores.get(1)).getUrl(), "http://toto");
      Assert.assertEquals(((OpenStackDataStoreConf) dataStores.get(1)).getContainer(), "container");
      Assert.assertEquals(((OpenStackDataStoreConf) dataStores.get(1)).getRegion(), "region");

      Assert.assertTrue(dataStores.get(2) instanceof GmpDataStoreConf);
      Assert.assertEquals(dataStores.get(2).isReadOnly(), false);
      Assert.assertEquals(dataStores.get(2).getPriority(), 75);
      Assert.assertEquals(((GmpDataStoreConf) dataStores.get(2)).getName(), "gmp");
      Assert.assertEquals(((GmpDataStoreConf) dataStores.get(2)).isIsMaster(), true);
      Assert.assertNotNull(((GmpDataStoreConf) dataStores.get(2)).getMysqlConnectionInfo());
      Assert.assertEquals(((GmpDataStoreConf) dataStores.get(2)).getMysqlConnectionInfo().getValue(), "url");
      Assert.assertEquals(((GmpDataStoreConf) dataStores.get(2)).getMysqlConnectionInfo().getUser(), "user");
      Assert.assertEquals(((GmpDataStoreConf) dataStores.get(2)).getMysqlConnectionInfo().getPassword(), "sa");
      Assert.assertEquals(((GmpDataStoreConf) dataStores.get(2)).getGmpRepoLocation(), "gmpRepo");
      Assert.assertEquals(((GmpDataStoreConf) dataStores.get(2)).getHfsLocation(), "hfsLoc");
      Assert.assertEquals(((GmpDataStoreConf) dataStores.get(2)).getMaxQueuedRequest(), new Integer(5));
   }

   @Test(dependsOnMethods = "getDataStoresTest")
   public void createTest()
   {
      HfsDataStoreConf ds = (HfsDataStoreConf) dsManager.get("lambda");
      Assert.assertNull(ds);
      HfsDataStoreConf ds2 = new HfsDataStoreConf();
      ds2 = (HfsDataStoreConf) dsManager.create(ds2, false);
      ds2.setName("lambda");
      ds = (HfsDataStoreConf) dsManager.get("lambda");
      Assert.assertNotNull(ds);
      Assert.assertEquals(ds.getName(), "lambda");
   }

   @Test(dependsOnMethods = {"createTest"})
   public void updateTest()
   {
      HfsDataStoreConf ds = (HfsDataStoreConf) dsManager.get("lambda");
      ds.setPath("myPath");
      ds.setPriority(100);
      dsManager.update(ds);

      HfsDataStoreConf ds2 = (HfsDataStoreConf) dsManager.get("lambda");
      Assert.assertEquals(ds2.getPath(), "myPath");
      Assert.assertEquals(ds2.getPriority(), 100);
   }

   @Test(dependsOnMethods = {"updateTest", "createTest"})
   public void deleteTest()
   {
      HfsDataStoreConf ds = (HfsDataStoreConf) dsManager.get("lambda");
      Assert.assertNotNull(ds);
      dsManager.delete(ds);
      ds = (HfsDataStoreConf) dsManager.get("lambda");
      Assert.assertNull(ds);
   }

}
