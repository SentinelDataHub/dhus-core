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
package org.dhus.scanner.service;

import fr.gael.dhus.datastore.scanner.Scanner;
import fr.gael.dhus.datastore.scanner.ScannerException;
import fr.gael.dhus.datastore.scanner.ScannerStatus;
import fr.gael.dhus.system.config.ConfigurationManager;
import java.util.Collections;

import java.util.List;
import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.scanner.ScannerContainer;
import org.dhus.scanner.ScannerFactory;
import org.dhus.scanner.config.ScannerConfigurationManager;
import org.dhus.scanner.config.ScannerInfo;
import org.dhus.scanner.schedule.ScannerScheduler;

import org.quartz.SchedulerException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("scannerService")
public class ScannerServiceImpl implements ScannerService
{
   private static final Logger LOGGER = LogManager.getLogger();

   @Autowired
   private ConfigurationManager cfgManager;
   @Autowired
   private ScannerContainer scannerContainer;
   @Autowired
   private ScannerScheduler scannerScheduler;

   private ScannerConfigurationManager scannerConfigurationManager;

   @PostConstruct
   private void init() throws SchedulerException
   {

      scannerConfigurationManager = cfgManager.getScannerManager();

      scannerConfigurationManager.getScanners()
            .forEach(sc ->
            {
               Scanner scanner = ScannerFactory.getScanner(sc);
               scannerContainer.addScanner(scanner);
            });

      scannerConfigurationManager.getScanners().stream()
            .filter(sc -> sc.getCron() != null && sc.getCron().isActive())
            .forEach(sc ->
            {
               try
               {
                  scannerScheduler.scheduleScanner(sc);
               }
               catch (SchedulerException e)
               {
                  LOGGER.error("Error trying to schedule scanner {}", sc.getId());
               }
            });
      scannerScheduler.start();
   }

   @Override
   public List<Scanner> listScanners()
   {
      return Collections.<Scanner>unmodifiableList(scannerContainer.getScanners());
   }

   @Override
   public Scanner createScanner(ScannerInfo si) throws ScannerException
   {
      if (si.getUrl() == null || si.getUrl().isEmpty())
      {
         throw new ScannerException.BadScannerConfigException();
      }

      Scanner existingScanner = getScanner(si.getId());
      if (null != existingScanner)
      {
         throw new ScannerException("Scanner with the same ID already exist");
      }

      si.setId(getNextScannerID());

      Scanner scanner;
      scanner = ScannerFactory.getScanner(si);
      scannerConfigurationManager.create(si);

      scannerContainer.addScanner(scanner);

      if (si.getCron() != null && si.getCron().isActive())
      {
         try
         {
            scannerScheduler.scheduleScanner(si);
         }
         catch (SchedulerException e)
         {
            LOGGER.error("Exception while scheduling scanner {}", si.getId());
         }
      }

      return scanner;
   }

   public void delete(ScannerInfo si)
   {
      scannerConfigurationManager.delete(si.getId());

      try
      {
         scannerScheduler.unscheduleScanner(si);
      }
      catch (SchedulerException e)
      {
         LOGGER.error("Exception while unschelduling scanner {}", si.getId());
      }
   }

   @Override
   public Scanner updateScanner(ScannerInfo scannerInfo) throws ScannerException
   {
      Scanner oldScanner = getScanner(scannerInfo.getId());

      if (null == oldScanner)
      {
         throw new ScannerException.ScannerNotFoundException(scannerInfo.getId());
      }

      if (ScannerStatus.STATUS_RUNNING.equalsIgnoreCase(oldScanner.getStatus().getStatus()))
      {
         throw new ScannerException.CannotUpdateRunningScannerException();
      }

      scannerContainer.removeScanner(scannerInfo.getId());
      Scanner scanner = ScannerFactory.getScanner(scannerInfo);
      scannerContainer.addScanner(scanner);

      scannerConfigurationManager.update(scannerInfo, true);
      try
      {
         if (scannerInfo.getCron() != null && scannerInfo.getCron().isActive())
         {
            scannerScheduler.scheduleScanner(scannerInfo);
         }
         else
         {
            scannerScheduler.unscheduleScanner(scannerInfo);
         }
      }
      catch (SchedulerException e)
      {
         LOGGER.error("Exception while updating scheduler for scanner {}", scannerInfo.getId());
      }

      return scanner;
   }

   @Override
   public Scanner getScanner(long scannerID)
   {
      return scannerContainer.getScanner(scannerID);
   }

   @Override
   public void deleteScanner(long scannerID) throws ScannerException
   {
      Scanner scanner = scannerContainer.getScanner(scannerID);

      if (null == scanner)
      {
         throw new ScannerException.ScannerNotFoundException(scannerID);
      }

      if (ScannerStatus.STATUS_RUNNING.equalsIgnoreCase(scanner.getStatus().getStatus()))
      {
         throw new ScannerException.CannotUpdateRunningScannerException();
      }

      scannerConfigurationManager.delete(scannerID);
      scannerContainer.removeScanner(scannerID);

      try
      {
         scannerScheduler.unscheduleScanner(scanner.getConfig());
      }
      catch (SchedulerException e)
      {
         LOGGER.error("Exception while unschelduling scanner {}", scannerID);
      }
   }

   @Override
   public String startScanner(long scannerId)
   {
      try
      {
         scannerContainer.processScan(scannerId);
      }
      catch (ScannerException.ScannerNotFoundException e)
      {
         throw new RuntimeException("Scanner not found " + e.getMessage());
      }
      catch (ScannerException e)
      {
         throw new RuntimeException("Scanner exception occured " + e.getMessage());
      }
      return String.format("Scanner #%d started.", scannerId);
   }

   @Override
   public String stopScanner(long scannerId)
   {

      if (scannerContainer.stopScan(scannerId))
      {
         return String.format("Scanner #%d stopped.", scannerId);
      }
      else
      {
         return String.format("Scanner #%d failed to stop, Scanner is not initialized (retry stop later).", scannerId);
      }
   }

   @Override
   public long getNextScannerID()
   {
      long max = scannerContainer.getScanners().stream()
            .mapToLong(s -> s.getConfig().getId())
            .max()
            .orElse(0);

      return max + 1;
   }
}
