/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2020 GAEL Systems
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
import org.dhus.olingo.v2.datamodel.HttpAsyncDataStoreModel;
import org.dhus.olingo.v2.datamodel.PdgsDataStoreModel;
import org.dhus.store.datastore.config.HttpAsyncDataStoreConf;
import org.dhus.store.datastore.config.NamedDataStoreConf;

/**
 * Entity producer for the http abstract type AsyncDataStore.
 */
@TypeInfo(type = HttpAsyncDataStoreConf.class, baseType = NamedDataStoreConf.class)
public class HttpAsyncDataStoreEntityProducer<I extends HttpAsyncDataStoreConf> extends AsyncDataStoreEntityProducer<I>
{
   @Override
   public Entity transform(I httpAsyncDataStore)
   {
      Entity dataStoreEntity = super.transform(httpAsyncDataStore);

      dataStoreEntity.setType(PdgsDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());

      dataStoreEntity
            .addProperty(new Property(
                  null,
                  HttpAsyncDataStoreModel.PROPERTY_SERVICE_URL,
                  ValueType.PRIMITIVE,
                  httpAsyncDataStore.getServiceUrl()))
            .addProperty(new Property(
                  null,
                  HttpAsyncDataStoreModel.PROPERTY_LOGIN,
                  ValueType.PRIMITIVE,
                  httpAsyncDataStore.getLogin()))
            .addProperty(new Property(
                  null,
                  HttpAsyncDataStoreModel.PROPERTY_PASSWORD,
                  ValueType.PRIMITIVE,
                  "******"))
            .addProperty(new Property(
                  null,
                  HttpAsyncDataStoreModel.PROPERTY_MAX_CONCURRENTS_DOWNLOADS,
                  ValueType.PRIMITIVE,
                  httpAsyncDataStore.getMaxConcurrentsDownloads()))
            .addProperty(new Property(
                  null,
                  HttpAsyncDataStoreModel.PROPERTY_INTERVAL,
                  ValueType.PRIMITIVE,
                  httpAsyncDataStore.getInterval()));

      return dataStoreEntity;
   }

}
