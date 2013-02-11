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
package fr.gael.dhus.util.functional;

import java.util.Iterator;
import java.util.function.Function;

/**
 * Iterator adapter that transform element using the given transformer.
 *
 * @param <E> from type
 * @param <T> to type
 */
public class IteratorAdapter<E, T> implements Iterator<T>
{
   private final Iterator<E> iterator;
   private final Function<E, T> transformer;

   /**
    * Create adapter instance.
    *
    * @param iterator non null instance
    * @param transformer non null function instance
    */
   public IteratorAdapter(Iterator<E> iterator, Function<E, T> transformer)
   {
      this.iterator = iterator;
      this.transformer = transformer;
   }

   @Override
   public boolean hasNext()
   {
      return iterator.hasNext();
   }

   @Override
   public T next()
   {
      return transformer.apply(iterator.next());
   }
}