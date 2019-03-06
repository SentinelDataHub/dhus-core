/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2016,2017 GAEL Systems
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
package fr.gael.dhus.olingo.v1;

import fr.gael.dhus.database.object.DeletedProduct;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.olingo.v1.visitor.CartSQLVisitor;
import fr.gael.dhus.olingo.v1.visitor.DeletedProductSQLVisitor;
import fr.gael.dhus.olingo.v1.visitor.ProductSQLVisitor;
import fr.gael.dhus.olingo.v1.visitor.UserSQLVisitor;
import fr.gael.dhus.service.DeletedProductService;
import fr.gael.dhus.service.ProductCartService;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.service.UserService;

import java.util.List;

import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.uri.expression.ExceptionVisitExpression;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Product Service provides connected clients with a set of method to interact with it. */
@Service
public class OlingoManager
{
   @Autowired
   private DeletedProductService deletedProductService;

   @Autowired
   private ProductService productService;

   @Autowired
   private UserService userService;

   @Autowired
   private ProductCartService productCartService;

   public List<DeletedProduct> getDeletedProducts(FilterExpression filter_expr,
         OrderByExpression order_expr, int skip, int top)
         throws ExceptionVisitExpression, ODataApplicationException
   {
      DeletedProductSQLVisitor expV = new DeletedProductSQLVisitor(filter_expr, order_expr);
      return deletedProductService.getProducts(expV, skip, top);
   }

   public int getDeletedProductsNumber(FilterExpression filter_expr)
         throws ExceptionVisitExpression, ODataApplicationException
   {
      DeletedProductSQLVisitor expV = new DeletedProductSQLVisitor(filter_expr, null);
      return deletedProductService.countProducts(expV);
   }

   public List<Product> getProducts(FilterExpression filter_expr,
         OrderByExpression order_expr, int skip, int top)
         throws ExceptionVisitExpression, ODataApplicationException
   {
      return getProducts(null, filter_expr, order_expr, skip, top);
   }

   public List<Product> getProducts(String uuid, FilterExpression filter_expr,
         OrderByExpression order_expr, int skip,
         int top) throws ExceptionVisitExpression, ODataApplicationException
   {
      ProductSQLVisitor expV = new ProductSQLVisitor(filter_expr, order_expr);
      return productService.getProducts(expV, uuid, skip, top);
   }

   public int getProductsNumber(FilterExpression filter_expr)
         throws ExceptionVisitExpression, ODataApplicationException
   {
      // if no filter, using a count method with a smart cache
      if (filter_expr == null)
      {
         return productService.count();
      }
      return getProductsNumber(null, filter_expr);
   }

   public int getProductsNumber(String uuid, FilterExpression filter_expr)
            throws ExceptionVisitExpression, ODataApplicationException
   {
      ProductSQLVisitor expV = new ProductSQLVisitor(filter_expr, null);
      return productService.countProducts(expV, uuid);
   }

   /**
    * Filterable and sortable list of product from a user's cart.
    *
    * @param uuid user UUID.
    * @param filter_expr $filter expression
    * @param order_expr $orderby expression
    * @param skip $skip parameter
    * @param top $top parameter
    * @return A filtered and ordered list of product from givern user's cart
    * @throws ExceptionVisitExpression could not visit given filter/order expression
    * @throws ODataApplicationException other error
    */
   public List<Product> getProductCart(String uuid, FilterExpression filter_expr,
         OrderByExpression order_expr, int skip, int top)
         throws ExceptionVisitExpression, ODataApplicationException
   {
      CartSQLVisitor expV = new CartSQLVisitor(filter_expr, order_expr);
      return productCartService.getCartProducts(expV, uuid, skip, top);
   }

   /**
    * Filterable product of cart count ($count)
    *
    * @param uuid user UUID
    * @param filter_expr $filter expression
    * @return count of product in cart of given user that validate the given filter
    * @throws ExceptionVisitExpression could not visit given filter expression
    * @throws ODataApplicationException other error
    */
   public int getCartProductCount(String uuid, FilterExpression filter_expr)
         throws ExceptionVisitExpression, ODataApplicationException
   {
      CartSQLVisitor expV = new CartSQLVisitor(filter_expr, null);
      return productCartService.countProducts(expV, uuid);
   }

   public List<User> getUsers(
         FilterExpression filter_expr, OrderByExpression order_expr, int skip,
         int top) throws ExceptionVisitExpression, ODataApplicationException
   {
      UserSQLVisitor expV = new UserSQLVisitor(filter_expr, order_expr);
      return userService.getUsers(expV, skip, top);
   }

   public int getUsersNumber(FilterExpression filter_expr)
         throws ExceptionVisitExpression, ODataApplicationException
   {
      UserSQLVisitor expV = new UserSQLVisitor(filter_expr, null);
      return userService.countUsers(expV);
   }
}
