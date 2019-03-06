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
package org.dhus.store.metadatastore;

import fr.gael.dhus.service.SearchService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;

import org.dhus.Product;
import org.dhus.store.StoreException;
import org.dhus.store.ingestion.IngestibleProduct;
import org.springframework.beans.factory.annotation.Autowired;

public class SolrMetadataStore implements MetadataStore
{
   @Autowired
   private SearchService searchService;

   public static final String NAME = "SolrMetadataStore";

   @Override
   public void addProduct(IngestibleProduct inProduct) throws StoreException
   {
      addProduct(inProduct, Collections.<String>emptyList());
   }

   @Override
   public void addProduct(IngestibleProduct inProduct, List<String> targetCollectionNames) throws StoreException
   {
      try
      {
         searchService.index(inProduct, targetCollectionNames);
      }
      catch (IOException | SolrServerException e)
      {
         throw new StoreException(e);
      }
   }

   @Override
   public void deleteProduct(String uuid) throws StoreException
   {
      searchService.removeProduct(uuid);
   }

   @Override
   public boolean isReadOnly()
   {
      return false;
   }

   @Override
   public boolean hasProduct(String uuid)
   {
      // TODO
      return false;
   }

   @Override
   public Product getProduct(String uuid)
   {
      // TODO
      throw new UnsupportedOperationException();
   }

}
