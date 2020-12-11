/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2018 GAEL Systems
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

import fr.gael.drbx.cortex.DrbCortexItemClass;
import fr.gael.dhus.datastore.scanner.listener.AsynchronousLinkedList;

import java.util.List;

import org.dhus.scanner.config.ScannerInfo;

/**
 * Scanner aims to scan uri in order to retrieve data.
 */
public interface Scanner
{
   void processScan();

   void stop();

   /**
    * Scan
    *
    * @return number of products found, or -1 if not implemented
    * @throws InterruptedException is user stop called.
    */
   int scan() throws InterruptedException;

   boolean isStopped();

   AsynchronousLinkedList<URLExt> getScanList();

   void setSupportedClasses(List<DrbCortexItemClass> supported);

   /**
    * Force the navigation through the scanned directories even if the directory has been recognised.
    * (defaults to false).
    *
    * @param force
    */
   void setForceNavigate(boolean force);

   boolean isForceNavigate();

   /**
    * Defines the user pattern to restricts recursive search among scanned directories.
    * Passed pattern string in not stored in this class, but automatically compiled with {@link Pattern#compile(String)}.
    * Once called, and even if this method throws exception, The stored pattern is reset.
    *
    * @param pattern to set
    */
   void setUserPattern(String pattern);

   ScannerStatus getStatus();

   void setStatus(ScannerStatus status);

   ScannerInfo getConfig();

   void setConfig(ScannerInfo config);
}
