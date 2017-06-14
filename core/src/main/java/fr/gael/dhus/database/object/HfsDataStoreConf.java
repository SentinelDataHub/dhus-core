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
package fr.gael.dhus.database.object;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("hfs")
public class HfsDataStoreConf extends DataStoreConfiguration
{
   @Column(name = "PATH", length = 512)
   private String path;

   @Column(name = "MAX_FILE_DEPTH")
   private int maxFileDepth;

   public String getPath()
   {
      return path;
   }

   public void setPath(String path)
   {
      this.path = path;
   }

   public int getMaxFileDepth()
   {
      return maxFileDepth;
   }

   public void setMaxFileDepth(int maxFileDepth)
   {
      this.maxFileDepth = maxFileDepth;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null)
      {
         return false;
      }
      if (this == obj)
      {
         return true;
      }
      if (obj instanceof HfsDataStoreConf)
      {
         HfsDataStoreConf other = (HfsDataStoreConf) obj;
         return getName().equals(other.getName()) || path.equals(other.path);
      }
      return false;
   }

   @Override
   public int hashCode()
   {
      return getName().hashCode() + path.hashCode();
   }
}
