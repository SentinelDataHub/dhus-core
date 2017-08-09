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

import fr.gael.dhus.util.MultipleDigestInputStream;
import fr.gael.drb.impl.swift.DrbSwiftObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.Product;
import org.dhus.ProductConstants;
import org.dhus.store.datastore.AbstractDataStore;
import org.dhus.store.datastore.DataStoreConstants;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.ProductAlreadyExist;
import org.dhus.store.datastore.ProductNotFoundException;
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
   private static final String MD_OCCURRENCE = "occurrence";

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
    */
   public OpenStackDataStore(String name, String provider, String identity, String credential,
         String url, String container, String region, boolean readOnly)
   {
      super(name, readOnly);
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

   @Override
   public void set(String id, Product product) throws DataStoreException
   {
      if (isReadOnly())
      {
         throw new UnsupportedOperationException("DataStore " + getName() + " is read only");
      }

      if (exists(id))
      {
         throw new ProductAlreadyExist();
      }

      DrbSwiftObject open_stack = getOpenStackObject();
      String object_name = product.getName();

      if (open_stack.exists(object_name, container, region))
      {
         incrementOccurrence(product.getName(), 1);
         putResource(id, new OpenStackLocation(region, container, object_name).toResourceLocation());
      }
      else
      {
         try
         {
            String path = put(product, false);
            putResource(id, path);
         }
         catch (IOException e)
         {
            throw new RuntimeException(e);
         }
      }

      if (!product.getPropertyNames().contains(ProductConstants.DATA_SIZE))
      {
         BlobMetadata md = open_stack.getMetadata(
               container, region, object_name);
         product.setProperty(ProductConstants.DATA_SIZE, md.getSize());
      }
   }

   /**
    * Updates occurrence MetaData value (by addition) of an object.
    *
    * @param object_name object name
    * @param value       added value to the current occurrence of object
    */
   private void incrementOccurrence(String object_name, Integer value)
   {
      DrbSwiftObject openStack = getOpenStackObject();
      Map<String, String> metadata = openStack.getMetadata(container, region, object_name).getUserMetadata();

      String occurrence_value = metadata.get(MD_OCCURRENCE);
      Integer occurrence = Integer.parseInt(occurrence_value) + value;

      Map<String, String> input_metadata = new HashMap<>(metadata);
      input_metadata.put(MD_OCCURRENCE, occurrence.toString());
      getOpenStackObject().setMetadata(container, region, object_name, input_metadata);
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
      long product_size = -1L;
      Map<String, String> user_data = new HashMap<>();
      user_data.put(MD_OCCURRENCE, "1");

      if (product.hasImpl(File.class))
      {
         product_size = product.getImpl(File.class).length();
      }

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
   public Product get(String id) throws DataStoreException
   {
      if (!exists(id))
      {
         throw new ProductNotFoundException();
      }

      OpenStackLocation location;
      try
      {
         location = new OpenStackLocation(getResource(id));
      }
      catch (IllegalArgumentException e)
      {
         throw new DataStoreException("Invalid resource location for product: " + id, e);
      }

      return new OpenStackProduct(getOpenStackObject(), location);
   }

   // FIXME isReadOnly is never called in this method because it relies on
   // openstack's occurrence feature
   // this approach may lead to incoherent situations and a proper read-only
   // and data ownership approach should be adopted instead
   @Override
   public void delete(String id) throws DataStoreException
   {
      if (!exists(id))
      {
         throw new ProductNotFoundException();
      }

      LOGGER.info("Deleting Product reference {}, from {} DataStore", id, getName());
      DrbSwiftObject open_stack = getOpenStackObject ();
      OpenStackLocation object_location = new OpenStackLocation(getResource(id));
      String object_name = object_location.getObjectName();
      removeResource(id);

      if (open_stack.exists (object_name, container, region))
      {
         Map<String, String> metadata =
               open_stack.getMetadata(container, region, object_name).getUserMetadata();
         String occurrence_value = metadata.get(MD_OCCURRENCE);

         if (occurrence_value == null)
         {
            LOGGER.warn("Object not instantiated by a DHuS: {}", object_name);
         }

         int occurrence = Integer.parseInt(occurrence_value) - 1;
         if (occurrence == 0)
         {
            LOGGER.info("Product data {} deleted from {} DataStore", id, getName());
            getOpenStackObject().deleteObject(object_name, this.container, this.region);
         }
         else
         {
            incrementOccurrence(object_name, -1);
         }
      }
   }

   @Override
   public void move(String id, Product product) throws DataStoreException
   {
      set(id, product);
      if (product.hasImpl(File.class))
      {
         FileUtils.deleteQuietly(product.getImpl(File.class));
      }
      else
      {
         LOGGER.warn("Cannot remove source stream");
      }
   }

   // FIXME should not increment product occurrence to respect product
   // ownership, see comment delete method
   @Override
   protected boolean onAddProductReference(String resource)
   {
      OpenStackLocation location = new OpenStackLocation(resource);
      incrementOccurrence(location.getObjectName(), 1);
      return true;
   }

   @Override
   public boolean canAccess(String resource_location)
   {
      OpenStackLocation location = new OpenStackLocation(resource_location);
      return getOpenStackObject().exists(location.getObjectName(), container, region);
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
}
