/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2018 GAEL Systems
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
package org.dhus.olingo.v2.data;

import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.service.DataStoreService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.odata.engine.data.DataHandler;
import fr.gael.odata.engine.data.DataHandlerUtil;

import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;

import org.dhus.olingo.v2.ODataSecurityManager;
import org.dhus.olingo.v2.datamodel.DataStoreModel;
import org.dhus.olingo.v2.datamodel.RemoteDhusDataStoreModel;
import org.dhus.olingo.v2.datamodel.EvictionModel;
import org.dhus.olingo.v2.datamodel.GmpDataStoreModel;
import org.dhus.olingo.v2.datamodel.HfsDataStoreModel;
import org.dhus.olingo.v2.datamodel.OpenstackDataStoreModel;
import org.dhus.olingo.v2.datamodel.complex.GMPConfigurationComplexType;
import org.dhus.olingo.v2.datamodel.complex.GMPQuotasComplexType;
import org.dhus.olingo.v2.datamodel.complex.MySQLConnectionInfoComplexType;

import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreFactory;
import org.dhus.store.datastore.DataStoreFactory.InvalidConfigurationException;
import org.dhus.store.datastore.DataStoreManager;
import org.dhus.store.datastore.config.DataStoreManager.UnavailableNameException;
import org.dhus.store.datastore.config.RemoteDhusDataStoreConf;
import org.dhus.store.datastore.config.GmpDataStoreConf;
import org.dhus.store.datastore.config.GmpDataStoreConf.Configuration;
import org.dhus.store.datastore.config.GmpDataStoreConf.MysqlConnectionInfo;
import org.dhus.store.datastore.config.HfsDataStoreConf;
import org.dhus.store.datastore.config.NamedDataStoreConf;
import org.dhus.store.datastore.config.OpenStackDataStoreConf;

/**
 * Provides data for DataStore entities.
 */
public class DataStoreDataHandler implements DataHandler
{
   private static final DataStoreManager DS_MANAGER =
         ApplicationContextProvider.getBean(DataStoreManager.class);

   private static final DataStoreService DS_SERVICE =
         ApplicationContextProvider.getBean(DataStoreService.class);

   private Entity toOlingoEntity(NamedDataStoreConf dataStore)
   {
      if (dataStore == null)
      {
         return null;
      }

      Entity dataStoreEntity = new Entity()
            .addProperty(new Property(
                  null,
                  DataStoreModel.PROPERTY_NAME,
                  ValueType.PRIMITIVE,
                  dataStore.getName()))
            .addProperty(new Property(
                  null,
                  DataStoreModel.PROPERTY_READONLY,
                  ValueType.PRIMITIVE,
                  dataStore.isReadOnly()))
            .addProperty(new Property(
                  null,
                  DataStoreModel.PROPERTY_PRIORITY,
                  ValueType.PRIMITIVE,
                  dataStore.getPriority()))
            .addProperty(new Property(
                  null,
                  DataStoreModel.PROPERTY_MAXIMUMSIZE,
                  ValueType.PRIMITIVE,
                  dataStore.getMaximumSize()))
            .addProperty(new Property(
                  null,
                  DataStoreModel.PROPERTY_CURRENTSIZE,
                  ValueType.PRIMITIVE,
                  dataStore.getCurrentSize()))
            .addProperty(new Property(
                  null,
                  DataStoreModel.PROPERTY_AUTOEVICTION,
                  ValueType.PRIMITIVE,
                  dataStore.isAutoEviction()));

      dataStoreEntity.setId(DataHandlerUtil.createEntityId(
            DataStoreModel.ABSTRACT_ENTITY_SET_NAME,
            dataStore.getName()));

      if (dataStore instanceof HfsDataStoreConf)
      {
         HfsDataStoreConf hfsDataStore = (HfsDataStoreConf) dataStore;

         dataStoreEntity.setType(HfsDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());

         dataStoreEntity
               .addProperty(new Property(
                     null,
                     HfsDataStoreModel.PROPERTY_PATH,
                     ValueType.PRIMITIVE,
                     hfsDataStore.getPath()))
               .addProperty(new Property(
                     null,
                     HfsDataStoreModel.PROPERTY_MAXFILEDEPTH,
                     ValueType.PRIMITIVE,
                     hfsDataStore.getMaxFileNo()))
               .addProperty(new Property(
                     null,
                     HfsDataStoreModel.PROPERTY_MAXITEMS,
                     ValueType.PRIMITIVE,
                     hfsDataStore.getMaxItems()));
      }

      if (dataStore instanceof OpenStackDataStoreConf)
      {
         OpenStackDataStoreConf openStackDataStore = (OpenStackDataStoreConf) dataStore;

         dataStoreEntity.setType(OpenstackDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());

         dataStoreEntity
               .addProperty(new Property(
                     null,
                     OpenstackDataStoreModel.PROPERTY_PROVIDER,
                     ValueType.PRIMITIVE,
                     openStackDataStore.getProvider()))
               .addProperty(new Property(
                     null,
                     OpenstackDataStoreModel.PROPERTY_IDENTITY,
                     ValueType.PRIMITIVE,
                     openStackDataStore.getIdentity()))
               .addProperty(new Property(
                     null,
                     OpenstackDataStoreModel.PROPERTY_CREDENTIAL,
                     ValueType.PRIMITIVE,
                     openStackDataStore.getCredential()))
               .addProperty(new Property(
                     null,
                     OpenstackDataStoreModel.PROPERTY_URL,
                     ValueType.PRIMITIVE,
                     openStackDataStore.getUrl()))
               .addProperty(new Property(
                     null,
                     OpenstackDataStoreModel.PROPERTY_REGION,
                     ValueType.PRIMITIVE,
                     openStackDataStore.getRegion()))
               .addProperty(new Property(
                     null,
                     OpenstackDataStoreModel.PROPERTY_CONTAINER,
                     ValueType.PRIMITIVE,
                     openStackDataStore.getContainer()));
      }

      if (dataStore instanceof GmpDataStoreConf)
      {
         GmpDataStoreConf gmpDataStore = (GmpDataStoreConf) dataStore;

         dataStoreEntity.setType(GmpDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());

         dataStoreEntity
               .addProperty(new Property(
                     null,
                     GmpDataStoreModel.PROPERTY_REPO_LOCATION,
                     ValueType.PRIMITIVE,
                     gmpDataStore.getGmpRepoLocation()))
               .addProperty(new Property(
                     null,
                     GmpDataStoreModel.PROPERTY_HFS_LOCATION,
                     ValueType.PRIMITIVE,
                     gmpDataStore.getHfsLocation()))
               .addProperty(new Property(
                     null,
                     GmpDataStoreModel.PROPERTY_MAX_QUEUED_REQUESTS,
                     ValueType.PRIMITIVE,
                     gmpDataStore.getMaxQueuedRequest()))
               .addProperty(new Property(
                     null,
                     GmpDataStoreModel.PROPERTY_IS_MASTER,
                     ValueType.PRIMITIVE,
                     gmpDataStore.isIsMaster()))
               .addProperty(makeMySQLConnectionInfoProperty(
                     gmpDataStore.getMysqlConnectionInfo()));

         if (gmpDataStore.getQuotas() != null)
         {
            dataStoreEntity.addProperty(makeQuotasProperty(gmpDataStore.getQuotas()));
         }

         if(gmpDataStore.getConfiguration() != null)
         {
            dataStoreEntity.addProperty(makeConfigurationProperty(gmpDataStore.getConfiguration()));
         }
      }

      if(dataStore instanceof RemoteDhusDataStoreConf)
      {
         RemoteDhusDataStoreConf dhusDataStore = (RemoteDhusDataStoreConf) dataStore;

         dataStoreEntity.setType(RemoteDhusDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());

         dataStoreEntity
            .addProperty(new Property(
                  null,
                  RemoteDhusDataStoreModel.PROPERTY_SERVICE_URL,
                  ValueType.PRIMITIVE,
                  dhusDataStore.getServiceUrl()))
            .addProperty(new Property(
                  null,
                  RemoteDhusDataStoreModel.PROPERTY_LOGIN,
                  ValueType.PRIMITIVE,
                  dhusDataStore.getLogin()))
            .addProperty(new Property(
                  null,
                  RemoteDhusDataStoreModel.PROPERTY_PASSWORD,
                  ValueType.PRIMITIVE,
                  "******"));
      }

      return dataStoreEntity;
   }

   private Property makeMySQLConnectionInfoProperty(MysqlConnectionInfo mysql)
   {
      ComplexValue complexValue = new ComplexValue();
      complexValue.getValue().add(new Property(
            null,
            MySQLConnectionInfoComplexType.PROPERTY_DATABASE_URL,
            ValueType.PRIMITIVE,
            mysql.getValue()));
      complexValue.getValue().add(new Property(
            null,
            MySQLConnectionInfoComplexType.PROPERTY_USER,
            ValueType.PRIMITIVE,
            mysql.getUser()));
      complexValue.getValue().add(new Property(
            null,
            MySQLConnectionInfoComplexType.PROPERTY_PASSWORD,
            ValueType.PRIMITIVE,
            mysql.getPassword()));

      return new Property(
            null,
            GmpDataStoreModel.PROPERTY_MYSQLCONNECTIONINFO,
            ValueType.COMPLEX,
            complexValue);
   }

   private Property makeQuotasProperty(GmpDataStoreConf.Quotas quotas)
   {
      ComplexValue complexValue = new ComplexValue();
      complexValue.getValue().add(new Property(
            null,
            GMPQuotasComplexType.PROPERTY_MAX_QPU,
            ValueType.PRIMITIVE,
            quotas.getMaxQueryPerUser()));

      complexValue.getValue().add(new Property(
            null,
            GMPQuotasComplexType.PROPERTY_TIMESPAN,
            ValueType.PRIMITIVE,
            quotas.getTimespan()));

      return new Property(
            null,
            GmpDataStoreModel.PROPERTY_QUOTAS,
            ValueType.COMPLEX,
            complexValue);
   }

   private Property makeConfigurationProperty(GmpDataStoreConf.Configuration configuration)
   {
      ComplexValue configurationValue = new ComplexValue();
      configurationValue.getValue().add(new Property(
            null,
            GMPConfigurationComplexType.PROPERTY_AGENTID,
            ValueType.PRIMITIVE,
            configuration.getAgentid()));

      configurationValue.getValue().add(new Property(
            null,
            GMPConfigurationComplexType.PROPERTY_TARGETID,
            ValueType.PRIMITIVE,
            configuration.getTargetid()));

      return new Property(
            null,
            GmpDataStoreModel.PROPERTY_CONFIGURATION,
            ValueType.COMPLEX,
            configurationValue);
   }

   /**
    * @param entity
    * @return MysqlConnectionInfo
    */
   private MysqlConnectionInfo extractMySQLConnectionInfo(Entity mysqlConnectInfoEntity, MysqlConnectionInfo mysqlConnectionInfo)
   {
      Property mySqlConnectionProperty = mysqlConnectInfoEntity.getProperty(GmpDataStoreModel.PROPERTY_MYSQLCONNECTIONINFO);
      if (mySqlConnectionProperty != null && mySqlConnectionProperty.getValue() != null)
      {
         List<Property> mysqlProperties =
               ((ComplexValue) mySqlConnectionProperty.getValue()).getValue();
         for (Property property: mysqlProperties)
         {
            switch (property.getName())
            {
               case MySQLConnectionInfoComplexType.PROPERTY_DATABASE_URL:
                  mysqlConnectionInfo.setValue((String) property.getValue());
                  break;

               case MySQLConnectionInfoComplexType.PROPERTY_USER:
                  mysqlConnectionInfo.setUser((String) property.getValue());
                  break;

               case MySQLConnectionInfoComplexType.PROPERTY_PASSWORD:
                  mysqlConnectionInfo.setPassword((String) property.getValue());
                  break;
            }
         }
      }
      return mysqlConnectionInfo;
   }

   private GmpDataStoreConf.Quotas extractQuotas(Entity quotaEntity,  GmpDataStoreConf.Quotas quotas )
   {
      Property quotasProperty = quotaEntity.getProperty(GmpDataStoreModel.PROPERTY_QUOTAS);
      if (quotasProperty != null && quotasProperty.getValue() != null)
      {
         for (Property prop: ComplexValue.class.cast(quotasProperty.getValue()).getValue())
         {
            switch(prop.getName())
            {
               case GMPQuotasComplexType.PROPERTY_MAX_QPU:
                  quotas.setMaxQueryPerUser(Integer.class.cast(prop.getValue()));
                  break;
               case GMPQuotasComplexType.PROPERTY_TIMESPAN:
                  quotas.setTimespan(Long.class.cast(prop.getValue()));
                  break;
            }
         }
         return quotas;
      }
      return null;
   }

   private GmpDataStoreConf.Configuration extractConfiguration(Entity entity, Configuration configuration)
   {
      Property configurationProperty = entity.getProperty(GmpDataStoreModel.PROPERTY_CONFIGURATION);
      if(configurationProperty != null && configurationProperty.getValue() != null)
      {
         for(Property property : ((ComplexValue) configurationProperty.getValue()).getValue())
         {
            switch(property.getName())
            {
               case GMPConfigurationComplexType.PROPERTY_AGENTID:
                  configuration.setAgentid((String) property.getValue());
                  break;

               case GMPConfigurationComplexType.PROPERTY_TARGETID:
                  configuration.setTargetid((String) property.getValue());
                  break;
            }
         }
      }
      return configuration;
   }

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      // TODO remove getEntityCollectionData and only use Filter/Order version
      List<NamedDataStoreConf> dataStores = DS_SERVICE.getNamedDataStoreConfigurations();
      EntityCollection entityCollection = new EntityCollection();
      for (NamedDataStoreConf dataStore: dataStores)
      {
         entityCollection.getEntities().add(toOlingoEntity(dataStore));
      }
      return entityCollection;
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters)
         throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      for (UriParameter keyParameter: keyParameters)
      {
         if (keyParameter.getName().equals(DataStoreModel.PROPERTY_NAME))
         {
            String dataStoreName = DataHandlerUtil.trimStringKeyParameter(keyParameter);
            return toOlingoEntity(DS_SERVICE.getNamedDataStore(dataStoreName));
         }
      }
      return null;
   }

   @Override
   public Entity createEntityData(Entity entity) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      NamedDataStoreConf dataStore = null;

      if (entity.getType().equals(HfsDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         HfsDataStoreConf hfsDataStore = new HfsDataStoreConf();
         hfsDataStore.setPath((String) DataHandlerUtil.getPropertyValue(entity, HfsDataStoreModel.PROPERTY_PATH));

         // if MaxFileDepth is null, set to 10
         Object maxFileDepth = DataHandlerUtil.getPropertyValue(entity, HfsDataStoreModel.PROPERTY_MAXFILEDEPTH);
         if (maxFileDepth == null)
         {
            hfsDataStore.setMaxFileNo(10);
         }
         else
         {
            hfsDataStore.setMaxFileNo((Integer) maxFileDepth);
         }

         Object maxItems = DataHandlerUtil.getPropertyValue(entity, HfsDataStoreModel.PROPERTY_MAXITEMS);
         if (maxItems == null)
         {
            hfsDataStore.setMaxItems(1024);
         }
         else
         {
            hfsDataStore.setMaxItems((Integer) maxItems);
         }

         dataStore = hfsDataStore;
      }
      else if (entity.getType().equals(OpenstackDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         OpenStackDataStoreConf openstackDataStore = new OpenStackDataStoreConf();
         openstackDataStore.setProvider((String) DataHandlerUtil.getPropertyValue(entity, OpenstackDataStoreModel.PROPERTY_PROVIDER));
         openstackDataStore.setIdentity((String) DataHandlerUtil.getPropertyValue(entity, OpenstackDataStoreModel.PROPERTY_IDENTITY));
         openstackDataStore.setCredential((String) DataHandlerUtil.getPropertyValue(entity, OpenstackDataStoreModel.PROPERTY_CREDENTIAL));
         openstackDataStore.setUrl((String) DataHandlerUtil.getPropertyValue(entity, OpenstackDataStoreModel.PROPERTY_URL));
         openstackDataStore.setContainer((String) DataHandlerUtil.getPropertyValue(entity, OpenstackDataStoreModel.PROPERTY_CONTAINER));
         openstackDataStore.setRegion((String) DataHandlerUtil.getPropertyValue(entity, OpenstackDataStoreModel.PROPERTY_REGION));

         dataStore = openstackDataStore;
      }
      else if (entity.getType().equals(GmpDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         GmpDataStoreConf gmpDataStore = new GmpDataStoreConf();
         gmpDataStore.setGmpRepoLocation((String) DataHandlerUtil.getPropertyValue(entity, GmpDataStoreModel.PROPERTY_REPO_LOCATION));
         gmpDataStore.setHfsLocation((String) DataHandlerUtil.getPropertyValue(entity, GmpDataStoreModel.PROPERTY_HFS_LOCATION));
         gmpDataStore.setMaxQueuedRequest((Integer) DataHandlerUtil.getPropertyValue(entity, GmpDataStoreModel.PROPERTY_MAX_QUEUED_REQUESTS));
         gmpDataStore.setIsMaster(((Boolean) DataHandlerUtil.getPropertyValue(entity, GmpDataStoreModel.PROPERTY_IS_MASTER)));

         // extract mysql complex property
         gmpDataStore.setMysqlConnectionInfo(extractMySQLConnectionInfo(entity, new GmpDataStoreConf.MysqlConnectionInfo()));
         // extract quotas complex property
         gmpDataStore.setQuotas(extractQuotas(entity, new GmpDataStoreConf.Quotas()));
         // extract GMP configuration
         gmpDataStore.setConfiguration(extractConfiguration(entity, new GmpDataStoreConf.Configuration()));
         dataStore = gmpDataStore;
      }
      else if (entity.getType().equals(RemoteDhusDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         RemoteDhusDataStoreConf remoteDhusDataStore = new RemoteDhusDataStoreConf();
         remoteDhusDataStore.setServiceUrl((String) DataHandlerUtil.getPropertyValue(entity, RemoteDhusDataStoreModel.PROPERTY_SERVICE_URL));
         remoteDhusDataStore.setLogin((String) DataHandlerUtil.getPropertyValue(entity, RemoteDhusDataStoreModel.PROPERTY_LOGIN));
         remoteDhusDataStore.setPassword((String) DataHandlerUtil.getPropertyValue(entity, RemoteDhusDataStoreModel.PROPERTY_PASSWORD));
         dataStore = remoteDhusDataStore;
      }

      // this means none of the expected type instantiated it
      if (dataStore == null)
      {
         throw new ODataApplicationException("Unknown DataStore type",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }

      // set properties common to all dataStore subtypes
      dataStore.setName((String) DataHandlerUtil.getPropertyValue(entity, DataStoreModel.PROPERTY_NAME));
      dataStore.setReadOnly((Boolean) DataHandlerUtil.getPropertyValue(entity, DataStoreModel.PROPERTY_READONLY));
      dataStore.setPriority((Integer) DataHandlerUtil.getPropertyValue(entity, DataStoreModel.PROPERTY_PRIORITY));
      dataStore.setMaximumSize((Long) DataHandlerUtil.getPropertyValue(entity, DataStoreModel.PROPERTY_MAXIMUMSIZE));
      dataStore.setCurrentSize((Long) DataHandlerUtil.getPropertyValue(entity, DataStoreModel.PROPERTY_CURRENTSIZE));
      dataStore.setAutoEviction((Boolean) DataHandlerUtil.getPropertyValue(entity, DataStoreModel.PROPERTY_AUTOEVICTION));

      DataStore liveDataStore;
      try
      {
         liveDataStore = DataStoreFactory.createDataStore(dataStore);
         DS_SERVICE.createNamed(dataStore);
      }
      catch (InvalidConfigurationException | UnavailableNameException | IllegalArgumentException e)
      {
         throw new ODataApplicationException(e.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
      DS_MANAGER.add(liveDataStore);
      return toOlingoEntity(dataStore);
   }

   @Override
   public void updateEntityData(List<UriParameter> keyParameters, Entity updatedEntity,  HttpMethod httpMethod)
         throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      NamedDataStoreConf dataStoreConf = getDataStoreFromService(keyParameters);

      // read only
      // String name = (String) DataHandlerUtil.getPropertyValue(updatedEntity, DataStoreModel.PROPERTY_NAME);
      Boolean readOnly = (Boolean) DataHandlerUtil.getPropertyValue(updatedEntity, DataStoreModel.PROPERTY_READONLY);
      Integer priority = (Integer) DataHandlerUtil.getPropertyValue(updatedEntity, DataStoreModel.PROPERTY_PRIORITY);
      Long maximumSize = (Long) DataHandlerUtil.getPropertyValue(updatedEntity, DataStoreModel.PROPERTY_MAXIMUMSIZE);
      // NOTE: the CurrentSize property is read only from an OData standpoint
      Boolean autoEviction = (Boolean) DataHandlerUtil.getPropertyValue(updatedEntity, DataStoreModel.PROPERTY_AUTOEVICTION);

      // get updated properties
      List<Property> updatedProperties = updatedEntity.getProperties();

      for (Property updatedproperty: updatedProperties)
      {
         String propertyName = updatedproperty.getName();

         // set properties common to all datastores
         if (propertyName.equals(DataStoreModel.PROPERTY_READONLY))
         {
            dataStoreConf.setReadOnly(readOnly);
         }
         if (propertyName.equals(DataStoreModel.PROPERTY_PRIORITY))
         {
            dataStoreConf.setPriority(priority);
         }
         if (propertyName.equals(DataStoreModel.PROPERTY_MAXIMUMSIZE))
         {
            dataStoreConf.setMaximumSize(maximumSize);
         }
         if (propertyName.equals(DataStoreModel.PROPERTY_AUTOEVICTION))
         {
            dataStoreConf.setAutoEviction(autoEviction);
         }

         if (dataStoreConf instanceof HfsDataStoreConf)
         {
            HfsDataStoreConf hfsDataStore = (HfsDataStoreConf) dataStoreConf;

            if (propertyName.equals(HfsDataStoreModel.PROPERTY_PATH))
            {
               String path = (String) DataHandlerUtil.getPropertyValue(updatedEntity, HfsDataStoreModel.PROPERTY_PATH);
               hfsDataStore.setPath(path);
            }
            if (propertyName.equals(HfsDataStoreModel.PROPERTY_MAXFILEDEPTH))
            {
               Integer maxFileDepth = (Integer) DataHandlerUtil.getPropertyValue(updatedEntity, HfsDataStoreModel.PROPERTY_MAXFILEDEPTH);
               hfsDataStore.setMaxFileNo(maxFileDepth);
            }
            if (propertyName.equals(HfsDataStoreModel.PROPERTY_MAXITEMS))
            {
               Integer maxItems = (Integer) DataHandlerUtil.getPropertyValue(updatedEntity, HfsDataStoreModel.PROPERTY_MAXITEMS);
               hfsDataStore.setMaxItems(maxItems);
            }
         }
         else if (dataStoreConf instanceof OpenStackDataStoreConf)
         {
            OpenStackDataStoreConf openstackDataStore = (OpenStackDataStoreConf) dataStoreConf;

            if (propertyName.equals(OpenstackDataStoreModel.PROPERTY_PROVIDER))
            {
               String provider = (String) DataHandlerUtil.getPropertyValue(updatedEntity, OpenstackDataStoreModel.PROPERTY_PROVIDER);
               openstackDataStore.setProvider(provider);
            }
            if (propertyName.equals(OpenstackDataStoreModel.PROPERTY_IDENTITY))
            {
               String identity = (String) DataHandlerUtil.getPropertyValue(updatedEntity, OpenstackDataStoreModel.PROPERTY_IDENTITY);
               openstackDataStore.setIdentity(identity);
            }
            if (propertyName.equals(OpenstackDataStoreModel.PROPERTY_CREDENTIAL))
            {
               String credential = (String) DataHandlerUtil.getPropertyValue(updatedEntity, OpenstackDataStoreModel.PROPERTY_CREDENTIAL);
               openstackDataStore.setCredential(credential);
            }
            if (propertyName.equals(OpenstackDataStoreModel.PROPERTY_URL))
            {
               String url = (String) DataHandlerUtil.getPropertyValue(updatedEntity, OpenstackDataStoreModel.PROPERTY_URL);
               openstackDataStore.setUrl(url);
            }
            if (propertyName.equals(OpenstackDataStoreModel.PROPERTY_CONTAINER))
            {
               String container = (String) DataHandlerUtil.getPropertyValue(updatedEntity, OpenstackDataStoreModel.PROPERTY_CONTAINER);
               openstackDataStore.setContainer(container);
            }
            if (propertyName.equals(OpenstackDataStoreModel.PROPERTY_REGION))
            {
               String region = (String) DataHandlerUtil.getPropertyValue(updatedEntity, OpenstackDataStoreModel.PROPERTY_REGION);
               openstackDataStore.setRegion(region);
            }
         }
         else if (dataStoreConf instanceof GmpDataStoreConf)
         {
            GmpDataStoreConf gmpDataStore = (GmpDataStoreConf) dataStoreConf;

            if (propertyName.equals(GmpDataStoreModel.PROPERTY_REPO_LOCATION))
            {
               String repoLocation = (String) DataHandlerUtil.getPropertyValue(updatedEntity, GmpDataStoreModel.PROPERTY_REPO_LOCATION);
               gmpDataStore.setGmpRepoLocation(repoLocation);
            }
            if (propertyName.equals(GmpDataStoreModel.PROPERTY_HFS_LOCATION))
            {
               String hfsLocation = (String) DataHandlerUtil.getPropertyValue(updatedEntity, GmpDataStoreModel.PROPERTY_HFS_LOCATION);
               gmpDataStore.setHfsLocation(hfsLocation);
            }
            if (propertyName.equals(GmpDataStoreModel.PROPERTY_MAX_QUEUED_REQUESTS))
            {
               Integer maxQueuedRequest = (Integer) DataHandlerUtil.getPropertyValue(updatedEntity, GmpDataStoreModel.PROPERTY_MAX_QUEUED_REQUESTS);
               gmpDataStore.setMaxQueuedRequest(maxQueuedRequest);
            }
            if (propertyName.equals(GmpDataStoreModel.PROPERTY_IS_MASTER))
            {
               Boolean isMaster = (Boolean) DataHandlerUtil.getPropertyValue(updatedEntity, GmpDataStoreModel.PROPERTY_IS_MASTER);
               gmpDataStore.setIsMaster(isMaster);
            }
            if (gmpDataStore.getMysqlConnectionInfo() == null)
            {
               gmpDataStore.setMysqlConnectionInfo(new MysqlConnectionInfo());
            }
            if (propertyName.equals(GmpDataStoreModel.PROPERTY_MYSQLCONNECTIONINFO))
            {
               // extract mysql complex property
               MysqlConnectionInfo mysqlConnectionInfo = extractMySQLConnectionInfo(updatedEntity,gmpDataStore.getMysqlConnectionInfo() );
               gmpDataStore.setMysqlConnectionInfo(mysqlConnectionInfo);
            }
            if (propertyName.equals(GmpDataStoreModel.PROPERTY_QUOTAS))
            {
               // extract quotas complex property
               GmpDataStoreConf.Quotas quotas = extractQuotas(updatedEntity, gmpDataStore.getQuotas());
               gmpDataStore.setQuotas(quotas);
            }
            if(gmpDataStore.getConfiguration() == null)
            {
               gmpDataStore.setConfiguration(new Configuration());
            }
            if(propertyName.equals(GmpDataStoreModel.PROPERTY_CONFIGURATION))
            {
               // extract gmp configuration
               Configuration configuration = extractConfiguration(updatedEntity, gmpDataStore.getConfiguration());
               gmpDataStore.setConfiguration(configuration);
            }
         }
         else if (dataStoreConf instanceof RemoteDhusDataStoreConf)
         {
            RemoteDhusDataStoreConf remoteDhusDataStore = (RemoteDhusDataStoreConf) dataStoreConf;

            if(propertyName.equals(RemoteDhusDataStoreModel.PROPERTY_SERVICE_URL))
            {
               String serviceUrl = (String) DataHandlerUtil.getPropertyValue(updatedEntity, RemoteDhusDataStoreModel.PROPERTY_SERVICE_URL);
               remoteDhusDataStore.setServiceUrl(serviceUrl);
            }
            if(propertyName.equals(RemoteDhusDataStoreModel.PROPERTY_LOGIN))
            {
               String login = (String) DataHandlerUtil.getPropertyValue(updatedEntity, RemoteDhusDataStoreModel.PROPERTY_LOGIN);
               remoteDhusDataStore.setLogin(login);
            }
            if(propertyName.equals(RemoteDhusDataStoreModel.PROPERTY_PASSWORD))
            {
               String password = (String) DataHandlerUtil.getPropertyValue(updatedEntity, RemoteDhusDataStoreModel.PROPERTY_PASSWORD);
               remoteDhusDataStore.setPassword(password);
            }
         }
         else
         {
            throw new ODataApplicationException("Unknown DataStore type",
                  HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
         }
      }

      DataStore newLiveDataStore;
      try
      {
         newLiveDataStore = DataStoreFactory.createDataStore(dataStoreConf);
      }
      catch (InvalidConfigurationException e)
      {
         throw new ODataApplicationException(e.getMessage(),
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }

      try
      {
         DS_MANAGER.remove(dataStoreConf.getName()).close();
      }
      catch (Exception suppressed) {}
      DS_SERVICE.update(dataStoreConf);
      DS_MANAGER.add(newLiveDataStore);
   }

   @Override
   public void deleteEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      NamedDataStoreConf dataStoreConf = getDataStoreFromService(keyParameters);
      DS_SERVICE.delete(dataStoreConf);

      try
      {
         DS_MANAGER.remove(dataStoreConf.getName()).close();
      }
      catch (Exception suppressed) {}
   }

   private NamedDataStoreConf getDataStoreFromService(List<UriParameter> keyParameters)
         throws ODataApplicationException
   {
      NamedDataStoreConf dataStoreConf = null;
      String dataStoreName = null;
      for (UriParameter keyParameter: keyParameters)
      {
         if (keyParameter.getName().equals(DataStoreModel.PROPERTY_NAME))
         {
            dataStoreName = DataHandlerUtil.trimStringKeyParameter(keyParameter);
         }
      }

      dataStoreConf = DS_SERVICE.getNamedDataStore(dataStoreName);
      if (dataStoreConf == null)
      {
         throw new ODataApplicationException("DataStore not found: " + dataStoreName,
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }
      return dataStoreConf;
   }

   @Override
   public void updateReference(List<UriParameter> sourceKeyPredicates, EdmNavigationProperty navigationProperty, List<UriParameter> targetKeyPredicates)
         throws ODataApplicationException
   {
      if (navigationProperty.getName().equals(DataStoreModel.NAVIGATION_EVICTION))
      {
         // get datastore
         NamedDataStoreConf dataStoreConf = getDataStoreFromService(sourceKeyPredicates);

         // get and set eviction name
         for (UriParameter keyParameter: targetKeyPredicates)
         {
            if (keyParameter.getName().equals(EvictionModel.NAME))
            {
               String evictionName = DataHandlerUtil.trimStringKeyParameter(keyParameter);
               dataStoreConf.setEvictionName(evictionName);
               DS_SERVICE.update(dataStoreConf);
               return;
            }
         }
      }
      else
      {
         throw new ODataApplicationException("Cannot update such reference: " + navigationProperty,
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
   }

   @Override
   public void deleteReference(List<UriParameter> sourceKeyPredicates, EdmNavigationProperty navigationProperty)
         throws ODataApplicationException
   {
      if (navigationProperty.getName().equals(DataStoreModel.NAVIGATION_EVICTION))
      {
         NamedDataStoreConf dataStoreConf = getDataStoreFromService(sourceKeyPredicates);
         dataStoreConf.setEvictionName(null);
         DS_SERVICE.update(dataStoreConf);
      }
      else
      {
         throw new ODataApplicationException("Cannot update such reference: " + navigationProperty,
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
   }
}
