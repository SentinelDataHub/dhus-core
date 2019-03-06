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
package org.dhus.olingo.v2.datamodel.action;

import fr.gael.odata.engine.model.ActionModel;

import java.util.Collections;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;

import org.dhus.olingo.v2.datamodel.EvictionModel;
import org.dhus.olingo.v2.web.DHuSODataServlet;

public class CancelEvictionAction implements ActionModel
{
   public static final String ACTION_CANCEL_EVICTION = "CancelEviction";
   public static final FullQualifiedName ACTION_CANCEL_EVICTION_FQN =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ACTION_CANCEL_EVICTION);
   public static final String PARAMETER_EVICTION = "Eviction";

   @Override
   public CsdlAction getAction()
   {
      CsdlParameter evictionParameter = new CsdlParameter()
            .setName(PARAMETER_EVICTION)
            .setType(EvictionModel.FULL_QUALIFIED_NAME)
            .setNullable(false);

      CsdlAction cancelEviction = new CsdlAction()
            .setName(ACTION_CANCEL_EVICTION)
            .setBound(true)
            .setParameters(Collections.singletonList(evictionParameter))
            .setReturnType(new CsdlReturnType().setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()));

      return cancelEviction;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return ACTION_CANCEL_EVICTION_FQN;
   }

   @Override
   public String getName()
   {
      return ACTION_CANCEL_EVICTION;
   }
}
