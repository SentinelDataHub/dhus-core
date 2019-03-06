/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018 GAEL Systems
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
package fr.gael.dhus.util.log;

import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusFactory;

/**
 * Extension of {@code CXFBusFactory} that add a custom {@code FaultListener} implementation.
 * This implementation ({@code ClientAbortFaultListener}) is just displaying at INFO level the
 * {@code Fault} message instead of the full stack trace when a {@code ClientAbortException} occurs.
 */
public class ClientAbortRemovalBusFactory extends CXFBusFactory
{

   @Override
   public Bus createBus(Map<Class<?>, Object> e, Map<String, Object> properties)
   {
      Bus bus = super.createBus(e, properties);
      bus.getProperties().put("org.apache.cxf.logging.FaultListener", new ClientAbortFaultListener());
      return bus;
   }

}
