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

import org.dhus.olingo.v2.datamodel.ParamPdgsDataStoreModel;
import org.dhus.store.datastore.config.ParamPdgsDataStoreConf;
import org.dhus.store.datastore.config.PdgsDataStoreConf;

@TypeInfo(type = ParamPdgsDataStoreConf.class, baseType = PdgsDataStoreConf.class)
public class ParamPdgsDataStoreEntityProducer extends PdgsDataStoreEntityProducer
{
   @Override
   public Entity transform(PdgsDataStoreConf pdgsDataStore)
   {
      Entity dataStoreEntity = super.transform(pdgsDataStore);

      ParamPdgsDataStoreConf paramDataStore = (ParamPdgsDataStoreConf) pdgsDataStore;

      dataStoreEntity.setType(ParamPdgsDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());

      dataStoreEntity
            .addProperty(new Property(
                  null,
                  ParamPdgsDataStoreModel.PROPERTY_GPUP_PATTERN,
                  ValueType.PRIMITIVE,
                  paramDataStore.getUrlParamPattern()));

      dataStoreEntity
            .addProperty(new Property(
                  null,
                  ParamPdgsDataStoreModel.PROPERTY_PNAME_PATTERN,
                  ValueType.PRIMITIVE,
                  paramDataStore.getProductNamePattern()));

      return dataStoreEntity;
   }
}
