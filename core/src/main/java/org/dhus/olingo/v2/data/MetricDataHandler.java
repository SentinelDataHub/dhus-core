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
package org.dhus.olingo.v2.data;

import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import fr.gael.odata.engine.data.DatabaseDataHandler;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDateTimeOffset;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;

import org.dhus.metrics.embed.MetricsService;
import org.dhus.olingo.v2.ODataSecurityManager;
import org.dhus.olingo.v2.datamodel.MetricModel;
import org.dhus.olingo.v2.visitor.MetricSQLVisitor;

public class MetricDataHandler implements DatabaseDataHandler
{
   private static MetricsService METRICS_SERVICE;

   static
   {
      try
      {
         METRICS_SERVICE = ApplicationContextProvider.getBean(MetricsService.class);
      }
      catch (Throwable t) // bean not found => dhus-reporter not configured (not enabled)
      {
         METRICS_SERVICE = null;
      }
   }

   public static boolean isEnabled()
   {
      return METRICS_SERVICE != null;
   }

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.STATISTICS);

      EntityCollection entityCollection = new EntityCollection();
      List<Entity> entities = entityCollection.getEntities();

      METRICS_SERVICE.listMetricsAsEntities()
            .forEach((t) -> entities.add(t));

      return entityCollection;
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.STATISTICS);

      // Fetch, Parse and Check Key Parameters
      Map<String, String> keyPredicates = Util.uriParametersToMap(keyParameters);
      String metricName = keyPredicates.get(MetricModel.NAME);
      metricName = metricName.substring(1, metricName.length() - 1); // remove apostrophes
      String timestampStr = keyPredicates.get(MetricModel.TIMESTAMP);
      Timestamp metricTimestamp = null;

      try
      {
         metricTimestamp = EdmDateTimeOffset.getInstance()
               .<Timestamp>valueOfString(timestampStr, false, null, 3, null, null, Timestamp.class);
      }
      catch (EdmPrimitiveTypeException ex)
      {
         throw new ODataApplicationException("Could not parse key parameter 'Timestamp': " + ex.getMessage(),
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }

      if (metricName == null)
      {
         throw new ODataApplicationException("Could not parse key parameter 'Name': The literal 'null' is not allowed",
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }

      return METRICS_SERVICE.getMetricAsEntity(metricName, metricTimestamp);
   }

   @Override
   public Integer countEntities(FilterOption filterOption) throws ODataApplicationException
   {
      // TODO filter
      return METRICS_SERVICE.countMetrics();
   }

   @Override
   public EntityCollection getEntityCollectionData(FilterOption filterOption, OrderByOption orderByOption,
         TopOption topOption, SkipOption skipOption, CountOption countOption)
         throws ODataApplicationException
   {
      MetricSQLVisitor sqlVisitor = new MetricSQLVisitor(filterOption, orderByOption, topOption, skipOption);

      EntityCollection entityCollection = new EntityCollection();
      List<Entity> entities = entityCollection.getEntities();

      entities.addAll(METRICS_SERVICE.listMetricsAsEntities(sqlVisitor));

      return entityCollection;
   }

}
