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

import com.google.common.collect.ImmutableList;

import fr.gael.dhus.database.object.config.scanner.FtpScannerConf;
import fr.gael.dhus.datastore.scanner.Scanner;
import fr.gael.dhus.datastore.scanner.URLExt;
import fr.gael.dhus.datastore.scanner.listener.Listener;
import fr.gael.drbx.cortex.DrbCortexItemClass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dhus.scanner.config.ScannerInfo;

import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class FtpScannerTest
{
   String[] classes_strings = new String[]
   {
      "http://www.gael.fr/test#product1",
      "http://www.gael.fr/test#product2",
   };

   List<DrbCortexItemClass> classes;
   FakeFtpServer fakeFtpServer;

   @BeforeClass
   protected void startftp() throws Exception
   {
      fakeFtpServer = new FakeFtpServer();
      fakeFtpServer.setServerControlPort(8089);  // use any free port

      FileSystem fileSystem = new UnixFakeFileSystem();
      fileSystem.add(new FileEntry("/data/s1-level-1-calibration.xsd", "<schema/>"));
      fileSystem.add(new FileEntry("/data/s1-object-types.xsd", "<schema/>"));
      fileSystem.add(new FileEntry("/data/GOM_EXT_2PNPDE20070312_232536_000000542056_00202_26308_1271.N1", "GOMOS DATA!"));
      fileSystem.add(new FileEntry("/data/S1A_IW_SLC__1SDV_20141003T054235_20141003T054304_002661_002F66_D5C8.SAFE/manifest.safe", "<XFDU/>"));
      fileSystem.add(new FileEntry("/data/S1A_EW_GRDH_1SSH_20120101T022934_20120101T022945_001770_000001_AF02.SAFE/manifest.safe", "<XFDU/>"));
      fileSystem.add(new FileEntry("/data/manifest.safe", "<XFDU/>"));

      fakeFtpServer.setFileSystem(fileSystem);

      UserAccount userAccount = new UserAccount("user", "password", "/");
      fakeFtpServer.addUserAccount(userAccount);

      fakeFtpServer.start();
   }

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
   }

   @Test
   public void runFtpScanner() throws InterruptedException
   {
      ScannerInfo config = new FtpScannerConf();
      config.setUrl("ftp://localhost:8089/data");
      config.setUsername("user");
      config.setPassword("password");
      Scanner scanner = ScannerFactory.getScanner(config);
      scanner.getConfig().setId(1L);

      // scanner.setSupportedClasses(classes);
      // Scan all the items...
      scanner.setSupportedClasses(ImmutableList.of(DrbCortexItemClass.getCortexItemClassByName("http://www.gael.fr/drb#item")));
      scanner.getScanList().simulate(false);

      // Remove all listener
      List<Listener<URLExt>> listener = new ArrayList<>(scanner.getScanList().getListeners());
      listener.stream().forEach(l -> scanner.getScanList().removeListener(l));

      scanner.scan();

      Assert.assertTrue(scanner.getScanList().size() > 0, "No item found.");
   }

   @AfterClass
   public void stopftp()
   {
      fakeFtpServer.stop();
   }
}
