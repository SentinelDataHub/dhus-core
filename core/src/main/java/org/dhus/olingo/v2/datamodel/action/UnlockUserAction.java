/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2020 GAEL Systems
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

import java.util.Arrays;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;

import org.dhus.olingo.v2.datamodel.UserModel;
import org.dhus.olingo.v2.web.DHuSODataServlet;

public class UnlockUserAction implements ActionModel
{
   public static final String ACTION_UNLOCK_USER = "Unlock";
   public static final FullQualifiedName ACTION_UNLOCK_USER_FQN =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ACTION_UNLOCK_USER);

   public static final String PARAMETER_USER = "User";
   public static final String PARAMETER_ID = "Id";

   @Override
   public CsdlAction getAction()
   {
      CsdlParameter user = new CsdlParameter()
            .setName(PARAMETER_USER)
            .setType(UserModel.FULL_QUALIFIED_NAME)
            .setNullable(false);

      CsdlParameter id = new CsdlParameter()
            .setName(PARAMETER_ID)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);

      return new CsdlAction().setName(ACTION_UNLOCK_USER)
            .setBound(true)
            .setParameters(Arrays.asList(user, id))
            .setReturnType(new CsdlReturnType().setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()));
   }

   @Override
   public String getName()
   {
      return ACTION_UNLOCK_USER;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return ACTION_UNLOCK_USER_FQN;
   }
}
