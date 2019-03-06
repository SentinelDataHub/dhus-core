/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
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
package fr.gael.dhus.olingo.v1.visitor;

import fr.gael.dhus.olingo.v1.FunctionalVisitor;
import fr.gael.dhus.olingo.v1.entity.Scanner;
import fr.gael.dhus.olingo.v1.entityset.ScannerEntitySet;

import org.apache.commons.collections4.Transformer;

import org.apache.olingo.odata2.api.edm.EdmTyped;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.uri.expression.PropertyExpression;

public class ScannerFunctionalVisitor extends FunctionalVisitor
{

   @Override
   public Object visitProperty(PropertyExpression pe, String uriLiteral,
         EdmTyped prop)
   {
      Transformer<Scanner, ? extends Object> result;
      switch (uriLiteral)
      {
         case ScannerEntitySet.ID:
            result = new Provider(ScannerEntitySet.ID);
            break;
         case ScannerEntitySet.URL:
            result = new Provider(ScannerEntitySet.URL);
            break;
         case ScannerEntitySet.STATUS:
            result = new Provider(ScannerEntitySet.STATUS);
            break;
         case ScannerEntitySet.STATUS_MESSAGE:
            result = new Provider(ScannerEntitySet.STATUS_MESSAGE);
            break;
         case ScannerEntitySet.ACTIVE:
            result = new Provider(ScannerEntitySet.ACTIVE);
            break;
         case ScannerEntitySet.USERNAME:
            result = new Provider(ScannerEntitySet.USERNAME);
            break;
         case ScannerEntitySet.PATTERN:
            result = new Provider(ScannerEntitySet.PATTERN);
            break;
         // non-filterable properties
         case ScannerEntitySet.PASSWORD:
            throw new IllegalArgumentException(String.format("Property \"%s\" is not filterable", uriLiteral));

         default:
            throw new UnsupportedOperationException("Unknown property: " + uriLiteral);
      }

      return ExecutableExpressionTree.Node.createLeave(result);
   }

   private static class Provider implements Transformer<Scanner, String>
   {
      private final String property;

      public Provider(String property)
      {
         this.property = property;
      }

      @Override
      public String transform(Scanner scanner)
      {
         try
         {
            return (String) scanner.getProperty(property);
         }
         catch (ODataException e)
         {
            // should not happen since we know which property are reachable
            return null;
         }
      }
   }
}
