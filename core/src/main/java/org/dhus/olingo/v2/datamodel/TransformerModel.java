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
package org.dhus.olingo.v2.datamodel;

import java.util.Arrays;
import java.util.Collections;

import fr.gael.odata.engine.model.EntityModel;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.dhus.olingo.v2.datamodel.complex.DescriptiveParameterComplexType;
import org.dhus.olingo.v2.web.DHuSODataServlet;

public class TransformerModel implements EntityModel
{
   public static final String ENTITY_NAME = "Transformer";
   public static final String ENTITY_SET_NAME = "Transformers";
   public static final FullQualifiedName FULL_QUALIFIED_NAME = new FullQualifiedName(
         DHuSODataServlet.NAMESPACE, ENTITY_NAME);

   public static final String PROPERTY_NAME = "Name";
   public static final String PROPERTY_DESCRIPTION = "Description";
   public static final String PROPERTY_PARAMETERS = "Parameters";
   public static final String NAV_TRANSFORMATION = "Transformations";

   @Override
   public CsdlEntityType getEntityType()
   {
      CsdlProperty name = new CsdlProperty()
            .setName(PROPERTY_NAME)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty description = new CsdlProperty()
            .setName(PROPERTY_DESCRIPTION)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

      CsdlProperty parameters = new CsdlProperty()
            .setName(PROPERTY_PARAMETERS)
            .setType(DescriptiveParameterComplexType.FULL_QUALIFIED_NAME)
            .setCollection(true)
            .setNullable(false);

      CsdlNavigationProperty transformations = new CsdlNavigationProperty();
      transformations.setType(TransformationModel.FULL_QUALIFIED_NAME);
      transformations.setName(NAV_TRANSFORMATION);
      transformations.setCollection(true);

      return new CsdlEntityType()
            .setName(ENTITY_NAME)
            .setKey(Collections.singletonList(new CsdlPropertyRef().setName(PROPERTY_NAME)))
            .setProperties(Arrays.asList(name, description, parameters))
            .setNavigationProperties(Collections.emptyList())
            .setNavigationProperties(Collections.singletonList(transformations));
   }

   @Override
   public String getEntitySetName()
   {
      return ENTITY_SET_NAME;
   }

   @Override
   public String getName()
   {
      return ENTITY_NAME;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return FULL_QUALIFIED_NAME;
   }

   @Override
   public CsdlEntitySet getEntitySet()
   {
      return EntityModel.super.getEntitySet().setNavigationPropertyBindings(
            Collections.singletonList(
                  new CsdlNavigationPropertyBinding()
                        .setPath(NAV_TRANSFORMATION)
                        .setTarget(TransformationModel.ENTITY_SET_NAME)
            )
      );
   }
}
