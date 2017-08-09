/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
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
package org.dhus;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.store.datastore.hfs.HfsProduct;

public class ProductFactory
{
   private static final String FILE_PROTOCOL = "file";
   private static final String HTTP_PROTOCOL = "http";
   private static final String FTP_PROTOCOL = "ftp";
   private static final Logger LOGGER = LogManager.getLogger();

   public static Product generateProduct(final URL url)
   {
      LOGGER.debug("Product path: {}", url);
      // No Product can be generated for a null url
      if (url == null)
      {
         return null;
      }
      String protocol = url.getProtocol();
      if (protocol.equals(FILE_PROTOCOL))
      {
         return getProductFromPath(Paths.get(url.getPath()));
      }
      else
      {
         Product product = new AbstractProduct()
         {
            @Override
            protected Class<?>[] implsTypes()
            {
               return new Class[]
               {
                  InputStream.class
               };
            }

            @Override
            public <T> T getImpl(Class<? extends T> cl)
            {
               if (cl.isAssignableFrom(InputStream.class))
               {
                  try
                  {
                     return cl.cast(url.openConnection().getInputStream());
                  }
                  catch (IOException e)
                  {
                     LOGGER.error("Cannot open stream from '{}'", url, e);
                  }
               }
               return null;
            }
         };
         product.setName(url.getFile());
         return product;
      }
   }

   private static Product getProductFromPath(Path path)
   {
      return new HfsProduct(path.toFile());
   }
}
