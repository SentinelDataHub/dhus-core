/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2019 GAEL Systems
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
package org.dhus.store.quota;

import org.dhus.ProductConstants;
import org.dhus.store.StoreException;
import org.dhus.store.datastore.async.AsyncProduct;

/**
 * Exception thrown when quotas are to be overpassed by a user request.
 */
public class QuotaException extends StoreException
{
   private static final long serialVersionUID = 1L;

   public QuotaException(String message)
   {
      super(message);
   }

   /**
    * A user tried to fetch more than the maximum amount he is allowed to.
    * Relates to the {@link org.dhus.store.datastore.async.AsyncProduct} and its
    * {@link org.dhus.store.datastore.async.AsyncProduct#asyncFetchData()} mechanism.
    */
   static public class ParallelFetchResquestQuotaException extends QuotaException
   {
      private static final long serialVersionUID = 1L;

      /**
       * Create new instance.
       *
       * @param userIdentifier user identifier
       * @param maxFetches maximum parallel fetches allowed
       * @param product whose fetch request failed due to this quota exception
       */
      public ParallelFetchResquestQuotaException(String userIdentifier, int maxFetches, AsyncProduct product)
      {
         super("User '" + userIdentifier + "' offline products retrieval quota exceeded (" + maxFetches + " fetches max) "
               + " trying to fetch product " + product.getName() + " (" + product.getProperty(ProductConstants.DATA_SIZE) + " bytes compressed)");
      }
   }
}
