/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2021 GAEL Systems
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

import java.util.Arrays;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.dhus.olingo.v2.web.DHuSODataServlet;

import fr.gael.odata.engine.model.EntityModel;

public class ReferencedSourceModel implements EntityModel
{
   public static final String ENTITY_TYPE_NAME = "ReferencedSource";
   public static final String ENTITY_SET_NAME = "ReferencedSources";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);

   public static final String PROPERTY_ID = "Id";
   public static final String PROPERTY_REFERENCED_ID = "ReferenceId";
   public static final String PROPERTY_SOURCE_COLLECTION = "SourceCollection";
   public static final String PROPERTY_LAST_CREATION_DATE = "LastCreationDate";
   public static final String PROPERTY_LAST_DATE_SOURCE_USED = "LastDateSourceUsed";


   @Override
   public CsdlEntityType getEntityType()
   {      
      CsdlProperty referencedId = new CsdlProperty()
            .setName(PROPERTY_REFERENCED_ID)
            .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName())
            .setNullable(false);
      
      CsdlProperty sourceCollection = new CsdlProperty()
            .setName(PROPERTY_SOURCE_COLLECTION)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

      CsdlProperty lastCreationDate = new CsdlProperty()
            .setName(PROPERTY_LAST_CREATION_DATE)
            .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
            .setPrecision(3);
      
      CsdlProperty lastDateSourceUsed = new CsdlProperty()
            .setName(PROPERTY_LAST_DATE_SOURCE_USED)
            .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
            .setPrecision(3);

      return new CsdlEntityType()
            .setName(ENTITY_TYPE_NAME)
            .setProperties(Arrays.asList(referencedId, sourceCollection, lastCreationDate, lastDateSourceUsed));
   }

   @Override
   public CsdlEntitySet getEntitySet()
   {
      CsdlEntitySet entitySet = EntityModel.super.getEntitySet();
      return entitySet.setIncludeInServiceDocument(false);
   }

   @Override
   public String getName()
   {
      return ENTITY_TYPE_NAME;
   }

   @Override
   public String getEntitySetName()
   {
      return ENTITY_SET_NAME;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return FULL_QUALIFIED_NAME;
   }
}
