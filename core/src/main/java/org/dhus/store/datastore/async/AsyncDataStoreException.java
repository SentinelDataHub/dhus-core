/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019 GAEL Systems
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
package org.dhus.store.datastore.async;

import org.dhus.store.datastore.DataStoreException;

public class AsyncDataStoreException extends DataStoreException
{
   private static final long serialVersionUID = -1023764626340271980L;

   private final int httpStatusCode;
   private final String userMessage;

   public AsyncDataStoreException()
   {
      super();
      this.userMessage = "An unexpected error occured";
      this.httpStatusCode = 500;
   }

   public AsyncDataStoreException(String message, String userMessage, int httpStatusCode)
   {
      super(message);
      this.userMessage = userMessage;
      this.httpStatusCode = httpStatusCode;
   }

   /**
    * @return the httpStatusCode
    */
   public int getHttpStatusCode()
   {
      return httpStatusCode;
   }

   /**
    * @return the userMessage
    */
   public String getUserMessage()
   {
      return userMessage;
   }

   public static class ProductNotFoundException extends AsyncDataStoreException
   {
      private static final long serialVersionUID = 1L;

      public ProductNotFoundException(String message)
      {
         super(message, "Requested product not found", 404);
      }
   }

   public static class TooManyRequestsException extends AsyncDataStoreException
   {
      private static final long serialVersionUID = 1L;

      public TooManyRequestsException(String message)
      {
         super(message, "Quota exceeded", 429);
      }
   }

   public static class BadGatewayException extends AsyncDataStoreException
   {
      private static final long serialVersionUID = 1L;

      public BadGatewayException(String message)
      {
         super(message, "Bad gateway", 502);
      }
   }
}