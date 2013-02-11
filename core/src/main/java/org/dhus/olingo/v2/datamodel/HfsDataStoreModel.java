/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2018 GAEL Systems
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
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;

import org.dhus.olingo.v2.web.DHuSODataServlet;

/**
 * Describes the HfsDataStores entity types and set.
 */
public class HfsDataStoreModel extends DataStoreModel
{
   public static final String ENTITY_TYPE_NAME = "HFSDataStore";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);

   public static final String PROPERTY_PATH = "Path";
   public static final String PROPERTY_MAXFILEDEPTH = "MaxFileDepth";
   public static final String PROPERTY_MAXITEMS = "MaxItems";

   @Override
   public CsdlEntityType getEntityType()
   {
      CsdlProperty path = new CsdlProperty()
            .setName(PROPERTY_PATH)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty maxFileDepth = new CsdlProperty()
            .setName(PROPERTY_MAXFILEDEPTH)
            .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty maxItems = new CsdlProperty()
            .setName(PROPERTY_MAXITEMS)
            .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
            .setNullable(false);

      // TODO handle authorizations
      // TODO handle navigation
      return new CsdlEntityType()
            .setBaseType(DataStoreModel.ABSTRACT_FULL_QUALIFIED_NAME)
            .setName(ENTITY_TYPE_NAME)
            .setProperties(Arrays.asList(path, maxFileDepth, maxItems));
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
