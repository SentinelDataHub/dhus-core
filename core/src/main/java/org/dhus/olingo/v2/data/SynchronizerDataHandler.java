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
package org.dhus.olingo.v2.data;

import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerConfiguration;
import fr.gael.dhus.service.ISynchronizerService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.odata.engine.data.DataHandler;
import fr.gael.odata.engine.data.DataHandlerUtil;

import java.util.List;

import org.dhus.api.olingo.v2.EntityProducer;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.dhus.olingo.v2.ODataSecurityManager;
import org.dhus.olingo.v2.datamodel.SynchronizerModel;
import org.dhus.olingo.v2.entity.TypeStore;

public class SynchronizerDataHandler implements DataHandler
{
   protected static final ISynchronizerService SYNCHRONIZER_SERVICE =
         ApplicationContextProvider.getBean(ISynchronizerService.class);

   private final TypeStore typeStore;

   public SynchronizerDataHandler(TypeStore typeStore)
   {
      this.typeStore = typeStore;
   }

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      final EntityCollection entities = new EntityCollection();
      SYNCHRONIZER_SERVICE.getSynchronizerConfs().forEachRemaining(
            (SynchronizerConfiguration syncConf) -> entities.getEntities().add(toOlingoEntity(syncConf))
      );
      return entities;
   }

   protected Entity toOlingoEntity(SynchronizerConfiguration syncConf)
   {
      Entity res;
      EntityProducer<SynchronizerConfiguration> defaultEntityProducer =
            typeStore.get(SynchronizerConfiguration.class).<SynchronizerConfiguration>getEntityProducer();
      TypeStore.Node entityProducerNode = typeStore.get(syncConf.getClass());
      if (entityProducerNode != null)
      {
         res = entityProducerNode.<SynchronizerConfiguration>getEntityProducer().toOlingoEntity(syncConf);
      }
      else
      {
         res = defaultEntityProducer.toOlingoEntity(syncConf);
      }
      res.setId(DataHandlerUtil.createEntityId(SynchronizerModel.ABSTRACT_ENTITY_SET_NAME, String.valueOf(syncConf.getId())));
      return res;
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      Integer kp = Integer.valueOf(keyParameters.get(0).getText());
      SynchronizerConfiguration config = SYNCHRONIZER_SERVICE.getSynchronizerConfById(kp, SynchronizerConfiguration.class);
      return toOlingoEntity(config);
   }
}
