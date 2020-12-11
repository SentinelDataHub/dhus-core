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

import fr.gael.dhus.database.object.Transformation;
import fr.gael.odata.engine.data.DataHandlerUtil;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.dhus.api.olingo.v2.EntityProducer;
import org.dhus.api.olingo.v2.TypeInfo;

import org.dhus.olingo.v2.datamodel.JobModel;
import org.dhus.olingo.v2.datamodel.TransformationModel;
import org.dhus.transformation.TransformationStatusUtil;

@TypeInfo(type = Transformation.class)
public class TransformationEntityProducer implements EntityProducer<Transformation>
{
   @Override
   public Entity transform(Transformation execution)
   {
      if (execution != null)
      {
         Entity entity = new Entity();
         entity.setType(TransformationModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());
         entity.setId(DataHandlerUtil.createEntityId(TransformationModel.ENTITY_SET_NAME, execution.getUuid()));

         entity.addProperty(new Property(
               EdmPrimitiveTypeKind.String.getFullQualifiedName().getFullQualifiedNameAsString(),
               JobModel.PROPERTY_ID,
               ValueType.PRIMITIVE,
               execution.getUuid()));

         entity.addProperty(new Property(
               EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName().getFullQualifiedNameAsString(),
               JobModel.PROPERTY_SUBMISSION_TIME,
               ValueType.PRIMITIVE,
               execution.getCreationDate()));

         entity.addProperty(new Property(
               EdmPrimitiveTypeKind.Int32.getFullQualifiedName().getFullQualifiedNameAsString(),
               JobModel.PROPERTY_ESTIMATED_TIME,
               ValueType.PRIMITIVE,
               null));

         entity.addProperty(new Property(
               EdmPrimitiveTypeKind.String.getFullQualifiedName().getFullQualifiedNameAsString(),
               JobModel.PROPERTY_STATUS,
               ValueType.ENUM,
               TransformationStatusUtil.fromString(execution.getStatus()).value()));

         return entity;
      }
      return null;
   }
}
