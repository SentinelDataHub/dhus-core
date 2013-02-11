/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2015-2019 GAEL Systems
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
import fr.gael.dhus.database.object.Search;
import fr.gael.dhus.util.TestContextLoader;

import java.math.BigInteger;

import org.hibernate.HibernateException;
import org.hibernate.query.NativeQuery;
import org.hibernate.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.test.context.ContextConfiguration;

import org.testng.Assert;

@ContextConfiguration(locations = "classpath:fr/gael/dhus/spring/context-dao-test.xml", loader = TestContextLoader.class)
public class SearchDaoIT extends AbstractHibernateDaoIT<Search, String>
{
   @Autowired
   private SearchDao dao;

   @Override
   protected HibernateDao<Search, String> getHibernateDao ()
   {
      return dao;
   }

   @Override
   protected int howMany ()
   {
      return 4;
   }
   
   @Override
   public void create ()
   {
      String value = "search_value";
      String complete = "search_complete";
      String footprint = "search_footprint";
      String polygone = "polygone";
      String france = "France";

      Search search = new Search ();
      search.setValue (value);
      search.setComplete (complete);
      search.setFootprint (footprint);
      search.setNotify (false);
      search.getAdvanced ().put (polygone, france);

      search = dao.create (search);
      Assert.assertEquals (dao.count (), (howMany () + 1));
      Assert.assertNotNull (search);
      Assert.assertEquals (search.getAdvanced ().get (polygone), france);
      Assert.assertEquals (search.getComplete (), complete);
      Assert.assertEquals (search.getValue (), value);
      Assert.assertFalse (search.isNotify ());
   }

   @Override
   public void read ()
   {
      Search s = dao.read ("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0");
      Assert.assertNotNull (s);
      Assert.assertEquals (s.getValue (), "value0");
      Assert.assertEquals (s.getAdvanced ().get ("advanceKey"), "advanceValue");
   }

   @Override
   public void update ()
   {
      String id = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1";
      Search s = dao.read (id);
      boolean notify = !s.isNotify ();
      String advancedKey = "toto";
      String advancedValue = "titi";

      s.setNotify (notify);
      s.getAdvanced ().put (advancedKey, advancedValue);
      dao.update (s);

      s = dao.read (id);
      Assert.assertEquals (s.isNotify (), notify);
      Assert.assertEquals (s.getAdvanced ().get (advancedKey), advancedValue);
   }

   private int countAdvanced (final String sid)
   {
      return dao.getHibernateTemplate ().execute (
         new HibernateCallback<Integer> ()
         {
            @Override
            public Integer doInHibernate (Session session)
               throws HibernateException
            {
               String hql =
                  "SELECT count(*) FROM SEARCH_ADVANCED WHERE SEARCH_UUID = ?1";
               NativeQuery query = session.createNativeQuery (hql);
               query.setParameter(1, sid);
               return ((BigInteger) query.uniqueResult ()).intValue ();
            }
         });
   }

   @Override
   public void delete ()
   {
      String sid = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0";
      Search search = dao.read (sid);
      Assert.assertNotNull (search);
      Assert.assertFalse (search.getAdvanced ().isEmpty ());

      dao.delete (search);
      Assert.assertEquals (dao.count (), (howMany () - 1));
      Assert.assertNull (dao.read (sid));
      Assert.assertEquals (countAdvanced (sid), 0);
   }

   @Override
   public void first ()
   {
      String hql = "FROM Search WHERE notify IS TRUE ORDER BY uuid DESC";
      Search search = dao.first (hql);
      Assert.assertNotNull (search);
      Assert.assertEquals (search.getUUID (), "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2");
   }

   // TODO merge others test
}
