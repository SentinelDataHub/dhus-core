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
import org.dhus.olingo.v2.datamodel.LtaDataStoreModel;
import org.dhus.olingo.v2.datamodel.OndaDataStoreModel;
import org.dhus.store.datastore.config.LtaDataStoreConf;
import org.dhus.store.datastore.config.NamedDataStoreConf;

@TypeInfo(type = LtaDataStoreConf.class, baseType = NamedDataStoreConf.class)
public class LtaDataStoreEntityProducer extends HttpAsyncDataStoreEntityProducer<LtaDataStoreConf>
{

   @Override
   public Entity transform(LtaDataStoreConf ltaDataStore)
   {
      Entity dataStoreEntity = super.transform(ltaDataStore);
      dataStoreEntity.setType(LtaDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());

      dataStoreEntity
      .addProperty(new Property(
            null,
            OndaDataStoreModel.PROPERTY_ORDER,
            ValueType.PRIMITIVE,
            ltaDataStore.isOrder()));
      
      return dataStoreEntity;
   }
}
