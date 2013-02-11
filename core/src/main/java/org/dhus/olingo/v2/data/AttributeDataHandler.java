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

import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import fr.gael.drb.DrbAttribute;
import fr.gael.drb.DrbAttributeList;
import fr.gael.drb.DrbDefaultAttribute;
import fr.gael.drb.DrbNode;
import fr.gael.drb.DrbSequence;
import fr.gael.drb.query.Query;
import fr.gael.odata.engine.data.DataHandler;
import fr.gael.odata.engine.data.DataHandlerUtil;

import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;

import org.dhus.olingo.v2.datamodel.AttributeModel;
import org.dhus.olingo.v2.datamodel.ItemModel;
import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.store.StoreException;
import org.dhus.store.datastore.DataStoreProduct;
import org.dhus.store.derived.DerivedProductStore;
import org.dhus.store.derived.DerivedProductStoreService;

public class AttributeDataHandler implements DataHandler
{
   private final ProductService productService =
         ApplicationContextProvider.getBean(ProductService.class);
   private final DerivedProductStoreService derivedProductStoreService =
         ApplicationContextProvider.getBean(DerivedProductStoreService.class);

   private static final String DEFAULT_CONTENT_TYPE = "text/plain";

   private Entity toOlingoEntity(String id, String name, String contentType,
         String value, long contentLength, String category)
   {
      Entity attributeEntity = new Entity();

      // ID
      attributeEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_ID,
            ValueType.PRIMITIVE,
            id));

      // Name
      attributeEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_NAME,
            ValueType.PRIMITIVE,
            name));

      // ContentType
      attributeEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_CONTENTTYPE,
            ValueType.PRIMITIVE,
            contentType));

      // ContentLength
      attributeEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_CONTENTLENGTH,
            ValueType.PRIMITIVE,
            contentLength));

      // Value
      attributeEntity.addProperty(new Property(
            null,
            AttributeModel.PROPERTY_VALUE,
            ValueType.PRIMITIVE,
            value));

      // Category
      attributeEntity.addProperty(new Property(
            null,
            AttributeModel.PROPERTY_CATEGORY,
            ValueType.PRIMITIVE,
            category));

      // Set Id
      attributeEntity.setId(DataHandlerUtil.createEntityId(AttributeModel.ENTITY_SET_NAME, id));

      return attributeEntity;
   }

   private Entity indexToEntity(MetadataIndex index)
   {
      return toOlingoEntity(
            index.getName(),
            index.getName(),
            index.getType(),
            index.getValue(),
            index.getValue() == null ? 0 : index.getValue().length(),
            index.getCategory());
   }

   private Entity drbAttributeToEntity(DrbAttribute attr)
   {
      return toOlingoEntity(
            attr.getName(),
            attr.getName(),
            DEFAULT_CONTENT_TYPE,
            attr.getValue() == null ? null : attr.getValue().toString(),
            attr.getValue() == null ? 0 : attr.getValue().toString().length(),
            null);
   }

   @Override
   public Entity getRelatedEntityData(Entity sourceEntity, List<UriParameter> navigationKeyParameters, EdmNavigationProperty edmNavigationProperty)
         throws ODataApplicationException
   {
      // Derived Product to Attribute Navigation
      if (sourceEntity instanceof DerivedProductEntity)
      {
         String tag = (String) sourceEntity.getProperty(ItemModel.PROPERTY_ID).getValue();
         String uuid = ((DerivedProductEntity) sourceEntity).getParentUuid();
         String keyParameterValue = DataHandlerUtil.getSingleStringKeyParameterValue(navigationKeyParameters, ItemModel.PROPERTY_ID);
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

         for (String xpath: ProductDataHandler.image_xpath_attributes)
         {
            Query query = new Query(xpath);
            DrbSequence results = query.evaluate(node);
            if ((results != null) && (results.getLength() > 0))
            {
               DrbNode result = (DrbNode) results.getItem(0);
               if (result.getName().equals(keyParameterValue))
               {
                  DrbDefaultAttribute attr = new DrbDefaultAttribute(result.getName(), result.getValue());
                  return drbAttributeToEntity(attr);
               }
            }
         }

         DrbAttributeList drbAttributeList = node.getAttributes();
         for (int index = 0; index < drbAttributeList.getLength(); index++)
         {
            DrbAttribute drbAttribute = drbAttributeList.item(index);
            if (drbAttribute.getName().equals(keyParameterValue))
            {
               return drbAttributeToEntity(drbAttribute);
            }
         }
      }
      // Product to Attribute Navigation
      else if (sourceEntity.getType().equals(ProductModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         String productUuid = (String) sourceEntity.getProperty(ItemModel.PROPERTY_ID).getValue();
         String keyParameterValue = DataHandlerUtil.getSingleStringKeyParameterValue(navigationKeyParameters, ItemModel.PROPERTY_ID);

         List<MetadataIndex> metadataIndex = productService.getIndexes(productUuid);
         for (MetadataIndex index: metadataIndex)
         {
            if (keyParameterValue.equals(index.getName()))
            {
               return indexToEntity(index);
            }
         }
      }
      // Node to Attribute Navigation
      else if (sourceEntity instanceof NodeEntity)
      {
         NodeEntity node = (NodeEntity) sourceEntity;
         DrbNode drbNode = node.getDrbNode();

         String attributeName = DataHandlerUtil.getSingleStringKeyParameterValue(navigationKeyParameters, ItemModel.PROPERTY_ID);
         DrbAttribute attribute = drbNode.getAttribute(attributeName);
         return attribute == null ? null : drbAttributeToEntity(attribute);
      }
      return null;
   }

   @Override
   public EntityCollection getRelatedEntityCollectionData(Entity sourceEntity, EdmNavigationProperty edmNavigationProperty)
         throws ODataApplicationException
   {
      EntityCollection navigationTargetEntityCollection = new EntityCollection();

      // Derived Product to Attributes Navigation
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

         for (String xpath: ProductDataHandler.image_xpath_attributes)
         {
            Query query = new Query(xpath);
            DrbSequence results = query.evaluate(node);
            if ((results != null) && (results.getLength() > 0))
            {
               DrbNode result = (DrbNode) results.getItem(0);
               if (result.getValue() != null)
               {
                  DrbDefaultAttribute attr = new DrbDefaultAttribute(result.getName(), result.getValue());
                  navigationTargetEntityCollection.getEntities().add(drbAttributeToEntity(attr));
               }
            }
         }

         DrbAttributeList drbAttributeList = node.getAttributes();
         for (int index = 0; index < drbAttributeList.getLength(); index++)
         {
            DrbAttribute drbAttribute = drbAttributeList.item(index);
            navigationTargetEntityCollection.getEntities().add(drbAttributeToEntity(drbAttribute));
         }
      }
      // Product to Attributes Navigation
      else if (sourceEntity.getType().equals(ProductModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         String productUuid = (String) sourceEntity.getProperty(ItemModel.PROPERTY_ID).getValue();
         List<MetadataIndex> metadataIndex = productService.getIndexes(productUuid);
         for (MetadataIndex index: metadataIndex)
         {
            navigationTargetEntityCollection.getEntities().add(indexToEntity(index));
         }
      }
      // Node to Attributes Navigation
      else if (sourceEntity instanceof NodeEntity)
      {
         NodeEntity node = (NodeEntity) sourceEntity;
         DrbNode drbNode = node.getDrbNode();

         DrbAttributeList drbAttributeList = drbNode.getAttributes();
         if (drbAttributeList != null)
         {
            for (int index = 0; index < drbAttributeList.getLength(); index++)
            {
               DrbAttribute drbAttribute = drbAttributeList.item(index);
               navigationTargetEntityCollection.getEntities().add(drbAttributeToEntity(drbAttribute));
            }
         }
      }
      return navigationTargetEntityCollection;
   }

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
}
