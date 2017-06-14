/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016,2017 GAEL Systems
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
package org.dhus;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.dhus.store.AbstractHasImpl;

public abstract class AbstractProduct extends AbstractHasImpl implements Product
{
   private final Map<String, Object> properties = new HashMap<>();
   private String name;

   @Override
   public String getName()
   {
      return name;
   }

   @Override
   public void setName(String name)
   {
      this.name=name;
   }

   @Override
   public Set<String> getPropertyNames()
   {
      return properties.keySet();
   }

   @Override
   public Object getProperty(String key)
   {
      return properties.get(key);
   }

   @Override
   public Object setProperty(String key, Object value)
   {
      return properties.put(key, value);
   }
}
