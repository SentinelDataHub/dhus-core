/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018 GAEL Systems
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
package org.dhus.api.transformation;

import java.io.InputStream;
import java.util.Map;

/**
 * Allows to retrieve some information about a product.
 */
public interface ProductInfo
{
   /**
    * Returns the product name.
    *
    * @return product name
    */
   String getName();

   /**
    * Returns a string representation of the product URI item class.
    *
    * @return product class
    */
   String getItemClass();

   /**
    * Returns product metadata.
    *
    * @return map containing product metadata
    */
   Map<String, String> getMetadata();

   /**
    * True if the product data is accessible.
    *
    * @return true if the product data is accessible, otherwise false
    */
   boolean hasStream();

   /**
    * Returns product content-length.
    *
    * @return product content-length
    */
   long contentLength();

   /**
    * Returns input stream product data.
    *
    * @return product data or null if no data linked to the product
    */
   InputStream getInputStream();
}
