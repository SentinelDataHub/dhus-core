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
package org.dhus.olingo.v2.datamodel.complex;

import fr.gael.odata.engine.model.ComplexModel;

import java.util.Arrays;
import org.apache.olingo.commons.api.data.ValueType;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;

import org.dhus.olingo.v2.web.DHuSODataServlet;

public class ScannerStatusComplexType implements ComplexModel
{
   public static final String COMPLEX_TYPE_NAME = "ScannerStatus";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, COMPLEX_TYPE_NAME);

   public static final String PROPERTY_STATUS = "Status";
   public static final String PROPERTY_STATUS_MESSAGES = "StatusMessages";
   public static final String PROPERTY_INBOX = "Inbox";
   public static final String PROPERTY_PROCESSED = "Processed";
   public static final String PROPERTY_CANCELLED = "Cancelled";
   public static final String PROPERTY_ERROR = "Error";
   public static final String PROPERTY_TOTAL = "Total";

   @Override
   public CsdlComplexType getComplexType()
   {
      return new CsdlComplexType()
            .setName(COMPLEX_TYPE_NAME)
            .setProperties(Arrays.asList(new CsdlProperty()
                        .setName(PROPERTY_STATUS)
                        .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
                        .setNullable(false),
                  new CsdlProperty()
                        .setName(PROPERTY_STATUS_MESSAGES)
                        .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
                        .setCollection(true)
                        .setNullable(true),
                  new CsdlProperty()
                        .setName(PROPERTY_INBOX)
                        .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName())
                        .setNullable(true),
                  new CsdlProperty()
                        .setName(PROPERTY_PROCESSED)
                        .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName())
                        .setNullable(true),
                  new CsdlProperty()
                        .setName(PROPERTY_CANCELLED)
                        .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName())
                        .setNullable(true),
                  new CsdlProperty()
                        .setName(PROPERTY_ERROR)
                        .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName())
                        .setNullable(true),
                  new CsdlProperty()
                        .setName(PROPERTY_TOTAL)
                        .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName())
                        .setNullable(true)
            ));
   }

   @Override
   public String getName()
   {
      return COMPLEX_TYPE_NAME;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return FULL_QUALIFIED_NAME;
   }
}
