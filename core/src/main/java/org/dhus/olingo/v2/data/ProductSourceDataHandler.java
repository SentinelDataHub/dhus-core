/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2021 GAEL Systems
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.dhus.olingo.v2.ODataSecurityManager;
import org.dhus.olingo.v2.datamodel.ProductSourceModel;
import org.dhus.olingo.v2.datamodel.SourceModel;

import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.config.productsource.ProductSource;
import fr.gael.dhus.service.IProductSourceService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.odata.engine.data.DataHandler;
import fr.gael.odata.engine.data.DataHandlerUtil;

public class ProductSourceDataHandler implements DataHandler
{   
   private static final IProductSourceService PRODUCT_SOURCE_SERVICE =
         ApplicationContextProvider.getBean(IProductSourceService.class);

   private static final Object DEFAULT_PASSWORD = "*****";
   
   private Entity toSourceEntity(ProductSource source)
   {
      Entity sourceEntity = new Entity();

      // Id
      sourceEntity.addProperty(new Property(
            null, 
            ProductSourceModel.PROPERTY_ID, 
            ValueType.PRIMITIVE,
            source.getId()));

      // Url
      sourceEntity.addProperty(new Property(
            null,
            ProductSourceModel.PROPERTY_URL,
            ValueType.PRIMITIVE,
            source.getUrl()));
      
      // Login
      sourceEntity.addProperty(new Property(
            null,
            ProductSourceModel.PROPERTY_LOGIN,
            ValueType.PRIMITIVE,
            source.getLogin()));
      
      // Password
      sourceEntity.addProperty(new Property(
            null,
            ProductSourceModel.PROPERTY_PASSWORD,
            ValueType.PRIMITIVE,
            DEFAULT_PASSWORD));
      
      // RemoteIncoming
      sourceEntity.addProperty(new Property(
            null,
            ProductSourceModel.PROPERTY_REMOTE_INCOMING,
            ValueType.PRIMITIVE,
            source.getRemoteIncoming()));

      // ListableIProductSourceService
      sourceEntity.addProperty(new Property(
            null, 
            ProductSourceModel.PROPERTY_LISTABLE, 
            ValueType.PRIMITIVE,
            source.isListable()));

      try
      {
         sourceEntity.setId(new URI(ProductSourceModel.ENTITY_SET_NAME + "("+ source.getId() + ")"));
      }
      catch (URISyntaxException e)
      {
         throw new ODataRuntimeException("Unable to create id for entity: "+ ProductSourceModel.ENTITY_SET_NAME, e);
      }
      return sourceEntity;
   }

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      List<ProductSource> productSources = PRODUCT_SOURCE_SERVICE.getProductSources();
      EntityCollection entities = new EntityCollection();
      productSources.forEach(productSource -> entities.getEntities().add(toSourceEntity(productSource)));
      return entities;
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters)
         throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);
      for (UriParameter parameter : keyParameters)
      {
         if (parameter.getName().equals(SourceModel.PROPERTY_ID))
         {
            ProductSource source = PRODUCT_SOURCE_SERVICE.getProductSource(Integer.valueOf(parameter.getText()));
            return toSourceEntity(source);
         }
      }
      return null;
   }

   @Override
   public Entity createEntityData(Entity entity) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      String url = null;
      String login = null;
      String password = null;
      String remoteIncoming = null;
      String sourceCollection = null;
      boolean listable = false;
      XMLGregorianCalendar lastCreationDate = null;
      
      
      Object value = DataHandlerUtil.getPropertyValue(entity, ProductSourceModel.PROPERTY_URL);
      if (value == null)
      {
         throw new ODataApplicationException("Cannot create source without Url property",
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
       url = (String) value;

      value = DataHandlerUtil.getPropertyValue(entity, ProductSourceModel.PROPERTY_LOGIN);
      if (value != null)
      {
         login = (String) value;
      }

      value = DataHandlerUtil.getPropertyValue(entity, ProductSourceModel.PROPERTY_PASSWORD);
      if (value != null)
      {
         password = (String) value;
      }

      value = DataHandlerUtil.getPropertyValue(entity, ProductSourceModel.PROPERTY_REMOTE_INCOMING);
      if (value != null)
      {
         remoteIncoming = (String) value;
      }

      value = DataHandlerUtil.getPropertyValue(entity, ProductSourceModel.PROPERTY_LISTABLE);
      if (value != null)
      {
         listable = (Boolean) value;
      }

      ProductSource source = PRODUCT_SOURCE_SERVICE.createProductSource(url,
            login,
            password,
            remoteIncoming,
            sourceCollection,
            listable,
            lastCreationDate);
      return toSourceEntity(source);
   }

   @Override
   public void updateEntityData(List<UriParameter> keyParameters, Entity updatedEntity,
         HttpMethod httpMethod) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);
      for (UriParameter parameter : keyParameters)
      {
         if (parameter.getName().equals(SourceModel.PROPERTY_ID))
         {
            ProductSource source = PRODUCT_SOURCE_SERVICE.getProductSource(Integer.valueOf(parameter.getText()));
            if (source == null)
            {
               throw new ODataApplicationException("No source exists width this id",
                     HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
            }
            for (Property property: updatedEntity.getProperties())
            {
               String propertyName = property.getName();
               Object propertyValue = property.getValue();

               if (propertyName.equals(ProductSourceModel.PROPERTY_URL))
               {
                  source.setUrl((String) propertyValue);
               }
               if (propertyName.equals(ProductSourceModel.PROPERTY_LOGIN))
               {
                  source.setLogin((String) propertyValue);
               }
               if (propertyName.equals(ProductSourceModel.PROPERTY_PASSWORD))
               {
                  source.setPassword((String) propertyValue);
               }
               if (propertyName.equals(ProductSourceModel.PROPERTY_REMOTE_INCOMING))
               {
                  source.setRemoteIncoming((String) propertyValue);
               }
               if (propertyName.equals(ProductSourceModel.PROPERTY_LISTABLE))
               {
                  source.setListable((Boolean) propertyValue);
               }
            }
            try
            {
               PRODUCT_SOURCE_SERVICE.updateProductSource(source);
            }
            catch (IllegalArgumentException e)
            {
               throw new ODataApplicationException("Invalid source property value: " + e.getMessage(),
                     HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
            }  
         }
      }
   }

   @Override
   public void deleteEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      for (UriParameter parameter : keyParameters)
      {
         if (parameter.getName().equals(SourceModel.PROPERTY_ID))
         {
            ProductSource source = PRODUCT_SOURCE_SERVICE.getProductSource(Integer.valueOf(parameter.getText()));
            if (source == null)
            {
               throw new ODataApplicationException("No source exists width this id",
                     HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
            }
            PRODUCT_SOURCE_SERVICE.removeProductSource(source);
         }
      }
   }
}