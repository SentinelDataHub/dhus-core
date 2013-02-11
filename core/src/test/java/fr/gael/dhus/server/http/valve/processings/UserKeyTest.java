/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
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
package fr.gael.dhus.server.http.valve.processings;

import fr.gael.dhus.server.http.valve.processings.ProcessingValve.UserSelection;

import org.testng.Assert;
import org.testng.annotations.Test;

public class UserKeyTest
{
   @Test
   public void equalsTest()
   {
      ProcessingInformation pi = new ProcessingInformation("Request");
      pi.setRemoteAddress("123.12.122.33");
      pi.setUsername("user1");

      ProcessingInformation pi1 = new ProcessingInformation("Request");
      pi1.setRemoteAddress("99.99.99.123");
      pi1.setUsername("user1");

      ProcessingInformation pi2 = new ProcessingInformation("Request");
      pi2.setRemoteAddress("99.99.99.123");
      pi2.setUsername("user2");

      ProcessingInformation pi3 = new ProcessingInformation("Request");

      UserKey uk = new UserKey(pi, new UserSelection[]{UserSelection.LOGIN});
      UserKey uk1 = new UserKey(pi1, new UserSelection[]{UserSelection.IP});
      UserKey uk2 = new UserKey(pi2, new UserSelection[]{UserSelection.LOGIN, UserSelection.IP});
      UserKey uk3 = new UserKey(pi3, new UserSelection[]{UserSelection.LOGIN, UserSelection.IP});

      Assert.assertTrue(uk.equals(uk1)); // Compare by LOGIN with same LOGIN only
      Assert.assertFalse(uk.equals(uk2)); // Compare by LOGIN with different LOGIN only

      Assert.assertTrue(uk1.equals(uk2)); // Compare by IP with same IP
      Assert.assertFalse(uk1.equals(uk)); // Compare by IP different IP

      Assert.assertFalse(uk2.equals(uk)); // Compare with different IP and LOGIN
      Assert.assertTrue(uk2.equals(uk2)); // Compare with same IP and LOGIN

      Assert.assertFalse(uk3.equals(uk)); // Compare with different IP and LOGIN
      Assert.assertTrue(uk3.equals(uk3)); // Compare with different IP and LOGIN
      Assert.assertFalse(uk2.equals(uk3)); // Compare with different IP and LOGIN
   }

   @Test()
   public void toStringTest()
   {
      ProcessingInformation pi = new ProcessingInformation("Request");
      pi.setRemoteAddress("123.12.122.33");
      pi.setUsername("user1");

      ProcessingInformation pi1 = new ProcessingInformation("Request");

      UserKey uk = new UserKey(pi, new UserSelection[]
      {
         UserSelection.LOGIN, UserSelection.IP
      });
      UserKey uk1 = new UserKey(pi1, new UserSelection[]
      {
         UserSelection.LOGIN, UserSelection.IP
      });

      Assert.assertEquals(uk.toString(), "LOGIN:user1;IP:123.12.122.33;"); // No Exception
      Assert.assertEquals(uk1.toString(), "LOGIN:null;IP:null;"); // No Exception (null)
   }
}
