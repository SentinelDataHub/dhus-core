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

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.dhus.olingo.v2.datamodel.complex.CronComplexType;
import org.dhus.olingo.v2.web.DHuSODataServlet;

import fr.gael.odata.engine.model.EntityModel;

public class SynchronizerModel implements EntityModel
{
   public static final String ABSTRACT_ENTITY_TYPE_NAME = "Synchronizer";
   public static final String ABSTRACT_ENTITY_SET_NAME = "Synchronizers";
   public static final FullQualifiedName ABSTRACT_FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ABSTRACT_ENTITY_TYPE_NAME);

   public static final String PROPERTY_ID = "Id";
   public static final String PROPERTY_CREATED_DATE = "CreationDate";
   public static final String PROPERTY_UPDATED_DATE = "ModificationDate";
   public static final String PROPERTY_LABEL = "Label";
   public static final String PROPERTY_CRON = "Cron";
   public static final String PROPERTY_PAGE_SIZE = "PageSize";
   public static final String NAVIGATION_TARGET_COLLECTION = "TargetCollection";
   public static final String PROPERTY_STATUS = "Status";
   public static final String PROPERTY_STATUS_DATE = "StatusDate";
   public static final String PROPERTY_STATUS_MESSAGE = "StatusMessage";

   @Override
   public CsdlEntityType getEntityType()
   {
      CsdlProperty id = new CsdlProperty()
            .setName(PROPERTY_ID)
            .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName())
            .setNullable(false);
      CsdlPropertyRef propertyRef = new CsdlPropertyRef().setName(PROPERTY_ID);

      CsdlProperty label = new CsdlProperty()
            .setName(PROPERTY_LABEL)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);

      CsdlProperty created = new CsdlProperty()
            .setName(PROPERTY_CREATED_DATE)
            .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
            .setPrecision(3)
            .setNullable(false);

      CsdlProperty updated = new CsdlProperty()
            .setName(PROPERTY_UPDATED_DATE)
            .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
            .setPrecision(3)
            .setNullable(false);

      CsdlProperty cron = new CsdlProperty()
            .setName(PROPERTY_CRON)
            .setType(CronComplexType.FULL_QUALIFIED_NAME)
            .setNullable(false);

      CsdlProperty pageSize = new CsdlProperty()
            .setName(PROPERTY_PAGE_SIZE)
            .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName())
            .setNullable(false)
            .setDefaultValue("2");
      
      CsdlProperty status = new CsdlProperty()
            .setName(PROPERTY_STATUS)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);
      
      CsdlProperty statusDate = new CsdlProperty()
            .setName(PROPERTY_STATUS_DATE)
            .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
            .setPrecision(3)
            .setNullable(true);
      
      CsdlProperty statusMessage = new CsdlProperty()
            .setName(PROPERTY_STATUS_MESSAGE)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);
      
      //define navigation properties
      CsdlNavigationProperty collectionsNav = new CsdlNavigationProperty()
            .setName(NAVIGATION_TARGET_COLLECTION)
            .setType(CollectionModel.FULL_QUALIFIED_NAME)
            .setCollection(true);
      
      CsdlNavigationProperty referencedSources = new CsdlNavigationProperty()
            .setName(ReferencedSourceModel.ENTITY_SET_NAME)
            .setType(ReferencedSourceModel.FULL_QUALIFIED_NAME)
            .setCollection(true);

      return new CsdlEntityType()
            .setName(ABSTRACT_ENTITY_TYPE_NAME)
            .setKey(Collections.singletonList(propertyRef))
            .setProperties(Arrays.asList(id, created, updated, label, cron, pageSize, status, statusDate, statusMessage))
            .setNavigationProperties(Arrays.asList(collectionsNav, referencedSources))
            .setAbstract(true);
   }

   @Override
   public String getName()
   {
      return ABSTRACT_ENTITY_TYPE_NAME;
   }

   @Override
   public String getEntitySetName()
   {
      return ABSTRACT_ENTITY_SET_NAME;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return ABSTRACT_FULL_QUALIFIED_NAME;
   }

   @Override
   public CsdlEntitySet getEntitySet()
   {
      CsdlEntitySet entitySet = EntityModel.super.getEntitySet();
      CsdlNavigationPropertyBinding collectionNavPropBinding = new CsdlNavigationPropertyBinding();
      collectionNavPropBinding.setTarget(CollectionModel.ENTITY_SET_NAME);
      collectionNavPropBinding.setPath(NAVIGATION_TARGET_COLLECTION);

      CsdlNavigationPropertyBinding referencedSourceNavPropBinding = new CsdlNavigationPropertyBinding();
      referencedSourceNavPropBinding.setTarget(ReferencedSourceModel.ENTITY_SET_NAME);
      referencedSourceNavPropBinding.setPath(ReferencedSourceModel.ENTITY_SET_NAME);

      return entitySet.setNavigationPropertyBindings(Arrays.asList(
            collectionNavPropBinding, referencedSourceNavPropBinding));
   }
}
