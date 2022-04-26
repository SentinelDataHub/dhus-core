/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2020 GAEL Systems
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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
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
import org.dhus.olingo.v2.datamodel.HttpAsyncDataStoreModel;
import org.dhus.olingo.v2.datamodel.LtaDataStoreModel;
import org.dhus.olingo.v2.datamodel.OndaDataStoreModel;
import org.dhus.olingo.v2.datamodel.OpenstackDataStoreModel;
import org.dhus.olingo.v2.datamodel.ParamPdgsDataStoreModel;
import org.dhus.olingo.v2.datamodel.PdgsDataStoreModel;
import org.dhus.olingo.v2.datamodel.RemoteDhusDataStoreModel;
import org.dhus.olingo.v2.datamodel.complex.CronComplexType;
import org.dhus.olingo.v2.datamodel.complex.GMPConfigurationComplexType;
import org.dhus.olingo.v2.datamodel.complex.MySQLConnectionInfoComplexType;
import org.dhus.olingo.v2.datamodel.complex.ObjectStorageComplexType;
import org.dhus.olingo.v2.datamodel.complex.OndaScannerComplexType;
import org.dhus.olingo.v2.datamodel.complex.PatternReplaceComplexType;
import org.dhus.olingo.v2.entity.TypeStore;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreFactory;
import org.dhus.store.datastore.DataStoreFactory.InvalidConfigurationException;
import org.dhus.store.datastore.DataStoreManager;
import org.dhus.store.datastore.config.AsyncDataStoreConf;
import org.dhus.store.datastore.config.DataStoreConf;
import org.dhus.store.datastore.config.DataStoreManager.UnavailableNameException;
import org.dhus.store.datastore.config.DataStoreRestriction;
import org.dhus.store.datastore.config.GmpDataStoreConf;
import org.dhus.store.datastore.config.GmpDataStoreConf.Configuration;
import org.dhus.store.datastore.config.GmpDataStoreConf.MysqlConnectionInfo;
import org.dhus.store.datastore.config.HfsDataStoreConf;
import org.dhus.store.datastore.config.HttpAsyncDataStoreConf;
import org.dhus.store.datastore.config.LtaDataStoreConf;
import org.dhus.store.datastore.config.NamedDataStoreConf;
import org.dhus.store.datastore.config.ObjectStorageCredentialConf;
import org.dhus.store.datastore.config.OndaDataStoreConf;
import org.dhus.store.datastore.config.OndaScannerConf;
import org.dhus.store.datastore.config.OpenStackDataStoreConf;
import org.dhus.store.datastore.config.ParamPdgsDataStoreConf;
import org.dhus.store.datastore.config.PatternReplace;
import org.dhus.store.datastore.config.PdgsDataStoreConf;
import org.dhus.store.datastore.config.RemoteDhusDataStoreConf;

import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.config.cron.Cron;
import fr.gael.dhus.service.DataStoreService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.odata.engine.data.DataHandler;
import fr.gael.odata.engine.data.DataHandlerUtil;

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
   
   private ObjectStorageCredentialConf extractObjectStorageCredential(Entity entity, ObjectStorageCredentialConf objectStorageConf)
   {
      Property objectStorageCredentials = entity.getProperty(OndaDataStoreModel.PROPERTY_OBJECT_STORAGE_CREDENTIAL);
      if (objectStorageCredentials != null && objectStorageCredentials.getValue() != null)
      {
         for (Property property : ((ComplexValue) objectStorageCredentials.getValue()).getValue())
         {
            switch (property.getName())
            {     
               case ObjectStorageComplexType.PROPERTY_PROVIDER:
                  objectStorageConf.setProvider((String)property.getValue());
                  break;
                 
               case ObjectStorageComplexType.PROPERTY_IDENTITY:
                  objectStorageConf.setIdentity((String)property.getValue());
                  break;
                  
               case ObjectStorageComplexType.PROPERTY_CREDENTIAL:
                  objectStorageConf.setCredential((String)property.getValue());
                  break;
                  
               case ObjectStorageComplexType.PROPERTY_URL:
                  objectStorageConf.setUrl((String)property.getValue());
                  break;
                  
               case ObjectStorageComplexType.PROPERTY_REGION:
                  objectStorageConf.setRegion((String)property.getValue());
                  break;
                  
            }
         }
         return objectStorageConf;
      }
      return null;
   }

   private OndaScannerConf extractOndaScanner(Entity entity, OndaScannerConf ondaScannerConf)
   {
      Property ondaScanner = entity.getProperty(OndaDataStoreModel.PROPERTY_ONDA_SCANNER);
      if (ondaScanner != null && ondaScanner.getValue() != null)
      {
         for (Property property : ((ComplexValue) ondaScanner.getValue()).getValue())
         {
            switch (property.getName())
            {
               case OndaScannerComplexType.PROPERTY_OPENSEARCH_URL:
                  ondaScannerConf.setOpensearchUrl((String)property.getValue());
                  break;
                  
               case OndaScannerComplexType.PROPERTY_LAST_CREATION_DATE:
                  Timestamp time = (Timestamp) property.getValue();
                  
                  LocalDateTime ldt = time.toLocalDateTime();

                  XMLGregorianCalendar cal;
                  try
                  {
                     cal = DatatypeFactory.newInstance().newXMLGregorianCalendar();

                     cal.setYear(ldt.getYear());
                     cal.setMonth(ldt.getMonthValue());
                     cal.setDay(ldt.getDayOfMonth());
                     cal.setHour(ldt.getHour());
                     cal.setMinute(ldt.getMinute());
                     cal.setSecond(ldt.getSecond());
                     String nanos = "0." + StringUtils.leftPad(String.valueOf(ldt.getNano()), 9, '0');
                     cal.setFractionalSecond(new BigDecimal(nanos));
                     
                     ondaScannerConf.setLastCreationDate(cal);
                  }
                  catch (DatatypeConfigurationException e)
                  {
                     LOGGER.error("Error while updating "+OndaScannerComplexType.PROPERTY_LAST_CREATION_DATE+".", e);
                  }
                  break;
                  
               case OndaScannerComplexType.PROPERTY_FILTER:
                  ondaScannerConf.setFilter((String)property.getValue());
                  break;
                  
               case OndaScannerComplexType.PROPERTY_PAGE_SIZE:
                  Object pageSize = property.getValue();
                  if (pageSize != null)
                  {
                     ondaScannerConf.setPageSize((int) pageSize);
                  }
                  break;

               case OndaScannerComplexType.PROPERTY_CRON:
                  if (ondaScannerConf.getCron() == null)
                  {
                     ondaScannerConf.setCron(new Cron());
                  }
                  for (Property cronProperty : ((ComplexValue) property.getValue()).getValue())
                  {
                     switch (cronProperty.getName())
                     {
                        case CronComplexType.PROPERTY_ACTIVE: ondaScannerConf.getCron()
                        .setActive((boolean) cronProperty.getValue());
                           break;
                           
                        case CronComplexType.PROPERTY_SCHEDULE: ondaScannerConf.getCron()
                        .setSchedule((String) cronProperty.getValue());
                           break;
                     }
                  }
                  break;
            }
         }
         return ondaScannerConf;
      }
      return null;
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
            if (dsConf != null)
            {
               return toOlingoEntity(dsConf);
            }
         }
      }
      return null;
   }

   @Override
   public Entity createEntityData(Entity entity) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      // check datastore name
      String datastoreName = DataHandlerUtil.<String>getRequiredProperty(entity, DataStoreModel.PROPERTY_NAME, String.class);
      if (DS_MANAGER.getDataStoreByName(datastoreName) != null)
      {
         throw new ODataApplicationException("The DataStore name '" + datastoreName + "' is unavailable", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
      
      // check cache name
      Link link = entity.getNavigationLink(DataStoreModel.NAVIGATION_CACHE);
      if (link != null)
      {
         Entity cacheEntity = link.getInlineEntity();
         String cacheName = DataHandlerUtil.<String>getRequiredProperty(cacheEntity, DataStoreModel.PROPERTY_NAME, String.class);
         if (DS_MANAGER.getDataStoreByName(cacheName) != null)
         {
            throw new ODataApplicationException("The cache DataStore name '" + cacheName + "' is unavailable", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
         }
      }

      NamedDataStoreConf dataStore = null;

      if (entity.getType().equals(HfsDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {  
         HfsDataStoreConf hfsDataStore = createHfsDataStoreEntity(entity);
         dataStore = hfsDataStore;
      }
      else if (entity.getType().equals(OpenstackDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         OpenStackDataStoreConf openstackDataStore = createOpenStackEntity(entity);
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
         // common httpAsync
         createhttpAsyncDataStore(pdgsDhusDataStore, entity);

         dataStore = pdgsDhusDataStore;
      }

      else if (entity.getType().equals(LtaDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         LtaDataStoreConf ltaDataStoreConf = new LtaDataStoreConf();

         // common async
         createAsyncDataStoreConf(ltaDataStoreConf, entity);
         // common httpAsync
         createhttpAsyncDataStore(ltaDataStoreConf, entity);
         // optional order
         Object order = DataHandlerUtil.getPropertyValue(entity, LtaDataStoreModel.PROPERTY_ORDER);
         if (order == null)
         {
            ltaDataStoreConf.setOrder(false);
         }
         else
         {
            ltaDataStoreConf.setOrder((boolean) order);
         }
         dataStore = ltaDataStoreConf;
      }

      else if (entity.getType().equals(OndaDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         OndaDataStoreConf ondaDataStoreConf = new OndaDataStoreConf();

         // common async
         createAsyncDataStoreConf(ondaDataStoreConf, entity);
         // common httpAsync
         createhttpAsyncDataStore(ondaDataStoreConf, entity);
         // optional order
         Object order = DataHandlerUtil.getPropertyValue(entity, LtaDataStoreModel.PROPERTY_ORDER);
         if (order == null)
         {
            ondaDataStoreConf.setOrder(false);
         }
         else
         {
            ondaDataStoreConf.setOrder((boolean) order);
         }
         // objectStorageCredentials
         ObjectStorageCredentialConf objectStorageProperty = extractObjectStorageCredential(entity, new ObjectStorageCredentialConf());
         if (objectStorageProperty != null)
         {
            ondaDataStoreConf.setObjectStorageCredential(objectStorageProperty);
         }

         // ondaScanner
         OndaScannerConf ondaScannerProperty = extractOndaScanner(entity, new OndaScannerConf());
         if (ondaScannerProperty != null)
         {
            ondaDataStoreConf.setOndaScanner(ondaScannerProperty);
         }

         dataStore = ondaDataStoreConf;
      }

      // this means none of the expected type instantiated it
      if (dataStore == null)
      {
         throw new ODataApplicationException("Unknown DataStore type",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }

      setCommonProperties(entity, dataStore);

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
         throws ODataApplicationException
   {
      Link link = entity.getNavigationLink(DataStoreModel.NAVIGATION_CACHE);
      Entity cacheEntity = link.getInlineEntity();

      if (HfsDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()
            .equals(cacheEntity.getType()))
      {
         HfsDataStoreConf hfsCache = createHfsDataStoreEntity(cacheEntity);
         setCommonProperties(cacheEntity, hfsCache);
         asyncDataStore.setDataStore(hfsCache);
      }
      else if (OpenstackDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()
            .equals(cacheEntity.getType()))
      {
         OpenStackDataStoreConf openStackCache = createOpenStackEntity(cacheEntity);
         setCommonProperties(cacheEntity, openStackCache);
         asyncDataStore.setDataStore(openStackCache);
      }

      // isMaster
      Object isMaster = DataHandlerUtil.getPropertyValue(entity, AsyncDataStoreModel.PROPERTY_IS_MASTER);
      if(isMaster == null)
      {
         asyncDataStore.setIsMaster(false);   
      }
      else
      {
         asyncDataStore.setIsMaster((Boolean)(isMaster));
      }

      // extract product naming
      asyncDataStore.setPatternReplaceIn(extractPatternReplace(entity, new PatternReplace(), true));
      asyncDataStore.setPatternReplaceOut(extractPatternReplace(entity, new PatternReplace(), false));

      // max concurrent requests per datastore
      asyncDataStore.setMaxPendingRequests((Integer) DataHandlerUtil.getPropertyValue(entity, AsyncDataStoreModel.PROPERTY_MAX_PENDING_REQUESTS));
      asyncDataStore.setMaxRunningRequests((Integer) DataHandlerUtil.getPropertyValue(entity, AsyncDataStoreModel.PROPERTY_MAX_RUNNING_REQUESTS));

      // max concurrent requests per user
      asyncDataStore.setMaxParallelFetchRequestsPerUser((Integer) DataHandlerUtil.getPropertyValue(entity, AsyncDataStoreModel.PROPERTY_MAX_PFRPU));
   }

   private void createhttpAsyncDataStore(HttpAsyncDataStoreConf httpAsyncDataStoreConf, Entity entity)
   {
      // ServiceUrl
      httpAsyncDataStoreConf.setServiceUrl((String) DataHandlerUtil.getPropertyValue(entity, HttpAsyncDataStoreModel.PROPERTY_SERVICE_URL));
      // Login
      httpAsyncDataStoreConf.setLogin((String) DataHandlerUtil.getPropertyValue(entity, HttpAsyncDataStoreModel.PROPERTY_LOGIN));
      // Password
      httpAsyncDataStoreConf.setPassword((String) DataHandlerUtil.getPropertyValue(entity, HttpAsyncDataStoreModel.PROPERTY_PASSWORD));
      
      // MaxConcurrentDownloads
      Object maxConcurrentsDownloads = DataHandlerUtil.getPropertyValue(entity, HttpAsyncDataStoreModel.PROPERTY_MAX_CONCURRENTS_DOWNLOADS);
      if(maxConcurrentsDownloads == null)
      {
         httpAsyncDataStoreConf.setMaxConcurrentsDownloads(4);
      }
      else
      {
         httpAsyncDataStoreConf.setMaxConcurrentsDownloads((int)maxConcurrentsDownloads);
      }   
      // Interval
      Object interval = DataHandlerUtil.getPropertyValue(entity, HttpAsyncDataStoreModel.PROPERTY_INTERVAL);
      if (interval == null)
      {
         httpAsyncDataStoreConf.setInterval((long) 600000);
      }
      else
      {
         httpAsyncDataStoreConf.setInterval((long) interval);
      }
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

      if (updatedProperties.isEmpty() && dataStoreConf instanceof AsyncDataStoreConf)
      {
         AsyncDataStoreConf asyncDataStore = (AsyncDataStoreConf) dataStoreConf;
         updateAsyncDataStoreCacheProperty(updatedEntity, asyncDataStore);
      }
      else
      {
         for (Property updatedproperty : updatedProperties)
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
               updateHfsDataStoreEntity(updatedEntity, propertyName, hfsDataStore);
            }
            else if (dataStoreConf instanceof OpenStackDataStoreConf)
            {
               OpenStackDataStoreConf openstackDataStore = (OpenStackDataStoreConf) dataStoreConf;
               updateOpenStackDataStoreEntity(updatedEntity, propertyName, openstackDataStore);
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
                  String repoLocation = (String) DataHandlerUtil.getPropertyValue(updatedEntity,  GmpDataStoreModel.PROPERTY_REPO_LOCATION);
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
                  Configuration configuration = extractConfiguration(updatedEntity, gmpDataStore.getConfiguration() == null ? new Configuration(): gmpDataStore.getConfiguration());

                  gmpDataStore.setConfiguration(configuration);
               }
            }
            else if (dataStoreConf instanceof PdgsDataStoreConf)
            {
               PdgsDataStoreConf pdgsDataStore = (PdgsDataStoreConf) dataStoreConf;

               // common async
               updateAsyncDataStoreProperty(updatedEntity, pdgsDataStore, propertyName);

               // common httpAsync
               updatehttpAsyncDataStoreProperty(updatedEntity, pdgsDataStore, propertyName);

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
            else if (dataStoreConf instanceof LtaDataStoreConf)
            {
               LtaDataStoreConf ltaDataStore = (LtaDataStoreConf) dataStoreConf;

               // common async
               updateAsyncDataStoreProperty(updatedEntity, ltaDataStore, propertyName);

               // common httpAsync
               updatehttpAsyncDataStoreProperty(updatedEntity, ltaDataStore, propertyName);

               // optional order
               if (propertyName.equals(LtaDataStoreModel.PROPERTY_ORDER))
               {
                  Object order = DataHandlerUtil.getPropertyValue(updatedEntity, LtaDataStoreModel.PROPERTY_ORDER);
                  if (order != null)
                  {
                     ltaDataStore.setOrder((boolean) order);
                  }
               }
            }
            else if (dataStoreConf instanceof OndaDataStoreConf)
            {
               OndaDataStoreConf ondaDataStore = (OndaDataStoreConf) dataStoreConf;

               // common async
               updateAsyncDataStoreProperty(updatedEntity, ondaDataStore, propertyName);

               // common httpAsync
               updatehttpAsyncDataStoreProperty(updatedEntity, ondaDataStore, propertyName);

               // optional order
               if (propertyName.equals(OndaDataStoreModel.PROPERTY_ORDER))
               {
                  Object order = DataHandlerUtil.getPropertyValue(updatedEntity,
                        OndaDataStoreModel.PROPERTY_ORDER);
                  if (order != null)
                  {
                     ondaDataStore.setOrder((Boolean) order);
                  }
               }

               // extract objectStorageCredentials
               if (propertyName.equals(OndaDataStoreModel.PROPERTY_OBJECT_STORAGE_CREDENTIAL))
               {
                  ObjectStorageCredentialConf objectStorage = extractObjectStorageCredential(updatedEntity, ondaDataStore.getObjectStorageCredential());

                  ondaDataStore.setObjectStorageCredential(objectStorage);
               }

               // extract ondaScanner
               if (propertyName.equals(OndaDataStoreModel.PROPERTY_ONDA_SCANNER))
               {
                  OndaScannerConf ondaScanner = extractOndaScanner(updatedEntity, ondaDataStore.getOndaScanner());

                  ondaDataStore.setOndaScanner(ondaScanner);
               }
            }
            else
            {
               throw new ODataApplicationException("Unknown DataStore type", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
            }
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

   private void updateAsyncDataStoreCacheProperty(Entity updatedEntity, AsyncDataStoreConf asyncDataStore)
   {
      DataStoreConf dataStoreCache = asyncDataStore.getDataStore();
      Link link = updatedEntity.getNavigationLink(DataStoreModel.NAVIGATION_CACHE);
      if (link != null)
      {
         Entity cacheEntity = link.getInlineEntity();
         String restriction = (String) DataHandlerUtil.getPropertyValue(cacheEntity, DataStoreModel.PROPERTY_RESTRICTION);
         Integer priority = (Integer) DataHandlerUtil.getPropertyValue(cacheEntity, DataStoreModel.PROPERTY_PRIORITY);
         Long maximumSize = (Long) DataHandlerUtil.getPropertyValue(cacheEntity, DataStoreModel.PROPERTY_MAXIMUMSIZE);
         Boolean autoEviction = (Boolean) DataHandlerUtil.getPropertyValue(cacheEntity, DataStoreModel.PROPERTY_AUTOEVICTION);
         String filter = (String) DataHandlerUtil.getPropertyValue(cacheEntity, DataStoreModel.PROPERTY_FILTER);

         List<Property> properties = cacheEntity.getProperties();
         for (Property property : properties)
         {
            String cacheProperty = property.getName();
            // set properties common to all datastores
            if (cacheProperty.equals(DataStoreModel.PROPERTY_RESTRICTION))
            {
               dataStoreCache.setRestriction(DataStoreRestriction.fromValue(restriction));
            }
            if (cacheProperty.equals(DataStoreModel.PROPERTY_PRIORITY))
            {
               dataStoreCache.setPriority(priority);
            }
            if (cacheProperty.equals(DataStoreModel.PROPERTY_MAXIMUMSIZE))
            {
               dataStoreCache.setMaximumSize(maximumSize);
            }
            if (cacheProperty.equals(DataStoreModel.PROPERTY_AUTOEVICTION))
            {
               dataStoreCache.setAutoEviction(autoEviction);
            }
            if (cacheProperty.equals(DataStoreModel.PROPERTY_FILTER))
            {
               dataStoreCache.setFilter(filter);
            }

            if (dataStoreCache instanceof HfsDataStoreConf)
            {
               HfsDataStoreConf hfsCache = (HfsDataStoreConf) dataStoreCache;
               updateHfsDataStoreEntity(cacheEntity, cacheProperty, hfsCache);
            }
            else if (dataStoreCache instanceof OpenStackDataStoreConf)
            {
               OpenStackDataStoreConf openStackCache = (OpenStackDataStoreConf) dataStoreCache;
               updateOpenStackDataStoreEntity(cacheEntity, cacheProperty, openStackCache);
            }
         }
      }
   }
   
   private void updateAsyncDataStoreProperty(Entity updatedEntity, AsyncDataStoreConf asyncDataStore,
         String propertyName)
   {
      //update cache navigation link  
      updateAsyncDataStoreCacheProperty(updatedEntity, asyncDataStore);

      if (propertyName.equals(AsyncDataStoreModel.PROPERTY_IS_MASTER))
      {
         Object isMaster = DataHandlerUtil.getPropertyValue(updatedEntity, AsyncDataStoreModel.PROPERTY_IS_MASTER);
         if (isMaster != null)
         {
            asyncDataStore.setIsMaster((Boolean) isMaster);
         }
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

   private void updatehttpAsyncDataStoreProperty(Entity updatedEntity, HttpAsyncDataStoreConf httpDataStoreConf, String propertyName)
   {
      if (propertyName.equals(HttpAsyncDataStoreModel.PROPERTY_SERVICE_URL))
      {
         String serviceUrl = (String) DataHandlerUtil.getPropertyValue(updatedEntity, HttpAsyncDataStoreModel.PROPERTY_SERVICE_URL);
         httpDataStoreConf.setServiceUrl(serviceUrl);
      }
      else if (propertyName.equals(HttpAsyncDataStoreModel.PROPERTY_LOGIN))
      {
         String login = (String) DataHandlerUtil.getPropertyValue(updatedEntity, HttpAsyncDataStoreModel.PROPERTY_LOGIN);
         httpDataStoreConf.setLogin(login);
      }
      else if (propertyName.equals(HttpAsyncDataStoreModel.PROPERTY_PASSWORD))
      {
         String password = (String) DataHandlerUtil.getPropertyValue(updatedEntity, HttpAsyncDataStoreModel.PROPERTY_PASSWORD);
         httpDataStoreConf.setPassword(password);
      }
      else if (propertyName.equals(HttpAsyncDataStoreModel.PROPERTY_MAX_CONCURRENTS_DOWNLOADS))
      {
         Object maxConcurrentsDownloads = DataHandlerUtil.getPropertyValue(updatedEntity, HttpAsyncDataStoreModel.PROPERTY_MAX_CONCURRENTS_DOWNLOADS);
         if (maxConcurrentsDownloads != null)
         {
            httpDataStoreConf.setMaxConcurrentsDownloads((Integer) maxConcurrentsDownloads);
         }
      }
      else if (propertyName.equals(HttpAsyncDataStoreModel.PROPERTY_INTERVAL))
      {
         Object interval = DataHandlerUtil.getPropertyValue(updatedEntity, HttpAsyncDataStoreModel.PROPERTY_INTERVAL);
         if (interval != null)
         {
            httpDataStoreConf.setInterval((long) interval);
         }
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
   public void updateReference(List<UriParameter> sourceKeyPredicates,
         EdmNavigationProperty navigationProperty, List<UriParameter> targetKeyPredicates, Entity relatedEntity)
         throws ODataApplicationException
   {
      if (navigationProperty.getName().equals(DataStoreModel.NAVIGATION_EVICTION))
      {
         NamedDataStoreConf dataStoreConf = getDataStoreFromService(sourceKeyPredicates);
         String fullQualifiedName = HfsDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString();
         // get datastore
         // case example : http://serviceUrl/odata/v2/DataStores('DSName')/DataStoreCache/Eviction/\$ref
         if (dataStoreConf instanceof AsyncDataStoreConf && relatedEntity != null && relatedEntity.getType() != null && relatedEntity.getType()
                     .equals(fullQualifiedName))
         {
            AsyncDataStoreConf asyncDS = (AsyncDataStoreConf) dataStoreConf;
            String cacheName = (String) relatedEntity.getProperty(HfsDataStoreModel.PROPERTY_NAME).getValue();
            NamedDataStoreConf dataStorecache = DS_SERVICE.getNamedDataStore(cacheName);
            if (dataStorecache == null)
            {
               throw new ODataApplicationException("DataStore Cache not found: " + cacheName,
                     HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
            }
              setDataStoreEvictionName(targetKeyPredicates, dataStorecache, asyncDS);
         }
         else
         {
            // case example : http://serviceUrl/odata/v2/DataStores('DSName')/Eviction/\$ref
            setDataStoreEvictionName(targetKeyPredicates, dataStoreConf, null);
         }
      }
      else
      {
         throw new ODataApplicationException("Cannot update such reference: " + navigationProperty,
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
   }

   private void setDataStoreEvictionName(List<UriParameter> targetKeyPredicates, NamedDataStoreConf dataStoreConf, AsyncDataStoreConf asyncDS)
   {
      // get and set eviction name
      for (UriParameter keyParameter : targetKeyPredicates)
      {
         if (keyParameter.getName().equals(EvictionModel.NAME))
         {
            String evictionName = DataHandlerUtil.trimStringKeyParameter(keyParameter);
            dataStoreConf.setEvictionName(evictionName);
            if(asyncDS != null)
            {
               asyncDS.setDataStore(dataStoreConf);
               DS_SERVICE.update(asyncDS);
            }
            else
            {
               DS_SERVICE.update(dataStoreConf);
            }
         }
      }
   }
 
   @Override
   public void deleteReference(List<UriParameter> sourceKeyPredicates, EdmNavigationProperty navigationProperty, Entity relatedEntity)
         throws ODataApplicationException
   {
      if (navigationProperty.getName().equals(DataStoreModel.NAVIGATION_EVICTION))
      {
         NamedDataStoreConf dataStoreConf = getDataStoreFromService(sourceKeyPredicates);
         String fullQualifiedName = HfsDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString();
 
         if (dataStoreConf instanceof AsyncDataStoreConf && relatedEntity != null && relatedEntity.getType() != null &&
               relatedEntity.getType().equals(fullQualifiedName))
         {
            AsyncDataStoreConf asyncDS = (AsyncDataStoreConf) dataStoreConf;
            String cacheName = (String) relatedEntity.getProperty(HfsDataStoreModel.PROPERTY_NAME).getValue();
            NamedDataStoreConf dataStorecache = DS_SERVICE.getNamedDataStore(cacheName);
            dataStorecache.setEvictionName(null);
            asyncDS.setDataStore(dataStorecache);
            DS_SERVICE.update(asyncDS);
         }
         else
         {
            dataStoreConf.setEvictionName(null);
            DS_SERVICE.update(dataStoreConf);
         }
      }
      else
      {
         throw new ODataApplicationException("Cannot update such reference: " + navigationProperty,
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
   }

   @Override
   public Entity getRelatedEntityData(Entity entity, EdmNavigationProperty edmNavigationProperty)
         throws ODataApplicationException
   {
      String name = (String) entity.getProperty(DataStoreModel.PROPERTY_NAME).getValue();
      NamedDataStoreConf dsConf = DS_SERVICE.getNamedDataStore(name);

      if (dsConf instanceof AsyncDataStoreConf)
      {
         NamedDataStoreConf cacheConf = (NamedDataStoreConf) ((AsyncDataStoreConf) dsConf).getDataStore();
         return toOlingoEntity(cacheConf);
      }
      throw new ODataApplicationException("Only AsyncDataStores can navigate to " + DataStoreModel.NAVIGATION_CACHE,
            HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
   }
   
   private HfsDataStoreConf createHfsDataStoreEntity(Entity entity)
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
      return hfsDataStore;    
   }
   
   private OpenStackDataStoreConf createOpenStackEntity(Entity entity)
   {
      OpenStackDataStoreConf openstackDataStore = new OpenStackDataStoreConf();
      openstackDataStore.setProvider((String) DataHandlerUtil.getPropertyValue(entity, OpenstackDataStoreModel.PROPERTY_PROVIDER));
      openstackDataStore.setIdentity((String) DataHandlerUtil.getPropertyValue(entity, OpenstackDataStoreModel.PROPERTY_IDENTITY));
      openstackDataStore.setCredential((String) DataHandlerUtil.getPropertyValue(entity, OpenstackDataStoreModel.PROPERTY_CREDENTIAL));
      openstackDataStore.setUrl((String) DataHandlerUtil.getPropertyValue(entity, OpenstackDataStoreModel.PROPERTY_URL));
      openstackDataStore.setContainer((String) DataHandlerUtil.getPropertyValue(entity, OpenstackDataStoreModel.PROPERTY_CONTAINER));
      openstackDataStore.setRegion((String) DataHandlerUtil.getPropertyValue(entity, OpenstackDataStoreModel.PROPERTY_REGION));

      return openstackDataStore;
   }

   private void updateHfsDataStoreEntity(Entity updatedEntity, String propertyName, HfsDataStoreConf hfsDataStore)
   {
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

   private void updateOpenStackDataStoreEntity(Entity updatedEntity, String propertyName,
         OpenStackDataStoreConf openstackDataStore)
   {
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
   
   private void setCommonProperties(Entity entity, NamedDataStoreConf dataStore) throws ODataApplicationException
   {
   // Required properties
      String datastoreName = DataHandlerUtil.<String>getRequiredProperty(entity, DataStoreModel.PROPERTY_NAME, String.class);
      
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
   }
}
