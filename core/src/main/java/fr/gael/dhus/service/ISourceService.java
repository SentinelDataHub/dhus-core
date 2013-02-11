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
package fr.gael.dhus.service;

import fr.gael.dhus.database.object.config.source.Source;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerSource;

import java.util.List;

public interface ISourceService
{

   /**
    * Counts number of source defined.
    *
    * @return the number of source
    */
   int count();

   /**
    * Creates a new source and saves it into the configuration.
    *
    * @param url      source URL
    * @param username source authentication username
    * @param password source authentication password
    * @param maxDownload maximum concurrent downloads
    * @return the created source
    */
   Source createSource(String url, String username, String password, Integer maxDownload);

   /**
    * Retrieves a source by its id.
    *
    * @param sourceId source id
    * @return a source or <code>null</code> if not found
    */
   Source getSource(int sourceId);

   /**
    * Retrieves a source from a {@link SynchronizerSource}.
    *
    * @param synchronizerSource synchronizer source configuration
    * @return a source, or <code>null</code> if it not found
    */
   Source getSource(SynchronizerSource synchronizerSource);

   /**
    * Retrieves a sources from a list of {@link SynchronizerSource}.
    *
    * @param synchronizerSources synchronizer source configuration list
    * @return a source list containing only found sources
    */
   List<Source> getSource(List<SynchronizerSource> synchronizerSources);

   /**
    * Saves into the configuration an updated source.
    *
    * @param source updated source
    * @return true if update is performed successfully, otherwise false
    * @throws IllegalArgumentException if the give source is not valid
    */
   boolean updateSource(Source source);

   /**
    * Deletes a source from the configuration.
    *
    * @param source source to delete
    * @return true if the deletion is successfully done, otherwise false
    */
   boolean deleteSource(Source source);

   /**
    * Lists all sources defined into the configuration.
    *
    * @return a source list
    */
   List<Source> list();

   /**
    * Sorts given sources depending of their bandwidth.
    *
    * @param sources source list to sort
    * @return a sorted source list
    */
   List<Source> sortSources(List<Source> sources);

   /**
    * Returns the source with the best estimated bandwidth contained into the given source list.
    *
    * @param sources source list
    * @return the best source
    */
   Source getBestSource(List<Source> sources);
}
