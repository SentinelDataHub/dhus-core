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
package fr.gael.dhus.util.stream;

import fr.gael.dhus.database.object.config.source.Source;
import fr.gael.dhus.sync.smart.ProductInfo;
import fr.gael.dhus.sync.smart.SynchronizerRules;

import java.io.InputStream;

public interface ProductStreamFactory
{
   /**
    * Generates a new input stream to download the requested product from the given source.
    * <p>
    * If the targeted product content cannot be represented into a {@link InputStream}, this
    * method returns {@code null}.
    *
    * @param productInfo requested product information
    * @param source      source where retrieve the product data
    * @param rules       rules to follow during product data transfer
    * @param skip        bytes number to skip from the beginning product data
    * @return a input stream representing the targeted content, or {@code null}
    * @throws IllegalArgumentException if a parameter is not valid
    */
   InputStream generateInputStream(ProductInfo productInfo, Source source,
         SynchronizerRules rules, long skip);
}
