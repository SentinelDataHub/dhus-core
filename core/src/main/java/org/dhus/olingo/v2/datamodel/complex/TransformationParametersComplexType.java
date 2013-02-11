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
package org.dhus.olingo.v2.datamodel.complex;

import fr.gael.odata.engine.model.ComplexModel;

import java.util.Collections;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;

import org.dhus.olingo.v2.web.DHuSODataServlet;

public class TransformationParametersComplexType implements ComplexModel
{
   public static final String COMPLEX_NAME = "TransformationParameters";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, COMPLEX_NAME);

   @Override
   public CsdlComplexType getComplexType()
   {
      return new CsdlComplexType()
            .setName(COMPLEX_NAME)
            .setOpenType(true)
            .setProperties(Collections.emptyList());
   }

   @Override
   public String getName()
   {
      return COMPLEX_NAME;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return FULL_QUALIFIED_NAME;
   }
}
