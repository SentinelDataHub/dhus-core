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

import fr.gael.odata.engine.model.EntityModel;

import java.util.Arrays;
import java.util.Collections;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;

import org.dhus.olingo.v2.web.DHuSODataServlet;

public class ClassModel implements EntityModel
{
   public static final String ENTITY_TYPE_NAME = "Class";
   public static final String ENTITY_SET_NAME = "Classes";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);

   public static final String PROPERTY_ID = "Id";
   public static final String PROPERTY_URI = "Uri";

   @Override
   public CsdlEntityType getEntityType()
   {
      CsdlProperty classId = new CsdlProperty().setName(PROPERTY_ID)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()).setNullable(false);

      CsdlProperty classUri = new CsdlProperty().setName(PROPERTY_URI)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()).setNullable(false);

      CsdlPropertyRef keyRef = new CsdlPropertyRef().setName(PROPERTY_ID);

      CsdlNavigationProperty classNavigationProperty = new CsdlNavigationProperty()
            .setName(ClassModel.ENTITY_SET_NAME)
            .setType(ClassModel.FULL_QUALIFIED_NAME)
            .setCollection(true);

      return new CsdlEntityType().setName(ENTITY_TYPE_NAME)
            .setProperties(Arrays.asList(classId, classUri))
            .setKey(Collections.singletonList(keyRef))
            .setNavigationProperties(Arrays.asList(classNavigationProperty));
   }

   @Override
   public CsdlEntitySet getEntitySet()
   {
      CsdlEntitySet entitySet = EntityModel.super.getEntitySet();

      CsdlNavigationPropertyBinding classNavigPropBinding = new CsdlNavigationPropertyBinding();
      classNavigPropBinding.setTarget(ClassModel.ENTITY_SET_NAME);
      classNavigPropBinding.setPath(ClassModel.ENTITY_SET_NAME);

      return entitySet.setNavigationPropertyBindings(Arrays.asList(classNavigPropBinding));
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
   public String getEntitySetName()
   {
      return ENTITY_SET_NAME;
   }
}
