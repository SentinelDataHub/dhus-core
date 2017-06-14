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

import fr.gael.dhus.database.object.config.system.IncomingConfiguration;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.Product;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.ProductNotFoundException;
import org.dhus.store.datastore.ReadOnlyDataStoreException;

/**
 * DataStore for backward compatibility.
 */
public class OldIncomingDataStore implements DataStore
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final String DEFAULT_NAME = "OldDHuSIncomingDataStore";

   private final ProductService productService;
   private final HfsManager hfs;
   private final String name;

   public OldIncomingDataStore()
   {
      this.name = DEFAULT_NAME;
      ConfigurationManager config = ApplicationContextProvider.getBean(ConfigurationManager.class);
      IncomingConfiguration old_incoming = config.getArchiveConfiguration().getIncomingConfiguration();

      this.productService = ApplicationContextProvider.getBean(ProductService.class);
      this.hfs = new HfsManager(old_incoming.getPath(), old_incoming.getMaxFileNo());
   }

   @Override
   public String getName()
   {
      return name;
   }

   @Override
   public Product get(String id) throws DataStoreException
   {
      fr.gael.dhus.database.object.Product product = productService.systemGetProduct(id);

      if (product == null)
      {
         throw new ProductNotFoundException();
      }

      final String path = product.getDownloadablePath();
      return new HfsProduct(new File(path))
      {
         @Override
         public String getResourceLocation()
         {
            return HfsDataStoreUtils.generateResource(hfs.getPath(), path);
         }
      };
   }

   @Override
   public void set(String id, Product product) throws DataStoreException
   {
      throw new ReadOnlyDataStoreException(getName() + " datastore is read only");
   }

   @Override
   public void move(String id, Product product) throws DataStoreException
   {
      throw new ReadOnlyDataStoreException(getName() + " datastore is read only");
   }

   @Override
   public void delete(String id) throws DataStoreException
   {
      if (exists(id))
      {
         fr.gael.dhus.database.object.Product product = productService.systemGetProduct(id);
         String path = product.getDownloadablePath();
         try
         {
            if (product.getThumbnailFlag())
            {
               hfs.delete(product.getThumbnailPath());
            }
            if (product.getQuicklookFlag())
            {
               hfs.delete(product.getQuicklookPath());
            }
            hfs.delete(path);
         }
         catch (IOException e)
         {
            LOGGER.warn("Cannot delete physical product at {}", path);
         }
      }
   }

   @Override
   public boolean exists(String id)
   {
      return productService.systemGetProduct(id) != null;
   }

   @Override
   public boolean addProductReference(String id, Product product) throws DataStoreException
   {
      throw new ReadOnlyDataStoreException();
   }

   @Override
   public boolean canAccess(String resource_location)
   {
      return hfs.isInIncoming(new File(hfs.getPath(), resource_location));
   }
}
