/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018 GAEL Systems
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
import fr.gael.dhus.database.object.MetadataDefinition;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import org.springframework.stereotype.Repository;

@Repository
public class MetadataDefinitionDao extends HibernateDao<MetadataDefinition, Integer>
{
   private static final String CATEGORY = "category";
   private static final String NAME = "name";
   private static final String TYPE = "type";
   private static final String QUERYABLE = "queryable";

   private synchronized MetadataDefinition find(String name, String category, String type, String queryable)
   {
      MetadataDefinition result = getHibernateTemplate().execute((Session session) ->
      {
         Criteria criteria = session.createCriteria(MetadataDefinition.class);

         Criterion categoryRestriction = (category == null) ?
               Restrictions.isNull(CATEGORY) : Restrictions.eq(CATEGORY, category);
         criteria.add(categoryRestriction);

         Criterion nameRestriction = (name == null) ?
               Restrictions.isNull(NAME) : Restrictions.eq(NAME, name);
         criteria.add(nameRestriction);

         Criterion typeRestriction = (type == null) ?
               Restrictions.isNull(TYPE) : Restrictions.eq(TYPE, type);
         criteria.add(typeRestriction);

         Criterion queryableRestriction = (queryable == null) ?
               Restrictions.isNull(QUERYABLE) : Restrictions.eq(QUERYABLE, queryable);
         criteria.add(queryableRestriction);

         return (MetadataDefinition) criteria.uniqueResult();
      });
      return result;
   }

   public synchronized MetadataDefinition findAndCreateIfAbsent(String name, String category, String type, String queryable)
   {
      MetadataDefinition metadataDefinition = find(name, category, type, queryable);
      return (metadataDefinition == null) ? create(new MetadataDefinition(name, type, category, queryable)) : metadataDefinition;
   }
}
