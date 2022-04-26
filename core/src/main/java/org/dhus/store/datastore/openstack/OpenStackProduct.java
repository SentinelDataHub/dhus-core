/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016,2017 GAEL Systems
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

import org.dhus.AbstractProduct;
import org.dhus.ProductConstants;
import org.dhus.store.datastore.DataStoreProduct;
import org.jclouds.openstack.swift.v1.domain.SwiftObject;

import fr.gael.drb.DrbNode;
import fr.gael.drb.impl.DrbNodeImpl;
import fr.gael.drb.impl.swift.DrbSwiftObject;
import fr.gael.drb.impl.swift.SwiftFactory;
import fr.gael.drb.impl.swift.SwiftObjectURL;


public class OpenStackProduct extends AbstractProduct implements DataStoreProduct
{
   private static final Class<?>[] SUPPORTED_CLASSES =
   {
      InputStream.class, DrbNode.class, URL.class, DataStoreProduct.class
   };

   private final DrbSwiftObject swift;
   private final OpenStackLocation location;

   private SwiftObjectURL url;
   private final String uuid;

   /**
    *
    * @param swiftServer
    * @param region
    * @param container
    * @param name
    */
   public OpenStackProduct(DrbSwiftObject swiftServer, String region, String container, String name)
   {
      this(swiftServer, new OpenStackLocation(region, container, name));
   }

   /**
    *
    * @param swiftServer
    * @param location
    */
   public OpenStackProduct(DrbSwiftObject swiftServer, OpenStackLocation location)
   {    
      this(swiftServer, location, null);
   }
   
   /**
   *
   * @param swiftServer
   * @param location
   * @param uuid
   */
   public OpenStackProduct(DrbSwiftObject swiftServer, OpenStackLocation location, String uuid)
   {
      this.swift = Objects.requireNonNull(swiftServer);
      this.location = Objects.requireNonNull(location);
      this.uuid = uuid;
      setName(location.getObjectName());
   }
  
   public String getUUID()
   {
      return uuid;
   }

   @Override
   protected Class<?>[] implsTypes()
   {
      return SUPPORTED_CLASSES;
   }

   @Override
   public <T> T getImpl(Class<? extends T> cl) throws UnsupportedOperationException
   {
      if (cl.isAssignableFrom(InputStream.class))
      {
         try
         {
            if (uuid != null)
            {
               return cl.cast(getObjectStorageURL().getUrl().openStream());
            }
            else
            {
               return cl.cast(getURL().getUrl().openStream());
            }
         }
         catch (IOException e)
         {
            throw new UnsupportedOperationException("URL stream cannot be opened", e);
         }
      }

      if (cl.isAssignableFrom(URL.class))
      {
         return cl.cast(getURL().getUrl());
      }

      if (cl.isAssignableFrom(DrbNodeImpl.class))
      {
         if (uuid != null)
         {
            return cl.cast(swiftFactory(uuid.concat(".zip")));
         }
         else
         {
            return cl.cast(swiftFactory(location.getObjectName()));
         }
      }

      if (cl.isAssignableFrom(DataStoreProduct.class))
      {
         return cl.cast(this);
      }

      throw new UnsupportedOperationException("No implementation for class " + cl.getName());
   }

   /**
    * Returns null if the corresponding SwiftObject is not found.
    */
   @Override
   public Long getContentLength()
   {
      SwiftObject obj = swift.getSwiftApi()
            .getObjectApi(location.getRegion(), location.getContainer())
            .getWithoutBody(location.getObjectName());

      return (obj != null) ? obj.getPayload().getContentMetadata().getContentLength() : null;
   }

   @Override
   public String getResourceLocation()
   {
      return location.toString();
   }

   private SwiftObjectURL getURL()
   {
      if (this.url == null)
      {
         this.url = new SwiftObjectURL(swift.getUrl(location.getObjectName(),
               location.getContainer(), location.getRegion(), 0));
      }
      return url;
   }
   
   private SwiftObjectURL getObjectStorageURL()
   {
      if (this.url == null)
      {
         this.url = new SwiftObjectURL(swift.getUrl(uuid.concat(".zip"),
               location.getContainer(), location.getRegion(), 0));
      }
      return url;
   }

   private DrbNode swiftFactory(String objectName)
   {
      return SwiftFactory.node(swift, location.getRegion(), location.getContainer(), objectName);
   }

   public String getRemoteName()
   {
      if (uuid != null)
      {
         return uuid.concat(".zip");
      }
      else
      {
         return getName();
      }
   }
   
   public void prepareDownloadInformation()
   {
      if (uuid != null)
      {
         DrbNode node = swiftFactory(uuid.concat(".zip"));
         setProperty(ProductConstants.CHECKSUM_MD5, node.getAttribute("contentMD5").getValue().toString());
         setProperty(ProductConstants.DATA_SIZE, Long.parseLong(node.getAttribute("contentLength").getValue().toString()));
      }
   }
}