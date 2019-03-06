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

import fr.gael.dhus.database.object.config.source.AverageBandwidthSourceComparator;
import fr.gael.dhus.database.object.config.source.Source;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerSource;
import fr.gael.dhus.sync.smart.SourceManager;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SourceService implements ISourceService
{
   private static final int LIMIT_MAX_DOWNLOADS = 30;
   private Comparator<Source> sourceComparator;

   @Autowired
   private ConfigurationManager configurationManager;

   private SourceManager getSourceManager()
   {
      return configurationManager.getSourceManager();
   }

   private boolean exits(int sourceId)
   {
      return getSourceManager().get(sourceId) != null;
   }

   private synchronized Comparator<Source> getSourceComparator()
   {
      if (sourceComparator == null)
      {
         sourceComparator = new AverageBandwidthSourceComparator();
      }
      return sourceComparator;
   }

   @Override
   public int count()
   {
      return getSourceManager().count();
   }

   @Override
   public Source createSource(String url, String username, String password, Integer maxDownloads)
   {
      IllegalArgumentException exception = new IllegalArgumentException("Invalid URL");
      if (url == null || url.isEmpty())
      {
         throw exception;
      }

      try
      {
         URL checkUrl = new URL(url);
         if (!checkUrl.getProtocol().startsWith("http"))
         {
            throw exception;
         }
      }
      catch (MalformedURLException e)
      {
         throw new IllegalArgumentException("Invalid URL");
      }

      return getSourceManager().create(url, username, password, maxDownloads);
   }

   @Override
   public Source getSource(int sourceId)
   {
      if (sourceId < 0)
      {
         throw new IllegalArgumentException("Source identifier cannot be negative");
      }
      return getSourceManager().get(sourceId);
   }

   @Override
   public Source getSource(SynchronizerSource synchronizerSource)
   {
      return getSourceManager().get(synchronizerSource.getSourceId());
   }

   @Override
   public List<Source> getSource(List<SynchronizerSource> synchronizerSources)
   {
      if (synchronizerSources == null || synchronizerSources.isEmpty())
      {
         return Collections.emptyList();
      }

      return synchronizerSources
            .stream().map(this::getSource)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
   }

   @Override
   public boolean updateSource(Source source)
   {
      if (source.getId() < 0)
      {
         throw new IllegalArgumentException("Invalid source id");
      }
      if (source.getUrl() == null || source.getUrl().isEmpty())
      {
         throw new IllegalArgumentException("url must not be null or empty");
      }
      if (source.getMaxDownload() > LIMIT_MAX_DOWNLOADS)
      {
         throw new IllegalArgumentException(
               "max concurrent download must be less or equals than " + LIMIT_MAX_DOWNLOADS);
      }
      return configurationManager.getSourceManager().updateSource(source);
   }

   @Override
   public boolean deleteSource(Source source)
   {
      int sourceId = source.getId();
      if (exits(sourceId))
      {
         getSourceManager().removeSource(sourceId);
         return getSource(sourceId) == null;
      }
      return false;
   }

   @Override
   public List<Source> list()
   {
      return getSourceManager().list();
   }

   @Override
   public List<Source> sortSources(List<Source> sources)
   {
      List<Source> sortedList = new ArrayList<>(sources);
      Collections.sort(sortedList, getSourceComparator().reversed());
      return Collections.unmodifiableList(sortedList);
   }

   @Override
   public Source getBestSource(List<Source> sources)
   {
      if (sources == null || sources.isEmpty())
      {
         throw new IllegalArgumentException();
      }

      return sortSources(sources).get(0);
   }
}
