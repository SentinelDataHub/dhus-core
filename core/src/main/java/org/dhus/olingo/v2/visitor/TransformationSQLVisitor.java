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
package org.dhus.olingo.v2.visitor;

import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

import org.dhus.olingo.v2.datamodel.JobModel;
import org.dhus.olingo.v2.datamodel.enumeration.JobStatusEnum;
import org.dhus.transformation.TransformationStatusUtil;

public class TransformationSQLVisitor extends SQLVisitor
{
   private static final long serialVersionUID = 1L;

   public TransformationSQLVisitor(FilterOption filter, OrderByOption order, TopOption topOption, SkipOption skipOption)
   {
      super("select trf", "from Transformation trf", filter, order, topOption, skipOption);
   }

   /**
    * Method overridden to handle a special case: use IN instead of = (NOT IN instead of !=) because a JobStatus
    * may have several values in DataBase for transformations.
    * <p>
    * {@inheritDoc}
    *
    * @see TransformationStatusUtil#fromJobStatusString(String)
    * @see #visitEnum(EdmEnumType, List)
    */
   @Override
   public Object visitBinaryOperator(BinaryOperatorKind operator, Object left, Object right)
         throws ExpressionVisitException, ODataApplicationException
   {
      String rvalue = String.class.cast(right);
      if (rvalue.charAt(0) == '(') // Only right values returned by visitEnum start with a left parenthesis
      {
         String lvalue = String.class.cast(left);
         switch (operator)
         {
            // Usually 'eq' and 'ne' compare with ONLY one value, this is a special case of the TransformationStatus
            case NE:
               return lvalue + " NOT IN " + rvalue;
            case EQ:
               return lvalue + " IN " + rvalue;
            // No default case, goes to the delegation below
         }
      }
      return super.visitBinaryOperator(operator, left, right); // Delegating to super implementation
   }

   @Override
   public Object visitMember(Member member) throws ExpressionVisitException, ODataApplicationException
   {
      final List<UriResource> uriResourceParts = member.getResourcePath().getUriResourceParts();

      if (uriResourceParts.size() == 1 && uriResourceParts.get(0) instanceof UriResourcePrimitiveProperty)
      {
         String segmentVal = ((UriResourcePrimitiveProperty) uriResourceParts.get(0)).getSegmentValue();
         // switch over the properties to identify the exact property
         switch (segmentVal)
         {
            case JobModel.PROPERTY_ID:
               return "trf.uuid";

            case JobModel.PROPERTY_SUBMISSION_TIME:
               return "trf.creationDate";

            case JobModel.PROPERTY_STATUS:
               return "trf.status";

            default:
               throw new ODataApplicationException("Property not supported: " + segmentVal,
                     HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
         }
      }
      else
      {
         throw new ODataApplicationException("Non-primitive properties are not supported in filter expressions",
               HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
      }
   }

   @Override
   public Object visitEnum(EdmEnumType type, List<String> enumValues)
         throws ExpressionVisitException, ODataApplicationException
   {
      if (type.getFullQualifiedName().equals(JobStatusEnum.FULL_QUALIFIED_NAME))
      {
         String res = enumValues.stream()
               .map(str -> TransformationStatusUtil.fromJobStatusString(str))
               .reduce((s1, s2) -> s1 + ',' + s2)
               .orElseThrow(() -> new ODataApplicationException("Invalid state: no value in enum",
                     HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH));

         if (res.indexOf(',') != -1) // More than one value, enclose with parenthesis
         {
            res = '(' + res + ')';
         }
         return res;
      }
      throw new ODataApplicationException("Enum " + type + " not supported",
            HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
   }

}
