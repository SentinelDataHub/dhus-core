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

import org.apache.commons.collections4.Transformer;
import org.apache.olingo.odata2.api.edm.EdmTyped;
import org.apache.olingo.odata2.api.uri.expression.PropertyExpression;

import fr.gael.dhus.olingo.v1.FunctionalVisitor;
import fr.gael.dhus.olingo.v1.entity.Collection;
import fr.gael.dhus.olingo.v1.entityset.CollectionEntitySet;

public class CollectionFunctionalVisitor extends FunctionalVisitor
{
   
   @Override
   public Object visitProperty(PropertyExpression pe, String uriLiteral,
         EdmTyped prop)
   {
      Transformer<Collection, ? extends Object> result;
      switch(uriLiteral)
      {
         case CollectionEntitySet.NAME:
            result = new NameProvider();
            break;
         case CollectionEntitySet.DESCRIPTION:
            result = new DescriptionProvider();
            break;
         case CollectionEntitySet.UUID:
            result = new UUIDProvider();
            break;
            
         default:
            throw new UnsupportedOperationException("Unknown property: " + 
                  uriLiteral);
      }
      
      return ExecutableExpressionTree.Node.createLeave(result);
   }
   
   private static class NameProvider implements Transformer<Collection, String>
   {
      @Override
      public String transform(Collection collection)
      {
         return collection.getName();
      }
   }
   
   private static class DescriptionProvider implements Transformer<Collection, String>
   {
      @Override
      public String transform(Collection collection)
      {
         return collection.getDescription();
      }  
   }
   
   private static class UUIDProvider implements Transformer<Collection, String>
   {
      @Override
      public String transform(Collection collection)
      {
         return collection.getUUID();
      }
   }
}
