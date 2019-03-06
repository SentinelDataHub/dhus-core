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
package fr.gael.dhus.sync.smart;

import fr.gael.dhus.database.object.config.source.Source;

import java.util.List;

public interface SourceManager
{
   /**
    * Adds a source into the manager.
    *
    * @param url      URL of the source
    * @param username username for connecting to the source
    * @param password password for connecting to the source
    * @return the added source configuration
    * @throws NullPointerException  if the required argument 'url' is {@code null}
    * @throws IllegalStateException if the created source cannot be managed by this manager
    */
   Source create(final String url, final String username, final String password);

   /**
    * Creates a new source into this manager.
    *
    * @param url         URL of the source
    * @param username    username for connecting to the source
    * @param password    password for connecting to the source
    * @param maxDownload maximum concurrent download allowed
    * @return the added source configuration
    * @throws IllegalArgumentException if maxDownload < 1
    * @throws NullPointerException     if the required argument 'url' is {@code null}
    * @throws IllegalStateException    if the created source cannot be managed by this manager
    */
   Source create(final String url, final String username, final String password,
         final Integer maxDownload);

   /**
    * Updates a source already referenced by the manager.
    *
    * @param config new configuration of source
    * @return true if update is done, otherwise false
    */
   boolean updateSource(final Source config);

   /**
    * Removes a source from the manager if it referenced by the manager.
    *
    * @param sourceId id of source to remove
    */
   void removeSource(final Integer sourceId);

   /**
    * Returns a source by its id.
    *
    * @param sourceId source id
    * @return a source if the source is referenced by this manager, otherwise {@code null}
    */
   Source get(Integer sourceId);

   /**
    * Returns a list of source by their ids.
    *
    * @param sourceIds the identifiers of sources
    * @return a list of source if the source is referenced by this manager
    */
   List<Source> get(List<Integer> sourceIds);

   /**
    * Lists all sources referenced by this manager.
    *
    * @return a list of source
    */
   List<Source> list();

   /**
    * Returns number of defined sources.
    *
    * @return number of defined sources
    */
   int count();
}
