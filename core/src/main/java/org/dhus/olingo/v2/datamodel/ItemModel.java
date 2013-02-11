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
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;

import org.dhus.olingo.v2.web.DHuSODataServlet;

public class ItemModel implements EntityModel
{
   public static final String ENTITY_TYPE_NAME = "Item";
   public static final String ENTITY_SET_NAME = "Items";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);

   public static final String PROPERTY_ID = "Id";
   public static final String PROPERTY_NAME = "Name";
   public static final String PROPERTY_CONTENTTYPE = "ContentType";
   public static final String PROPERTY_CONTENTLENGTH = "ContentLength";

   @Override
   public CsdlEntityType getEntityType()
   {
      CsdlProperty uuid = new CsdlProperty()
            .setName(PROPERTY_ID)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty name = new CsdlProperty()
            .setName(PROPERTY_NAME)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);
      CsdlProperty contentType = new CsdlProperty()
            .setName(PROPERTY_CONTENTTYPE)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);
      CsdlProperty contentLength = new CsdlProperty()
            .setName(PROPERTY_CONTENTLENGTH)
            .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName())
            .setNullable(false);

      CsdlPropertyRef nameKey = new CsdlPropertyRef().setName(PROPERTY_ID);

      return new CsdlEntityType().setName(ENTITY_TYPE_NAME)
            .setProperties(Arrays.<CsdlProperty>asList(uuid, contentLength, contentType, name))
            .setKey(Collections.<CsdlPropertyRef>singletonList(nameKey))
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
