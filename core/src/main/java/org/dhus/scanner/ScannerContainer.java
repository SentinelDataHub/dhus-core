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
package org.dhus.scanner;

import fr.gael.dhus.datastore.scanner.Scanner;
import fr.gael.dhus.datastore.scanner.ScannerException;
import fr.gael.dhus.datastore.scanner.ScannerException.ScannerNotFoundException;
import fr.gael.dhus.datastore.scanner.ScannerStatus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.stereotype.Component;

/**
 * Keep track of all scanner.
 */
@Component("scannerContainer")
public class ScannerContainer
{
   public static final SimpleDateFormat SDF = new SimpleDateFormat("EEEE dd MMMM yyyy - HH:mm:ss", Locale.ENGLISH);

   private static final Logger LOGGER = LogManager.getLogger();

   private final ConcurrentHashMap<Long, Scanner> scanners = new ConcurrentHashMap<>();

   public List<Scanner> getScanners()
   {
      List<Scanner> snapshot = new ArrayList<>(scanners.values());
      return Collections.unmodifiableList(snapshot);
   }

   public synchronized void addScanner(Scanner scanner)
   {
      this.scanners.put(scanner.getConfig().getId(), scanner);
   }

   public synchronized Scanner getScanner(long id)
   {
      return this.scanners.get(id);
   }

   public synchronized Scanner removeScanner(long id)
   {
      return this.scanners.remove(id);
   }

   /**
    * Process passed file scanner in a separate thread.
    * If the requested scanner is already running (from schedule or UI), it will not restart.
    *
    * @param scan_id scanner identifier
    * @throws ScannerException when scanner cannot be started
    */
   public synchronized void processScan(final Long scan_id) throws ScannerException
   {
      Scanner scanner = scanners.get(scan_id);

      if (null == scanner)
      {
         throw new ScannerNotFoundException(scan_id);
      }

      if (ScannerStatus.STATUS_RUNNING.equalsIgnoreCase(scanner.getStatus().getStatus()))
      {
         LOGGER.info("Scanner {} is already running", scan_id);
         return;
      }

      // perform scan on scanner
      scanner.processScan();
   }

   /**
    * Stops a processScan.
    *
    * @param scan_id scanner identifier
    * @return {@code false} if could not stop (scanner is not initialised)
    */
   public synchronized boolean stopScan(final Long scan_id)
   {
      Scanner scanner = scanners.get(scan_id);

      if (scanner == null || !ScannerStatus.STATUS_RUNNING.equalsIgnoreCase(scanner.getStatus().getStatus()))
      {
         LOGGER.warn("Scanner already stopped");
         return true;
      }

      scanner.getStatus().addStatusMessage("Interrupted: waiting for ongoing processes to end...");

      scanner.stop();
      LOGGER.info("Scanner stopped");
      return true;
   }
}
