/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2019 GAEL Systems
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
import java.sql.Timestamp;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Store quota information.
 */
@Entity
@Table(name = "STORE_QUOTAS")
public class StoreQuota implements Serializable
{
   private static final long serialVersionUID = 1L;

   @EmbeddedId
   private Key key = new Key();

   @Column(name = "DATETIME", nullable = false)
   private Timestamp datetime = new Timestamp(0L);

   /**
    * @return the datetime
    */
   public long getDatetime()
   {
      return datetime.getTime();
   }

   /**
    * @param datetime the datetime to set
    */
   public void setDatetime(long datetime)
   {
      this.datetime = new Timestamp(datetime);
   }

   /**
    * @return the storeName
    */
   public String getStoreName()
   {
      return this.key.storeName;
   }

   /**
    * @param storeName the storeName to set
    */
   public void setStoreName(String storeName)
   {
      this.key.storeName = storeName;
   }

   /**
    * @return the quotaName
    */
   public String getQuotaName()
   {
      return this.key.quotaName;
   }

   /**
    * @param quotaName the quotaName to set
    */
   public void setQuotaName(String quotaName)
   {
      this.key.quotaName = quotaName;
   }

   /**
    * @return the userUUID
    */
   public String getUserUUID()
   {
      return this.key.userUUID;
   }

   /**
    * @param userUUID the userUUID to set
    */
   public void setUserUUID(String userUUID)
   {
      this.key.userUUID = userUUID;
   }

   /**
    * @return the identifier
    */
   public String getIdentifier()
   {
      return this.key.identifier;
   }

   /**
    * @param identifier the identifier to set
    */
   public void setIdentifier(String identifier)
   {
      this.key.identifier = identifier;
   }

   @Override
   public String toString()
   {
      return "Quota: ("+ key +")";
   }

   /** Multivalued Primary Key. */
   @Embeddable
   public static class Key implements Serializable
   {
      private static final long serialVersionUID = 1L;

      @Column(name = "STORE_NAME")
      private String storeName;

      @Column(name = "QUOTA_NAME")
      private String quotaName;

      @Column(name = "USER_UUID")
      private String userUUID;

      @Column(name = "IDENTIFIER")
      private String identifier;

      @Override
      public int hashCode()
      {
         return quotaName.hashCode();
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
         final Key other = (Key) obj;
         if (!Objects.equals(this.storeName, other.storeName))
         {
            return false;
         }
         if (!Objects.equals(this.quotaName, other.quotaName))
         {
            return false;
         }
         if (!Objects.equals(this.userUUID, other.userUUID))
         {
            return false;
         }
         return Objects.equals(this.identifier, other.identifier);
      }

      @Override
      public String toString()
      {
         return "store: " + storeName + " quota: " + quotaName + " user: " + userUUID;
      }
   }
}
