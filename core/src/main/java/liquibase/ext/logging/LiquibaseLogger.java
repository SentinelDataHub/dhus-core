/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2015,2016,2019 GAEL Systems
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
package liquibase.ext.logging;

import liquibase.logging.LogType;
import liquibase.logging.core.AbstractLogger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Stupid class, to enable liquibase logging. Liquibase finds this class by
 * itself by doing a custom component scan (they though sl4fj wasn't generic
 * enough).
 */
public class LiquibaseLogger extends AbstractLogger
{
   private static final Logger LOGGER = LogManager.getLogger(LiquibaseLogger.class);

   @Override
   public void severe(String message)
   {
      LOGGER.error(message);
   }

   @Override
   public void severe(String message, Throwable t)
   {
      LOGGER.error(message, t);
   }

   @Override
   public void warning(String message)
   {
      LOGGER.warn(message);
   }

   @Override
   public void warning(String message, Throwable t)
   {
      LOGGER.warn(message, t);
   }

   @Override
   public void info(String message)
   {
      LOGGER.info(message);
   }

   @Override
   public void info(String message, Throwable t)
   {
      LOGGER.info(message, t);
   }

   @Override
   public void debug(String message)
   {
      LOGGER.debug(message);
   }

   @Override
   public void debug(String message, Throwable t)
   {
      LOGGER.debug(message, t);
   }

   @Override
   public void severe(LogType lt, String message)
   {
      LOGGER.error(message);
   }

   @Override
   public void severe(LogType lt, String message, Throwable t)
   {
      LOGGER.error(message, t);
   }

   @Override
   public void warning(LogType lt, String message)
   {
      LOGGER.warn(message);
   }

   @Override
   public void warning(LogType lt, String message, Throwable t)
   {
      LOGGER.warn(message, t);
   }

   @Override
   public void info(LogType lt, String message)
   {
      LOGGER.info(message);
   }

   @Override
   public void info(LogType lt, String message, Throwable t)
   {
      LOGGER.info(message, t);
   }

   @Override
   public void debug(LogType lt, String message)
   {
      LOGGER.debug(message);
   }

   @Override
   public void debug(LogType lt, String message, Throwable t)
   {
      LOGGER.debug(message, t);
   }
}
