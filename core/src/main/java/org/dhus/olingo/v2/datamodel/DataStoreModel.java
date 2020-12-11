/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2019 GAEL Systems
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

/**
 * Describes the DataStores entity types and set.
 */
public class DataStoreModel implements EntityModel
{
   public static final String ABSTRACT_ENTITY_TYPE_NAME = "DataStore";
   public static final String ABSTRACT_ENTITY_SET_NAME = "DataStores";
   public static final FullQualifiedName ABSTRACT_FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ABSTRACT_ENTITY_TYPE_NAME);

   public static final String PROPERTY_NAME = "Name";
   public static final String PROPERTY_RESTRICTION = "Restriction";
   public static final String PROPERTY_PRIORITY = "Priority";

   public static final String PROPERTY_CURRENTSIZE = "CurrentSize";
   public static final String PROPERTY_MAXIMUMSIZE = "MaximumSize";
   public static final String PROPERTY_AUTOEVICTION = "AutoEviction";
   public static final String PROPERTY_FILTER = "Filter";

   public static final String NAVIGATION_PRODUCTS = "Products";
   public static final String NAVIGATION_EVICTION = "Eviction";

   public static boolean isDataStoreSubType(String type)
   {
      return type != null && type.endsWith(ABSTRACT_ENTITY_TYPE_NAME);
   }

   @Override
   public CsdlEntityType getEntityType()
   {
      // define properties
      CsdlProperty name = new CsdlProperty()
            .setName(PROPERTY_NAME)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      // TODO only for SYSTEM_MANAGER users
      CsdlProperty restriction = new CsdlProperty()
            .setName(PROPERTY_RESTRICTION)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty priority = new CsdlProperty()
            .setName(PROPERTY_PRIORITY)
            .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty maximumSize = new CsdlProperty()
            .setName(PROPERTY_MAXIMUMSIZE)
            .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName());

      CsdlProperty currentSize = new CsdlProperty()
            .setName(PROPERTY_CURRENTSIZE)
            .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName());

      CsdlProperty autoEviction = new CsdlProperty()
            .setName(PROPERTY_AUTOEVICTION)
            .setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty filter = new CsdlProperty()
            .setName(PROPERTY_FILTER)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);

      // define reference property
      CsdlPropertyRef propertyRef = new CsdlPropertyRef().setName(PROPERTY_NAME);

      //define navigation properties
      CsdlNavigationProperty productNavigationProperty = new CsdlNavigationProperty()
            .setName(NAVIGATION_PRODUCTS)
            .setType(ProductModel.FULL_QUALIFIED_NAME)
            .setCollection(true);

      CsdlNavigationProperty evictionNavigationProperty = new CsdlNavigationProperty()
            .setName(NAVIGATION_EVICTION)
            .setType(EvictionModel.FULL_QUALIFIED_NAME);

      // TODO handle authorizations
      return new CsdlEntityType()
            .setName(ABSTRACT_ENTITY_TYPE_NAME)
            .setProperties(Arrays.asList(
                  name,
                  restriction,
                  priority,
                  maximumSize,
                  currentSize,
                  autoEviction,
                  filter))
            .setKey(Collections.singletonList(propertyRef))
            .setAbstract(true)
            .setNavigationProperties(Arrays.asList(
                  productNavigationProperty,
                  evictionNavigationProperty
            ));
   }

   @Override
   public CsdlEntitySet getEntitySet()
   {
      CsdlEntitySet entitySet = EntityModel.super.getEntitySet();

      CsdlNavigationPropertyBinding productNavigPropBinding = new CsdlNavigationPropertyBinding();
      productNavigPropBinding.setTarget(ProductModel.ENTITY_SET_NAME);
      productNavigPropBinding.setPath(NAVIGATION_PRODUCTS);

      CsdlNavigationPropertyBinding evictionNavigPropBinding = new CsdlNavigationPropertyBinding();
      evictionNavigPropBinding.setTarget(EvictionModel.ENTITY_SET_NAME);
      evictionNavigPropBinding.setPath(NAVIGATION_EVICTION);

      return entitySet.setNavigationPropertyBindings(Arrays.asList(productNavigPropBinding, evictionNavigPropBinding));
   }

   @Override
   public String getEntitySetName()
   {
      return ABSTRACT_ENTITY_SET_NAME;
   }

   @Override
   public String getName()
   {
      return ABSTRACT_ENTITY_TYPE_NAME;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return ABSTRACT_FULL_QUALIFIED_NAME;
   }
}
