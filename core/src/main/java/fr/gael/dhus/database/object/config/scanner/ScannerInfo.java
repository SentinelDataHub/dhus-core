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

import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

/**
 * Holds the status of a Scanner.
 */
public class ScannerInfo extends ScannerConfiguration
{
   @XmlTransient
   private String status = ScannerManager.STATUS_ADDED;
   @XmlTransient
   private String statusMessage = "";

   public String getStatus()
   {
      return status;
   }

   public void setStatus(String status)
   {
      this.status = status;
   }

   public String getStatusMessage()
   {
      return statusMessage;
   }

   public void setStatusMessage(String statusMessage)
   {
      this.statusMessage = statusMessage;
   }

   public List<String> getCollectionList()
   {
      Collections collectionListconf = getCollections();
      if (collectionListconf == null)
      {
         return java.util.Collections.<String>emptyList();
      }
      return collectionListconf.getCollection();
   }

}
