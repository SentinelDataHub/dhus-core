package org.dhus.store.datastore.hfs;

import fr.gael.dhus.olingo.v1.ExpectedException;
import fr.gael.dhus.util.TestContextLoader;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import org.dhus.Product;
import org.dhus.ProductConstants;
import org.dhus.store.datastore.DataStoreException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@ContextConfiguration(
        locations = "classpath:fr/gael/dhus/spring/context-test.xml",
        loader = TestContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class IdentityHfsDataStoreTest extends AbstractTransactionalTestNGSpringContextTests{

    private static final String TEMP_DIRECTORY_PATH =
            System.getProperty("java.io.tmpdir") + File.separator + "incoming";

    private IdentityHfsDataStore hfsDataStore;

    private File tmp;

    @BeforeClass
    void init()
    {
        HfsManager hfs = new HfsManager(TEMP_DIRECTORY_PATH, 5);
        hfsDataStore = new IdentityHfsDataStore("identity-hfs-test", hfs, false);

    }

    @AfterClass
    void exit()
    {
        FileUtils.deleteQuietly(new File(hfsDataStore.getHfs().getPath()));
    }

    @Test
    public void seProductStaysInInbox() {
        Assert.assertNotNull(hfsDataStore);

        String id = UUID.randomUUID().toString();
        try
        {
            File tmp = File.createTempFile("datastore", ".zip");
            tmp.deleteOnExit();
            final HfsProduct hfsProduct = new HfsProduct(tmp);

            hfsDataStore.set(id, hfsProduct);
            Product product = hfsDataStore.get(id);
            File productImpl = product.getImpl(File.class);
//          Final product is the same as the original
            Assert.assertEquals(productImpl.getCanonicalPath(), tmp.getPath());
        }
        catch (DataStoreException e)
        {
            Assert.fail("An exception occurred:", e);
        }catch (IOException e)
        {
            Assert.fail("An exception occurred:", e);
        }
    }

    @Test
    public void setImageGoesToIncoming() {
        Assert.assertNotNull(hfsDataStore);

        String id = UUID.randomUUID().toString();
        try
        {
            File tmp = File.createTempFile("datastore", ".jpg");
            tmp.deleteOnExit();
            final HfsProduct hfsProduct = new HfsProduct(tmp);
            hfsProduct.setProperty(ProductConstants.PRODUCT_TYPE_PROPERTY, ProductConstants.IMAGE_TYPE);
            hfsDataStore.set(id, hfsProduct);
            Product product = hfsDataStore.get(id);

            File productImpl = product.getImpl(File.class);

//          Final product is the incoming folder defined in the HfsManager
            Assert.assertTrue(productImpl.getPath().startsWith(hfsDataStore.hfs.getPath()));
        }
        catch (DataStoreException e)
        {
            Assert.fail("An exception occurred:", e);
        }catch (IOException e)
        {
            Assert.fail("An exception occurred:", e);
        }
    }

    @Test
    public void testMove() {
        Assert.assertNotNull(hfsDataStore);

        String id = UUID.randomUUID().toString();
        try
        {
            File tmp = File.createTempFile("datastore", ".zip");
            tmp.deleteOnExit();
            final HfsProduct hfsProduct = new HfsProduct(tmp);
//            move is not supported and should raise a DataStoreException
            hfsDataStore.move(id, hfsProduct);
            Assert.fail();
        }
        catch (DataStoreException e)
        {
//          Expected output
            return;
        }catch (IOException e)
        {
            Assert.fail("An exception occurred:", e);
        }
    }
}