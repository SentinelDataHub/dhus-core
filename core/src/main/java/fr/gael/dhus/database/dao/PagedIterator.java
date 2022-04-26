/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2016 GAEL Systems
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
package fr.gael.dhus.database.dao;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import fr.gael.dhus.database.dao.interfaces.HibernateDao;
import fr.gael.dhus.database.dao.interfaces.Pageable;

import org.dhus.olingo.v2.visitor.SQLVisitorParameter;
/**
 * This class implements an iterator over Pageable items. All the database dao
 * implements Pageable to retrieve the list of their entities. This class helps
 * to iterate other these list of element.<br/>
 * 
 * This class has been designed with decorator design pattern. A typical sample
 * usage to list all the products from database can be done as followed:
 * <pre>
 * <code>
 * ProductDao dao=getProductDao();
 * {@code PagedIterator<Product>} iterator=new {@code PagedIterator<Product>}(dao, query, 0, 3);
 * while (iterator.hasNext ())
 * {
 *    Product product = iterator.next ();
 *    if (shouldRemove(product))
 *       iterator.remove ();
 * }
 * </code>
 * </pre>
 * 
 * {@link PagedIterator} also implements Iterable interface to be used
 * as followed:
 * 
 * <pre>
 * <code>
 * ProductDao dao;
 * {@code PagedIterator<Product>} iterator=new {@code PagedIterator<Product>}(dao, query, 0, 3);
 * for (Product product:iterator)
 * {
 *    if (shouldRemove(product))
 *       iterator.remove ();
 * }
 * </code>
 * </pre>
 * 
 * This class is manly designed to be used at Database service level to take
 * advantage of common transactional blocks.
 * 
 * @see {@link java.lang.Iterable}, {@link java.lang.Iterator}, 
 *    {@link Pageable}
 */
final public class PagedIterator<E> implements Iterator<E>, Iterable<E>
{
   public static final int DEFAULT_PAGE_SIZE = 30;

   private final Pageable<E> pageable;
   private final String query;
   private final List<SQLVisitorParameter> parameters;
   private List<E> result;
   private E currentElement;
   private int index;
   private int globalIndex;
   private int skip;
   private final int pageSize;
   private final int count;

   PagedIterator (Pageable<E> pageable, String query)
   {
      this (pageable, query, 0, DEFAULT_PAGE_SIZE);
   }

   PagedIterator (Pageable<E> pageable, String query, int skip)
   {
      this (pageable, query, skip, DEFAULT_PAGE_SIZE);
   }

   PagedIterator (Pageable<E> pageable, String query, int skip, int page_size)
   {
      this (pageable, query, skip, page_size, -1);
   }

   PagedIterator (Pageable<E> pageable, String query, int skip, int page_size, int count)
   {
      this (pageable, query, null, skip, page_size, count);
   }

   PagedIterator (Pageable<E> pageable, String query, List<SQLVisitorParameter> parameters, int skip, int page_size, int count)
   {
      if (pageable == null || query == null || query.trim ().isEmpty ()
            || skip < 0 || page_size < 1 || count < -1)
      {
         throw new IllegalArgumentException ();
      }

      this.pageable = pageable;
      this.query = query;
      this.parameters = parameters;
      this.pageSize = page_size;
      this.index = 0;
      this.globalIndex = 0;
      this.skip = skip;
      this.count = count;
   }

   /**
    * @return total query count when possible, otherwise -1
    *         skip and top are not taken into account
    */
   public Integer getCount()
   {
      if(pageable instanceof HibernateDao<?, ?>)
      {
         return ((HibernateDao<?, ?>)pageable).count();
      }
      return -1;
   }

   /**
    * Retrieves results of next page of the query, and checks if a element is
    * present in this new page.
    *
    * @return true if next page is not empty.
    */
   private boolean loadNextPage ()
   {
      result = pageable.getPage (query, parameters, skip, pageSize);
      index = 0;

      if (result == null || result.isEmpty ())
      {
         return false;
      }
      return true;
   }

   @Override
   public boolean hasNext ()
   {
      if(count > 0 && globalIndex >= count)
      {
         return false;
      }
      if (result == null || result.isEmpty () || index == result.size ())
      {
         return loadNextPage ();
      }
      return true;
   }

   @Override
   public E next ()
   {
      if (hasNext ())
      {
         currentElement = result.get (index);
         index++;
         skip++;
         globalIndex++;
         return currentElement;
      }
      throw new NoSuchElementException ();
   }

   /**
    * Removes from the underlying collection the last element returned
    * by this iterator. This method calls {@link Pageable#delete(Object)}.
    *
    * @throws IllegalStateException if the {@code next} method has not yet been
    *                               called, or the {@code remove} method has
    *                               already been called after the last call to
    *                               the {@code next} method.
    */
   @Override
   public void remove ()
   {
      if (currentElement == null)
      {
         throw new IllegalStateException ();
      }

      result = result.subList (index, result.size ());
      index = 0;

      pageable.delete (currentElement);
      currentElement = null;
      skip--;
      globalIndex--;
   }

   /**
    *  Returns an iterator over a set of elements of type E.
    *  This methods add Collection sequential Iterable capability to allow
    *  java foreach short syntax calls.
    *  @return this iterator.
    *  @See {@link Collection#iterator()}
    */
   @Override
   public Iterator<E> iterator()
   {
      return this;
   }
}
