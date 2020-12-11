/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019,2020 GAEL Systems
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

import fr.gael.dhus.database.object.Order;
import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.service.OrderService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.odata.engine.data.DataHandlerUtil;
import fr.gael.odata.engine.data.DatabaseDataHandler;

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
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;

import org.dhus.olingo.v2.ODataSecurityManager;
import org.dhus.olingo.v2.datamodel.ItemModel;
import org.dhus.olingo.v2.datamodel.JobModel;
import org.dhus.olingo.v2.datamodel.OrderModel;
import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.olingo.v2.visitor.OrderSQLVisitor;

public class OrderDataHandler implements DatabaseDataHandler
{
   private static final OrderService ORDER_SERVICE =
         ApplicationContextProvider.getBean(OrderService.class);

   public static String makeOrderId(Order order)
   {
      return order.getOrderId().getDataStoreName().concat("-").concat(order.getOrderId().getProductUuid());
   }

   public static String extractUuidFromOrderId(String orderId)
   {
      return orderId.substring(orderId.length() - 36, orderId.length());
   }

   public static Entity toOlingoEntity(Order order)
   {
      Entity orderEntity = new Entity();

      String id = makeOrderId(order);

      // orderId
      orderEntity.addProperty(new Property(
            null,
            JobModel.PROPERTY_ID,
            ValueType.PRIMITIVE,
            id));

      // status
      orderEntity.addProperty(new Property(
            null,
            JobModel.PROPERTY_STATUS,
            ValueType.ENUM,
            order.getStatus().value()));

      // estimatedTime
      orderEntity.addProperty(new Property(
            null,
            JobModel.PROPERTY_ESTIMATED_TIME,
            ValueType.PRIMITIVE,
            order.getEstimatedTime()));

      // submissionTime
      orderEntity.addProperty(new Property(
            null,
            JobModel.PROPERTY_SUBMISSION_TIME,
            ValueType.PRIMITIVE,
            order.getSubmissionTime()));

      // StatusMessage
      orderEntity.addProperty(new Property(
            null,
            JobModel.PROPERTY_STATUS_MESSAGE,
            ValueType.PRIMITIVE,
            order.getStatusMessage()));

      orderEntity.setId(DataHandlerUtil.createEntityId(OrderModel.ENTITY_SET_NAME, id));
      orderEntity.setType(OrderModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());

      return orderEntity;
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      String orderId = DataHandlerUtil.getSingleStringKeyParameterValue(keyParameters, JobModel.PROPERTY_ID);
      String productUuid = extractUuidFromOrderId(orderId);

      Order order = ORDER_SERVICE.getOrderByProductUuid(productUuid);
      if (order != null)
      {
         if (ODataSecurityManager.hasPermission(Role.SYSTEM_MANAGER)
               || order.hasOwner(ODataSecurityManager.getCurrentUser()))
         {
            return toOlingoEntity(order);
         }
      }
      return null;
   }

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
     return getEntityCollectionData(null, null, null, null, null);
   }

   @Override
   public Entity getRelatedEntityData(Entity entity, EdmNavigationProperty edmNavigationProperty)
         throws ODataApplicationException
   {
      String type = entity.getType();

      // No product available from derived product
      if (entity instanceof DerivedProductEntity)
      {
         throw new ODataApplicationException("Invalid navigation from derived product to "
               + OrderModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString(),
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
      else if (ProductModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString().equals(type))
      {
         String uuid = (String) entity.getProperty(ItemModel.PROPERTY_ID).getValue();

         Order order = ORDER_SERVICE.getOrderByProductUuid(uuid);

         if (order.getOrderId().getProductUuid().equals(uuid))
         {
            return toOlingoEntity(order);
         }

         throw new ODataApplicationException("Order not found for product: " + uuid,
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }
      throw new ODataApplicationException("Invalid navigation from " + type + " to "
            + OrderModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString(),
            HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
   }

   @Override
   public void deleteEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      String orderId = DataHandlerUtil.getSingleStringKeyParameterValue(keyParameters, JobModel.PROPERTY_ID);
      String productUuid = extractUuidFromOrderId(orderId);

      ORDER_SERVICE.deleteOrderByProductUUID(productUuid);
   }

   @Override
   public EntityCollection getEntityCollectionData(FilterOption filterOption, OrderByOption orderByOption,
         TopOption topOption, SkipOption skipOption, CountOption countOption)
         throws ODataApplicationException
   {
      OrderSQLVisitor orderSqlVisitor = new OrderSQLVisitor(filterOption, orderByOption, topOption, skipOption);
      // FIXME full list in memory: paginate or stream
      List<Order> ordersCollections;
      if (ODataSecurityManager.hasPermission(Role.SYSTEM_MANAGER))
      {
         ordersCollections = ORDER_SERVICE.getOrders(orderSqlVisitor);
      }
      else
      {
         ordersCollections = ORDER_SERVICE.getOrdersOfUser(orderSqlVisitor, ODataSecurityManager.getCurrentUser());
      }

      EntityCollection entityCollection = new EntityCollection();
      for (Order order: ordersCollections)
      {
         Entity entityProduct = toOlingoEntity(order);
         entityCollection.getEntities().add(entityProduct);
      }

      // handle count, must ignore skip & top
      if (countOption != null && countOption.getValue())
      {
         // FIXME unnecessary extra request to database?
         entityCollection.setCount(countEntities(filterOption));
      }

      return entityCollection;
   }

   @Override
   public Integer countEntities(FilterOption filterOption)
         throws ODataApplicationException
   {
      OrderSQLVisitor orderSqlVisitor = new OrderSQLVisitor(filterOption, null, null, null);
      if (ODataSecurityManager.hasPermission(Role.SYSTEM_MANAGER))
      {
         return ORDER_SERVICE.countOrders(orderSqlVisitor);
      }
      return ORDER_SERVICE.countOrdersOfUser(orderSqlVisitor, ODataSecurityManager.getCurrentUser());
   }
}
