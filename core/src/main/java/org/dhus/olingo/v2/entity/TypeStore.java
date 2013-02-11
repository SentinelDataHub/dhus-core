/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018,2019 GAEL Systems
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
package org.dhus.olingo.v2.entity;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.dhus.api.olingo.v2.EntityProducer;
import org.dhus.api.olingo.v2.TypeInfo;

/**
 * A data structure to store {@link EntityProducer} in a tree reproducing
 * the genealogy (OO inheritance) of their respective EntityTypes.
 * <p>
 * Entity producers must be annotated with the {@link TypeInfo} annotation.
 *
 * @see EntityProducer
 * @see TypeInfo
 */
public final class TypeStore
{
   /** To store types having no baseType. */
   private final Node root = new Node(null, void.class, null);

   private final List<Node> headlessNodes = new LinkedList<>();

   /**
    * Gets the EntityProducer accepting instances of the given class as input.
    *
    * @param type Input type of {@link EntityProducer#transform(Object)}
    * @return an instance or {@code null}
    * @throws NullPointerException if {@code type} parameter is null
    */
   public Node get(Class<?> type)
   {
      Objects.requireNonNull(type, "parameter `type` must not be null");
      return root.get(type);
   }

   /**
    * Inserts given {@link EntityProducer} in the type tree.
    *
    * @param entityProducer to insert
    * @throws NullPointerException if {@code entityProducer} parameter is null or is not annotated
    */
   public void insert(EntityProducer<?> entityProducer)
   {
      Objects.requireNonNull(entityProducer, "parameter `entityProducer` must not be null");

      Class<?> clazz = entityProducer.getClass();
      TypeInfo typeInfo = clazz.getAnnotation(TypeInfo.class);
      Objects.requireNonNull(typeInfo, "parameter `typeInfo` must be annotated with @TypeInfo");

      Node toAdd = new Node(typeInfo.baseType(), typeInfo.type(), entityProducer);

      Iterator<Node> it = headlessNodes.iterator();
      while (it.hasNext())
      {
         Node n = it.next();
         if (typeInfo.type().equals(n.getBaseType()))
         {
            toAdd.add(n);
            it.remove();
         }
         else if (n.getType().equals(typeInfo.baseType()))
         {
            n.add(toAdd);
            return;
         }
      }

      Node base = get(typeInfo.baseType());
      if (base == null)
      {
         headlessNodes.add(toAdd);
      }
      else
      {
         base.add(toAdd);
      }
   }

   /** Holds an {@link EntityProducer}. */
   public final static class Node
   {
      private Node parent = null;
      private final List<Node> subTypes = new LinkedList<>();

      private final Class<?> baseType;
      private final Class<?> type;
      private final EntityProducer<?> entityProducer;

      public Node(Class<?> baseType, Class<?> type, EntityProducer<?> entityProducer)
      {
         this.baseType = baseType;
         this.type = type;
         this.entityProducer = entityProducer;
      }

      public void add(Node n)
      {
         n.parent = this;
         this.subTypes.add(n);
      }

      public Node getParent()
      {
         return parent;
      }

      public Class<?> getBaseType()
      {
         return baseType;
      }

      public Class<?> getType()
      {
         return type;
      }

      @SuppressWarnings("unchecked")
      public <T> EntityProducer<T> getEntityProducer()
      {
         return (EntityProducer<T>)entityProducer;
      }

      public List<Node> getSubTypes()
      {
         return Collections.<Node>unmodifiableList(subTypes);
      }

      public Node get(Class<?> type)
      {
         if (this.type.equals(type))
         {
            return this;
         }
         for (Node n: subTypes)
         {
            Node res = n.get(type);
            if (res != null)
            {
               return res;
            }
         }
         return null;
      }
   }
}
