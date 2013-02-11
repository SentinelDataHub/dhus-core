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
package org.dhus.metrics.embed;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;

import org.dhus.olingo.v2.datamodel.MetricModel;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Map metrics to Olingo4 entities without overhead.
 */
public abstract class MetricCounterEntityMapper implements RowMapper<Entity>
   {
      @Override
      public Entity map(ResultSet rs, StatementContext ctx) throws SQLException
      {
         Entity entity = new Entity();
         entity.addProperty(new Property(null, MetricModel.NAME, ValueType.PRIMITIVE, processName(rs.getString("name"))));
         entity.addProperty(new Property(null, MetricModel.TIMESTAMP, ValueType.PRIMITIVE, Timestamp.from(Instant.now())));
         entity.addProperty(new Property(null, MetricModel.TYPE, ValueType.ENUM, rs.getInt("type")));
         entity.addProperty(new Property(null, MetricModel.COUNT, ValueType.PRIMITIVE, rs.getLong("count")));

         return entity;
      }
      
      public abstract String processName(String name);

   }
