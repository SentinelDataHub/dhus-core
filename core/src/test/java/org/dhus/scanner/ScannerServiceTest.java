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
package org.dhus.scanner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.gael.dhus.database.object.config.scanner.FileScannerConf;
import fr.gael.dhus.datastore.scanner.FileScanner;
import fr.gael.dhus.datastore.scanner.Scanner;
import fr.gael.dhus.datastore.scanner.ScannerException;
import fr.gael.dhus.datastore.scanner.ScannerStatus;
import fr.gael.dhus.system.config.ConfigurationManager;

import org.dhus.scanner.config.ScannerConfigurationManager;
import org.dhus.scanner.config.ScannerInfo;
import org.dhus.scanner.schedule.ScannerScheduler;
import org.dhus.scanner.service.ScannerServiceImpl;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.quartz.SchedulerException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ScannerServiceTest
{
   @InjectMocks
   ScannerServiceImpl service;

   @Mock
   private ScannerContainer container;
   @Mock
   private ConfigurationManager configurationManager;
   @Mock
   private ScannerScheduler scheduler;

   private ScannerConfigurationManager scm = mock(ScannerConfigurationManager.class);

   @BeforeMethod
   public void init()
   {
      MockitoAnnotations.initMocks(this);
      when(configurationManager.getScannerManager()).thenReturn(scm);
   }

   @Test
   public void create() throws SchedulerException, ScannerException
   {
      ScannerInfo config = getConfig();
      when(scm.create(config)).thenReturn(getConfigWithID());
      Scanner scanner = null;

      scanner = service.createScanner(config);

      verify(container, times(1)).addScanner(scanner);
      verify(scm, times(1)).create(config);
      verify(scheduler, times(1)).scheduleScanner(config);
   }

   @Test
   public void update() throws SchedulerException, ScannerException
   {
      ScannerInfo config = getConfigWithID(10L);
      Scanner scanner = mock(FileScanner.class);
      when(scanner.getStatus()).thenReturn(getScannerStatus(ScannerStatus.STATUS_ADDED));
      when(container.getScanner(10L)).thenReturn(scanner);

      Scanner result = service.updateScanner(config);

      verify(container, times(1)).getScanner(10L);
      verify(scm, times(1)).update(config, true);
      verify(scheduler, times(1)).scheduleScanner(config);
   }

   @Test(expectedExceptions = ScannerException.CannotUpdateRunningScannerException.class)
   public void updateFailWhenScannerRunning() throws SchedulerException, ScannerException
   {
      ScannerInfo config = getConfigWithID(10L);
      Scanner scanner = mock(FileScanner.class);
      when(scanner.getStatus()).thenReturn(getScannerStatus(ScannerStatus.STATUS_RUNNING));
      when(container.getScanner(10L)).thenReturn(scanner);

      service.updateScanner(config);
   }

   @Test(expectedExceptions = ScannerException.ScannerNotFoundException.class)
   public void updateFailWhenScannerNotFound() throws SchedulerException, ScannerException
   {
      ScannerInfo config = getConfigWithID(10L);
      when(container.getScanner(10L)).thenReturn(null);

      service.updateScanner(config);
   }

   @Test(expectedExceptions = ScannerException.ScannerNotFoundException.class)
   public void deleteFailWhenScannerNotFound() throws SchedulerException, ScannerException
   {
      when(container.getScanner(10L)).thenReturn(null);

      service.deleteScanner(10L);
   }

   @Test
   public void delete() throws SchedulerException, ScannerException
   {
      ScannerInfo config = getConfigWithID(10L);
      Scanner scanner = mock(FileScanner.class);
      when(scanner.getConfig()).thenReturn(config);
      when(scanner.getStatus()).thenReturn(getScannerStatus(ScannerStatus.STATUS_ADDED));
      when(container.getScanner(10L)).thenReturn(scanner);
      when(container.removeScanner(10L)).thenReturn(scanner);

      service.deleteScanner(10L);

      verify(container, times(1)).removeScanner(10L);
      verify(container, times(1)).getScanner(10L);
      verify(scheduler, times(1)).unscheduleScanner(config);
   }

   @Test(expectedExceptions = ScannerException.CannotUpdateRunningScannerException.class)
   public void deleteFailWhenRunning() throws SchedulerException, ScannerException
   {
      Scanner scanner = mock(FileScanner.class);
      when(scanner.getStatus()).thenReturn(getScannerStatus(ScannerStatus.STATUS_RUNNING));
      when(container.getScanner(10L)).thenReturn(scanner);

      service.deleteScanner(10L);
   }

   private ScannerStatus getScannerStatus(String statusCode)
   {
      ScannerStatus status = new ScannerStatus();
      status.setStatus(statusCode);
      return status;
   }

   private ScannerInfo getConfig()
   {
      ScannerInfo info = new FileScannerConf();
      info.setUrl("file://example.data");
      info.setPattern("data");
      info.setActive(true);
      info.setUsername("username");
      return info;
   }

   private ScannerInfo getConfigWithID()
   {
      ScannerInfo info = getConfig();
      info.setId((long) Math.floor(Math.random() * 100));
      return info;
   }

   private ScannerInfo getConfigWithID(long id)
   {
      ScannerInfo info = getConfig();
      info.setId(id);
      return info;
   }
}
