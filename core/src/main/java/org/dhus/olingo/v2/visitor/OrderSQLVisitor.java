/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019 GAEL Systems
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
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

import org.dhus.api.JobStatus;
import org.dhus.olingo.v2.datamodel.JobModel;
import org.dhus.olingo.v2.datamodel.enumeration.JobStatusEnum;

import org.hibernate.type.StandardBasicTypes;

public class OrderSQLVisitor extends SQLVisitor
{
   private static final long serialVersionUID = 1L;

   public OrderSQLVisitor(FilterOption filter, OrderByOption order, TopOption topOption, SkipOption skipOption)
   {
      super("select ord", "from Order ord", filter, order, topOption, skipOption);
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
               return "concat(ord.orderId.dataStoreName, '-', ord.orderId.productUuid)";

            case JobModel.PROPERTY_ESTIMATED_TIME:
               return "ord.estimatedTime";

            case JobModel.PROPERTY_STATUS:
               return "ord.status";

            case JobModel.PROPERTY_SUBMISSION_TIME:
               return "ord.submissionTime";

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
         try
         {
            hqlParameters.add(new SQLVisitorParameter(positionToUse, JobStatus.valueOf(enumValues.get(0)).toString(), StandardBasicTypes.STRING));
            return HQL_REQUEST_PARAM + positionToUse++;
         }
         catch (Exception e)
         {
            throw new ODataApplicationException("Illegal enum value: " + enumValues.get(0),
                  HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
         }
      }
      throw new ODataApplicationException("Enum " + type + " not supported",
            HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
   }
}
