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
package org.dhus.olingo.v2.datamodel;

import java.util.Arrays;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.dhus.olingo.v2.datamodel.complex.ObjectStorageComplexType;
import org.dhus.olingo.v2.datamodel.complex.OndaScannerComplexType;
import org.dhus.olingo.v2.web.DHuSODataServlet;

public class OndaDataStoreModel extends HttpAsyncDataStoreModel
{
   public static final String ENTITY_TYPE_NAME = "ONDADataStore";
   public static final FullQualifiedName FULL_QUALIFIED_NAME
         = new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);

   public static final String PROPERTY_ORDER = "Order";
   public static final String PROPERTY_OBJECT_STORAGE_CREDENTIAL = "ObjectStorageCredential";
   public static final String PROPERTY_ONDA_SCANNER = "OndaScanner";

   @Override
   public CsdlEntityType getEntityType()
   {
      CsdlProperty order = new CsdlProperty()
            .setName(PROPERTY_ORDER)
            .setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName())
            .setNullable(true);
      
      CsdlProperty objectStorageCredential = new CsdlProperty()
            .setName(PROPERTY_OBJECT_STORAGE_CREDENTIAL)
            .setType(ObjectStorageComplexType.FULL_QUALIFIED_NAME)
            .setNullable(true);
      
      CsdlProperty ondaScanner = new CsdlProperty()
            .setName(PROPERTY_ONDA_SCANNER)
            .setType(OndaScannerComplexType.FULL_QUALIFIED_NAME)
            .setNullable(true);

      return new CsdlEntityType()
            .setBaseType(HttpAsyncDataStoreModel.FULL_QUALIFIED_NAME)
            .setName(ENTITY_TYPE_NAME)
            .setProperties(Arrays.asList(
                  order,
                  objectStorageCredential,
                  ondaScanner));
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
