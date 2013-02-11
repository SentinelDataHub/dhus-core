/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
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
package fr.gael.dhus.database.object.config.eviction;

import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationException;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages evictions defined in configuration file (dhus.xml).
 */
public class EvictionManager extends Evictions
{
   private static final Logger LOGGER = LogManager.getLogger();

   public Eviction getEvictionByName(String name)
   {
      Objects.requireNonNull(name);
      List<EvictionConfiguration> evictions = getEviction();
      for (EvictionConfiguration evictionConfiguration: evictions)
      {
         if (name.equals(evictionConfiguration.getName()))
         {
            return (Eviction) evictionConfiguration;
         }
      }
      return null;
   }

   public void create(Eviction eviction)
   {
      Objects.requireNonNull(eviction);
      this.eviction.add(eviction);
      save();
   }

   public void delete(Eviction eviction)
   {
      Objects.requireNonNull(eviction);
      delete(eviction.getName());
   }

   public void delete(String evictionName)
   {
      Objects.requireNonNull(evictionName);

      for (int i = 0; i < eviction.size(); i++)
      {
         if (evictionName.equals(eviction.get(i).getName()))
         {
            eviction.remove(i);
            break;
         }
      }
      save();
   }

   /**
    * Saves the current eviction configuration.
    */
   public void save()
   {
      ConfigurationManager cfg = ApplicationContextProvider.getBean(ConfigurationManager.class);
      try
      {
         cfg.saveConfiguration();
      }
      catch (ConfigurationException e)
      {
         LOGGER.error("There was an error while saving eviction configuration", e);
      }
   }

   /**
    * Returns an immutable list of Evictions. Use other methods of this class to add/remove Evictions.
    *
    * @return unmodifiable list of Evictions
    */
   @SuppressWarnings("unchecked")
   public List<Eviction> getEvictions()
   {
      List<Eviction> res = (List<Eviction>) (List<?>) eviction;
      return Collections.<Eviction>unmodifiableList(res);
   }
}
