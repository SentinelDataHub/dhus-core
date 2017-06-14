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

import fr.gael.drb.DrbNode;
import fr.gael.drb.impl.DrbNodeImpl;
import fr.gael.drb.impl.swift.DrbSwiftObject;
import fr.gael.drb.impl.swift.SwiftFactory;
import fr.gael.drb.impl.swift.SwiftObjectURL;
import org.dhus.AbstractProduct;
import org.dhus.store.datastore.DataStoreProduct;
import org.jclouds.openstack.swift.v1.domain.SwiftObject;

public class OpenStackProduct extends AbstractProduct implements DataStoreProduct
{
   private static final Class<?>[] SUPPORTED_CLASSES =
   {
      InputStream.class, DrbNode.class, URL.class, DataStoreProduct.class
   };

   private final DrbSwiftObject swift;
   private final OpenStackLocation location;

   private SwiftObjectURL url;

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
   OpenStackProduct(DrbSwiftObject swiftServer, OpenStackLocation location)
   {
      this.swift = Objects.requireNonNull(swiftServer);
      this.location = Objects.requireNonNull(location);
      setName(location.getObjectName());
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
            return cl.cast(getURL().getUrl().openStream());
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
         return cl.cast(SwiftFactory.node(swift, location.getRegion(),
               location.getContainer(), location.getObjectName()));
      }

      if (cl.isAssignableFrom(DataStoreProduct.class))
      {
         return cl.cast(this);
      }

      throw new UnsupportedOperationException("No implementation for class " + cl.getName());
   }

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
}
