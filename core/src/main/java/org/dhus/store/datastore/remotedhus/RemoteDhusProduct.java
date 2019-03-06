/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018 GAEL Systems
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
package org.dhus.store.datastore.remotedhus;

import fr.gael.dhus.olingo.ODataClient;
import fr.gael.dhus.util.http.BasicAuthHttpClientProducer;
import fr.gael.dhus.util.http.DownloadableProduct;
import fr.gael.dhus.util.http.InterruptibleHttpClient;
import fr.gael.drb.DrbNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.dhus.store.datastore.DataStoreProduct;

// TODO refactor without DownloadableProduct
public class RemoteDhusProduct extends DownloadableProduct implements DataStoreProduct
{
   private final String serviceUrl;
   private final String uuid;
   private final String login;
   private final String password;
   private final ODataClient odataClient;

   // make a factory method instead ?
   public RemoteDhusProduct(String serviceUrl, String uuid, String login, String password, ODataClient odataClient)
         throws IOException, InterruptedException
   {
      super(
            new InterruptibleHttpClient(new BasicAuthHttpClientProducer(login, password)),
            1,
            getProductStreamURI(serviceUrl, uuid),
            null,
            "unknown");
      this.serviceUrl = serviceUrl;
      this.uuid = uuid;
      this.login = login;
      this.password = password;
      this.odataClient = odataClient;
      setName(filename);
   }

   @Override
   protected Class<?>[] implsTypes()
   {
      return new Class<?>[]
      {
         InputStream.class, DrbNode.class, URL.class, DataStoreProduct.class
      };
   }

   @Override
   public boolean hasImpl(Class<?> cl)
   {
      return Arrays.asList(implsTypes()).contains(cl);
   }

   @Override
   public <T> T getImpl(Class<? extends T> claff)
   {
      // InputStream thanks to DownloadableProduct
      T impl = super.getImpl(claff);
      if (impl != null)
      {
         return impl;
      }

      if (claff.isAssignableFrom(URL.class))
      {
         try
         {
            return claff.cast(new URL(url));
         }
         catch (MalformedURLException e)
         {
            throw new IllegalStateException("URL of this product is invalid", e);
         }
      }
      if (claff.isAssignableFrom(DrbNode.class))
      {
         return claff.cast(new DhusODataV1Node(
               getProductEntryURI(serviceUrl, uuid), 
               login, 
               password, 
               odataClient));
      }
      if (claff.isAssignableFrom(DataStoreProduct.class))
      {
         return claff.cast(this);
      }
      throw new UnsupportedOperationException();
   }

   @Override
   public Long getContentLength()
   {
      return contentLength;
   }

   @Override
   public String getResourceLocation()
   {
      return url;
   }

   private static String getProductStreamURI(String serviceUrl, String uuid)
   {
      return getProductEntryURI(serviceUrl, uuid) + "/$value";
   }

   private static String getProductEntryURI(String serviceUrl, String uuid)
   {
      return serviceUrl + "/Products('" + uuid + "')";
   }
}
