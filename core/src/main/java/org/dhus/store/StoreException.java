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
package org.dhus.store;

/**
 * Exception thrown by Store operations.
 * Base class for all store related exceptions.
 */
public class StoreException extends Exception
{
   private static final long serialVersionUID = 1L;

   /**
    * @see Exception#Exception()
    */
   public StoreException() {}

   /**
    * @see Exception#Exception(String)
    * @param message
    */
   public StoreException(String message)
   {
      super(message);
   }

   /**
    * @see Exception#Exception(Throwable)
    * @param cause
    */
   public StoreException(Throwable cause)
   {
      super(cause);
   }

   /**
    * @see Exception#Exception(String, Throwable)
    * @param message
    * @param cause
    */
   public StoreException(String message, Throwable cause)
   {
      super(message, cause);
   }

   /**
    * Exception thrown by a DataStore if it already has data for the given product UUID.
    */
   public static class ProductAlreadyExistsException extends StoreException
   {
      private static final long serialVersionUID = 1L;

      public ProductAlreadyExistsException(String productUuid)
      {
         super("Product of UUID " + productUuid + " already exists");
      }
   }
}
