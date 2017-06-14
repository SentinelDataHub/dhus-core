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

import fr.gael.drb.DrbFactory;
import fr.gael.drb.DrbNode;
import fr.gael.drb.DrbSequence;
import fr.gael.drb.impl.DrbNodeImpl;
import fr.gael.drb.query.Query;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

import org.dhus.AbstractProduct;
import org.dhus.store.datastore.DataStoreProduct;

/**
 * An abstract representation of the object Product within the data store.
 */
public class HfsProduct extends AbstractProduct implements DataStoreProduct
{
   private File product_file = null;
   private DrbNode product_node = null;
   private URL product_url = null;
   private Long contentLength;
   private String resourceLocation;

   public HfsProduct(File product)
   {
      product_file = product;
      setName(product.getName());
   }

   public HfsProduct(DrbNode product)
   {
      product_node = product;
      setName(product.getName());
   }

   public HfsProduct(URL url, String name)
   {
      product_url = url;
      setName(name);
   }

   private File getFile()
   {
      if (this.product_file == null)
      {
         if (this.product_node != null)
         {
            this.product_file = new File(this.product_node.getPath());
         }
         else if (this.product_url != null)
         {
            this.product_file = new File(this.product_url.getPath());
         }
         else
         {
            // Cannot happen
            throw new IllegalStateException("Product was not correctly constructed");
         }
      }
      return this.product_file;
   }

   private URL getUrl()
   {
      if (this.product_url == null)
      {
         try
         {
            if (this.product_file != null)
            {
               this.product_url = this.product_file.toURI().toURL();
            }
            else if (this.product_node != null)
            {
               this.product_url = Paths.get(this.product_node.getPath()).toUri().toURL();
            }
            else
            {
               // Cannot happen
               throw new IllegalStateException("Product was not correctly constructed");
            }
         }
         catch (MalformedURLException ex)
         {
         } // Should never happen
      }
      return this.product_url;
   }

   private DrbNode getDrbNode()
   {
      if (this.product_node == null)
      {
         if (this.product_file != null)
         {
            this.product_node = DrbFactory.openURI(this.product_file.getAbsolutePath());
         }
         else if (this.product_url != null)
         {
            this.product_node = DrbFactory.openURI(this.product_file.getAbsolutePath());
         }
      }
      return this.product_node;
   }

   /**
    * Service Provider section : this implementation must be spread into dedicated classes
    */
   @Override
   public boolean hasImpl(Class<?> cl)
   {
      // Check if this is a file and defines compatible implementations
      if ((getFile() != null)
            && (cl.isAssignableFrom(File.class)
             || cl.isAssignableFrom(InputStream.class)
             || cl.isAssignableFrom(DrbNode.class)))
      {
         return true;
      }

      // Checks is this is a node and defines compatible implementations
      if (getDrbNode() != null)
      {
         if (cl.isAssignableFrom(DrbNode.class))
         {
            return true;
         }
         if (getDrbNode() instanceof DrbNodeImpl)
         {
            return ((DrbNodeImpl) getDrbNode()).hasImpl(cl);
         }
      }
      // Checks if this is a stream/URL
      if ((getUrl() != null)
            && (cl.isAssignableFrom(InputStream.class) || cl.isAssignableFrom(URL.class)))
      {
         return true;
      }

      if (cl.isAssignableFrom(DataStoreProduct.class))
      {
         return true;
      }

      // Otherwise : not supported
      return false;
   }

   @Override
   public <T> T getImpl(Class<? extends T> cl) throws UnsupportedOperationException
   {
      if (cl.isAssignableFrom(DataStoreProduct.class))
      {
         return cl.cast(this);
      }

      // returns a file compatible implementation
      if ((getFile() != null) && cl.isAssignableFrom(File.class))
      {
         return cl.cast(getFile());
      }

      if ((getFile() != null) && cl.isAssignableFrom(InputStream.class))
      {
         try
         {
            return cl.cast(new FileInputStream(getFile()));
         }
         catch (IOException e)
         {
            throw new UnsupportedOperationException("Error creating stream from file " + getFile().getPath(), e);
         }
      }

      if ((getFile() != null) && cl.isAssignableFrom(DrbNode.class))
      {
         try
         {
            String path = getFile().getPath();
            String query_string = "fn:doc('" + path + "')";
            Query query = new Query(query_string);
            DrbSequence sequence = query.evaluate(DrbFactory.openURI("."));
            DrbNode node = (DrbNode) sequence.getItem(0);
            return cl.cast(node);
         }
         catch (Exception e)
         {
            throw new UnsupportedOperationException("Cannot open DrbNode from file " + getFile().getPath(), e);
         }
      }

      // returns a node compatible implementations
      if ((getDrbNode() != null) && cl.isAssignableFrom(DrbNode.class))
      {
         return cl.cast(getDrbNode());
      }

      if (getDrbNode() != null)
      {
         if (getDrbNode() instanceof DrbNodeImpl)
         {
            Object impl = ((DrbNodeImpl) getDrbNode()).getImpl(cl);
            if (impl != null)
            {
               return cl.cast(impl);
            }
         }
      }
      if (getUrl() != null)
      {
         if (cl.isAssignableFrom(InputStream.class))
         {
            try
            {
               return cl.cast(getUrl().openStream());
            }
            catch (IOException e)
            {
               throw new RuntimeException("Cannot build the stream for URL " + getUrl().toString(), e);
            }
         }

         if (cl.isAssignableFrom(URL.class))
         {
            return cl.cast(getUrl());
         }
      }

      // Otherwise : not supported
      throw new UnsupportedOperationException(cl.getName() + "not supported.");
   }

   @Override
   public Long getContentLength()
   {
      if (contentLength == null)
      {
         if (getFile() != null && getFile().exists())
         {
            if (getFile().isFile())
            {
               contentLength = getFile().length();
            }
            else
            {
               contentLength = UNKNOWN_PRODUCT_SIZE;
            }
         }
      }
      return contentLength;
   }

   public void setContentLength(Long content_length)
   {
      this.contentLength = content_length;
   }

   @Override
   public String getResourceLocation()
   {
      return resourceLocation;
   }

   public void setResourceLocation(String resource_location)
   {
      this.resourceLocation = resource_location;
   }
}
