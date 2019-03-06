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
package org.dhus.test.olingov4;

import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientObjectFactory;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.dhus.test.olingov4.scenario.DataStoresTestScenario;

public class EntityFactory
{
   public static final String NAME_SPACE = "OData.DHuS";

   private final ClientObjectFactory factory;
   
   public EntityFactory(ClientObjectFactory factory)
   {
      this.factory = factory;
   }

   public ClientEntity makeHFSDataStore(String name, boolean readOnly,
         int priority, long maximumSize, long currentSize, boolean autoEviction,
         String path, int maxFileDepth)
   {
      ClientEntity hfsDataStoreEntity = prepareDataStore(
            DataStoresTestScenario.HFS_DATASTORE_ENTITY_TYPE, 
            name, 
            readOnly,
            priority, 
            maximumSize, 
            currentSize, 
            autoEviction);

      hfsDataStoreEntity.getProperties().add(
            factory.newPrimitiveProperty(
                  DataStoresTestScenario.PROPERTY_PATH, 
                  factory.newPrimitiveValueBuilder().buildString(path)));

      hfsDataStoreEntity.getProperties().add(
            factory.newPrimitiveProperty(
                  DataStoresTestScenario.PROPERTY_MAXFILEDEPTH,
                  factory.newPrimitiveValueBuilder().buildInt32(maxFileDepth)));

      return hfsDataStoreEntity;
   }

   public ClientEntity makeOpenStackDataStore(String name, boolean readOnly,
         int priority, long maximumSize, long currentSize, boolean autoEviction,
         String provider, String identity, String credential, String url,
         String region, String container)
   {
      ClientEntity openStackDataStoreEntity = prepareDataStore(
            DataStoresTestScenario.OPENSTACK_DATASTORE_ENTITY_TYPE, 
            name, 
            readOnly,
            priority, 
            maximumSize, 
            currentSize, 
            autoEviction);

      openStackDataStoreEntity.getProperties().add(factory.newPrimitiveProperty(
            DataStoresTestScenario.PROPERTY_PROVIDER, 
            factory.newPrimitiveValueBuilder().buildString(provider)));

      openStackDataStoreEntity.getProperties().add(factory.newPrimitiveProperty(
            DataStoresTestScenario.PROPERTY_IDENTITY, 
            factory.newPrimitiveValueBuilder().buildString(identity)));

      openStackDataStoreEntity.getProperties().add(factory.newPrimitiveProperty(
            DataStoresTestScenario.PROPERTY_CREDENTIAL,
            factory.newPrimitiveValueBuilder().buildString(credential)));

      openStackDataStoreEntity.getProperties().add(factory.newPrimitiveProperty(
            DataStoresTestScenario.PROPERTY_URL, 
            factory.newPrimitiveValueBuilder().buildString(url)));

      openStackDataStoreEntity.getProperties().add(factory.newPrimitiveProperty(
            DataStoresTestScenario.PROPERTY_REGION, 
            factory.newPrimitiveValueBuilder().buildString(region)));

      openStackDataStoreEntity.getProperties().add(factory.newPrimitiveProperty(
            DataStoresTestScenario.PROPERTY_CONTAINER,
            factory.newPrimitiveValueBuilder().buildString(container)));

      return openStackDataStoreEntity;

   }

   public ClientEntity makeGMPDataStore(String name, boolean readOnly,
         int priority, long maximumSize, long currentSize, boolean autoEviction,
         String gmpPepoLocation, String hfsLocation, String maxQueuedRequest,
         Boolean isMaster, String databaseUrl, String user, String password )
   {
      ClientEntity gmpDataStoreEntity = prepareDataStore(
            DataStoresTestScenario.GMP_DATASTORE_ENTITY_TYPE, 
            name, 
            readOnly,
            priority, 
            maximumSize, 
            currentSize, 
            autoEviction);

      gmpDataStoreEntity.getProperties().add(factory.newPrimitiveProperty(
            DataStoresTestScenario.PROPERTY_GMPREPOLOCATION,
            factory.newPrimitiveValueBuilder().buildString(gmpPepoLocation)));

      gmpDataStoreEntity.getProperties().add(factory.newPrimitiveProperty(
            DataStoresTestScenario.PROPERTY_HFSLOCATION,
            factory.newPrimitiveValueBuilder().buildString(hfsLocation)));

      gmpDataStoreEntity.getProperties().add(factory.newPrimitiveProperty(
            DataStoresTestScenario.PROPERTY_MAXQEUEDREQUEST,
            factory.newPrimitiveValueBuilder().buildString(maxQueuedRequest)));

      gmpDataStoreEntity.getProperties().add(factory.newPrimitiveProperty(
            DataStoresTestScenario.PROPERTY_ISMASTER,
            factory.newPrimitiveValueBuilder().buildBoolean(isMaster)));

      gmpDataStoreEntity.getProperties().add(factory.newComplexProperty(
            DataStoresTestScenario.PROPERTY_MYSQLCONNECTIONINFO,
            factory.newComplexValue(DataStoresTestScenario.MYSQLCONNECTIONINFO_COMPLEX_TYPE)
                  .add(factory.newPrimitiveProperty(
                        DataStoresTestScenario.MYSQLCONNECTIONINFO_DATABASEURL, 
                        factory.newPrimitiveValueBuilder().buildString(databaseUrl)))
                  .add(factory.newPrimitiveProperty(
                        DataStoresTestScenario.MYSQLCONNECTIONINFO_USER, 
                        factory.newPrimitiveValueBuilder().buildString(user)))
                  .add(factory.newPrimitiveProperty(
                        DataStoresTestScenario.MYSQLCONNECTIONINFO_PASSWORD, 
                        factory.newPrimitiveValueBuilder().buildString(password)))));
      
      return gmpDataStoreEntity;
   }

   private ClientEntity prepareDataStore(String dataStoreType, String name, boolean readOnly,
         int priority, long maximumSize, long currentSize,
         boolean autoEviction)
   {
      ClientEntity dataStoreEntity =
         factory.newEntity(new FullQualifiedName(NAME_SPACE, dataStoreType));

      dataStoreEntity.getProperties().add(factory.newPrimitiveProperty(
            DataStoresTestScenario.PROPERTY_NAME,
            factory.newPrimitiveValueBuilder().buildString(name)));

      dataStoreEntity.getProperties().add(factory.newPrimitiveProperty(
            DataStoresTestScenario.PROPERTY_READONLY,
            factory.newPrimitiveValueBuilder().buildBoolean(readOnly)));

      dataStoreEntity.getProperties().add(factory.newPrimitiveProperty(
            DataStoresTestScenario.PROPERTY_PRIORITY,
            factory.newPrimitiveValueBuilder().buildInt32(priority)));

      dataStoreEntity.getProperties().add(factory.newPrimitiveProperty(
            DataStoresTestScenario.PROPERTY_MAXIMUMSIZE,
            factory.newPrimitiveValueBuilder().buildInt64(maximumSize)));

      dataStoreEntity.getProperties().add(factory.newPrimitiveProperty(
            DataStoresTestScenario.PROPERTY_CURRENTSIZE,
            factory.newPrimitiveValueBuilder().buildInt64(currentSize)));

      dataStoreEntity.getProperties().add(factory.newPrimitiveProperty(
            DataStoresTestScenario.PROPERTY_AUTOEVICTION,
            factory.newPrimitiveValueBuilder().buildBoolean(autoEviction)));

      return dataStoreEntity;
   }

}
