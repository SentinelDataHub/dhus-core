/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2014-2018 GAEL Systems
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
package fr.gael.dhus.util;

import static org.apache.logging.log4j.util.Unbox.*;

import fr.gael.dhus.database.object.User;

import org.apache.commons.net.io.CopyStreamAdapter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DownloadActionRecordListener extends CopyStreamAdapter
{
   private static final Logger LOGGER = LogManager.getLogger();

   private final String uuid;
   private final String identifier;
   private final User user;
   private boolean started = false;
   private long start;

   public DownloadActionRecordListener(String uuid,String identifier,User user)
   {
      this.uuid = uuid;
      this.identifier = identifier;
      this.user = user;
   }

   @Override
   public void bytesTransferred(long total_bytes_transferred, int bytes_transferred, long stream_size)
   {
      if ((total_bytes_transferred == bytes_transferred) && !started)
      {
         start = System.currentTimeMillis();
         started = true;
         LOGGER.info("Product '{}' ({}) download by user '{}' started -> {}",
               this.uuid, this.identifier, user.getUsername(), box(stream_size));
         return;
      }

      if (bytes_transferred == -1)
      {
         long end = System.currentTimeMillis() - start;
         if (total_bytes_transferred == stream_size)
         {
            LOGGER.info("Product '{}' ({}) download by user '{}' completed in {}ms -> {}",
                  this.uuid, this.identifier, user.getUsername(), box(end), box(stream_size));
         }
         else
         {
            LOGGER.info("Product '{}' ({}) download by user '{}' failed at {}/{} in {}ms",
                  this.uuid, this.identifier, user.getUsername(), box(total_bytes_transferred), box(stream_size), box(end));
         }
      }
   }
}
