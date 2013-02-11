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

import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.config.source.Source;
import fr.gael.dhus.service.ISourceService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.odata.engine.data.DataHandler;
import fr.gael.odata.engine.data.DataHandlerUtil;

import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.dhus.olingo.v2.ODataSecurityManager;
import org.dhus.olingo.v2.datamodel.SourceModel;

public class SourceDataHandler implements DataHandler
{
   private static final ISourceService SOURCE_SERVICE =
         ApplicationContextProvider.getBean(ISourceService.class);
   private static final String HIDED_PASSWORD = "******";

   /**
    * Retrieves a {@link Source}.
    *
    * @param keyParameters key parameters containing the source id
    * @return a source, or null if it not found
    */
   private Source getSourceFromParameters(List<UriParameter> keyParameters)
   {
      for (UriParameter parameter: keyParameters)
      {
         if (parameter.getName().equals(SourceModel.PROPERTY_ID))
         {
            return SOURCE_SERVICE.getSource(Integer.valueOf(parameter.getText()));
         }
      }
      return null;
   }

   /**
    * Generates a {@link Entity} from a {@link Source}.
    *
    * @param source source to adapt to entity
    * @return a new entity representing the given source
    */
   private Entity toOlingoEntity(Source source)
   {
      Entity entity = null;
      if (source != null)
      {
         entity = new Entity();

         entity.setId(DataHandlerUtil.createEntityId(
               SourceModel.ENTITY_SET_NAME, String.valueOf(source.getId())));

         entity.addProperty(new Property(
               null,
               SourceModel.PROPERTY_ID,
               ValueType.PRIMITIVE,
               source.getId()));

         entity.addProperty(new Property(
               null,
               SourceModel.PROPERTY_URL,
               ValueType.PRIMITIVE,
               source.getUrl()));

         entity.addProperty(new Property(
               null,
               SourceModel.PROPERTY_USERNAME,
               ValueType.PRIMITIVE,
               source.getUsername()));

         entity.addProperty(new Property(
               null,
               SourceModel.PROPERTY_PASSWORD,
               ValueType.PRIMITIVE,
               HIDED_PASSWORD));

         entity.addProperty(new Property(
               null,
               SourceModel.PROPERTY_ACTIVE_DOWNLOAD,
               ValueType.PRIMITIVE,
               source.concurrentDownload()));

         entity.addProperty(new Property(
               null,
               SourceModel.PROPERTY_MAX_DOWNLOAD,
               ValueType.PRIMITIVE,
               source.getMaxDownload()));

         entity.addProperty(new Property(
               null,
               SourceModel.PROPERTY_BANDWIDTH,
               ValueType.PRIMITIVE,
               source.getBandwidth()));
      }
      return entity;
   }

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      List<Source> sources = SOURCE_SERVICE.list();
      EntityCollection entities = new EntityCollection();
      sources.forEach(source -> entities.getEntities().add(toOlingoEntity(source)));
      return entities;
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      return toOlingoEntity(getSourceFromParameters(keyParameters));
   }

   @Override
   public void updateEntityData(List<UriParameter> keyParameters, Entity updatedEntity,
         HttpMethod httpMethod) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      Source source = getSourceFromParameters(keyParameters);
      if (source == null)
      {
         throw new ODataApplicationException("No source exists width this id",
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }

      for (Property property: updatedEntity.getProperties())
      {
         String propertyName = property.getName();
         Object propertyValue = property.getValue();

         if (propertyName.equals(SourceModel.PROPERTY_URL))
         {
            source.setUrl((String) propertyValue);
         }
         if (propertyName.equals(SourceModel.PROPERTY_USERNAME))
         {
            source.setUsername((String) propertyValue);
         }
         if (propertyName.equals(SourceModel.PROPERTY_PASSWORD))
         {
            source.setPassword((String) propertyValue);
         }
         if (propertyName.equals(SourceModel.PROPERTY_MAX_DOWNLOAD))
         {
            source.setMaxDownload((Integer) propertyValue);
         }
      }
      try
      {
         SOURCE_SERVICE.updateSource(source);
      }
      catch (IllegalArgumentException e)
      {
         throw new ODataApplicationException("Invalid source property value: " + e.getMessage(),
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
   }

   @Override
   public void deleteEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      Source source = getSourceFromParameters(keyParameters);
      if (source == null)
      {
         throw new ODataApplicationException("No source exists width this id",
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }

      if (!SOURCE_SERVICE.deleteSource(source))
      {
         throw new ODataApplicationException("Source deletion failure",
               HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
      }
   }

   @Override
   public Entity createEntityData(Entity entity) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      String sourceUrl;
      String sourceUsername = null;
      String sourcePassword = null;
      Integer maxDownload = null;

      Object value = DataHandlerUtil.getPropertyValue(entity, SourceModel.PROPERTY_URL);
      if (value == null)
      {
         throw new ODataApplicationException("Cannot create source without Url property",
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
      sourceUrl = (String) value;

      value = DataHandlerUtil.getPropertyValue(entity, SourceModel.PROPERTY_USERNAME);
      if (value != null)
      {
         sourceUsername = (String) value;
      }

      value = DataHandlerUtil.getPropertyValue(entity, SourceModel.PROPERTY_PASSWORD);
      if (value != null)
      {
         sourcePassword = (String) value;
      }

      value = DataHandlerUtil.getPropertyValue(entity, SourceModel.PROPERTY_MAX_DOWNLOAD);
      if (value != null)
      {
         maxDownload = (Integer) value;
      }

      Source source = SOURCE_SERVICE.createSource(sourceUrl, sourceUsername, sourcePassword, maxDownload);
      return toOlingoEntity(source);
   }
}
