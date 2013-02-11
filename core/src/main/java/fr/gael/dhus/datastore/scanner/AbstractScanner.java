/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013-2019 GAEL Systems
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

import fr.gael.dhus.datastore.scanner.listener.AsynchronousLinkedList;
import fr.gael.dhus.datastore.scanner.listener.Listener;
import fr.gael.dhus.datastore.scanner.listener.ScannerListener;

import fr.gael.drb.DrbItem;
import fr.gael.drb.DrbNode;
import fr.gael.drbx.cortex.DrbCortexItemClass;
import fr.gael.drbx.cortex.DrbCortexModel;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.scanner.ScannerContainer;
import org.dhus.scanner.config.ScannerInfo;
import org.dhus.store.ingestion.IngestibleRawProduct;
import org.dhus.store.ingestion.ProcessingManager;

/**
 * This class implements a specific data {@link Scanner} that matches list of {@link DrbCortexItemClass}.
 * It uses {@link AsynchronousLinkedList} able to register {@link Listener#addedElement(Event)} and
 * {@link Listener#addedElement(Event)} events, that give the capability to be notified on the fly
 * that a new element has been retrieved.
 * {@link AbstractScanner#AbstractScanner(String, boolean)} constructor, allow build a scanner
 * instance that will not save scanned elements during the scan in order to increase memory
 * performance when lots of files are susceptible to be retrieved.
 *
 * @see DrbCortexItemClass#getCortexItemClassByName(String)
 */
public abstract class AbstractScanner implements Scanner
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final SimpleDateFormat SDF = new SimpleDateFormat("EEEE dd MMMM yyyy - HH:mm:ss", Locale.ENGLISH);

   public URL repository;
   private List<DrbCortexItemClass> supportedClasses;
   private boolean forceNavigate = false;
   private Pattern userPattern = null;

   private ScannerStatus status;
   private ScannerInfo config;

   private final AsynchronousLinkedList<URLExt> currentFiles = new AsynchronousLinkedList<>();

   /**
    * Build a {@link Scanner} to scan inside the passed uri.
    * Scanned results can be store and retrieve into a list if the storeScanList flag is set to
    * true (default). otherwise, scanned result will not be saved, Results list can be retrieved
    * on the fly via the listener while performing scan.
    *
    * @param store_scan_list don't store scanned list in order to to preserve memory (default=true)
    */
   public AbstractScanner(boolean store_scan_list)
   {
      currentFiles.simulate(!store_scan_list);
   }

   @Override
   public AsynchronousLinkedList<URLExt> getScanList()
   {
      return this.currentFiles;
   }

   @Override
   public void setUserPattern(String pattern)
   {
      userPattern = null;
      if ((pattern != null) && !pattern.trim().isEmpty())
      {
         try
         {
            userPattern = Pattern.compile(pattern);
         }
         catch (PatternSyntaxException e)
         {
            // FIXME a user pattern that does not compile does not prevent a scanner from running
            LOGGER.error("Cannot set scanner pattern", e);
         }
      }
   }

   public Pattern getUserPattern()
   {
      return userPattern;
   }

   protected boolean matches(DrbItem item)
   {
      if (item == null)
      {
         return false;
      }
      LOGGER.debug("matches {}", ((DrbNode) item).getPath());
      // First of all, checks if the pattern matches this item
      Pattern p = getUserPattern();
      if (p != null)
      {
         return p.matcher(item.getName()).matches();
      }

      // If no supported class defined, nothing match.
      if (supportedClasses == null)
      {
         return true;
      }

      for (DrbCortexItemClass cl: supportedClasses)
      {
         if (LOGGER.isDebugEnabled())
         {
            String str_cl = "";
            try
            {
               DrbCortexModel m = DrbCortexModel.getDefaultModel();
               DrbCortexItemClass item_class = m.getClassOf(item);
               if (item_class != null)
               {
                  str_cl = item_class.getOntClass().getURI();
               }
            }
            catch (IOException e1)
            {
               LOGGER.error("Could not get the default DrbCortex model", e1);
            }

            LOGGER.debug("Checking class : {} ({}) - with - {} ({})",
                  cl.getLabel(), cl.getOntClass().getURI(), item.getName(), str_cl);
         }
         try
         {
            if (cl.includes(item, false))
            {
               LOGGER.debug("{} Matches \"{}\"", item.getName(), cl.getLabel());
               return true;
            }
         }
         catch (Exception e)
         {
            LOGGER.warn("Cannot match the item \"{}\" with class \"{}\": continuing...",
                  ((DrbNode) item).getName(), cl.getLabel(), e);
         }
      }
      LOGGER.debug("No match for {}", ((DrbNode) item).getPath());
      return false;
   }

   /**
    * Retrieve the list of {@link DrbCortexItemClass} that this scan is supposed to recognise and reacts.
    * <pre>
    * List<DrbCortexItemClass> supported = new ArrayList&lt;>();
    * supported.add(DrbCortexItemClass.getCortexItemClassByName("http://www.esa.int/envisat#product"));
    * scanner.setSupportedClasses(supported);
    * </pre>
    *
    * @return the supportedClasses the list of {@link DrbCortexItemClass}
    * @see DrbCortexItemClass#getCortexItemClassByName(String)
    */
   public List<DrbCortexItemClass> getSupportedClasses()
   {
      return supportedClasses;
   }

   @Override
   public void setSupportedClasses(List<DrbCortexItemClass> supported_classes)
   {
      this.supportedClasses = supported_classes;
   }

   @Override
   public abstract int scan() throws InterruptedException;

   @Override
   public void stop()
   {
      setStopped(true);
   }

   @Override
   public void setForceNavigate(boolean force_navigate)
   {
      this.forceNavigate = force_navigate;
   }

   @Override
   public boolean isForceNavigate()
   {
      return forceNavigate;
   }

   @Override
   public boolean isStopped()
   {
      return status.isStopped();
   }

   public void setStopped(boolean stopping)
   {
      this.status.setStopped(stopping);
   }

   // listeners implementation
   @Override
   public ScannerStatus getStatus()
   {
      return this.status;
   }

   @Override
   public void setStatus(ScannerStatus status)
   {
      this.status = status;
   }

   @Override
   public ScannerInfo getConfig()
   {
      return this.config;
   }

   @Override
   public void setConfig(ScannerInfo config)
   {
      this.config = config;
   }

   @Override
   public void processScan()
   {
      status = new ScannerStatus();
      status.setStatus(ScannerStatus.STATUS_RUNNING);
      status.addStatusMessage("Started on " + ScannerContainer.SDF.format(new Date()));

      // reset all listener
      this.getScanList().getListeners().forEach(Listener::reset);

      // perform scan
      try
      {
         scan();
      }
      catch (InterruptedException e)
      {
         status.setStatus(ScannerStatus.STATUS_OK);
         status.addStatusMessage("Scanner stopped by user on " + SDF.format(new Date()));
         LOGGER.warn("Scanner #{} stop by a user", config.getId());
         return;
      }
      catch (Exception e)
      {
         status.setStatus(ScannerStatus.STATUS_ERROR);
         status.addStatusMessage("There was an error during Scanner execution :'" + e.getMessage() + "'");
         LOGGER.error("Scanner #{}: There was an error during Scanner execution", config.getId(), e);
         return;
      }

      // prepare ingestion
      ScannerListener listener = (ScannerListener) getScanList().getListeners().get(0);
      if (null == listener)
      {
         LOGGER.error("No listener registered for scanner:{}, ingestion would not be started", config.getId());
         return;
      }

      List<URL> waiting_product = listener.newlyProducts();
      if (waiting_product.isEmpty())
      {
         status.setStatus(ScannerStatus.STATUS_OK);
         status.addStatusMessage("No products scanned.");
         LOGGER.info("Scanner #{}: No products scanned", config.getId());
         return;
      }

      List<String> collectionNames = config.getCollectionList();
      status.setTotalProcessed(waiting_product.size());
      LOGGER.info("Scanner #{}: {} products scanned", config.getId(), status.getTotalProcessed());

      // perform ingestion
      for (URL url: waiting_product)
      {
         try
         {
            ProcessingManager.processProduct(IngestibleRawProduct.fromURL(url), collectionNames, this, this.getStatus());
         }
         catch (RuntimeException e)
         {
            LOGGER.error("Unable to start ingestion", e);
            status.setStatus(ScannerStatus.STATUS_ERROR);
            status.addStatusMessage(e.getMessage());
         }
      }
   }
}
