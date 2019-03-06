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

import fr.gael.dhus.spring.context.ApplicationContextProvider;

import fr.gael.odata.engine.data.DataHandler;

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

import org.dhus.olingo.v2.datamodel.DataStoreModel;
import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreManager;

/**
 * Provides data for Product entities.
 */
public class ProductDataHandler implements DataHandler
{
   private static final DataStoreManager DS_MANAGER =
         ApplicationContextProvider.getBean(DataStoreManager.class);

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      return null;
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      return null;
   }

   @Override
   public EntityCollection getRelatedEntityCollectionData(Entity sourceEntity) throws ODataApplicationException
   {
      EntityCollection navigationTargetEntityCollection = new EntityCollection();
      String nameEntity = (String) sourceEntity.getProperty(DataStoreModel.PROPERTY_NAME).getValue();

      if (nameEntity.isEmpty())
      {
         throw new ODataApplicationException("Entity name not found",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }

      // get list of datastores
      List<DataStore> dataStore = DS_MANAGER.list();
      for (int i = 0; i < dataStore.size(); i++)
      {
         // retrieve list of product UUIDs from datastore of given name
         if (dataStore.get(i).getName().equals(nameEntity))
         {
            List<String> productList = dataStore.get(i).getProductList();

            // make entities
            for (String uuid: productList)
            {
               Entity productEntity = new Entity().addProperty(new Property(null, ProductModel.ID, ValueType.PRIMITIVE, uuid));
               navigationTargetEntityCollection.getEntities().add(productEntity);
            }
         }
      }
      return navigationTargetEntityCollection;
   }

   @Override
   public Entity getRelatedEntityData(Entity entity)
   {
      return null;
   }

   @Override
   public Entity getRelatedEntityData(Entity entity, List<UriParameter> navigationKeyParameters)
         throws ODataApplicationException
   {
      return null;
   }

   @Override
   public void updateEntityData(List<UriParameter> keyParameters, Entity updatedEntity, HttpMethod httpMethod)
         throws ODataApplicationException {}

   @Override
   public void deleteEntityData(List<UriParameter> keyParameters) throws ODataApplicationException {}

   @Override
   public Entity createEntityData(Entity entity) throws ODataApplicationException
   {
      return null;
   }
}
