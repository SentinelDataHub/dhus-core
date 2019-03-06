/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013-2017 GAEL Systems
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import fr.gael.dhus.database.dao.interfaces.HibernateDao;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.ProductCart;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.util.CheckIterator;
import fr.gael.dhus.util.TestContextLoader;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import org.testng.annotations.Test;

@ContextConfiguration(
      locations = "classpath:fr/gael/dhus/spring/context-test.xml",
      loader = TestContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestProductCartDao extends TestAbstractHibernateDao<ProductCart, String>
{
   @Autowired
   private ProductCartDao dao;

   @Autowired
   private ProductDao pdao;

   @Autowired
   private UserDao udao;

   @Override
   protected HibernateDao<ProductCart, String> getHibernateDao()
   {
      return dao;
   }

   @Override
   protected int howMany()
   {
      return 3;
   }

   @Override
   public void create()
   {
      User user = udao.read("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2");
      ProductCart pc = new ProductCart();
      pc.setUser(user);

      pc = dao.create(pc);
      assertNotNull(pc);
      assertEquals(pc.getUser(), user);
   }

   @Override
   public void read()
   {
      ProductCart pc = dao.read("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0");
      assertNotNull(pc);
      assertEquals(pc.getProducts().size(), 2);
   }

   @Override
   public void update()
   {
      String cartId = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0";
      ProductCart pc = dao.read(cartId);
      Product p = new Product();
      p.setId(3L);

      assertEquals(pc.getProducts().size(), 2);
      pc.getProducts().add(p);
      dao.update(pc);

      pc = dao.read(cartId);
      assertEquals(pc.getProducts().size(), 3);
   }

   @Override
   public void delete()
   {
      String id = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0";
      ProductCart cart = dao.read(id);
      assertNotNull(cart);
      Set<Product> products = cart.getProducts();

      dao.delete(cart);
      assertNull(dao.read(id));
      for (Product product: products)
      {
         assertNotNull(pdao.read(product.getId()));
      }
   }

   @Override
   public void first()
   {
      ProductCart cart = dao.first("FROM ProductCart ORDER BY id DESC");
      assertNotNull(cart);
      assertEquals(cart.getUUID(), "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2");
   }

   @Test
   public void deleteCartOfUser()
   {
      // emulate userDao.read (2L)
      User u = new User();
      u.setUUID("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2");

      dao.deleteCartOfUser(u);
      ProductCart res = dao.getCartOfUser(u);
      assertNull(res);
   }

   @Test
   public void getCartOfUser()
   {
      User user = udao.read("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3");
      ProductCart cart = dao.getCartOfUser(user);
      assertNotNull(cart);
      assertEquals(cart.getUUID(), "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1");
      assertEquals(cart.getUser(), user, "User not owner of the found cart.");

      // No cart attached to User #1
      user = udao.read("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1");
      cart = dao.getCartOfUser(user);
      assertNull(cart);

      // User #2 has 2 carts
      user = udao.read("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2");
      cart = dao.getCartOfUser(user);
      assertNotNull(cart);
   }

   @Test
   public void deleteProductReferences()
   {
      Product product = new Product();
      product.setId(5L);
      dao.deleteProductReferences(product);

      List<ProductCart> pcs = dao.readAll();
      for (ProductCart pc: pcs)
      {
         assertFalse(pc.getProducts().contains(product));
      }
   }

   @Test
   public void getProductsIdOfCart()
   {
      User user = new User();
      user.setUUID("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0");

      List<Long> ids = dao.getProductsIdOfCart(user);
      assertNotNull(ids);

      assertEquals(ids.size(), 2);
      assertTrue(ids.contains(Long.valueOf(0)));
      assertTrue(ids.contains(Long.valueOf(5)));
   }

   @Test
   public void scrollCartOfUser()
   {
      User user = new User();
      user.setUUID("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0");

      Iterator<Product> it = dao.scrollCartOfUser(user, -1, -1).iterator();
      assertTrue(CheckIterator.checkElementNumber(it, 2));

      it = dao.scrollCartOfUser(user, 0, 10000).iterator();
      assertTrue(CheckIterator.checkElementNumber(it, 2));

      it = dao.scrollCartOfUser(user, 1, 10).iterator();
      assertTrue(CheckIterator.checkElementNumber(it, 1));

      it = dao.scrollCartOfUser(user, 2, 10).iterator();
      assertTrue(CheckIterator.checkElementNumber(it, 0));

      it = dao.scrollCartOfUser(user, 10, 10).iterator();
      assertTrue(CheckIterator.checkElementNumber(it, 0));

   }

}
