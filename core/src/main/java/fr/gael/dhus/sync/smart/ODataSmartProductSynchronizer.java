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
package fr.gael.dhus.sync.smart;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

import fr.gael.dhus.database.object.config.source.Source;
import fr.gael.dhus.database.object.config.synchronizer.SmartProductSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerSource;
import fr.gael.dhus.olingo.ODataClient;
import fr.gael.dhus.service.ISourceService;
import fr.gael.dhus.sync.ProductSynchronizerUtils;
import fr.gael.dhus.sync.SyncException;
import fr.gael.dhus.sync.Synchronizer;
import fr.gael.dhus.util.JTSGeometryValidator;
import fr.gael.dhus.util.XmlProvider;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.odata2.api.edm.EdmLiteralKind;
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind;
import org.apache.olingo.odata2.api.edm.provider.Facets;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;

import org.dhus.store.ingestion.IngestibleODataProduct;

public class ODataSmartProductSynchronizer extends Synchronizer
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final String PRODUCT_ENTITY_SET = "Products";
   private static final String COLLECTION_ENTITY_SET = "Collections";
   private static final String BASIC_SYNC_FILTER = "CreationDate ge ";
   private static final String QUERY_FILTER_KEY = "$filter";
   private static final String QUERY_ORDER_KEY = "$orderby";
   private static final String QUERY_ORDER_VALUE = "CreationDate";
   private static final String QUERY_RAW_KEY = "$top";
   private static final String QUERY_SKIP_KEY = "$skip";
   private static final String QUERY_EXPAND_KEY = "$expand";
   private static final String QUERY_EXPAND_VALUE = "Class,Attributes,Products";

   /**
    * Extracts if possible the 'filterParam' property value from a {@link SmartProductSynchronizer}.
    *
    * @param conf smart synchronizer configuration
    * @return a string representation of filter, or null
    */
   private static String extractParamFilter(SmartProductSynchronizer conf)
   {
      String paramFilter = conf.getFilterParam();
      if (paramFilter == null || paramFilter.isEmpty())
      {
         return null;
      }
      return paramFilter;
   }

   /**
    * Generates if possible a geographical WKT validator from a smart synchronizer configuration.
    *
    * @param conf synchronizer configuration
    * @return a geographical WKT validator, or null
    * @throws ParseException if a parsing problem occurs
    */
   private static JTSGeometryValidator.WKTAdapter initGeoFilter(SmartProductSynchronizer conf)
         throws ParseException
   {
      String geofilter_op = conf.getGeofilterOp();
      String geofilter_shape = conf.getGeofilterShape();
      JTSGeometryValidator.WKTAdapter validator = null;

      if (geofilter_shape != null && !geofilter_shape.isEmpty()
            && geofilter_op != null && !geofilter_op.isEmpty())
      {
         Geometry geometry = JTSGeometryValidator.WKTAdapter.WKTtoJTSGeometry(geofilter_shape);
         JTSGeometryValidator.WithReference refval = new JTSGeometryValidator.WithReference(geometry);
         switch (geofilter_op)
         {
            case "disjoint":
               validator = new JTSGeometryValidator.WKTAdapter(refval.new Disjoint());
               break;
            case "within":
               validator = new JTSGeometryValidator.WKTAdapter(refval.new Within());
               break;
            case "contains":
               validator = new JTSGeometryValidator.WKTAdapter(refval.new Contains());
               break;
            case "intersects":
               validator = new JTSGeometryValidator.WKTAdapter(refval.new Intersects());
               break;
            default:
               throw new IllegalArgumentException("Invalid geofilter operator");
         }
      }
      return validator;
   }

   // synchronizer mechanism
   private final ISourceService sourceService;
   private final ProductSynchronizerUtils utils;
   private final IngestionPageFactory ingestionPageFactory;

   // synchronizer properties
   private final int pageSize;
   private final String paramFilter;
   private final JTSGeometryValidator.WKTAdapter geoFilter;

   private SynchronizerSource currentSyncSource;
   private IngestionPage ingestionPage;

   /**
    * Constructs a new SmartProductSynchronizer from a configuration.
    *
    * @param sourceService source service bean
    * @param utils sync utilities bean
    * @param pageFactory a page factory
    * @param configuration smart synchronizer configuration
    * @throws com.vividsolutions.jts.io.ParseException could not parse geofilter from configuration
    */
   public ODataSmartProductSynchronizer(ISourceService sourceService, ProductSynchronizerUtils utils,
         IngestionPageFactory pageFactory, SmartProductSynchronizer configuration)
         throws ParseException
   {
      super(configuration);
      this.pageSize = configuration.getPageSize();
      this.sourceService = sourceService;
      this.utils = utils;
      this.ingestionPageFactory = pageFactory;
      this.paramFilter = extractParamFilter(configuration);
      this.geoFilter = initGeoFilter(configuration);
   }

   private String getProductOrigin(ODataEntry entry)
   {
      StringBuilder origin = new StringBuilder();
      origin.append(sourceService.getSource(currentSyncSource).getUrl())
            .append("/Products('").append(entry.getProperties().get("Id")).append("')")
            .append("/$value");

      return origin.toString();
   }

   private String getProductUuid(ODataEntry entry)
   {
      return (String) entry.getProperties().get("Id");
   }

   /**
    * Retrieves products list from an OData service.
    * <p>
    * This products are retrieved from the current source used by the synchronizer (following
    * synchronizer rules).
    *
    * @param skip number of products to skip
    * @return feed of OData entries
    * @throws ODataException       if no OData service have been found
    * @throws IOException          if the OData client fails to contact the serve
    * @throws URISyntaxException   if the current source url parameter is invalid
    * @throws InterruptedException if running thread has been interrupted
    */
   private ODataFeed getPage(int skip) throws ODataException, IOException,
         URISyntaxException, InterruptedException
   {
      // Prepare query
      String collection = currentSyncSource.getSourceCollection();
      StringBuilder query = new StringBuilder();
      if (collection != null)
      {
         query.append(COLLECTION_ENTITY_SET).append(collection).append('/');
      }
      query.append(PRODUCT_ENTITY_SET);

      // Prepare query parameters
      String dateIndex = EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance()
            .valueToString(currentSyncSource.getLastCreated().toGregorianCalendar(),
                  EdmLiteralKind.URI, new Facets().setPrecision(3));

      StringBuilder filter = new StringBuilder();
      filter.append(BASIC_SYNC_FILTER).append(dateIndex);
      if (paramFilter != null)
      {
         filter.append(" and (").append(paramFilter).append(')');
      }
      Map<String, String> queryParam = new HashMap<>();
      queryParam.put(QUERY_FILTER_KEY, filter.toString());
      queryParam.put(QUERY_EXPAND_KEY, QUERY_EXPAND_VALUE);
      queryParam.put(QUERY_SKIP_KEY, String.valueOf(skip));
      queryParam.put(QUERY_RAW_KEY, String.valueOf(pageSize));
      queryParam.put(QUERY_ORDER_KEY, QUERY_ORDER_VALUE);

      // performs request
      int timeout = ((SmartProductSynchronizer) syncConf).getTimeout().intValue();
      Source source = sourceService.getSource(currentSyncSource);
      ODataClient client = new ODataClient(source.getUrl(), source.getUsername(), source.getPassword(), timeout, timeout);
      long start = System.currentTimeMillis();
      ODataFeed feed = client.readFeed(query.toString(), queryParam);
      long duration = System.currentTimeMillis() - start;
      LOGGER.debug("SmartSynchronizer#{} query({}) done in {} ms", getId(), query, duration);
      return feed;
   }

   private int internalGetNewProducts(List<IngestibleODataProduct> products, int skip)
         throws InterruptedException
   {
      int count = 0;
      ODataFeed feed;

      try
      {
         feed = getPage(skip);
      }
      catch (ODataException | IOException | URISyntaxException e)
      {
         throw new SyncException("Cannot access to the remote service", e);
      }

      for (ODataEntry entry: feed.getEntries())
      {
         if (Thread.currentThread().isInterrupted())
         {
            throw new InterruptedException();
         }
         if (!ProductSynchronizerUtils.isOnline(entry))
         {
            LOGGER.debug("Product {} is not online", entry.getProperties().get("Name"));
            continue;
         }

         try
         {
            String origin = getProductOrigin(entry);
            IngestibleODataProduct product = utils.entryToDownloadableProduct(entry, origin,
                  (SmartProductSynchronizer) syncConf, sourceService.getSource(currentSyncSource));

            if (!utils.exists(product) && utils.inROI(product, geoFilter))
            {
               count = count + 1;
               products.add(product);
            }
         }
         catch (IngestibleODataProduct.MissingProductsException e)
         {
            LOGGER.warn("SmartSynchronizer#{} Skipping product {} from {} ({})",
                  getId(),
                  getProductUuid(entry),
                  sourceService.getSource(currentSyncSource).getUrl(),
                  e.getMessage());
         }
      }

      return count;
   }

   private List<IngestibleODataProduct> getNewProducts() throws InterruptedException
   {
      int skip = 0;
      int found = -1;
      List<IngestibleODataProduct> productList = new ArrayList<>(pageSize);

      while (productList.size() < pageSize && found != 0)
      {
         if (Thread.currentThread().isInterrupted())
         {
            throw new InterruptedException();
         }

         try
         {
            found = internalGetNewProducts(productList, skip);
            skip = skip + found;
         }
         catch (SyncException e)
         {
            return productList;
         }
      }
      return productList;
   }

   private void logPageStatus(IngestionPage.PageStatus status)
   {
      LOGGER.info("SmartSynchronizer#{} page status completed at {}% | completed:{}, failed:{}, pending:{}",
            getId(), status.getPagePercentage(), status.getCompleted().size(),
            status.getFailed().size(), status.pendingIngestion());
   }

   private SynchronizerSource retrieveSynchronizerSource(Source source)
   {
      SmartProductSynchronizer config = (SmartProductSynchronizer) syncConf;
      for (SynchronizerSource syncSource: config.getSources().getSource())
      {
         if (source.getId() == syncSource.getSourceId())
         {
            return syncSource;
         }
      }
      return null;
   }

   @Override
   public boolean synchronize() throws InterruptedException
   {
      LOGGER.info("SmartSynchronizer#{} started", super.getSynchronizerConf().getId());

      if (ingestionPage == null)
      {
         LOGGER.debug("SmartSynchronizer#{}: Ingestion page null", getId());
         SmartProductSynchronizer conf = (SmartProductSynchronizer) syncConf;
         Source source = sourceService.getBestSource(sourceService.getSource((conf.getSources().getSource())));

         if (currentSyncSource == null)
         {
            currentSyncSource = retrieveSynchronizerSource(source);
         }

         List<IngestibleODataProduct> products = getNewProducts();
         if (products.isEmpty())
         {
            LOGGER.info("SmartSynchronizer#{}: no products found from Source#{}", conf.getId(), source.getId());
            return false;
         }

         ingestionPage = ingestionPageFactory.createIngestionPage((SmartProductSynchronizer) getSynchronizerConf(), products);

         LOGGER.info("SmartSynchronizer#{} starting to synchronize {} new products found from Source#{}",
               getId(), ingestionPage.size(), currentSyncSource.getSourceId());
      }

      logPageStatus(ingestionPage.getStatus());
      LOGGER.debug("SmartSynchronizer#{}: currentSyncSource='{}', ingestionPage size={}", getId(), currentSyncSource, ingestionPage.size());
      if (ingestionPage.isDone())
      {
         currentSyncSource.setLastCreated(XmlProvider.getCalendar(ingestionPage.lastValidCreationDate()));
         utils.saveConfiguration(this);

         try
         {
            ingestionPage.close();
            LOGGER.debug("SmartSynchronizer#{}: ingestion page closed", getId());
         }
         catch (Exception e)
         {
            LOGGER.warn("SmartSynchronizer#{}: Cannot close completely previous page", getId());
         }
         finally
         {
            currentSyncSource = null;
            ingestionPage = null;
            LOGGER.debug("SmartSynchronizer#{}: Current SyncSource and ingestionPage set to null", getId());
         }
      }

      return true;
   }

   @Override
   public void close() throws Exception
   {
      if (ingestionPage != null)
      {
         ingestionPage.close();
         LOGGER.debug("SmartSynchronizer#{}: ingestion page closed", getId());
      }
   }
}
