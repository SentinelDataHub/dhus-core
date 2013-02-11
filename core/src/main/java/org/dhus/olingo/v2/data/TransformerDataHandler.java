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

import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.Transformation;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.service.TransformationService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.odata.engine.data.DataHandler;
import fr.gael.odata.engine.data.DataHandlerUtil;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;

import org.dhus.api.transformation.TransformationException;
import org.dhus.api.transformation.Transformer;
import org.dhus.olingo.v2.ODataSecurityManager;
import org.dhus.olingo.v2.datamodel.JobModel;
import org.dhus.olingo.v2.datamodel.TransformationModel;
import org.dhus.olingo.v2.datamodel.TransformerModel;
import org.dhus.olingo.v2.datamodel.action.RunTransformerAction;
import org.dhus.olingo.v2.entity.TypeStore;
import org.dhus.store.datastore.ProductNotFoundException;
import org.dhus.transformation.TransformationManager;
import org.dhus.transformation.TransformationQuotasException;

public class TransformerDataHandler implements DataHandler
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final TransformationManager TRANSFORMATION_MANAGER =
         ApplicationContextProvider.getBean(TransformationManager.class);

   private static final TransformationService TRANSFORMATION_SERVICE =
         ApplicationContextProvider.getBean(TransformationService.class);

   private static final ProductService PRODUCT_SERVICE =
         ApplicationContextProvider.getBean(ProductService.class);

   private final TypeStore typeStore;

   public TransformerDataHandler(TypeStore typeStore)
   {
      this.typeStore = typeStore;
   }

   private Entity transformerToEntity(org.dhus.api.transformation.Transformer transformer)
   {
      return typeStore.get(Transformer.class).getEntityProducer().toOlingoEntity(transformer);
   }

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      EntityCollection entityCollection = new EntityCollection();
      List<Transformer> transformerList = TRANSFORMATION_MANAGER.getTransformers();
      for (Transformer transformer : transformerList)
      {
         entityCollection.getEntities().add(transformerToEntity(transformer));
      }
      return entityCollection;
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      String name = DataHandlerUtil.getSingleStringKeyParameterValue(keyParameters,
            TransformerModel.PROPERTY_NAME);
      Transformer transformer = TRANSFORMATION_MANAGER.getTransformer(name);
      if (transformer == null)
      {
         throw new ODataApplicationException("Transformer not found: " + name,
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }
      return transformerToEntity(transformer);
   }

   @Override
   public Object performBoundAction(List<UriParameter> keyPredicates, EdmAction action,
         Map<String, Parameter> parameters) throws ODataApplicationException
   {
      if (action.getFullQualifiedName().equals(RunTransformerAction.FULL_QUALIFIED_NAME))
      {
         String username = ODataSecurityManager.getCurrentUser().getUsername();
         String productUuid = (String) parameters.get(RunTransformerAction.PARAM_PRODUCT_UUID).getValue();

         try
         {
            String transformationName = DataHandlerUtil.getSingleStringKeyParameterValue(keyPredicates,
                  TransformerModel.PROPERTY_NAME);

            Transformation transformation = TRANSFORMATION_MANAGER.transform(
                  transformationName, productUuid,
                  Util.parametersToMap(parameters.get(RunTransformerAction.PARAM_PARAMETERS)));

            return typeStore.get(Transformation.class).getEntityProducer().toOlingoEntity(transformation);
         }
         catch (TransformationQuotasException e)
         {
            throw new ODataApplicationException("Transformation quota exceeded: " + e.getMessage(), 429, Locale.ENGLISH);
         }
         catch (TransformationException e)
         {
            Product product = PRODUCT_SERVICE.getProduct(productUuid);
            LOGGER.info("Failed to submit transformation for request from '{}' on product {} ({}) (~{} bytes), invalid request: {}",
                  username,
                  product != null ? product.getIdentifier() : "UNKNOWN",
                  product != null ? product.getUuid() : "UNKNOWN",
                  product != null ? product.getSize() : "UNKNOWN",
                  e.getMessage());

            throw new ODataApplicationException("Invalid Transformation request: " + e.getMessage(),
                  HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
         }
         catch (ProductNotFoundException e)
         {
            LOGGER.info("Failed to submit transformation for request from '{}', product {} not found",
                  username, productUuid);
            throw new ODataApplicationException("Product not found",
                  HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
         }
      }
      return null;
   }

   @Override
   public Entity getRelatedEntityData(Entity entity, EdmNavigationProperty edmNavigationProperty)
         throws ODataApplicationException
   {
      String type = entity.getType();
      if (TransformationModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString().equals(type))
      {
         Transformation transformation = TRANSFORMATION_SERVICE.getTransformation(
               entity.getProperty(JobModel.PROPERTY_ID).getValue().toString());

         String transformerName = transformation.getTransformer();
         return transformerToEntity(TRANSFORMATION_MANAGER.getTransformer(transformerName));
      }

      throw new ODataApplicationException("Invalid navigation from " + type + " to " +
            TransformerModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString(),
            HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
   }
}
