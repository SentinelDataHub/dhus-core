/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018-2020 GAEL Systems
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
import fr.gael.dhus.database.object.Transformation;
import fr.gael.dhus.service.TransformationService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.odata.engine.data.DataHandlerUtil;
import fr.gael.odata.engine.data.DatabaseDataHandler;

import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;

import org.dhus.olingo.v2.ODataSecurityManager;
import org.dhus.olingo.v2.datamodel.JobModel;
import org.dhus.olingo.v2.datamodel.TransformationModel;
import org.dhus.olingo.v2.datamodel.TransformerModel;
import org.dhus.olingo.v2.entity.TypeStore;
import org.dhus.olingo.v2.visitor.TransformationSQLVisitor;
import org.dhus.transformation.TransformationManager;


public class TransformationDataHandler implements DatabaseDataHandler
{
   private static final TransformationManager TRANSFORMATION_MANAGER =
         ApplicationContextProvider.getBean(TransformationManager.class);

   private static final TransformationService TRANSFORMATION_SERVICE =
         ApplicationContextProvider.getBean(TransformationService.class);


   private final TypeStore typeStore;

   public TransformationDataHandler(TypeStore typeStore)
   {
      this.typeStore = typeStore;
   }

   private Entity transformationToEntity(Transformation transformation)
   {
      return typeStore.get(Transformation.class).getEntityProducer().toOlingoEntity(transformation);
   }

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      return getEntityCollectionData(null, null, null, null, null);
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      String id = DataHandlerUtil.getSingleStringKeyParameterValue(keyParameters,
            JobModel.PROPERTY_ID);
      Transformation transformation = TRANSFORMATION_SERVICE.getTransformation(id);
      if (transformation == null)
      {
         throw new ODataApplicationException("Transformation not found: " + id,
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }
      return transformationToEntity(transformation);
   }

   @Override
   public EntityCollection getRelatedEntityCollectionData(Entity source, EdmNavigationProperty navProperty)
         throws ODataApplicationException
   {
      String type = source.getType();
      if (TransformerModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString().equals(type))
      {
         String name = source.getProperty(TransformerModel.PROPERTY_NAME).getValue().toString();
         EntityCollection entities = new EntityCollection();
         List<Transformation> transformations = TRANSFORMATION_SERVICE.getTransformationsOf(name);
         transformations.forEach(trf -> entities.getEntities().add(transformationToEntity(trf)));
         return entities;
      }

      throw new ODataApplicationException("Invalid navigation from " + type + " to "
            + TransformationModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString(),
            HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
   }

   @Override
   public Entity getRelatedEntityData(Entity source, List<UriParameter> navigationKeyParameters, EdmNavigationProperty edmNavigationProperty)
         throws ODataApplicationException
   {
      String type = source.getType();
      if (TransformerModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString().equals(type))
      {
         String id = DataHandlerUtil.getSingleStringKeyParameterValue(navigationKeyParameters, JobModel.PROPERTY_ID);
         String transformationName = source.getProperty(TransformerModel.PROPERTY_NAME).getValue().toString();
         Transformation execution = TRANSFORMATION_SERVICE.getTransformationOf(transformationName, id);
         if (execution == null)
         {
            throw new ODataApplicationException("Transformation not found: " + id,
                  HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
         }
         return transformationToEntity(execution);
      }

      throw new ODataApplicationException("Invalid navigation from " + type + " to "
            + TransformationModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString(),
            HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
   }

   @Override
   public EntityCollection getEntityCollectionData(FilterOption filter, OrderByOption orderby, TopOption top, SkipOption skip, CountOption count)
         throws ODataApplicationException
   {
      EntityCollection entities = new EntityCollection();
      TransformationSQLVisitor visitor = new TransformationSQLVisitor(filter, orderby, top, skip);
      List<Transformation> transfomationList;

      if (ODataSecurityManager.hasPermission(Role.SYSTEM_MANAGER))
      {
         transfomationList = TRANSFORMATION_SERVICE.getTransformations(visitor);
      }
      else
      {
         transfomationList = TRANSFORMATION_SERVICE.getTransformationsOfUser(visitor, ODataSecurityManager.getCurrentUser());
      }
      transfomationList.forEach(tr -> entities.getEntities().add(transformationToEntity(tr)));
      return entities;
   }

   @Override
   public Integer countEntities(FilterOption filter) throws ODataApplicationException
   {
      TransformationSQLVisitor visitor = new TransformationSQLVisitor(filter, null, null, null);
      if (ODataSecurityManager.hasPermission(Role.SYSTEM_MANAGER))
      {
         return TRANSFORMATION_SERVICE.countTransformations(visitor);
      }
      return TRANSFORMATION_SERVICE.countTransformationsOfUser(visitor, ODataSecurityManager.getCurrentUser());
   }

   @Override
   public void deleteEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      String id = null;

      try
      {
         id = DataHandlerUtil.getSingleStringKeyParameterValue(keyParameters, "Id");
      }
      catch (ODataApplicationException e)
      {
         throw new ODataApplicationException("No Id found in request",
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH, e);
      }

      Transformation transfo = TRANSFORMATION_SERVICE.getTransformation(id);
      if (null == transfo)
      {
         throw new ODataApplicationException("No transformation exists with this id",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }

      TRANSFORMATION_MANAGER.deleteTransformation(transfo.getUuid());
   }
}
