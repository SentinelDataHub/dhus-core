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
package fr.gael.dhus.olingo.v1.map.impl;

import fr.gael.dhus.olingo.v1.entity.AbstractEntity;

import java.util.Iterator;

/**
 * A iterator that adapts results from type {@code <E>} to type {@code <T>}.
 * Implies that T has a constructor(E instance).
 *
 * @param <E> from type
 * @param <T> to type
 */
final class ODataEntityIterator<E, T extends AbstractEntity>
      implements Iterator<T>
{
   private final Iterator<E> it;
   private final Class<E> fromClass;
   private final Class<T> toClass;

   public ODataEntityIterator (Iterator<E> iterator, Class<E> from, Class<T> to)
   {
      this.fromClass = from;
      this.toClass = to;
      this.it = iterator;
   }

   @Override
   public boolean hasNext()
   {
      return it.hasNext ();
   }

   @Override
   @SuppressWarnings("unchecked")
   public T next()
   {
      E element = it.next ();
      try
      {
         return toClass.getConstructor(fromClass).newInstance(element);
      }
      catch (ReflectiveOperationException e)
      {
         throw new UnsupportedOperationException(e);
      }
   }

   @Override
   public void remove()
   {
      throw new UnsupportedOperationException();
   }
}
