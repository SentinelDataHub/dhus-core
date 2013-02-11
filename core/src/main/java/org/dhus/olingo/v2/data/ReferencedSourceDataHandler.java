/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2021 GAEL Systems
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


package org.dhus.olingo.v2.data;

import java.util.List;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.dhus.olingo.v2.datamodel.ProductSynchronizerModel;
import org.dhus.olingo.v2.datamodel.ReferencedSourceModel;
import org.dhus.olingo.v2.datamodel.SynchronizerModel;

import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.Source;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerConfiguration;
import fr.gael.dhus.service.ISynchronizerService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.odata.engine.data.DataHandler;

public class ReferencedSourceDataHandler implements DataHandler
{
   protected static final ISynchronizerService SYNCHRONIZER_SERVICE =
      ApplicationContextProvider.getBean(ISynchronizerService.class);

   @Override
   public EntityCollection getRelatedEntityCollectionData(Entity sourceEntity,
         EdmNavigationProperty edmNavigationProperty) throws ODataApplicationException
   {
      EntityCollection entities = new EntityCollection();
      if (ProductSynchronizerModel.FULL_QUALIFIED_NAME
            .getFullQualifiedNameAsString().equals(sourceEntity.getType()))
      {
         ProductSynchronizer sync = (ProductSynchronizer) SYNCHRONIZER_SERVICE.getSynchronizerConfById(
                  (Long) sourceEntity.getProperty(SynchronizerModel.PROPERTY_ID)
                        .getValue(),
                  SynchronizerConfiguration.class);

         List<Source> sources = sync.getSources().getSource();
         for (Source source : sources)
         {
            entities.getEntities().add(toOlingoEntity(source));
         }
         return entities;
      }
      return null;
   }

   private Entity toOlingoEntity(Source source)
   {
      Entity entity = new Entity();
      entity.addProperty(new Property(null, ReferencedSourceModel.PROPERTY_REFERENCED_ID,
            ValueType.PRIMITIVE, source.getReferenceId()));
      
      // SourceCollection
      entity.addProperty(new Property(
            null, 
            ReferencedSourceModel.PROPERTY_SOURCE_COLLECTION, 
            ValueType.PRIMITIVE,
            source.getSourceCollection()));

      // LastCreationDate
      if (source.getLastCreationDate() != null)
      {
         entity.addProperty(new Property(
               null,
               ReferencedSourceModel.PROPERTY_LAST_CREATION_DATE,
               ValueType.PRIMITIVE,
               source.getLastCreationDate().toGregorianCalendar()));
      }
      if (source.getLastDateSourceUsed() != null)
      {
         entity.addProperty(new Property(
               null,
               ReferencedSourceModel.PROPERTY_LAST_DATE_SOURCE_USED,
               ValueType.PRIMITIVE,
               source.getLastDateSourceUsed().toGregorianCalendar()));
      }
      return entity;
   }

   @Override
   public EntityCollection getEntityCollectionData()
         throws ODataApplicationException
   {
     return null;
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters)
         throws ODataApplicationException
   {
      // TODO Auto-generated method stub
      return null;
   }
}
