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
package org.dhus.olingo.v2.datamodel.action;

import fr.gael.odata.engine.model.ActionModel;

import java.util.Arrays;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;

import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.olingo.v2.datamodel.TransformationModel;
import org.dhus.olingo.v2.datamodel.complex.TransformationParametersComplexType;
import org.dhus.olingo.v2.web.DHuSODataServlet;

public class TransformProductAction implements ActionModel
{
   public static final String ACTION_NAME = "TransformProduct";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ACTION_NAME);

   public static final String PARAM_PRODUCT = "Product";
   public static final String PARAM_TRANSFORMER_NAME = "TransformerName";
   public static final String PARAM_PARAMETERS = "Parameters";

   @Override
   public CsdlAction getAction()
   {
      CsdlParameter boundProduct = new CsdlParameter()
            .setName(PARAM_PRODUCT)
            .setType(ProductModel.FULL_QUALIFIED_NAME)
            .setNullable(false);

      CsdlParameter transformerName = new CsdlParameter()
            .setName(PARAM_TRANSFORMER_NAME)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      CsdlParameter parameters = new CsdlParameter()
            .setName(PARAM_PARAMETERS)
            .setType(TransformationParametersComplexType.FULL_QUALIFIED_NAME)
            .setNullable(true);

      return new CsdlAction()
            .setName(ACTION_NAME)
            .setBound(true)
            .setParameters(Arrays.asList(boundProduct, transformerName, parameters))
            .setReturnType(new CsdlReturnType().setType(TransformationModel.FULL_QUALIFIED_NAME));
   }

   @Override
   public String getName()
   {
      return ACTION_NAME;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return FULL_QUALIFIED_NAME;
   }
}
