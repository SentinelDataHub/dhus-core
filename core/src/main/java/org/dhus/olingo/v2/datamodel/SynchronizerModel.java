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
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;

import org.dhus.olingo.v2.datamodel.complex.CronComplexType;
import org.dhus.olingo.v2.web.DHuSODataServlet;

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
            .setType(EdmPrimitiveTypeKind.Date.getFullQualifiedName())
            .setPrecision(3)
            .setNullable(false);

      CsdlProperty updated = new CsdlProperty()
            .setName(PROPERTY_UPDATED_DATE)
            .setType(EdmPrimitiveTypeKind.Date.getFullQualifiedName())
            .setPrecision(3)
            .setNullable(false);

      CsdlProperty cron = new CsdlProperty()
            .setName(PROPERTY_CRON)
            .setType(CronComplexType.FULL_QUALIFIED_NAME)
            .setNullable(false);

      return new CsdlEntityType()
            .setName(ABSTRACT_ENTITY_TYPE_NAME)
            .setKey(Collections.singletonList(propertyRef))
            .setProperties(Arrays.asList(id, created, updated, label, cron))
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
}
