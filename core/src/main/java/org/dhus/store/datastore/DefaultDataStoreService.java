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
package org.dhus.store.datastore;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.Product;
import org.dhus.ProductConstants;
import org.dhus.store.datastore.config.DataStoreConf;

import org.springframework.transaction.annotation.Transactional;

/**
 * This class implements the default DataStore container that is constructed according to aggregated DataStores.
 * The execution of this DataStore commands will automatically be propagated to the handled DataStores.
 */
public class DefaultDataStoreService implements DataStoreService
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final String DATA_STORE_NAME = "DefaultDataStoreService";

   /** Default constructor needed for Spring to construct this bean. */
   @SuppressWarnings("unused")
   private DefaultDataStoreService() {}

   private List<DataStore> datastores = new ArrayList<>();

   /**
    * Construct a DataStore from a list of DataStoreConf.
    *
    * @param confs
    */
   public DefaultDataStoreService(List<DataStoreConf> confs)
   {
      for (DataStoreConf conf : confs)
      {
         this.datastores.add(DataStoreFactory.createDataStore(conf));
      }
   }

   @Override
   public String getName()
   {
      return DATA_STORE_NAME;
   }

   @Override
   public Product get(String id) throws DataStoreException
   {
      Product product = null;
      Iterator<DataStore> iterator = datastores.iterator();

      while (product == null && iterator.hasNext())
      {
         try
         {
            product = iterator.next().get(id);
         }
         catch (DataStoreException e)
         {
            continue;
         }
      }

      if (product == null)
      {
         throw new DataStoreException("Cannot retrieve product for id " + id);
      }
      return product;
   }

   // This method MUST be transactional: in case of failure all the DataStores that have been set
   // in this context must be rolled back.
   @Transactional
   @Override
   public void set(String id, Product product) throws DataStoreException
   {
      List<Throwable> throwables = new ArrayList<>();
      for (DataStore datastore:datastores)
      {
         try
         {
            long duration = System.currentTimeMillis();
            datastore.set(id, product);
            duration = System.currentTimeMillis() - duration;
            LOGGER.info("Product [{}:{}] stored in {} datastore in {}ms",
                  product.getName(), product.getProperty(ProductConstants.DATA_SIZE),
                  datastore.getName(), duration);
         }
         catch (ReadOnlyDataStoreException e)
         {
            continue;
         }
         catch (Exception e)
         {
            throwables.add(e);
         }
      }
      throwErrors(throwables, id);
   }

   // This method MUST be transactional: in case of failure all the DataStores that have been set
   // in this context must be rolled back.
   @Transactional
   @Override
   public void move(String id, Product product) throws DataStoreException
   {
      if (datastores.isEmpty())
      {
         return;
      }

      DataStore dataStore = datastores.get(0);
      try
      {
         dataStore.move(id, product);
      }
      catch (Exception e)
      {
         throw new DataStoreException("Cannot move product " + id, e);
      }

      if (datastores.size() > 1)
      {
         List<Throwable> throwables = new ArrayList<>();
         Product movedProduct = dataStore.get(id);
         for (int i = 1; i < datastores.size(); i++)
         {
            try
            {
               datastores.get(i).set(id, movedProduct);
            }
            catch (ReadOnlyDataStoreException e)
            {
               continue;
            }
            catch (Exception e)
            {
               throwables.add(e);
            }
         }
         throwErrors(throwables, id);
      }
   }

   @Override
   public void delete(String id) throws DataStoreException
   {
      List<Throwable>throwables = new ArrayList<>();
      for (DataStore datastore: datastores)
      {
         try
         {
            if (datastore.exists(id))
            {
               datastore.delete(id);
            }
         }
         catch (ReadOnlyDataStoreException e)
         {
            continue;
         }
         catch (Exception e)
         {
            throwables.add(e);
         }
      }
      throwErrors(throwables, id);
   }

   @Override
   public boolean exists(String id)
   {
      boolean found = false;
      Iterator<DataStore> iterator = datastores.iterator();
      while (!found && iterator.hasNext())
      {
         found = iterator.next().exists(id);
      }
      return found;
   }

   @Override
   public void add(DataStore datastore)
   {
      if (datastores.contains(datastore))
      {
         LOGGER.warn("DataStore already present in the service");
      }
      else
      {
         datastores.add(datastore);
      }
   }

   @Override
   public void remove(DataStore datastore)
   {
      datastores.remove(datastore);
   }

   @Override
   public List<DataStore> list()
   {
      return datastores;
   }

   public List<DataStore> getDatastores()
   {
      return datastores;
   }

   public void setDatastores(List<DataStore> datastores)
   {
      this.datastores = datastores;
   }

   private void throwErrors (List<Throwable> throwableList, String id) throws DataStoreException
   {
      if (throwableList.isEmpty())
      {
         return;
      }

      if (throwableList.size() == 1)
      {
         throw new DataStoreException("Error while removing id: " + id, throwableList.get (0));
      }
      else
      {
         StringBuilder sb = new StringBuilder("Errors while removing id: ");
         sb.append(id).append(" errors:");
         for (Throwable throwable: throwableList)
         {
            sb.append('[').append(throwable.getMessage()).append(']');
         }

         throw new DataStoreException(sb.toString());
      }
   }

   @Override
   public boolean addProductReference(String id, Product product) throws DataStoreException
   {
      List<Throwable> throwable_list = new ArrayList<>();
      for (DataStore dataStore: datastores)
      {
         try
         {
            if (dataStore.addProductReference(id, product))
            {
               return true;
            }
         }
         catch (ReadOnlyDataStoreException e)
         {
            continue;
         }
         catch (Exception e)
         {
            throwable_list.add(e);
         }
      }
      throwErrors(throwable_list, id);
      return false;
   }

   @Override
   public boolean canAccess(String resource_location)
   {
      for (DataStore dataStore: datastores)
      {
         if (dataStore.canAccess(resource_location))
         {
            return true;
         }
      }
      return false;
   }
}
