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
package fr.gael.dhus.database.object.config.source;

import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.sync.smart.SourceManager;
import fr.gael.dhus.system.config.ConfigurationException;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SourceManagerImpl extends Sources implements SourceManager
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int DEFAULT_MAX_DOWNLOAD = 2;

   @XmlTransient
   private int maxId = -1;

   private synchronized Integer nextSourceId()
   {
      if (maxId == -1)
      {
         for (SourceConfiguration source: super.getSource())
         {
            int id = source.getId();
            if (maxId < id)
            {
               maxId = id;
            }
         }
      }
      maxId = maxId + 1;
      return maxId;
   }

   private synchronized boolean save()
   {
      boolean saved = false;
      try
      {
         ApplicationContextProvider.getBean(ConfigurationManager.class).saveConfiguration();
         saved = true;
      }
      catch (ConfigurationException e)
      {
         LOGGER.error("There was an error while saving configuration", e);
      }
      return saved;
   }

   @Override
   public Source create(final String url, final String username, final String password)
   {
      return create(url, username, password, DEFAULT_MAX_DOWNLOAD);
   }

   @Override
   public Source create(final String url, final String username, final String password,
         final Integer maxDownload)
   {
      Source source = new Source();
      source.setUrl(Objects.requireNonNull(url));
      source.setUsername(username);
      source.setPassword(password);
      if (maxDownload != null)
      {
         source.setMaxDownload(maxDownload);
      }

      Integer id = nextSourceId();
      source.setId(id);

      boolean saved = super.getSource().add(source) && save();
      if (saved)
      {
         return new Source(source);
      }
      throw new IllegalStateException("Cannot create the SynchronizerSource");
   }

   @Override
   public boolean updateSource(Source source)
   {
      List<SourceConfiguration> sources = super.getSource();
      int id = source.getId();
      int index = sources.indexOf(source);
      if (index != -1)
      {
         SourceConfiguration syncSource = sources.get(index);
         syncSource.setUrl(source.getUrl());
         syncSource.setUsername(source.getUsername());
         syncSource.setPassword(source.getPassword());

         if (!save())
         {
            LOGGER.warn("Cannot update SynchronizerSource#{}", id);
            return false;
         }
         return true;
      }
      return false;
   }

   @Override
   public void removeSource(Integer sourceId)
   {
      if (sourceId != null)
      {
         Source localSource = get(sourceId);
         if (localSource != null && super.getSource().remove(localSource))
         {
            localSource.close();
            save();
         }
      }
   }

   @Override
   public Source get(Integer sourceId)
   {
      Source source = null;
      for (SourceConfiguration conf: super.getSource())
      {
         if (conf.getId() == sourceId)
         {
            source = (Source) conf;
            break;
         }
      }
      return source;
   }

   @Override
   public List<Source> get(List<Integer> sourceIds)
   {
      ArrayList<Source> sourceList = new ArrayList<>(sourceIds.size());
      for (SourceConfiguration sourceConfiguration: super.getSource())
      {
         if (sourceIds.contains(sourceConfiguration.getId()))
         {
            sourceList.add((Source) sourceConfiguration);
         }
      }
      return sourceList;
   }

   @Override
   @SuppressWarnings("unchecked")
   public List<Source> list()
   {
      List<?> list = super.getSource();
      return Collections.unmodifiableList((List<Source>) list);
   }

   @Override
   public int count()
   {
      return super.getSource().size();
   }
}
