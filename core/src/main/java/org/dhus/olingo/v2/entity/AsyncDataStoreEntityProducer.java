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

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;

import org.dhus.api.olingo.v2.TypeInfo;

import org.dhus.olingo.v2.datamodel.AsyncDataStoreModel;
import org.dhus.olingo.v2.datamodel.complex.PatternReplaceComplexType;
import org.dhus.store.datastore.config.AsyncDataStoreConf;
import org.dhus.store.datastore.config.NamedDataStoreConf;
import org.dhus.store.datastore.config.PatternReplace;

/**
 * Entity producer for the abstract type AsyncDataStore.
 *
 * @param <I> type of input object to transform into an OData entity
 */
@TypeInfo(type = AsyncDataStoreConf.class, baseType = NamedDataStoreConf.class)
public class AsyncDataStoreEntityProducer<I extends AsyncDataStoreConf> extends DataStoreEntityProducer<I>
{
   @Override
   public Entity transform(I asyncDataStore)
   {
      Entity dataStoreEntity = super.transform(asyncDataStore);

      dataStoreEntity
            .addProperty(new Property(
                  null,
                  AsyncDataStoreModel.PROPERTY_HFS_LOCATION,
                  ValueType.PRIMITIVE,
                  asyncDataStore.getHfsLocation()))
            .addProperty(new Property(
                  null,
                  AsyncDataStoreModel.PROPERTY_IS_MASTER,
                  ValueType.PRIMITIVE,
                  asyncDataStore.isIsMaster()))
            .addProperty(new Property(
                  null,
                  AsyncDataStoreModel.PROPERTY_MAX_PENDING_REQUESTS,
                  ValueType.PRIMITIVE,
                  asyncDataStore.getMaxPendingRequests()))
            .addProperty(new Property(
                  null,
                  AsyncDataStoreModel.PROPERTY_MAX_RUNNING_REQUESTS,
                  ValueType.PRIMITIVE,
                  asyncDataStore.getMaxRunningRequests()))
            .addProperty(new Property(
                  null,
                  AsyncDataStoreModel.PROPERTY_MAX_PFRPU,
                  ValueType.PRIMITIVE,
                  asyncDataStore.getMaxParallelFetchRequestsPerUser()));

      if (asyncDataStore.getPatternReplaceIn() != null)
      {
         dataStoreEntity.addProperty(makePatternReplaceProperty(asyncDataStore.getPatternReplaceIn(), true));
      }

      if (asyncDataStore.getPatternReplaceIn() != null)
      {
         dataStoreEntity.addProperty(makePatternReplaceProperty(asyncDataStore.getPatternReplaceOut(), false));
      }

      return dataStoreEntity;
   }

   private static Property makePatternReplaceProperty(PatternReplace patternReplace, boolean in)
   {
      ComplexValue complexValue = new ComplexValue();

      complexValue.getValue().add(new Property(
            null,
            PatternReplaceComplexType.PROPERTY_PATTERN,
            ValueType.PRIMITIVE,
            patternReplace.getPattern()));

      complexValue.getValue().add(new Property(
            null,
            PatternReplaceComplexType.PROPERTY_REPLACEMENT,
            ValueType.PRIMITIVE,
            patternReplace.getReplacement()));

      return new Property(
            null,
            in ? AsyncDataStoreModel.PROPERTY_PATRE_IN : AsyncDataStoreModel.PROPERTY_PATRE_OUT,
            ValueType.COMPLEX,
            complexValue);
   }

}
