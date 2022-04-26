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
package org.dhus.olingo.v2.datamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.dhus.olingo.v2.datamodel.complex.ChecksumComplexType;
import org.dhus.olingo.v2.datamodel.complex.TimeRangeComplexType;
import org.dhus.olingo.v2.web.DHuSODataServlet;

import fr.gael.odata.engine.model.EntityModel;


public class DeletedProductModel implements EntityModel
{
   public static final String ENTITY_TYPE_NAME = "DeletedProduct";
   public static final String ENTITY_SET_NAME = "DeletedProducts";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);

   public static final String PROPERTY_ID = "Id";
   public static final String PROPERTY_NAME = "Name";
   public static final String PROPERTY_CREATION_DATE = "CreationDate";
   public static final String PROPERTY_FOOTPRINT = "FootPrint";
   public static final String PROPERTY_SIZE = "Size";
   public static final String PROPERTY_INGESTION_DATE = "IngestionDate";
   public static final String PROPERTY_CONTENT_DATE = "ContentDate";
   public static final String PROPERTY_DELETION_DATE = "DeletionDate";
   public static final String PROPERTY_DELETION_CAUSE = "DeletionCause";
   public static final String PROPERTY_CHECKSUM = "Checksum";

   @Override
   public CsdlEntityType getEntityType()
   {
      List<CsdlProperty> properties = new ArrayList<>();

      CsdlProperty id = new CsdlProperty()
            .setName(PROPERTY_ID)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);
      properties.add(id);

      CsdlProperty name = new CsdlProperty()
            .setName(PROPERTY_NAME)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);
      properties.add(name);

      CsdlProperty creationDate = new CsdlProperty()
            .setName(PROPERTY_CREATION_DATE)
            .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
            .setPrecision(3)
            .setNullable(false);
      properties.add(creationDate);

      CsdlProperty footPrint = new CsdlProperty()
            .setName(PROPERTY_FOOTPRINT)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);
      properties.add(footPrint);

      CsdlProperty size = new CsdlProperty()
            .setName(PROPERTY_SIZE)
            .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName())
            .setNullable(true);
      properties.add(size);

      CsdlProperty ingestionDate = new CsdlProperty()
            .setName(PROPERTY_INGESTION_DATE)
            .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
            .setPrecision(3)
            .setNullable(true);
      properties.add(ingestionDate);

      CsdlProperty deletionDate = new CsdlProperty()
            .setName(PROPERTY_DELETION_DATE)
            .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
            .setPrecision(3)
            .setNullable(true);
      properties.add(deletionDate);

      CsdlProperty deletionCause = new CsdlProperty()
            .setName(PROPERTY_DELETION_CAUSE)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);
      properties.add(deletionCause);

      // Complex Properties
      CsdlProperty contentDate = new CsdlProperty()
            .setName(PROPERTY_CONTENT_DATE)
            .setType(TimeRangeComplexType.FULL_QUALIFIED_NAME)
            .setPrecision(3)
            .setNullable(true);
      properties.add(contentDate);

      CsdlProperty checksum = new CsdlProperty()
            .setName(PROPERTY_CHECKSUM)
            .setType(ChecksumComplexType.FULL_QUALIFIED_NAME)
            .setCollection(true)
            .setNullable(true);
      properties.add(checksum);

      CsdlPropertyRef nameKey = new CsdlPropertyRef().setName(PROPERTY_ID);

      return new CsdlEntityType().setName(ENTITY_TYPE_NAME)
            .setProperties(properties)
            .setKey(Collections.<CsdlPropertyRef> singletonList(nameKey));
   }

   @Override
   public String getEntitySetName()
   {
      return ENTITY_SET_NAME;
   }

   @Override
   public String getName()
   {
      return ENTITY_TYPE_NAME;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return FULL_QUALIFIED_NAME;
   }
}
