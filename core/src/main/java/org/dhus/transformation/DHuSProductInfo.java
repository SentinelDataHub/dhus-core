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
package org.dhus.transformation;

import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.database.object.Product;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.dhus.api.transformation.ProductInfo;

public final class DHuSProductInfo implements ProductInfo
{
   private final Product product;
   private final Map<String, String> metadata;

   DHuSProductInfo(Product product)
   {
      this.product = product;
      this.metadata = product.getIndexes()
            .stream()
            .collect(Collectors.toMap(MetadataIndex::getName, MetadataIndex::getValue));
   }

   @Override
   public String getName()
   {
      return product.getIdentifier();
   }

   @Override
   public String getItemClass()
   {
      return product.getItemClass();
   }

   @Override
   public Map<String, String> getMetadata()
   {
      return Collections.unmodifiableMap(metadata);
   }

   @Override
   public boolean hasStream()
   {
      return product.isOnline();
   }

   @Override
   public long contentLength()
   {
      return (hasStream()) ? product.getSize() : -1;
   }

   @Override
   public InputStream getInputStream()
   {
      return null;
   }
}
