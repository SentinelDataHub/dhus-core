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
package fr.gael.dhus.database.dao;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import fr.gael.dhus.database.dao.interfaces.HibernateDao;
import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.Product.Download;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.factory.MetadataFactory;
import fr.gael.dhus.util.TestContextLoader;

@ContextConfiguration (
      locations = "classpath:fr/gael/dhus/spring/context-test.xml",
      loader = TestContextLoader.class)
@DirtiesContext (classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestProductDao extends TestAbstractHibernateDao<Product, Long>
{

   @Autowired
   private ProductDao dao;

   @Override
   protected HibernateDao<Product, Long> getHibernateDao ()
   {
      return dao;
   }

   @Override
   protected int howMany ()
   {
      return 8;
   }

   @Override
   public void create ()
   {
      String identifier = "test-create-product";
      String indexName = "index-name";
      String indexCategory = "category";
      String indexValue = "test";

      MetadataIndex mi = MetadataFactory.createMetadataIndex(indexName, null, indexCategory, null, indexValue);

      Product product = new Product ();
      product.setIdentifier (identifier);
      product.setLocked (false);
      product.setIndexes (Arrays.asList (mi));
      product.setOnline(true);

      Product createdProduct = dao.create (product);
      Assert.assertNotNull (createdProduct);
      Assert.assertEquals (dao.count (), (howMany () + 1));
      Assert.assertEquals (createdProduct.getUuid (), product.getUuid ());

      List<MetadataIndex> indexes = createdProduct.getIndexes ();
      Assert.assertEquals (indexes.size (), 1);
      Assert.assertEquals (indexes.get (0), mi);
   }

   @Override
   public void read ()
   {
      Product p = dao.read (6L);
      Assert.assertNotNull (p);
      Assert.assertEquals (p.getIdentifier (), "prod6");
      Download dl = p.getDownload ();
      Map<String, String> checksums = dl.getChecksums ();
      Assert.assertEquals (checksums.get ("MD5"), "abc");
   }

   @Override
   public void update ()
   {
      String productIdentifier = "test-prod-7";
      String indexName = "updatable";
      Long pid = Long.valueOf (7);

      Product product = dao.read (pid);
      List<MetadataIndex> indexes = product.getIndexes ();
      product.setIdentifier (productIdentifier);
      for (MetadataIndex mi : indexes)
      {
         mi.setName (indexName);
      }
      dao.setIndexes (pid, indexes);
      dao.update (product);

      product = dao.read (pid);
      indexes = product.getIndexes ();
      Assert.assertNotNull (product);
      Assert.assertEquals (product.getIdentifier (), productIdentifier);
      for (MetadataIndex mi : indexes)
      {
         Assert.assertEquals (mi.getName (), indexName);
      }
   }

   private int countElements (final String table, final Long pid)
   {
      return dao.getHibernateTemplate ().execute (
            new HibernateCallback<Integer> ()
            {
               @Override
               public Integer doInHibernate (Session session)
                     throws HibernateException, SQLException
               {
                  String sql =
                        "SELECT count(*) FROM " + table +
                              " WHERE PRODUCT_ID = ?";
                  Query query = session.createSQLQuery (sql).setLong (0, pid);
                  return ((BigInteger) query.uniqueResult ()).intValue ();
               }
            });
   }

   @Override
  public void delete ()
   {
      cancelListeners (getHibernateDao ());

      Long pid = Long.valueOf (6L);
      Product product = dao.read (pid);
      Assert.assertNotNull (product);

      List<MetadataIndex> indexes = product.getIndexes ();
      Assert.assertNotNull (indexes);
      Assert.assertFalse (indexes.isEmpty ());

      dao.delete (product);
      getHibernateDao ().getSessionFactory ().getCurrentSession ().flush ();

      Assert.assertNull (dao.read (pid));
      Assert.assertEquals (countElements ("METADATA_INDEXES", pid), 0);
      Assert.assertEquals (countElements ("CHECKSUMS", pid), 0);
   }

   @Override
   public void first () {}

   @Test
   public void getProductByUuid ()
   {
      String valid = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa6";
      String invalid = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb6";
      User user = new User ();
      user.setUUID ("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0");
      user.setUsername ("koko");

      Product product = dao.getProductByUuid(valid);
      Assert.assertNotNull (product);
      Assert.assertEquals (product.getId ().intValue (), 6);

      product = dao.getProductByUuid(invalid);
      Assert.assertNull (product);

      product = dao.getProductByUuid(valid);
      Assert.assertNotNull (product);
      Assert.assertEquals (product.getId ().intValue (), 6);

      product = dao.getProductByUuid(null);
      Assert.assertNull (product);
   }

   // TODO merge others test

   @Test (groups={"non-regression"})
   public void testChecksumUpdate () throws MalformedURLException
   {
      Product product = new Product ();
      product.setOnline(true);

      Download download = new Product.Download ();
      download.setType ("application/octet-stream");
      download.setChecksums (
         Maps.newHashMap (ImmutableMap.of(
            "MD5", "54ABCDEF98765",
            "SHA-1", "9876FEDCBA1234")));

      product.setDownload (download);

      // First create the defined product:
      try
      {
         product = dao.create (product);
      }
      catch (Exception e)
      {
         Assert.fail ("Creation of product fails", e);
      }

      /**
       * Clear/putAll feature testing
       */
      product.getDownload ().getChecksums ().clear ();
      product.getDownload ().getChecksums ().putAll (
         Maps.newHashMap (ImmutableMap.of(
            "SHA-256", "4554ABCDEF98765",
            "SHA-512", "ABDEFFE9876FEDCBA1234")));
      try
      {
         dao.update (product);
      }
      catch (Exception e)
      {
         Assert.fail ("Modifying checksums with map clear/put fails", e);
      }

      /**
       * Set feature testing
       */
      product.getDownload ().setChecksums (
         Maps.newHashMap (ImmutableMap.of(
            "MD5", "54ABCDEF98765",
            "SHA-1", "9876FEDCBA1234")));
      try
      {
         dao.update (product);
      }
      catch (Exception e)
      {
         Assert.fail ("Modifying checksums with \"set\" fails", e);
      }

      /**
       * Remove residuals for this test
       */
      cancelListeners (getHibernateDao ());
      dao.delete (product);

   }

}
