/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2014,2015,2017-2019 GAEL Systems
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
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * This class implements persistent metadata element list attached to each
 * {@link Product}.
 */
@Embeddable
public class MetadataIndex implements Serializable
{
   private static final long serialVersionUID = 3436198155343857720L;

   @ManyToOne()
   @JoinColumn(name = "metadata_definition_id")
   private MetadataDefinition metadataDefinition;

   @Column(name = "VALUE", nullable = false, length = 8192)
   private String value;

   public MetadataIndex() {}

   public String getValue()
   {
      return value;
   }

   public void setValue(String value)
   {
      this.value = value;
   }

   public MetadataIndex(MetadataIndex index)
   {
      this.metadataDefinition = new MetadataDefinition(index.getName(), index.getType(), index.getCategory(), index.getQueryable());
      this.value = index.getValue();
   }

   public MetadataIndex(MetadataDefinition metadataDefinition, String value)
   {
      this.metadataDefinition = metadataDefinition;
      this.value = value;
   }

   public void setName(String name)
   {
      metadataDefinition.setName(name);
   }

   public String getName()
   {
      return metadataDefinition.getName();
   }

   public void setType(String type)
   {
      metadataDefinition.setType(type);
   }

   public String getType()
   {
      return metadataDefinition.getType();
   }

   public void setCategory(String category)
   {
      metadataDefinition.setCategory(category);
   }

   public String getCategory()
   {
      return metadataDefinition.getCategory();
   }

   public void setQueryable(String queryable)
   {
      metadataDefinition.setQueryable(queryable);
   }

   public String getQueryable()
   {
      return metadataDefinition.getQueryable();
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((metadataDefinition == null) ? 0 : metadataDefinition.hashCode());
      result = prime * result + ((value == null) ? 0 : value.hashCode());
      return result;
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
      MetadataIndex other = (MetadataIndex) obj;
      if (metadataDefinition == null)
      {
         if (other.metadataDefinition != null)
         {
            return false;
         }
      }
      else if (!metadataDefinition.equals(other.metadataDefinition))
      {
         return false;
      }
      if (value == null)
      {
         if (other.value != null)
         {
            return false;
         }
      }
      else if (!value.equals(other.value))
      {
         return false;
      }
      return true;
   }

   @Override
   public String toString()
   {
      return metadataDefinition.toString() + " value: [" + value + "]";
   }
}
