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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;

import org.dhus.olingo.v2.datamodel.complex.ChecksumComplexType;
import org.dhus.olingo.v2.datamodel.complex.TimeRangeComplexType;
import org.dhus.olingo.v2.web.DHuSODataServlet;

public class ProductModel implements EntityModel
{
   public static final String ENTITY_TYPE_NAME = "Product";
   public static final String ENTITY_SET_NAME = "Products";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);

   public static final String PROPERTY_CREATIONDATE = "CreationDate";
   public static final String PROPERTY_INGESTIONDATE = "IngestionDate";
   public static final String PROPERTY_MODIFICATIONDATE = "ModificationDate";
   public static final String PROPERTY_CONTENTDATE = "ContentDate";
   public static final String PROPERTY_CHECKSUM = "Checksum";
   public static final String PROPERTY_ONLINE = "Online";
   public static final String PROPERTY_CONTENTGEOMETRY = "ContentGeometry";
   public static final String PROPERTY_FOOTPRINT = "Footprint";
   public static final String PROPERTY_ONDEMAND = "OnDemand";

   @Override
   public CsdlEntityType getEntityType()
   {
      List<CsdlProperty> properties = new ArrayList<>();
      CsdlProperty publicationdate = new CsdlProperty()
            .setName(PROPERTY_CREATIONDATE)
            .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
            .setPrecision(3)
            .setNullable(false);
      properties.add(publicationdate);

      CsdlProperty ingestionDate = new CsdlProperty()
            .setName(PROPERTY_INGESTIONDATE)
            .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
            .setPrecision(3)
            .setNullable(false);
      properties.add(ingestionDate);

      CsdlProperty modificationDate = new CsdlProperty()
            .setName(PROPERTY_MODIFICATIONDATE)
            .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
            .setPrecision(3)
            .setNullable(false);
      properties.add(modificationDate);

      CsdlProperty footprint = new CsdlProperty()
            .setName(PROPERTY_FOOTPRINT)
            .setType(EdmPrimitiveTypeKind.GeographyPolygon.getFullQualifiedName())
            .setNullable(true);
      properties.add(footprint);

      CsdlProperty online = new CsdlProperty()
            .setName(PROPERTY_ONLINE)
            .setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName())
            .setNullable(false);
      properties.add(online);

      CsdlProperty onDemand = new CsdlProperty()
            .setName(PROPERTY_ONDEMAND)
            .setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName())
            .setNullable(false);
      properties.add(onDemand);

      // Complex Properties
      CsdlProperty contentDate = new CsdlProperty()
            .setName(PROPERTY_CONTENTDATE)
            .setType(TimeRangeComplexType.FULL_QUALIFIED_NAME)
            .setNullable(false);
      properties.add(contentDate);

      CsdlProperty checksum = new CsdlProperty()
            .setName(PROPERTY_CHECKSUM)
            .setType(ChecksumComplexType.FULL_QUALIFIED_NAME)
            .setCollection(true)
            .setNullable(true);
      properties.add(checksum);

      // Define navigation properties
      CsdlNavigationProperty derivedProductNavigationProperty = new CsdlNavigationProperty()
            .setName(ProductModel.ENTITY_SET_NAME)
            .setType(ProductModel.FULL_QUALIFIED_NAME)
            .setCollection(true);

      CsdlNavigationProperty orderNavigationProperty = new CsdlNavigationProperty()
            .setName(OrderModel.ENTITY_TYPE_NAME)
            .setType(OrderModel.FULL_QUALIFIED_NAME)
            .setCollection(false);

      return new CsdlEntityType().setName(ENTITY_TYPE_NAME)
            .setBaseType(NodeModel.FULL_QUALIFIED_NAME)
            .setProperties(properties)
            .setHasStream(true)
            .setNavigationProperties(Arrays.asList(derivedProductNavigationProperty, orderNavigationProperty));
   }

   @Override
   public CsdlEntitySet getEntitySet()
   {
      CsdlEntitySet entitySet = EntityModel.super.getEntitySet();

      CsdlNavigationPropertyBinding productNavigPropBinding = new CsdlNavigationPropertyBinding();
      productNavigPropBinding.setTarget(ProductModel.ENTITY_SET_NAME);
      productNavigPropBinding.setPath(ProductModel.ENTITY_SET_NAME);

      CsdlNavigationPropertyBinding nodeNavigPropBinding = new CsdlNavigationPropertyBinding();
      nodeNavigPropBinding.setTarget(NodeModel.ENTITY_SET_NAME);
      nodeNavigPropBinding.setPath(NodeModel.ENTITY_SET_NAME);

      CsdlNavigationPropertyBinding attributeNavigPropBinding = new CsdlNavigationPropertyBinding();
      attributeNavigPropBinding.setTarget(AttributeModel.ENTITY_SET_NAME);
      attributeNavigPropBinding.setPath(AttributeModel.ENTITY_SET_NAME);

      CsdlNavigationPropertyBinding classNavigPropBinding = new CsdlNavigationPropertyBinding();
      classNavigPropBinding.setTarget(ClassModel.ENTITY_SET_NAME);
      classNavigPropBinding.setPath(ClassModel.ENTITY_TYPE_NAME);

      CsdlNavigationPropertyBinding orderNavigPropBinding = new CsdlNavigationPropertyBinding();
      orderNavigPropBinding.setTarget(OrderModel.ENTITY_SET_NAME);
      orderNavigPropBinding.setPath(OrderModel.ENTITY_TYPE_NAME);

      return entitySet.setNavigationPropertyBindings(
            Arrays.asList(
                  productNavigPropBinding,
                  nodeNavigPropBinding,
                  attributeNavigPropBinding,
                  classNavigPropBinding,
                  orderNavigPropBinding)
      );
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
