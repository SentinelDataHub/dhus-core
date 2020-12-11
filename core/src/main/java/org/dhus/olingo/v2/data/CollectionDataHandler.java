/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018 GAEL Systems
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

import fr.gael.dhus.database.object.Collection;
import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.config.scanner.ScannerConfiguration.Collections;
import fr.gael.dhus.datastore.scanner.Scanner;
import fr.gael.dhus.service.CollectionService;
import fr.gael.dhus.service.exception.CollectionNameExistingException;
import fr.gael.dhus.service.exception.RequiredFieldMissingException;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.odata.engine.data.DataHandler;
import fr.gael.odata.engine.data.DataHandlerUtil;

import java.util.List;
import java.util.Locale;
import java.util.Set;

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
import org.dhus.olingo.v2.datamodel.CollectionModel;
import org.dhus.olingo.v2.datamodel.ScannerModel;
import org.dhus.scanner.service.ScannerServiceImpl;

public class CollectionDataHandler implements DataHandler
{
   private static final CollectionService COLLECTION_SVC =
         ApplicationContextProvider.getBean(CollectionService.class);
   private static final ScannerServiceImpl SCANNER_SERVICE =
         ApplicationContextProvider.getBean(ScannerServiceImpl.class);

   private Collection getCollectionFromParameters(List<UriParameter> keyParameters)
   {
      Collection collection = null;
      for (UriParameter parameter: keyParameters)
      {
         if (parameter.getName().equals("Name"))
         {
            String collectionName = DataHandlerUtil.trimStringKeyParameter(parameter);
            collection = COLLECTION_SVC.getCollectionByName(collectionName);
            break;
         }
      }
      return collection;
   }

   private Entity toOlingoEntity(Collection collection)
   {
      Entity entity = null;
      if (collection != null)
      {
         entity = new Entity();
         entity.setId(DataHandlerUtil.createEntityId(
               CollectionModel.ENTITY_SET_NAME, collection.getName()));

         entity.addProperty(new Property(null,
               CollectionModel.PROPERTY_NAME,
               ValueType.PRIMITIVE,
               collection.getName()));
         
         entity.addProperty(new Property(null,
               CollectionModel.PROPERTY_UUID,
               ValueType.PRIMITIVE,
               collection.getUUID()));

         entity.addProperty(new Property(
               null,
               CollectionModel.PROPERTY_DESCRIPTION,
               ValueType.PRIMITIVE,
               collection.getDescription()));
      }
      return entity;
   }

   @Override
   public EntityCollection getEntityCollectionData()
   {
      EntityCollection entities = new EntityCollection();
      Set<Collection> collections = COLLECTION_SVC.getCollections();
      collections.forEach(collection -> entities.getEntities().add(toOlingoEntity(collection)));
      return entities;
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      return toOlingoEntity(getCollectionFromParameters(keyParameters));
   }

   @Override
   public EntityCollection getRelatedEntityCollectionData(Entity sourceEntity,
         EdmNavigationProperty edmNavigationProperty) throws ODataApplicationException
   {
      if (ScannerModel.isScannerSubType(sourceEntity.getType()))
      {
         Scanner scanner = SCANNER_SERVICE.getScanner((Long) sourceEntity.getProperty(ScannerModel.PROPERTY_ID).getValue());
         EntityCollection entities = new EntityCollection();
         Collections collections = scanner.getConfig().getCollections();
         if (collections != null)
         {
            collections.getCollection().forEach(collectionName
                  -> entities.getEntities().add(toOlingoEntity(COLLECTION_SVC.getCollectionByName(collectionName))));
         }
         return entities;
      }
      else
      {
         return null;
      }
   }

   @Override
   public Entity getRelatedEntityData(Entity sourceEntity,
         List<UriParameter> navigationKeyParameters, EdmNavigationProperty edmNavigationProperty)
         throws ODataApplicationException
   {
      if (ScannerModel.isScannerSubType(sourceEntity.getType()))
      {
         Scanner scanner = SCANNER_SERVICE.getScanner((Long) sourceEntity.getProperty(ScannerModel.PROPERTY_ID).getValue());
         String collectionName = DataHandlerUtil.getSingleStringKeyParameterValue(navigationKeyParameters, CollectionModel.PROPERTY_NAME);
         Collections collections = scanner.getConfig().getCollections();
         if (collections != null)
         {
            return collections.getCollection()
                  .stream()
                  .filter(collName -> collName.equals(collectionName))
                  .map(collName -> toOlingoEntity(COLLECTION_SVC.getCollectionByName(collName)))
                  .findFirst().orElse(null);
         }
         else
         {
            return null;
         }
      }
      else
      {
         return null;
      }
   }

   @Override
   public void updateEntityData(List<UriParameter> keyParameters, Entity updatedEntity,
         HttpMethod httpMethod) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.DATA_MANAGER);

      Collection collection = getCollectionFromParameters(keyParameters);
      if (collection != null)
      {
         if (DataHandlerUtil.containsProperty(updatedEntity, CollectionModel.PROPERTY_NAME))
         {
            collection.setName((String) DataHandlerUtil.getPropertyValue(
                  updatedEntity, CollectionModel.PROPERTY_NAME));
         }

         if (DataHandlerUtil.containsProperty(updatedEntity, CollectionModel.PROPERTY_DESCRIPTION))
         {
            collection.setDescription((String) DataHandlerUtil.getPropertyValue(
                  updatedEntity, CollectionModel.PROPERTY_DESCRIPTION));
         }

         try
         {
            COLLECTION_SVC.updateCollection(collection);
         }
         catch (RequiredFieldMissingException e)
         {
            // should not occurred
            throw new ODataApplicationException("Cannot update collection: " + e.getMessage(),
                  HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
         }
      }
      else
      {
         throw new ODataApplicationException("The target collection does not exist",
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
   }

   @Override
   public void deleteEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.DATA_MANAGER);

      Collection collection = getCollectionFromParameters(keyParameters);
      if (collection != null)
      {
         COLLECTION_SVC.deleteCollection(collection.getUUID());
      }
   }

   @Override
   public Entity createEntityData(Entity entity) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.DATA_MANAGER);

      if (DataHandlerUtil.containsProperty(entity, CollectionModel.PROPERTY_NAME))
      {
         Collection collection = new Collection();
         collection.setName((String) DataHandlerUtil.getPropertyValue(
               entity, CollectionModel.PROPERTY_NAME));
         collection.setDescription((String) DataHandlerUtil.getPropertyValue(
               entity, CollectionModel.PROPERTY_DESCRIPTION));

         try
         {
            collection = COLLECTION_SVC.createCollection(collection);
         }
         catch (RequiredFieldMissingException | CollectionNameExistingException e)
         {
            throw new ODataApplicationException("Cannot create collection: " + e.getMessage(),
                  HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
         }

         return toOlingoEntity(collection);
      }
      throw new ODataApplicationException("Cannot create collection without Name property",
            HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
   }
}
