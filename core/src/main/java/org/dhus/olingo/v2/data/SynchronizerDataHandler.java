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
package org.dhus.olingo.v2.data;

import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerConfiguration;
import fr.gael.dhus.database.object.config.synchronizer.UserSynchronizer;
import fr.gael.dhus.service.CollectionService;
import fr.gael.dhus.service.ISynchronizerService;
import fr.gael.dhus.service.exception.InvokeSynchronizerException;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;
import fr.gael.dhus.util.XmlProvider;
import fr.gael.odata.engine.data.DataHandler;
import fr.gael.odata.engine.data.DataHandlerUtil;

import static fr.gael.odata.engine.data.DataHandlerUtil.containsProperty;
import static org.dhus.olingo.v2.datamodel.ScannerModel.PROPERTY_ID;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.time.LocalDateTime;
import java.sql.Timestamp;

import org.apache.commons.lang.StringUtils;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.dhus.api.olingo.v2.EntityProducer;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.dhus.olingo.v2.ODataSecurityManager;
import org.dhus.olingo.v2.datamodel.CollectionModel;
import org.dhus.olingo.v2.datamodel.ProductSynchronizerModel;
import org.dhus.olingo.v2.datamodel.SynchronizerModel;
import org.dhus.olingo.v2.datamodel.complex.CronComplexType;
import org.dhus.olingo.v2.entity.TypeStore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

public class SynchronizerDataHandler extends AbstractSynchronizerDataHandler implements DataHandler
{
   private static final ConfigurationManager CONFIG_MANAGER = ApplicationContextProvider.getBean(ConfigurationManager.class);
   protected static final ISynchronizerService SYNCHRONIZER_SERVICE = ApplicationContextProvider.getBean(ISynchronizerService.class);
   private static final CollectionService COLLECTION_SVC = ApplicationContextProvider.getBean(CollectionService.class);
  
  
   private static final Logger LOGGER = LogManager.getLogger();
   
   private static final java.util.regex.Pattern GEO_FILTER_PATTERN =
         Pattern.compile("(disjoint|within|contains|intersects) (.+)");

   private final TypeStore typeStore;

   public SynchronizerDataHandler(TypeStore typeStore)
   {
      this.typeStore = typeStore;
   }

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      if(!ODataSecurityManager.hasPermission(Role.FEDERATION_USER))
      {
         ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);
      }

      final EntityCollection entities = new EntityCollection();
      SYNCHRONIZER_SERVICE.getSynchronizerConfs().forEachRemaining(
            (SynchronizerConfiguration syncConf) -> checkAndAddSynchronizer(entities, syncConf)
      );
      return entities;
   }
   
   private void checkAndAddSynchronizer(EntityCollection entities, SynchronizerConfiguration syncConf)
   {
      if (CONFIG_MANAGER.isGDPREnabled() && syncConf instanceof UserSynchronizer)
      {
         return;
      }
      entities.getEntities().add(toOlingoEntity(syncConf));
   }

   protected Entity toOlingoEntity(SynchronizerConfiguration syncConf)
   {
      Entity res;
      EntityProducer<SynchronizerConfiguration> defaultEntityProducer =
            typeStore.get(SynchronizerConfiguration.class).<SynchronizerConfiguration>getEntityProducer();
      TypeStore.Node entityProducerNode = typeStore.get(syncConf.getClass());
      if (entityProducerNode != null)
      {
         res = entityProducerNode.getEntityProducer().toOlingoEntity(syncConf);
      }
      else
      {
         res = defaultEntityProducer.toOlingoEntity(syncConf);
      }
      res.setId(DataHandlerUtil.createEntityId(SynchronizerModel.ABSTRACT_ENTITY_SET_NAME, String.valueOf(syncConf.getId())));
      return res;
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      if(!ODataSecurityManager.hasPermission(Role.FEDERATION_USER))
      {
         ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);
      }

      Integer kp = Integer.valueOf(keyParameters.get(0).getText());
      SynchronizerConfiguration config = SYNCHRONIZER_SERVICE.getSynchronizerConfById(kp, SynchronizerConfiguration.class);
      if (CONFIG_MANAGER.isGDPREnabled() && config instanceof UserSynchronizer)
      {
         return null;
      }
      return toOlingoEntity(config);
   }
   
   private long getSyncIdFromParam(List<UriParameter> keyParameters)
   {
      return keyParameters.stream()
            .filter(param -> param.getName().equalsIgnoreCase(PROPERTY_ID))
            .map(UriParameter::getText)
            .mapToLong(Long::parseLong)
            .findFirst()
            .orElse(-1);
   }
   
   private SynchronizerConfiguration getSynchronizer(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      long id = getSyncIdFromParam(keyParameters);

      if (id < 0)
      {
         throw new ODataApplicationException("No Id found in request",
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }

      SynchronizerConfiguration sync = SYNCHRONIZER_SERVICE.getSynchronizerConfById(id, SynchronizerConfiguration.class);
      if (null == sync)
      {
         throw new ODataApplicationException("No Synchronizer exists with this id",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }

      return sync;
   }
   
   @Override
   public void updateEntityData(List<UriParameter> keyParameters, Entity updatedEntity, HttpMethod httpMethod)
         throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      SynchronizerConfiguration sync = getSynchronizer(keyParameters);
     
      if (sync == null)
      {
         throw new ODataApplicationException(
               "Cannot update synchronizer: targeted synchronizer does not exist",
               HttpStatusCode.BAD_REQUEST.getStatusCode(),
               Locale.ENGLISH);
      }

      boolean currentState = sync.isActive();
      //Deactivate the synchronizer before updating it
      SYNCHRONIZER_SERVICE.deactivateSynchronizer(sync.getId());
      sync.setActive(currentState);

      updateSynchronizerProperties(updatedEntity, sync);
      
      try
      {
         SYNCHRONIZER_SERVICE.saveSynchronizerConf(sync);
      }
      catch (InvokeSynchronizerException e)
      {
         throw new ODataApplicationException("Exception when updating synchronizer: " + e.getMessage(),
               HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
               Locale.ENGLISH);
      }
   }

   @Override
   public void deleteEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);
      SynchronizerConfiguration sync = getSynchronizer(keyParameters);
      
      if (sync == null)
      {
         throw new ODataApplicationException(
               "Cannot update synchronizer: targeted synchronizer does not exist",
               HttpStatusCode.BAD_REQUEST.getStatusCode(),
               Locale.ENGLISH);
      }
      SYNCHRONIZER_SERVICE.deactivateSynchronizer(sync.getId());
      SYNCHRONIZER_SERVICE.removeSynchronizer(sync.getId());
   }

   @Override
   protected void updateSpecificProperties(Entity updatedEntity, SynchronizerConfiguration sync)
   {
      if (sync instanceof ProductSynchronizer)
      {
         ProductSynchronizer prodSync = (ProductSynchronizer) sync;
         
         if (containsProperty(updatedEntity, ProductSynchronizerModel.PROPERTY_REMOTE_INCOMING))
         {
            prodSync.setRemoteIncoming((String) DataHandlerUtil.getPropertyValue(updatedEntity,
                  ProductSynchronizerModel.PROPERTY_REMOTE_INCOMING));
         }     
         
         if (containsProperty(updatedEntity, ProductSynchronizerModel.PROPERTY_COPY_PRODUCT))
         {
            prodSync.setCopyProduct((Boolean) DataHandlerUtil.getPropertyValue(updatedEntity,
                  ProductSynchronizerModel.PROPERTY_COPY_PRODUCT));
         }

         if (containsProperty(updatedEntity, ProductSynchronizerModel.PROPERTY_SYNC_OFFLINE))
         {
            prodSync.setSyncOfflineProducts((Boolean) DataHandlerUtil.getPropertyValue(updatedEntity,
                  ProductSynchronizerModel.PROPERTY_SYNC_OFFLINE));
         }

         if (containsProperty(updatedEntity, ProductSynchronizerModel.PROPERTY_SKIP_ON_ERROR))
         {
            prodSync.setSkipOnError((Boolean) DataHandlerUtil.getPropertyValue(updatedEntity,
                  ProductSynchronizerModel.PROPERTY_SKIP_ON_ERROR));
         }
         
         if (containsProperty(updatedEntity, ProductSynchronizerModel.PROPERTY_FILTER_GEO))
         {
            updateGeoFilter((String) DataHandlerUtil.getPropertyValue(updatedEntity, 
                  ProductSynchronizerModel.PROPERTY_FILTER_GEO), prodSync);
         }
         
         if (containsProperty(updatedEntity, ProductSynchronizerModel.PROPERTY_FILTER_PARAM))
         {
            prodSync.setFilterParam((String) DataHandlerUtil.getPropertyValue(updatedEntity, 
                  ProductSynchronizerModel.PROPERTY_FILTER_PARAM));
         }
         
         if (containsProperty(updatedEntity, ProductSynchronizerModel.PROPERTY_SOURCE_COLLECTION))
         {
            prodSync.setSourceCollection((String) DataHandlerUtil.getPropertyValue(updatedEntity, 
                  ProductSynchronizerModel.PROPERTY_SOURCE_COLLECTION));
         }
         
         if (containsProperty(updatedEntity, ProductSynchronizerModel.PROPERTY_LAST_CREATION_DATE))
         {
            Timestamp time = (Timestamp) DataHandlerUtil.getPropertyValue
                  (updatedEntity, ProductSynchronizerModel.PROPERTY_LAST_CREATION_DATE);
              
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
                     
               prodSync.setLastCreated(cal);
            }
            catch (DatatypeConfigurationException e)
            {
               LOGGER.error("Error while updating LastCreationDate.", e);
            }
         }
         
         if (containsProperty(updatedEntity, ProductSynchronizerModel.PROPERTY_RETRIES_SKIPPED_PRODUCTS))         
         {
            Long val = (Long) DataHandlerUtil.getPropertyValue(updatedEntity,
                  ProductSynchronizerModel.PROPERTY_RETRIES_SKIPPED_PRODUCTS);
            prodSync.setRetriesForSkippedProducts(val.intValue());
         }
         
         if (containsProperty(updatedEntity, ProductSynchronizerModel.PROPERTY_TIMEOUT_SKIPPED_PRODUCTS))
         {
            prodSync.setTimeoutSkippedProducts((Long) DataHandlerUtil.getPropertyValue(updatedEntity, 
                  ProductSynchronizerModel.PROPERTY_TIMEOUT_SKIPPED_PRODUCTS));
         }
      }
   }
   
   @Override
   public Entity createEntityData(Entity entity) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);
      SynchronizerConfiguration sync = null;
      if (entity.getType().equalsIgnoreCase(ProductSynchronizerModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         sync = buildProductSynchronizerFromEntity(entity);
      }
      return toOlingoEntity(sync);
   }
   
   private SynchronizerConfiguration buildProductSynchronizerFromEntity(Entity entity) 
         throws ODataApplicationException
   {
      ProductSynchronizer psync = new ProductSynchronizer();

      String schedule = (String) DataHandlerUtil.getPropertyValueFromComplexProperty(entity,
            ProductSynchronizerModel.PROPERTY_CRON, CronComplexType.PROPERTY_SCHEDULE);

      String label = (String) DataHandlerUtil.getPropertyValue(entity, 
            ProductSynchronizerModel.PROPERTY_LABEL);
      try
      {
         // create synchronizer (base)
         psync = SYNCHRONIZER_SERVICE.createSynchronizer(label, schedule, ProductSynchronizer.class);

         updateSynchronizerProperties(entity, psync);

         SYNCHRONIZER_SERVICE.saveSynchronizerConf(psync);
      }
      catch (ParseException | ReflectiveOperationException | InvokeSynchronizerException e)
      {
         if (psync != null)
         {
            SYNCHRONIZER_SERVICE.removeSynchronizer(psync.getId());
         }

         throw new ODataApplicationException("Cannot create synchronizer",
               HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
      }
      catch (IllegalArgumentException e)
      {
         throw new ODataApplicationException("Cannot create synchronizer: " + e.getMessage(),
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
      return psync;
   }

   /**
    * Extracts geometry operation and shape.
    *
    * @param geoFilter geometry filter
    * @param syncConf  synchronizer configuration to update
    * @throws IllegalArgumentException if the <em>geoFilter</em> is not valid
    */
   private void updateGeoFilter(String geoFilter, ProductSynchronizer syncConf)
   {
      if (geoFilter == null || geoFilter.isEmpty())
      {
         syncConf.setGeofilterOp(null);
         syncConf.setGeofilterShape(null);
         return;
      }
      String baseErrorMessage = "Invalid parameter " + ProductSynchronizerModel.PROPERTY_FILTER_GEO;
      Matcher matcher = GEO_FILTER_PATTERN.matcher(geoFilter);
      if (!matcher.matches())
      {
         throw new IllegalArgumentException(baseErrorMessage);
      }

      String operation = matcher.group(1);
      String shape = matcher.group(2);
      WKTReader wktReader = new WKTReader();

      try
      {
         Geometry geometry = wktReader.read(shape);
         if (!geometry.isValid())
         {
            throw new IllegalArgumentException(baseErrorMessage + ": invalid shape");
         }
      }
      catch (com.vividsolutions.jts.io.ParseException e)
      {
         throw new IllegalArgumentException(baseErrorMessage + ": cannot parse shape");
      }

      syncConf.setGeofilterOp(operation);
      syncConf.setGeofilterShape(shape);
   }
   
   @Override
   public void createReferenceInCollection(List<UriParameter> sourceKeyPredicates,
         EdmNavigationProperty navigationProperty, List<UriParameter> targetKeyPredicates)
         throws ODataApplicationException
   {
      if (navigationProperty.getName().equals(ProductSynchronizerModel.NAVIGATION_TARGET_COLLECTION))
      {
         // get collection name
         String collectionName =
            DataHandlerUtil.getSingleStringKeyParameterValue(targetKeyPredicates, CollectionModel.PROPERTY_NAME);
         
         SynchronizerConfiguration sync = getSynchronizer(sourceKeyPredicates);

         if (COLLECTION_SVC.getCollectionByName(collectionName) == null)
         {
            throw new ODataApplicationException("Target collection does not exist: " + collectionName,
                  HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
         }
         ProductSynchronizer pSync = (ProductSynchronizer) sync;
         if (pSync.getTargetCollection() == null)
         {
            pSync.setTargetCollection(collectionName);
         }

         try
         {
            SYNCHRONIZER_SERVICE.saveSynchronizerConf(pSync);
         }
         catch (InvokeSynchronizerException e)
         {
            throw new ODataApplicationException("Exception when updating synchronizer: " + e.getMessage(),
                  HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
         }
      }
      else
      {
         throw new ODataApplicationException("Cannot update such reference: " + navigationProperty,
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
   }
   
   @Override
   public void deleteReference(List<UriParameter> sourceKeyPredicates,
         EdmNavigationProperty navigationProperty, List<UriParameter> targetKeyPredicates) throws ODataApplicationException
   {
      if (navigationProperty.getName().equals(ProductSynchronizerModel.NAVIGATION_TARGET_COLLECTION))
      {
         // get scanner configuration
         SynchronizerConfiguration sync = getSynchronizer(sourceKeyPredicates);

         // get collection name
         String collectionName = DataHandlerUtil.getSingleStringKeyParameterValue(
               targetKeyPredicates, CollectionModel.PROPERTY_NAME);

         // remove existing collection names
         ProductSynchronizer pSync = (ProductSynchronizer) sync;
         String targetCollection = pSync.getTargetCollection();

         boolean removed = false;
         if (targetCollection != null)
         {
            removed = collectionName.equalsIgnoreCase(targetCollection);
            targetCollection = null;
         }

         // could not remove collection because it was not listed
         if (!removed)
         {
            throw new ODataApplicationException("Unknown Collection name: " + collectionName,
                  HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
         }
      }
      else
      {
         throw new ODataApplicationException("Cannot update such reference: " + navigationProperty,
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
   }
}
