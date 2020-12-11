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
package org.dhus.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

/**
 * Valve to compute metrics.
 * <p>
 * Removing that valve from the pipeline does not affect other components.
 */
public class MetricsValve extends ValveBase
{
   /** Metric Registry, for monitoring purposes. */
   private static final MetricRegistry METRIC_REGISTRY =
         ApplicationContextProvider.getBean(MetricRegistry.class);

   protected String getMetricPrefix(Request request)
   {
      int port = request.getConnector().getPort();
      String contextName = request.getContext().getName();
      if (contextName == null || contextName.isEmpty())
      {
         contextName = "none";
      }
      return MetricRegistry.name("access", String.valueOf(port), contextName);
   }

   @Override
   public void invoke(Request request, Response response) throws IOException, ServletException
   {
      String prefix = getMetricPrefix(request);

      String activeCounterName, successCounterName, failureCounterName, timerName;
      activeCounterName = MetricRegistry.name(prefix, "counters", "active");
      successCounterName = MetricRegistry.name(prefix, "counters", "success");
      failureCounterName = MetricRegistry.name(prefix, "counters", "failure");
      timerName = MetricRegistry.name(prefix, "timer");

      Counter active = METRIC_REGISTRY.counter(activeCounterName);
      Counter success = METRIC_REGISTRY.counter(successCounterName);
      Counter failure = METRIC_REGISTRY.counter(failureCounterName);
      try (Timer.Context context = METRIC_REGISTRY.timer(timerName).time())
      {
         active.inc();

         getNext().invoke(request, response);
         // Uncaught exception or null
         Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

         if (throwable != null || response.isError() || response.getStatus() >= 400)
         {
            failure.inc();
         }
         else
         {
            success.inc();
         }
      }
      catch (Throwable t)
      {
         failure.inc();
         throw t;
      }
      finally
      {
         active.dec();
      }
   }

}
