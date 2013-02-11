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
package org.dhus.test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This bean removes the dhus.xml file in the CWD.
 * Add a dependency on this bean to the ConfigurationManager bean to properly
 * re-extract the configuration from the classpath.
 */
public class ConfigFileEraserBean
{
   private final Logger logger = LogManager.getLogger();
   private final Path path = Paths.get("dhus.xml");

   public ConfigFileEraserBean() {}

   public void init()
   {
      removeConfigfile();
      extractConfigFile();
   }

   public void removeConfigfile()
   {
      try
      {
         if (Files.deleteIfExists(path))
         {
            logger.info("Removed `dhus.xml` from the CWD");
         }
         else
         {
            logger.info("Could not remove `dhus.xml` from the CWD");
         }
      }
      catch (Throwable ex)
      {
         logger.info("Could not remove `dhus.xml` from the CWD", ex);
      }
   }

   public void extractConfigFile()
   {
      try (InputStream in = ClassLoader.getSystemResource("dhus-config-it.xml").openStream())
      {
         Files.copy(in, path);
         logger.info("Re-extracted `dhus.xml` in the CWD from classpath resource `dhus-config-it.xml`");
      }
      catch (Throwable ex)
      {
         logger.info("Could not re-extract `dhus.xml` from the classpath", ex);
      }
   }

}
