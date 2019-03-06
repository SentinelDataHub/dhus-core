/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
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
package fr.gael.dhus.database.object.config.scanner;

import fr.gael.dhus.database.object.config.scanner.ScannerConfiguration.Collections;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationException;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages scanners defined in the configuration (dhus.xml).
 */
public class ScannerManager extends Scanners
{
   private static final Logger LOGGER = LogManager.getLogger();

   public static final String STATUS_ADDED = "added";
   public static final String STATUS_RUNNING = "running";
   public static final String STATUS_OK = "ok";
   public static final String STATUS_ERROR = "error";

   private static long nextId = -1;

   /**
    * Returns an immutable list of Scanners.
    * Use other methods of this class to add/remove Scanners.
    *
    * @return unmodifiable list of Scanners
    */
   @SuppressWarnings("unchecked")
   public List<ScannerInfo> getScanners()
   {
      List<ScannerInfo> res = (List<ScannerInfo>) (List<?>) getScanner();
      return java.util.Collections.<ScannerInfo>unmodifiableList(res);
   }

   private long getNextId()
   {
      if (nextId < 0)
      {
         for (ScannerConfiguration scan: getScanner())
         {
            if (scan.getId() > nextId)
            {
               nextId = scan.getId();
            }
         }
      }
      nextId += 1;
      return nextId;
   }

   public ScannerInfo create(String url, String status, String statusMessage,
         boolean active, String username, String password, String pattern, boolean save)
   {
      return create(url, status, statusMessage, active, username, password,
            pattern, new Collections(), save);
   }

   public ScannerInfo create(String url, String status, String statusMessage,
         boolean active, String username, String password, String pattern,
         Collections collections, boolean save)
   {
      ScannerInfo scannerInfo = new ScannerInfo();
      scannerInfo.setId(getNextId());
      scannerInfo.setUrl(url);
      scannerInfo.setStatus(status);
      scannerInfo.setStatusMessage(statusMessage);
      scannerInfo.setActive(active);
      scannerInfo.setUsername(username);
      scannerInfo.setPassword(password);
      scannerInfo.setPattern(pattern);
      scannerInfo.setCollections(collections);
      getScanner().add(scannerInfo);
      if (save)
      {
         save();
      }
      LOGGER.info("Scanner created");
      return scannerInfo;
   }

   /**
    * Updates a existing scanner in configuration.
    *
    * @param update scanner to update
    * @param save writes back config to file
    */
   public void update(final ScannerInfo update, boolean save)
   {
      if (update == null)
      {
         return;
      }

      for (int i = 0; i < getScanner().size(); i++)
      {
         ScannerConfiguration scan = getScanner().get(i);
         if (scan != null && scan.getId() == update.getId())
         {
            getScanner().set(i, update);
            break;
         }
      }

      if (save)
      {
         save();
      }
   }

   /**
    * Returns a scanner according the given id or null if it doesn't exist.
    *
    * @param id of scanner
    * @return instance or null
    */
   public ScannerInfo get(Long id)
   {
      for (ScannerConfiguration scan: getScanner())
      {
         if (scan != null && scan.getId() == id)
         {
            return (ScannerInfo) scan;
         }
      }
      return null;
   }

   public void delete(Long id)
   {
      for (int i = 0; i < getScanner().size(); i++)
      {
         ScannerConfiguration scan = getScanner().get(i);
         if (scan != null && scan.getId() == id)
         {
            getScanner().remove(i);
            break;
         }
      }

      save();
   }

   public int count()
   {
      return getScanner().size();
   }

   public void deleteCollectionReferences(String name)
   {
      for (ScannerConfiguration scan: getScanner())
      {
         if (scan != null)
         {
            if (scan.getCollections() != null)
            {
               scan.getCollections().getCollection().remove(name);
            }
         }
      }

      save();
   }

   public void setScannerActive(Long id, boolean active)
   {
      for (ScannerConfiguration scan: getScanner())
      {
         if (scan != null && scan.getId() == id)
         {
            scan.setActive(active);
            break;
         }
      }

      save();
   }

   /**
    * Returns a list of active Scanners.
    * The returned list is mutable but modifying it won't affect Scanners managed by this object.
    *
    * @return list of active Scanners
    */
   public List<ScannerInfo> getActiveScanners()
   {
      List<ScannerInfo> result = new ArrayList<>();
      for (ScannerConfiguration scan: getScanner())
      {
         if (scan != null && scan.isActive())
         {
            result.add((ScannerInfo) scan);
         }
      }
      return result;
   }

   private void save()
   {
      ConfigurationManager cfg = ApplicationContextProvider.getBean(ConfigurationManager.class);
      try
      {
         cfg.saveConfiguration();
      }
      catch (ConfigurationException e)
      {
         LOGGER.error("There was an error while saving configuration", e);
      }
   }
}
