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

public interface MonitorableStream
{
   /**
    * Returns monitorable stream identifier.
    *
    * @return the monitorable stream identifier
    */
   String getMonitorableId();

   /**
    * Returns total of transferred bytes from the stream.
    *
    * @return total of transferred bytes
    */
   long getTransferredBytes();

   /**
    * Returns transfer bandwidth.
    *
    * @return transfer bandwidth or -1 if the bandwidth cannot be computed
    */
   long getBandwidth();

   /**
    * Returns true if the stream is closed.
    *
    * @return true if the stream is closed, otherwise false
    */
   boolean isClosed();
}
