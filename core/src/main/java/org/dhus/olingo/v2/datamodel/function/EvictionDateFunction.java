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
package org.dhus.olingo.v2.datamodel.function;

import fr.gael.odata.engine.model.FunctionModel;

import java.util.Collections;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;

import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.olingo.v2.web.DHuSODataServlet;

public class EvictionDateFunction implements FunctionModel
{
   public static final String FUNCTION_EVICTION_DATE = "EvictionDate";
   public static final FullQualifiedName FUNCTION_EVICTION_DATE_FNQ =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, FUNCTION_EVICTION_DATE);

   public static final String PARAM_PRODUCT = "Product";

   @Override
   public CsdlFunction getFunction()
   {
      CsdlParameter productParameter = new CsdlParameter()
            .setName(PARAM_PRODUCT)
            .setNullable(false)
            .setType(ProductModel.FULL_QUALIFIED_NAME);

      CsdlReturnType returnType = new CsdlReturnType()
            .setCollection(false)
            .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
            .setNullable(true)
            .setPrecision(3);

      return new CsdlFunction()
            .setName(FUNCTION_EVICTION_DATE)
            .setBound(true)
            .setParameters(Collections.singletonList(productParameter))
            .setReturnType(returnType);
   }

   @Override
   public String getName()
   {
      return FUNCTION_EVICTION_DATE;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return FUNCTION_EVICTION_DATE_FNQ;
   }

}
