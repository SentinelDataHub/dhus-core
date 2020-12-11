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
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Join table for Orders and Users.
 *
 * Annotation @ManyToMany should not be used for this specific case:
 * its cascading mechanism deletes the other entity, not the rows in the join table.
 *
 * Instead two @ManyToOne relations and an explicit join table (this class) should be used.
 *
 * The @OnDelete(action=CASCADE) provided by hibernate is also needed (on the parent entity).
 */
@Entity
@Table(name = "ORDER_OWNERS")
public class OrderOwner implements Serializable
{
   private static final long serialVersionUID = 1L;

   @Id
   private ForeignKeys key;

   public OrderOwner() {}

   public OrderOwner(Order order, User owner)
   {
      key = new ForeignKeys(order, owner);
   }

   public ForeignKeys getKey()
   {
      return key;
   }

   public void setKey(ForeignKeys key)
   {
      this.key = key;
   }

   @Override
   public int hashCode()
   {
      return Objects.hashCode(this.key);
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
      final OrderOwner other = (OrderOwner) obj;
      return Objects.equals(this.key, other.key);
   }

   @Embeddable
   public static class ForeignKeys implements Serializable
   {
      private static final long serialVersionUID = 1L;

      @ManyToOne()
      @JoinColumns({@JoinColumn(name = "DATASTORE_NAME"), @JoinColumn(name = "PRODUCT_UUID")})
      private Order order;

      @ManyToOne()
      @JoinColumn(name="USER_UUID")
      private User owner;

      public ForeignKeys() {}

      public ForeignKeys(Order order, User owner)
      {
         this.order = order;
         this.owner = owner;
      }

      public Order getOrder()
      {
         return order;
      }

      public void setOrder(Order order)
      {
         this.order = order;
      }

      public User getOwner()
      {
         return owner;
      }

      public void setOwner(User owner)
      {
         this.owner = owner;
      }

      @Override
      public int hashCode()
      {
         int hash = 7;
         hash = 97 * hash + Objects.hashCode(this.order);
         hash = 97 * hash + Objects.hashCode(this.owner);
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
         final ForeignKeys other = (ForeignKeys) obj;
         return Objects.equals(this.order, other.order) && Objects.equals(this.owner, other.owner);
      }

   }
}
