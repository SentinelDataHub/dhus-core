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
package fr.gael.dhus.util.stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Represents a {@link java.io.InputStream} implementation of a unique content
 * coming from multiple sources.
 */
public abstract class MultiSourceInputStream extends InputStream implements MonitorableStream
{
   private final String monitarableId = UUID.randomUUID().toString();
   private boolean isClosed = false;

   /**
    * Releases all current resources used to perform the download.
    */
   protected abstract void releaseResources();

   @Override
   public String getMonitorableId()
   {
      return monitarableId;
   }

   @Override
   public boolean isClosed()
   {
      return isClosed;
   }

   @Override
   public void close() throws IOException
   {
      isClosed = true;
      releaseResources();
   }
}
