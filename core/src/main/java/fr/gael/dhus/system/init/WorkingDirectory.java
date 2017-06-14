/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016 GAEL Systems
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
package fr.gael.dhus.system.init;

import fr.gael.dhus.system.config.ConfigurationManager;

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
public class WorkingDirectory implements ApplicationListener<ContextClosedEvent>
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final String TEMP_DIRECTORY_PATH =
         System.getProperty("java.io.tmpdir") + File.separator + "dhus";

   private static File tempDirectoryFile;

   @Autowired
   private ConfigurationManager config;

   @PostConstruct
   private void init()
   {
      String path = config.getWorkingDirectoryPath();
      if (path == null || path.isEmpty())
      {
         path = TEMP_DIRECTORY_PATH;
      }
      if (tempDirectoryFile == null)
      {
         tempDirectoryFile = new File(path);
         if (!tempDirectoryFile.exists() || !tempDirectoryFile.isDirectory())
         {
            tempDirectoryFile.mkdirs();
            LOGGER.info("DHuS working directory : {}", tempDirectoryFile.getAbsolutePath());
         }
      }
   }

   @Override
   public void onApplicationEvent(ContextClosedEvent event)
   {
      LOGGER.info("Deleting temporal directory...");
      deleteFile(tempDirectoryFile);
   }

   private static void deleteFile(File file)
   {
      if (file.exists())
      {
         if (file.isDirectory())
         {
            File[] files = file.listFiles();
            if (files == null || files.length == 0)
            {
               file.delete();
            }
            else
            {
               for (File sub_file: files)
               {
                  deleteFile(sub_file);
               }
               file.delete();
            }
         }
         else if (file.isFile())
         {
            file.delete();
         }
      }
   }

   public static File getTempDirectoryFile()
   {
      return tempDirectoryFile;
   }

   public static boolean contains(Path path)
   {
      return path.toAbsolutePath().startsWith(tempDirectoryFile.toPath().toAbsolutePath());
   }

   /**
    * Deletes a sub working directory.
    *
    * @param name the sub directory name to delete.
    *
    * @throws IllegalArgumentException if name is null or empty.
    */
   public static void deleteSubWorkingDirectory(String name) throws IllegalArgumentException
   {
      if (name == null || name.isEmpty())
      {
         throw new IllegalArgumentException("Invalid sub working directory name.");
      }

      Path path = getTempDirectoryFile().toPath().resolve(name);
      if (path.toFile().exists())
      {
         File file = path.toFile();
         if (file.exists())
         {
            deleteFile(file);
         }
      }
   }

   /**
    * Returns the a sub working directory with the given child name, creates it
    * if does not exist.
    *
    * @param name the sub directory name.
    *
    * @return the path of the created sub working directory.
    *
    * @throws IllegalArgumentException   if name is null or empty.
    * @throws FileAlreadyExistsException if can not create the working directory.
    */
   public static Path getSubWorkingDirectory(String name)
         throws IllegalArgumentException, FileAlreadyExistsException
   {
      if (name == null || name.isEmpty())
      {
         throw new IllegalArgumentException("Invalid sub working directory name.");
      }

      Path path = getTempDirectoryFile().toPath().resolve(name);
      File file = path.toFile();
      if (!file.exists() || !file.isDirectory())
      {
         file = new File(WorkingDirectory.getTempDirectoryFile(), name);
         if (!file.mkdir())
         {
            throw new FileAlreadyExistsException(file.getAbsolutePath(), null,
                  "Cannot create temporary directory to process product");
         }
         path = file.toPath();
      }

      return path;
   }
}
