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

import org.dhus.olingo.v2.datamodel.HfsDataStoreModel;
import org.dhus.store.datastore.config.HfsDataStoreConf;
import org.dhus.store.datastore.config.NamedDataStoreConf;

/**
 * Transforms HfsDataStoreConf configuration objects to OData entities.
 */
@TypeInfo(type = HfsDataStoreConf.class, baseType = NamedDataStoreConf.class)
public class HFSDataStoreEntityProducer extends DataStoreEntityProducer<HfsDataStoreConf>
{
   @Override
   public Entity transform(HfsDataStoreConf hfsDataStore)
   {
      Entity entity = super.transform(hfsDataStore);

      entity.setType(HfsDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());

      entity
            .addProperty(new Property(
                  null,
                  HfsDataStoreModel.PROPERTY_PATH,
                  ValueType.PRIMITIVE,
                  hfsDataStore.getPath()))
            .addProperty(new Property(
                  null,
                  HfsDataStoreModel.PROPERTY_MAXFILEDEPTH,
                  ValueType.PRIMITIVE,
                  hfsDataStore.getMaxFileNo()))
            .addProperty(new Property(
                  null,
                  HfsDataStoreModel.PROPERTY_MAXITEMS,
                  ValueType.PRIMITIVE,
                  hfsDataStore.getMaxItems()));

      return entity;
   }

}
