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
package fr.gael.dhus.database.dao;

import fr.gael.dhus.database.dao.interfaces.HibernateDao;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.ProductCart;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Repository;

@Repository
public class ProductCartDao extends HibernateDao<ProductCart, String>
{
   @Autowired
   private ConfigurationManager conf;

   /**
    * Retrieves the cart of a specified user.
    *
    * @param user the user to retrieve the cart
    * @return the cart of the user, null if user has no cart
    */
   @SuppressWarnings("unchecked")
   public ProductCart getCartOfUser(final User user)
   {
      List<ProductCart> result =
            (List<ProductCart>) getHibernateTemplate().find("from ProductCart where user=?", user);
      return (result.isEmpty()) ? null : result.get(0);
   }

   /**
    * Deletes a cart entry from its owner user.
    *
    * @param user the owning user
    */
   public void deleteCartOfUser(User user)
   {
      ProductCart cart = getCartOfUser(user);
      if (cart != null)
      {
         delete(cart);
      }
   }

   /**
    * Removes all the references of one product from all the existing carts.
    *
    * @param product the product to be removed from carts
    */
   public void deleteProductReferences(final Product product)
   {
      getHibernateTemplate().execute(new HibernateCallback<Void>()
      {
         @Override
         public Void doInHibernate(Session session) throws HibernateException, SQLException
         {
            session.createSQLQuery("DELETE FROM CART_PRODUCTS p WHERE p.PRODUCT_ID = :pid")
                  .setParameter("pid", product.getId()).executeUpdate();
            return null;
         }
      });
   }

   /**
    * Computes a scrollable list of all products contained into a cart owned by specified user.
    * If the user has more than one cart, only content of the first one will be returned.
    * If the cart is empty or does not exists, or skip/top parameters are outside the limit
    * of the list, an empty list is returned.
    *
    * @param user the user owner of the cart to be retrieved
    * @param skip number of products to skip (-1 means 0)
    * @param top  number of product to kept (-1 means all the entries of the list)
    * @return the list of product according to the passed limitation
    * @see {@link ProductCartDao#getCartOfUser(User)}
    */
   public List<Product> scrollCartOfUser(final User user, final int skip, final int top)
   {
      if (user == null)
      {
         return Collections.emptyList();
      }
      ProductCart cart = getCartOfUser(user);
      if (cart == null)
      {
         return Collections.emptyList();
      }

      return setToLimitedList(cart.getProducts(), skip, top);
   }

   // Computes a limited list (skip, n) from a full list.
   private List<Product> setToLimitedList(Set<Product> input, int skip, int n)
   {
      List<Product> lst = new ArrayList<>();

      if (n < 1)
      {
         n = conf.getOdataConfiguration().getDefaultTop();
      }

      Iterator<Product> it = input.iterator();
      for (; skip > 0 && it.hasNext(); skip--)
      {
         it.next();
      }

      for (; n > 0 && it.hasNext(); n--)
      {
         lst.add(it.next());
      }
      return lst;
   }

   /**
    * Retrieve the entire list of products ids contained in the user cart.
    *
    * @param user to retrieve the cart
    * @return a list of ids
    * @see ProductCartDao#scrollCartOfUser(User, int, int)
    */
   public List<Long> getProductsIdOfCart(User user)
   {
      long start = System.currentTimeMillis();
      List<Long> ret = toId(scrollCartOfUser(user, 0, -1));
      long end = System.currentTimeMillis();
      logger.info("Query getProductsIdOfCart spent " + (end - start) + "ms");
      return ret;
   }

   // Convert a list of products to a list of these products ids....
   private List<Long> toId(List<Product> products)
   {
      List<Long> lst = new ArrayList<>();
      if (products != null)
      {
         for (Product product: products)
         {
            lst.add(product.getId());
         }
      }
      return lst;
   }
}
