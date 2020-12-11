/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018,2019 GAEL Systems
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

import fr.gael.dhus.database.dao.MetadataDefinitionDao;
import fr.gael.dhus.database.object.MetadataDefinition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetadataService extends WebService
{
   @Autowired
   private MetadataDefinitionDao metadataDefinitionDao;

   /**
    * Retrieves the metadata definition and create it if it not found.
    *
    * @param name      metadata name
    * @param type      metadata type
    * @param category  metadata category
    * @param queryable metadata queryable
    * @return a metadata definition
    */
   @Transactional
   public MetadataDefinition getMetadataDefinition(String name, String type, String category, String queryable)
   {
      MetadataDefinition metadataDefinition = metadataDefinitionDao.find(name, category, type, queryable);
      if (metadataDefinition == null)
      {
         metadataDefinition = metadataDefinitionDao.create(new MetadataDefinition(name, type, category, queryable));
      }
      return metadataDefinition;
   }
}
