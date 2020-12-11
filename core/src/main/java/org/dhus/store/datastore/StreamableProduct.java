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
package org.dhus.store.datastore;

import java.io.InputStream;

import org.dhus.AbstractProduct;

/**
 * Creates a product from an InputStream.
 * <p>
 * The {@link #getImpl(java.lang.Class)} method only accepts a super implementation of InputStream
 * or the InputStream class itself.
 * <p>
 * The {@link #getImpl(java.lang.Class)} always return the same steam, it is not thread safe and can
 * only be used once with one DataStore.
 * To download a product from an HTTP endpoint, use the {@link fr.gael.dhus.util.http.DownloadableProduct}.
 * <p>
 * This class is intended to be used by {@link org.dhus.store.datastore.async.AbstractAsyncCachedDataStore}
 * to stream products directly in their HFS DataStore cache.
 */
public class StreamableProduct extends AbstractProduct
{
   private final InputStream stream;

   private String name;

   public StreamableProduct(InputStream stream)
   {
      this.stream = stream;
   }

   @Override
   public boolean hasImpl(Class<?> cl)
   {
      return cl.isAssignableFrom(InputStream.class);
   }

   @Override
   public <T> T getImpl(Class<? extends T> cl)
   {
      if (cl.isAssignableFrom(InputStream.class))
      {
         return cl.cast(stream);
      }
      return null;
   }

   @Override
   public String getName()
   {
      return name;
   }

   @Override
   public void setName(String name)
   {
      this.name = name;
   }
}
