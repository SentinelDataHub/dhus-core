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
package org.dhus.store.datastore.hfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.Product;
import org.dhus.ProductConstants;
import org.dhus.store.datastore.AbstractDataStore;
import org.dhus.store.datastore.DataStoreConstants;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.ProductNotFoundException;
import org.dhus.store.datastore.ReadOnlyDataStoreException;

import fr.gael.dhus.util.MultipleDigestInputStream;

public class HfsDataStore extends AbstractDataStore
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String SUPPORTED_ALGORITHMS = "MD5,SHA-1,SHA-256,SHA-512";

   /** The inner implementation of the hierarchical file system. */
   private HfsManager hfs;

   public HfsDataStore(String name, HfsManager hfs, boolean readOnly,
         int priority, long maximumSize, long currentSize, boolean autoEviction)
   {
      super(name, readOnly, priority, maximumSize, currentSize, autoEviction);
      this.hfs = hfs;

      super.setProperty(DataStoreConstants.PROP_KEY_TRANSFER_DIGEST, SUPPORTED_ALGORITHMS);
   }

   private void put(Product product, Path destination) throws IOException, DataStoreException
   {
      if (product.hasImpl(File.class))
      {
         Path productPath = product.getImpl(File.class).toPath();
         try
         {
            Files.createLink(destination, productPath);
            computeAndSetChecksum(productPath.toFile(), product);
         }
         catch (UnsupportedOperationException | IOException e)
         {
            copyProduct(product, destination);
         }
      }
      else if (product.hasImpl(InputStream.class))
      {
         copyProduct(product, destination);
      }
      else
      {
         // Case of data is not a SPI or it does not support both File and InputStream accesses (unlikely)
         throw new IOException("Input product \"" + product.getName() + "\" has no defined implementation for access");
      }

      // handle size and eviction
      long dataSize = Files.size(destination);
      product.setProperty(ProductConstants.DATA_SIZE, dataSize);

      // TODO increase current size of datastore BEFORE calling
      // on insert eviction, and change the way onInsertEviction
      // checks the size of the datastore

      // evicts products if necessary
      onInsertEviction(dataSize);
      // report DataStore size increase
      increaseCurrentSize(dataSize);
   }

   @Override
   protected Product internalGetProduct(String uuid, String tag) throws DataStoreException
   {
      if (internalHasProduct(uuid, tag))
      {
         String resource_location = getResource(uuid, tag);
         if (resource_location != null)
         {
            String path = HfsDataStoreUtils.generatePath(hfs.getPath(), resource_location);
            HfsProduct product = new HfsProduct(new File(path));
            product.setResourceLocation(resource_location);
            return product;
         }
      }
      throw new ProductNotFoundException();
   }

   @Override
   protected void internalAddProduct(String uuid, String tag, Product product) throws DataStoreException
   {
      if (isReadOnly())
      {
         throw new ReadOnlyDataStoreException(getName() + " datastore is read only");
      }

      try
      {
         Path path = getNextLocation(product);
         String destination = HfsDataStoreUtils.generateResource(hfs.getPath(), path.toString());

         putResource(uuid, tag, destination);
         put(product, path);
      }
      catch (IOException e)
      {
         throw new DataStoreException(e);
      }
   }

   private Path getNextLocation(Product product)
   {
      return hfs.getNewPath(product.getName()).toPath().resolve(product.getName());
   }

   @Override
   protected void internalDeleteProduct(String resourceLocation) throws DataStoreException
   {
      try
      {
         LOGGER.debug("HFS Path: {}, Resource Path: {}", hfs.getPath(), resourceLocation);
         String path = HfsDataStoreUtils.generatePath(hfs.getPath(), resourceLocation);
         LOGGER.debug("Complete Path: {}", path);
         long dataSize = hfs.sizeOf(path);
         hfs.delete(path);

         // report DataStore size decrease
         decreaseCurrentSize(dataSize);
      }
      catch (IOException e)
      {
         throw new DataStoreException(e);
      }
   }

   public HfsManager getHfs()
   {
      return hfs;
   }

   public void setHfs(HfsManager hfs)
   {
      this.hfs = hfs;
   }

   @Override
   public boolean canAccess(String resourceLocation)
   {
      HfsManager hfs = getHfs();
      return hfs.isContaining(new File(hfs.getPath(), resourceLocation));
   }
   
   private void computeAndSetChecksum(File file, Product product)
         throws IOException
   {
      byte[] buffer = new byte[1024 * 4];
      String[] algorithms = SUPPORTED_ALGORITHMS.split(",");
      MultipleDigestInputStream inputStream = null;
      try
      {
         inputStream = new MultipleDigestInputStream(new FileInputStream(file), algorithms);
         while (inputStream.read(buffer) != -1); // read file completely

      }
      catch (NoSuchAlgorithmException e)
      {
         // Should be never happen
         throw new IOException("Invalid supported algorithms !", e);
      }
      finally
      {
         if (inputStream != null)
         {
            inputStream.close();
         }
      }
      extractAndSetChecksum(inputStream, algorithms, product);
   }

   private void extractAndSetChecksum(MultipleDigestInputStream stream,
         String[] algorithms, Product product)
   {
      for (String algorithm: algorithms)
      {
         String k = ProductConstants.CHECKSUM_PREFIX + "." + algorithm;
         String v = stream.getMessageDigestAsHexadecimalString(algorithm);
         product.setProperty(k, v);
      }
   }

   /**
    * Copy the given product into the given destination path.
    * <p>
    * During the copy of the product checksum are computed and set to the given product.
    *
    * @param product     product to copy
    * @param destination copy destination path
    * @throws IOException if an I/O error occurs
    * @throws DataStoreException if current thread is interrupted during transfer
    */
   private void copyProduct(Product product, Path destination) throws IOException,
         DataStoreException
   {
      String[] algorithms = SUPPORTED_ALGORITHMS.split(",");
      if (product.hasImpl(InputStream.class))
      {
         InputStream input = product.getImpl(InputStream.class);
         OutputStream output = Files.newOutputStream(destination);
         try (MultipleDigestInputStream stream = new MultipleDigestInputStream(input, algorithms))
         {
            int count;
            byte[] buffer = new byte[4096];
            while ((count = stream.read(buffer)) != -1)
            {
               if (Thread.currentThread().isInterrupted())
               {
                  throw new DataStoreException("Transfer interrupted");
               }
               output.write(buffer, 0, count);
            }
            extractAndSetChecksum(stream, algorithms, product);
         }
         catch (NoSuchAlgorithmException e)
         {
            throw new IOException("Invalid hash function", e);
         }
         finally
         {
            output.close();
         }
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
      if (!(obj instanceof HfsDataStore))
      {
         return false;
      }
      HfsDataStore other = (HfsDataStore) obj;
      return super.equals(obj) && other.hfs.getPath().equals(hfs.getPath());
   }

   @Override
   public int hashCode()
   {
      return super.hashCode() + hfs.hashCode();
   }

   @Override
   protected long getProductSize(String id) throws DataStoreException
   {
      return ((HfsProduct) get(id)).getContentLength();
   }
}
