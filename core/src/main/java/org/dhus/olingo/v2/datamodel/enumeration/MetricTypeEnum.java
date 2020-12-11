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
package org.dhus.olingo.v2.datamodel.enumeration;

import fr.gael.odata.engine.model.EnumModel;

import java.util.Arrays;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEnumMember;
import org.apache.olingo.commons.api.edm.provider.CsdlEnumType;

import org.dhus.metrics.embed.MetricTypes;
import org.dhus.olingo.v2.datamodel.MetricModel;
import org.dhus.olingo.v2.web.DHuSODataServlet;

/**
 * Metric type: Counter, Meter, Timer, Histogram and Gauge.
 *
 * @see MetricModel
 */
public class MetricTypeEnum implements EnumModel
{
   public static final String ENUM_NAME = "MetricType";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENUM_NAME);

   public static enum Enumeration {

      COUNTER(new CsdlEnumMember()
         .setName(MetricTypes.COUNTER.name)
         .setValue(MetricTypes.COUNTER.getTypeAsString())),

      METER(new CsdlEnumMember()
         .setName(MetricTypes.METER.name)
         .setValue(MetricTypes.METER.getTypeAsString())),

      HISTOGRAM(new CsdlEnumMember()
         .setName(MetricTypes.HISTOGRAM.name)
         .setValue(MetricTypes.HISTOGRAM.getTypeAsString())),

      TIMER(new CsdlEnumMember()
         .setName(MetricTypes.TIMER.name)
         .setValue(MetricTypes.TIMER.getTypeAsString())),

      GAUGE(new CsdlEnumMember()
         .setName(MetricTypes.GAUGE.name)
         .setValue(MetricTypes.GAUGE.getTypeAsString()));

      public final CsdlEnumMember definition;

      private Enumeration(CsdlEnumMember definition)
      {
         this.definition = definition;
      }

      public CsdlEnumMember getDefinition()
      {
         return definition;
      }

   }

   @Override
   public CsdlEnumType getEnumType()
   {
      return new CsdlEnumType().setName(ENUM_NAME)
            .setUnderlyingType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
            .setMembers(Arrays.asList(
                  Enumeration.COUNTER.getDefinition(),
                  Enumeration.METER.getDefinition(),
                  Enumeration.HISTOGRAM.getDefinition(),
                  Enumeration.TIMER.getDefinition(),
                  Enumeration.GAUGE.getDefinition()
            ));
   }

   @Override
   public String getName()
   {
      return ENUM_NAME;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return FULL_QUALIFIED_NAME;
   }

}
