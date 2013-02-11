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

import java.util.Arrays;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;
import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.olingo.v2.web.DHuSODataServlet;

import fr.gael.odata.engine.model.ActionModel;

public class RepairProductsAction implements ActionModel
{
   public static final String ACTION_REPAIR_PRODUCTS = "RepairProducts";
   public static final FullQualifiedName ACTION_REPAIR_PRODUCTS_FQN =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ACTION_REPAIR_PRODUCTS);

   public static final String PARAMETER_PRODUCTS = "Products"; // bound entity set

   public static final String PARAMETER_FILTER = "Filter";
   public static final String PARAMETER_ORDERBY = "OrderBy";
   public static final String PARAMETER_MAXREP = "MaxRepairedProducts";
   public static final String PARAMETER_SKIP = "Skip";

   @Override
   public String getName()
   {
      return ACTION_REPAIR_PRODUCTS;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return ACTION_REPAIR_PRODUCTS_FQN;
   }

   @Override
   public CsdlAction getAction()
   {
      CsdlParameter productsParameter = new CsdlParameter()
            .setName(PARAMETER_PRODUCTS)
            .setType(ProductModel.FULL_QUALIFIED_NAME)
            .setCollection(true)
            .setNullable(false);

      CsdlParameter filterParameter = new CsdlParameter()
            .setName(PARAMETER_FILTER)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);

      CsdlParameter orderByParameter = new CsdlParameter()
            .setName(PARAMETER_ORDERBY)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);

      CsdlParameter maxRepairedProducts = new CsdlParameter()
            .setName(PARAMETER_MAXREP)
            .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
            .setNullable(true);

      CsdlParameter skip = new CsdlParameter()
            .setName(PARAMETER_SKIP)
            .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
            .setNullable(true);

      CsdlAction repairProducts = new CsdlAction()
            .setName(ACTION_REPAIR_PRODUCTS)
            .setBound(true)
            .setParameters(Arrays.asList(productsParameter, filterParameter, orderByParameter, maxRepairedProducts, skip))
            // TODO make a monitoring entity as return type of this
            // similar to on-demand processing or new LTA interface
            .setReturnType(new CsdlReturnType().setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()));

      return repairProducts;
   }

}
