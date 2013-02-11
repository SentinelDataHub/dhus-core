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
package fr.gael.dhus.datastore.scanner.listener;

/**
 * The event reported to a {@link Listener} class.
 *
 * @param <E> element type passed into the list
 */
public class Event<E>
{
   private final EventType type;
   private final E element;
   private final int index;

   /**
    * Build the event.
    *
    * @param type    the type of event passed according to {@link EventType} types.
    * @param element the changed element in the list.
    * @param index   the index of the element in the list.
    */
   public Event(EventType type, E element, int index)
   {
      this.type = type;
      this.element = element;
      this.index = index;
   }

   public EventType getType()
   {
      return this.type;
   }

   public E getElement()
   {
      return this.element;
   }

   public int getIndex()
   {
      return this.index;
   }
}
