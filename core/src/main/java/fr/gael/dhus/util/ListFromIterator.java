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
package fr.gael.dhus.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A list backed by an Iterator (typically a ScrollIterator).
 *
 * @param <E> the type of elements in this collection
 */
public class ListFromIterator<E> implements List<E>
{
   private final Iterator<E> it;

   public ListFromIterator(Iterator<E> iterator)
   {
      this.it = iterator;
   }

   @Override
   public int size()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isEmpty()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean contains(Object o)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Iterator<E> iterator()
   {
      return it;
   }

   @Override
   public Object[] toArray()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public <T> T[] toArray(T[] a)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean add(E e)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean remove(Object o)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean containsAll(Collection<?> c)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean addAll(Collection<? extends E> c)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean addAll(int index, Collection<? extends E> c)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean removeAll(Collection<?> c)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean retainAll(Collection<?> c)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public void clear()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public E get(int index)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public E set(int index, E element)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public void add(int index, E element)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public E remove(int index)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public int indexOf(Object o)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public int lastIndexOf(Object o)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public ListIterator<E> listIterator()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public ListIterator<E> listIterator(int index)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public List<E> subList(int fromIndex, int toIndex)
   {
      throw new UnsupportedOperationException();
   }
}
