/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019 GAEL Systems
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
package org.dhus.olingo.v2.entity;

import fr.gael.odata.engine.data.DataHandlerUtil;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;

import org.dhus.api.olingo.v2.EntityProducer;
import org.dhus.api.olingo.v2.TypeInfo;

import org.dhus.olingo.v2.datamodel.DataStoreModel;
import org.dhus.store.datastore.config.NamedDataStoreConf;

/**
 * Entity producer for the abstract type DataStore.
 *
 * @param <I> type of input object to transform into an OData entity
 */
@TypeInfo(type = NamedDataStoreConf.class)
public class DataStoreEntityProducer<I extends NamedDataStoreConf> implements EntityProducer<I>
{
   @Override
   public Entity transform(I dataStore)
   {
      Entity entity = new Entity();

      entity
            .addProperty(new Property(
                  null,
                  DataStoreModel.PROPERTY_NAME,
                  ValueType.PRIMITIVE,
                  dataStore.getName()))
            .addProperty(new Property(
                  null,
                  DataStoreModel.PROPERTY_RESTRICTION,
                  ValueType.PRIMITIVE,
                  dataStore.getRestriction().value()))
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
                  dataStore.isAutoEviction()))
            .addProperty(new Property(
                  null,
                  DataStoreModel.PROPERTY_FILTER,
                  ValueType.PRIMITIVE,
                  dataStore.getFilter()));

      entity.setId(DataHandlerUtil.createEntityId(
            DataStoreModel.ABSTRACT_ENTITY_SET_NAME,
            dataStore.getName()));

      return entity;
   }

}
