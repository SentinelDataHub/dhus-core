/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018-2019 GAEL Systems
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
package org.dhus.olingo.v2.visitor;

import java.io.Serializable;

import org.hibernate.type.Type;

public final class SQLVisitorParameter implements Serializable
{
   private static final long serialVersionUID = 1L;
   private final int position;
   private final Type type;
   private final Object value;

   public SQLVisitorParameter(int position, Object value, Type type)
   {
      this.position = position;
      this.value = value;
      this.type = type;
   }

   public Type getType()
   {
      return type;
   }

   public Object getValue()
   {
      return value;
   }

   public int getPosition()
   {
      return position;
   }

   @Override
   public boolean equals(Object o)
   {
      if (this == o)
      {
         return true;
      }
      if (o == null || getClass() != o.getClass())
      {
         return false;
      }

      SQLVisitorParameter that = (SQLVisitorParameter) o;

      if (!type.equals(that.type))
      {
         return false;
      }
      return value.equals(that.value);
   }

   @Override
   public int hashCode()
   {
      int result = type.hashCode();
      result = 31 * result + value.hashCode();
      return result;
   }

   @Override
   public String toString()
   {
      return new StringBuilder("{").append(value).append(',').append(type).append("}").toString();
   }
}