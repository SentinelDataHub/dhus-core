/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019 GAEL Systems
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
import fr.gael.dhus.database.object.TransfoParameter;

import java.util.List;

import org.hibernate.query.Query;

import org.springframework.stereotype.Repository;

@Repository
public class TransfoParameterDao extends HibernateDao<TransfoParameter, String>
{
   @SuppressWarnings("unchecked")
   public List<TransfoParameter> getParametersFromTransformation(String transformationUuid)
   {
      return getHibernateTemplate().execute(session ->
      {
         String hql = "FROM TransfoParameter WHERE key.transfoId = ?1";
         Query<TransfoParameter> query = session.createQuery(hql);
         query.setParameter(1, transformationUuid);
         return query.list();
      });
   }
}
