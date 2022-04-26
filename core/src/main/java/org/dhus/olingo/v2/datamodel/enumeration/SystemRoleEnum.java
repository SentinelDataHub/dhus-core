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
package org.dhus.olingo.v2.datamodel.enumeration;

import fr.gael.dhus.database.object.Role;

import fr.gael.odata.engine.model.EnumModel;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEnumMember;
import org.apache.olingo.commons.api.edm.provider.CsdlEnumType;

import org.dhus.olingo.v2.web.DHuSODataServlet;

/**
 * System roles. Automatically declares all roles defined in enum {@link Role}.
 *
 * Uses the declaration order in enum {@link Role}, starting at zero, use method {@link Role#ordinal()};
 * To access the Role from the EnumValue, use {@link Role#values()}[enumvalue];
 */
public class SystemRoleEnum implements EnumModel
{
   public static final String ENUM_NAME = "SystemRole";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENUM_NAME);

   @Override
   public CsdlEnumType getEnumType()
   {
      return new CsdlEnumType().setName(ENUM_NAME)
            .setMembers(Arrays.<Role>stream(Role.values()).map(SystemRoleEnum::roleToEnumMember).collect(Collectors.toList()));
   }

   private static CsdlEnumMember roleToEnumMember(Role role)
   {
      return new CsdlEnumMember().setName(role.name());
   }

   @Override
   public String getName()
   {
      return ENUM_NAME;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return FULL_QUALIFIED_NAME;
   }
}
