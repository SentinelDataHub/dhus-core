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
package org.dhus.scanner.service;

import fr.gael.dhus.datastore.scanner.Scanner;
import fr.gael.dhus.datastore.scanner.ScannerException;

import java.util.List;

import org.dhus.scanner.config.ScannerInfo;

/**
 * Service to manager scanners.
 */
public interface ScannerService
{
   /**
    * Get an instance of scanner.
    *
    * @param scannerID scanner identifier
    * @return an instance of Scanner object that represent an live scanner (and not the config)
    */
   Scanner getScanner(long scannerID);

   /**
    * Get a list of all known scanners.
    *
    * @return a non null possibly empty list of scanner instances
    */
   public List<Scanner> listScanners();

   /**
    * Create a scanner based on configuration
    *
    * @param scannerConfiguration scanner configuration object
    * @return a new instance of scanner
    * @throws ScannerException could not create a scanner
    */
   Scanner createScanner(ScannerInfo scannerConfiguration) throws ScannerException;

   /**
    * Update a scanner based on configuration
    *
    * @param scannerConfiguration scanner configuration object
    * @return an updated instance of scanner
    * @throws ScannerException could not update a scanner
    */
   Scanner updateScanner(ScannerInfo scannerConfiguration) throws ScannerException;

   /**
    * Remove a scanner by ID
    *
    * @param scannerID scanner identifier
    * @throws ScannerException could not delete a scanner
    */
   void deleteScanner(long scannerID) throws ScannerException;

   /**
    * Start a scanner by ID
    *
    * @param scannerID scanner identifier
    * @return A message indicating whether the scanner is started or not
    * @throws ScannerException could not start a scanner
    */
   String startScanner(long scannerID) throws ScannerException;

   /**
    * Stop a scanner by ID
    *
    * @param scannerID scanner identifier
    * @return A message indicating whether the scanner is stopped or not
    * @throws ScannerException could not stop a scanner
    */
   String stopScanner(long scannerID) throws ScannerException;

   /**
    * Get the next possible ID of scanner
    *
    * @return the next possible ID
    */
   long getNextScannerID();
}
