/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2016 GAEL Systems
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
package fr.gael.dhus.datastore.eviction;

import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.DeletedProduct;
import fr.gael.dhus.service.ProductService;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Manages eviction functions
 *
 */
@Component
public class EvictionManager
{
   private static final Logger LOGGER = LogManager.getLogger();

   @Autowired
   private ProductService productService;

   public EvictionManager(){}

   /**
    * Evicts <i>products</i> sequentially.
    *
    * @param products to evict
    */
   public void doEvict(List<Product> products)
   {
      if (!products.isEmpty())
      {
         for (Product product: products)
         {
            try
            {
               productService.systemDeleteProduct(product.getId(), true, DeletedProduct.AUTO_EVICTION);
               LOGGER.info("Evicted {} ({} bytes, {} bytes compressed)",
                     product.getIdentifier(), product.getSize(), product.getDownloadableSize());
            }
            catch (Exception e)
            {
               LOGGER.error("Unable to delete product: [uuid:{} identifier:{}]",
                     product.getUuid(), product.getIdentifier(), e);
            }
         }
      }
      else
      {
         LOGGER.info("No product to evict");
      }
   }

}
