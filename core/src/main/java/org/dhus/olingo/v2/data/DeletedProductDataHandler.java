/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019 GAEL Systems
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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.dhus.olingo.v2.ODataSecurityManager;
import org.dhus.olingo.v2.datamodel.DeletedProductModel;
import org.dhus.olingo.v2.datamodel.ItemModel;
import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.olingo.v2.datamodel.action.DeleteDeletedProductsAction;
import org.dhus.olingo.v2.datamodel.complex.ChecksumComplexType;
import org.dhus.olingo.v2.datamodel.complex.TimeRangeComplexType;
import org.dhus.olingo.v2.visitor.DeletedProductSqlVisitor;

import fr.gael.dhus.database.object.DeletedProduct;
import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.olingo.Security;
import fr.gael.dhus.service.DeletedProductService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.odata.engine.data.DataHandlerUtil;
import fr.gael.odata.engine.data.DatabaseDataHandler;


public class DeletedProductDataHandler implements DatabaseDataHandler
{
   private static final DeletedProductService DELETED_PRODUCT_SERVICE =
         ApplicationContextProvider.getBean(DeletedProductService.class);

   private static final Logger LOGGER = LogManager.getLogger(ProductDataHandler.class);

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      return getEntityCollectionData(null, null, null, null, null);
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      for (UriParameter uriParameter : keyParameters)
      {
         if (DeletedProductModel.PROPERTY_ID.equals(uriParameter.getName()))
         {
            String uuid = DataHandlerUtil.trimStringKeyParameter(uriParameter);
            DeletedProduct deletedProduct = DELETED_PRODUCT_SERVICE.getProduct(uuid);
            return toDeletedProductEntity(deletedProduct);
         }
      }
      return null;
   }

   @Override
   public EntityCollection getEntityCollectionData(FilterOption filterOption,
         OrderByOption orderByOption, TopOption topOption, SkipOption skipOption,
         CountOption countOption) throws ODataApplicationException
   {
      DeletedProductSqlVisitor visitor = new DeletedProductSqlVisitor(filterOption, orderByOption, topOption, skipOption);
      List<DeletedProduct> deletedProducts = DELETED_PRODUCT_SERVICE.getProducts(visitor, visitor.getSkip(), visitor.getTop());

      EntityCollection entityCollection = new EntityCollection();
      for (DeletedProduct deletedProduct: deletedProducts)
      {
         Entity entityProduct = toDeletedProductEntity(deletedProduct);
         entityCollection.getEntities().add(entityProduct);
      }
      return entityCollection;
   }

   @Override
   public Integer countEntities(FilterOption filterOption) throws ODataApplicationException
   {
      return DELETED_PRODUCT_SERVICE.count();
   }

   @Override
   public Object performBoundActionEntityCollection(UriInfo uriInfo,
         List<UriParameter> keyPredicates, EdmAction action, Map<String, Parameter> parameters)
         throws ODataApplicationException
   {
      FullQualifiedName actionFQN = action.getFullQualifiedName();
      if (actionFQN.equals(DeleteDeletedProductsAction.ACTION_DELETE_PRODUCTS_FQN))
      {
         if (parameters == null || parameters.isEmpty())
         {
            LOGGER.error("Parameters need to be filled");
            throw new ODataApplicationException("Parameters need to be filled",
                  HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
         }
         performDeleteProductListBoundAction(parameters);

         return new Property(null, DeleteDeletedProductsAction.PARAMETER_PRODUCT_LIST, ValueType.PRIMITIVE,
               "The deletion is successfully done");
      }
      throw new ODataApplicationException("Action not found",
            HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
   }

   private void performDeleteProductListBoundAction(Map<String, Parameter> parameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.DATA_MANAGER);
      EntityCollection productParameters =
         (EntityCollection) parameters.get(DeleteDeletedProductsAction.PARAMETER_PRODUCT_LIST).getValue();

      List<Entity> entities = productParameters.getEntities();

      for (Entity entity : entities)
      {
         String uuid = (String) entity.getProperty("Id").getValue();

         DeletedProduct deletedProduct = DELETED_PRODUCT_SERVICE.getProduct(uuid);
         if (deletedProduct == null)
         {
            LOGGER.error("The product id {} is not valid", uuid);
         }
         long start = System.currentTimeMillis();
         DELETED_PRODUCT_SERVICE.delete(deletedProduct);

         long totalTime = System.currentTimeMillis() - start;
         LOGGER.info("Deletion of product '{}' ({} bytes) successful spent {}ms",
               deletedProduct.getIdentifier(), deletedProduct.getSize(), totalTime);
      }
   }

   private Entity toDeletedProductEntity(DeletedProduct deletedProduct)
   {
      if (deletedProduct == null)
      {
         return null;
      }

      Entity deletedProductEntity = new Entity();

      // UUID
      deletedProductEntity.addProperty(new Property(
            null,
            DeletedProductModel.PROPERTY_ID,
            ValueType.PRIMITIVE,
            deletedProduct.getUuid()));

      // Name
      deletedProductEntity.addProperty(new Property(
            null,
            DeletedProductModel.PROPERTY_NAME,
            ValueType.PRIMITIVE,
            deletedProduct.getIdentifier()));

      // CreationDate
      deletedProductEntity.addProperty(new Property(
            null,
            DeletedProductModel.PROPERTY_CREATION_DATE,
            ValueType.PRIMITIVE,
            deletedProduct.getCreated()));

      // FootPrint
      deletedProductEntity.addProperty(new Property(
            null,
            DeletedProductModel.PROPERTY_FOOTPRINT,
            ValueType.PRIMITIVE,
            deletedProduct.getFootPrint()));

      // Size
      deletedProductEntity.addProperty(new Property(
            null,
            DeletedProductModel.PROPERTY_SIZE,
            ValueType.PRIMITIVE,
            deletedProduct.getSize()));

      // IngestionDate
      deletedProductEntity.addProperty(new Property(
            null,
            DeletedProductModel.PROPERTY_INGESTION_DATE,
            ValueType.PRIMITIVE,
            deletedProduct.getIngestionDate()));

      // DeletionDate
      deletedProductEntity.addProperty(new Property(
            null,
            DeletedProductModel.PROPERTY_DELETION_DATE,
            ValueType.PRIMITIVE,
            deletedProduct.getDeletionDate()));

      // DeletionCause
      deletedProductEntity.addProperty(new Property(
            null,
            DeletedProductModel.PROPERTY_DELETION_CAUSE,
            ValueType.PRIMITIVE,
            deletedProduct.getDeletionCause()));

      // Checksum
      deletedProductEntity.addProperty(checksumProperty(deletedProduct));

      // TimeRange (Begin-End positions)
      deletedProductEntity.addProperty(timeRangeProperty(deletedProduct.getContentStart(), deletedProduct.getContentEnd()));

      deletedProductEntity.setId(DataHandlerUtil.createEntityId(DeletedProductModel.ENTITY_SET_NAME, deletedProduct.getUuid()));

      return deletedProductEntity;
   }

   private Property checksumProperty(DeletedProduct deletedProduct)
   {
      List<ComplexValue> checksumComplexCollection = new ArrayList<>();
      Map<String, String> checksumsMap = deletedProduct.getChecksums();
      if (checksumsMap != null)
      {
         for (Entry<String, String> checksum : checksumsMap.entrySet())
         {
            ComplexValue checksumValue = new ComplexValue();
            // checksum algorithm
            checksumValue.getValue().add(new Property(
                  null,
                  ChecksumComplexType.PROPERTY_ALGORITHM,
                  ValueType.PRIMITIVE,
                  checksum.getKey()));

            // checksum value
            checksumValue.getValue().add(new Property(
                  null,
                  ChecksumComplexType.PROPERTY_VALUE,
                  ValueType.PRIMITIVE,
                  checksum.getValue()));

            checksumComplexCollection.add(checksumValue);
         }
         return new Property(
               null,
               ChecksumComplexType.COMPLEX_TYPE_NAME,
               ValueType.COLLECTION_COMPLEX,
               checksumComplexCollection);
      }
      return null;
   }

   private Property timeRangeProperty(Date contentStart, Date contentEnd)
   {
      ComplexValue complexValue = new ComplexValue();
      complexValue.getValue().add(new Property(
            null,
            TimeRangeComplexType.PROPERTY_START,
            ValueType.PRIMITIVE,
            new Timestamp(contentStart.getTime())));
      complexValue.getValue().add(new Property(
            null,
            TimeRangeComplexType.PROPERTY_END,
            ValueType.PRIMITIVE,
            new Timestamp(contentEnd.getTime())));

      return new Property(
            null,
            ProductModel.PROPERTY_CONTENTDATE,
            ValueType.COMPLEX,
            complexValue);
   }

   @Override
   public void deleteEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.DATA_MANAGER);
      String productId = DataHandlerUtil.getSingleStringKeyParameterValue(keyParameters, ItemModel.PROPERTY_ID);

      DeletedProduct deletedProduct = DELETED_PRODUCT_SERVICE.getProduct(productId);
      if (deletedProduct == null)
      {
         throw new ODataApplicationException("Deleted product not found",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }
      DELETED_PRODUCT_SERVICE.delete(deletedProduct);
   }
}
