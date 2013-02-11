/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016,2017,2018 GAEL Systems
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
import java.nio.file.DirectoryNotEmptyException;
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
   private static final Logger LOGGER = LogManager.getLogger();

   private String path;
   private Integer maxFileNo;
   private Integer maxItems;
   private HierarchicalDirectoryBuilder hdb;

   @SuppressWarnings("unused")
   private HfsManager() {}

   public HfsManager(String path, int max_file_no, int maxItems)
   {
      if (max_file_no < 5)
      {
         throw new IllegalArgumentException();
      }

      this.path = path;
      this.maxFileNo = max_file_no;
      this.maxItems = maxItems;
   }

   public HierarchicalDirectoryBuilder getHierarchicalDirectoryBuilder()
   {
      File root = new File(path);
      if (!root.exists())
      {
         root.mkdirs();
      }
      if (hdb == null)
      {
         hdb = new HierarchicalDirectoryBuilder(root.getAbsoluteFile(), this.maxFileNo, this.maxItems);
      }
      return hdb;
   }

   /**
    * Returns the next existing available path of this HfsManager.
    *
    * @param fileName Name of file that will be stored in the requested path
    * @return the path to an existing directory
    */
   public synchronized File getNewPath(String fileName)
   {
      return getHierarchicalDirectoryBuilder().getDirectory(fileName);
   }

   /**
    * Check file existence within the current HFS.
    *
    * @param file file to check
    * @return true if HFS contains the file and if exists
    */
   public boolean isContaining(File file)
   {
      Path root = Paths.get(this.getPath()).toAbsolutePath();
      Path product_path = file.getAbsoluteFile().toPath();
      File product = product_path.toFile();

      return product_path.startsWith(root) && product.exists() && product.isFile();
   }

   /**
    * Properly delete a path in HFSManager directory.
    * If the path is not inside the HFSManager path, it will not be removed.
    *
    * @param path the path to delete.
    * @throws IOException
    */
   public void delete(String path) throws IOException
   {
      Objects.requireNonNull(path, "Path is null");
      LOGGER.debug("Delete of path {}", path);

      File container = new File(path);
      // Case of path not inside hfsManager: do not remove !
      if (!container.exists() || !isContaining(container))
      {
         return;
      }
      // We first delete the file
      Files.delete(Paths.get(path));
      // Case of path basename matches the 'old' reserved "dhus_entry" folder
      if (HierarchicalDirectoryBuilder.DHUS_ENTRY_NAME.equals(container.getParentFile().getName()))
      {
         container = container.getParentFile();
         // Force recursive delete of the folder
         if (container != null)
         {
            try
            {
               Files.delete(container.toPath());
            }
            catch (DirectoryNotEmptyException suppressed) {}
            catch (IOException ex)
            {
               LOGGER.warn("Unable de delete directory {}", container);
            }
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

   /**
    * Retrieves the size (in bytes) of the file pointed by path or zero if it does not exist.
    *
    * @param path of file
    * @return the size of the file or zero
    */
   public long sizeOf(String path)
   {
      File file = new File(path);
      if (file.exists())
      {
         return FileUtils.sizeOf(new File(path));
      }
      else
      {
         return 0;
      }
   }

}
