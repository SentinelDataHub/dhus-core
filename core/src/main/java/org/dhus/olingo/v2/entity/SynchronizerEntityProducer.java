/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018 GAEL Systems
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

import fr.gael.dhus.database.object.config.synchronizer.SynchronizerConfiguration;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;

import org.dhus.olingo.v2.datamodel.SynchronizerModel;
import org.dhus.olingo.v2.datamodel.complex.CronComplexType;

@TypeInfo(type = SynchronizerConfiguration.class)
public class SynchronizerEntityProducer<I extends SynchronizerConfiguration> implements EntityProducer<I>
{
   @Override
   public Entity transform(I syncConf)
   {
      Entity entity = new Entity();

      entity.setType(SynchronizerModel.ABSTRACT_FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());

      entity.addProperty(new Property(
            null,
            SynchronizerModel.PROPERTY_ID,
            ValueType.PRIMITIVE,
            syncConf.getId()));

      entity.addProperty(new Property(
            null,
            SynchronizerModel.PROPERTY_LABEL,
            ValueType.PRIMITIVE,
            syncConf.getLabel()));

      entity.addProperty(new Property(
            null,
            SynchronizerModel.PROPERTY_CREATED_DATE,
            ValueType.PRIMITIVE,
            syncConf.getCreated().toGregorianCalendar()));

      entity.addProperty(new Property(
            null,
            SynchronizerModel.PROPERTY_UPDATED_DATE,
            ValueType.PRIMITIVE,
            syncConf.getModified().toGregorianCalendar()));

      ComplexValue complexValue = new ComplexValue();
      complexValue.getValue().add(new Property(
            null,
            CronComplexType.PROPERTY_ACTIVE,
            ValueType.PRIMITIVE,
            syncConf.isActive()));
      complexValue.getValue().add(new Property(
            null,
            CronComplexType.PROPERTY_SCHEDULE,
            ValueType.PRIMITIVE,
            syncConf.getSchedule()));

      entity.addProperty(new Property(
            null,
            CronComplexType.COMPLEX_TYPE_NAME,
            ValueType.COMPLEX,
            complexValue));

      return entity;
   }
}
