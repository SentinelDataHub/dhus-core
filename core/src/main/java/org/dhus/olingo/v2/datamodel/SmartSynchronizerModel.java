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

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;

import org.dhus.olingo.v2.datamodel.complex.SynchronizerSourceComplexType;
import org.dhus.olingo.v2.web.DHuSODataServlet;

public class SmartSynchronizerModel extends SynchronizerModel
{
   public static final String ENTITY_TYPE_NAME = "SmartSynchronizer";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);

   public static final String PROPERTY_PAGE_SIZE = "PageSize";
   public static final String PROPERTY_FILTER_PARAM = "ParamFilter";
   public static final String PROPERTY_FILTER_GEO = "GeoFilter";
   public static final String PROPERTY_ATTEMPTS = "Attempts";
   public static final String PROPERTY_TIMEOUT = "Timeout";
   public static final String PROPERTY_THRESHOLD = "Threshold";
   public static final String PROPERTY_SOURCES = "SynchronizerSources";
// public static final String LINK_TARGET_COLLECTION = "TargetCollection";

   @Override
   public CsdlEntityType getEntityType()
   {
      CsdlProperty pageSize = new CsdlProperty()
            .setName(PROPERTY_PAGE_SIZE)
            .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty filterParam = new CsdlProperty()
            .setName(PROPERTY_FILTER_PARAM)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);

      CsdlProperty filterGeo = new CsdlProperty()
            .setName(PROPERTY_FILTER_GEO)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);

      CsdlProperty attempts = new CsdlProperty()
            .setName(PROPERTY_ATTEMPTS)
            .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty timeout = new CsdlProperty()
            .setName(PROPERTY_TIMEOUT)
            .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty threshold = new CsdlProperty()
            .setName(PROPERTY_THRESHOLD)
            .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty syncSources = new CsdlProperty()
            .setName(PROPERTY_SOURCES)
            .setType(SynchronizerSourceComplexType.FULL_QUALIFIED_NAME)
            .setCollection(true)
            .setNullable(false);

      return new CsdlEntityType()
            .setName(ENTITY_TYPE_NAME)
            .setProperties(Arrays.asList(pageSize, filterParam, filterGeo, attempts, timeout, threshold, syncSources))
            .setAbstract(false)
            .setBaseType(ABSTRACT_FULL_QUALIFIED_NAME);
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
