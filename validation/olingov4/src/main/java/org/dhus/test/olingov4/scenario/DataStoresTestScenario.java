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
package org.dhus.test.olingov4.scenario;

import java.io.IOException;
import java.util.Collections;

import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.domain.ClientEntitySetIterator;
import org.dhus.test.olingov4.EntityFactory;
import org.dhus.test.olingov4.ODataOperatorV4;
import org.dhus.test.olingov4.OlingoTestException;
import org.dhus.test.olingov4.TestManagerV4;
import org.dhus.test.olingov4.Utils;

/**
 *
 */
public class DataStoresTestScenario implements TestScenarioV4
{
   // entity set
   public static final String ENTITY_SET_NAME = "DataStores";
   
   // entity sub-types
   public static final String HFS_DATASTORE_ENTITY_TYPE = "HFSDataStore";
   public static final String OPENSTACK_DATASTORE_ENTITY_TYPE = "OpenStackDataStore";
   public static final String GMP_DATASTORE_ENTITY_TYPE = "GMPDataStore";
   
   // base properties
   public static final String PROPERTY_NAME = "Name";
   public static final String PROPERTY_READONLY = "ReadOnly";
   public static final String PROPERTY_PRIORITY = "Priority";
   public static final String PROPERTY_MAXIMUMSIZE = "MaximumSize";
   public static final String PROPERTY_CURRENTSIZE = "CurrentSize";
   public static final String PROPERTY_AUTOEVICTION = "AutoEviction";
   
   // HFS properties
   public static final String PROPERTY_PATH = "Path";
   public static final String PROPERTY_MAXFILEDEPTH = "MaxFileDepth";
   
   // OpenStack properties
   public static final String PROPERTY_PROVIDER = "Provider";
   public static final String PROPERTY_IDENTITY = "Identity";
   public static final String PROPERTY_CREDENTIAL = "Credential";
   public static final String PROPERTY_URL = "Url";
   public static final String PROPERTY_REGION = "Region";
   public static final String PROPERTY_CONTAINER = "Container";
   
   // GMP properties
   public static final String PROPERTY_GMPREPOLOCATION = "GMPRepoLocation";
   public static final String PROPERTY_HFSLOCATION = "HFSLocation";
   public static final String PROPERTY_MAXQEUEDREQUEST = "MaxQueuedRequest";
   public static final String PROPERTY_ISMASTER = "IsMaster";
   public static final String PROPERTY_MYSQLCONNECTIONINFO = "MySQLConnectionInfo";
   
   // MySQLConnectionInfo complex type
   public static final String MYSQLCONNECTIONINFO_COMPLEX_TYPE = "MySQLConnectionInfo";
   public static final String MYSQLCONNECTIONINFO_DATABASEURL = "DatabaseUrl";
   public static final String MYSQLCONNECTIONINFO_USER = "User";
   public static final String MYSQLCONNECTIONINFO_PASSWORD = "Password";

   private static final String HFSDATASTORE_NAME = "myHFSDataStore";
   private static final String OPENSTACKDATASTORE_NAME = "myOpenstackDataStore";
   
   private final EntityFactory entityFactory;
   private final ODataOperatorV4 odDataOperatorV4;

   public DataStoresTestScenario(EntityFactory entityFactory,
         ODataOperatorV4 odDataOperatorV4)
   {
      this.entityFactory = entityFactory;
      this.odDataOperatorV4 = odDataOperatorV4;
   }

   public void execute() throws OlingoTestException, IOException
   {
      // HFS
      TestManagerV4.logScenarioInfo("");
      TestManagerV4.logScenarioInfo("Testing HFSDataStore operations...");
      
      ClientEntity hfsDataStoreToCreate = entityFactory.makeHFSDataStore(
            HFSDATASTORE_NAME, false, 0, 100L, 0, false, "/my/path", 10);
      
      ClientEntity hfsDataStoreToUpdate = entityFactory.makeHFSDataStore(
            HFSDATASTORE_NAME, false, 50, 200000, 0, false, "/myyy/path", 10);
      
      internalExecute(HFSDATASTORE_NAME, hfsDataStoreToCreate, hfsDataStoreToUpdate);      
      
      // OpenStack
      TestManagerV4.logScenarioInfo("");
      TestManagerV4.logScenarioInfo("Testing OpenstackDataStore operations...");
      
      ClientEntity openstackDataStoreToCreate = entityFactory.makeOpenStackDataStore(
            OPENSTACKDATASTORE_NAME, false, 10, 12, 0, false, "providerProperty",
            "Identity", "credential", "my/url", "region", "container");
      
      ClientEntity openstackDataStoreToUpdate = entityFactory.makeOpenStackDataStore(
            OPENSTACKDATASTORE_NAME, true, 10, 10, 10, true, "providerUpdated",
            "identityUpdated", "credentialUpdated", "urlUpdated", "regionUpdated", "containerUpdated");
      
      internalExecute(OPENSTACKDATASTORE_NAME, openstackDataStoreToCreate, openstackDataStoreToUpdate);
      
      //TODO find a way to have a GMP mockup to enable GMPDataStore creation
   }
   
   private void internalExecute(String dataStoreName, ClientEntity dataStoreToCreate, 
         ClientEntity dataStoreToUpdate) throws OlingoTestException, IOException
   {
      // create datastore
      odDataOperatorV4.createEntity(ENTITY_SET_NAME, dataStoreToCreate);
      
      // read created datastore
      ClientEntity createdDataStore = odDataOperatorV4.readEntry(ENTITY_SET_NAME, dataStoreName);
      if (createdDataStore == null)
      {
         throw new OlingoTestException("Created DataStore not found");
      }
      
      // compare datastores
      if(!Utils.compareEntitiesExcept(dataStoreToCreate, createdDataStore, 
            Collections.singleton(PROPERTY_CURRENTSIZE)))
      {
         throw new OlingoTestException("Invalid created DataStore");
      }
      
      // read collection
      ClientEntitySetIterator<ClientEntitySet, ClientEntity> dataStoreFeed =
            odDataOperatorV4.readEntities(ENTITY_SET_NAME);
      if (dataStoreFeed == null)
      {
         throw new OlingoTestException("Cannot read DataStores entity collection");
      }
         
      // update datastore
      odDataOperatorV4.updateEntry(ENTITY_SET_NAME, dataStoreName, dataStoreToUpdate);
      
      ClientEntity updatedDataStore = odDataOperatorV4.readEntry(ENTITY_SET_NAME, dataStoreName);
      
      // compare datastores
      if(!Utils.compareEntitiesExcept(dataStoreToUpdate, updatedDataStore,
            Collections.singleton(PROPERTY_CURRENTSIZE)))
      {
         throw new OlingoTestException("Invalid updated DataStore");
      }
      
      // delete
      odDataOperatorV4.deleteEntry(ENTITY_SET_NAME, dataStoreName);
      
      // read collection to check deletion
      ClientEntitySetIterator<ClientEntitySet, ClientEntity> dataStoreEntities =
         odDataOperatorV4.readEntities(ENTITY_SET_NAME);
      while (dataStoreEntities.hasNext())
      {
         String name = (String) dataStoreEntities.next().getProperty(PROPERTY_NAME)
               .getPrimitiveValue().toValue();
         if (name.equals(dataStoreName))
         {
            throw new OlingoTestException("Failed to delete datastore entity");
         }
      }
   }
}
