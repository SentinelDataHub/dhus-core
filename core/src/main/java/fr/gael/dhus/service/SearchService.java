/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013-2019 GAEL Systems
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
package fr.gael.dhus.service;

import fr.gael.dhus.database.object.Collection;
import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.search.DHusSearchException;
import fr.gael.dhus.search.SolrDao;
import fr.gael.dhus.service.metadata.MetadataType;
import fr.gael.dhus.service.metadata.SolrField;

import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.Suggestion;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import org.dhus.ProductConstants;
import org.dhus.store.ingestion.IngestibleProduct;
import org.dhus.store.ingestion.MetadataExtractionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class SearchService extends WebService
{
   /** Logger. */
   private static final Logger LOGGER = LogManager.getLogger(SearchService.class);

   /** The `path` field is no longer used, kept for compatibility. */
   private static final String DEFAULT_PATH = "deprecated";

   /** Autowired dependency. */
   @Autowired
   private SolrDao solrDao;

   /** Autowired dependency. */
   @Autowired
   private CollectionService collectionService;

   /** Autowired dependency. */
   @Autowired
   private ProductService productService;

   /** Autowired dependency. */
   @Autowired
   private MetadataTypeService metadataTypeService;

   /** Max tries for solr indexing, default is 1 */
   private static final int MAX_TRY =
      Integer.valueOf(System.getProperty("dhus.solr.max.index.try", "1"));

   /**
    * Indexes or Reindexes a product.
    * {@link Product#getId()} is the unique key in the index.
    *
    * @param product a product
    *
    * @throws IOException connectivity issue
    * @throws SolrServerException indexing failed or server-side issue
    */
   // TODO move to MetadataStoreService
   public void index(Product product) throws IOException, SolrServerException
   {
      LOGGER.debug("Indexing product '{}'", product.getUuid());
      SolrInputDocument document = toInputDocument(product);
      indexSolrDocument(document);
   }

   /**
    * Indexes or Reindexes a product.
    *
    * @param inProduct a product
    * @param targetCollectionNames list of names of collections referencing that product
    *
    * @throws IOException connectivity issue
    * @throws SolrServerException indexing failed or server-side issue
    * @throws MetadataExtractionException Could not extract metadatas from given product
    */
   public void index(IngestibleProduct inProduct, List<String> targetCollectionNames)
         throws IOException, SolrServerException, MetadataExtractionException
   {
      LOGGER.debug("Indexing product '{}'", inProduct.getUuid());
      long start = System.currentTimeMillis();
      SolrInputDocument document = makeInputDocument(
            inProduct.getUuid(),
            (Long) inProduct.getProperty(ProductConstants.DATABASE_ID),
            inProduct.getItemClass(),
            inProduct.getMetadataIndexes(),
            targetCollectionNames,
            inProduct.isOnDemand());
      LOGGER.debug("Solr Input Document for product '{}' made in {}ms", inProduct.getUuid(), System.currentTimeMillis() - start);
      indexSolrDocument(document);
   }

   private void indexSolrDocument(final SolrInputDocument document) throws IOException, SolrServerException
   {
      long start = System.currentTimeMillis();
      boolean indexed = false;
      for (int tries = 0; !indexed && tries <= MAX_TRY; tries++)
      {
         try
         {
            solrDao.index(document);
            indexed = true;
         }
         catch (SolrServerException | IOException e)
         {
            if (tries == MAX_TRY)
            {
               LOGGER.error("Could not index product {}", document.getFieldValue("uuid"));
               throw e;
            }
         }
      }
      LOGGER.debug("Product '{}' indexed in {}ms",
            () -> document.getFieldValue("uuid"),
            () -> (System.currentTimeMillis() - start));
   }

   /**
    * Updates the given product from the index.
    *
    * @param product to update
    *
    * @throws IOException connectivity issue
    * @throws SolrServerException indexing failed or server-side issue
    */
   public void update(Product product) throws IOException, SolrServerException
   {
      index(product);
   }

   /**
    * Paginated search for system operations.
    * @param query Solr query `q` parameter.
    * @return an iterator of found products.
    */
   public Iterator<Product> search(String query)
   {
      try
      {
         final Iterator<SolrDocument> it = solrDao.scroll(new SolrQuery(query));

         return new Iterator<Product>()
         {
            @Override
            public boolean hasNext()
            {
               return it.hasNext();
            }

            @Override
            public Product next()
            {
               return productService.systemGetProduct((String) it.next().getFieldValue("uuid"));
            }

            @Override
            public void remove()
            {
               throw new UnsupportedOperationException();
            }
         };
      }
      catch (IOException|SolrServerException ex)
      {
         LOGGER.error("An exception occured while searching", ex);
      }
      return Collections.emptyIterator();
   }

   /**
    * Search.
    * <p>
    * Set `start` and `rows` values in the SolrQuery parameter to paginate the results.<br>
    * <strong>If no `rows` have been set, solr will only return 10 documents, no more.</strong>
    * <p>
    * To get the total number of document matching the given query, use {@code res.getNumFound()}.
    *
    * @param query a SolrQuery with at least a 'q' parameter set.
    * @return A list of solr document matching the given query.
    */
   @PreAuthorize("hasRole('ROLE_SEARCH')")
   public SolrDocumentList search(SolrQuery query)
   {
      Objects.requireNonNull(query);

      query.setQuery(solrDao.updateQuery(query.getQuery()));
      try
      {
         return solrDao.search(query).getResults();
      }
      catch (SolrServerException | IOException ex)
      {
         LOGGER.error(ex);
         throw new DHusSearchException("An exception occured while searching", ex);
      }
   }

   /**
    * Search.
    * Bypasses authentication checks.
    *
    * @see #search(SolrQuery)
    * @param query a SolrQuery with at least a 'q' parameter set
    * @return a list of Solr documents matching the given query
    */
   public SolrDocumentList systemSearch(SolrQuery query)
   {
      return search(query);
   }

   /**
    * Returns the product associated with the given solr document.
    * @param doc Index entry for a product, are returned by {@link #search(SolrQuery)}.
    * @return A product (database object).
    */
   public Product asProduct(SolrDocument doc)
   {
      Long pid = Long.class.cast(doc.get("id"));
      return productService.systemGetProduct(pid);
   }

   /**
    * Returns a list of suggestions for the given input.
    * @param input search input.
    * @return list of suggestions.
    */
   @PreAuthorize("hasRole('ROLE_SEARCH')")
   public List<String> getSuggestions(String input)
   {
      try
      {
         final List<Suggestion> sggs =
               solrDao.getSuggestions(input).getSuggestions().get("suggest");
         return new AbstractList<String>()
         {
            @Override
            public String get(int index)
            {
               return sggs.get(index).getTerm();
            }

            @Override
            public int size()
            {
               return sggs.size();
            }
         };
      }
      catch (IOException|SolrServerException ex)
      {
         LOGGER.error("Cannot get suggestions from Solr", ex);
      }
      return Collections.emptyList();
   }

   /**
    * Integrity check.
    */
   public void checkIndex()
   {
      try
      {
         SolrQuery query = new SolrQuery("*:*");
         query.setFilterQueries("*");
         query.setStart(0);
         Iterator<SolrDocument> it = solrDao.scroll(query);
         while (it.hasNext())
         {
            SolrDocument doc = it.next();
            Long pid = (Long) doc.get("id");
            Product product = productService.systemGetProduct(pid);
            if (product == null)
            {
               Long id = (Long) doc.getFieldValue("id");
               LOGGER.warn("Removing unknown product " + id + " from solr index");
               try
               {
                  solrDao.remove(id);
                  // decrease the offset, because a product has been removed
                  query.setStart(query.getStart() - 1);
               }
               catch (IOException e)
               {
                  LOGGER.error("Cannot remove Solr entry " + id, e);
               }
            }
         }
      }
      catch (IOException|SolrServerException ex)
      {
         LOGGER.error("Cannot check the index", ex);
      }
   }

   /**
    * Optimize the index, merges every segment of the index into one monolithic file.
    * Optimizing is very expensive, and if the index is constantly changing,
    * the slight performance boost will not last long...
    * The tradeoff is not often worth it for a non static index.
    * <p>
    * Blocking method, will block until optimization is complete. Solr won't respond to
    * search queries until optimization is done.
    */
   public void optimizeIndex()
   {
      try
      {
         solrDao.optimize();
      }
      catch (IOException|SolrServerException ex)
      {
         LOGGER.error("Cannot optimize index", ex);
      }
   }

   /**
    * Wipes the current index and reindex everything from the DataBase.
    */
   // TODO move to MetadataStoreService
   public void fullReindex()
   {
      try
      {
         solrDao.removeAll();

         long start = System.currentTimeMillis();

         final Iterator<Product> products = productService.systemGetProducts(null, null, 0);

         if (!products.hasNext())
         {
            LOGGER.warn("Reindex: table PRODUCTS is empty, aborting...");
            return;
         }

         // Makes an adaptor for SolrDao#batchIndex(...)
         Iterator<SolrInputDocument> it = new Iterator<SolrInputDocument>()
         {
            @Override
            public boolean hasNext()
            {
               return products.hasNext();
            }

            @Override
            public SolrInputDocument next()
            {
               Product product = products.next();
               product.setIndexes(productService.getIndexes(product.getUuid()));
               return toInputDocument(product);
            }

            @Override
            public void remove()
            {
               throw new UnsupportedOperationException("Do not use remove().");
            }
         };

         // Best config for bulk reindex
         // see: http://lucidworks.com/blog/2013/08/23/understanding-transaction-logs-softcommit-and-commit-in-sorlcloud/
         Map<String, String> config = new HashMap<>();
         config.put("updateHandler.autoSoftCommit.maxDocs", "-1");     // Opens a new searcher (the slowest operation).
         config.put("updateHandler.autoSoftCommit.maxTime", "-1");     // Opens a new searcher (the slowest operation).
         config.put("updateHandler.autoCommit.maxDocs", "-1");         // Time based autocommit is better.
         config.put("updateHandler.autoCommit.maxTime", "60000");      // 1 minute, controls the size of tlog files.
         config.put("updateHandler.autoCommit.openSearcher", "false"); // Opens a new searcher (the slowest operation).
         solrDao.setProperties(config);

         solrDao.batchIndex(it);
         solrDao.optimize();

         solrDao.unsetProperties(config.keySet());

         LOGGER.info("Full reindex done in " + (System.currentTimeMillis() - start) + "ms");
      }
      catch (IOException | SolrServerException ex)
      {
         LOGGER.error("Failed to reindex", ex);
      }
   }

   /**
    * Partially reindex products matched by the provided query.
    * Uses {@link SolrDao#batchIndex(java.util.Iterator)}.
    *
    * @param query to select a partition of products
    * @param enableTweaks true to to disable autocommits and autosoftcommits to speed-up the reindex
    *                     (disables NRT availability of solr documents)
    */
   public void partialReindex(SolrQuery query, boolean enableTweaks)
   {
      try
      {
         LOGGER.info("Partial reindex using query '{}', tweaks are {}", query, enableTweaks ? "on" : "off");
         long start = System.currentTimeMillis();

         SolrDocumentList queryRes = search(query);
         if (queryRes.isEmpty())
         {
            LOGGER.warn("Partial reindex: Query '{}' returned an empty document list, aborting...", query);
            return;
         }
         LOGGER.info("Partial reindex, {} products to reindex", queryRes.size());

         final Iterator<SolrDocument> toReindex = queryRes.iterator();

         // Makes an adaptor for SolrDao#batchIndex(...)
         Iterator<SolrInputDocument> it = new Iterator<SolrInputDocument>()
         {
            @Override
            public boolean hasNext()
            {
               return toReindex.hasNext();
            }

            @Override
            public SolrInputDocument next()
            {
               Product product = asProduct(toReindex.next());
               product.setIndexes(productService.getIndexes(product.getUuid()));
               return toInputDocument(product);
            }

            @Override
            public void remove()
            {
               throw new UnsupportedOperationException("Do not use remove()");
            }
         };

         Map<String, String> config = null;
         if (enableTweaks)
         {
            config = new HashMap<>();
            // Best config for bulk reindex
            // see: http://lucidworks.com/blog/2013/08/23/understanding-transaction-logs-softcommit-and-commit-in-sorlcloud/
            config.put("updateHandler.autoSoftCommit.maxDocs", "-1");     // Opens a new searcher (the slowest operation).
            config.put("updateHandler.autoSoftCommit.maxTime", "-1");     // Opens a new searcher (the slowest operation).
            config.put("updateHandler.autoCommit.maxDocs", "-1");         // Time based autocommit is better.
            config.put("updateHandler.autoCommit.maxTime", "60000");      // 1 minute, controls the size of tlog files.
            config.put("updateHandler.autoCommit.openSearcher", "false"); // Opens a new searcher (the slowest operation).
            solrDao.setProperties(config);
         }

         solrDao.batchIndex(it);

         if (enableTweaks && config != null)
         {
            solrDao.unsetProperties(config.keySet());
         }

         LOGGER.info("Partial reindex done in {}ms", (System.currentTimeMillis() - start));
      }
      catch (IOException | SolrServerException ex)
      {
         LOGGER.error("Failed to perform partial reindex", ex);
      }
   }

   private SolrInputDocument makeInputDocument(String productUuid, Long productId,
         String productClass, List<MetadataIndex> metadataIndices, List<String> targetCollectionNames, boolean onDemand)
   {
      SolrInputDocument doc = new SolrInputDocument();

      // Metadata
      if (metadataIndices != null && !metadataIndices.isEmpty())
      {
         for (MetadataIndex index: metadataIndices)
         {
            String type = index.getType();

            // Only textual information stored in field contents (full-text search)
            if ((type == null) || type.isEmpty() || "text/plain".equals(type))
            {
               doc.addField("contents", index.getValue());
            }

            // next line is considered bad practice:
            //doc.addField("contents", index.getQueryable());

            MetadataType mt = metadataTypeService
                  .getMetadataTypeByName(productClass, index.getName());
            SolrField sf = (mt != null)? mt.getSolrField(): null;

            if (sf != null || index.getQueryable() != null)
            {
               Boolean is_multivalued = (sf != null)? sf.isMultiValued(): null;
               String field_name = (sf != null)? sf.getName(): index.getQueryable().toLowerCase();

               if (is_multivalued != null && is_multivalued)
               {
                  doc.addField(field_name, index.getValue());
               }
               else
               {
                  doc.setField(field_name, index.getValue());
               }

               //LOGGER.debug("Added " + field_name + ":" + index.getValue());
            }
         }
      }
      else
      {
         LOGGER.warn("Product '{}' contains no metadata", productUuid);
      }

      // DHuS Attributes
      doc.setField("id", productId);
      doc.setField("uuid", productUuid);
      doc.setField("path", DEFAULT_PATH);
      doc.setField("ondemand", onDemand);

      // Collections
      if (targetCollectionNames != null)
      {
         for (String collectionName : targetCollectionNames)
         {
            doc.addField("collection", collectionName);
         }
      }

      return doc;
   }

   /**
    * Makes a SolrInputDocument from a Product database object.
    * The returned document can be indexed as is.
    *
    * @param product to convert
    * @return an indexable solr document
    */
   // TODO move to metadatastoreservice
   private SolrInputDocument toInputDocument(Product product)
   {
      long start = System.currentTimeMillis();
      List<Collection> collections = collectionService.getCollectionsOfProduct(product.getId());
      List<String> collectionNames = new ArrayList<>();
      if (collections != null && !collections.isEmpty())
      {
         collections.forEach((collection) -> collectionNames.add(collection.getName()));
      }
      SolrInputDocument res = makeInputDocument(product.getUuid(), product.getId(), product.getItemClass(), product.getIndexes(), collectionNames, product.isOnDemand());
      LOGGER.debug("Solr Input Document for product '{}' made in {}ms", product.getUuid(), System.currentTimeMillis() - start);
      return res;
   }

   /**
    * Removes a product from the index by uuid.
    *
    * @param uuid the uuid of a product
    */
   public void removeProduct(String uuid)
   {
      try
      {
         solrDao.removeProduct(uuid);
      }
      catch (SolrServerException | IOException e)
      {
         LOGGER.error("Cannot remove product {} from index", uuid, e);
      }
   }
}
