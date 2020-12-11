/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019 GAEL Systems
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
package org.dhus.store.datastore.async.pdgs;

import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.service.SearchService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import org.dhus.Util;
import org.dhus.store.datastore.async.AsyncDataStoreException;
import org.dhus.store.datastore.config.PatternReplace;

/**
 * This type of PDGSDataStore uses configurable metadata indexes instead of product identifiers
 * to request products from special LTA Brokers, for example the one for Sentinel-2.
 * <p>
 * Its new property is the pattern of product requests URLs it performs,
 * containing the queryables of the metadata it uses to be replaced by their values.
 * e.g. {@code url?param1=[queryable1]&param2=[queryable2]}
 */
public class ParamPdgsDataStore extends PdgsDataStore
{
   private static final Logger LOGGER = LogManager.getLogger();

   // for matching queryables to products
   private static final SearchService SEARCH_SERVICE =
         ApplicationContextProvider.getBean(SearchService.class);

   // for requesting
   private final String getProductUrlParamPattern;
   private final List<String> requestQueryables;

   // for parsing responses
   private final Pattern productNamePattern;
   private final List<String> productNameQueryables;

   /**
    * Create a ParamPDGS DateStore.
    *
    * @param name                      of this DataStore
    * @param priority                  DataStores are ordered
    * @param isManager                 true to enable the ingest job in this instance of the DHuS (only one instance per cluster)
    * @param hfsLocation               path to the local HFS cache
    * @param patternReplaceIn          transform PDGS identifiers to DHuS identifiers
    * @param patternReplaceOut         transform DHuS identifiers to PDGS identifiers
    * @param maxPendingRequests        maximum number of pending orders at the same time
    * @param maxPendingRequests        maximum number of running orders at the same time
    * @param maximumSize               maximum size in bytes of the local HFS cache DataStore
    * @param currentSize               overall size of the local HFS cache DataStore (disk usage)
    * @param autoEviction              true to activate auto-eviction based on disk usage on the local HFS cache DataStore
    * @param urlService                URL to connect to the PDGS Service
    * @param login                     user to log to PDGS Service
    * @param password                  password to log to PDGS Service
    * @param interval                  interval
    * @param maxConcurrentsDownloads   maximum number of product download occurring in parallel
    * @param hashAlgorithms            to compute on restore
    * @param getProductUrlParamPattern pattern of product request URL to perform
    * @param productNamePattern        shape of product "names" in responses
    *
    * @throws URISyntaxException could not create PDGSDataStore
    * @throws IOException        could not create PDGS repo location directory
    */
   public ParamPdgsDataStore(String name, int priority, boolean isManager, String hfsLocation,
         PatternReplace patternReplaceIn, PatternReplace patternReplaceOut, Integer maxPendingRequests, Integer maxRunningRequests,
         long maximumSize, long currentSize, boolean autoEviction, String urlService, String login, String password,
         long interval, int maxConcurrentsDownloads, String[] hashAlgorithms, String getProductUrlParamPattern, String productNamePattern)
         throws URISyntaxException, IOException
   {
      super(name, priority, isManager, hfsLocation, patternReplaceIn, patternReplaceOut, maxPendingRequests, maxRunningRequests,
            maximumSize, currentSize, autoEviction, urlService, login, password, interval, maxConcurrentsDownloads, hashAlgorithms);

      LOGGER.debug("Initializing ParamPdgsDataStore: {}", name);

      Objects.requireNonNull(getProductUrlParamPattern);
      Objects.requireNonNull(productNamePattern);

      /*
       * prepare requesting tools
       */
      LOGGER.debug("GetProductUrlPattern: {}", getProductUrlParamPattern);
      this.getProductUrlParamPattern = getProductUrlParamPattern;

      // matches: "[)blabla(]"
      Pattern pattern = Pattern.compile("\\[\\)(.*?)\\(\\]");
      Matcher matcher = pattern.matcher(getProductUrlParamPattern);

      requestQueryables = new ArrayList<>();
      while (matcher.find())
      {
         requestQueryables.add(matcher.group(1));
      }
      LOGGER.debug("List of request queryables: {}", requestQueryables);

      /*
       * prepare response parsing tools
       */
      LOGGER.debug("ProductNamePattern: {}", productNamePattern);
      matcher = pattern.matcher(productNamePattern);
      productNameQueryables = new ArrayList<>();
      while (matcher.find())
      {
         productNameQueryables.add(matcher.group(1));
      }
      LOGGER.debug("List of product name queryables: {}", productNameQueryables);

      String finalPattern = productNamePattern;
      // replace queryables
      for (String queryable: productNameQueryables)
      {
         finalPattern = finalPattern.replace("[)" + queryable + "(]", "REGEX_TO_REPLACE");
      }
      // escape special characters
      finalPattern = Util.escapeSpecialRegexChars(finalPattern);

      // add matching groups
      finalPattern = finalPattern.replaceAll("REGEX_TO_REPLACE", "(.*)");

      // aaaaand compile (should look like this: "pdi_list=\\[(.*)\\]")
      LOGGER.debug("Final product name regex pattern: {}", finalPattern);
      this.productNamePattern = Pattern.compile(finalPattern);

      // note on pattern matching:
      // java 9 allows to do something like this instead
      // pattern.matcher(str).results().map(MatchResult::group).toArray(String[]::new);
   }

   @Override
   protected PDGSJob createNewJob(String remoteIdentifier, String uuid) throws AsyncDataStoreException
   {
      // get metadata indexes of product from the database
      List<MetadataIndex> indexes = PRODUCT_SERVICE.getIndexes(uuid);

      // prepare url by inserting desired indexes
      String getProductUrl = urlService + SEPARATOR + prepareUrlParams(indexes);

      // retrieve Job from remote
      return getJobForProductAt(getProductUrl);
   }

   private String prepareUrlParams(List<MetadataIndex> metadatums) throws AsyncDataStoreException
   {
      int queryableCount = requestQueryables.size();
      String preparedUrlParams = getProductUrlParamPattern;

      for (MetadataIndex metadatum: metadatums)
      {
         String queryable = metadatum.getQueryable();
         if (requestQueryables.contains(queryable))
         {
            //!\ could break something later if the value contains an illegal URL character
            preparedUrlParams = preparedUrlParams.replace("[)" + queryable + "(]", metadatum.getValue());
            queryableCount--;
         }
      }

      // not all queryables in the url have been replaced
      if (queryableCount > 0)
      {
         throw new AsyncDataStoreException(
               "Could not build product request URL due to " + queryableCount + " missing queryables: '" + preparedUrlParams + "'",
               "Could not request asynchronous product.", 500);
      }

      return preparedUrlParams;
   }

   @Override
   protected String getLocalIdentifierFromRemoteProductName(String string) throws AsyncDataStoreException
   {
      LOGGER.debug("Received product name (in {}) as {}", getName(), string);

      // extract queryable values
      Matcher matcher = productNamePattern.matcher(string);
      List<String> queryableValues = new ArrayList<>();
      if (matcher.find() && matcher.groupCount() >= productNameQueryables.size())
      {
         for (int group = 1; group <= matcher.groupCount(); group++)
         {
            queryableValues.add(matcher.group(group));
         }
      }
      else
      {
         throw new AsyncDataStoreException("Cannot find queryables (" + productNameQueryables + ") in string: " + string,
               "Could not retrieve asynchronous product.", 500);
      }

      // make solr query using queryables and values
      StringBuilder querySB = new StringBuilder();
      for (int i = 0; i < productNameQueryables.size(); i++)
      {
         querySB.append(productNameQueryables.get(i)).append(":").append(queryableValues.get(i)).append(" ");
      }
      String queryString = querySB.toString();

      // perform solr query
      LOGGER.debug("Performing Solr query: {}", queryString);
      SolrDocumentList productDocList = SEARCH_SERVICE.systemSearch(new SolrQuery(queryString).setRows(1));

      // return first result
      if (!productDocList.isEmpty())
      {
         SolrDocument productDocument = productDocList.get(0);
         String uuid = (String) productDocument.getFieldValue("uuid");
         String identifier = (String) productDocument.getFieldValue("identifier");

         LOGGER.debug("Found local product {} ({})", identifier, uuid);
         return identifier;
      }
      else
      {
         throw new AsyncDataStoreException("No product found for query: " + queryString + " (from string: " + string + ")",
               "Could not retrieve asynchronous product.", 500);
      }
   }
}
