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

import com.ryantenney.metrics.spring.reporter.AbstractReporterElementParser;

/**
 * Parses the reporter configuration from file monitoring.xml, registered via SPI.
 */
public final class ReporterElementParser extends AbstractReporterElementParser
{
   @Override
   public String getType()
   {
      return Reporter.NAME;
   }

   @Override
   protected Class<?> getBeanClass()
   {
      return ReporterFactoryBean.class;
   }

   @Override
   protected void validate(ValidationContext valctx)
   {
      valctx.require(ReporterFactoryBean.PERIOD, DURATION_STRING_REGEX,
            "Period is required and must be in the form '\\d+(ns|us|ms|s|m|h|d)'");

      valctx.optional(ReporterFactoryBean.RATE_UNIT, TIMEUNIT_STRING_REGEX,
            "Rate unit must be one of the enum constants from java.util.concurrent.TimeUnit");
      valctx.optional(ReporterFactoryBean.DURATION_UNIT, TIMEUNIT_STRING_REGEX,
            "Duration unit must be one of the enum constants from java.util.concurrent.TimeUnit");

      valctx.optional(ReporterFactoryBean.FILTER_PATTERN);
      valctx.optional(ReporterFactoryBean.FILTER_REF);
      if (valctx.has(ReporterFactoryBean.FILTER_PATTERN) && valctx.has(ReporterFactoryBean.FILTER_REF))
      {
         valctx.reject(ReporterFactoryBean.FILTER_REF, "Reporter element must not specify both the 'filter' and 'filter-ref' attributes");
      }

      valctx.rejectUnmatchedProperties();
   }

}
