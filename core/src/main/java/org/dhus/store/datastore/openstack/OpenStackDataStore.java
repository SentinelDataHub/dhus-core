/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016,2017,2018 GAEL Systems
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

import fr.gael.dhus.util.MultipleDigestInputStream;
import fr.gael.drb.impl.swift.DrbSwiftObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.Product;
import org.dhus.ProductConstants;
import org.dhus.store.datastore.AbstractDataStore;
import org.dhus.store.datastore.DataStoreConstants;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.ProductAlreadyExist;
import org.dhus.store.datastore.ProductNotFoundException;
import org.dhus.store.datastore.ReadOnlyDataStoreException;

import org.jclouds.blobstore.domain.BlobMetadata;

/**
 * Implementation of OpenStack DataStore.
 * This version only supports swift API, but regarding drbx-impl-swift implementation it could be
 * possible to manage other implementations (amazon s3, ...).
 */
public class OpenStackDataStore extends AbstractDataStore
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String SUPPORTED_DIGEST_ALGORITHMS = "MD5,SHA-1,SHA-256,SHA-512";

   /**
    * Occurrence MetaData, allows to manage multiple references of the same object in a
    * Swift OpenStack container.
    */

   private final String provider;
   private final String identity;
   private final String credential;
   private final String url;
   private final String container;
   private final String region;

   private DrbSwiftObject ostack;

   /**
    * Build an open-stack storage API for DHuS data store. Currently only swift is supported.
    *
    * @param name
    * @param provider
    * @param identity
    * @param credential
    * @param url
    * @param container
    * @param region
    * @param readOnly
    * @param priority
    * @param maximumSize
    * @param currentSize
    * @param autoEviction
    */
   public OpenStackDataStore(String name, String provider, String identity, String credential,
         String url, String container, String region, boolean readOnly, int priority,
         long maximumSize, long currentSize, boolean autoEviction)
   {
      super(name, readOnly, priority, maximumSize, currentSize, autoEviction);
      this.provider = provider;
      this.identity = identity;
      this.credential = credential;
      this.url = url;
      this.container = container;
      this.region = region;
      super.setProperty(DataStoreConstants.PROP_KEY_TRANSFER_DIGEST, SUPPORTED_DIGEST_ALGORITHMS);
   }

   /**
    * Retrieve the OpenStack storage object from the construction parameters.
    *
    * @return the object to manipulate OpenStack
    */
   private DrbSwiftObject getOpenStackObject()
   {
      boolean reset = false;
      if (reset || (this.ostack == null))
      {
         this.ostack = new DrbSwiftObject(this.url, this.provider, this.identity, this.credential);
      }
      return this.ostack;
   }

   /**
    * Retrieves content size of the given product.
    *
    * @param product to check
    * @return content size of the product in byte, or -1 if not found
    */
   private long productContentSize(Product product)
   {
      Long size = (Long) product.getProperty(ProductConstants.DATA_SIZE);

      if (size != null && size != -1)
      {
         return size;
      }

      if (product.hasImpl(File.class))
      {
         return product.getImpl(File.class).length();
      }

      return -1;
   }

   /**
    * Inner put performs put action only using stream.
    * If file not supported, product size will not be set into the HTTP content-length header,
    * and the move command will raise exception.
    *
    * @param product the product to copy.
    * @param move    determines if the product moved or only copied
    *
    * @return a set of data, allowing to retrieve the OpenStack Swift object
    *
    * @throws IOException if transfer fails
    */
   private String put(Product product, boolean move) throws IOException
   {
      long product_size = productContentSize(product);
      Map<String, String> user_data = new HashMap<>();

      // Case of source file not supported
      if (product.hasImpl(InputStream.class))
      {
         String[] algorithms = SUPPORTED_DIGEST_ALGORITHMS.split(",");

         try (MultipleDigestInputStream source =
               new MultipleDigestInputStream(product.getImpl(InputStream.class), algorithms))
         {
            getOpenStackObject()
                  .putObject(source, product.getName(), container, region, product_size, user_data);
            for (String algorithm: algorithms)
            {
               String key = ProductConstants.CHECKSUM_PREFIX + "." + algorithm;
               product.setProperty(key, source.getMessageDigestAsHexadecimalString(algorithm));
            }
         }
         catch (NoSuchAlgorithmException e)
         {
            throw new IOException("Checksum computation error.", e);
         }

         OpenStackLocation location = new OpenStackLocation(region, container, product.getName());
         return location.toResourceLocation();
      }
      // Case of data is not a SPI or it does not support both File and
      // InputStream accesses ... Don't think it is possible!
      throw new IOException("Input product \"" + product.getName() +
            "\" has no defined implementation for access.");
   }

   @Override
   protected final Product internalGetProduct(String uuid, String tag) throws DataStoreException
   {
      if (!internalHasProduct(uuid, tag))
      {
         throw new ProductNotFoundException();
      }

      OpenStackLocation location;
      try
      {
         location = new OpenStackLocation(getResource(uuid, tag));
      }
      catch (IllegalArgumentException e)
      {
         throw new DataStoreException(
               String.format("Invalid resource location for product: %s (%s)", uuid, tag), e);
      }

      return new OpenStackProduct(getOpenStackObject(), location);
   }

   @Override
   protected final void internalAddProduct(String id, String tag, Product product) throws DataStoreException
   {
      if (isReadOnly())
      {
         throw new ReadOnlyDataStoreException("DataStore " + getName() + " is read only");
      }

      if (internalHasProduct(id, tag))
      {
         throw new ProductAlreadyExist();
      }

      DrbSwiftObject open_stack = getOpenStackObject();
      String object_name = product.getName();

      boolean duplicate = open_stack.exists(object_name, container, region);
      if (duplicate)
      {
         LOGGER.warn("Product '{}' already found in '{}/{}', not pushing it again", object_name, container, region);
         putResource(id, tag, new OpenStackLocation(region, container, object_name).toResourceLocation());
      }
      else
      {
         try
         {
            // FIXME call "put" before "putResource" in order to keep a reference
            // of the data before transferring it
            String path = put(product, false);
            putResource(id, tag, path);
         }
         catch (IOException e)
         {
            throw new DataStoreException(e);
         }
      }

      long dataSize = open_stack.getMetadata(container, region, object_name).getSize();

      if (!duplicate)
      {
         // evicts products if necessary
         onInsertEviction(dataSize);
         // report DataStore size increase
         increaseCurrentSize(dataSize);
      }

      if (!product.getPropertyNames().contains(ProductConstants.DATA_SIZE))
      {
         product.setProperty(ProductConstants.DATA_SIZE, dataSize);
      }
   }

   @Override
   protected final void internalDeleteProduct(String resourceLocation)
         throws ProductNotFoundException, ReadOnlyDataStoreException
   {
      DrbSwiftObject open_stack = getOpenStackObject();

      OpenStackLocation object_location = new OpenStackLocation(resourceLocation);
      String object_name = object_location.getObjectName();

      if (open_stack.exists(object_name, container, region))
      {
         BlobMetadata blobMetadata = open_stack.getMetadata(container, region, object_name);
         long dataSize = blobMetadata.getSize();

         getOpenStackObject().deleteObject(object_name, this.container, this.region);

         // report DataStore size decrease
         decreaseCurrentSize(dataSize);
      }
   }

   @Override
   public boolean canAccess(String resource_location)
   {
      try
      {
         OpenStackLocation location = new OpenStackLocation(resource_location);
         return getOpenStackObject().exists(location.getObjectName(), container, region);
      }
      catch (IllegalArgumentException ex)
      {
         return false;
      }
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null)
      {
         return false;
      }
      if (obj == this)
      {
         return true;
      }
      if (!(obj instanceof OpenStackDataStore))
      {
         return false;
      }
      OpenStackDataStore other = (OpenStackDataStore) obj;
      return super.equals(obj) && (other.url.equals(url))
            && (other.region.equals(region)) && (other.container.equals(container));
   }

   @Override
   public int hashCode()
   {
      return super.hashCode() + url.hashCode() + region.hashCode() +
            container.hashCode();
   }

   public String getProvider()
   {
      return provider;
   }

   public String getIdentity()
   {
      return identity;
   }

   public String getCredential()
   {
      return credential;
   }

   public String getUrl()
   {
      return url;
   }

   public String getContainer()
   {
      return container;
   }

   public String getRegion()
   {
      return region;
   }

   @Override
   protected long getProductSize(String id) throws DataStoreException
   {
      Long size = ((OpenStackProduct) get(id)).getContentLength();
      return size == null ? 0 : size;
   }
}
