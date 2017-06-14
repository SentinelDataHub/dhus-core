/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016 GAEL Systems
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

   public KeyStoreEntry()
   {
   }

   public KeyStoreEntry(String keyStore, String entryKey, String value)
   {
      this(new Key(keyStore, entryKey), value);
   }

   public KeyStoreEntry(Key key, String value)
   {
      this.key = key;
      this.value = value;
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

   public Key getKey()
   {
      return key;
   }

   @Embeddable
   public static class Key implements Serializable
   {
      private static final long serialVersionUID = 6938215865159432909L;

      @Column(name = "KEYSTORE", nullable = false)
      private String keyStore;
      @Column(name = "ENTRYKEY", nullable = false)
      private String entryKey;

      public Key()
      {
      }

      public Key(String keyStore, String entryKey)
      {
         this.keyStore = keyStore;
         this.entryKey = entryKey;
      }

      public String getKeyStore()
      {
         return keyStore;
      }

      public String getEntryKey()
      {
         return entryKey;
      }
   }
}
