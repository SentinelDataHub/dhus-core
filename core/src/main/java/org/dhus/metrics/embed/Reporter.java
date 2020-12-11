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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;

import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.BeansException;

/**
 * Reporter to the embedded metrics database.
 *
 * @see MetricsService
 */
public class Reporter extends ScheduledReporter
{
   /** Name of this reporter. */
   public static final String NAME = "dhus-reporter";
   /** Log. */
   public static final Logger LOGGER = LogManager.getLogger();

   /**
    * Create new instance.
    *
    * @see ScheduledReporter#ScheduledReporter(MetricRegistry, String, MetricFilter, TimeUnit, TimeUnit)
    *
    * @param registry
    * @param rateUnit
    * @param durationUnit
    * @param filter
    */
   public Reporter(MetricRegistry registry, MetricFilter filter, TimeUnit rateUnit, TimeUnit durationUnit)
   {
      super(registry, NAME, filter, rateUnit, durationUnit);
   }

   @Override
   public void report(SortedMap<String, Gauge> gauges,
                      SortedMap<String, Counter> counters,
                      SortedMap<String, Histogram> histograms,
                      SortedMap<String, Meter> meters,
                      SortedMap<String, Timer> timers)
   {
      try
      {
         MetricsService svc = ApplicationContextProvider.getBean(MetricsService.class);
         svc.report(gauges, counters, histograms, meters, timers);
         svc.applySlidingWindow();
      }
      catch (BeansException ex)
      {
         LOGGER.error("Could not store metrics in the embedded storage, embbeded storage is not well configured");
      }
   }

}
