/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2015,2017 GAEL Systems
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

public class ScannerException extends Exception
{
   private static final long serialVersionUID = -1023764626340271980L;

   public ScannerException()
   {
      super();
   }

   public ScannerException(String message)
   {
      super(message);
   }

   public ScannerException(String message, Throwable cause)
   {
      super(message, cause);
   }

   public static class ScannerNotFoundException extends ScannerException
   {
      private static final long serialVersionUID = 1L;

      public ScannerNotFoundException(Long scannerId)
      {
         super("Scanner #" + scannerId + " not found");
      }
   }

   public static class ScannerAlreadyRunningException extends ScannerException
   {
      private static final long serialVersionUID = 1L;

      public ScannerAlreadyRunningException(Long scannerId)
      {
         super("Scanner #" + scannerId + " already running");
      }
   }
}
