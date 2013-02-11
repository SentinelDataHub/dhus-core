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
package org.dhus.metrics.embed;

import com.codahale.metrics.MetricRegistry;

import com.ryantenney.metrics.spring.reporter.AbstractScheduledReporterFactoryBean;

import java.util.concurrent.TimeUnit;

/**
 * Factory bean used by the metrics-spring component.
 */
public final class ReporterFactoryBean extends AbstractScheduledReporterFactoryBean<Reporter>
{
   // Required
   public static final String PERIOD = "period";

   // Optional
   public static final String DURATION_UNIT = "duration-unit";
   public static final String RATE_UNIT = "rate-unit";
   public static final String FILTER_PATTERN = "filter"; // Field is protected in parent class !?
   public static final String FILTER_REF = "filter-ref"; // Field is protected in parent class !?

   @Override
   protected long getPeriod()
   {
      return convertDurationString(getProperty(PERIOD));
   }

   @Override
   public Class<? extends Reporter> getObjectType()
   {
      return Reporter.class;
   }

   @Override
   protected Reporter createInstance() throws Exception
   {
      MetricRegistry registry = getMetricRegistry();
      TimeUnit durationUnit = TimeUnit.MILLISECONDS;
      if (hasProperty(DURATION_UNIT))
      {
         durationUnit = getProperty(DURATION_UNIT, TimeUnit.class);
      }
      TimeUnit rateUnit = TimeUnit.SECONDS;
      if (hasProperty(RATE_UNIT))
      {
         rateUnit = getProperty(RATE_UNIT, TimeUnit.class);
      }

      return new Reporter(registry, getMetricFilter(), rateUnit, durationUnit);
   }

}
