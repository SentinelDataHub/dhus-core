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
import java.util.Objects;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "USER_TRANSFORMATIONS")
public class UserTransformation implements Serializable
{
   private static final long serialVersionUID = 1L;

   @EmbeddedId
   private UserKey userKey;

   public UserTransformation() {}

   public UserTransformation(Transformation transformation, User user)
   {
      userKey = new UserKey(user, transformation);
   }

   public UserKey getUserKey()
   {
      return userKey;
   }

   public void setUserKey(UserKey userKey)
   {
      this.userKey = userKey;
   }

   public UserTransformation(UserKey userKey)
   {
      this.userKey = userKey;
   }

   @Override
   public int hashCode()
   {
      return Objects.hashCode(userKey);
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
      UserTransformation other = (UserTransformation) obj;
      return Objects.equals(userKey, other.userKey);
   }

   @Override
   public String toString()
   {
      return "UserTransformation [userUuid=" + userKey + "]";
   }

   @Embeddable
   public static class UserKey implements Serializable
   {
      private static final long serialVersionUID = 1L;

      @ManyToOne
      @JoinColumn(name = "TRANSFORMATION_UUID", nullable = false)
      private Transformation transformation;

      @ManyToOne
      @JoinColumn(name = "USER_UUID", nullable = false)
      private User user;

      public UserKey(User user, Transformation transformation)
      {
         this.transformation = transformation;
         this.user = user;
      }

      public UserKey()
      {
      }

      public Transformation getTransformation()
      {
         return transformation;
      }

      public void setTransformation(Transformation transformation)
      {
         this.transformation = transformation;
      }

      public User getUser()
      {
         return user;
      }

      public void setUser(User user)
      {
         this.user = user;
      }

      @Override
      public int hashCode()
      {
         return Objects.hash(transformation, user);
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
         UserKey other = (UserKey) obj;
         return Objects.equals(transformation, other.transformation) && Objects.equals(user, other.user);
      }

      @Override
      public String toString()
      {
         return "UserUuid [transformation=" + transformation + ", user=" + user + "]";
      }
   }

}
