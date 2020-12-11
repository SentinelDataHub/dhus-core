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

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;

import org.dhus.olingo.v2.web.DHuSODataServlet;

public class NodeModel implements EntityModel
{
   public static final String ENTITY_TYPE_NAME = "Node";
   public static final String ENTITY_SET_NAME = "Nodes";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);

   public static final String PROPERTY_CHILDREN_COUNT = "ChildrenCount";
   public static final String PROPERTY_VALUE = "Value";

   @Override
   public CsdlEntityType getEntityType()
   {
      CsdlProperty childrenNumber = new CsdlProperty().setName(PROPERTY_CHILDREN_COUNT)
            .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName());
      CsdlProperty value = new CsdlProperty().setName(PROPERTY_VALUE)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

      CsdlNavigationProperty nodeNavigationProperty = new CsdlNavigationProperty()
            .setName(NodeModel.ENTITY_SET_NAME)
            .setType(NodeModel.FULL_QUALIFIED_NAME)
            .setCollection(true);

      CsdlNavigationProperty attributeNavigationProperty = new CsdlNavigationProperty()
            .setName(AttributeModel.ENTITY_SET_NAME)
            .setType(AttributeModel.FULL_QUALIFIED_NAME)
            .setCollection(true);

      CsdlNavigationProperty classNavigationProperty = new CsdlNavigationProperty()
            .setName(ClassModel.ENTITY_TYPE_NAME)
            .setType(ClassModel.FULL_QUALIFIED_NAME);

      return new CsdlEntityType().setName(ENTITY_TYPE_NAME)
            .setBaseType(ItemModel.FULL_QUALIFIED_NAME)
            .setHasStream(true)
            .setProperties(Arrays.asList(childrenNumber, value))
            .setNavigationProperties(Arrays.asList(nodeNavigationProperty, attributeNavigationProperty, classNavigationProperty));
   }

   @Override
   public CsdlEntitySet getEntitySet()
   {
      CsdlEntitySet entitySet = EntityModel.super.getEntitySet();
      CsdlNavigationPropertyBinding nodeNavigPropBinding = new CsdlNavigationPropertyBinding();
      nodeNavigPropBinding.setTarget(NodeModel.ENTITY_SET_NAME);
      nodeNavigPropBinding.setPath(NodeModel.ENTITY_SET_NAME);

      CsdlNavigationPropertyBinding attributeNavigPropBinding = new CsdlNavigationPropertyBinding();
      attributeNavigPropBinding.setTarget(AttributeModel.ENTITY_SET_NAME);
      attributeNavigPropBinding.setPath(AttributeModel.ENTITY_SET_NAME);

      CsdlNavigationPropertyBinding classNavigPropBinding = new CsdlNavigationPropertyBinding();
      classNavigPropBinding.setTarget(ClassModel.ENTITY_SET_NAME);
      classNavigPropBinding.setPath(ClassModel.ENTITY_TYPE_NAME);

      return entitySet
            .setNavigationPropertyBindings(Arrays.asList(nodeNavigPropBinding, attributeNavigPropBinding, classNavigPropBinding))
            .setIncludeInServiceDocument(false);
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

}
