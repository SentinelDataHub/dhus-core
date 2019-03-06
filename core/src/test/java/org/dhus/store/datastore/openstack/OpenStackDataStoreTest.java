package org.dhus.store.datastore.openstack;

import org.dhus.Product;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.ReadOnlyDataStoreException;
import org.easymock.EasyMock;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import fr.gael.dhus.util.TestContextLoader;

@ContextConfiguration(
      locations = "classpath:fr/gael/dhus/spring/context-test.xml", 
      loader = TestContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OpenStackDataStoreTest extends AbstractTestNGSpringContextTests
{

   private OpenStackDataStore openstack; 

   @BeforeClass
   void init()
   {
      openstack = new OpenStackDataStore(
            "name", 
            "provider",
            "identity", 
            "credential", 
            "url", 
            "container", 
            "region", 
            true, // read-only
            5, -1, 0, false);
   }
   
   // test set(uuid, Product)
   @Test(expectedExceptions = ReadOnlyDataStoreException.class)
   public void testSet() throws DataStoreException
   {
      Product product = EasyMock.createMock(Product.class);
      openstack.set("6b95f536-34e9-415e-9e3d-cd5a2f6429df", product);
   }

}
