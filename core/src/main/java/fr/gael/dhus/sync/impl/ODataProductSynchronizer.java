/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2015-2020 GAEL Systems
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
package fr.gael.dhus.sync.impl;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer;
import fr.gael.dhus.factory.MetadataFactory;
import fr.gael.dhus.olingo.ODataClient;
import fr.gael.dhus.olingo.v1.Model;
import fr.gael.dhus.service.DataStoreService;
import fr.gael.dhus.service.ISynchronizerService;
import fr.gael.dhus.service.MetadataTypeService;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.service.metadata.MetadataType;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.sync.ProductSynchronizerUtils;
import fr.gael.dhus.sync.SyncException;
import fr.gael.dhus.sync.Synchronizer;
import fr.gael.dhus.util.JTSGeometryValidator;
import fr.gael.dhus.util.XmlProvider;
import fr.gael.dhus.util.http.DownloadableProduct;
import fr.gael.dhus.util.http.HttpAsyncClientProducer;
import fr.gael.dhus.util.http.InterruptibleHttpClient;
import fr.gael.dhus.util.http.Timeouts;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.odata2.api.edm.EdmLiteralKind;
import org.apache.olingo.odata2.api.edm.EdmSimpleType;
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeException;
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind;
import org.apache.olingo.odata2.api.edm.provider.Facets;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataDeltaFeed;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;

import org.dhus.Product;
import org.dhus.ProductConstants;
import org.dhus.metrics.Utils;
import org.dhus.store.ParallelProductSetter;
import org.dhus.store.StoreException;
import org.dhus.store.StoreService;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreFactory.InvalidConfigurationException;
import org.dhus.store.datastore.DataStoreManager;
import org.dhus.store.datastore.ProductReference;
import org.dhus.store.datastore.config.DataStoreManager.UnavailableNameException;
import org.dhus.store.datastore.config.DataStoreRestriction;
import org.dhus.store.ingestion.IngestibleODataProduct;
import org.dhus.store.ingestion.IngestibleODataProduct.MissingProductsException;
import org.dhus.store.datastore.config.HfsDataStoreConf;
import org.dhus.store.ingestion.ProcessingManager;

import org.hibernate.exception.LockAcquisitionException;

import org.springframework.dao.CannotAcquireLockException;

/**
 * A synchronizer using the OData API of another DHuS.
 */
public class ODataProductSynchronizer extends Synchronizer
{
   /** Log. */
   private static final Logger LOGGER = LogManager.getLogger(ODataProductSynchronizer.class);

   /** Number of download attempts (-1 for infinite, must be at least 1). */
   private static final int DL_TRIES = Integer.getInteger("dhus.sync.download_attempts", 10);

   /** Synchronizer Service, for this sync to save its own settings. */
   private static final ISynchronizerService SYNC_SERVICE =
         ApplicationContextProvider.getBean(ISynchronizerService.class);

   /** Product service, to store Products in the database. */
   private static final ProductService PRODUCT_SERVICE =
         ApplicationContextProvider.getBean(ProductService.class);

   /** Metadata Type Service, MetadataIndex name to Queryable. */
   private static final MetadataTypeService METADATA_TYPE_SERVICE =
         ApplicationContextProvider.getBean(MetadataTypeService.class);

   /** Service that manages all Stores. */
   private static final StoreService STORE_SERVICE =
         ApplicationContextProvider.getBean(StoreService.class);

   /** Service that manages DataStores. */
   private static final DataStoreManager DATA_STORE_MANAGER =
         ApplicationContextProvider.getBean(DataStoreManager.class);

   /** Service to store persistent DataStores. */
   private static final DataStoreService DSC_SERVICE =
         ApplicationContextProvider.getBean(DataStoreService.class);

   /** Metric Registry, for monitoring purposes. */
   private static final MetricRegistry METRIC_REGISTRY =
         ApplicationContextProvider.getBean(MetricRegistry.class);

   /** Global set of all queued downloads, to avoid simultaneous downloads of the same product. */
   private static final Set<DownloadTask> RUNNING_DOWNLOADS = new HashSet<>();

   /** Prefix for all generated datastore by a product synchronizer */
   private static final String DATASTORE_SYNC_PREFIX_NAME = "datastore-sync#";

   /** An {@link ODataClient} configured to query another DHuS OData service. */
   private final ODataClient client;

   /** Parallelised product setter (if CopyProducts is set to true). */
   private final ParallelProductSetter productSetter;

   /** A set of queued downloads managed by this synchroniser. An implementation of SortedSet is
    * required to avoid skipping failing downloads. */
   private final Set<DownloadTask> runningDownloads = new TreeSet<>();

   /** Credentials: username. */
   private final String serviceUser;

   /** Credentials: password. */
   private final String servicePass;

   /** Path to the remote DHuS incoming directory (if accessible). */
   private final String remoteIncoming;

   /** Every new product is added to these collections. */
   private final List<String> targetCollectionName;

   /** OData resource path to a remote source collection: "Collections('a')/.../Collections('z')" */
   private final String sourceCollection;

   /** True if this synchronizer must download a local copy of the product. */
   private final boolean copyProduct;

   /** Custom $filter parameter, to be added to the query URI. */
   private final String filterParam;

   /** GeoSync filter operator. */
   private final JTSGeometryValidator.WKTAdapter validator;

   /** Skip product on error. */
   private final boolean skipOnError;

   /** Last created product's updated time. */
   private GregorianCalendar lastCreated;

   /** Set to true whenever one of the three date fields is modified. */
   private boolean dateChanged = false;

   /** Size of a Page (count of products to retrieve at once). */
   private int pageSize;

   /** Controls whether we are updating LastCreationDate or not. */
   private boolean update_created = true;

   static
   {
      METRIC_REGISTRY.register("prod_sync.global.gauges.queued_downloads",
            (Gauge<Integer>) () -> RUNNING_DOWNLOADS.size());
   }

   /**
    * Creates a new ODataSynchronizer.
    *
    * @param productSynchronizer configuration for this synchronizer.
    *
    * @throws IllegalStateException if the configuration doe not contains the required fields, or those fields are malformed
    * @throws IOException           when the OdataClient fails to contact the server at {@code url}
    * @throws ODataException        when no OData service have been found at the given url
    * @throws NumberFormatException if the value of the `target_collection` configuration field is not a number
    * @throws ParseException        Should not occur as the WKT in geofilter_shape is validated early
    */
   public ODataProductSynchronizer(ProductSynchronizer productSynchronizer)
         throws IOException, ODataException, ParseException
   {
      super(productSynchronizer);

      // Checks if required configuration is set
      String urilit = productSynchronizer.getServiceUrl();
      serviceUser = productSynchronizer.getServiceLogin();
      servicePass = productSynchronizer.getServicePassword();
      if (urilit == null || urilit.isEmpty())
      {
         throw new IllegalStateException("`service_uri` is not set");
      }

      try
      {
         client = new ODataClient(urilit, serviceUser, servicePass);
      }
      catch (URISyntaxException suppressed)
      {
         throw new IllegalStateException("`service_uri` is malformed");
      }

      String dec_name = client.getSchema().getDefaultEntityContainer().getName();
      if (!dec_name.equals(Model.ENTITY_CONTAINER))
      {
         throw new IllegalStateException("`service_uri` does not reference a DHuS odata service");
      }

      XMLGregorianCalendar last_cr = productSynchronizer.getLastCreated();
      if (last_cr != null)
      {
         lastCreated = last_cr.toGregorianCalendar();
      }
      else
      {
         lastCreated = new GregorianCalendar();
         lastCreated.setTimeInMillis(0L);
      }

      pageSize = productSynchronizer.getPageSize();

      String remote_incoming = productSynchronizer.getRemoteIncoming();
      if (remote_incoming != null && !remote_incoming.isEmpty())
      {
         this.remoteIncoming = remote_incoming;
      }
      else
      {
         this.remoteIncoming = null;
      }

      // store target collection name in singleton list
      String targetCollectionString = productSynchronizer.getTargetCollection();
      if (targetCollectionString == null)
      {
         targetCollectionName = Collections.<String>emptyList();
      }
      else
      {
         targetCollectionName = Collections.<String>singletonList(targetCollectionString);
      }

      String filter_param = productSynchronizer.getFilterParam();
      if (filter_param != null && !filter_param.isEmpty())
      {
         filterParam = filter_param;
      }
      else
      {
         filterParam = null;
      }

      String source_collection = productSynchronizer.getSourceCollection();
      if (source_collection != null && !source_collection.isEmpty())
      {
         sourceCollection = source_collection;
      }
      else
      {
         sourceCollection = "";
      }

      Boolean copy_product = productSynchronizer.isCopyProduct();
      this.copyProduct = copy_product != null ? copy_product : false;

      if (this.copyProduct)
      {
         this.productSetter = new ParallelProductSetter("sync-ingestion", this.pageSize, this.pageSize, 0, TimeUnit.SECONDS);
         METRIC_REGISTRY.register("prod_sync.sync" + productSynchronizer.getId() + ".gauges.queued_downloads",
               (Gauge<Integer>) () -> RUNNING_DOWNLOADS.size());
      }
      else
      {
         this.productSetter = null;
      }

      String geofilter_op = productSynchronizer.getGeofilterOp();
      String geofilter_shape = productSynchronizer.getGeofilterShape();
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
               throw new RuntimeException("Invalid geofilter operator");
         }
      }
      else
      {
         validator = null;
      }

      Boolean skipOnErrorObj = productSynchronizer.isSkipOnError();
      this.skipOnError = skipOnErrorObj != null ? skipOnErrorObj : true;
   }

   /** Logs how much time an OData command consumed. */
   private void logODataPerf(String queryPrefix, String query, Map<String, String> queryParam, long deltaTime)
   {
      if (LOGGER.isDebugEnabled())
      {
         StringBuilder line = new StringBuilder("Synchronizer#").append(getId());

         line.append(" Query='").append(queryPrefix).append(query).append("'");

         if (queryParam != null && !queryParam.isEmpty())
         {
            String params = queryParam.entrySet().stream()
                  .<String>map(entry -> entry.getKey() + '=' + entry.getValue())
                  .reduce((a, b) -> a + ", " + b).get();

            line.append(" Params=[").append(params).append(']');
         }

         line.append(" done in ").append(deltaTime).append("ms");

         LOGGER.debug(line);
      }
   }

   /**
    * Gets `pageSize` products from the data source.
    *
    * @param optional_skip   an optional $skip parameter, may be null
    * @param expand_navlinks if `true`, the query will contain: `$expand=Class,Attributes,Products`
    */
   private ODataFeed getPage(Integer optional_skip, boolean expand_navlinks)
         throws ODataException, IOException, InterruptedException
   {
      // Makes the query parameters
      Map<String, String> query_param = new HashMap<>();

      String lup_s = DateFormatter.format(lastCreated);
      // 'GreaterEqual' because of products with the same CreationDate
      String filter = "CreationDate ge " + lup_s;

      // Appends custom $filter parameter
      if (filterParam != null)
      {
         filter += " and (" + filterParam + ")";
      }

      query_param.put("$filter", filter);

      query_param.put("$top", String.valueOf(pageSize));

      query_param.put("$orderby", "CreationDate");

      if (optional_skip != null && optional_skip > 0)
      {
         query_param.put("$skip", optional_skip.toString());
      }

      if (expand_navlinks)
      {
         query_param.put("$expand", "Class,Attributes,Products");
      }

      // Executes the query
      long delta = System.currentTimeMillis();
      String rsrc = "Products";
      if (sourceCollection != null && !sourceCollection.isEmpty())
      {
         if (sourceCollection.endsWith("/"))
         {
            rsrc = sourceCollection + "Products";
         }
         else
         {
            rsrc = sourceCollection + "/Products";
         }
      }
      ODataFeed pdf = client.readFeed(rsrc, query_param);
      logODataPerf("", rsrc, query_param, System.currentTimeMillis() - delta);

      return pdf;
   }

   /** Returns the CreationDate of the given product entry. */
   private Date getCreationDate(ODataEntry entry)
   {
      return ((GregorianCalendar) entry.getProperties().get("CreationDate")).getTime();
   }

   /** Retrieves associated resource location from the given product entry. */
   private String getResourceLocation(ODataEntry entry)
   {
      return (String) entry.getProperties().get("LocalPath");
   }

   /** Checks that the given products validates the geofilter. */
   private boolean validateFootprint(ODataEntry entry)
   {
      if (validator != null)
      {
         String WKTFootprint = "";
         ODataFeed attributesFeed = ODataDeltaFeed.class.cast(entry.getProperties().get("Attributes"));
         for (ODataEntry attributeEntry: attributesFeed.getEntries())
         {
            Map<String, Object> attributeProperties = attributeEntry.getProperties();

            // retrieve or create metadata definition
            String name = (String) attributeProperties.get("Name");

            if ("JTS footprint".equals(name))
            {
               WKTFootprint = (String) attributeProperties.get("Value");
               break;
            }
         }
         if (WKTFootprint == null || WKTFootprint.isEmpty())
         {
            return false;
         }
         try
         {
            if (!validator.validate(WKTFootprint))
            {
               return false;
            }
         }
         catch (ParseException ex)
         {
            return false;
         }
      }
      return true;
   }

   /** Returns `true` if the given product entry already exists in the database. */
   private boolean exists(String uuid)
   {
      return PRODUCT_SERVICE.systemGetProduct(uuid) != null;
   }

   @SuppressWarnings("element-type-mismatch") // Suspicious call to method `contains` due to param not being a DownloadTask
   private boolean isDownloading(String uuid)
   {
      // This trick only works with a TreeSet (ordered), overriding the `equals` method would probably work with other Set implementations
      return this.runningDownloads.contains((Comparable<DownloadTask>) (DownloadTask dlt) -> uuid.compareTo(dlt.product.getUuid()));
   }

   /** Creates and returns a new Product from the given entry. */
   private IngestibleODataProduct entryToProducts(ODataEntry productEntry, boolean getDownloads)
         throws ODataException, IOException, InterruptedException, MissingProductsException
   {
      long delta;
      Map<String, Object> properties = productEntry.getProperties();

      // (`UUID` and `PATH` have unique constraint), PATH references the UUID
      String uuid = (String) properties.get("Id");
      String identifier = (String) properties.get("Name");

      // Makes the product resource path
      String odataProductResource = "Products('" + uuid + "')";
      String origin = getProductMediaURI(odataProductResource);

      // checksum is MD5
      @SuppressWarnings("unchecked")
      Map<String, String> checksum = (Map<String, String>) properties.get("Checksum");
      String checksumAlgorithm = checksum.get(Model.ALGORITHM);
      String checksumValue = checksum.get(Model.VALUE);

      // Retrieves the Product Class if not inlined
      String itemClass = getClassFromEntry(productEntry, odataProductResource);

      // Retrieves Metadata Indexes (aka Attributes on odata) if not inlined
      List<MetadataIndex> metadataIndexList = getMetadataIndexFromEntry(productEntry, odataProductResource, itemClass);

      // Retrieves derived products if not inlined
      ODataFeed derivedProductFeed;
      if (productEntry.containsInlineEntry() && properties.get("Products") != null)
      {
         derivedProductFeed = ODataDeltaFeed.class.cast(properties.get("Products"));
      }
      else
      {
         delta = System.currentTimeMillis();
         derivedProductFeed = client.readFeed(odataProductResource + "/Products", null);
         logODataPerf(odataProductResource, "/Products", null, System.currentTimeMillis() - delta);
      }

      Map<String, ? extends Product> productAndDerived;
      if (getDownloads)
      {
         productAndDerived = getDownloadableProductsFromEntry(derivedProductFeed,
               odataProductResource, origin, checksumValue, identifier);
      }
      // case of sync without copy, make ingestible product from product references
      else
      {
         productAndDerived = getProductReferences(productEntry, derivedProductFeed);
      }

      IngestibleODataProduct ingestibleODataProduct =
            IngestibleODataProduct.fromODataEntry(productEntry, origin, itemClass, metadataIndexList, productAndDerived);

      ingestibleODataProduct.setProperty(ProductConstants.CHECKSUM_PREFIX + "." + checksumAlgorithm, checksumValue);

      return ingestibleODataProduct;
   }

   private Map<String, ProductReference> getProductReferences(ODataEntry productEntry, ODataFeed derivedProductFeed)
   {
      Map<String, ProductReference> productReferences = new HashMap<>();

      // handle unaltered product reference
      ProductReference unalteredProductReference = new ProductReference(getResourceLocation(productEntry));
      unalteredProductReference.setProperty(ProductConstants.DATA_SIZE, (Long) productEntry.getProperties().get("ContentLength"));

      productReferences.put(IngestibleODataProduct.UNALTERED, unalteredProductReference);

      // make derived product references
      for (ODataEntry derivedProductEntry: derivedProductFeed.getEntries())
      {
         String id = (String) derivedProductEntry.getProperties().get("Id");
         String path = (String) derivedProductEntry.getProperties().get("LocalPath");

         // Retrieves the Quicklook
         if (id.equals("Quicklook"))
         {
            ProductReference quicklookReference = new ProductReference(path);
            quicklookReference.setProperty(ProductConstants.DATA_SIZE, (Long) derivedProductEntry.getProperties().get("ContentLength"));
            productReferences.put(IngestibleODataProduct.QUICKLOOK, quicklookReference);
         }

         // Retrieves the Thumbnail
         else if (id.equals("Thumbnail"))
         {
            ProductReference thumbnailReference = new ProductReference(path);
            thumbnailReference.setProperty(ProductConstants.DATA_SIZE, (Long) derivedProductEntry.getProperties().get("ContentLength"));
            productReferences.put(IngestibleODataProduct.THUMBNAIL, thumbnailReference);
         }
      }
      return productReferences;
   }

   private Map<String, DownloadableProduct> getDownloadableProductsFromEntry(ODataFeed derivedProductFeed,
         String odataProductResource, String origin, String checksumValue, String identifier)
         throws IOException, InterruptedException, ODataException
   {
      // create http client to handle downloadable data
      InterruptibleHttpClient httpClient = new InterruptibleHttpClient(new BasicAuthHttpClientProducer());

      // initialize result map
      Map<String, DownloadableProduct> resultDownloadableProducts = new HashMap<>();

      // unaltered physical product
      DownloadableProduct downloadableProduct = new DownloadableProduct(httpClient, DL_TRIES, origin, checksumValue, identifier);
      resultDownloadableProducts.put(IngestibleODataProduct.UNALTERED, downloadableProduct);

      // make downloadable derived products
      for (ODataEntry derivedProductEntry: derivedProductFeed.getEntries())
      {
         String id = (String) derivedProductEntry.getProperties().get("Id");
         String path = client.getServiceRoot() + '/' + odataProductResource
               + "/Products('" + derivedProductEntry.getProperties().get("Id") + "')/$value";

         // Retrieves the Quicklook
         if (id.equals("Quicklook"))
         {
            DownloadableProduct quicklook = new DownloadableProduct(httpClient, DL_TRIES, path, null, "quicklook");
            resultDownloadableProducts.put(IngestibleODataProduct.QUICKLOOK, quicklook);
         }

         // Retrieves the Thumbnail
         else if (id.equals("Thumbnail"))
         {
            DownloadableProduct thumbnail = new DownloadableProduct(httpClient, DL_TRIES, path, null, "thumbnail");
            resultDownloadableProducts.put(IngestibleODataProduct.THUMBNAIL, thumbnail);
         }
      }

      return resultDownloadableProducts;
   }

   private List<MetadataIndex> getMetadataIndexFromEntry(ODataEntry productEntry, String odataProductResource, String productClass)
         throws IOException, ODataException, InterruptedException
   {
      long delta;
      ODataFeed attributesFeed;
      Map<String, Object> properties = productEntry.getProperties();
      if (productEntry.containsInlineEntry() && properties.get("Attributes") != null)
      {
         attributesFeed = ODataDeltaFeed.class.cast(properties.get("Attributes"));
      }
      else
      {
         delta = System.currentTimeMillis();
         attributesFeed = client.readFeed(odataProductResource + "/Attributes", null);
         logODataPerf(odataProductResource, "/Attributes", null, System.currentTimeMillis() - delta);
      }

      List<MetadataIndex> metadataIndexList = new ArrayList<>(attributesFeed.getEntries().size());
      for (ODataEntry attributeEntry: attributesFeed.getEntries())
      {
         Map<String, Object> attributeProperties = attributeEntry.getProperties();

         // retrieve or create metadata definition
         String name = (String) attributeProperties.get("Name");
         String type = (String) attributeProperties.get("ContentType");
         String queryable = null;
         String category;

         MetadataType mt = METADATA_TYPE_SERVICE.getMetadataTypeByName(productClass, name);
         if (mt != null)
         {
            category = mt.getCategory();
            if (mt.getSolrField() != null)
            {
               queryable = mt.getSolrField().getName();
            }
         }
         else if (name.equals("Identifier"))
         {
            category = "summary";
            queryable = "identifier";
         }
         else if (name.equals("Ingestion Date"))
         {
            category = "product";
            queryable = "ingestionDate";
         }
         else
         {
            category = "";
         }

         // generate metadata
         metadataIndexList.add(MetadataFactory.createMetadataIndex(
               name, type, category, queryable, (String) attributeProperties.get("Value")));
      }
      return metadataIndexList;
   }

   private String getProductMediaURI(String odataProductResource)
   {
      return client.getServiceRoot() + '/' + odataProductResource + "/$value";
   }

   private String getClassFromEntry(ODataEntry productEntry, String odataProductRessource)
         throws IOException, ODataException, InterruptedException
   {
      long delta;
      Map<String, Object> properties = productEntry.getProperties();
      ODataEntry classEntry;
      if (productEntry.containsInlineEntry() && properties.get("Class") != null)
      {
         classEntry = ODataEntry.class.cast(properties.get("Class"));
      }
      else
      {
         delta = System.currentTimeMillis();
         classEntry = client.readEntry(odataProductRessource + "/Class", null);
         logODataPerf(odataProductRessource, "/Class", null, System.currentTimeMillis() - delta);
      }
      Map<String, Object> pdt_class_pm = classEntry.getProperties();
      String pdt_class = String.class.cast(pdt_class_pm.get("Uri"));
      return pdt_class;
   }

   /** Report metrics on product timeliness. */
   private void reportTimelinessMetrics(IngestibleODataProduct product)
   {
      long now = System.currentTimeMillis();
      Date creaDate = Date.class.cast(product.getProperty(ProductConstants.CREATION_DATE));
      METRIC_REGISTRY.histogram("prod_sync.sync" + getId() + ".timeliness.creation")
            .update(now - creaDate.getTime());
      METRIC_REGISTRY.histogram("prod_sync.sync" + getId() + ".timeliness.ingestion")
            .update(now - product.getIngestionDate().getTime());
   }

   /**
    * Handle completed downloads.
    *
    * @return true if there are at least one empty download slot
    * @throws InterruptedException running thread was interrupted
    */
   private boolean checkDownloadTasks() throws InterruptedException
   {
      int count = 0;
      int skip = 0;
      // Get download results from Futures, and create product entries in DB, Solr
      Iterator<DownloadTask> itdl = this.runningDownloads.iterator();
      while (itdl.hasNext())
      {
         DownloadTask dltask = itdl.next();
         if (!dltask.isDone())
         {
            LOGGER.debug("Synchronizer#{} Download of product {} in progress", getId(), dltask.product.getIdentifier());
            update_created = false;
            continue;
         }
         IngestibleODataProduct product = dltask.product;
         String metricPrefix = String.format("prod_sync.sync%d.counters.%s.",
               getId(), Utils.ItemClass.toMetricNamePart(product.getItemClass(), null));
         Long size = ProcessingManager.getVolume(product);
         try
         {
            dltask.productDownload.get();
            RUNNING_DOWNLOADS.remove(dltask);
            itdl.remove();
            count++;
            if (update_created)
            {
               this.lastCreated.setTime(product.getCreationDate());
               this.dateChanged = true;
            }
            METRIC_REGISTRY.counter(metricPrefix + "success").inc();
            if (size != null)
            {
               METRIC_REGISTRY.counter(metricPrefix + "volume").inc(size);
            }

            reportTimelinessMetrics(product);

            LOGGER.info("Synchronizer#{} Product {} ({} bytes compressed) successfully synchronized from {}",
                  getId(), product.getIdentifier(), product.getProperty(ProductConstants.DATA_SIZE), this.client.getServiceRoot());
         }
         catch (ExecutionException ex)
         {
            if (this.skipOnError)
            {
               // force lsatCreated update in case of skip
               if (update_created)
               {
                  this.lastCreated.setTime(product.getCreationDate());
                  this.dateChanged = true;
               }
               LOGGER.warn("Synchronizer#{} PRODUCT SKIPPED from '{}', creationDate: {}",
                     getId(), product.getOrigin(), product.getCreationDate());
               skip++;
            }
            else
            {
               update_created = false;
            }
            LOGGER.error("Synchronizer#{} Product {} failed to download", getId(), product.getIdentifier(), ex);
            RUNNING_DOWNLOADS.remove(dltask);
            METRIC_REGISTRY.counter(metricPrefix + "failure").inc();
            itdl.remove();
         }
         catch (CancellationException ex)
         {
            LOGGER.debug("Synchronizer#{} download of Product {} cancelled", getId(), product.getIdentifier());
            update_created = false;
            RUNNING_DOWNLOADS.remove(dltask);
            itdl.remove();
         }
      }
      if (count > 0 || skip > 0)
      {
         LOGGER.info("Synchronizer#{} {} new Products copied and {} Products skipped", getId(), count, skip);
      }
      return runningDownloads.size() < this.pageSize;
   }

   /** Returns TRUE if download of given product is already queued. */
   private boolean isAlreadyQueued(IngestibleODataProduct product)
   {
      // Uses the UUID to identify products
      return RUNNING_DOWNLOADS.contains(new DownloadTask(product, null));
   }

   /** Insert given product in the download queue. */
   private void queue(IngestibleODataProduct product) throws IOException, InterruptedException
   {
      Future<?> result = productSetter.submitProduct(STORE_SERVICE, product, targetCollectionName, false);
      DownloadTask downloadTask = new DownloadTask(product, result);

      RUNNING_DOWNLOADS.add(downloadTask); // To avoid simultaneaous download of same product
      this.runningDownloads.add(downloadTask); // For ordered processing
   }

   /** Retrieves and download new products, downloads are parallelized. */
   private int getAndCopyNewProduct() throws InterruptedException
   {
      int res = 0;
      int count = this.pageSize - runningDownloads.size();
      int skip = 0;
      Date updatedLastCreationDate = this.lastCreated.getTime();
      boolean updateLastCreated = true;

      // Downloads are done asynchronously in another threads
      try
      {
         // Downloads at least `pageSize` products
         while (count > 0)
         {
            ODataFeed pdf = getPage(skip, true);
            this.update_created = true;
            if (pdf.getEntries().isEmpty()) // No more products
            {
               break;
            }

            skip += pdf.getEntries().size();

            for (ODataEntry pdt: pdf.getEntries())
            {
               String uuid = (String) pdt.getProperties().get("Id");
               if (exists(uuid))
               {
                  LOGGER.info("Synchronizer#{} Product of UUID {} already exists, skipping", getId(), uuid);
                  if (updateLastCreated && !isDownloading(uuid))
                  {
                     updatedLastCreationDate = getCreationDate(pdt);
                     this.dateChanged = true;
                     if (LOGGER.isDebugEnabled())
                     {
                        LOGGER.debug("Synchronizer#{} Product {} safely skipped, new LCD={}",
                              getId(), uuid, DateFormatter.format(updatedLastCreationDate));
                     }
                  }
                  continue;
               }
               if (!ProductSynchronizerUtils.isOnline(pdt))
               {
                  LOGGER.debug("Synchronizer#{} Product {} is not online", getId(), pdt.getProperties().get("Name"));
                  if (updateLastCreated)
                  {
                     updatedLastCreationDate = getCreationDate(pdt);
                     this.dateChanged = true;
                  }
                  continue;
               }

               if (!validateFootprint(pdt))
               {
                  LOGGER.info("Synchronizer#{} Product of UUID {} does not match the geographic filter",
                        getId(), (String) pdt.getProperties().get("Id"));
                  if (updateLastCreated)
                  {
                     updatedLastCreationDate = getCreationDate(pdt);
                     this.dateChanged = true;
                  }
                  continue;
               }
               updateLastCreated = false;

               IngestibleODataProduct product = entryToProducts(pdt, true);

               // Avoid downloading of the same product several times
               // and run post-filters of the product to download
               if (isAlreadyQueued(product))
               {
                  continue;
               }
               count--;
               res++;

               queue(product);
               LOGGER.info("Synchronizer#{} added product {} to the queue ", getId(), product.getIdentifier());
            }
         }
         this.lastCreated.setTime(updatedLastCreationDate);
         LOGGER.info("Synchronizer#{} queued products: ", getId());

         for (DownloadTask rd: this.runningDownloads)
         {
            LOGGER.info("Product name, uuid : {}, {}", rd.product.getIdentifier(), rd.product.getUuid());
         }
      }
      catch (IOException | ODataException | MissingProductsException ex)
      {
         LOGGER.error("Synchronizer#{} OData failure", getId(), ex);
      }
      finally
      {
         LOGGER.info("Synchronizer#{} {} new download tasks submitted", getId(), res);
      }
      return res;
   }

   /**
    * Retrieve new/updated products.
    *
    * @return how many products have been retrieved
    */
   private int getNewProducts() throws InterruptedException
   {
      int res = 0;
      int skip = 0;
      Counter successCounter = METRIC_REGISTRY.counter("prod_sync.sync" + getId() + ".counters.success");
      Counter failureCounter = METRIC_REGISTRY.counter("prod_sync.sync" + getId() + ".counters.failure");
      try
      {
         ODataFeed pdf = getPage(null, true);

         // For each entry, creates a DataBase Object
         for (ODataEntry pdt: pdf.getEntries())
         {
            String uuid = (String) pdt.getProperties().get("Id");
            if (exists(uuid))
            {
               LOGGER.info("Synchronizer#{} Product of UUID {} already exists", getId(), uuid);
               this.lastCreated.setTime(getCreationDate(pdt));
               this.dateChanged = true;
               continue;
            }
            if (!ProductSynchronizerUtils.isOnline(pdt))
            {
               LOGGER.info("Synchronizer#{} Product of UUID {} is not online", getId(), uuid);
               this.lastCreated.setTime(getCreationDate(pdt));
               this.dateChanged = true;
               continue;
            }

            if (!validateFootprint(pdt))
            {
               LOGGER.info("Synchronizer#{} Product of UUID {} does not match the geographic filter",
                     getId(), (String) pdt.getProperties().get("Id"));
               this.lastCreated.setTime(getCreationDate(pdt));
               this.dateChanged = true;
               continue;
            }

            String product_resource_location = getResourceLocation(pdt);
            if (product_resource_location == null)
            {
               String identifier = (String) pdt.getProperties().get("Name");
               String odataProductResource = "Products('" + uuid + "')";
               String origin = getProductMediaURI(odataProductResource);
               Date creationDate = ((GregorianCalendar) pdt.getProperties ().get("CreationDate")).getTime();
               LOGGER.warn("Synchronizer#{} Cannot synchronize product '{}' (uuid: {}), LocalPath property is null;"
                     + " please check that user '{}' has the Archive Manager role on the backend",
                     getId(), identifier, uuid, serviceUser);
               failureCounter.inc();
               if (this.skipOnError)
               {
                  LOGGER.warn("Synchronizer#{} PRODUCT SKIPPED from '{}', creationDate: {}",
                        getId(), origin, creationDate);
                  skip++;
                  // force lsatCreated update in case of skip
                  this.lastCreated.setTime(getCreationDate(pdt));
                  this.dateChanged = true;
               }
               continue;
            }

            IngestibleODataProduct product = entryToProducts(pdt, false);

            String productUuid = product.getUuid();
            // check product accessibility
            if (!DATA_STORE_MANAGER.canAccess(product_resource_location) && !DATA_STORE_MANAGER.hasProduct(productUuid))
            {
               boolean success = false;
               // If remoteIncoming set, tries to create DS and check it can access product
               if (remoteIncoming != null)
               {
                  String dataStoreName = DATASTORE_SYNC_PREFIX_NAME + String.valueOf(getId());
                  HfsDataStoreConf dataStoreConf;
                  dataStoreConf = new HfsDataStoreConf();
                  dataStoreConf.setName(dataStoreName);
                  dataStoreConf.setRestriction(DataStoreRestriction.REFERENCES_ONLY);
                  dataStoreConf.setPath(remoteIncoming);

                  if (!DSC_SERVICE.dataStoreExists(dataStoreName))
                  {
                     try
                     {
                        DSC_SERVICE.createNamed(dataStoreConf);
                        DataStore liveDataStore = DSC_SERVICE.getDataStoreByName(dataStoreName);
                        if (liveDataStore.canAccess(product_resource_location))
                        {
                           DATA_STORE_MANAGER.add(liveDataStore);
                           success = true;
                        }
                        else
                        {
                           DSC_SERVICE.delete(dataStoreConf);
                        }
                     }
                     catch (UnavailableNameException | InvalidConfigurationException e)
                     {
                        LOGGER.error("Synchronizer#{}", getId(), e);
                     }
                  }
               }

               if (!success)
               {
                  String err = String.format("Product not available in datastores and RemoteIncoming: %s/Products('%s')",
                        client.getServiceRoot(), productUuid);
                  LOGGER.error("Synchronizer#{} {}", getId(), err);
                  failureCounter.inc();
                  if (!this.skipOnError)
                  {
                     throw new SyncException(err);
                  }
                  LOGGER.warn("Synchronizer#{} PRODUCT SKIPPED from '{}', creationDate: {}",
                        getId(), product.getOrigin(), product.getCreationDate());
                  skip++;
                  // force lsatCreated update in case of skip
                  this.lastCreated.setTime(getCreationDate(pdt));
                  this.dateChanged = true;
                  continue;
               }
            }

            // adding product and derived products
            try
            {
               STORE_SERVICE.addProductReference(product, targetCollectionName);
            }
            catch (StoreException e)
            {
               LOGGER.error("Synchronizer#{} Cannot synchronise product '{}'", getId(), productUuid, e);
               failureCounter.inc();
               if (!this.skipOnError)
               {
                  throw new SyncException(e);
               }
               LOGGER.warn("Synchronizer#{} PRODUCT SKIPPED from '{}', creationDate: {}",
                     getId(), product.getOrigin(), product.getCreationDate());
               skip++;
               // force lsatCreated update in case of skip
               this.lastCreated.setTime(getCreationDate(pdt));
               this.dateChanged = true;
               continue;
            }

            LOGGER.info("Synchronizer#{} Product {} ({} bytes compressed) successfully synchronized from {}",
                  getId(), product.getIdentifier(), product.getProperty(ProductConstants.DATA_SIZE), this.client.getServiceRoot());

            res++;
            successCounter.inc();
            this.lastCreated.setTime(getCreationDate(pdt));
            this.dateChanged = true;

            reportTimelinessMetrics(product);

            // Checks if we have to abandon the current pass
            if (Thread.interrupted())
            {
               throw new InterruptedException();
            }
         }
      }
      catch (IOException | ODataException | MissingProductsException ex)
      {
         LOGGER.error("Synchronizer#{} OData failure", getId(), ex);
         throw new SyncException(ex);
      }
      finally
      {
         // Logs a summary of what it has been done
         LOGGER.info("Synchronizer#{} {} new Products and {} Products skipped", getId(), res, skip);
      }

      return res;
   }

   @Override
   public boolean synchronize() throws InterruptedException
   {
      int retrieved = 0;

      LOGGER.info("Synchronizer#{} started", getId());
      try (Timer.Context ctx = METRIC_REGISTRY.timer("prod_sync.sync" + getId() + ".timer").time())
      {
         if (this.copyProduct)
         {
            // synchronization with copy
            if (checkDownloadTasks())
            {
               retrieved = getAndCopyNewProduct();
            }
         }
         else
         {
            // synchronization without copy
            retrieved = getNewProducts();
         }
         if (Thread.interrupted())
         {
            throw new InterruptedException();
         }
      }
      catch (LockAcquisitionException | CannotAcquireLockException e)
      {
         throw new InterruptedException(e.getMessage());
      }
      finally
      {
         // Writes the database only if there is a modification
         if (this.dateChanged)
         {
            ((ProductSynchronizer) this.syncConf).setLastCreated(XmlProvider.getCalendar(this.lastCreated));
            SYNC_SERVICE.saveSynchronizer(this);
            this.dateChanged = false;
         }
      }

      return retrieved > 0;
   }

   @Override
   public void close() throws Exception
   {
      // Dispose of running downloads
      if (this.copyProduct)
      {
         for (DownloadTask task: runningDownloads)
         {
            try
            {
               if (task.product != null)
               {
                  task.product.close();
               }
               task.productDownload.cancel(false);
               task.productDownload.get();
            }
            catch (CancellationException | ExecutionException suppressed) {}

            RUNNING_DOWNLOADS.remove(task);
         }
         this.runningDownloads.clear();
         this.productSetter.shutdownNonBlockingIO();
      }
      // Remove metrics from registry
      METRIC_REGISTRY.removeMatching((name, metric) -> name.startsWith("prod_sync.sync" + getId()));
   }

   @Override
   public String toString()
   {
      return "OData Product Synchronizer on " + syncConf.getServiceUrl();
   }

   /** Creates a client producer that produces HTTP Basic auth aware clients. */
   private class BasicAuthHttpClientProducer implements HttpAsyncClientProducer
   {
      @Override
      public CloseableHttpAsyncClient generateClient()
      {
         CredentialsProvider credsProvider = new BasicCredentialsProvider();
         credsProvider.setCredentials(new AuthScope(AuthScope.ANY),
               new UsernamePasswordCredentials(serviceUser, servicePass));
         RequestConfig rqconf = RequestConfig.custom()
               .setCookieSpec(CookieSpecs.DEFAULT)
               .setSocketTimeout(Timeouts.SOCKET_TIMEOUT)
               .setConnectTimeout(Timeouts.CONNECTION_TIMEOUT)
               .setConnectionRequestTimeout(Timeouts.CONNECTION_REQUEST_TIMEOUT)
               .build();
         CloseableHttpAsyncClient res = HttpAsyncClients.custom()
               .setDefaultCredentialsProvider(credsProvider)
               .setDefaultRequestConfig(rqconf)
               .build();
         res.start();
         return res;
      }
   }

   private static class DownloadTask implements Comparable<DownloadTask>
   {
      /** Not null */
      public final IngestibleODataProduct product;
      /** Not null */
      public final Future<?> productDownload;

      public DownloadTask(IngestibleODataProduct prod, Future<?> pdl)
      {
         this.product = prod;
         this.productDownload = pdl;
      }

      /** Returns true of all Futures are done. */
      public boolean isDone()
      {
         return productDownload.isDone();
      }

      @Override
      public boolean equals(Object obj)
      {
         if (obj == null || !DownloadTask.class.isAssignableFrom(obj.getClass()))
         {
            return false;
         }
         DownloadTask other = DownloadTask.class.cast(obj);
         return other.product.getUuid().equals(this.product.getUuid());
      }

      @Override
      public int hashCode()
      {
         return UUID.fromString(this.product.getUuid()).hashCode();
      }

      @Override
      public int compareTo(DownloadTask o)
      {
         // Ordering by creation date is important because it is the pivot between already synced
         // products and to be synced products. Used to order elements in the `runningDownloads` set.
         int result = product.getCreationDate().compareTo(o.product.getCreationDate());

         // In case of two products have the same creation date.
         if (result == 0)
         {
            result = product.getUuid().compareTo(o.product.getUuid());
         }
         return result;
      }
   }

   /* Format date objects to a valid OData String representation, using a precision of 3 (milliseconds) */
   private static final class DateFormatter
   {
      private static final Facets FACETS = new Facets().setPrecision(3);
      private static final EdmSimpleType DATE_TYPE = EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance();

      /* Param date can be a Date, Long, Calendar or Timestamp object. */
      private static String format(Object date) throws EdmSimpleTypeException
      {
         return DATE_TYPE.valueToString(date, EdmLiteralKind.URI, FACETS);
      }
   }
}
