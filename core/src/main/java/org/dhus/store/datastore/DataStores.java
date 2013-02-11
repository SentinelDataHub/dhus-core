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
package org.dhus.store.datastore;

import fr.gael.dhus.util.MultipleDigestInputStream;

import java.util.ArrayList;
import java.util.List;

import org.dhus.Product;
import org.dhus.ProductConstants;

import org.springframework.security.crypto.codec.Hex;

/**
 * Utility class for Store objects.
 */
public class DataStores
{
   // FIXME make DataStore comparable and add a default implementation of compareTo to the interface (requires java 8)
   /**
    * Compares two Store objects based on their priority and name.
    *
    * @param ds1
    * @param ds2
    * @return
    */
   public static int compare(DataStore ds1, DataStore ds2)
   {
      int res = ds1.getPriority() - ds2.getPriority();
      if (res == 0)
      {
         return ds1.getName().compareTo(ds2.getName());
      }
      else
      {
         return res;
      }
   }

   public static void throwErrors(List<Throwable> throwableList, String operation, String uuid) throws DataStoreException
   {
      if (throwableList.isEmpty())
      {
         return;
      }

      if (throwableList.size() == 1)
      {
         Throwable throwable = throwableList.get(0);
         if (DataStoreException.class.isAssignableFrom(throwable.getClass()))
         {
            throw DataStoreException.class.cast(throwable);
         }
         else
         {
            throw new DataStoreException(throwable);
         }
      }

      StringBuilder sb = new StringBuilder("Error while performing operation ");
      sb.append(operation).append(" with product ").append(uuid).append(": ");
      for (Throwable throwable: throwableList)
      {
         sb.append('[').append(throwable.getMessage()).append(']');
      }

      throw new DataStoreException(sb.toString());
   }

   /**
    * Removes the '.SAFE' extension (if any).
    *
    * @param identifier product identifier (should ends with '.SAFE')
    * @return identifier without the '.SAFE' extension
    */
   public static String unSafe(String identifier)
   {
      if (identifier.endsWith(".SAFE"))
      {
         return identifier.substring(0, identifier.length()-5);
      }
      return identifier;
   }

   /**
    * Returns the hash algorithms in hashAlgos that were not yet computed for the given product.
    *
    * @param product a non null instance
    * @param hashAlgos array of hash algorithm names to check
    * @return an array of hash algorithm names to compute
    */
   public static String[] checkHashAlgorithms(Product product, String[] hashAlgos)
   {
      if (hashAlgos == null)
      {
         return EMPTY_ARRAY;
      }
      else if (hashAlgos.length == 0)
      {
         return hashAlgos;
      }
      ArrayList<String> res = new ArrayList<>(hashAlgos.length);
      for (String algo: hashAlgos)
      {
         String k = ProductConstants.CHECKSUM_PREFIX + "." + algo;
         if (product.getProperty(k) == null)
         {
            res.add(algo);
         }
      }
      return res.toArray(new String[res.size()]);
   }
   private static final String[] EMPTY_ARRAY = new String[] {}; // To avoid creating new objects

   /**
    * Set the `checksum.HASH` properties on the given product, for each hash computed by the
    * MultipleDigestInputStream parameter.
    *
    * @param stream a non null instance that read the product's data completely
    * @param product a non null product instance to set
    */
   public static void extractAndSetChecksum(MultipleDigestInputStream stream, Product product)
   {
      stream.getDigests().entrySet().stream().forEach(entry -> {
         String key = ProductConstants.checksum(entry.getKey());
         String val = new String(Hex.encode(entry.getValue().digest()));
         product.setProperty(key, val);
      });
   }
}
