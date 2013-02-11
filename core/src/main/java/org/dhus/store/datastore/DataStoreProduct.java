/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016 GAEL Systems
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

import org.dhus.Product;

public interface DataStoreProduct extends Product
{
   public static final Long UNKNOWN_PRODUCT_SIZE = -1L;
   /**
    * Retrieves size of product.
    * @return size of product in bytes.
    */
   public Long getContentLength();

   /**
    * Retrieves the resource location.
    * @return a string to locate the physical resource
    */
   public String getResourceLocation();
}
