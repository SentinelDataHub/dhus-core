/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2014-2018 GAEL Systems
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
package org.dhus.scanner.entity;

import com.google.common.collect.ImmutableList;
import fr.gael.dhus.datastore.scanner.AbstractScanner;
import fr.gael.dhus.datastore.scanner.Scanner;
import fr.gael.drb.DrbDefaultMutableNode;
import fr.gael.drb.DrbNode;
import fr.gael.drbx.cortex.DrbCortexItemClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AbstractScannerTest
{
   private static final Logger LOGGER = LogManager.getLogger();

   final DrbNode[] testItem =
   {
      new DrbDefaultMutableNode("MyOwnTestItem.1"),
      new DrbDefaultMutableNode("MyOwnTestItem.2"),
      new DrbDefaultMutableNode("MyOwnTestItem.3"),
      new DrbDefaultMutableNode("MyOwnTestItem.tar"),
      new DrbDefaultMutableNode("GOM_MM__0PNPDE20041204_012208_000007492032_00360_14442_0031.N1"),
      new DrbDefaultMutableNode("GOM_NL__0PNPDE20041215_033131_000053822033_00018_14601_1448.N1"),
      new DrbDefaultMutableNode("GOM_TRA_1PNPDE20041113_230620_000000712032_00073_14155_0012.N1"),
      new DrbDefaultMutableNode("S1A_IW_GRDH_1SDV_20141009T155724_20141009T155749_002755_003184_8C3F.zip"),
      new DrbDefaultMutableNode("S1A_EW_GRDH_1SDH_20141008T090011_20141008T090027_002736_003117_107F.zip"),
      new DrbDefaultMutableNode("S1A_S4_RAW__0SSV_20141004T062011_20141004T062031_002676_002FBE_3B30.zip"),
      new DrbDefaultMutableNode("S1A_S3_RAW__0SSV_20141003T104036_20141003T104106_002664_002F79_D4F3.zip"),
      new DrbDefaultMutableNode("S1A_IW_RAW__0SSV_20141004T212603_20141004T212640_002685_002FF6_E94F.zip"),
      new DrbDefaultMutableNode("S1A_IW_SLC__1SDV_20141004T155003_20141004T155031_002682_002FE4_20EA.zip"),
      new DrbDefaultMutableNode("S1A_IW_SLC__1SDV_20141003T164823_20141003T164850_002668_002F8B_47F1.zip"),
   };

   /* Fonctionne sous eclipse mais pas en ligne de commande "mvn test"
    * TBC
    * */
   @Test
   public void matchesAll() throws InterruptedException
   {
      LOGGER.info("match all list :");
      Scanner scanner = getTestScanner();
      scanner.setUserPattern(null);
      Assert.assertEquals(scanner.scan(), testItem.length);
   }

   @Test
   public void matchesOne() throws InterruptedException
   {
      LOGGER.info("matchOne (pattern=\".*\\.tar\") :");
      Scanner scanner = getTestScanner();
      scanner.setUserPattern(".*\\.tar");
      Assert.assertEquals(scanner.scan(), 1);
   }

   @Test
   public void matchesGomos() throws InterruptedException
   {
      LOGGER.info("match GOMOS only (pattern=\"GOM.*\\.N1\") :");
      Scanner scanner = getTestScanner();
      scanner.setUserPattern("GOM.*\\.N1");
      Assert.assertEquals(scanner.scan(), 3);
   }

   @Test
   public void matchesThree() throws InterruptedException
   {
      LOGGER.info("match tree products (pattern=\".*\\.[0-9]{1}\") :");
      Scanner scanner = getTestScanner();
      scanner.setUserPattern(".*\\.[0-9]*");
      Assert.assertEquals(scanner.scan(), 3);
   }

   @Test
   public void matchesWrongPattern() throws InterruptedException
   {
      LOGGER.info("match wrong (pattern=\"*][\") :");
      Scanner scanner = getTestScanner();

      scanner.setUserPattern("*][");

      Assert.assertEquals(scanner.scan(), testItem.length);
   }

   @Test
   public void matchesAllSentinels() throws InterruptedException
   {
      LOGGER.info("match \"S1A_.*\\.zip\"");
      Scanner scanner = getTestScanner();
      scanner.setUserPattern("S1A_.*\\.zip");
      Assert.assertEquals(scanner.scan(), 7);
   }

   @Test
   public void matchesAllSlcGdmSentinels() throws InterruptedException
   {
      LOGGER.info("match \"S1[AB]_\\p{Upper}{2}_(SLC|GRDM).*\"");
      Scanner scanner = getTestScanner();
      scanner.setUserPattern("S1[AB]_\\p{Upper}{2}_(SLC|GRDM).*");
      Assert.assertEquals(scanner.scan(), 2);
   }

   @Test
   public void matchesAllEwSlcGdSentinels() throws InterruptedException
   {
      LOGGER.info("match \"S1A_EW_(SLC_|GRD(F|H|M))_.*\"");
      Scanner scanner = getTestScanner();
      scanner.setUserPattern("S1A_EW_(SLC_|GRD(F|H|M))_.*");
      Assert.assertEquals(scanner.scan(), 1);
   }

   public Scanner getTestScanner()
   {
      Scanner scanner = new AbstractScanner(false)
      {
         @Override
         public int scan()
         {
            int result_count = 0;
            // Use scan() method to launch test :-)
            for (DrbNode node: testItem)
            {
               String pattern = getUserPattern() == null ? "" : getUserPattern().pattern();

               if (matches(node))
               {
                  result_count++;
                  LOGGER.info("  + \"" + pattern + "\" Node " + node.getName() + " match !");
               }
               else
               {
                  LOGGER.info("  - \"" + pattern + "\" Node " + node.getName() + " Not match scanner.");
               }
            }
            return result_count;
         }
      };
      scanner.setSupportedClasses(ImmutableList.of(DrbCortexItemClass
            .getCortexItemClassByName("http://www.gael.fr/drb#item")));
      return scanner;
   }
}
