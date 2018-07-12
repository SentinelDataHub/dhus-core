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
package org.dhus;

public interface ProductConstants
{
   // Available MessageDigest algorithms from Oracle Java 7
   public static final String CHECKSUM_PREFIX = "checksum";
   public static final String CHECKSUM_MD2 = CHECKSUM_PREFIX + ".MD2";
   public static final String CHECKSUM_MD5 = CHECKSUM_PREFIX + ".MD5";
   public static final String CHECKSUM_SHA_1 = CHECKSUM_PREFIX + ".SHA-1";
   public static final String CHECKSUM_SHA_256 = CHECKSUM_PREFIX + ".SHA-256";
   public static final String CHECKSUM_SHA_384 = CHECKSUM_PREFIX + ".SHA-384";
   public static final String CHECKSUM_SHA_512 = CHECKSUM_PREFIX + ".SHA-512";

   // Data
   public static final String DATA_SIZE = "data.size";
}
