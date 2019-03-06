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
package fr.gael.dhus.olingo.v1;

import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.exception.ODataMessageException;
import org.apache.olingo.odata2.api.rt.RuntimeDelegate;
import org.apache.olingo.odata2.api.uri.UriParser;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;

import fr.gael.dhus.olingo.v1.entityset.ProductEntitySet;

/**
 *
 */
public class ODataExpressionParser
{
   private final Edm edm;
   private final EdmEntityType type;

   private ODataExpressionParser(Edm edm, EdmEntityType type)
   {
      this.edm = edm;
      this.type = type;
   }

   public static ODataExpressionParser getProductExpressionParser()
   {
      try
      {
         Edm edm = RuntimeDelegate.createEdm(new Model());
         EdmEntityType productType = edm.getEntityType(Model.NAMESPACE, ProductEntitySet.ENTITY_NAME);

         return new ODataExpressionParser(edm, productType);
      }
      catch (EdmException e)
      {
         throw new IllegalStateException(e);
      }
   }

   public FilterExpression parseFilterString(String filter) throws ODataMessageException
   {
      return UriParser.parseFilter(edm, type, filter);
   }

   public OrderByExpression parseOrderByString(String orderBy) throws ODataMessageException
   {
      return UriParser.parseOrderBy(edm, type, orderBy);
   }
}
