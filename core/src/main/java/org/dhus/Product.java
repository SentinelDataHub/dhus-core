/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016,2017 GAEL Systems
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

import java.io.IOException;
import java.util.Set;

import org.dhus.store.HasImpl;

/**
 * Product is an abstract representation of domain model products.
 */
public interface Product extends HasImpl
{
   /**
    * Returns the name of this product.
    * @return name
    */
   // TODO define the "name" of a product or remove the method
   public String getName();

   /**
    * Sets the name of this product.
    * @param name
    */
   // TODO define the "name" of a product or remove the method
   public void setName(String name);

   /**
    * Returns a custom product-specific property.
    * Such a property capability allow specific datastores implementations to
    * share their own computations.
    * <p>
    * The set of returned properties may be further extended or customized
    * specific implementation level.
    *
    * @param key the property key.
    * @return the value in this property with the specified key value, or
    * <code>null</code> if the property is not found.
    */
   public Object getProperty(String key);

   /**
    * Adds a custom product-specific property.
    * @param key the property key.
    * @param value the value in this property with the specified key value.
    * @return the previous value associated with the given key
    */
   public Object setProperty(String key, Object value);

   /**
    * Returns a set of keys in this property list.
    * @return a set of keys in this property list.
    */
   public Set<String> getPropertyNames();
   
   public default void closeProduct() throws IOException
   {
      return ;
   }
}
