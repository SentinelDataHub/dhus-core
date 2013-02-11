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

import java.util.Arrays;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;

import org.dhus.olingo.v2.web.DHuSODataServlet;

import fr.gael.odata.engine.model.ComplexModel;

public class GMPConfigurationComplexType implements ComplexModel
{
   public static final String COMPLEX_TYPE_NAME = "GMPConfiguration";
   public static final FullQualifiedName FULL_QUALIFIED_NAME
         = new FullQualifiedName(DHuSODataServlet.NAMESPACE, COMPLEX_TYPE_NAME);

   public static final String PROPERTY_AGENTID = "AgentId";
   public static final String PROPERTY_TARGETID = "TargetId";

   @Override
   public CsdlComplexType getComplexType()
   {
      CsdlProperty agentId = new CsdlProperty()
            .setName(PROPERTY_AGENTID)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty targetId = new CsdlProperty()
            .setName(PROPERTY_TARGETID)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      return new CsdlComplexType()
            .setName(COMPLEX_TYPE_NAME)
            .setProperties(Arrays.asList(agentId, targetId));
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
