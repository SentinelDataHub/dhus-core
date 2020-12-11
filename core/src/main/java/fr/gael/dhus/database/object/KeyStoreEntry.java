/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016-2019 GAEL Systems
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
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "KEYSTOREENTRIES")
public class KeyStoreEntry implements Serializable
{
   private static final long serialVersionUID = 4056390117742096488L;

   @EmbeddedId
   private Key key;

   @Column(name = "VALUE")
   private String value;

   @Column(name = "INSERTIONDATE")
   private Long insertionDate;

   public KeyStoreEntry()
   {
   }

   public KeyStoreEntry(String keyStore, String entryKey, String tag, String value, Long insertionDate)
   {
      this(new Key(keyStore, entryKey, tag), value, insertionDate);
   }

   public KeyStoreEntry(Key key, String value, Long insertionDate)
   {
      this.key = key;
      this.value = value;
      this.insertionDate = insertionDate;
   }

   public String getValue()
   {
      return value;
   }

   public void setValue(String value)
   {
      this.value = value;
   }

   public String getKeyStore()
   {
      return key != null ? key.getKeyStore() : null;
   }

   public String getEntryKey()
   {
      return key != null ? key.getEntryKey() : null;
   }

   public String getTag()
   {
      return key != null ? key.getTag() : null;
   }

   public Key getKey()
   {
      return key;
   }

   public Long getInsertionDate()
   {
      return insertionDate;
   }

   public void setInsertionDate(Long insertionDate)
   {
      this.insertionDate = insertionDate;
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

      final KeyStoreEntry other = (KeyStoreEntry) obj;
      if (!this.key.equals(other.key))
      {
         return false;
      }
      if (!Objects.equals(this.value, other.value))
      {
         return false;
      }
      if (!Objects.equals(this.insertionDate, other.insertionDate))
      {
         return false;
      }

      return true;
   }

   @Override
   public int hashCode()
   {
      return Objects.hashCode(this.key);
   }

   @Override
   public String toString()
   {
      return key.toString() + " (" + value + ")";
   }

   @Embeddable
   public static class Key implements Serializable
   {
      private static final long serialVersionUID = 6938215865159432909L;

      @Column(name = "KEYSTORE", nullable = false)
      private String keyStore;
      @Column(name = "ENTRYKEY", nullable = false)
      private String entryKey;
      @Column(name = "TAG", nullable = false)
      private String tag;

      public Key() {}

      public Key(String keyStore, String entryKey, String tag)
      {
         this.keyStore = keyStore;
         this.entryKey = entryKey;
         this.tag = tag;
      }

      public String getKeyStore()
      {
         return keyStore;
      }

      public String getEntryKey()
      {
         return entryKey;
      }

      public String getTag()
      {
         return tag;
      }

      @Override
      public String toString()
      {
         return "Keystore: " + keyStore + ", Entrykey: " + entryKey + ", Tag: " + tag;
      }

      @Override
      public int hashCode()
      {
         int hash = 3;
         hash = 17 * hash + Objects.hashCode(this.keyStore);
         hash = 17 * hash + Objects.hashCode(this.entryKey);
         hash = 17 * hash + Objects.hashCode(this.tag);
         return hash;
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
         if (!Objects.equals(this.keyStore, other.keyStore))
         {
            return false;
         }
         if (!Objects.equals(this.entryKey, other.entryKey))
         {
            return false;
         }
         if (!Objects.equals(this.tag, other.tag))
         {
            return false;
         }
         return true;
      }

   }
}
