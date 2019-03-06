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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.odata2.api.exception.ODataException;

import org.dhus.Product;
import org.dhus.store.StoreException;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.ReadOnlyDataStoreException;
import org.dhus.store.ingestion.IngestibleProduct;

import fr.gael.dhus.olingo.ODataClient;

/**
 * Does not support derived products.
 */
public class RemoteDhusDataStore implements DataStore
{
   private static final Logger LOGGER = LogManager.getLogger();

   // datastore management fields
   private final String name;
   private final int priority;

   // functional field
   private final String serviceUrl;
   private final String login;
   private final String password;
   private final ODataClient odataClient;

   public RemoteDhusDataStore(String name, String serviceUrl, String login, String password, int priority)
         throws URISyntaxException, IOException, ODataException
   {
      this.name = name;
      this.serviceUrl = serviceUrl;
      this.login = login;
      this.password = password;
      this.odataClient = new ODataClient(serviceUrl, login, password);
      this.priority = priority;
   }

   @Override
   public boolean isReadOnly()
   {
      return true;
   }

   @Override
   public String getName()
   {
      return name;
   }

   @Override
   public int getPriority()
   {
      return priority;
   }

   @Override
   public Product get(String uuid) throws DataStoreException
   {
      try
      {
         return new RemoteDhusProduct(serviceUrl, uuid, login, password, odataClient);
      }
      catch (IOException | InterruptedException e)
      {
         // FIXME might be product not found ?
         LOGGER.debug("A problem occured while retrieving a distant DHuS product", e);
         throw new DataStoreException("Cannot access product on remote DHuS instance");
      }
   }

   @Override
   public boolean hasProduct(String uuid)
   {
      try
      {
         new RemoteDhusProduct(serviceUrl, uuid, login, password, odataClient).close();
         return true;
      }
      catch (IOException | InterruptedException e)
      {
         LOGGER.debug("A problem occured while checking a distant DHuS product", e);
         return false;
      }
   }

   @Override
   public boolean canAccess(String resource_location)
   {
      return false;
   }

   @Override
   public List<String> getProductList()
   {
      LOGGER.warn("Product listing not supported for RemoteDhusDataStore");
      return Collections.emptyList();
   }

   @Override
   public void addProduct(IngestibleProduct inProduct) throws StoreException
   {
      throw new ReadOnlyDataStoreException("DHuSDataStore is read-only");
   }

   @Override
   public void set(String uuid, Product product) throws DataStoreException
   {
      throw new ReadOnlyDataStoreException("DHuSDataStore is read-only");
   }

   @Override
   public boolean addProductReference(String uuid, Product product) throws DataStoreException
   {
      throw new ReadOnlyDataStoreException("DHuSDataStore is read-only");
   }

   @Override
   public void deleteProduct(String uuid) throws DataStoreException
   {
      throw new ReadOnlyDataStoreException("DHuSDataStore is read-only");
   }
}
