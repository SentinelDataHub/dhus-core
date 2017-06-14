/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016 GAEL Systems
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

/**
 * This interface is the service class to manage a set of data stores.
 * It is also a DataStore by convenience because is shall map all methods supported in DataStore
 * interface.
 */
public interface DataStoreService extends DataStore
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
    * @param datastore to remove.
    */
   public void remove(DataStore datastore);

   /**
    * Retrieve the list of DataStores managed by this service.
    *
    * @return
    */
   public List<DataStore> list();
}
