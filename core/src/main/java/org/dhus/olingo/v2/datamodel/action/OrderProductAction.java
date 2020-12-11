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

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;

import org.dhus.olingo.v2.datamodel.OrderModel;
import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.olingo.v2.web.DHuSODataServlet;

/**
 * To order offline products (fetch operation of Async DataStores).
 */
public class OrderProductAction implements ActionModel
{
   public static final String ACTION_ORDER_PRODUCT = "Order";
   public static final FullQualifiedName ACTION_ORDER_PRODUCT_FQN =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ACTION_ORDER_PRODUCT);

   public static final String PARAMETER_PRODUCT = "Product";

   @Override
   public CsdlAction getAction()
   {
      CsdlParameter productParameter = new CsdlParameter()
            .setName(PARAMETER_PRODUCT)
            .setType(ProductModel.FULL_QUALIFIED_NAME)
            .setNullable(false);

      CsdlAction order = new CsdlAction()
            .setName(ACTION_ORDER_PRODUCT)
            .setBound(true)
            .setParameters(Arrays.asList(productParameter))
            .setReturnType(new CsdlReturnType().setType(OrderModel.FULL_QUALIFIED_NAME));

      return order;
   }

   @Override
   public String getName()
   {
      return ACTION_ORDER_PRODUCT;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return ACTION_ORDER_PRODUCT_FQN;
   }
}
