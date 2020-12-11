/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2019 GAEL Systems
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;

import org.dhus.olingo.v2.ODataSecurityManager;
import org.dhus.olingo.v2.datamodel.AsyncDataStoreModel;
import org.dhus.olingo.v2.datamodel.DataStoreModel;
import org.dhus.olingo.v2.datamodel.EvictionModel;
import org.dhus.olingo.v2.datamodel.GmpDataStoreModel;
import org.dhus.olingo.v2.datamodel.HfsDataStoreModel;
import org.dhus.olingo.v2.datamodel.ParamPdgsDataStoreModel;
import org.dhus.olingo.v2.datamodel.OpenstackDataStoreModel;
import org.dhus.olingo.v2.datamodel.PdgsDataStoreModel;
import org.dhus.olingo.v2.datamodel.RemoteDhusDataStoreModel;
import org.dhus.olingo.v2.datamodel.complex.GMPConfigurationComplexType;
import org.dhus.olingo.v2.datamodel.complex.MySQLConnectionInfoComplexType;
import org.dhus.olingo.v2.datamodel.complex.PatternReplaceComplexType;
import org.dhus.olingo.v2.entity.TypeStore;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreFactory;
import org.dhus.store.datastore.DataStoreFactory.InvalidConfigurationException;
import org.dhus.store.datastore.DataStoreManager;
import org.dhus.store.datastore.config.AsyncDataStoreConf;
import org.dhus.store.datastore.config.DataStoreManager.UnavailableNameException;
import org.dhus.store.datastore.config.DataStoreRestriction;
import org.dhus.store.datastore.config.GmpDataStoreConf;
import org.dhus.store.datastore.config.GmpDataStoreConf.Configuration;
import org.dhus.store.datastore.config.GmpDataStoreConf.MysqlConnectionInfo;
import org.dhus.store.datastore.config.HfsDataStoreConf;
import org.dhus.store.datastore.config.ParamPdgsDataStoreConf;
import org.dhus.store.datastore.config.NamedDataStoreConf;
import org.dhus.store.datastore.config.OpenStackDataStoreConf;
import org.dhus.store.datastore.config.PatternReplace;
import org.dhus.store.datastore.config.PdgsDataStoreConf;
import org.dhus.store.datastore.config.RemoteDhusDataStoreConf;

/**
 * Provides data for DataStore entities.
 */
public class DataStoreDataHandler implements DataHandler
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final DataStoreManager DS_MANAGER =
         ApplicationContextProvider.getBean(DataStoreManager.class);

   private static final DataStoreService DS_SERVICE =
         ApplicationContextProvider.getBean(DataStoreService.class);

   /** Entity producer for abstract type NamedDataStoreConf. */
   private final TypeStore.Node dataStoreEntityProducerNode;

   public DataStoreDataHandler(TypeStore typeStore)
   {
      this.dataStoreEntityProducerNode = typeStore.get(NamedDataStoreConf.class);
   }

   private Entity toOlingoEntity(NamedDataStoreConf dsConf)
   {
      Entity res;

      // Get specialised entity producer for the given input type
      TypeStore.Node entityProducerNode = dataStoreEntityProducerNode.get(dsConf.getClass());
      if (entityProducerNode != null)
      {
         res = entityProducerNode.getEntityProducer().toOlingoEntity(dsConf);
      }
      else
      {
         res = dataStoreEntityProducerNode.getEntityProducer().toOlingoEntity(dsConf);
      }

      return res;
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

   private PatternReplace extractPatternReplace(Entity entity, PatternReplace patternReplace, boolean in)
   {
      Property property;
      if (in)
      {
         property = entity.getProperty(AsyncDataStoreModel.PROPERTY_PATRE_IN);
      }
      else
      {
         property = entity.getProperty(AsyncDataStoreModel.PROPERTY_PATRE_OUT);
      }

      if (property != null && property.getValue() != null)
      {
         for (Property prop: ComplexValue.class.cast(property.getValue()).getValue())
         {
            switch (prop.getName())
            {
               case PatternReplaceComplexType.PROPERTY_PATTERN:
                  patternReplace.setPattern(String.class.cast(prop.getValue()));
                  break;

               case PatternReplaceComplexType.PROPERTY_REPLACEMENT:
                  patternReplace.setReplacement(String.class.cast(prop.getValue()));
                  break;
            }
         }
         return patternReplace;
      }
      return null;
   }

   private GmpDataStoreConf.Configuration extractConfiguration(Entity entity, Configuration configuration)
   {
      Property configurationProperty = entity.getProperty(GmpDataStoreModel.PROPERTY_CONFIGURATION);
      if (configurationProperty != null && configurationProperty.getValue() != null)
      {
         for (Property property: ((ComplexValue) configurationProperty.getValue()).getValue())
         {
            switch (property.getName())
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
            NamedDataStoreConf dsConf = DS_SERVICE.getNamedDataStore(dataStoreName);
            return toOlingoEntity(dsConf);
         }
      }
      return null;
   }

   @Override
   public Entity createEntityData(Entity entity) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      // Required properties
      String datastoreName = DataHandlerUtil.<String>getRequiredProperty(entity, DataStoreModel.PROPERTY_NAME, String.class);

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

      else if (entity.getType().equals(RemoteDhusDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         RemoteDhusDataStoreConf remoteDhusDataStore = new RemoteDhusDataStoreConf();
         remoteDhusDataStore.setServiceUrl((String) DataHandlerUtil.getPropertyValue(entity, RemoteDhusDataStoreModel.PROPERTY_SERVICE_URL));
         remoteDhusDataStore.setLogin((String) DataHandlerUtil.getPropertyValue(entity, RemoteDhusDataStoreModel.PROPERTY_LOGIN));
         remoteDhusDataStore.setPassword((String) DataHandlerUtil.getPropertyValue(entity, RemoteDhusDataStoreModel.PROPERTY_PASSWORD));
         remoteDhusDataStore.setAliveInterval((Long) DataHandlerUtil.getPropertyValue(entity, RemoteDhusDataStoreModel.PROPERTY_ALIVE_INTERVAL));
         dataStore = remoteDhusDataStore;
      }

      else if (entity.getType().equals(GmpDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         GmpDataStoreConf gmpDataStore = new GmpDataStoreConf();

         // common async
         createAsyncDataStoreConf(gmpDataStore, entity);

         gmpDataStore.setRepoLocation((String) DataHandlerUtil.getPropertyValue(entity, GmpDataStoreModel.PROPERTY_REPO_LOCATION));

         // extract mysql complex property
         gmpDataStore.setMysqlConnectionInfo(extractMySQLConnectionInfo(entity, new GmpDataStoreConf.MysqlConnectionInfo()));
         // extract GMP configuration
         gmpDataStore.setConfiguration(extractConfiguration(entity, new GmpDataStoreConf.Configuration()));
         dataStore = gmpDataStore;
      }
      else if (entity.getType().equals(PdgsDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString())
            || entity.getType().equals(ParamPdgsDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         PdgsDataStoreConf pdgsDhusDataStore;

         // metadata pdgs datastore
         if(entity.getType().equals(ParamPdgsDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
         {
            ParamPdgsDataStoreConf conf = new ParamPdgsDataStoreConf();
            conf.setUrlParamPattern((String) DataHandlerUtil.getPropertyValue(entity, ParamPdgsDataStoreModel.PROPERTY_GPUP_PATTERN));
            conf.setProductNamePattern((String) DataHandlerUtil.getPropertyValue(entity, ParamPdgsDataStoreModel.PROPERTY_PNAME_PATTERN));
            pdgsDhusDataStore = conf;
         }
         else
         {
            pdgsDhusDataStore = new PdgsDataStoreConf();
         }

         // common async
         createAsyncDataStoreConf(pdgsDhusDataStore, entity);

         pdgsDhusDataStore.setServiceUrl((String) DataHandlerUtil.getPropertyValue(entity, PdgsDataStoreModel.PROPERTY_SERVICE_URL));
         pdgsDhusDataStore.setLogin((String) DataHandlerUtil.getPropertyValue(entity, PdgsDataStoreModel.PROPERTY_LOGIN));
         pdgsDhusDataStore.setPassword((String) DataHandlerUtil.getPropertyValue(entity, PdgsDataStoreModel.PROPERTY_PASSWORD));
         pdgsDhusDataStore.setMaxConcurrentsDownloads(((int) DataHandlerUtil.getPropertyValue(entity, PdgsDataStoreModel.PROPERTY_MAX_CONCURRENTS_DOWNLOADS)));
         pdgsDhusDataStore.setInterval(((long) DataHandlerUtil.getPropertyValue(entity, PdgsDataStoreModel.PROPERTY_INTERVAL)));

         dataStore = pdgsDhusDataStore;
      }

      // this means none of the expected type instantiated it
      if (dataStore == null)
      {
         throw new ODataApplicationException("Unknown DataStore type",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }

      // set properties common to all dataStore subtypes
      dataStore.setName(datastoreName);
      String restriction = (String) DataHandlerUtil.getPropertyValue(entity, DataStoreModel.PROPERTY_RESTRICTION);
      if (restriction != null)
      {
         dataStore.setRestriction(DataStoreRestriction.fromValue(restriction));
      }
      dataStore.setPriority((Integer) DataHandlerUtil.getPropertyValue(entity, DataStoreModel.PROPERTY_PRIORITY));
      dataStore.setMaximumSize((Long) DataHandlerUtil.getPropertyValue(entity, DataStoreModel.PROPERTY_MAXIMUMSIZE));
      dataStore.setCurrentSize((Long) DataHandlerUtil.getPropertyValue(entity, DataStoreModel.PROPERTY_CURRENTSIZE));
      dataStore.setAutoEviction((Boolean) DataHandlerUtil.getPropertyValue(entity, DataStoreModel.PROPERTY_AUTOEVICTION));
      dataStore.setFilter((String) DataHandlerUtil.getPropertyValue(entity, DataStoreModel.PROPERTY_FILTER));

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

      // add live data store as part of the store hierarchy
      DS_MANAGER.add(liveDataStore);

      return toOlingoEntity(dataStore);
   }

   private void createAsyncDataStoreConf(AsyncDataStoreConf asyncDataStore, Entity entity)
   {
      asyncDataStore.setHfsLocation((String) DataHandlerUtil.getPropertyValue(entity, AsyncDataStoreModel.PROPERTY_HFS_LOCATION));
      asyncDataStore.setIsMaster(((Boolean) DataHandlerUtil.getPropertyValue(entity, AsyncDataStoreModel.PROPERTY_IS_MASTER)));

      // extract product naming
      asyncDataStore.setPatternReplaceIn(extractPatternReplace(entity, new PatternReplace(), true));
      asyncDataStore.setPatternReplaceOut(extractPatternReplace(entity, new PatternReplace(), false));

      // max concurrent requests per datastore
      asyncDataStore.setMaxPendingRequests((Integer) DataHandlerUtil.getPropertyValue(entity, AsyncDataStoreModel.PROPERTY_MAX_PENDING_REQUESTS));
      asyncDataStore.setMaxRunningRequests((Integer) DataHandlerUtil.getPropertyValue(entity, AsyncDataStoreModel.PROPERTY_MAX_RUNNING_REQUESTS));

      // max concurrent requests per user
      asyncDataStore.setMaxParallelFetchRequestsPerUser((Integer) DataHandlerUtil.getPropertyValue(entity, AsyncDataStoreModel.PROPERTY_MAX_PFRPU));
   }

   @Override
   public void updateEntityData(List<UriParameter> keyParameters, Entity updatedEntity, HttpMethod httpMethod)
         throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      NamedDataStoreConf dataStoreConf = getDataStoreFromService(keyParameters);

      // read only
      // String name = (String) DataHandlerUtil.getPropertyValue(updatedEntity, DataStoreModel.PROPERTY_NAME);
      String restriction = (String) DataHandlerUtil.getPropertyValue(updatedEntity, DataStoreModel.PROPERTY_RESTRICTION);
      Integer priority = (Integer) DataHandlerUtil.getPropertyValue(updatedEntity, DataStoreModel.PROPERTY_PRIORITY);
      Long maximumSize = (Long) DataHandlerUtil.getPropertyValue(updatedEntity, DataStoreModel.PROPERTY_MAXIMUMSIZE);
      // NOTE: the CurrentSize property is read only from an OData standpoint
      Boolean autoEviction = (Boolean) DataHandlerUtil.getPropertyValue(updatedEntity, DataStoreModel.PROPERTY_AUTOEVICTION);
      String filter = (String) DataHandlerUtil.getPropertyValue(updatedEntity, DataStoreModel.PROPERTY_FILTER);

      // get updated properties
      List<Property> updatedProperties = updatedEntity.getProperties();

      for (Property updatedproperty: updatedProperties)
      {
         String propertyName = updatedproperty.getName();

         // set properties common to all datastores
         if (propertyName.equals(DataStoreModel.PROPERTY_RESTRICTION))
         {
            dataStoreConf.setRestriction(DataStoreRestriction.fromValue(restriction));
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
         if (propertyName.equals(DataStoreModel.PROPERTY_FILTER))
         {
            dataStoreConf.setFilter(filter);
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
         else if (dataStoreConf instanceof RemoteDhusDataStoreConf)
         {
            RemoteDhusDataStoreConf remoteDhusDataStore = (RemoteDhusDataStoreConf) dataStoreConf;

            if (propertyName.equals(RemoteDhusDataStoreModel.PROPERTY_SERVICE_URL))
            {
               String serviceUrl = (String) DataHandlerUtil.getPropertyValue(updatedEntity, RemoteDhusDataStoreModel.PROPERTY_SERVICE_URL);
               remoteDhusDataStore.setServiceUrl(serviceUrl);
            }
            if (propertyName.equals(RemoteDhusDataStoreModel.PROPERTY_LOGIN))
            {
               String login = (String) DataHandlerUtil.getPropertyValue(updatedEntity, RemoteDhusDataStoreModel.PROPERTY_LOGIN);
               remoteDhusDataStore.setLogin(login);
            }
            if (propertyName.equals(RemoteDhusDataStoreModel.PROPERTY_PASSWORD))
            {
               String password = (String) DataHandlerUtil.getPropertyValue(updatedEntity, RemoteDhusDataStoreModel.PROPERTY_PASSWORD);
               remoteDhusDataStore.setPassword(password);
            }
            if (propertyName.equals(RemoteDhusDataStoreModel.PROPERTY_ALIVE_INTERVAL))
            {
               Long aliveInterval = (Long) DataHandlerUtil.getPropertyValue(updatedEntity, RemoteDhusDataStoreModel.PROPERTY_ALIVE_INTERVAL);
               remoteDhusDataStore.setAliveInterval(aliveInterval);
            }
         }

         else if (dataStoreConf instanceof GmpDataStoreConf)
         {
            GmpDataStoreConf gmpDataStore = (GmpDataStoreConf) dataStoreConf;

            // common async
            updateAsyncDataStoreProperty(updatedEntity, gmpDataStore, propertyName);

            if (propertyName.equals(GmpDataStoreModel.PROPERTY_REPO_LOCATION))
            {
               String repoLocation = (String) DataHandlerUtil.getPropertyValue(updatedEntity, GmpDataStoreModel.PROPERTY_REPO_LOCATION);
               gmpDataStore.setRepoLocation(repoLocation);
            }
            if (gmpDataStore.getMysqlConnectionInfo() == null)
            {
               gmpDataStore.setMysqlConnectionInfo(new MysqlConnectionInfo());
            }
            if (propertyName.equals(GmpDataStoreModel.PROPERTY_MYSQLCONNECTIONINFO))
            {
               // extract mysql complex property
               MysqlConnectionInfo mysqlConnectionInfo = extractMySQLConnectionInfo(updatedEntity, gmpDataStore.getMysqlConnectionInfo());
               gmpDataStore.setMysqlConnectionInfo(mysqlConnectionInfo);
            }
            if (propertyName.equals(GmpDataStoreModel.PROPERTY_CONFIGURATION))
            {
               // extract gmp configuration
               Configuration configuration = extractConfiguration(
                     updatedEntity,
                     gmpDataStore.getConfiguration() == null ? new Configuration() : gmpDataStore.getConfiguration());

               gmpDataStore.setConfiguration(configuration);
            }
         }
         else if (dataStoreConf instanceof PdgsDataStoreConf)
         {
            PdgsDataStoreConf pdgsDataStore = (PdgsDataStoreConf) dataStoreConf;

            // common async
            updateAsyncDataStoreProperty(updatedEntity, pdgsDataStore, propertyName);

            if (propertyName.equals(PdgsDataStoreModel.PROPERTY_SERVICE_URL))
            {
               String serviceUrl = (String) DataHandlerUtil.getPropertyValue(updatedEntity, PdgsDataStoreModel.PROPERTY_SERVICE_URL);
               pdgsDataStore.setServiceUrl(serviceUrl);
            }
            if (propertyName.equals(PdgsDataStoreModel.PROPERTY_LOGIN))
            {
               String login = (String) DataHandlerUtil.getPropertyValue(updatedEntity, PdgsDataStoreModel.PROPERTY_LOGIN);
               pdgsDataStore.setLogin(login);
            }
            if (propertyName.equals(PdgsDataStoreModel.PROPERTY_PASSWORD))
            {
               String password = (String) DataHandlerUtil.getPropertyValue(updatedEntity, PdgsDataStoreModel.PROPERTY_PASSWORD);
               pdgsDataStore.setPassword(password);
            }
            if (propertyName.equals(PdgsDataStoreModel.PROPERTY_MAX_CONCURRENTS_DOWNLOADS))
            {
               int maxConcurrentsDownloads = (int) DataHandlerUtil.getPropertyValue(updatedEntity, PdgsDataStoreModel.PROPERTY_MAX_CONCURRENTS_DOWNLOADS);
               pdgsDataStore.setMaxConcurrentsDownloads(maxConcurrentsDownloads);
            }
            if (propertyName.equals(PdgsDataStoreModel.PROPERTY_INTERVAL))
            {
               long interval = (long) DataHandlerUtil.getPropertyValue(updatedEntity, PdgsDataStoreModel.PROPERTY_INTERVAL);
               pdgsDataStore.setInterval(interval);
            }

            if (dataStoreConf instanceof ParamPdgsDataStoreConf)
            {
               ParamPdgsDataStoreConf paramDataStore = (ParamPdgsDataStoreConf) dataStoreConf;

               if (propertyName.equals(ParamPdgsDataStoreModel.PROPERTY_GPUP_PATTERN))
               {
                  String gpup = (String) DataHandlerUtil.getPropertyValue(updatedEntity, ParamPdgsDataStoreModel.PROPERTY_GPUP_PATTERN);
                  paramDataStore.setUrlParamPattern(gpup);
               }
               if (propertyName.equals(ParamPdgsDataStoreModel.PROPERTY_PNAME_PATTERN))
               {
                  String pname = (String) DataHandlerUtil.getPropertyValue(updatedEntity, ParamPdgsDataStoreModel.PROPERTY_PNAME_PATTERN);
                  paramDataStore.setProductNamePattern(pname);
               }
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
      catch (Exception ex)
      {
         LOGGER.warn("DataStore.close() has thrown an exception", ex);
      }
      DS_SERVICE.update(dataStoreConf);
      DS_MANAGER.add(newLiveDataStore);
   }

   private void updateAsyncDataStoreProperty(Entity updatedEntity, AsyncDataStoreConf asyncDataStore, String propertyName)
   {
      if (propertyName.equals(AsyncDataStoreModel.PROPERTY_HFS_LOCATION))
      {
         String hfsLocation = (String) DataHandlerUtil.getPropertyValue(updatedEntity, AsyncDataStoreModel.PROPERTY_HFS_LOCATION);
         asyncDataStore.setHfsLocation(hfsLocation);
      }
      if (propertyName.equals(AsyncDataStoreModel.PROPERTY_IS_MASTER))
      {
         Boolean isMaster = (Boolean) DataHandlerUtil.getPropertyValue(updatedEntity, AsyncDataStoreModel.PROPERTY_IS_MASTER);
         asyncDataStore.setIsMaster(isMaster);
      }
      if (propertyName.equals(AsyncDataStoreModel.PROPERTY_MAX_PFRPU))
      {
         asyncDataStore.setMaxParallelFetchRequestsPerUser((Integer) DataHandlerUtil.getPropertyValue(updatedEntity, AsyncDataStoreModel.PROPERTY_MAX_PFRPU));
      }
      if (propertyName.equals(AsyncDataStoreModel.PROPERTY_PATRE_IN))
      {
         // extract productNaming complex property
         PatternReplace patterReplaceIn = extractPatternReplace(
               updatedEntity,
               asyncDataStore.getPatternReplaceIn() == null ? new PatternReplace() : asyncDataStore.getPatternReplaceIn(),
               true);
         asyncDataStore.setPatternReplaceIn(patterReplaceIn);
      }
      if (propertyName.equals(AsyncDataStoreModel.PROPERTY_PATRE_OUT))
      {
         PatternReplace patterReplaceOut = extractPatternReplace(
               updatedEntity,
               asyncDataStore.getPatternReplaceOut() == null ? new PatternReplace() : asyncDataStore.getPatternReplaceOut(),
               false);
         asyncDataStore.setPatternReplaceOut(patterReplaceOut);
      }
      if (propertyName.equals(AsyncDataStoreModel.PROPERTY_MAX_PENDING_REQUESTS))
      {
         Integer maxPendingRequests = (Integer) DataHandlerUtil.getPropertyValue(updatedEntity, AsyncDataStoreModel.PROPERTY_MAX_PENDING_REQUESTS);
         asyncDataStore.setMaxPendingRequests(maxPendingRequests);
      }
      if (propertyName.equals(AsyncDataStoreModel.PROPERTY_MAX_RUNNING_REQUESTS))
      {
         Integer maxRunningRequest = (Integer) DataHandlerUtil.getPropertyValue(updatedEntity, AsyncDataStoreModel.PROPERTY_MAX_RUNNING_REQUESTS);
         asyncDataStore.setMaxRunningRequests(maxRunningRequest);
      }
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
      catch (Exception ex)
      {
         LOGGER.warn("DataStore.close() has thrown an exception", ex);
      }
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
