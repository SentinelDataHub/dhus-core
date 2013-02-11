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

import java.util.Collections;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;
import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.olingo.v2.web.DHuSODataServlet;

import fr.gael.odata.engine.model.ActionModel;

public class RepairProductAction implements ActionModel
{
   public static final String ACTION_REPAIR_PRODUCT = "RepairProduct";
   public static final FullQualifiedName ACTION_REPAIR_PRODUCT_FQN =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ACTION_REPAIR_PRODUCT);
   public static final String PARAMETER_PRODUCT = "ProductId";

   @Override
   public String getName()
   {
      return ACTION_REPAIR_PRODUCT;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return ACTION_REPAIR_PRODUCT_FQN;
   }

   @Override
   public CsdlAction getAction()
   {
      CsdlParameter productParameter = new CsdlParameter()
            .setName(PARAMETER_PRODUCT)
            .setType(ProductModel.FULL_QUALIFIED_NAME)
            .setNullable(false);

      CsdlAction repairProduct = new CsdlAction()
            .setName(ACTION_REPAIR_PRODUCT)
            .setBound(true)
            .setParameters(Collections.singletonList(productParameter))
            // TODO make a better return value?
            .setReturnType(new CsdlReturnType().setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()));

      return repairProduct;
   }

}
