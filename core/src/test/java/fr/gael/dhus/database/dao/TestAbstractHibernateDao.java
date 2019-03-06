/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2014,2015,2017 GAEL Systems
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

import java.io.Serializable;

import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.annotations.Test;

import fr.gael.dhus.database.dao.interfaces.DaoListener;
import fr.gael.dhus.database.dao.interfaces.HibernateDao;

public abstract class TestAbstractHibernateDao<T, PK extends Serializable>
      extends AbstractTransactionalTestNGSpringContextTests
{

   protected abstract HibernateDao<T, PK> getHibernateDao();

   protected abstract int howMany();

   @Test
   public abstract void create();

   @Test
   public abstract void read();

   @Test
   public abstract void update();

   @Test
   public abstract void delete();

   @Test
   public abstract void first();

   @Test
   public void count()
   {
      assertEquals(getHibernateDao().count(), howMany());
   }

   @Test
   public void readAll()
   {
      assertEquals(getHibernateDao().readAll().size(), howMany());
   }

   @Test
   public void deleteAll()
   {
      cancelListeners(getHibernateDao());

      getHibernateDao().deleteAll();
      assertEquals(getHibernateDao().count(), 0);
   }

   protected void cancelListeners(HibernateDao<T, PK> dao)
   {
      for (DaoListener<T> listener: getHibernateDao().getListeners())
      {
         getHibernateDao().removeListener((DaoListener<T>) listener);
      }
   }
}
