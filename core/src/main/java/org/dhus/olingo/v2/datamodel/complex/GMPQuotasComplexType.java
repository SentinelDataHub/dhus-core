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
package org.dhus.olingo.v2.datamodel.complex;

import fr.gael.odata.engine.model.ComplexModel;

import java.util.Arrays;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;

import org.dhus.olingo.v2.web.DHuSODataServlet;

/**
 * Quotas configuration for GMP DataStores.
 */
public class GMPQuotasComplexType implements ComplexModel
{
   public static final String COMPLEX_TYPE_NAME = "Quotas";
   public static final String PROPERTY_MAX_QPU  = "MaxQueryPerUser";
   public static final String PROPERTY_TIMESPAN = "Timespan";

   public static final FullQualifiedName FULL_QUALIFIED_NAME
         = new FullQualifiedName(DHuSODataServlet.NAMESPACE, COMPLEX_TYPE_NAME);

   @Override
   public CsdlComplexType getComplexType()
   {
      CsdlProperty maxQueryPerUser = new CsdlProperty()
            .setName(PROPERTY_MAX_QPU)
            .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());

      CsdlProperty timespan = new CsdlProperty()
            .setName(PROPERTY_TIMESPAN)
            .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName());

      return new CsdlComplexType()
            .setName(COMPLEX_TYPE_NAME)
            .setProperties(Arrays.<CsdlProperty>asList(maxQueryPerUser, timespan));
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
