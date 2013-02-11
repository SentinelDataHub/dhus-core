/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019 GAEL Systems
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
package org.dhus.test;

import fr.gael.dhus.messaging.mail.MailServer;
import fr.gael.dhus.search.SolrDao;

import org.easymock.EasyMock;

/**
 * Factory methods to create bean mocks.
 */
public class MockFactory
{
   public static SolrDao createSolrDaoMock()
   {
      try
      {
         SolrDao res = EasyMock.niceMock(SolrDao.class);
         EasyMock.checkOrder(res, false);
         EasyMock.replay(res);
         return res;
      }
      catch (Throwable suppressed) {} // configuring a mock does not throw
      return null;
   }

   public static MailServer createMailServerMock()
   {
      MailServer res = EasyMock.niceMock(MailServer.class);
      EasyMock.replay(res);
      return res;
   }
}
