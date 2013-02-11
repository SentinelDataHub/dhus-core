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
import fr.gael.dhus.database.object.NetworkUsage;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.util.TestContextLoader;

import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@ContextConfiguration(locations = "classpath:fr/gael/dhus/spring/context-dao-test.xml", loader = TestContextLoader.class)
public class NetworkUsageDaoIT extends AbstractHibernateDaoIT<NetworkUsage, Long>
{

   @Autowired
   private NetworkUsageDao dao;

   @Autowired
   private UserDao udao;

   private Date period;

   @BeforeClass
   public void setUp ()
   {
      Calendar calendar = Calendar.getInstance ();
      calendar.set (2014, 01, 01);
      this.period = calendar.getTime ();
   }

   @Override
   protected HibernateDao<NetworkUsage, Long> getHibernateDao ()
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
      User user = new User ();
      user.setUUID ("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3");
      user.setUsername ("babar");

      NetworkUsage nu = new NetworkUsage ();
      nu.setDate (new Date ());
      nu.setIsDownload (false);
      nu.setSize (42L);
      nu.setUser (user);

      nu = dao.create (nu);
      Assert.assertNotNull (nu);
      Assert.assertNotNull (nu.getId ());
      Assert.assertEquals (nu.getSize ().intValue (), 42);
      Assert.assertEquals (nu.getUser (), user);
   }

   @Override
   public void read ()
   {
      User u = new User ();
      u.setUUID ("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2");
      NetworkUsage nu = dao.read (0L);
      Assert.assertNotNull (nu);
      Assert.assertEquals (nu.getSize ().intValue (), 2);
      Assert.assertEquals (nu.getUser ().getUsername (), "toto");
   }

   @Override
   public void update ()
   {
      Long id = 4L;
      NetworkUsage nu = dao.read (id);
      boolean bool = true;

      Assert.assertNotNull (nu);
      Assert.assertNotEquals (nu.getIsDownload (), bool);
      nu.setIsDownload (bool);
      dao.update (nu);

      nu = dao.read (id);
      Assert.assertTrue (nu.getIsDownload ());
   }

   @Override
   public void delete ()
   {
      long id = 5;
      dao.delete (dao.read (id));
      Assert.assertEquals (dao.count (), (howMany () - 1));
      Assert.assertNull (dao.read (id));
      Assert.assertNotNull (udao.read ("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3"));
   }

   @Override
   public void first ()
   {
      String hql = "FROM NetworkUsage WHERE isDownload = TRUE ORDER BY id DESC";
      NetworkUsage nu = dao.first (hql);
      Assert.assertNotNull (nu);
      Assert.assertEquals (nu.getId ().intValue (), 7);
   }

   @Test
   public void testGetDownloadedCountPerUser ()
   {
      User u = udao.getByName ("babar");

      int n = dao.countDownloadByUserSince (u, period);
      Assert.assertEquals (n, 2);
   }

   @Test
   public void testGetDownloadSizePerUser ()
   {
      User u = udao.getByName ("babar");

      long expected = 68;
      long size = dao.getDownloadedSizeByUserSince (u, period);
      Assert.assertEquals (size, expected);
   }
}
