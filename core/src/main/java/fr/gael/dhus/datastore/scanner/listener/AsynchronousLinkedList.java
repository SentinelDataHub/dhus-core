/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013-2015,2018 GAEL Systems
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
package fr.gael.dhus.datastore.scanner.listener;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This class implement a {@link java.util.LinkedList} with additional
 * {@link Listener#addedElement(Event)}/{@link Listener#removedElement(Event)} listeners.
 * This List implementation can be use in "simulation" mode according to call to {@link #simulate(boolean)} method.
 * Simulation runs {@link #add(Object)}/{@link #remove(Object)} methods without inserting/removing element from the list,
 * but the listeners are still executed.
 *
 * @param <E> type of elements held in this list
 */
public class AsynchronousLinkedList<E> extends AbstractList<E>
{
   /**
    * the delegate list used to store elements.
    */
   private LinkedList<E> delegate = new LinkedList<>();

   private final List<Listener<E>> listeners = new ArrayList<>();

   /**
    * Switch the component into a simulation mode.
    * If simulation mode is active, elements passed to this class are never stored into the delegated list.
    * To ensure no data is stored into the delegated list class, the internal instance is set to null.
    * In any case, changing this mode resets the list.
    *
    * @param simulation activates the simulation mode if {@code true}, otherwise, reset the list
    */
   public void simulate(boolean simulation)
   {
      if (simulation)
      {
         delegate = null;
      }
      else
      {
         delegate = new LinkedList<>();
      }
   }

   // List implementation ...
   @Override
   public E get(int index)
   {
      if (delegate == null)
      {
         return null;
      }
      return delegate.get(index);
   }

   @Override
   public int size()
   {
      if (delegate == null)
      {
         return 0;
      }
      return delegate.size();
   }

   @Override
   public E set(int index, E element)
   {
      E ret;
      if (delegate != null)
      {
         ret = delegate.set(index, element);
      }
      else
      {
         ret = null;
      }

      fireListChanged(new Event<>(EventType.ADD, element, index));
      return ret;
   }

   @Override
   public void add(int index, E element)
   {
      if (delegate != null)
      {
         delegate.add(index, element);
      }
      fireListChanged(new Event<>(EventType.ADD, element, index));
   }

   @SuppressWarnings("unchecked")
   @Override
   public boolean remove(Object o)
   {
      if (delegate == null)
      {
         return false;
      }

      int index = delegate.indexOf(o);
      boolean ret = delegate.remove(o);
      if (ret)
      {
         fireListChanged(new Event<>(EventType.REMOVE, (E) o, index));
      }
      return ret;
   }

   private void fireListChanged(Event<E> e)
   {
      if (e.getType() == EventType.ADD)
      {
         for (Listener<E> listener: getListeners())
         {
            listener.addedElement(e);
         }
      }
      else if (e.getType() == EventType.REMOVE)
      {
         for (Listener<E> listener: getListeners())
         {
            listener.removedElement(e);
         }
      }
   }

   public void addListener(Listener<E> listener)
   {
      listeners.add(listener);
   }

   public void removeListener(Listener<E> listener)
   {
      listeners.remove(listener);
   }

   public List<Listener<E>> getListeners()
   {
      return listeners;
   }
}
