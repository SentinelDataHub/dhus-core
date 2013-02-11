package fr.gael.dhus.datastore;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.io.Files;

public class HierarchicalDirectoryBuilderTest
{
   private static Log logger = LogFactory.getLog (
      HierarchicalDirectoryBuilderTest.class);

   File tmp;
   @BeforeMethod
   public void init ()
   {
      tmp = Files.createTempDir();
      tmp.mkdirs ();
   }

   @Test 
   public void getDirectory() throws IOException
   {
      HierarchicalDirectoryBuilder db=new HierarchicalDirectoryBuilder(tmp,3,5);
      File f = db.getDirectory(null);
      Assert.assertTrue (f.exists ());
      String fileName = "file";
      f = db.getDirectory (fileName);
      File file = new File (f, fileName);
      file.createNewFile ();
      File f2 = db.getDirectory (fileName);
      Assert.assertNotEquals (f.getAbsolutePath (), f2.getAbsolutePath ());
   }
   
   @Test 
   public void getDirectoryChange() throws IOException
   {
      HierarchicalDirectoryBuilder db=new HierarchicalDirectoryBuilder(tmp,3,5);
      File f = db.getDirectory(null);
      f = db.getDirectory(null);
      for (int i = 0; i < 5; i++)
      {
         File file = new File (f, "file"+i);
         file.createNewFile ();
      }
      File f2 = db.getDirectory(null);
      Assert.assertNotEquals (f.getAbsolutePath (), f2.getAbsolutePath ());
   }
   
   @Test
   public void getDirectoryDepth() throws IOException
   {
      HierarchicalDirectoryBuilder db=new HierarchicalDirectoryBuilder(tmp,3,5);
      for (int j = 0; j < 10; j++)
      {
         File f = db.getDirectory(null);
         f = db.getDirectory(null);
         for (int i = 0; i < 5; i++)
         {
            File file = new File (f, "file"+i);
            file.createNewFile ();
         }
      }
      File f2 = db.getDirectory(null);
      Assert.assertTrue (f2.getAbsolutePath ().contains ("x0/x1"));
   }
     
   @AfterMethod
   public void finalize_me()
   {
      logger.info ("Removing tmp files.");
      FileUtils.deleteQuietly (tmp);
   }
}
