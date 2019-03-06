/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2017 GAEL Systems
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

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import fr.gael.dhus.database.object.config.scanner.ScannerInfo;
import fr.gael.dhus.database.object.config.scanner.ScannerManager;
import fr.gael.dhus.datastore.scanner.ScannerException.ScannerNotFoundException;
import fr.gael.dhus.datastore.scanner.ScannerException.ScannerAlreadyRunningException;
import fr.gael.dhus.system.config.ConfigurationManager;
import fr.gael.drbx.cortex.DrbCortexItemClass;
import fr.gael.drbx.cortex.DrbCortexModel;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.odata2.api.exception.ODataException;

import org.dhus.store.ingestion.IngestibleRawProduct;
import org.dhus.store.ingestion.ProcessingManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provide the scanner instances.
 */
@Component ("scannerFactory")
public class ScannerFactory
{
   private static final Logger LOGGER = LogManager.getLogger(ScannerFactory.class);

   public static final SimpleDateFormat SDF = new SimpleDateFormat("EEEE dd MMMM yyyy - HH:mm:ss", Locale.ENGLISH);

   @Autowired
   private ConfigurationManager configurationManager;

   private final ConcurrentHashMap<Long, Scanner> RUNNING_SCANNERS = new ConcurrentHashMap<>();

   private String[] itemClasses;

   /**
    * Retrieve the list of items that the scanner is able to retrieve. This
    * support allow not to perform selective ingest according to the item
    * classes.
    *
    * @return the list of supported items.
    */
   public List<DrbCortexItemClass> getScannerSupport ()
   {
      if (itemClasses == null)
      {
         itemClasses = getDefaultCortexSupport ();
         if ((itemClasses!=null) && LOGGER.isDebugEnabled())
         {
            LOGGER.debug("Supported classes:");
            for (String cl:itemClasses)
            {
               LOGGER.debug(" - " + cl);
            }
         }
      }

      if (itemClasses == null)
         throw new UnsupportedOperationException (
            "Empty item list: no scanner support.");

      List<DrbCortexItemClass> supported = new ArrayList<DrbCortexItemClass> ();
      if (itemClasses != null)
      {
         for (String s : itemClasses)
         {
            try
            {
               supported.add (DrbCortexItemClass.getCortexItemClassByName (s));
            }
            catch (Exception e)
            {
               LOGGER.error ("Cannot add support for class " + s);
            }
         }
      }
      return supported;
   }

   /**
    * Retrieve the dhus system supported items for file scanning processing.
    * Is considered supported all classes having
    * <code>http://www.gael.fr/dhus#metadataExtractor</code> property
    * connection.
    * @return the list of supported class names.
    */
   public static synchronized String[] getDefaultCortexSupport ()
   {
      DrbCortexModel model;
      try
      {
         model = DrbCortexModel.getDefaultModel ();
      }
      catch (IOException e)
      {
         throw new UnsupportedOperationException (
            "Drb cortex not properly initialized.");
      }

      ExtendedIterator it=model.getCortexModel ().getOntModel ().listClasses ();
      List<String>list = new ArrayList<String> ();

      while (it.hasNext ())
      {
         OntClass cl = (OntClass)it.next ();

         OntProperty metadata_extractor_p = cl.getOntModel().getOntProperty(
               "http://www.gael.fr/dhus#support");

         StmtIterator properties = cl.listProperties (metadata_extractor_p);
         while (properties.hasNext ())
         {
            Statement stmt = properties.nextStatement ();
            LOGGER.debug ("Scanner Support Added for " +
               stmt.getSubject ().toString ());
            list.add (stmt.getSubject ().toString ());
         }
      }
      return list.toArray (new String[list.size ()]);
   }

   /**
    * Retrieve scanner for given url
    */
   public Scanner getScanner (String url)
   {
      return getScanner(url, null, null, null);
   }
   /**
    * Retrieve the scanner according to the passed archive.
    *
    * @param archive used to define the scanner.
    * @return the scanner able to scan passed archive, null if no scanner found.
    */
   public Scanner getScanner (String url, String username, String password,
         String pattern)
   {
      if (url == null) throw new NullPointerException ("URL is required.");

      if ( (new File (url)).exists ())
      {
         FileScanner scan = new FileScanner (url, false);
         scan.setUserPattern (pattern);
         return scan;
      }
      if (url.startsWith ("file:"))
      {
         FileScanner scan = new FileScanner (url.split ("file:", 2)[1], false);
         scan.setUserPattern (pattern);
         return scan;
      }
      if (url.startsWith ("ftp"))
      {
         FtpScanner s = new FtpScanner (url, false, username, password);
         s.setUserPattern (pattern);
         return s;
      }
      if (url.startsWith ("http")) // http or https
      {
         ODataScanner scan = null;
         try
         {
            scan = new ODataScanner (url, false, username, password);
         }
         catch (URISyntaxException | IOException | ODataException e)
         {
            throw new RuntimeException (e);
         }
         scan.setUserPattern (pattern);
         return scan;
      }

      throw new UnsupportedOperationException ("Url not supported (\"" +
         url + "\").");
   }

   /**
    * Provides a scanner able to fully scan&upload passed URL.
    *
    * @param url
    * @param username
    * @param password
    * @return
    */
   public Scanner getUploadScanner (String url, final String username,
      final String password, String pattern)
   {
      final Scanner scanner = getScanner (url, username, password, pattern);
      scanner.setSupportedClasses (getScannerSupport ());
      return scanner;
   }

   /**
    * Process passed file scanner in a separate thread.
    * If the requested scanner is already running (from schedule or UI), it will not restart.
    *
    * @param scan_id
    * @throws ScannerException when scanner cannot be started
    */
   public void processScan(final Long scan_id) throws ScannerException
   {
      // Synchronize with runningScanner instance to avoid 2 simultaneous
      // scanners executions.
      // Running scanner hash table should contains the scanner, but during the
      // transition between scanner the status settings and the scanner 
      // initialization, runningScanner[scan_id] could contains null to avoid 
      // the same scanner being executed twice.
      synchronized (RUNNING_SCANNERS)
      {
         if (RUNNING_SCANNERS.containsKey(scan_id))
         {
            throw new ScannerAlreadyRunningException(scan_id);
         }
         RUNNING_SCANNERS.put(scan_id, new UninitilizedScanner());
      }

      ScannerManager scannerManager = configurationManager.getScannerManager();
      ScannerInfo scannerInfo = scannerManager.get(scan_id);

      if(scannerInfo == null)
      {
         throw new ScannerNotFoundException(scan_id);
      }

      scannerInfo.setStatus(ScannerManager.STATUS_RUNNING);
      scannerInfo.setStatusMessage("Started on " + SDF.format(new Date()));
      scannerManager.update(scannerInfo, false);

      // prepare scan
      ScannerListener listener = new ScannerListener();
      Scanner scanner = getUploadScanner(scannerInfo.getUrl(),
            scannerInfo.getUsername(),
            scannerInfo.getPassword(),
            scannerInfo.getPattern());
      scanner.getScanList().addListener(listener);
      Hook hook = new Hook(scannerInfo);
      Runtime.getRuntime().addShutdownHook(hook);

      // replace UninitilizedScanner with actual scanner in runningScanners map so it can be stopped
      RUNNING_SCANNERS.put(scan_id, scanner);

      // perform scan
      try
      {
         scanner.scan ();
      }
      catch (InterruptedException e)
      {
         scannerInfo.setStatus(ScannerManager.STATUS_OK);
         scannerInfo.setStatusMessage("Scanner stopped by user on " + SDF.format(new Date()));
         scannerManager.update(scannerInfo, false);
         LOGGER.warn("Scanner #{} stop by a user", scan_id);
         return;
      }
      catch (UnsupportedOperationException e)
      {
         scannerInfo.setStatus(ScannerManager.STATUS_ERROR);
         scannerInfo.setStatusMessage("There was an error during Scanner execution :'" + e.getMessage() + "'");
         scannerManager.update(scannerInfo, false);
         LOGGER.error("Scanner #{}: There was an error during Scanner execution.", scan_id, e);
         RUNNING_SCANNERS.remove(scan_id);
         return;
      }

      // prepare ingestion
      List<URL> waiting_product = listener.newlyProducts();
      if (waiting_product.isEmpty())
      {
         scannerInfo.setStatus(ScannerManager.STATUS_OK);
         scannerInfo.setStatusMessage("No products scanned.");
         scannerManager.update(scannerInfo, false);
         RUNNING_SCANNERS.remove(scan_id);
         LOGGER.info("Scanner #{}: No products scanned.", scan_id);
         return;
      }
      List<String> collectionNames = scannerInfo.getCollectionList();
      FileScannerWrapper wrapper = new FileScannerWrapper(scannerInfo)
      {
         @Override
         protected synchronized void processingsDone (String end_message)
         {
            super.processingsDone (end_message);
            RUNNING_SCANNERS.remove(scan_id);
         }
      };
      wrapper.setTotalProcessed (waiting_product.size ());
      LOGGER.info("Scanner #{}: {} products scanned.", scan_id, wrapper.getTotalProcessed());

      // perform ingestion
      for (URL url : waiting_product)
      {
         try
         {
            ProcessingManager.processProduct(new IngestibleRawProduct(url), collectionNames, scanner, wrapper);
         }
         catch (RuntimeException e)
         {
            LOGGER.error("Unable to start ingestion.", e);
            scannerInfo.setStatus(ScannerManager.STATUS_ERROR);
            scannerInfo.setStatusMessage(e.getMessage());
            scannerManager.update(scannerInfo, false);
            RUNNING_SCANNERS.remove(scan_id);
         }
      }
   }

   /**
    * @param scan_id ID of scanner to stop
    * @return {@code false} if could not stop (scanner is not initialised)
    */
   public boolean stopScan(final Long scan_id)
   {
      Scanner scanner = null;
      // Thread-safe retrieve the scanner and remove it from the list.
      synchronized (RUNNING_SCANNERS)
      {
         scanner = RUNNING_SCANNERS.get(scan_id);
         if (scanner == null)
         {
            LOGGER.warn ("Scanner already stopped.");
            return true;
         }
         if (scanner instanceof UninitilizedScanner)
         {
            LOGGER.warn ("Scanner not initialized (retry stop later).");
            return false;
         }
         RUNNING_SCANNERS.remove(scan_id);
      }

      ScannerInfo scannerInfo = configurationManager.getScannerManager().get(scan_id);
      if (scannerInfo != null)
      {
         // Just update the message
         scannerInfo.setStatusMessage(scannerInfo.getStatusMessage()
               + "Interrupted: waiting ongoing processings ends...");
         configurationManager.getScannerManager().update(scannerInfo, false);
      }

      LOGGER.info ("Scanner stopped.");
      scanner.stop ();
      return true;
   }

   /**
    * Shutdown hook used to manage Scanner message when user stops dhus
    * while scanner is running.
    */
   class Hook extends Thread
   {
      private final ScannerInfo scanner;
      public Hook(ScannerInfo scanner)
      {
         this.scanner = scanner;
      }

      @Override
      public void run()
      {
         scanner.setStatus(ScannerManager.STATUS_ERROR);
         scanner.setStatusMessage(scanner.getStatusMessage() + "Scanner interrupted because DHuS stopped.");
         configurationManager.getScannerManager().update(scanner, false);
      }
   }

   /**
    * An internal scanner implementation to manage the scanner initialization 
    * transition.
    */
   class UninitilizedScanner implements Scanner
   {
      @Override
      public int scan() throws InterruptedException
      {
         return 0;
      }

      @Override
      public void stop()
      {
      }

      @Override
      public boolean isStopped()
      {
         return false;
      }

      @Override
      public AsynchronousLinkedList<URLExt> getScanList()
      {
         return null;
      }

      @Override
      public void setSupportedClasses(List<DrbCortexItemClass> supported)
      {
      }

      @Override
      public void setForceNavigate(boolean force)
      {
      }

      @Override
      public boolean isForceNavigate()
      {
         return false;
      }

      @Override
      public void setUserPattern(String pattern)
      {
      }
   }
}
