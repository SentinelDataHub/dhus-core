/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2018 GAEL Systems
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
package org.dhus;

import java.util.Map;
import java.util.stream.Collectors;

public abstract class ProductConstants
{
   // Available MessageDigest algorithms from Oracle Java 7
   public static final String CHECKSUM_PREFIX = "checksum";
   public static final String CHECKSUM_MD2 = CHECKSUM_PREFIX + ".MD2";
   public static final String CHECKSUM_MD5 = CHECKSUM_PREFIX + ".MD5";
   public static final String CHECKSUM_SHA_1 = CHECKSUM_PREFIX + ".SHA-1";
   public static final String CHECKSUM_SHA_256 = CHECKSUM_PREFIX + ".SHA-256";
   public static final String CHECKSUM_SHA_384 = CHECKSUM_PREFIX + ".SHA-384";
   public static final String CHECKSUM_SHA_512 = CHECKSUM_PREFIX + ".SHA-512";

   public static Map<String, String> getChecksums(Product product)
   {
      return product.getPropertyNames()
            .stream()
            // only keep properties with the checksum prefix
            .filter(propertyName -> propertyName.startsWith(CHECKSUM_PREFIX))
            // make a checksum map
            .collect(Collectors.toMap(
                  // remove checksum prefix to make map key
                  propertyName -> propertyName.replaceFirst(CHECKSUM_PREFIX + ".", ""),
                  // get the checksum's value
                  propertyName -> (String) product.getProperty(propertyName)));
   }

   public static final String checksum(String algorithm)
   {
      return CHECKSUM_PREFIX + "." + algorithm;
   }

   // Metadata
   public static final String IDENTIFIER = "identifier";
   public static final String CREATION_DATE = "creationDate";
   public static final String FOOTPRINT = "footprint";
   public static final String CONTENT_START = "contentStart";
   public static final String CONTENT_END = "contentEnd";
   public static final String ITEM_CLASS = "itemClass";
   public static final String ORIGIN = "origin";
   public static final String METADATA_INDEXES = "metadataIndexes";

   // Derived products
   public static final String QUICKLOOK = "quicklook";
   public static final String THUMBNAIL = "thumbnail";
   public static final String QUICKLOOK_SIZE = "quicklook_size";
   public static final String THUMBNAIL_SIZE = "thumbnail_size";

   // Data
   public static final String DATA_SIZE = "data.size";

   // Other
   public static final String DATABASE_ID = "database.id";
   public static final String UUID = "uuid";
}
