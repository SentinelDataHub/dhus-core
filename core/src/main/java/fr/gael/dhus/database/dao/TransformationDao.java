/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018,2019 GAEL Systems
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
import fr.gael.dhus.database.object.Transformation;

import java.util.List;

import org.hibernate.query.Query;

import org.springframework.stereotype.Repository;

@Repository
public class TransformationDao extends HibernateDao<Transformation, String>
{
   @SuppressWarnings("unchecked")
   public List<Transformation> findTransformationsOf(final String transformerName)
   {
      return getHibernateTemplate().execute(session ->
      {
         String hql = "FROM Transformation trf WHERE trf.transformer = ?1";
         Query query = session.createQuery(hql);
         query.setParameter(1, transformerName);
         return (List<Transformation>) query.list();
      });
   }

   public Transformation findTransformationOf(final String transformerName, final String id)
   {
      return getHibernateTemplate().execute(session ->
      {
         String hql = "FROM Transformation trf WHERE trf.uuid = ?1 AND trf.transformer = ?2";
         Query query = session.createQuery(hql);
         query.setParameter(1, id);
         query.setParameter(2, transformerName);
         return (Transformation) query.uniqueResult();
      });
   }

   public Transformation findTransformation(final String transformerName, final String inputProduct, final int hash)
   {
      return getHibernateTemplate().execute(session ->
      {
         String hql = "FROM Transformation trf WHERE trf.transformer = ?1 AND trf.productIn = ?2 AND trf.hash = ?3";
         Query query = session.createQuery(hql);
         query.setParameter(1, transformerName);
         query.setParameter(2, inputProduct);
         query.setParameter(3, hash);
         return (Transformation) query.uniqueResult();
      });
   }

   public Transformation findTransformationByOutput(final String outputProductUuid)
   {
      return getHibernateTemplate().execute(session ->
      {
         String hql = "FROM Transformation trf WHERE trf.productOut = ?1";
         Query query = session.createQuery(hql);
         query.setParameter(1, outputProductUuid);
         return (Transformation) query.uniqueResult();
      });
   }

   public Transformation findTransformationByUuid(final String uuid)
   {
      return getHibernateTemplate().execute(session ->
      {
         String hql = "FROM Transformation trf WHERE trf.uuid = ?1";
         Query query = session.createQuery(hql);
         query.setParameter(1, uuid);
         return (Transformation) query.uniqueResult();
      });
   }

   @SuppressWarnings("unchecked")
   public List<Transformation> getTransformationsByStatus(final String status)
   {
      return getHibernateTemplate().execute(session ->
      {
         String hql = "FROM Transformation trf WHERE trf.status = ?1";
         Query query = session.createQuery(hql);
         query.setParameter(1, status);
         return (List<Transformation>) query.list();
      });
   }

   public int countTransformationsByStatus(final String status)
   {
      return ((Long) getHibernateTemplate().execute(session ->
      {
         String hql = "SELECT COUNT(*) FROM Transformation trf WHERE trf.status = ?1";
         Query query = session.createQuery(hql);
         query.setParameter(1, status);
         return query.uniqueResult();
      })).intValue();
   }

   public void deleteTransformationByUuid(final String uuid)
   {
      getHibernateTemplate().execute((session) ->
      {
         Query query = session.createQuery("DELETE FROM Transformation where uuid=?1");
         query.setParameter(1, uuid);
         query.executeUpdate();
         return null;
      });
   }

   @SuppressWarnings("unchecked")
   public List<Transformation> getTransformationsOfUser(String userUUID)
   {
      return getHibernateTemplate().execute(session ->
      {
         String hql = "select trf FROM Transformation trf "
                    + "inner join trf.userTransformations ut with ut.userKey.user.uuid=?1";
         Query query = session.createQuery(hql);
         query.setParameter(1, userUUID);
         return (List<Transformation>) query.list();
      });
   }
}
