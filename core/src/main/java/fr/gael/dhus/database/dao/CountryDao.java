/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2015,2016,2018 GAEL Systems
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

import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Repository;

import fr.gael.dhus.database.dao.interfaces.HibernateDao;
import fr.gael.dhus.database.object.Country;

@Repository
public class CountryDao extends HibernateDao<Country, Long>
{
   @Override
   public void deleteAll ()
   {
      throw new UnsupportedOperationException ();
   }

   @Override
   public void delete (Country c)
   {
      throw new UnsupportedOperationException ();
   }

   @Override
   public void update (Country country)
   {
      throw new UnsupportedOperationException ();
   }

   @Override
   public Country create (Country country)
   {
      throw new UnsupportedOperationException ();
   }

   public List<String> readAllNames ()
   {
      return getHibernateTemplate ().execute (
         new HibernateCallback<List<String>> ()
         {
            @SuppressWarnings ("unchecked")
            @Override
            public List<String> doInHibernate (Session session)
               throws HibernateException, SQLException
            {
               String hql =
                  "SELECT name FROM Country "+
                        " ORDER BY name";
               Query query = session.createQuery (hql).setReadOnly (true);
               return (List<String>) query.list ();
            }
         });
   }

   public Country getCountryByName (final String name)
   {
      return getHibernateTemplate ().execute(session -> {
         Query query = session.createQuery("FROM Country WHERE name=?");
         query.setParameter(0, name, StandardBasicTypes.STRING);
         List list = query.list();
         return (Country) (list.isEmpty() ? null : list.get(0));
      });
   }

   public Country getCountryByAlpha2 (final String alpha2)
   {
      return getHibernateTemplate ().execute(session -> {
         Query query = session.createQuery("FROM Country WHERE alpha2=?");
         query.setParameter(0, alpha2, StandardBasicTypes.STRING);
         List list = query.list();
         return (Country) (list.isEmpty() ? null : list.get(0));
      });
   }

   public Country getCountryByAlpha3 (final String alpha3)
   {
      return getHibernateTemplate ().execute(session -> {
         Query query = session.createQuery("FROM Country WHERE alpha3=?");
         query.setParameter(0, alpha3, StandardBasicTypes.STRING);
         List list = query.list();
         return (Country) (list.isEmpty() ? null : list.get(0));
      });
   }

   public Country getCountryByNumeric (final Integer numeric)
   {
      return getHibernateTemplate ().execute(session -> {
         Query query = session.createQuery("FROM Country WHERE numeric=?");
         query.setParameter(0, numeric, StandardBasicTypes.INTEGER);
         List list = query.list();
         return (Country) (list.isEmpty() ? null : list.get(0));
      });
   }
}
