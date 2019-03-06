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
 * Describes the OpenStackDataStores entity types and set.
 */
public class OpenstackDataStoreModel extends DataStoreModel
{
   public static final String ENTITY_TYPE_NAME = "OpenStackDataStore";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);

   public static final String PROPERTY_PROVIDER = "Provider";
   public static final String PROPERTY_IDENTITY = "Identity";
   public static final String PROPERTY_CREDENTIAL = "Credential";
   public static final String PROPERTY_URL = "Url";
   public static final String PROPERTY_REGION = "Region";
   public static final String PROPERTY_CONTAINER = "Container";

   @Override
   public CsdlEntityType getEntityType()
   {
      CsdlProperty provider = new CsdlProperty()
            .setName("Provider")
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty identity = new CsdlProperty()
            .setName("Identity")
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty credential = new CsdlProperty()
            .setName("Credential")
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty url = new CsdlProperty()
            .setName("Url")
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty region = new CsdlProperty()
            .setName("Region")
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty container = new CsdlProperty()
            .setName("Container")
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      // TODO handle authorizations
      // TODO handle navigation
      return new CsdlEntityType()
            .setBaseType(DataStoreModel.ABSTRACT_FULL_QUALIFIED_NAME)
            .setName(ENTITY_TYPE_NAME)
            .setProperties(Arrays.asList(provider, identity, credential, url, region, container));
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
