/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2021 GAEL Systems
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
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.dhus.olingo.v2.web.DHuSODataServlet;

import fr.gael.odata.engine.model.EntityModel;

public class ProductSourceModel implements EntityModel
{
   public static final String ENTITY_TYPE_NAME = "ProductSource";
   public static final String ENTITY_SET_NAME = "ProductSources";

   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);

   public static final String PROPERTY_ID = "Id";
   public static final String PROPERTY_URL = "Url";
   public static final String PROPERTY_LOGIN = "Login";
   public static final String PROPERTY_PASSWORD = "Password";
   public static final String PROPERTY_REMOTE_INCOMING = "RemoteIncoming";
   public static final String PROPERTY_LISTABLE = "Listable";

   @Override
   public CsdlEntityType getEntityType()
   {
      CsdlProperty id = new CsdlProperty()
            .setName(PROPERTY_ID)
            .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
            .setNullable(false);
      CsdlPropertyRef propertyRef = new CsdlPropertyRef().setName(PROPERTY_ID);

      CsdlProperty url = new CsdlProperty()
            .setName(PROPERTY_URL)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      CsdlProperty username = new CsdlProperty()
            .setName(PROPERTY_LOGIN)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);

      CsdlProperty password = new CsdlProperty()
            .setName(PROPERTY_PASSWORD)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);
      
      CsdlProperty remoteIncoming = new CsdlProperty()
            .setName(PROPERTY_REMOTE_INCOMING)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

      CsdlProperty listable = new CsdlProperty()
            .setName(PROPERTY_LISTABLE)
            .setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName())
            .setDefaultValue("true");

      return new CsdlEntityType()
            .setName(ENTITY_TYPE_NAME)
            .setProperties(Arrays.asList(
                  id,
                  url,
                  username,
                  password,
                  remoteIncoming,
                  listable))
            .setKey(Collections.singletonList(propertyRef));
   }  

   @Override
   public String getName()
   {
      return ENTITY_TYPE_NAME;
   }

   @Override
   public String getEntitySetName()
   {
      return ENTITY_SET_NAME;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return FULL_QUALIFIED_NAME;
   }
}
