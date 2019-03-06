/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018 GAEL Systems
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
package fr.gael.dhus.database.object.config.source;

import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestSource
{
   @Test
   public void concurrentDownload()
   {
      Source source = new Source();
      Assert.assertEquals(source.concurrentDownload(), 0);

      String calculatorId = UUID.randomUUID().toString();
      Assert.assertTrue(source.generateBandwidthCalculator(calculatorId));
      Assert.assertEquals(source.concurrentDownload(), 1);

      source.removeBandwidthCalculator(calculatorId);
      Assert.assertEquals(source.concurrentDownload(), 0);
   }

   @Test(timeOut = 10_000)
   public void downloadBandwidth()
   {
      Source source = new Source();
      Assert.assertEquals(source.getBandwidth(), -1);

      long transferredBytes1 = 1024;
      String calculator1 = UUID.randomUUID().toString();
      Assert.assertTrue(source.generateBandwidthCalculator(calculator1));

      // populate 10 times to compute bandwidth, otherwise bandwidth is not computed (equals -1)
      source.populateBandwidthCalculator(calculator1, transferredBytes1);
      source.populateBandwidthCalculator(calculator1, transferredBytes1);
      source.populateBandwidthCalculator(calculator1, transferredBytes1);
      source.populateBandwidthCalculator(calculator1, transferredBytes1);
      source.populateBandwidthCalculator(calculator1, transferredBytes1);
      source.populateBandwidthCalculator(calculator1, transferredBytes1);
      source.populateBandwidthCalculator(calculator1, transferredBytes1);
      source.populateBandwidthCalculator(calculator1, transferredBytes1);
      source.populateBandwidthCalculator(calculator1, transferredBytes1);
      Assert.assertEquals(source.getBandwidth(), -1);
      Assert.assertEquals(source.getCalculatedBandwidth(calculator1), -1);

      source.populateBandwidthCalculator(calculator1, transferredBytes1);
      Assert.assertEquals(source.getCalculatedBandwidth(calculator1), transferredBytes1);
      Assert.assertEquals(source.getBandwidth(), transferredBytes1);

      long transferredBytes2 = 512;
      String calculator2 = UUID.randomUUID().toString();
      Assert.assertTrue(source.generateBandwidthCalculator(calculator2));
      source.populateBandwidthCalculator(calculator2, transferredBytes2);
      source.populateBandwidthCalculator(calculator2, transferredBytes2);
      source.populateBandwidthCalculator(calculator2, transferredBytes2);
      source.populateBandwidthCalculator(calculator2, transferredBytes2);
      source.populateBandwidthCalculator(calculator2, transferredBytes2);
      source.populateBandwidthCalculator(calculator2, transferredBytes2);
      source.populateBandwidthCalculator(calculator2, transferredBytes2);
      source.populateBandwidthCalculator(calculator2, transferredBytes2);
      source.populateBandwidthCalculator(calculator2, transferredBytes2);
      source.populateBandwidthCalculator(calculator2, transferredBytes2);
      Assert.assertEquals(source.getCalculatedBandwidth(calculator2), transferredBytes2);

      long expectedBandwidth = (transferredBytes1 + transferredBytes2) / 2;
      Assert.assertEquals(source.getBandwidth(), expectedBandwidth);

      source.removeBandwidthCalculator(calculator1);
      source.removeBandwidthCalculator(calculator2);
   }

   @Test(expectedExceptions = {IllegalArgumentException.class})
   public void removeCalculator()
   {
      Source source = new Source();
      String calculatorId = UUID.randomUUID().toString();
      Assert.assertTrue(source.generateBandwidthCalculator(calculatorId));
      source.removeBandwidthCalculator(calculatorId);
      source.populateBandwidthCalculator(calculatorId, 1024);
   }
}
