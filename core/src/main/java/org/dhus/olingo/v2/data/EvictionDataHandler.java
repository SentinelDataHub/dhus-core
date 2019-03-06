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
import fr.gael.dhus.database.object.config.eviction.Eviction;
import fr.gael.dhus.database.object.config.eviction.EvictionConfiguration.Cron;
import fr.gael.dhus.database.object.config.eviction.EvictionStatusEnum;
import fr.gael.dhus.service.DataStoreService;
import fr.gael.dhus.service.EvictionService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.odata.engine.data.DataHandler;
import fr.gael.odata.engine.data.DataHandlerUtil;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.edm.EdmParameter;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.dhus.olingo.v2.ODataSecurityManager;
import org.dhus.olingo.v2.datamodel.DataStoreModel;
import org.dhus.olingo.v2.datamodel.EvictionModel;
import org.dhus.olingo.v2.datamodel.action.CancelEvictionAction;
import org.dhus.olingo.v2.datamodel.action.QueueEvictionAction;
import org.dhus.olingo.v2.datamodel.action.StopEvictionAction;
import org.dhus.olingo.v2.datamodel.complex.CronComplexType;
import org.dhus.store.datastore.config.DataStoreConf;

/**
 * Provides data for Eviction entities.
 */
public class EvictionDataHandler implements DataHandler
{
   private static final DataStoreService DS_SERVICE =
         ApplicationContextProvider.getBean(DataStoreService.class);

   private static final EvictionService EVICTION_SERVICE =
         ApplicationContextProvider.getBean(EvictionService.class);

   private Entity toOlingoEntity(Eviction eviction)
   {
      Entity evictionEntity = new Entity()
            .addProperty(new Property(
                  null,
                  EvictionModel.NAME,
                  ValueType.PRIMITIVE,
                  eviction.getName()))
            .addProperty(new Property(
                  null,
                  EvictionModel.KEEP_PERIOD,
                  ValueType.PRIMITIVE,
                  eviction.getKeepPeriod()))
            .addProperty(new Property(
                  null,
                  EvictionModel.KEEP_PERIOD_UNIT,
                  ValueType.PRIMITIVE,
                  eviction.getKeepPeriodUnit()))
            .addProperty(new Property(
                  null,
                  EvictionModel.MAX_EVICTED_PRODUCTS,
                  ValueType.PRIMITIVE,
                  eviction.getMaxEvictedProducts()))
            .addProperty(new Property(
                  null,
                  EvictionModel.ORDER_BY,
                  ValueType.PRIMITIVE,
                  eviction.getOrderBy()))
            .addProperty(new Property(
                  null,
                  EvictionModel.FILTER,
                  ValueType.PRIMITIVE,
                  eviction.getFilter()))
            .addProperty(new Property(
                  null,
                  EvictionModel.TARGET_COLLECTION,
                  ValueType.PRIMITIVE,
                  eviction.getTargetCollection()))
            .addProperty(new Property(
                  null,
                  EvictionModel.SOFT_EVICTION,
                  ValueType.PRIMITIVE,
                  eviction.isSoftEviction()))
            .addProperty(new Property(
                  null,
                  EvictionModel.STATUS,
                  ValueType.PRIMITIVE,
                  eviction.getStatus()));

      // cron may be null
      if (eviction.getCron() != null)
      {
         evictionEntity.addProperty(makeCronProperty(eviction.getCron()));
      }

      evictionEntity.setId(DataHandlerUtil.createEntityId(EvictionModel.ENTITY_SET_NAME, eviction.getName()));
      return evictionEntity;
   }

   /**
    * @param cron
    * @return
    */
   private Property makeCronProperty(Cron cron)
   {
      ComplexValue complexValue = new ComplexValue();
      complexValue.getValue().add(new Property(
            null,
            CronComplexType.PROPERTY_ACTIVE,
            ValueType.PRIMITIVE,
            cron.isActive()));
      complexValue.getValue().add(new Property(
            null,
            CronComplexType.PROPERTY_SCHEDULE,
            ValueType.PRIMITIVE,
            cron.getSchedule()));

      return new Property(
            null,
            EvictionModel.CRON,
            ValueType.COMPLEX,
            complexValue);
   }

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      List<Eviction> evictions = EVICTION_SERVICE.getEvictions();

      EntityCollection entityCollection = new EntityCollection();
      for (Eviction eviction: evictions)
      {
         entityCollection.getEntities().add(toOlingoEntity(eviction));
      }
      return entityCollection;
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      for (UriParameter keyParameter: keyParameters)
      {
         if (keyParameter.getName().equals(EvictionModel.NAME))
         {
            String evictionName = DataHandlerUtil.trimStringKeyParameter(keyParameter);

            Eviction eviction = EVICTION_SERVICE.getEviction(evictionName);

            if (eviction == null)
            {
               throw new ODataApplicationException("No eviction exists with this name",
                     HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
            }

            return toOlingoEntity(eviction);
         }
      }
      return null;
   }

   @Override
   public EntityCollection getRelatedEntityCollectionData(Entity sourceEntity) throws ODataApplicationException
   {
      return null;
   }

   @Override
   public Entity getRelatedEntityData(Entity entity) throws ODataApplicationException
   {
      String nameEntity = (String) entity.getProperty(DataStoreModel.PROPERTY_NAME).getValue();

      DataStoreConf dataStore = DS_SERVICE.getNamedDataStore(nameEntity);

      // get eviction name
      String evictionName = dataStore.getEvictionName();
      if(evictionName == null)
      {
         return null; // not found
      }

      // get eviction
      Eviction eviction = EVICTION_SERVICE.getEviction(evictionName);
      if (eviction == null)
      {
         return null; // not found
      }

      return toOlingoEntity(eviction);
   }

   @Override
   public Entity getRelatedEntityData(Entity entity, List<UriParameter> navigationKeyParameters)
         throws ODataApplicationException
   {
      return null;
   }

   @Override
   public Entity createEntityData(Entity entity) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      Object name = DataHandlerUtil.getPropertyValue(entity, EvictionModel.NAME);
      if (name == null)
      {
         throw new ODataApplicationException("Eviction's name is mandatory",
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
      // Check if an eviction with the same name exists
      if (EVICTION_SERVICE.getEviction((String) name) != null)
      {
         throw new ODataApplicationException("An eviction with the same name already exists",
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }

      Eviction eviction = new Eviction();
      eviction.setName((String) name);

      if (DataHandlerUtil.containsProperty(entity, EvictionModel.KEEP_PERIOD))
      {
         eviction.setKeepPeriod(
               (Integer) DataHandlerUtil.getPropertyValue(entity, EvictionModel.KEEP_PERIOD));
      }
      else
      {
         eviction.setKeepPeriod(
               (Integer) EvictionModel.getDefaultValue(EvictionModel.KEEP_PERIOD));
      }
      if (DataHandlerUtil.containsProperty(entity, EvictionModel.KEEP_PERIOD_UNIT))
      {
         eviction.setKeepPeriodUnit(
               (String) DataHandlerUtil.getPropertyValue(entity, EvictionModel.KEEP_PERIOD_UNIT));
      }
      else
      {
         eviction.setKeepPeriodUnit(
               (String) EvictionModel.getDefaultValue(EvictionModel.KEEP_PERIOD_UNIT));
      }
      if (DataHandlerUtil.containsProperty(entity, EvictionModel.MAX_EVICTED_PRODUCTS))
      {
         eviction.setMaxEvictedProducts(
               (Integer) DataHandlerUtil.getPropertyValue(entity, EvictionModel.MAX_EVICTED_PRODUCTS));
      }
      else
      {
         eviction.setMaxEvictedProducts(
               (Integer) EvictionModel.getDefaultValue(EvictionModel.MAX_EVICTED_PRODUCTS));
      }
      if (DataHandlerUtil.containsProperty(entity, EvictionModel.SOFT_EVICTION))
      {
         eviction.setSoftEviction(
               (Boolean) DataHandlerUtil.getPropertyValue(entity, EvictionModel.SOFT_EVICTION));
      }
      else
      {
         eviction.setSoftEviction(
               (Boolean) EvictionModel.getDefaultValue(EvictionModel.SOFT_EVICTION));
      }
      if (DataHandlerUtil.containsProperty(entity, EvictionModel.FILTER))
      {
         try
         {
            eviction.checkAndSetFilter((String) DataHandlerUtil.getPropertyValue(entity, EvictionModel.FILTER));
         }
         catch (ODataException ex)
         {
            throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
         }
      }
      else
      {
         eviction.setFilter((String) EvictionModel.getDefaultValue(EvictionModel.FILTER));
      }
      if (DataHandlerUtil.containsProperty(entity, EvictionModel.ORDER_BY))
      {
         try
         {
            eviction.checkAndSetOrderBy((String) DataHandlerUtil.getPropertyValue(entity, EvictionModel.ORDER_BY));
         }
         catch (ODataException ex)
         {
            throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
         }
      }
      else
      {
         eviction.setOrderBy((String) EvictionModel.getDefaultValue(EvictionModel.ORDER_BY));
      }

      eviction.setStatus(EvictionStatusEnum.fromValue(
            (String) EvictionModel.getDefaultValue(EvictionModel.STATUS)));

      if (DataHandlerUtil.containsProperty(entity, EvictionModel.CRON))
      {
         eviction.setCron(extractCron(entity.getProperty(EvictionModel.CRON), new Cron()));
      }
      // cron is nullable
      if (DataHandlerUtil.containsProperty(entity, EvictionModel.TARGET_COLLECTION))
      {
         eviction.setTargetCollection((String) DataHandlerUtil.getPropertyValue(entity, EvictionModel.TARGET_COLLECTION));
      }
      // targetCollection is nullable

      EVICTION_SERVICE.create(eviction);

      return toOlingoEntity(eviction);
   }

   private Cron extractCron(Property cronProperty, Cron cron)
   {
      if (cronProperty != null && cronProperty.getValue() != null)
      {
         List<Property> cronProperties = ((ComplexValue) cronProperty.getValue()).getValue();
         for (Property property: cronProperties)
         {
            switch (property.getName())
            {
               case CronComplexType.PROPERTY_ACTIVE:
                  cron.setActive((Boolean) property.getValue());
                  break;

               case CronComplexType.PROPERTY_SCHEDULE:
                  cron.setSchedule((String) property.getValue());
                  break;
            }
         }
         return cron;
      }
      return null;
   }

   private Eviction getEvictionFromParameters(List<UriParameter> keyParameters)
   {
      for (UriParameter keyParameter: keyParameters)
      {
         if (EvictionModel.NAME.equals(keyParameter.getName()))
         {
            String name = DataHandlerUtil.trimStringKeyParameter(keyParameter);
            return EVICTION_SERVICE.getEviction(name);
         }
      }
      return null;
   }

   @Override
   public void updateEntityData(List<UriParameter> keyParameters, Entity updatedEntity, HttpMethod httpMethod)
         throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      Eviction eviction = getEvictionFromParameters(keyParameters);
      if (eviction == null)
      {
         throw new ODataApplicationException("No eviction exists with this name",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }

      // name is read-only
      Integer keepPeriod = (Integer) DataHandlerUtil.getPropertyValue(updatedEntity, EvictionModel.KEEP_PERIOD);
      String keepPeriodUnit = (String) DataHandlerUtil.getPropertyValue(updatedEntity, EvictionModel.KEEP_PERIOD_UNIT);
      Integer maxEvictedProduct = (Integer) DataHandlerUtil.getPropertyValue(updatedEntity, EvictionModel.MAX_EVICTED_PRODUCTS);
      String filter = (String) DataHandlerUtil.getPropertyValue(updatedEntity, EvictionModel.FILTER);
      String orderBy = (String) DataHandlerUtil.getPropertyValue(updatedEntity, EvictionModel.ORDER_BY);
      String targetCollection = (String) DataHandlerUtil.getPropertyValue(updatedEntity, EvictionModel.TARGET_COLLECTION);
      Boolean softEviction = (Boolean) DataHandlerUtil.getPropertyValue(updatedEntity, EvictionModel.SOFT_EVICTION);

      EvictionStatusEnum previousStatus = eviction.getStatus();

      // get updated properties
      List<Property> updatedProperties = updatedEntity.getProperties();

      for (Property updatedproperty: updatedProperties)
      {
         String propertyName = updatedproperty.getName();

         if (propertyName.equals(EvictionModel.KEEP_PERIOD))
         {
            eviction.setKeepPeriod(keepPeriod);
         }
         if (propertyName.equals(EvictionModel.KEEP_PERIOD_UNIT))
         {
            eviction.setKeepPeriodUnit(keepPeriodUnit);
         }
         if (propertyName.equals(EvictionModel.MAX_EVICTED_PRODUCTS))
         {
            eviction.setMaxEvictedProducts(maxEvictedProduct);
         }
         if (propertyName.equals(EvictionModel.FILTER))
         {
            try
            {
               eviction.checkAndSetFilter(filter);
            }
            catch (ODataException ex)
            {
               throw new ODataApplicationException(ex.getMessage(),
                     HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
            }
         }
         if (propertyName.equals(EvictionModel.ORDER_BY))
         {
            eviction.setOrderBy(orderBy);
         }
         if (propertyName.equals(EvictionModel.TARGET_COLLECTION))
         {
            eviction.setTargetCollection(targetCollection);
         }
         if (propertyName.equals(EvictionModel.SOFT_EVICTION))
         {
            eviction.setSoftEviction(softEviction);
         }
         if (propertyName.equals(EvictionModel.CRON))
         {
            if (eviction.getCron() == null)
            {
               eviction.setCron(extractCron(updatedproperty, new Cron()));
            }
            else
            {
               extractCron(updatedproperty, eviction.getCron());
            }
         }
      }
      EVICTION_SERVICE.updateEviction(eviction, previousStatus);
   }

   private void checkEvictionExists(String name) throws ODataApplicationException
   {
      Eviction eviction = EVICTION_SERVICE.getEviction(name);
      if (eviction == null)
      {
         throw new ODataApplicationException("Eviction does not exist",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }
   }

   @Override
   public Property performBoundActionPrimitive(List<UriParameter> keyPredicates, EdmAction action,
         Map<String, Parameter> parameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      if (action.getFullQualifiedName().equals(QueueEvictionAction.ACTION_QUEUE_EVICTION_FQN))
      {
         String evictionName = DataHandlerUtil.trimStringKeyParameter(keyPredicates.get(0));
         checkEvictionExists(evictionName);

         String targetDataStore = (String) parameters.get(QueueEvictionAction.PARAMETER_TARGET_DATASTORE).asPrimitive();
         Boolean safeMode = (Boolean) parameters.get(QueueEvictionAction.PARAMETER_SAFE_MODE).asPrimitive();

         EVICTION_SERVICE.doEvict(evictionName, targetDataStore, safeMode == null ? false : safeMode);

         return new Property(null, QueueEvictionAction.ACTION_QUEUE_EVICTION, ValueType.PRIMITIVE,
               "Eviction '" + evictionName + "' has been successfully queued");
      }
      else if (action.getFullQualifiedName().equals(CancelEvictionAction.ACTION_CANCEL_EVICTION_FQN))
      {
         String evictionName = DataHandlerUtil.trimStringKeyParameter(keyPredicates.get(0));
         checkEvictionExists(evictionName);

         if (EVICTION_SERVICE.cancelEviction(evictionName))
         {
            return new Property(null, CancelEvictionAction.ACTION_CANCEL_EVICTION, ValueType.PRIMITIVE,
                  "Eviction '" + evictionName + "' has been successfully canceled");
         }
         return new Property(null, CancelEvictionAction.ACTION_CANCEL_EVICTION, ValueType.PRIMITIVE,
               "Eviction '" + evictionName + "' cannot be canceled");
      }
      else if (action.getFullQualifiedName().equals(StopEvictionAction.ACTION_STOP_EVICTION_FQN))
      {
         EVICTION_SERVICE.stopCurrentEviction();
         return new Property(null, StopEvictionAction.ACTION_STOP_EVICTION, ValueType.PRIMITIVE,
               "Currently running Eviction successfully stopped");
      }
      else
      {
         throw new ODataApplicationException("Action not found",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }
   }

   @Override
   public void deleteEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      Eviction eviction = getEvictionFromParameters(keyParameters);
      if (eviction == null)
      {
         throw new ODataApplicationException("No eviction exists with this name",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }
      EVICTION_SERVICE.delete(eviction);
   }
}
