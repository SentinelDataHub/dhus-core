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

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;

import org.dhus.api.olingo.v2.TypeInfo;

import org.dhus.olingo.v2.datamodel.RemoteDhusDataStoreModel;
import org.dhus.store.datastore.config.RemoteDhusDataStoreConf;
import org.dhus.store.datastore.config.NamedDataStoreConf;

/**
 * Transforms RemoteDhusDataStoreConf configuration objects to OData entities.
 */
@TypeInfo(type = RemoteDhusDataStoreConf.class, baseType = NamedDataStoreConf.class)
public class RemoteDhusDataStoreEntityProducer extends DataStoreEntityProducer<RemoteDhusDataStoreConf>
{
   @Override
   public Entity transform(RemoteDhusDataStoreConf dhusDataStore)
   {
      Entity dataStoreEntity = super.transform(dhusDataStore);

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
                  "******"))
            .addProperty(new Property(
                  null,
                  RemoteDhusDataStoreModel.PROPERTY_ALIVE_INTERVAL,
                  ValueType.PRIMITIVE,
                  dhusDataStore.getAliveInterval()));

      return dataStoreEntity;
   }

}
