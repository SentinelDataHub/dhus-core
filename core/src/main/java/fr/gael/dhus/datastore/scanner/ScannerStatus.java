/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2015-2019 GAEL Systems
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

import fr.gael.dhus.util.functional.tuple.Duo;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dhus.scanner.ScannerContainer;

public class ScannerStatus
{
   private static final Logger LOGGER = LogManager.getLogger();

   public static final String STATUS_ADDED = "added";
   public static final String STATUS_RUNNING = "running";
   public static final String STATUS_OK = "ok";
   public static final String STATUS_ERROR = "error";

   final AtomicInteger startCounter;
   final AtomicInteger endCounter;
   final AtomicInteger errorCounter;
   final AtomicInteger cancelCount;
   final AtomicInteger totalProcessed;

   private boolean stopped = false;

   private String status = "";
   private final LinkedList<Duo<Long, String>> statuses = new LinkedList<>();
   String processingErrors = "";

   public ScannerStatus()
   {
      this.startCounter = new AtomicInteger(0);
      this.endCounter = new AtomicInteger(0);
      this.errorCounter = new AtomicInteger(0);
      this.cancelCount = new AtomicInteger(0);
      this.totalProcessed = new AtomicInteger(0);
      this.status = STATUS_ADDED;
   }

   /**
    * Case of error during processing: informations are accumulated to be
    * displayed to the user.
    */
   public synchronized void error(String origin, Throwable e)
   {
      if ((e != null) && (e.getMessage() != null))
      {
         String message = "";
         if (origin != null)
         {
            String file = origin.substring(origin.lastIndexOf("/") + 1, origin.length());
            message = "(" + file + ")";
         }
         processingErrors += e.getMessage() + message + "\n";
      }
      errorCounter.incrementAndGet();

      // As far as endIngestion is not called in case of error, it is
      // necessary to run it manually.
      if (isCompleted())
      {
         status = STATUS_ERROR;
         processingsDone(null);
      }
   }

   /**
    * Called on fatal error: the scanner crashed and no processing
    * are expected passed this event. scanner status forced to ERROR,
    * and error message is reported.
    */
   public synchronized void fatalError(Throwable e)
   {
      // Force the scanner status to ERROR.
      status = STATUS_ERROR;
      processingsDone(e.getMessage());
   }

   /**
    * Called at products ingestion start.
    */
   public synchronized void startIngestion()
   {
      startCounter.incrementAndGet();
   }

   /**
    * End of a product ingestion: check if the scanner is finished, and all
    * processing are completed, in this case, it modifies the scanner status
    * and message to inform user of finished processings.
    */
   public synchronized void endIngestion()
   {
      endCounter.incrementAndGet();
      logStatus();

      // Total number of product processed shall be coherent with
      // passed/non-passed number of products.
      if (isCompleted())
      {
         this.status = STATUS_OK;
         processingsDone(null);
      }
   }

   /**
    * Notifies the scanner that the processings are done.
    *
    * @param endMessage optional message status to append at the end
    */
   protected synchronized void processingsDone(String endMessage)
   {
      LOGGER.info("Scanner and processings are completed: update the UI status.");
      long now = System.currentTimeMillis();

      this.statuses.add(new Duo<>(now, "Ingestion completed at " + ScannerContainer.SDF.format(new Date(now))));
      this.statuses.add(new Duo<>(now, "with " + endCounter.get()
            + " products processed and " + errorCounter.get()
            + " error" + (errorCounter.get() > 1 ? "s" : "")
            + " during this processing."));

      if (!processingErrors.isEmpty())
      {
         this.statuses.add(new Duo<>(now, "Processing error(s): " + processingErrors));
      }

      if (endMessage != null)
      {
         this.statuses.add(new Duo<>(now, endMessage));
      }

      // reset the stopped flag
      this.stopped = false;
      this.setStatus(status);
      logStatus();
   }

   /**
    * Total processed is the effective number of products that are submitted
    * to the processing manager to be ingested. This count excludes the
    * products recognized as already ingested, or products that generates
    * exception during submission. This count includes submitted products
    * even if they causes exception during processing steps.
    * <p>
    * To be able to return this value, scanner execution should be finished
    * with a status. Otherwise, the method waits for the availability.
    */
   public int getTotalProcessed()
   {
      return totalProcessed.get();
   }

   public void setTotalProcessed(int total_processed)
   {
      totalProcessed.set(total_processed);
   }

   public void incrementTotalProcessed()
   {
      totalProcessed.incrementAndGet();
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
    * @param origin location of product whose ingestion has been cancelled
    */
   public synchronized void cancelProcess(String origin)
   {
      cancelCount.incrementAndGet();
      LOGGER.info("Ingestion cancelled for product located at '{}'", origin);
      if (isCompleted())
      {
         this.status = STATUS_OK;
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

   public synchronized void setStatus(String s)
   {
      this.status = s;
   }

   public synchronized String getStatus()
   {
      return this.status;
   }

   public synchronized void addStatusMessage(String message)
   {
      this.statuses.add(new Duo<>(System.currentTimeMillis(), message));
   }

   /**
    * Return all the statuses as an HTML paragraph.
    *
    * @return a {@code <br/>} separated list of all the statuses
    */
   public String getStatusMessage()
   {
      if (this.statuses.isEmpty())
      {
         return "";
      }
      return this.statuses.stream()
            .map(Duo::getB)
            .map(s -> s.replace("\n", "<br />\n"))
            .reduce((a, b) -> String.join("<br />\n", a, b))
            .get(); // Optional<String> (no throw)
   }

   public List<String> getStatusMessages()
   {
      return this.statuses.stream()
            .map(Duo::getB)
            .collect(Collectors.toList());
   }

   public synchronized int getInbox()
   {
      int total = totalProcessed.get();
      int inbox = total - (endCounter.get() + cancelCount.get() + errorCounter.get());
      return inbox;
   }

   public synchronized int getProcessed()
   {
      return endCounter.get();
   }

   public synchronized int getCancelled()
   {
      return cancelCount.get();
   }

   public synchronized int getError()
   {
      return errorCounter.get();
   }

   public synchronized int getTotal()
   {
      return totalProcessed.get();
   }

   public boolean isStopped()
   {
      return stopped;
   }

   public void setStopped(boolean stopping)
   {
      this.stopped = stopping;
   }
}
