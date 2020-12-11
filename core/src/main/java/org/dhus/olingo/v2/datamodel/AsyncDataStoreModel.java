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

import fr.gael.odata.engine.model.EntityModel;

import java.util.Arrays;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;

import org.dhus.olingo.v2.datamodel.complex.PatternReplaceComplexType;
import org.dhus.olingo.v2.web.DHuSODataServlet;

/**
 * Describes the AsyncDataStore abstract entity type.
 */
public class AsyncDataStoreModel implements EntityModel
{
   public static final String ENTITY_TYPE_NAME = "AsyncDataStore";
   public static final String ENTITY_SET_NAME = "AsyncDataStores";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);

   public static final String PROPERTY_HFS_LOCATION = "HFSLocation";
   public static final String PROPERTY_IS_MASTER = "IsMaster";
   public static final String PROPERTY_PATRE_IN = "PatternReplaceIn";
   public static final String PROPERTY_PATRE_OUT = "PatternReplaceOut";

   public static final String PROPERTY_MAX_PENDING_REQUESTS = "MaxPendingRequests";
   public static final String PROPERTY_MAX_RUNNING_REQUESTS = "MaxRunningRequests";

   public static final String PROPERTY_MAX_PFRPU = "MaxParallelFetchRequestsPerUser";

   @Override
   public CsdlEntityType getEntityType()
   {
      CsdlProperty hfsLocation = new CsdlProperty()
            .setName(PROPERTY_HFS_LOCATION)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty isMaster = new CsdlProperty()
            .setName(PROPERTY_IS_MASTER)
            .setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName())
            .setNullable(true);

      CsdlProperty patreIn = new CsdlProperty()
            .setName(PROPERTY_PATRE_IN)
            .setType(PatternReplaceComplexType.FULL_QUALIFIED_NAME)
            .setNullable(true);

      CsdlProperty patreOut = new CsdlProperty()
            .setName(PROPERTY_PATRE_OUT)
            .setType(PatternReplaceComplexType.FULL_QUALIFIED_NAME)
            .setNullable(true);

      CsdlProperty maxPendingRequests = new CsdlProperty()
            .setName(PROPERTY_MAX_PENDING_REQUESTS)
            .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());

      CsdlProperty maxRunningRequests = new CsdlProperty()
            .setName(PROPERTY_MAX_RUNNING_REQUESTS)
            .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());

      CsdlProperty maxQueryPerUser = new CsdlProperty()
            .setName(PROPERTY_MAX_PFRPU)
            .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
            .setNullable(true);

      return new CsdlEntityType()
            .setBaseType(DataStoreModel.ABSTRACT_FULL_QUALIFIED_NAME)
            .setName(ENTITY_TYPE_NAME)
            .setProperties(Arrays.asList(
                  hfsLocation,
                  isMaster,
                  patreIn,
                  patreOut,
                  maxPendingRequests,
                  maxRunningRequests,
                  maxQueryPerUser))
            .setAbstract(true);
   }

   @Override
   public CsdlEntitySet getEntitySet()
   {
      CsdlEntitySet entitySet = EntityModel.super.getEntitySet();
      return entitySet.setIncludeInServiceDocument(false);
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
