/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018-2020 GAEL Systems
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

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;

import org.dhus.olingo.v2.web.DHuSODataServlet;

/**
 * Describes the PdgsDataStore entity type.
 */
public class PdgsDataStoreModel extends HttpAsyncDataStoreModel
{

   public static final String ENTITY_TYPE_NAME = "PDGSDataStore";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);

   @Override
   public CsdlEntityType getEntityType()
   {
      return new CsdlEntityType()
            .setBaseType(HttpAsyncDataStoreModel.FULL_QUALIFIED_NAME)
            .setName(ENTITY_TYPE_NAME);
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
