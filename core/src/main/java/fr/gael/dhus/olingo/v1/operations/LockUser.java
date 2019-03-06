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
package fr.gael.dhus.olingo.v1.operations;

import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.olingo.v1.ExpectedException;
import fr.gael.dhus.service.UserService;
import fr.gael.dhus.service.exception.RootNotModifiableException;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.edm.EdmLiteral;
import org.apache.olingo.odata2.api.edm.EdmMultiplicity;
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind;
import org.apache.olingo.odata2.api.edm.provider.Facets;
import org.apache.olingo.odata2.api.edm.provider.FunctionImport;
import org.apache.olingo.odata2.api.edm.provider.FunctionImportParameter;
import org.apache.olingo.odata2.api.edm.provider.ReturnType;
import org.apache.olingo.odata2.api.exception.ODataException;

public class LockUser extends AbstractOperation
{
   private static final UserService USER_SERVICE =
         ApplicationContextProvider.getBean(UserService.class);

   // operation name
   public static final String NAME = "LockUser";

   // operation parameter
   public static final String PARAM_USER = "username";
   public static final String PARAM_REASON = "reason";

   @Override
   public String getName()
   {
      return NAME;
   }

   @Override
   public FunctionImport getFunctionImport()
   {
      // returned success message
      ReturnType returnType = new ReturnType()
            .setMultiplicity(EdmMultiplicity.ZERO_TO_ONE)
            .setTypeName(EdmSimpleTypeKind.String.getFullQualifiedName());

      // the username of a user
      FunctionImportParameter paramUser = new FunctionImportParameter()
            .setName(PARAM_USER)
            .setType(EdmSimpleTypeKind.String)
            .setFacets(new Facets().setNullable(false));

      // the reason of restricting user
      FunctionImportParameter paramReason = new FunctionImportParameter()
            .setName(PARAM_REASON)
            .setType(EdmSimpleTypeKind.String)
            .setFacets(new Facets().setNullable(false));

      List<FunctionImportParameter> params = new ArrayList<>();
      params.add(paramUser);
      params.add(paramReason);

      return new FunctionImport()
            .setName(NAME)
            .setHttpMethod("POST")
            .setParameters(params)
            .setReturnType(returnType);
   }

   @Override
   public Object execute(Map<String, EdmLiteral> parameters) throws ODataException
   {
      String username = parameters.get(PARAM_USER).getLiteral();
      String reason = parameters.get(PARAM_REASON).getLiteral();
      if (username == null || username.isEmpty() || reason == null || reason.isEmpty())
      {
         throw new ExpectedException("At least one parameter is invalid", HttpStatusCodes.BAD_REQUEST);
      }

      try
      {
         User user = USER_SERVICE.getUserByName(username);
         if (user == null)
         {
            throw new ExpectedException("User '" + username + "' does not exist", HttpStatusCodes.BAD_REQUEST);
         }
         USER_SERVICE.lockUser(user, reason);
      }
      catch (RootNotModifiableException ex)
      {
         throw new ExpectedException(ex.getMessage(), HttpStatusCodes.FORBIDDEN);
      }

      return "User " + username + " was successfully locked with reason '" + reason + "'.";
   }

   @Override
   public boolean canExecute(User user)
   {
      return user.getRoles().contains(Role.USER_MANAGER);
   }
}
