/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2015-2018 GAEL Systems
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

import fr.gael.dhus.database.object.MetadataDefinition;
import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.util.TestContextLoader;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.store.StoreException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@ContextConfiguration (
      locations = { "classpath:fr/gael/dhus/spring/context-test.xml"},
      loader = TestContextLoader.class)
@DirtiesContext (classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCacheProductService
      extends AbstractTransactionalTestNGSpringContextTests
{
   private static final Logger LOGGER = LogManager.getLogger(TestCacheProductService.class);

   @Autowired
   private ProductService productService;

   @Autowired
   private CacheManager cacheManager;

   private Product productTest;

   @BeforeClass
   public void setUp () throws MalformedURLException
   {
      initProductTest ();
      authenticate ();
   }

   private void authenticate ()
   {
      String name = "userTest";
      Set<GrantedAuthority> roles = new HashSet<> ();
      roles.add (new SimpleGrantedAuthority (Role.DOWNLOAD.getAuthority ()));
      roles.add (new SimpleGrantedAuthority (Role.SEARCH.getAuthority ()));
      roles.add (
            new SimpleGrantedAuthority (Role.DATA_MANAGER.getAuthority ()));

      SandBoxUser user = new SandBoxUser (name, name, true, 0, roles);
      Authentication auth = new UsernamePasswordAuthenticationToken (
            user, user.getPassword (), roles);
      SecurityContextHolder.getContext ().setAuthentication (auth);

      logger.info ("userTest roles: " + auth.getAuthorities ());
   }

   @Test
   public void testIndexesCache ()
   {
      // initialize variables
      Long productId1 = 1L;
      String uuid1 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1";

      Long productId2 = 2L;
      String uuid2 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2";

      String productsCacheName = "products";
      String productIndexCacheName = "indexes";

      // retrieve or create metadata definition (cannot use MetadataFactory caused by dirty context)
      MetadataDefinition metaDefinition = ApplicationContextProvider.getBean(MetadataService.class)
            .getMetadataDefinition("a", "b", "c", "d");
      if (metaDefinition == null)
      {
         metaDefinition = new MetadataDefinition("a", "b", "c", "d");
      }
      MetadataIndex mi = new MetadataIndex(metaDefinition, "e");

      List<MetadataIndex> index = new ArrayList<> (5);
      index.add (mi);

      // validate cache
      Cache productCache = cacheManager.getCache(productsCacheName);
      Cache indexCache = cacheManager.getCache(productIndexCacheName);

      Assert.assertNull(productCache.get(uuid1, Product.class));
      Assert.assertNull(productCache.get(uuid2, Product.class));
      Assert.assertNull(indexCache.get(productId1, List.class));
      Assert.assertNull(indexCache.get(productId2, List.class));

      List<MetadataIndex> index1 = productService.getIndexes(productId1);
      Assert.assertTrue(equalCollection(indexCache.get(productId1, List.class), index1));
      Assert.assertNull(indexCache.get(productId2));

      List<MetadataIndex> index2 = productService.getIndexes(productId2);
      Assert.assertTrue(equalCollection(indexCache.get(productId2, List.class), index2));
   }

   @Test
   public void testProductCountCache () throws StoreException
   {
      fr.gael.dhus.database.object.Collection c =
            new fr.gael.dhus.database.object.Collection ();
      c.setUUID ("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1");
      c.setName ("Asia");
      String cache_name = "product_count";
      String filter = "upper(p.identifier) LIKE upper('%prod_%')";
      Object filter_collection_key = Arrays.asList (filter, c.getUUID ());
      Object filter_key = Arrays.asList (filter, null);
      Object all_key = "all";

      // countAuthorizedProducts ()
      Integer expected = productService.count();
      Cache cache = cacheManager.getCache(cache_name);
      Assert.assertEquals (cache.get (all_key, Integer.class), expected);

      // addProduct (Product)
      expected = expected + 1;
      productService.addProduct (productTest);
      Assert.assertNull (cache.get (filter_collection_key, Integer.class));
      Assert.assertNull (cache.get (filter_key, Integer.class));
      Assert.assertEquals (cache.get (all_key, Integer.class), expected);

      // deleteProduct (Long)
      expected = productService.count () - 1;
      String product7Uuid = productService.getProduct(7L).getUuid();
      productService.deleteByUuid(product7Uuid, false, null);
      Assert.assertEquals (cache.get (all_key, Integer.class), expected);

      // systemDeleteProduct (Long)
      expected = productService.count () - 1;
      String product6Uuid = productService.getProduct(6L).getUuid();
      productService.deleteByUuid(product6Uuid, false, null);
      Assert.assertEquals (cache.get (all_key, Integer.class), expected);
   }

   @Test
   public void testProductCache () throws MalformedURLException, StoreException
   {
      String cache_name = "product";
      String uuid;
      Long pid;
      Product product;

      // getProduct (Long)
      pid = 1L;
      product = productService.getProduct (pid);
      Cache cache = cacheManager.getCache (cache_name);
      Assert.assertEquals (cache.get (pid, Product.class), product);

      // addProduct (Product)
      uuid = "testaaaaaaaaaaaaaaaaaaaaaaaaaaa0";
      Assert.assertNull (cache.get (productTest.getId (), Product.class));
      Assert.assertNull (cache.get (productTest.getUuid (), Product.class));
      productService.addProduct (productTest);
      Assert.assertNotNull (cache.get (pid, Product.class));
      Assert.assertNotNull (cache.get (uuid, Product.class));
      Assert.assertNotNull (cache.get (productTest.getId (), Product.class));
      Assert.assertNotNull (cache.get (productTest.getUuid (), Product.class));

      // load cache 'product_count' with key 'all'
      Cache cacheCounter = cacheManager.getCache ("product_count");
      Integer number = productService.count ();

      // systemDeleteProduct (Long)
      product = productService.getProduct (0L); // load product by id
      productService.deleteByUuid(product.getUuid(), false, null);
      Assert.assertNull (cache.get (product.getId (), Product.class));
      Assert.assertNull (cache.get (product.getUuid (), Product.class));
      Assert.assertEquals (cacheCounter.get ("all", Integer.class).intValue (), (number - 1));
   }

   @BeforeMethod
   private void clearCache ()
   {
      LOGGER.info ("### clearing cache for test.");
      for (String cache_name : cacheManager.getCacheNames ())
      {
         cacheManager.getCache (cache_name).clear ();
      }
   }

   /**
    * Returns {@code true} if the two specified collections have all
    * elements in common.
    * <p/>
    * <p>Care must be exercised if this method is used on collections that
    * do not comply with the general contract for {@code Collection}.
    * Implementations may elect to iterate over either collection and test
    * for containment in the other collection (or to perform any equivalent
    * computation).  If either collection uses a nonstandard equality test
    * (as does a {@link SortedSet} whose ordering is not <em>compatible with
    * equals</em>, or the key set of an {@link IdentityHashMap}), both
    * collections must use the same nonstandard equality test, or the
    * result of this method is undefined.
    * <p/>
    * <p>Care must also be exercised when using collections that have
    * restrictions on the elements that they may contain. Collection
    * implementations are allowed to throw exceptions for any operation
    * involving elements they deem ineligible. For absolute safety the
    * specified collections should contain only elements which are
    * eligible elements for both collections.
    * <p/>
    * <p>Note that it is permissible to pass the same collection in both
    * parameters, in which case the method will return {@code true}.
    *
    * @param c1 a collection
    * @param c2 a collection
    * @return {@code true} if the two specified collections have all
    * elements in common.
    * @throws NullPointerException if either collection is {@code null}.
    * @throws NullPointerException if one collection contains a {@code null}
    *                              element and {@code null} is not an eligible element for the other collection.
    *                              (<a href="Collection.html#optional-restrictions">optional</a>)
    * @throws ClassCastException   if one collection contains an element that is
    *                              of a type which is ineligible for the other collection.
    *                              (<a href="Collection.html#optional-restrictions">optional</a>)
    */
   private boolean equalCollection (final Collection<?> c1,
         final Collection<?> c2)
   {
      // The collection to be used for contains(). Preference is given to
      // the collection who's contains() has lower O() complexity.
      Collection<?> contains = c2;
      // The collection to be iterated. If the collections' contains() impl
      // are of different O() complexity, the collection with slower
      // contains() will be used for iteration. For collections who's
      // contains() are of the same complexity then best performance is
      // achieved by iterating the smaller collection.
      Collection<?> iterate = c1;

      int c1size = c1.size ();
      int c2size = c2.size ();
      if (c1size == 0 && c2size == 0)
      {
         // Both collections are empty.
         return true;
      }

      if (c1size != c2size)
      {
         return false;
      }

      // Performance optimization cases. The heuristics:
      //   1. Generally iterate over c1.
      //   2. If c1 is a Set then iterate over c2.
      //   3. If either collection is empty then result is always true.
      //   4. Iterate over the smaller Collection.
      if (c1 instanceof Set || c1 instanceof RandomAccess)
      {
         // Use c1 for contains as a Set's contains() is expected to perform
         // better than O(N/2)
         iterate = c2;
         contains = c1;
      } else if (!(c2 instanceof Set || c2 instanceof RandomAccess))
      {
         iterate = c2;
         contains = c1;
      }

      for (Object e : iterate)
      {
         if (!contains.contains (e))
         {
            // Found an uncommon element.
            return false;
         }
      }

      // No uncommon elements were found.
      return true;
   }

   private void initProductTest () throws MalformedURLException
   {
      productTest = new Product ();
      productTest.setUuid ("testaaaaaaaaaaaaaaaaaaaaaaaaaaa0");
      productTest.setIdentifier ("test");
      productTest.setLocked (false);
      productTest.setOrigin ("space");
   }
}
