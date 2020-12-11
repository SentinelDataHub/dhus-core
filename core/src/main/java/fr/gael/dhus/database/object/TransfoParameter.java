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
package fr.gael.dhus.database.object;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "TRANSFO_PARAMETERS")
public class TransfoParameter implements Serializable
{
   private static final long serialVersionUID = 1L;

   @EmbeddedId
   private Key key;

   @Column(name = "VALUE", nullable = false)
   private String value;

   public Key getKey()
   {
      return key;
   }

   public void setKey(Key key)
   {
      this.key = key;
   }

   public String getValue()
   {
      return value;
   }

   public void setValue(String value)
   {
      this.value = value;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null)
      {
         return false;
      }
      if (!getClass().equals(obj.getClass()))
      {
         return false;
      }
      TransfoParameter other = (TransfoParameter) obj;
      return getKey().equals(other.getKey()) && value.equals(other.value);
   }

   @Override
   public int hashCode()
   {
      return getKey().hashCode() ^ value.hashCode();
   }

   @Override
   public String toString()
   {
      return "Transformation Parameter: (key: " + getKey() + ", value: " + value + ")";
   }

   public static class Key implements Serializable
   {
      private static final long serialVersionUID = 1L;

      @Column(name = "TRANSFO_UUID", nullable = false)
      private String transfoId;

      @Column(name = "NAME", nullable = false)
      private String name;

      public String getTransfoId()
      {
         return transfoId;
      }

      public void setTransfoId(String transfoId)
      {
         this.transfoId = transfoId;
      }

      public String getName()
      {
         return name;
      }

      public void setName(String name)
      {
         this.name = name;
      }

      @Override
      public boolean equals(Object obj)
      {
         if (obj == null)
         {
            return false;
         }
         if (!getClass().equals(obj.getClass()))
         {
            return false;
         }
         Key other = (Key) obj;
         return transfoId.equals(other.transfoId) && name.equals(other.name);
      }

      @Override
      public int hashCode()
      {
         return transfoId.hashCode() ^ name.hashCode();
      }

      @Override
      public String toString()
      {
         return "(uuid: " + transfoId + ", name: " + name + ")";
      }
   }
}
