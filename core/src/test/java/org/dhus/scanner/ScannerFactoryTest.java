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

import org.testng.Assert;
import org.testng.annotations.Test;

public class ScannerFactoryTest
{

   @Test
   public void getScannerSupport()
   {
      String[] supported_classes = ScannerFactory.getDefaultCortexSupport();
      // http://www.gael.fr/test#product1 and http://www.gael.fr/test#product2
      // plus the ones defined by DHuS: http://www.gael.fr/drb#tgz,
      // http://www.gael.fr/drb#gzip, http://www.gael.fr/drb#zip,
      // http://www.gael.fr/drb#tar.

      // Scanner support is automatically computed according to the
      // upcoming ontologies. by default scanner at least contains
      // http://www.gael.fr/drb#zip, so support list shall never be empty.
      Assert.assertTrue(supported_classes.length > 1, "Missing supports");
   }
}
