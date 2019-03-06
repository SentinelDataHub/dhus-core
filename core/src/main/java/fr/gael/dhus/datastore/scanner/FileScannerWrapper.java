/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2015-2017 GAEL Systems
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

import fr.gael.dhus.database.object.config.scanner.ScannerInfo;
import fr.gael.dhus.database.object.config.scanner.ScannerManager;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileScannerWrapper
{
   private static final Logger LOGGER = LogManager.getLogger(FileScannerWrapper.class);

   final Long scannerId;
   final AtomicInteger startCounter;
   final AtomicInteger endCounter;
   final AtomicInteger errorCounter;
   final AtomicInteger cancelCount;
   final AtomicInteger totalProcessed;
   String scannerStatus;
   String scannerMessage;
   String processingErrors = "";

   public FileScannerWrapper(final ScannerInfo persistent_scanner)
   {
      this.startCounter = new AtomicInteger (0);
      this.endCounter = new AtomicInteger (0);
      this.errorCounter = new AtomicInteger (0);
      this.cancelCount = new AtomicInteger (0);
      this.totalProcessed = new AtomicInteger (0);

      this.scannerId = persistent_scanner.getId();
   }

   /**
    * Case of error during processing: informations are accumulated to be
    * displayed to the user.
    */
   public synchronized void error (String origin, Throwable e)
   {
      if ((e!=null) && (e.getMessage ()!=null))
      {
         String message = "";
         if (origin!=null)
         {
            String file=origin.substring (origin.lastIndexOf ("/")+1, origin.length ());
            message="(" + file + ")";
         }
         processingErrors +=e.getMessage () + message + "<br>\n";
      }
      errorCounter.incrementAndGet ();

      // As far as endIngestion is not called in case of error, it is
      // necessary to run it manually.
      if (isCompleted())
      {
         scannerStatus = ScannerManager.STATUS_ERROR;
         processingsDone(null);
      }
   }
   /**
    * Called on fatal error: the scanner crashed and no processing
    * are expected passed this event. scanner status forced to ERROR,
    * and error message is reported.
    */
   public synchronized void fatalError (Throwable e)
   {
      // Force the scanner status to ERROR.
      scannerStatus = ScannerManager.STATUS_ERROR;
      processingsDone(e.getMessage ());
   }

   /**
    * Called at products ingestion start.
    */
   public synchronized void startIngestion ()
   {
      startCounter.incrementAndGet ();
   }

   /**
    * End of a product ingestion: check if the scanner is finished, and all
    * processing are completed, in this case, it modifies the scanner status
    * and message to inform user of finished processings.
    */
   public synchronized void endIngestion ()
   {
      endCounter.incrementAndGet ();
      logStatus();

      // Total number of product processed shall be coherent with
      // passed/non-passed number of products.
      if (isCompleted())
      {
         this.scannerStatus = ScannerManager.STATUS_OK;
         processingsDone(null);
      }
   }

   /**
    * Notifies that the scanned finished its processing.
    * If the status is "ERROR"
    * @param status
    * @param message
    */
   public void setScannerDone (String status, String message)
   {
      this.scannerStatus = status;
      this.scannerMessage = message;

      // CASE of scanner stopped before first processing or no processing
      // to be performed.
      // If all processing started are finished and all processing
      // provided by the scanner to the processing manager are taken into
      // account.
      if ((startCounter.get () >= (endCounter.get () + errorCounter.get ())) &&
          (startCounter.get () >= getTotalProcessed ()))
      {
         processingsDone(null);
      }
   }

   /**
    * Notifies the scanner that the processings are done.
    */
   protected synchronized void processingsDone(String ended_message)
   {
      LOGGER.info(
         "Scanner and processings are completed: update the UI status.");
      SimpleDateFormat sdf = new SimpleDateFormat (
         "EEEE dd MMMM yyyy - HH:mm:ss", Locale.ENGLISH);

      String processing_message = "Ingestion completed at " +
         sdf.format (new Date ()) + "<br>\nwith " + endCounter.get () +
         " products processed and " + errorCounter.get () +
         " error" + (errorCounter.get () >1?"s":"") +
         " during this processing.<br>\n";

      if (!processingErrors.isEmpty ())
         processing_message += "<u>Processing error(s):</u><br>\n" +
               processingErrors;

      if (ended_message!= null)
      {
         processing_message += ended_message + "<br>\n";
      }

      ScannerManager scannerManager = ApplicationContextProvider.getBean(ConfigurationManager.class)
            .getScannerManager();
      if (scannerId != null)
      {
         // Set the scanner info
         ScannerInfo persistentScanner = scannerManager.get(scannerId);
         persistentScanner.setStatus(scannerStatus);
         persistentScanner.setStatusMessage(
               persistentScanner.getStatusMessage() + scannerMessage + "<br>\n" + processing_message);
         scannerManager.update(persistentScanner, false);
      }
      logStatus();
   }

   /**
    * Total processed is the effective number of products that are submitted
    * to the processing manager to be ingested. This count excludes the
    * products recognized as already ingested, or products that generates
    * exception during submission. This count includes submitted products
    * even if they causes exception during processing steps.
    *
    * To be able to return this value, scanner execution should be finished
    * with a status. Otherwise, the method waits for the availability.
    */
   public int getTotalProcessed ()
   {
      return totalProcessed.get ();
   }

   public void setTotalProcessed (int total_processed)
   {
      totalProcessed.set (total_processed);
   }

   public void incrementTotalProcessed ()
   {
      totalProcessed.incrementAndGet ();
   }

   /**
    * Checks if ingestions of the associated scanner are done.
    *
    * @return true if all ingestions are done, otherwise false
    */
   private boolean isCompleted()
   {
      int result = endCounter.get() + cancelCount.get() + errorCounter.get();
      return result >= totalProcessed.get();
   }

   /**
    * Updates scanner wrapper status after a cancelled product ingestion.
    *
    * @param product ingestion target
    */
   public synchronized void cancelProcess(String origin)
   {
      cancelCount.incrementAndGet();
      LOGGER.info("Ingestion cancelled for product located at '{}'", origin);
      if (isCompleted())
      {
         this.scannerStatus = ScannerManager.STATUS_OK;
         processingsDone("Scanner stopped");
      }
   }

   private void logStatus()
   {
      int total = totalProcessed.get();
      int inbox = total - (endCounter.get() + cancelCount.get() + errorCounter.get());

      LOGGER.info("End of product ingestion: processed={}, cancelled={}, error={}, inbox={}, total={}.",
            endCounter.get(), cancelCount.get(), errorCounter.get(), inbox, total);
   }
}
