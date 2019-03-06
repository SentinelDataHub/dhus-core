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
package org.dhus.store.datastore;

import java.util.List;

import fr.gael.dhus.datastore.Destination;

/**
 * This interface is the service class to manage a set of data stores.
 * It is also a DataStore by convenience because is shall map all methods supported in DataStore
 * interface.
 */
public interface DataStoreManager extends DataStore
{
   /**
    * Add a new DataStore managed by this service.
    *
    * @param datastore to add
    */
   public void add(DataStore datastore);

   /**
    * Remove a DataStore from the managed list.
    *
    * @param datastoreName name of DataStore to remove
    * @return the removed instance or {@code null}
    */
   public DataStore remove(String datastoreName);

   /**
    * Retrieve the list of DataStores managed by this service.
    *
    * @return
    */
   public List<DataStore> list();

   /**
    * Gets a DataStore by its name, {@code null} if not found.
    *
    * @param name of the DataStore
    * @return the instance or {@code null}
    */
   public default DataStore getDataStoreByName(String name)
   {
      return list().stream().filter(dataStore -> dataStore.getName().equals(name)).findFirst().orElse(null);
   }

   /**
    * Deletes a product from all the DataStores managed by this service.
    *
    * @param uuid the UUID of the product to delete
    * @param destination the destination of the product's data
    * @throws DataStoreException
    */
   // TODO remove destination from parameters and handle it in DataStoreManager implementation
   public void deleteProduct(String uuid, Destination destination) throws DataStoreException;

   /**
    * Delete the product from the DataStore
    *
    * @param uuid          the UUID of the product to delete
    * @param dataStoreName the name of the DataStore where the product will be deleted
    * @param destination   the destination of the product's data
    * @param safeMode      if `true`, given product will be deleted from named DataStore only if it is stored in another DataStore
    * @throws DataStoreException
    */
   public void deleteProductFromDataStore(String uuid, String dataStoreName, Destination destination, Boolean safeMode)
         throws DataStoreException;
}
