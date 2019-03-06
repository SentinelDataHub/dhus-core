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
package fr.gael.dhus.database.object;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "METADATA_DEFINITION")
public class MetadataDefinition implements Serializable
{
   private static final long serialVersionUID = 1L;

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   @Column(name = "ID")
   private Integer id;

   @Column(name = "NAME")
   private String name;

   @Column(name = "TYPE")
   private String type;

   @Column(name = "CATEGORY")
   private String category;

   @Column(name = "QUERYABLE")
   private String queryable;

   public MetadataDefinition() {}

   public MetadataDefinition(String name, String type, String category, String queryable)
   {
      this.name = name;
      this.type = type;
      this.category = category;
      this.queryable = queryable;
   }

   public Integer getId()
   {
      return id;
   }

   public void setId(Integer id)
   {
      this.id = id;
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public String getType()
   {
      return type;
   }

   public void setType(String type)
   {
      this.type = type;
   }

   public String getCategory()
   {
      return category;
   }

   public void setCategory(String category)
   {
      this.category = category;
   }

   public String getQueryable()
   {
      return queryable;
   }

   public void setQueryable(String queryable)
   {
      this.queryable = queryable;
   }

   @Override
   public int hashCode()
   {
      return ((id == null) ? 0 : id.hashCode());
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }
      if (obj == null)
      {
         return false;
      }
      if (getClass() != obj.getClass())
      {
         return false;
      }
      MetadataDefinition other = (MetadataDefinition) obj;
      if (id == null)
      {
         if (other.id != null)
         {
            return false;

         }
      }
      else if (!id.equals(other.id))
      {
         return false;

      }
      return true;
   }

   @Override
   public String toString()
   {
      return "name:[" + name + "] type:[" + type + "] queryable:[" + queryable + "] category:[" + category + "]";
   }
}
