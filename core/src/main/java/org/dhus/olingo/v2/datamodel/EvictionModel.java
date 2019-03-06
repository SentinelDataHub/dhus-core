/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2018 GAEL Systems
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

import fr.gael.dhus.database.object.config.eviction.EvictionStatusEnum;

import fr.gael.odata.engine.model.EntityModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;

import org.dhus.olingo.v2.datamodel.complex.CronComplexType;
import org.dhus.olingo.v2.web.DHuSODataServlet;

/**
 * Describes the Eviction entity type and Evictions entity set.
 */
public class EvictionModel implements EntityModel
{
   public static final String ENTITY_TYPE_NAME = "Eviction";
   public static final String ENTITY_SET_NAME = "Evictions";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);

   public static final String NAME = "Name";
   public static final String KEEP_PERIOD = "KeepPeriod";
   public static final String KEEP_PERIOD_UNIT = "KeepPeriodUnit";
   public static final String MAX_EVICTED_PRODUCTS = "MaxEvictedProducts";
   public static final String FILTER = "Filter";
   public static final String ORDER_BY = "OrderBy";
   public static final String TARGET_COLLECTION = "TargetCollection";
   public static final String SOFT_EVICTION = "SoftEviction";
   public static final String STATUS = "Status";
   public static final String CRON = "Cron";

   private static final Map<String, Object> DEFAULT_VALUES = new HashMap<>();

   static
   {
      DEFAULT_VALUES.put(KEEP_PERIOD, 10);
      DEFAULT_VALUES.put(KEEP_PERIOD_UNIT, "DAYS");
      DEFAULT_VALUES.put(MAX_EVICTED_PRODUCTS, 1000);
      DEFAULT_VALUES.put(SOFT_EVICTION, false);
      DEFAULT_VALUES.put(STATUS, EvictionStatusEnum.STOPPED.toString());
   }

   @Override
   public CsdlEntityType getEntityType()
   {
      CsdlPropertyRef nameKey = new CsdlPropertyRef().setName(NAME);

      CsdlProperty name = new CsdlProperty().setName(NAME)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty keepPeriod = new CsdlProperty().setName(KEEP_PERIOD)
            .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
            .setNullable(false)
            .setDefaultValue(DEFAULT_VALUES.get(KEEP_PERIOD).toString());

      CsdlProperty keepPeriodUnit = new CsdlProperty().setName(KEEP_PERIOD_UNIT)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false)
            .setDefaultValue(DEFAULT_VALUES.get(KEEP_PERIOD_UNIT).toString());

      CsdlProperty maxEvictedProduct = new CsdlProperty().setName(MAX_EVICTED_PRODUCTS)
            .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
            .setNullable(false)
            .setDefaultValue(DEFAULT_VALUES.get(MAX_EVICTED_PRODUCTS).toString());

      CsdlProperty filter = new CsdlProperty().setName(FILTER)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

      CsdlProperty orderBy = new CsdlProperty().setName(ORDER_BY)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

      CsdlProperty targetCollection = new CsdlProperty().setName(TARGET_COLLECTION)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

      CsdlProperty softEviction = new CsdlProperty().setName(SOFT_EVICTION)
            .setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName())
            .setNullable(false)
            .setDefaultValue(DEFAULT_VALUES.get(SOFT_EVICTION).toString());

      CsdlProperty status = new CsdlProperty().setName(STATUS)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false)
            .setDefaultValue(DEFAULT_VALUES.get(STATUS).toString());

      CsdlProperty cron = new CsdlProperty()
            .setName(CRON)
            .setType(CronComplexType.FULL_QUALIFIED_NAME)
            .setNullable(true);

      return new CsdlEntityType().setName(ENTITY_TYPE_NAME)
            .setProperties(
                  Arrays.asList(
                        name,
                        keepPeriod,
                        keepPeriodUnit,
                        maxEvictedProduct,
                        filter,
                        orderBy,
                        targetCollection,
                        softEviction,
                        status,
                        cron))
            .setKey(Collections.singletonList(nameKey));
   }

   @Override
   public String getEntitySetName()
   {
      return ENTITY_SET_NAME;
   }

   public static Object getDefaultValue(String propertyName)
   {
      return DEFAULT_VALUES.get(propertyName);
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
