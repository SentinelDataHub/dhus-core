/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2017,2018 GAEL Systems
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

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import fr.gael.dhus.database.object.config.scanner.FileScannerConf;
import fr.gael.dhus.database.object.config.scanner.FtpScannerConf;
import fr.gael.dhus.datastore.scanner.FileScanner;
import fr.gael.dhus.datastore.scanner.FtpScanner;
import fr.gael.dhus.datastore.scanner.ODataScanner;
import fr.gael.dhus.datastore.scanner.Scanner;
import fr.gael.dhus.datastore.scanner.ScannerStatus;
import fr.gael.dhus.datastore.scanner.listener.ScannerListener;

import fr.gael.drbx.cortex.DrbCortexItemClass;
import fr.gael.drbx.cortex.DrbCortexModel;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.odata2.api.exception.ODataException;

import org.dhus.scanner.config.ScannerInfo;

// Create scanner based on config
public class ScannerFactory
{

   private static final Logger LOGGER = LogManager.getLogger();
   private static String[] itemClasses;

   public static Scanner getScanner(ScannerInfo config)
   {
      Scanner scanner;
      if (config instanceof FileScannerConf)
      {
         scanner = makeFileScanner(config);
      }
      else if (config instanceof FtpScannerConf)
      {
         scanner = makeFtpScanner(config);
      }
      else if (config.getUrl().startsWith("http"))
      {
         try
         {
            return makeOdataScanner(config);
         }
         catch (URISyntaxException | IOException | ODataException e)
         {
            throw new RuntimeException(e);
         }
      }
      else
      { // If nothing specified, create a file scanner by default
         scanner = makeFileScanner(config);
      }

      ScannerListener listener = new ScannerListener();
      scanner.getScanList().addListener(listener);

      return scanner;
   }

   private static Scanner makeOdataScanner(ScannerInfo config)
         throws URISyntaxException, IOException, ODataException
   {
      ODataScanner scan = null;
      scan = new ODataScanner(config.getUrl(), false, config.getUsername(), config.getPassword());
      scan.setStatus(new ScannerStatus());
      scan.setConfig(config);
      scan.setUserPattern(config.getPattern());
      scan.setSupportedClasses(getScannerSupport());
      return scan;
   }

   private static Scanner makeFtpScanner(ScannerInfo config)
   {
      FtpScanner s = new FtpScanner(config.getUrl(), false, config.getUsername(), config.getPassword());
      s.setConfig(config);
      s.setStatus(new ScannerStatus());
      s.setUserPattern(config.getPattern());
      s.setSupportedClasses(getScannerSupport());
      return s;
   }

   private static FileScanner makeFileScanner(ScannerInfo config)
   {
      FileScanner scan = new FileScanner(getFileURL(config.getUrl()), false);
      scan.setStatus(new ScannerStatus());
      scan.setConfig(config);
      scan.setUserPattern(config.getPattern());
      scan.setSupportedClasses(getScannerSupport());
      return scan;
   }

   /**
    * Retrieve the list of items that the scanner is able to retrieve. This
    * support allow not to perform selective ingest according to the item
    * classes.
    *
    * @return the list of supported items.
    */
   public static List<DrbCortexItemClass> getScannerSupport()
   {
      if (itemClasses == null)
      {
         itemClasses = getDefaultCortexSupport();
         if (LOGGER.isDebugEnabled())
         {
            LOGGER.debug("Supported classes:");
            for (String cl: itemClasses)
            {
               LOGGER.debug(" - {}", cl);
            }
         }
      }

      if (itemClasses == null)
      {
         throw new UnsupportedOperationException("Empty item list: no scanner support");
      }

      List<DrbCortexItemClass> supported = new ArrayList<>();
      if (itemClasses != null)
      {
         for (String s: itemClasses)
         {
            try
            {
               supported.add(DrbCortexItemClass.getCortexItemClassByName(s));
            }
            catch (Exception e)
            {
               LOGGER.error("Cannot add support for class {}", s);
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
    *
    * @return the list of supported class names.
    */
   static synchronized String[] getDefaultCortexSupport()
   {
      DrbCortexModel model;
      try
      {
         model = DrbCortexModel.getDefaultModel();
      }
      catch (IOException e)
      {
         throw new UnsupportedOperationException("Drb cortex not properly initialized");
      }

      ExtendedIterator it = model.getCortexModel().getOntModel().listClasses();
      List<String> list = new ArrayList<>();

      while (it.hasNext())
      {
         OntClass cl = (OntClass) it.next();

         OntProperty metadata_extractor_p = cl.getOntModel().getOntProperty("http://www.gael.fr/dhus#support");

         StmtIterator properties = cl.listProperties(metadata_extractor_p);
         while (properties.hasNext())
         {
            Statement stmt = properties.nextStatement();
            LOGGER.debug("Scanner Support Added for {}", stmt.getSubject().toString());
            list.add(stmt.getSubject().toString());
         }
      }
      return list.toArray(new String[list.size()]);
   }

   private static String getFileURL(String url)
   {
      if ((new File(url)).exists())
      {
         return url;
      }
      else if (url.startsWith("file:"))
      {
         return url.split("file:", 2)[1];
      }
      else
      {
         return url;
      }
   }
}
