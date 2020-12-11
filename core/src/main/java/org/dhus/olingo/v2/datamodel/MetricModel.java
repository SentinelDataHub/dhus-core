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
package org.dhus.olingo.v2.datamodel;

import fr.gael.odata.engine.model.EntityModel;

import java.util.Arrays;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.dhus.olingo.v2.datamodel.enumeration.MetricTypeEnum;

import org.dhus.olingo.v2.web.DHuSODataServlet;

/**
 * OData entity type for the embedded metric store.
 */
public class MetricModel implements EntityModel
{
   public static FullQualifiedName FULL_QUALIFIED_NAME = new FullQualifiedName(DHuSODataServlet.NAMESPACE, "Metric");

   public static final String NAME = "Name";
   public static final String TYPE = "Type";
   public static final String TIMESTAMP = "Timestamp";

   public static final String COUNT = "Count";

   public static final String GAUGE = "Gauge";

   public static final String MINIMUM = "Minimum";
   public static final String MAXIMUM = "Maximum";
   public static final String MEAN = "Mean";
   public static final String MEDIAN = "Median";
   public static final String STANDARDDEVIATION = "StandardDeviation";
   public static final String SEVENTYFIFTHPERCENTILE = "SeventyFifthPercentile";
   public static final String NINETYFIFTHPERCENTILE = "NinetyFifthPercentile";
   public static final String NINETYEIGHTHPERCENTILE = "NinetyEighthPercentile";
   public static final String NINETYNINTHPERCENTILE = "NinetyNinthPercentile";
   public static final String NINETYNINTHNINEPERCENTILE = "NinetyNinthNinePercentile";

   public static final String MEANRATE = "MeanRate";
   public static final String ONEMINUTERATE = "OneMinuteRate";
   public static final String FIVEMINUTESRATE = "FiveMinutesRate";
   public static final String FIFTEENMINUTESRATE = "FifteenMinutesRate";

   @Override
   public CsdlEntityType getEntityType()
   {
      CsdlProperty name = new CsdlProperty()
            .setName(NAME)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);
      CsdlPropertyRef nameRef = new CsdlPropertyRef().setName(name.getName());

      CsdlProperty timestamp = new CsdlProperty()
            .setName(TIMESTAMP)
            .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
            .setNullable(false)
            .setPrecision(3);
      CsdlPropertyRef timestampRef = new CsdlPropertyRef().setName(timestamp.getName());

      CsdlProperty type = new CsdlProperty()
            .setName(TYPE)
            .setType(MetricTypeEnum.FULL_QUALIFIED_NAME)
            .setNullable(false);

      // Counting
      CsdlProperty count = new CsdlProperty()
            .setName(COUNT)
            .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName())
            .setNullable(true);

      // Gauge
      CsdlProperty gauge = new CsdlProperty()
            .setName(GAUGE)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);

      // Snapshot
      CsdlProperty min = new CsdlProperty()
            .setName(MINIMUM)
            .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName())
            .setNullable(true);

      CsdlProperty max = new CsdlProperty()
            .setName(MAXIMUM)
            .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName())
            .setNullable(true);

      CsdlProperty mean = new CsdlProperty()
            .setName(MEAN)
            .setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName())
            .setNullable(true);

      CsdlProperty median = new CsdlProperty()
            .setName(MEDIAN)
            .setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName())
            .setNullable(true);

      CsdlProperty standardDeviation = new CsdlProperty()
            .setName(STANDARDDEVIATION)
            .setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName())
            .setNullable(true);

      CsdlProperty percentile75th = new CsdlProperty()
            .setName(SEVENTYFIFTHPERCENTILE)
            .setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName())
            .setNullable(true);

      CsdlProperty percentile95th = new CsdlProperty()
            .setName(NINETYFIFTHPERCENTILE)
            .setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName())
            .setNullable(true);

      CsdlProperty percentile98th = new CsdlProperty()
            .setName(NINETYEIGHTHPERCENTILE)
            .setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName())
            .setNullable(true);

      CsdlProperty percentile99th = new CsdlProperty()
            .setName(NINETYNINTHPERCENTILE)
            .setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName())
            .setNullable(true);

      CsdlProperty percentile999th = new CsdlProperty()
            .setName(NINETYNINTHNINEPERCENTILE)
            .setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName())
            .setNullable(true);

      // Metered
      CsdlProperty rateMean = new CsdlProperty()
            .setName(MEANRATE)
            .setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName())
            .setNullable(true);

      CsdlProperty rate1minute = new CsdlProperty()
            .setName(ONEMINUTERATE)
            .setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName())
            .setNullable(true);

      CsdlProperty rate5minute = new CsdlProperty()
            .setName(FIVEMINUTESRATE)
            .setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName())
            .setNullable(true);

      CsdlProperty rate15minute = new CsdlProperty()
            .setName(FIFTEENMINUTESRATE)
            .setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName())
            .setNullable(true);

      return new CsdlEntityType()
            .setName(getName())
            .setKey(Arrays.asList(nameRef, timestampRef))
            .setProperties(Arrays.asList(
                  name, timestamp, type,
                  count,
                  gauge,
                  min, max, mean, median, standardDeviation, percentile75th, percentile95th, percentile98th, percentile99th, percentile999th,
                  rateMean, rate1minute, rate5minute, rate15minute));
   }

   @Override
   public String getEntitySetName()
   {
      return "Metrics";
   }

   @Override
   public String getName()
   {
      return "Metric";
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return new FullQualifiedName(DHuSODataServlet.NAMESPACE, getName());
   }

}
