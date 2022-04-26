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




package org.dhus.olingo.v2.datamodel.action;

import java.util.Arrays;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;
import org.dhus.olingo.v2.datamodel.DeletedProductModel;
import org.dhus.olingo.v2.web.DHuSODataServlet;

import fr.gael.odata.engine.model.ActionModel;

public class DeleteDeletedProductsAction implements ActionModel
{
   public static final String ACTION_DELETE_PRODUCTS = "DeleteProducts";
   public static final FullQualifiedName ACTION_DELETE_PRODUCTS_FQN =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ACTION_DELETE_PRODUCTS);

   public static final String PARAMETER_PRODUCTS = "DeletedProducts";
   public static final String PARAMETER_PRODUCT_LIST= "ProductList";

   @Override
   public CsdlAction getAction()
   {
      CsdlParameter productParameter = new CsdlParameter()
            .setName(PARAMETER_PRODUCTS)
            .setType(DeletedProductModel.FULL_QUALIFIED_NAME)
            .setCollection(true)
            .setNullable(false);

      CsdlParameter uuidParameter = new CsdlParameter()
            .setName(PARAMETER_PRODUCT_LIST)
            .setType(DeletedProductModel.FULL_QUALIFIED_NAME)
            .setCollection(true)
            .setNullable(false);

      CsdlAction deleteProducts = new CsdlAction()
            .setName(ACTION_DELETE_PRODUCTS)
            .setBound(true)
            .setParameters(Arrays.asList(productParameter, uuidParameter))
            .setReturnType(new CsdlReturnType().setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()).setCollection(true));

      return deleteProducts;
   }

   @Override
   public String getName()
   {
      return ACTION_DELETE_PRODUCTS;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return ACTION_DELETE_PRODUCTS_FQN;
   }
}
