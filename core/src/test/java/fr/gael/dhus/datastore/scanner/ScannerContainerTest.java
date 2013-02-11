/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018,2019 GAEL Systems
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
package fr.gael.dhus.datastore.scanner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.dhus.scanner.ScannerContainer;
import org.dhus.scanner.config.ScannerInfo;

import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ScannerContainerTest
{
   @InjectMocks
   ScannerContainer scannerContainer;

   @BeforeMethod
   public void init()
   {
      MockitoAnnotations.initMocks(this);
   }

   @Test
   public void processScan() throws ScannerException
   {
      Scanner scanner = getScannerMock(1L, ScannerStatus.STATUS_OK);
      scannerContainer.addScanner(scanner);
      scannerContainer.processScan(1L);
      verify(scanner, times(1)).processScan();
   }

   private Scanner getScannerMock(long ID, String scannerStatus)
   {
      ScannerInfo config = mock(ScannerInfo.class);
      ScannerStatus status = mock(ScannerStatus.class);
      Scanner scanner = mock(FileScanner.class);
      when(scanner.getConfig()).thenReturn(config);
      when(scanner.getStatus()).thenReturn(status);
      when(config.getId()).thenReturn(ID);
      when(status.getStatus()).thenReturn(scannerStatus);
      return scanner;
   }
}
