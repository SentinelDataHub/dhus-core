/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2018 GAEL Systems
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
package org.dhus.scanner.config;

import fr.gael.dhus.database.object.config.cron.Cron;
import fr.gael.dhus.database.object.config.scanner.ScannerConfiguration;

import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Holds the status of a Scanner.
 */
public class ScannerInfo extends ScannerConfiguration
{
   @XmlTransient
   private String username = null;
   @XmlTransient
   private String password = null;
   @XmlTransient
   private boolean active = false;

   public List<String> getCollectionList()
   {
      Collections collectionListconf = getCollections();
      if (collectionListconf == null)
      {
         return java.util.Collections.<String>emptyList();
      }
      return collectionListconf.getCollection();
   }

   /**
    * Adds given collection name to the configuration, avoids duplicates.
    *
    * @param collectionName to add
    */
   public void addCollection(String collectionName)
   {
      Objects.requireNonNull(collectionName);
      if (getCollections() == null)
      {
         setCollections(new Collections());
      }
      if (!getCollections().getCollection().contains(collectionName))
      {
         getCollections().getCollection().add(collectionName);
      }
   }

   public String getUsername()
   {
      return username;
   }

   public String getPassword()
   {
      return password;
   }

   public void setPassword(String password)
   {
      this.password = password;
   }

   public boolean isActive()
   {
      if (null != this.cron)
      {
         return this.cron.isActive();
      }
      else
      {
         return this.active;
      }
   }

   public void setActive(boolean active)
   {
      if (null == this.cron)
      {
         this.cron = new Cron();
      }

      this.cron.setActive(active);
      this.active = active;
   }

   public void setUsername(String username)
   {
      this.username = username;
   }
}
