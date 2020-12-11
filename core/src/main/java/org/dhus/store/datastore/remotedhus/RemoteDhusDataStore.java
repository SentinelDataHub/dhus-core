/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018,2019 GAEL Systems
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
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;

import org.dhus.Product;
import org.dhus.store.StoreException;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.DataStoreProduct;
import org.dhus.store.datastore.ProductNotFoundException;
import org.dhus.store.datastore.ReadOnlyDataStoreException;
import org.dhus.store.datastore.config.DataStoreRestriction;
import org.dhus.store.derived.DerivedProductStore;
import org.dhus.store.ingestion.IngestibleProduct;

import fr.gael.dhus.olingo.ODataClient;

/**
 * Remote DHuS DataStore (RDDS).
 * Does not support derived products.
 */
public class RemoteDhusDataStore implements DataStore, DerivedProductStore
{
   private static final Logger LOGGER = LogManager.getLogger();

   // datastore management fields
   private final String name;
   private final int priority;
   private final DataStoreRestriction restriction;

   // functional field
   private final String serviceUrl;
   private final String login;
   private final String password;

   // Timed task to periodically check if the remote is up.
   // Requests to the remote will not be attempted if the remote is down in order to save resources.
   private final Timer timer;

   // OData Client (may be null)
   private Optional<ODataClient> optODataClient;

   /**
    * Creates a new instance of the RDDS.
    *
    * @param name of this DataStore instance
    * @param serviceUrl of the remote DHuS backing this RDDS
    * @param login credentials on the remove DHuS instance
    * @param password credentials on the remove DHuS instance
    * @param priority of this DataStore instance
    * @param aliveInterval how often the task that checks if the remote DHuS is alive is run
    * @param restriction access restrictions of this DataStore
    */
   public RemoteDhusDataStore(String name, String serviceUrl, String login, String password, int priority,
         long aliveInterval, DataStoreRestriction restriction)
   {
      this.name = name;
      this.serviceUrl = serviceUrl;
      this.login = login;
      this.password = password;
      this.priority = priority;
      this.optODataClient = Optional.ofNullable(null);
      this.restriction = restriction;

      // schedule timer that will check if remote is down
      this.timer = new Timer("RDDS Is Alive", true);
      this.timer.schedule(new AliveTask(), 0, aliveInterval);
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

   private final DataStoreException remoteNotUpExSupplier()
   {
      return new DataStoreException("Cannot access product on remote DHuS instance: " + getName() + " is down");
   }

   @Override
   public Product get(String uuid) throws DataStoreException
   {
      ODataClient odataClient = optODataClient.orElseThrow(this::remoteNotUpExSupplier);

      try
      {
         // may throw ProductNotFoundException, but means the remote is still alive
         return RemoteDhusProduct.initialize(serviceUrl, uuid, login, password, odataClient);
      }
      catch (IOException | InterruptedException e)
      {
         odataClient = null;
         LOGGER.error("A problem occured while retrieving a distant DHuS product", e);
         throw new DataStoreException("Cannot access product on remote DHuS instance");
      }
   }

   @Override
   public DataStoreProduct getDerivedProduct(String uuid, String tag) throws StoreException
   {
      ODataClient odataClient = optODataClient.orElseThrow(this::remoteNotUpExSupplier);

      try
      {
         // may throw ProductNotFoundException, but means the remote is still alive
         return RemoteDhusProduct.initializeDerived(serviceUrl, uuid, tag, login, password, odataClient);
      }
      catch (IOException | InterruptedException e)
      {
         odataClient = null;
         LOGGER.error("A problem occured while retrieving a distant DHuS product", e);
         throw new DataStoreException("Cannot access product on remote DHuS instance");
      }
   }

   @Override
   public boolean hasProduct(String uuid)
   {
      if (!optODataClient.isPresent())
      {
         LOGGER.error("Cannot access product on remote DHuS instance: {} is down", getName());
         return false;
      }
      ODataClient odataClient = optODataClient.get();

      try
      {
         RemoteDhusProduct product = RemoteDhusProduct.initialize(serviceUrl, uuid, login, password, odataClient);
         LOGGER.debug("{} has product {}", name, product.getUuid());
         product.close();
         return true;
      }
      catch (ProductNotFoundException e)
      {
         return false;
      }
      catch (IOException | InterruptedException e)
      {
         odataClient = null;
         LOGGER.error("A problem occured while checking a distant DHuS product", e);
         return false;
      }
   }

   @Override
   public boolean hasDerivedProduct(String uuid, String tag)
   {
      if (!optODataClient.isPresent())
      {
         LOGGER.error("Cannot access product on remote DHuS instance: {} is down", getName());
         return false;
      }
      ODataClient odataClient = optODataClient.get();

      try
      {
         RemoteDhusProduct product = RemoteDhusProduct.initializeDerived(serviceUrl, uuid, tag, login, password, odataClient);
         LOGGER.debug("{} has product {}", name, product.getUuid());
         product.close();
         return true;
      }
      catch (ProductNotFoundException e)
      {
         return false;
      }
      catch (IOException | InterruptedException e)
      {
         odataClient = null;
         LOGGER.debug("A problem occured while checking a distant DHuS product", e);
         return false;
      }
   }

   @Override
   public boolean addProductReference(String uuid, Product product) throws DataStoreException
   {
      if (!canModifyReferences())
      {
         throw new ReadOnlyDataStoreException(getName() + " datastore is not allowed to modify references");
      }
      return hasProduct(uuid);
   }

   @Override
   public boolean addDerivedProductReference(String uuid, String tag, Product product)
         throws StoreException
   {
      if (!canModifyReferences())
      {
         throw new ReadOnlyDataStoreException(getName() + " datastore is not allowed to modify references");
      }
      return hasDerivedProduct(uuid, tag);
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
   public void set(String uuid, Product product) throws DataStoreException
   {
      throw new ReadOnlyDataStoreException("RemoteDHuSDataStore is read-only");
   }

   @Override
   public void addProduct(IngestibleProduct inProduct) throws StoreException
   {
      throw new ReadOnlyDataStoreException("RemoteDHuSDataStore is read-only");
   }

   @Override
   public void addDerivedProduct(String uuid, String tag, Product product) throws StoreException
   {
      throw new ReadOnlyDataStoreException("RemoteDHuSDataStore is read-only");
   }

   @Override
   public void deleteProduct(String uuid) throws DataStoreException
   {
      throw new ReadOnlyDataStoreException("RemoteDHuSDataStore is read-only");
   }

   @Override
   public void deleteDerivedProduct(String uuid, String tag) throws StoreException
   {
      throw new ReadOnlyDataStoreException("RemoteDHuSDataStore is read-only");
   }

   @Override
   public void deleteDerivedProducts(String uuid) throws StoreException
   {
      throw new ReadOnlyDataStoreException("RemoteDHuSDataStore is read-only");
   }

   @Override
   public boolean canHandleDerivedProducts()
   {
      return true;
   }

   @Override
   public boolean canModifyReferences()
   {
      return restriction == DataStoreRestriction.NONE || restriction == DataStoreRestriction.REFERENCES_ONLY;
   }

   @Override
   public boolean hasKeyStore()
   {
      return false;
   }

   @Override
   public void close() throws Exception
   {
      try
      {
         timer.cancel();
      }
      catch(RuntimeException suppressed) {}
   }

   private class AliveTask extends TimerTask
   {
      // params to use when checking the remote is alive
      private final Map<String, String> aliveParams;

      public AliveTask()
      {
         super();
         // $top=1
         aliveParams = Collections.<String, String>singletonMap("$top", String.valueOf(1));
      }

      @Override
      public void run()
      {
         LOGGER.debug("Checking health of {}...", name);

         // remote bas been down
         if (!optODataClient.isPresent())
         {
            LOGGER.debug("ODataClient unavailable in {}", name);
            try
            {
               // try to initialize ODataClient again
               optODataClient = Optional.of(new ODataClient(serviceUrl, login, password));
               LOGGER.debug("ODataClient successfully restored in {}", name);
            }
            catch (URISyntaxException | IOException | ODataException | RuntimeException e)
            {
               LOGGER.error("Could not restore ODataClient in {}", name, e);
            }
         }
         // remote has been up
         else
         {
            try
            {
               // check that remote is still alive with simple request
               ODataFeed feed = optODataClient.get().readFeed("/Products", aliveParams);
               LOGGER.debug("ODataClient of {} is alive, responded {} product(s)", name, feed.getEntries().size());
            }
            catch (IOException | ODataException | InterruptedException | RuntimeException e)
            {
               // rip
               optODataClient = Optional.ofNullable(null);
               LOGGER.error("ODataClient of {} is not responding", name, e);
            }
         }
      }
   }
}
