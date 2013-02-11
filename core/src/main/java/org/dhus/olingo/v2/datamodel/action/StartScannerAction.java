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

import org.dhus.olingo.v2.datamodel.ScannerModel;
import org.dhus.olingo.v2.web.DHuSODataServlet;

public class StartScannerAction implements ActionModel
{
   public static final String START_SCANNER = "StartScanner";
   public static final FullQualifiedName START_SCANNER_FQN =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, START_SCANNER);
   public static final String PARAMETER_SCANNER = "Scanner";

   @Override
   public CsdlAction getAction()
   {
      CsdlParameter scannerParam = new CsdlParameter()
            .setName(PARAMETER_SCANNER)
            .setType(ScannerModel.FULL_QUALIFIED_NAME);

      return new CsdlAction()
            .setName(START_SCANNER)
            .setBound(true)
            .setParameters(Collections.singletonList(scannerParam))
            .setReturnType(new CsdlReturnType().setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()));
   }

   @Override
   public String getName()
   {
      return START_SCANNER;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return START_SCANNER_FQN;
   }
}
