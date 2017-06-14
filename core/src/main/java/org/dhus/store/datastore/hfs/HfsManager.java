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
package org.dhus.store.datastore.hfs;

import fr.gael.dhus.datastore.HierarchicalDirectoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.apache.commons.io.FileUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Operates the Hierarchical File System (HFS).
 */
public class HfsManager
{
   public static final String INCOMING_PRODUCT_DIR = "product";

   private static final Logger LOGGER = LogManager.getLogger();

   private String path;
   private Integer maxFileNo;

   @SuppressWarnings("unused")
   private HfsManager() {}

   public HfsManager(String path, int max_file_no)
   {
      this.path = path;
      this.maxFileNo = max_file_no;
   }

   public HierarchicalDirectoryBuilder getIncomingBuilder()
   {
      File root = new File(path);
      if (!root.exists())
      {
         root.mkdirs();
      }
      return new HierarchicalDirectoryBuilder(root.getAbsoluteFile(), this.maxFileNo);
   }

   /**
    * Returns existing available path into incoming folder.
    *
    * @return the path to an existing directory
    */
   public synchronized File getNewIncomingPath()
   {
      return getIncomingBuilder().getDirectory();
   }

   /**
    * Check file existence within the current HFS.
    *
    * @param file file to check
    * @return true if HFS contains the file and if exists
    */
   public boolean isInIncoming(File file)
   {
      Path root = Paths.get(this.getPath()).toAbsolutePath();
      Path product_path = file.getAbsoluteFile().toPath();
      File product = product_path.toFile();

      return product_path.startsWith(root) && product.exists() && product.isFile();
   }

   public boolean isAnIncomingElement(File file)
   {
      int maxfileno = this.maxFileNo;

      boolean is_digit = true;
      try
      {
         // Incoming folders are "X5F" can be parse "0X5F" by decode
         // Warning '09' means octal value that raise error because 9>8...
         String filename = file.getName();
         if (filename.toUpperCase().startsWith("X"))
         {
            filename = "0" + filename;
         }

         if (Long.decode(filename) > maxfileno)
         {
            throw new NumberFormatException("Expected value exceeded.");
         }
      }
      catch (NumberFormatException e)
      {
         is_digit = false;
      }

      return isInIncoming(file) && (is_digit ||
            file.getName().equals(HierarchicalDirectoryBuilder.DHUS_ENTRY_NAME) ||
           (file.getName().equals(INCOMING_PRODUCT_DIR) &&
                  file.getParentFile().getName().equals(HierarchicalDirectoryBuilder.DHUS_ENTRY_NAME)));
   }

   /**
    * Returns existing available path into incoming folder to store products.
    *
    * @return the path to an existing product directory
    */
   public synchronized File getNewProductIncomingPath()
   {
      File file = new File(getNewIncomingPath(), INCOMING_PRODUCT_DIR);
      file.mkdirs();
      return file;
   }

   /**
    * The initialization of incoming manager.
    */
   public void initIncoming()
   {
      getIncomingBuilder().init();
   }

   /**
    * Properly delete a path in incoming directory.
    * If the path is not inside the incoming path, it will not be removed.
    *
    * @param path the path to delete.
    * @throws IOException
    */
   public void delete(String path) throws IOException
   {
      Objects.requireNonNull(path, "Path is null");
      LOGGER.debug("Delete of path {}", path);

      File container = new File(path);
      // Case of path not inside incoming: do not remove !
      if (!container.exists() || !isInIncoming(container))
      {
         return;
      }
      // Case of path basename matches the incoming reserved "product" folder.
      if (INCOMING_PRODUCT_DIR.equals(container.getParentFile().getName()))
      {
         container = container.getParentFile();
      }
      // Case of path basename matches the incoming reserved "dhus_entry" folder
      if (HierarchicalDirectoryBuilder.DHUS_ENTRY_NAME.equals(container
            .getParentFile().getName()))
      {
         container = container.getParentFile();
      }
      // Force recursive delete of the folder
      if (container != null)
      {
         // We first delete the file
         Files.delete(Paths.get(path));
         try
         {
            LOGGER.debug("Force delete of {}, Has rights to delete: {}", container, container.canWrite());
            FileUtils.forceDelete(container);
         }
         catch(IOException e)
         {
            LOGGER.warn("Unable de delete directory {}", container);
         }
      }
   }

   public String getPath()
   {
      return path;
   }

   public void setPath(String path)
   {
      this.path = path;
   }

   public Integer getMaxFileNo()
   {
      return maxFileNo;
   }

   public void setMaxFileNo(Integer maxFileNo)
   {
      this.maxFileNo = maxFileNo;
   }

}
