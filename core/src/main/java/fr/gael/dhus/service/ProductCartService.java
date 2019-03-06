/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2017 GAEL Systems
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

import fr.gael.dhus.database.dao.ProductCartDao;
import fr.gael.dhus.database.dao.ProductDao;
import fr.gael.dhus.database.dao.UserDao;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.ProductCart;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.olingo.v1.visitor.CartSQLVisitor;
import fr.gael.dhus.service.exception.ProductNotExistingException;
import fr.gael.dhus.service.exception.UserNotExistingException;
import fr.gael.dhus.util.BlockingObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Hibernate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product Service provides connected clients with a set of method to interact with it.
 */
@Service
public class ProductCartService extends WebService
{
   @Autowired
   private UserDao userDao;

   @Autowired
   private ProductCartDao productCartDao;

   @Autowired
   private ProductDao productDao;

   /**
    * Creates a cart for the passed user.
    *
    * @param uuid the user to create a new cart
    * @return the created cart
    * @throws UserNotExistingException when passed user is unknown
    */
   @PreAuthorize("hasRole('ROLE_DOWNLOAD')")
   @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
   public ProductCart createCartOfUser(String uuid)
   {
      return createCartOfUser(getUser(uuid));
   }

   /**
    * Creates a cart for the passed user.
    *
    * @param user the user to create a new cart
    * @return the created cart
    */
   @PreAuthorize("hasRole('ROLE_DOWNLOAD')")
   @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
   public ProductCart createCartOfUser(User user)
   {
      ProductCart cart = getCartOfUser(user);
      if (cart == null)
      {
         cart = new ProductCart();
         cart.setUser(user);
         return productCartDao.create(cart);
      }
      return cart;
   }

   /**
    * Removes a cart attached to a user.
    *
    * @param uuid the user to remove the cart
    * @throws UserNotExistingException when passed user is unknown
    */
   @PreAuthorize("hasRole('ROLE_DOWNLOAD')")
   @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
   public void deleteCartOfUser(String uuid)
   {
      ProductCart cart = getCartOfUser(uuid);
      if (cart != null)
      {
         productCartDao.delete(cart);
      }
   }

   /**
    * Get the cart of the related user. If the user has no cart configured, null is returned.
    *
    * @param uuid the related user to retrieve the cart
    * @return the cart
    * @throws UserNotExistingException when passed user is unknown
    */
   @PreAuthorize("hasRole('ROLE_DOWNLOAD')")
   @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
   public ProductCart getCartOfUser(String uuid) throws UserNotExistingException
   {
      return getCartOfUser(getUser(uuid));
   }

   /**
    * Get the cart of the given user. If the user has no cart configured, null is returned.
    *
    * @param user the related user to retrieve the cart
    * @return the cart
    */
   @PreAuthorize("hasRole('ROLE_DOWNLOAD')")
   @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
   public ProductCart getCartOfUser(User user)
   {
      ProductCart pc = productCartDao.getCartOfUser(user);
      if (pc != null)
      {
         Hibernate.initialize(pc.getProducts());
      }
      return pc;
   }

   /**
    * Add a product into a user's cart. Is user has no cart, it will be created.
    *
    * @param uuid id of the expected user
    * @param p_id id of the product to add
    * @throws UserNotExistingException    when passed user is unknown
    * @throws ProductNotExistingException when the passed to add does not exists
    */
   @PreAuthorize("hasRole('ROLE_DOWNLOAD')")
   @Transactional(propagation = Propagation.REQUIRED)
   public void addProductToCart(String uuid, Long p_id)
         throws UserNotExistingException, ProductNotExistingException
   {
      Product product = productDao.read(p_id);
      if (product == null)
      {
         throw new ProductNotExistingException();
      }

      User user = getUser(uuid);
      String key = "{" + uuid + "-" + p_id.toString() + "}";
      synchronized (BlockingObject.getBlockingObject(key))
      {
         ProductCart cart = getCartOfUser(user);
         if (cart == null)
         {
            cart = createCartOfUser(user);
         }
         if (cart.getProducts() == null)
         {
            cart.setProducts(new HashSet<Product>());
         }
         cart.getProducts().add(product);
         productCartDao.update(cart);
      }
   }

   /**
    * remove the specified product from cart.
    *
    * @param uuid user to remove the product
    * @param p_id product to be removed
    * @throws UserNotExistingException    when passed user is unknown
    * @throws ProductNotExistingException when the passed product to add does not exists
    */
   @PreAuthorize("hasRole('ROLE_DOWNLOAD')")
   @Transactional(propagation = Propagation.REQUIRED)
   public void removeProductFromCart(String uuid, Long p_id)
         throws UserNotExistingException, ProductNotExistingException
   {
      Product product = productDao.read(p_id);
      if (product == null)
      {
         throw new ProductNotExistingException();
      }
      ProductCart cart = getCartOfUser(uuid);
      if ((cart == null) || (cart.getProducts() == null))
      {
         return;
      }
      Iterator<Product> iterator = cart.getProducts().iterator();
      while (iterator.hasNext())
      {
         if (iterator.next().equals(product))
         {
            iterator.remove();
         }
      }
      productCartDao.update(cart);
   }

   /**
    * Retrieve the list of product ID from the cart of the passed user.
    *
    * @param uuid the user to retrieve the products
    * @return a list of product identifiers
    * @throws UserNotExistingException when passed user is unknown
    */
   @PreAuthorize("hasRole('ROLE_DOWNLOAD')")
   @Transactional(propagation = Propagation.REQUIRED)
   public List<Long> getProductsIdOfCart(String uuid) throws UserNotExistingException
   {
      return productCartDao.getProductsIdOfCart(getUser(uuid));
   }

   /**
    * Retrieve the product list from a product cart of a user.
    *
    * @param uuid the user to retrieve the products
    * @param skip product number to skip from the list
    * @param top  number of product to keep
    * @return the list of product within the passed window
    * @throws UserNotExistingException when passed user is unknown
    */
   @PreAuthorize("hasRole('ROLE_DOWNLOAD')")
   @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
   public List<Product> getProductsOfCart(String uuid, int skip, int top)
         throws UserNotExistingException
   {
      return productCartDao.scrollCartOfUser(getUser(uuid), skip, top);
   }

   /**
    * Count the number of products from a user's cart.
    *
    * @param uuid the user to retrieve the cart
    * @return the number of products in the cart
    * @throws UserNotExistingException when passed user is unknown
    */
   @PreAuthorize("hasRole('ROLE_DOWNLOAD')")
   @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
   public int countProductsInCart(String uuid)
         throws UserNotExistingException
   {
      ProductCart cart = getCartOfUser(uuid);
      if (cart == null)
      {
         return 0;
      }
      return cart.getProducts() == null ? 0 : cart.getProducts().size();
   }

   /**
    * $count on OData.
    *
    * @param visitor Expression visitor having visited the Filter and Order expressions
    * @param uuid UUID of the user
    * @return count of products in cart of given user
    */
   @PreAuthorize("hasRole('ROLE_DOWNLOAD')")
   @Transactional(readOnly = true)
   public int countProducts(CartSQLVisitor visitor, String uuid)
   {
      StringBuilder sb = new StringBuilder();
      sb.append("from ProductCart as cart inner join cart.products as products ");

      sb.append("where ");
      String filter = visitor.getHqlFilter();
      if (filter != null && !filter.isEmpty())
      {
         sb.append(filter).append(" and ");
      }
      sb.append("cart.user.uuid='").append(uuid).append('\'');
      return productCartDao.countHQLQuery(sb.toString(), visitor.getHqlParameters());
   }

   /**
    * /User('x')/Products on OData.
    *
    * @param visitor Expression visitor having visited the Filter and Order expressions
    * @param uuid UUID of the user
    * @param skip $skip parameter
    * @param top $top parameter
    * @return a non null list of products ordered and filtered
    */
   @PreAuthorize("hasRole('ROLE_DOWNLOAD')")
   @Transactional(readOnly = true)
   public List<Product> getCartProducts(CartSQLVisitor visitor, String uuid, int skip, int top)
   {
      StringBuilder sb = new StringBuilder();
      sb.append("select products from ProductCart as cart inner join cart.products as products ");

      sb.append("where ");
      String filter = visitor.getHqlFilter();
      if (filter != null && !filter.isEmpty())
      {
         sb.append(filter).append(" and ");
      }
      sb.append("cart.user.uuid='").append(uuid).append('\'');

      String order = visitor.getHqlOrder();
      if (order != null && !order.isEmpty())
      {
         sb.append(" order by ").append(order);
      }

      return productDao.executeHQLQuery(sb.toString(), visitor.getHqlParameters(), skip, top);
   }

   /**
    * Reports if the passed user has product in its cart.
    *
    * @param uuid the user to retrieve the cart
    * @return false is cart is empty, true otherwise
    */
   @PreAuthorize("hasRole('ROLE_DOWNLOAD')")
   @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
   public boolean hasProducts(String uuid)
   {
      return countProductsInCart(uuid) != 0;
   }

   /**
    * Empty cart of given user.
    *
    * @param uuid user UUID
    * @throws UserNotExistingException given user is unknown
    */
   @PreAuthorize("hasRole('ROLE_DOWNLOAD')")
   @Transactional(propagation = Propagation.REQUIRED)
   public void clearCart(String uuid) throws UserNotExistingException
   {
      ProductCart cart = getCartOfUser(uuid);
      if ((cart != null) && (cart.getProducts() != null))
      {
         cart.getProducts().clear();
         productCartDao.update(cart);
      }
   }

   /**
    * Get user owning given cart (identified by its UUID).
    *
    * @param uuid of cart
    * @return User owning given cart
    * @throws UserNotExistingException given user is unknown
    */
   @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
   private User getUser(String uuid) throws UserNotExistingException
   {
      User user = userDao.read(uuid);
      if (user == null)
      {
         throw new UserNotExistingException();
      }
      return user;
   }
}
