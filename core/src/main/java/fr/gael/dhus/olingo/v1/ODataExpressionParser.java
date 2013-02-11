/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2019 GAEL Systems
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

import fr.gael.dhus.olingo.v1.entityset.ProductEntitySet;

import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.exception.ODataMessageException;
import org.apache.olingo.odata2.api.rt.RuntimeDelegate;
import org.apache.olingo.odata2.api.uri.UriParser;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;

public class ODataExpressionParser
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final Edm EDM;

   private static final ODataExpressionParser PDT_EXP_PARSER;

   static
   {
      Edm edm = null;
      try
      {
         edm = RuntimeDelegate.createEdm(new Model());
      }
      catch (Throwable t)
      {
         LOGGER.fatal("Could not generate the EDM to parse OData filter/orderby expressions", t);
      }
      EDM = edm;

      ODataExpressionParser expParser = null;

      if (EDM != null)
      {
         try
         {
            EdmEntityType productType = EDM.getEntityType(Model.NAMESPACE, ProductEntitySet.ENTITY_NAME);
            expParser = new ODataExpressionParser(productType);
         }
         catch (Throwable t)
         {
            LOGGER.fatal("Could not get the Product entity type to parse OData filter/orderby expressions", t);
         }
      }
      PDT_EXP_PARSER = expParser;
   }

   public static ODataExpressionParser getProductExpressionParser() throws ODataMessageException, ODataApplicationException
   {
      if (PDT_EXP_PARSER != null)
      {
         return PDT_EXP_PARSER;
      }

      throw new ODataApplicationException("Could not create a Product filter/orderby expression parser",
            Locale.ENGLISH, HttpStatusCodes.INTERNAL_SERVER_ERROR);
   }

   private final EdmEntityType type;

   public ODataExpressionParser(EdmEntityType type)
   {
      this.type = type;
   }

   public FilterExpression parseFilterString(String filter) throws ODataMessageException, ODataApplicationException
   {
      return UriParser.parseFilter(EDM, type, filter);
   }

   public OrderByExpression parseOrderByString(String orderBy) throws ODataMessageException, ODataApplicationException
   {
      return UriParser.parseOrderBy(EDM, type, orderBy);
   }
}
