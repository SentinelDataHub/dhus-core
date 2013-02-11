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

import org.dhus.metrics.embed.MetricTypes;
import org.dhus.olingo.v2.datamodel.MetricModel;

/**
 * SQL Visitor for the embedded Metrics storage and OData API.
 *
 * @see org.dhus.metrics.embed.MetricsService
 * @see org.dhus.olingo.v2.datamodel.MetricModel
 */
public class MetricSQLVisitor extends HQLVisitor
{
   public MetricSQLVisitor(FilterOption filter, OrderByOption order, TopOption topOption, SkipOption skipOption)
   {
      super(filter, order, topOption, skipOption);
   }

   @Override
   public Object visitMember(Member member) throws ExpressionVisitException, ODataApplicationException
   {
      final List<UriResource> uriResourceParts = member.getResourcePath().getUriResourceParts();

      if (uriResourceParts.size() == 1 && uriResourceParts.get(0) instanceof UriResourcePrimitiveProperty)
      {
         String segmentVal = ((UriResourcePrimitiveProperty) uriResourceParts.get(0)).getSegmentValue();

         switch (segmentVal)
         {
            case MetricModel.NAME: return "name";
            case MetricModel.TIMESTAMP: return "date";
            case MetricModel.TYPE: return "type";
            case MetricModel.COUNT: return "count";
            case MetricModel.GAUGE: return "value";
            case MetricModel.MINIMUM: return "min";
            case MetricModel.MAXIMUM: return "max";
            case MetricModel.MEAN: return "mean";
            case MetricModel.MEDIAN: return "median";
            case MetricModel.STANDARDDEVIATION: return "std_dev";
            case MetricModel.SEVENTYFIFTHPERCENTILE: return "h_75thpercentile";
            case MetricModel.NINETYFIFTHPERCENTILE: return "h_95thpercentile";
            case MetricModel.NINETYEIGHTHPERCENTILE: return "h_98thpercentile";
            case MetricModel.NINETYNINTHPERCENTILE: return "h_99thpercentile";
            case MetricModel.NINETYNINTHNINEPERCENTILE: return "h_999thpercentile";
            case MetricModel.MEANRATE: return "mean_rate";
            case MetricModel.ONEMINUTERATE: return "m_1m_rate";
            case MetricModel.FIVEMINUTESRATE: return "m_5m_rate";
            case MetricModel.FIFTEENMINUTESRATE: return "m_15m_rate";
            default:
               throw new ODataApplicationException("Property not supported: " + segmentVal,
                     HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
         }
      }
      throw new ODataApplicationException("Non-primitive properties are not supported in filter expressions",
               HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
   }

   @Override
   public Object visitEnum(EdmEnumType type, List<String> enumValues) throws ExpressionVisitException, ODataApplicationException
   {
      return MetricTypes.fromName(enumValues.get(0)).type;
   }

}
