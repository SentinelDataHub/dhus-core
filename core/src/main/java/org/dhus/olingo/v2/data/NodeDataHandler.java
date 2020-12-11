/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018-2019 GAEL Systems
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

import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.util.DrbChildren;
import fr.gael.drb.DrbNode;
import fr.gael.odata.engine.data.DataHandler;
import fr.gael.odata.engine.data.DataHandlerUtil;
import fr.gael.odata.engine.processor.MediaResponseBuilder;

import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.uri.UriParameter;

import org.dhus.olingo.v2.datamodel.ItemModel;
import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.store.StoreException;
import org.dhus.store.StoreService;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.DataStoreProduct;
import org.dhus.store.derived.DerivedProductStore;
import org.dhus.store.derived.DerivedProductStoreService;

public class NodeDataHandler implements DataHandler
{
   private static final StoreService STORE_SERVICE =
         ApplicationContextProvider.getBean(StoreService.class);
   private final DerivedProductStoreService derivedProductStoreService =
         ApplicationContextProvider.getBean(DerivedProductStoreService.class);

   private static final Logger LOGGER = LogManager.getLogger(NodeDataHandler.class);

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      return null;
   }

   @Override
   public Entity getEntityData(List<UriParameter> arg0) throws ODataApplicationException
   {
      return null;
   }

   @Override
   public Entity getRelatedEntityData(Entity sourceEntity, List<UriParameter> navigationKeyParameters, EdmNavigationProperty edmNavigationProperty)
         throws ODataApplicationException
   {
      // Derived Product to Node Navigation
      if (sourceEntity instanceof DerivedProductEntity)
      {
         String tag = (String) sourceEntity.getProperty(ItemModel.PROPERTY_ID).getValue();
         String uuid = ((DerivedProductEntity) sourceEntity).getParentUuid();
         DataStoreProduct dp = null;
         try
         {
            switch (tag)
            {
               case ProductDataHandler.QUICKLOOK_ID:
               {
                  dp = derivedProductStoreService.getDerivedProduct(uuid, DerivedProductStore.QUICKLOOK_TAG);
                  break;
               }
               case ProductDataHandler.THUMBNAIL_ID:
               {
                  dp = derivedProductStoreService.getDerivedProduct(uuid, DerivedProductStore.THUMBNAIL_TAG);
                  break;
               }
            }
         }
         catch (StoreException e)
         {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
         }
         if (dp == null)
         {
            throw new ODataApplicationException("Derived product " + tag + " not found", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
         }
         DrbNode node = dp.getImpl(DrbNode.class);
         if (node.hasChild())
         {
            String nodeName = DataHandlerUtil.getSingleStringKeyParameterValue(navigationKeyParameters, ItemModel.PROPERTY_ID);
            return NodeEntity.initialize(node.getNamedChild(nodeName, 1));
         }
      }
      // Product to Node navigation
      else if (sourceEntity.getType().equals(ProductModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         DrbNode drbNode = getDrbNodeFromProduct(sourceEntity);

         String keyParameterValue = DataHandlerUtil.getSingleStringKeyParameterValue(navigationKeyParameters, ItemModel.PROPERTY_ID);

         if (keyParameterValue.equals(drbNode.getName()))
         {
            return NodeEntity.initialize(drbNode);
         }
      }
      // Node to Node navigation
      else if (sourceEntity instanceof NodeEntity)
      {
         NodeEntity node = (NodeEntity) sourceEntity;

         DrbNode drbNode = node.getDrbNode();
         if (drbNode.hasChild())
         {
            String nodeName = DataHandlerUtil.getSingleStringKeyParameterValue(navigationKeyParameters, ItemModel.PROPERTY_ID);
            return NodeEntity.initialize(drbNode.getNamedChild(nodeName, 1));
         }
      }
      return null;
   }

   @Override
   public EntityCollection getRelatedEntityCollectionData(Entity sourceEntity,
         EdmNavigationProperty edmNavigationProperty) throws ODataApplicationException
   {
      // Derived Product to Nodes Navigation
      if (sourceEntity instanceof DerivedProductEntity)
      {
         EntityCollection navigationTargetEntityCollection = new EntityCollection();
         String tag = (String) sourceEntity.getProperty(ItemModel.PROPERTY_ID).getValue();
         String uuid = ((DerivedProductEntity) sourceEntity).getParentUuid();
         DataStoreProduct dp = null;
         try
         {
            switch (tag)
            {
               case ProductDataHandler.QUICKLOOK_ID:
               {
                  dp = derivedProductStoreService.getDerivedProduct(uuid, DerivedProductStore.QUICKLOOK_TAG);
                  break;
               }
               case ProductDataHandler.THUMBNAIL_ID:
               {
                  dp = derivedProductStoreService.getDerivedProduct(uuid, DerivedProductStore.THUMBNAIL_TAG);
                  break;
               }
            }
         }
         catch (StoreException e)
         {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
         }
         if (dp == null)
         {
            throw new ODataApplicationException("Derived product " + tag + " not found", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
         }
         DrbNode node = dp.getImpl(DrbNode.class);

         if (node.hasChild())
         {
            for (int i = 0; i < node.getChildrenCount(); i++)
            {
               navigationTargetEntityCollection
                     .getEntities()
                     .add(NodeEntity.initialize(node.getChildAt(i)));
            }
         }
         return navigationTargetEntityCollection;
      }
      // Product to Nodes navigation
      else if (sourceEntity.getType().equals(ProductModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         EntityCollection navigationTargetEntityCollection = new EntityCollection();
         DrbNode drbNode = getDrbNodeFromProduct(sourceEntity);
         navigationTargetEntityCollection
               .getEntities()
               .add(NodeEntity.initialize(drbNode));
         return navigationTargetEntityCollection;
      }
      // Node to Nodes navigation
      else if (sourceEntity instanceof NodeEntity)
      {
         NodeEntity nodeEntity = (NodeEntity) sourceEntity;

         EntityCollection navigationTargetEntityCollection = new EntityCollection();
         DrbNode drbNode = nodeEntity.getDrbNode();
         if (drbNode.hasChild())
         {
            for (int i = 0; i < drbNode.getChildrenCount(); i++)
            {
               navigationTargetEntityCollection
                     .getEntities()
                     .add(NodeEntity.initialize(drbNode.getChildAt(i)));
            }
         }
         return navigationTargetEntityCollection;
      }
      return null;
   }

   private DrbNode getDrbNodeFromProduct(Entity sourceEntity) throws ODataApplicationException
   {
      DrbNode drbNode = null;
      String productUuid = (String) sourceEntity.getProperty(ItemModel.PROPERTY_ID).getValue();
      try
      {
         DataStoreProduct product = STORE_SERVICE.getPhysicalProduct(productUuid);
         if (product.hasImpl(DrbNode.class))
         {
            drbNode = product.getImpl(DrbNode.class);
            if (DrbChildren.shouldODataUseFirstChild(product.getName(), drbNode))
            {
               // skip product zip node
               drbNode = drbNode.getFirstChild();
            }
         }
      }
      catch (DataStoreException e)
      {
         throw new ODataApplicationException("Product not available", HttpStatusCode.NOT_FOUND.getStatusCode(),
               Locale.ENGLISH);
      }
      return drbNode;
   }

   @Override
   public void prepareResponseForDownload(ODataRequest request, ODataResponse response, Entity entity)
   {
      if (entity instanceof NodeEntity)
      {
         NodeEntity nodeEntity = (NodeEntity) entity;
         DrbNode drbNode = nodeEntity.getDrbNode();

         // ContentType
         String contentType = (String) entity.getProperty(ItemModel.PROPERTY_CONTENTTYPE).getValue();

         // ContentLength
         long contentLength = (long) entity.getProperty(ItemModel.PROPERTY_CONTENTLENGTH).getValue();

         if (NodeEntity.hasStream(drbNode))
         {
            MediaResponseBuilder.prepareMediaResponse(
                  null,
                  drbNode.getName(),
                  contentType,
                  0,
                  contentLength,
                  request,
                  response,
                  NodeEntity.getStream(drbNode));
         }
         else
         {
            LOGGER.error("No stream for node");
         }
      }

   }
}
