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

import org.dhus.olingo.v2.datamodel.complex.CronComplexType;
import org.dhus.olingo.v2.datamodel.complex.ScannerStatusComplexType;
import org.dhus.olingo.v2.web.DHuSODataServlet;

public class ScannerModel implements EntityModel
{
   public static final String ENTITY_TYPE_NAME = "Scanner";
   public static final String ENTITY_SET_NAME = "Scanners";

   public static final String PROPERTY_ID = "Id";
   public static final String PROPERTY_URL = "Url";
   public static final String PROPERTY_PATTERN = "Pattern";
   public static final String PROPERTY_CRON = "Cron";
   public static final String PROPERTY_SOURCE_REMOVE = "SourceRemove";

   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);
   public static final String NAVIGATION_COLLECTIONS = "Collections";

   public static boolean isScannerSubType(String typeFQN)
   {
      return typeFQN.endsWith(ENTITY_TYPE_NAME);
   }

   @Override
   public CsdlEntityType getEntityType()
   {
      CsdlNavigationProperty collectionsNav = new CsdlNavigationProperty()
            .setName(NAVIGATION_COLLECTIONS)
            .setType(CollectionModel.FULL_QUALIFIED_NAME)
            .setCollection(true);

      return new CsdlEntityType()
            .setName(ENTITY_TYPE_NAME)
            .setAbstract(true)
            .setProperties(Arrays.asList(
                  new CsdlProperty()
                        .setName(PROPERTY_ID)
                        .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName())
                        .setNullable(false),
                  new CsdlProperty()
                        .setName(PROPERTY_URL)
                        .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
                        .setNullable(false),
                  new CsdlProperty()
                        .setName(PROPERTY_PATTERN)
                        .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
                        .setNullable(true),
                  new CsdlProperty()
                        .setName(PROPERTY_SOURCE_REMOVE)
                        .setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName())
                        .setNullable(true)
                        .setDefaultValue(Boolean.FALSE.toString()),
                  new CsdlProperty()
                        .setName(PROPERTY_CRON)
                        .setType(CronComplexType.FULL_QUALIFIED_NAME)
                        .setNullable(true),
                  new CsdlProperty()
                        .setName(ScannerStatusComplexType.COMPLEX_TYPE_NAME)
                        .setType(ScannerStatusComplexType.FULL_QUALIFIED_NAME)
                        .setNullable(false)))
            .setKey(Collections.singletonList(new CsdlPropertyRef().setName(PROPERTY_ID)))
            .setNavigationProperties(Arrays.asList(collectionsNav));
   }

   @Override
   public CsdlEntitySet getEntitySet()
   {
      CsdlEntitySet entitySet = EntityModel.super.getEntitySet();

      CsdlNavigationPropertyBinding collectionsNavPropBinding = new CsdlNavigationPropertyBinding()
            .setTarget(CollectionModel.ENTITY_SET_NAME)
            .setPath(NAVIGATION_COLLECTIONS);

      return entitySet.setNavigationPropertyBindings(Arrays.asList(collectionsNavPropBinding));
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
