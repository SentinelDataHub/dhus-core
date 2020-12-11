/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2018 GAEL Systems
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
import fr.gael.dhus.datastore.scanner.ScannerException;
import fr.gael.dhus.olingo.v1.ExpectedException;
import fr.gael.dhus.olingo.v1.entityset.ScannerEntitySet;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.util.Collections;
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

import org.dhus.scanner.ScannerContainer;

/**
 *
 */
public class StartScanner extends AbstractOperation
{
   private static final ScannerContainer SCANNER_CONTAINER =
         ApplicationContextProvider.getBean(ScannerContainer.class);

   // operation name
   public static final String NAME = "StartScanner";

   // operation parameter
   public static final String PARAM_SCANNER = "id";

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

      // the id of a scanner
      FunctionImportParameter paramScanner = new FunctionImportParameter()
            .setName(PARAM_SCANNER)
            .setType(EdmSimpleTypeKind.Int64)
            .setFacets(new Facets().setNullable(false));

      return new FunctionImport()
            .setName(NAME)
            .setHttpMethod("POST")
            .setParameters(Collections.singletonList(paramScanner))
            .setReturnType(returnType);
   }

   @Override
   public Object execute(Map<String, EdmLiteral> parameters) throws ODataException
   {
      Long scannerId;
      try
      {
         scannerId = Long.decode(parameters.get(PARAM_SCANNER).getLiteral());
      }
      catch (NumberFormatException e)
      {
         throw new ExpectedException(e.getMessage(), HttpStatusCodes.BAD_REQUEST);
      }

      try
      {
         SCANNER_CONTAINER.processScan(scannerId);
      }
      catch (ScannerException.ScannerNotFoundException e)
      {
         throw new ExpectedException.InvalidKeyException(scannerId.toString(), ScannerEntitySet.ENTITY_NAME);
      }
      catch (ScannerException.ScannerAlreadyRunningException e)
      {
         throw new ExpectedException(e.getMessage(), HttpStatusCodes.FORBIDDEN);
      }
      catch (ScannerException e)
      {
         throw new ODataException(e.getMessage());
      }

      return String.format("Scanner #%d started.", scannerId);
   }

   @Override
   public boolean canExecute(User user)
   {
      return user.getRoles().contains(Role.UPLOAD);
   }
}
