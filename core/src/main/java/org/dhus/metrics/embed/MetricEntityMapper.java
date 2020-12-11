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

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;

import org.dhus.olingo.v2.datamodel.MetricModel;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Map metrics to Olingo4 entities without overhead.
 */
public class MetricEntityMapper implements RowMapper<Entity>
   {
      @Override
      public Entity map(ResultSet rs, StatementContext ctx) throws SQLException
      {
         Entity entity = new Entity();
         entity.addProperty(new Property(null, MetricModel.NAME, ValueType.PRIMITIVE, rs.getString("name")));
         entity.addProperty(new Property(null, MetricModel.TIMESTAMP, ValueType.PRIMITIVE, rs.getTimestamp("date")));
         entity.addProperty(new Property(null, MetricModel.TYPE, ValueType.ENUM, rs.getInt("type")));
         entity.addProperty(new Property(null, MetricModel.COUNT, ValueType.PRIMITIVE, rs.getLong("count")));
         entity.addProperty(new Property(null, MetricModel.GAUGE, ValueType.PRIMITIVE, rs.getString("value")));
         entity.addProperty(new Property(null, MetricModel.MINIMUM, ValueType.PRIMITIVE, rs.getLong("min")));
         entity.addProperty(new Property(null, MetricModel.MAXIMUM, ValueType.PRIMITIVE, rs.getLong("max")));
         entity.addProperty(new Property(null, MetricModel.MEAN, ValueType.PRIMITIVE, rs.getDouble("mean")));
         entity.addProperty(new Property(null, MetricModel.MEDIAN, ValueType.PRIMITIVE, rs.getDouble("median")));
         entity.addProperty(new Property(null, MetricModel.STANDARDDEVIATION, ValueType.PRIMITIVE, rs.getDouble("std_dev")));
         entity.addProperty(new Property(null, MetricModel.SEVENTYFIFTHPERCENTILE, ValueType.PRIMITIVE, rs.getDouble("h_75thpercentile")));
         entity.addProperty(new Property(null, MetricModel.NINETYFIFTHPERCENTILE, ValueType.PRIMITIVE, rs.getDouble("h_95thpercentile")));
         entity.addProperty(new Property(null, MetricModel.NINETYEIGHTHPERCENTILE, ValueType.PRIMITIVE, rs.getDouble("h_98thpercentile")));
         entity.addProperty(new Property(null, MetricModel.NINETYNINTHPERCENTILE, ValueType.PRIMITIVE, rs.getDouble("h_99thpercentile")));
         entity.addProperty(new Property(null, MetricModel.NINETYNINTHNINEPERCENTILE, ValueType.PRIMITIVE, rs.getDouble("h_999thpercentile")));
         entity.addProperty(new Property(null, MetricModel.MEANRATE, ValueType.PRIMITIVE, rs.getDouble("mean_rate")));
         entity.addProperty(new Property(null, MetricModel.ONEMINUTERATE, ValueType.PRIMITIVE, rs.getDouble("m_1m_rate")));
         entity.addProperty(new Property(null, MetricModel.FIVEMINUTESRATE, ValueType.PRIMITIVE, rs.getDouble("m_5m_rate")));
         entity.addProperty(new Property(null, MetricModel.FIFTEENMINUTESRATE, ValueType.PRIMITIVE, rs.getDouble("m_15m_rate")));

         return entity;
      }

   }
