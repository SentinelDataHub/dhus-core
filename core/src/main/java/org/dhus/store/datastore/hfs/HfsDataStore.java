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
package org.dhus.store.datastore.hfs;

import fr.gael.dhus.util.MultipleDigestInputStream;
import fr.gael.dhus.util.MultipleDigestOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.Product;
import org.dhus.ProductConstants;
import org.dhus.store.datastore.AbstractDataStore;
import org.dhus.store.datastore.DataStoreConstants;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.ProductNotFoundException;
import org.dhus.store.datastore.ReadOnlyDataStoreException;

public class HfsDataStore extends AbstractDataStore
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String SUPPORTED_ALGORITHMS = "MD5,SHA-1,SHA-256,SHA-512";

   /** The inner implementation of the hierarchical file system. */
   private HfsManager hfs;

   public HfsDataStore(String name, HfsManager hfs, boolean readOnly)
   {
      super(name, readOnly);
      this.hfs = hfs;

      super.setProperty(DataStoreConstants.PROP_KEY_TRANSFER_DIGEST, SUPPORTED_ALGORITHMS);
   }

   @Override
   public void set(String id, Product product) throws DataStoreException
   {
      if (isReadOnly())
      {
         throw new ReadOnlyDataStoreException(getName() + " datastore is read only");
      }

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

   @Override
   public void move(String id, Product product) throws DataStoreException
   {
      if (isReadOnly())
      {
         throw new ReadOnlyDataStoreException(getName() + " datastore is read only");
      }

      try
      {
         String resource = put(product, true);
         putResource(id, resource);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   private String put(Product product, boolean move) throws IOException
   {
      // Compute the target
      File dest = new File(hfs.getNewIncomingPath(), product.getName());

      // Computes the source
      if (product.hasImpl(File.class))
      {
         File source = product.getImpl(File.class);
         if (move)
         {
            if (source.isDirectory())
            {
               FileUtils.moveDirectory(source, dest);
            }
            else
            {
               Files.move(source.toPath(), dest.toPath());
               computeAndSetChecksum(dest, product);
            }
         }
         else
         {
            if (source.isDirectory())
            {
               FileUtils.copyDirectory(source, dest);
            }
            else
            {
               String[] algorithms = SUPPORTED_ALGORITHMS.split(",");
               try (MultipleDigestOutputStream outputStream =
                     new MultipleDigestOutputStream(new FileOutputStream(dest), algorithms))
               {
                  // store and compute checksum
                  FileUtils.copyFile(source, outputStream);
                  extractAndSetChecksum(outputStream, algorithms, product);
               }
               catch (NoSuchAlgorithmException e)
               {
                  // Should be never happen
                  throw new IOException("Invalid supported algorithms !", e);
               }
            }
         }

         product.setProperty(ProductConstants.DATA_SIZE, dest.length());
         return HfsDataStoreUtils.generateResource(hfs.getPath(), dest.getAbsolutePath());
      }

      // Case of source file not supported
      if (product.hasImpl(InputStream.class))
      {
         String[] algorithms = SUPPORTED_ALGORITHMS.split(",");
         try (InputStream source = product.getImpl(InputStream.class))
         {
            try (MultipleDigestOutputStream bos =
                  new MultipleDigestOutputStream(new FileOutputStream(dest), algorithms))
            {
               IOUtils.copy(source, bos);
               extractAndSetChecksum(bos, algorithms, product);
            }
            catch (NoSuchAlgorithmException e)
            {
               // Should be never happen
               throw new IOException("Invalid supported algorithms !", e);
            }
         }

         // Move is not possible for input streams: at least raise a warning
         if (move)
         {
            LOGGER.warn("Cannot move source stream.");
         }

         return HfsDataStoreUtils.generateResource(hfs.getPath(), dest.getAbsolutePath());
      }
      // Case of data is not a SPI or it does not support both File and InputStream accesses (unlikely)
      throw new IOException("Input product \"" + product.getName() +
            "\" has no defined implementation for access.");
   }

   @Override
   public Product get(String id) throws DataStoreException
   {
      if (exists(id))
      {
         String resource_location = getResource(id);
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
   public void delete(String id) throws DataStoreException
   {
      if (exists(id))
      {
         try
         {
            String resource = getResource(id);
            removeResource(id);
            if (!isReadOnly())
            {
               LOGGER.debug("HFS Path: {}, Resource Path: {}", hfs.getPath(), resource);
               String path = HfsDataStoreUtils.generatePath(hfs.getPath(), resource);
               LOGGER.debug("Complete Path: {}", path);
               hfs.delete(path);
            }
         }
         catch (IOException e)
         {
            throw new RuntimeException(e);
         }
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
      return hfs.isInIncoming(new File(hfs.getPath(), resourceLocation));
   }

   @Override
   protected boolean onAddProductReference(String resource)
   {
      // no additional action
      return true;
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

   private void extractAndSetChecksum(MultipleDigestOutputStream stream,
         String[] algorithms, Product product)
   {
      for (String algorithm: algorithms)
      {
         String k = ProductConstants.CHECKSUM_PREFIX + "." + algorithm;
         String v = stream.getMessageDigestAsHexadecimalString(algorithm);
         product.setProperty(k, v);
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
}
