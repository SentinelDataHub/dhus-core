/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018,2019 GAEL Systems
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

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.dhus.olingo.v2.web.DHuSODataServlet;

import fr.gael.odata.engine.model.EntityModel;

public class TransformationModel implements EntityModel
{
   public static final String ENTITY_TYPE_NAME = "Transformation";
   public static final String ENTITY_SET_NAME = "Transformations";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);

   public static final String NAV_TRANSFORMER = "Transformer";

   @Override
   public CsdlEntityType getEntityType()
   {
      // Define navigation properties
      CsdlNavigationProperty transformer = new CsdlNavigationProperty()
            .setName(NAV_TRANSFORMER)
            .setType(TransformerModel.FULL_QUALIFIED_NAME)
            .setCollection(false)
            .setNullable(true);

      return new CsdlEntityType()
            .setBaseType(JobModel.FULL_QUALIFIED_NAME)
            .setName(ENTITY_TYPE_NAME)
            .setNavigationProperties(Arrays.asList(transformer));
   }

   @Override
   public String getEntitySetName()
   {
      return ENTITY_SET_NAME;
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

   @Override
   public CsdlEntitySet getEntitySet()
   {
      CsdlNavigationPropertyBinding transformer = new CsdlNavigationPropertyBinding()
            .setPath(NAV_TRANSFORMER)
            .setTarget(TransformerModel.ENTITY_SET_NAME);

      CsdlNavigationPropertyBinding product = new CsdlNavigationPropertyBinding()
            .setTarget(ProductModel.ENTITY_SET_NAME)
            .setPath(ProductModel.ENTITY_TYPE_NAME);

      return EntityModel.super.getEntitySet()
            .setNavigationPropertyBindings(Arrays.asList(transformer, product));
   }
}
