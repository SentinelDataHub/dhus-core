/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2014,2015,2017,2018 GAEL Systems
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
package org.dhus.scanner;

import fr.gael.dhus.database.object.config.scanner.FileScannerConf;
import fr.gael.dhus.datastore.scanner.Scanner;
import fr.gael.dhus.datastore.scanner.URLExt;
import fr.gael.dhus.datastore.scanner.listener.Listener;
import fr.gael.drbx.cortex.DrbCortexItemClass;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import org.dhus.scanner.config.ScannerInfo;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class FileScannerTest
{

   String[] classes_strings =
   {
      "http://www.gael.fr/test#product1", "http://www.gael.fr/test#product2",
   };
   List<DrbCortexItemClass> classes;
   File tmpDir;

   @BeforeClass
   public void before() throws IOException
   {
      classes = new ArrayList<>();
      for (String cl: classes_strings)
      {
         try
         {
            classes.add(DrbCortexItemClass.getCortexItemClassByName(cl));
         }
         catch (NullPointerException ex)
         {
            ex.printStackTrace();
         }
      }

      tmpDir = File.createTempFile("scanner", ".test");
      String path = tmpDir.getPath();
      tmpDir.delete();
      tmpDir = new File(path);
      tmpDir.mkdirs();

      // empty XML files makes drb producing many error logs on standard output.
      File file = new File(tmpDir, "s1-level-1-calibration.test");
      FileUtils.touch(file);

      file = new File(tmpDir, "s1-object-types.titi");
      FileUtils.touch(file);

      file = new File(tmpDir, "GOM_EXT_2PNPDE20070312_232536_000000542056_00202_26308_1271.N1");
      FileUtils.touch(file);

      file = new File(tmpDir, "S1A_IW_SLC__1SDV_20141003T054235_20141003T054304_002661_002F66_D5C8.SAFE");
      file.mkdir();

      file = new File(tmpDir, "S1A_EW_GRDH_1SSH_20120101T022934_20120101T022945_001770_000001_AF02.SAFE");
      file.mkdir();

      file = new File(file, "manifest.safe");
      FileUtils.touch(file);

      file = new File(tmpDir, "manifest.safe");
      FileUtils.touch(file);
   }

   @Test
   public void runFileScanner() throws InterruptedException
   {
      ScannerInfo config = new FileScannerConf();
      config.setUrl(tmpDir.getPath());
      Scanner scanner = ScannerFactory.getScanner(config);
      scanner.getConfig().setId(1L);
      scanner.setSupportedClasses(classes);
      scanner.getScanList().simulate(false);

      // Remove all listener
      List<Listener<URLExt>> listener = new ArrayList<>(scanner.getScanList().getListeners());
      listener.stream().forEach(l -> scanner.getScanList().removeListener(l));

      scanner.scan();

      Assert.assertTrue(scanner.getScanList().size() > 0, "No item found.");

      // Expected result is 2: only S1[AB]_ pattern signature is supported.
      Assert.assertEquals(scanner.getScanList().size(), 2, "Wrong number of items found.");
   }

   @AfterClass
   public void after()
   {
      FileUtils.deleteQuietly(tmpDir);
   }
}
