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
package org.dhus.store.datastore;

public class DataStoreConstants
{
   private DataStoreConstants(){};

   public static final String DIGEST_ALGORITHM_MD5    = "MD5";
   public static final String DIGEST_ALGORITHM_SHA1   = "SHA-1";
   public static final String DIGEST_ALGORITHM_SHA256 = "SHA-256";

   public static final String PROP_KEY_TRANSFER_DIGEST = "transfer.compute.digests";
   public static final String PROP_VAL_TRANSFER_DIGEST_DEFAULT = DIGEST_ALGORITHM_MD5;
}
