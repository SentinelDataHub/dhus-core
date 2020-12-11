/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018,2019 GAEL Systems
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

import fr.gael.dhus.database.object.config.synchronizer.SmartProductSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerConfiguration;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerSource;

import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;

import org.dhus.api.olingo.v2.TypeInfo;

import org.dhus.olingo.v2.datamodel.SmartSynchronizerModel;
import org.dhus.olingo.v2.datamodel.complex.SynchronizerSourceComplexType;

/**
 * Converts {@link SmartProductSynchronizer} configuration objects to OData Entries.
 * @param <I>
 */
@TypeInfo(type = SmartProductSynchronizer.class, baseType = SynchronizerConfiguration.class)
public class SmartSynchronizerEntityProducer<I extends SmartProductSynchronizer> extends SynchronizerEntityProducer<I>
{
   @Override
   public Entity transform(I syncConf)
   {
      Entity entity = super.transform(syncConf);

      entity.setType(SmartSynchronizerModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());

      entity.addProperty(new Property(
            null,
            SmartSynchronizerModel.PROPERTY_PAGE_SIZE,
            ValueType.PRIMITIVE,
            syncConf.getPageSize()));

      entity.addProperty(new Property(
            null,
            SmartSynchronizerModel.PROPERTY_FILTER_PARAM,
            ValueType.PRIMITIVE,
            syncConf.getFilterParam()));

      String geofilter = null;
      String operation = syncConf.getGeofilterOp();
      String shape = syncConf.getGeofilterShape();
      if (operation != null && !operation.isEmpty() && shape != null && !shape.isEmpty())
      {
         geofilter = String.format("%s %s", operation, shape);
      }
      entity.addProperty(new Property(
            null,
            SmartSynchronizerModel.PROPERTY_FILTER_GEO,
            ValueType.PRIMITIVE,
            geofilter));

      entity.addProperty(new Property(
            null,
            SmartSynchronizerModel.PROPERTY_ATTEMPTS,
            ValueType.PRIMITIVE,
            syncConf.getAttempts()));

      entity.addProperty(new Property(
            null,
            SmartSynchronizerModel.PROPERTY_TIMEOUT,
            ValueType.PRIMITIVE,
            syncConf.getTimeout()));

      entity.addProperty(new Property(
            null,
            SmartSynchronizerModel.PROPERTY_THRESHOLD,
            ValueType.PRIMITIVE,
            syncConf.getThreshold()));

      List<SynchronizerSource> synchronizerSourceList = syncConf.getSources().getSource();
      ArrayList<ComplexValue> complexValueList = new ArrayList<>(synchronizerSourceList.size());
      for (SynchronizerSource synchronizerSource: synchronizerSourceList)
      {
         ComplexValue complexValue = toComplexValue(synchronizerSource);
         if (complexValue != null)
         {
            complexValueList.add(complexValue);
         }
      }
      entity.addProperty(new Property(
            null,
            SmartSynchronizerModel.PROPERTY_SOURCES,
            ValueType.COLLECTION_COMPLEX,
            complexValueList));

      return entity;
   }

   private ComplexValue toComplexValue(SynchronizerSource synchronizerSource)
   {
      ComplexValue complexValue = null;
      if (synchronizerSource != null)
      {
         complexValue = new ComplexValue();

         complexValue.getValue().add(new Property(
               null,
               SynchronizerSourceComplexType.PROPERTY_SOURCE_ID,
               ValueType.PRIMITIVE,
               synchronizerSource.getSourceId()));

         complexValue.getValue().add(new Property(
               null,
               SynchronizerSourceComplexType.PROPERTY_LAST_CREATION_DATE,
               ValueType.PRIMITIVE,
               synchronizerSource.getLastCreated().toGregorianCalendar()));

         complexValue.getValue().add(new Property(
               null,
               SynchronizerSourceComplexType.PROPERTY_SOURCE_COLLECTION,
               ValueType.PRIMITIVE,
               synchronizerSource.getSourceCollection()));
      }
      return complexValue;
   }

}
