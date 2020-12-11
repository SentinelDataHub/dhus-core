/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018,2019 GAEL Systems
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

import java.util.Arrays;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEnumMember;
import org.apache.olingo.commons.api.edm.provider.CsdlEnumType;
import org.dhus.api.JobStatus;
import org.dhus.olingo.v2.web.DHuSODataServlet;

import fr.gael.odata.engine.model.EnumModel;

public class JobStatusEnum implements EnumModel
{
   public static final String ENUM_NAME = "JobStatus";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENUM_NAME);

   public static final CsdlEnumMember PENDING = new CsdlEnumMember()
         .setName(JobStatus.PENDING.name())
         .setValue(JobStatus.PENDING.value().toString());

   public static final CsdlEnumMember RUNNING = new CsdlEnumMember()
         .setName(JobStatus.RUNNING.name())
         .setValue(JobStatus.RUNNING.value().toString());

   public static final CsdlEnumMember PAUSED = new CsdlEnumMember()
         .setName(JobStatus.PAUSED.name())
         .setValue(JobStatus.PAUSED.value().toString());

   public static final CsdlEnumMember COMPLETED = new CsdlEnumMember()
         .setName(JobStatus.COMPLETED.name())
         .setValue(JobStatus.COMPLETED.value().toString());

   public static final CsdlEnumMember FAILED = new CsdlEnumMember()
         .setName(JobStatus.FAILED.name())
         .setValue(JobStatus.FAILED.value().toString());

   public static final CsdlEnumMember UNKNOWN = new CsdlEnumMember()
         .setName(JobStatus.UNKNOWN.name())
         .setValue(JobStatus.UNKNOWN.value().toString());

   @Override
   public CsdlEnumType getEnumType()
   {
      CsdlEnumType enumeration = new CsdlEnumType()
            .setName(ENUM_NAME)
            .setUnderlyingType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
            .setMembers(Arrays.asList(PENDING, RUNNING, PAUSED, COMPLETED, FAILED, UNKNOWN));
      return enumeration;
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
