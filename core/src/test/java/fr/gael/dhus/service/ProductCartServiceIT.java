/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2015,2016,2018,2019 GAEL Systems
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

import fr.gael.dhus.database.dao.ProductDao;
import fr.gael.dhus.database.dao.UserDao;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.ProductCart;
import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.util.TestContextLoader;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@SuppressWarnings ("deprecation")
@ContextConfiguration(locations = "classpath:fr/gael/dhus/spring/context-service-test.xml", loader = TestContextLoader.class)
@DirtiesContext (classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ProductCartServiceIT extends AbstractTransactionalTestNGSpringContextTests
{
   Map<Integer, String> users = new HashMap<Integer, String>(2);
   {
      users.put(0, "test1");
      users.put(1, "test2");
   }

   // FIXME: security must be detached from the service !
   @BeforeClass
   public void authenticate()
   {
      int sb = new Random().nextBoolean() ? 1 : 0;
      String name = users.get(sb);

      // Workaround: @Transactional does not work here...
      TransactionTemplate txtp = new TransactionTemplate(transactionManager);
      txtp.executeWithoutResult((t) ->
      {
         User user = new User();
         user.setUsername(name);
         user.setPassword(name);
         user.setRoles(Collections.singletonList(Role.DOWNLOAD));
         userDao.create(user);
      });

      UsernamePasswordAuthenticationToken token =
         new UsernamePasswordAuthenticationToken(name, name);
      SecurityContextHolder.getContext().setAuthentication(token);
   }

   @Autowired
   PlatformTransactionManager transactionManager;

   @Autowired 
   ProductCartService pcs;
   
   @Autowired
   UserDao userDao;
   
   @Autowired
   ProductDao productDao;
   
   @Test (priority=10)
   @Transactional
   public void addProductToCart() throws MalformedURLException
   {
      User u = new User();
      u.setUsername ("TMPUSER##1");
      u.setPassword ("TMPPASS##1");
      u = userDao.create(u);
      
      Product p = new Product();
      p.setOnline(true);
      p = productDao.create(p);
      
      try
      {
         pcs.addProductToCart(u.getUUID (), p.getId());
      }
      catch (Exception e)
      {
         Assert.fail("Error while adding a product into a product cart.", e);
      }
   }

  @Test (priority=100)
  public void clearCart()
  {
     try
     {
        pcs.clearCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1000");
        Assert.fail("System remove cart of an unknown user ?");
     }
     catch (Exception e)
     {
        // Shall raise an exception because user is unknown.
     }
     
     try
     {
        pcs.clearCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3");
     }
     catch (Exception e)
     {
        Assert.fail("Cannot remove single cart of existing user.", e);
     }
     
     try
     {
        pcs.clearCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1");
     }
     catch (Exception e)
     {
        Assert.fail("Cannot remove empty cart of existing user.", e);
     }

     try
     {
        pcs.clearCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2");
     }
     catch (Exception e)
     {
        Assert.fail("Cannot remove multi-cart of existing user.", e);
     }
  }

  @Test (priority=1)
  public void countProductsInCart()
  {
     Assert.assertEquals(pcs.countProductsInCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0"), 2);
     Assert.assertEquals(pcs.countProductsInCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1"), 0);
     Assert.assertEquals(pcs.countProductsInCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2"), 0);
     Assert.assertEquals(pcs.countProductsInCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3"), 1);
  }
  
  @Test (priority=1)
  public void getCartOfUser()
  {
     // Users #0 and #3: normal users with single cart
     ProductCart cart = pcs.getCartOfUser("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0");
     Assert.assertEquals (cart.getUUID(), "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0");
     cart = pcs.getCartOfUser("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3");
     Assert.assertEquals (cart.getUUID(), "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1");

    
     // user #1 has no cart. The cart should be NULL.
     cart = pcs.getCartOfUser("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1");
     Assert.assertNull(cart);
    
     // User #2 has carts: first cart must be returned #2
     cart = pcs.getCartOfUser("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2");
     Assert.assertEquals (cart.getUUID(), "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2");
  }

  @Test (priority=1)
  public void getProductsIdOfCart()
  {
     // Users ID #0 -> CARD ID #0 -> PRODUCT IDs #0 and #5 
     List<Long>products = pcs.getProductsIdOfCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0");
     Assert.assertEquals (products.size(), 2);
     Assert.assertEquals (products.get (0), new Long(5L));
     Assert.assertEquals (products.get (1), new Long(0L));
     

     // USER #1 -> no cart
     products = pcs.getProductsIdOfCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1");
     Assert.assertEquals (products.size(), 0);

     
     // USER #2 -> CART #2 -> no product 
     //            CART #3 -> product #0
     // DAO methods returns only first cart if multiple... 
     products = pcs.getProductsIdOfCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2");
     Assert.assertEquals (products.size(), 0);

     // Users ID #3 -> CARD ID #1 -> PRODUCT IDs #5
     products = pcs.getProductsIdOfCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3");
     Assert.assertEquals (products.size(), 1);
     Assert.assertEquals (products.get (0), new Long(5L));
  }

  @Test (priority=1)
  public void getProductsOfCart()
  {
     // Users ID #0 -> CARD ID #0 -> PRODUCT IDs #0 and #5 
     List<Product>products = pcs.getProductsOfCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0", 0, 100);
     Assert.assertEquals (products.size(), 2);
     Assert.assertEquals (products.get (0).getId(), new Long(5L));
     Assert.assertEquals (products.get (1).getId(), new Long(0L));
     

     // USER #1 -> no cart
     products = pcs.getProductsOfCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1", 0 , 100);
     Assert.assertEquals (products.size(), 0);

     
     // USER #2 -> CART #2 -> no product 
     //            CART #3 -> product #0
     // DAO methods returns only first cart if multiple... 
     products = pcs.getProductsOfCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2", 0, 100);
     Assert.assertEquals (products.size(), 0);

     // Users ID #3 -> CARD ID #1 -> PRODUCT IDs #5
     products = pcs.getProductsOfCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3", 0, 100);
     Assert.assertEquals (products.size(), 1);
     Assert.assertEquals (products.get (0).getId(), new Long(5L));
  }

  @Test (priority=100)
  public void removeProductFromCart()
  {
     // Users ID #0 -> CARD ID #0 -> PRODUCT IDs #0 and #5
     // Remove product #0 
     pcs.removeProductFromCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0", 0L);
     
     // recheck it product list size is now 1
     List<Long>products = pcs.getProductsIdOfCart("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0");
     Assert.assertEquals (products.size(), 1);
     Assert.assertEquals (products.get (0), new Long(5L));
  }
  
  @Test (priority=1000)
  @Transactional
  public void createCartOfUser ()
  {
     User u = new User();
     u.setUsername ("TMPUSER##2");
     u.setPassword ("TMPPASS##2");
     u = userDao.create(u);
     
     ProductCart cart = pcs.createCartOfUser(u.getUUID());
     Assert.assertNotNull(cart);
     
     ProductCart cart_new = pcs.createCartOfUser(u.getUUID());
     Assert.assertEquals(cart, cart_new);
  }
  
  @Test (priority=1000)
  public void deleteCartOfUser ()
  {
     pcs.deleteCartOfUser("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0");
     Assert.assertNull (pcs.getCartOfUser("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0"));
  }

}
