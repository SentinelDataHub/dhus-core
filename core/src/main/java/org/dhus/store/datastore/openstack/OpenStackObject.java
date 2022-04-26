/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2020 GAEL Systems
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




package org.dhus.store.datastore.openstack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.gael.drb.impl.swift.DrbSwiftObject;

public class OpenStackObject
{

   private final String provider;
   private final String identity;
   private final String credential;
   private final String url;

   private volatile DrbSwiftObject ostack = null;
   
   private static final Logger LOGGER = LogManager.getLogger();
   
   
   public OpenStackObject(String provider, String identity, String credential, String url)
   {
      this.provider = provider;
      this.identity = identity;
      this.credential = credential;
      this.url = url;
   }

   /**
    * Retrieve the OpenStack storage object from the construction parameters.
    *
    * @return the object to manipulate OpenStack
    */
   public DrbSwiftObject getOpenStackObject()
   {
      // Double-checked locking using a local variable for better performances
      DrbSwiftObject res = this.ostack;
      if (res == null)
      {
         synchronized (this)
         {
            res = this.ostack;
            if (res == null)
            {
               res = createOpenStackObject();
               if (res != null)
               {
                  this.ostack = res;
               }
            }
         }
      }
      return res;
   }

   /**
    * Always create a new DrbSwiftObject, use {@link #getOpenStackObject()} instead (lazy loading).
    * @return a new instance or null of the configuration is invalid (error logged)
    */
   private DrbSwiftObject createOpenStackObject()
   {
      // Keystone v3 API
      if (url.contains("/v3"))
      {
         String[] splitIdentity = this.identity.split(":");
         if (splitIdentity.length != 3)
         {
            LOGGER.error("Invalid identity format, must be: <domain>:<project>:<username>");
            return null;
         }
         else
         {
            String domain   = splitIdentity[0];
            String project  = splitIdentity[1];
            String username = splitIdentity[2];
            return new DrbSwiftObject(this.url, this.provider, username, project, this.credential, domain);
         }
      }
      // Keystone v3 API
      return new DrbSwiftObject(this.url, this.provider, this.identity, this.credential);
   }  
}
