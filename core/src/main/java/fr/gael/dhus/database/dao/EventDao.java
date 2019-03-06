/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
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
import fr.gael.dhus.database.object.Event;
import fr.gael.dhus.database.object.Event.EventCategory;

import java.util.Date;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import org.springframework.stereotype.Repository;

@Repository
public class EventDao extends HibernateDao<Event, Long>
{

   public Event read(EventCategory category, String subcategory, String title,
         Date startDate, Date stopDate)
   {
      DetachedCriteria criteria = DetachedCriteria.forClass(Event.class);
      criteria.add(Restrictions.eq("category", category));
      criteria.add(Restrictions.eq("subcategory", subcategory));
      criteria.add(Restrictions.eq("title", title));
      criteria.add(Restrictions.eq("startDate", startDate));
      criteria.add(Restrictions.eq("stopDate", stopDate));
      return uniqueResult(criteria);
   }

   public Event read(String title, Date publicationDate)
   {
      DetachedCriteria criteria = DetachedCriteria.forClass(Event.class);
      criteria.add(Restrictions.ilike("title", title));
      criteria.add(Restrictions.eq("publicationDate", publicationDate));
      return uniqueResult(criteria);
   }

}
