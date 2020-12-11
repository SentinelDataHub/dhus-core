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

import java.util.EventListener;

/**
 * This listener is called once the operation is performed.
 * When add event happens, the passed {@link AsynchronousLinkedList.Event} parameter contains the
 * element that has just been added, and its position in the list.
 * When remove event happens, the passed {@link AsynchronousLinkedList.Event} parameter contains
 * the removed element, and the index in the list where the element was.
 * If the element to remove is not present in the list and no remove action is performed,
 * the listener will not be called, even in simulation mode.
 *
 * @param <E> the type of element that can be reported to this listener
 * @see AsynchronousLinkedList#simulate(boolean)
 */
public interface Listener<E> extends EventListener
{
   void addedElement(Event<E> e);

   void removedElement(Event<E> e);

   void reset();
}
