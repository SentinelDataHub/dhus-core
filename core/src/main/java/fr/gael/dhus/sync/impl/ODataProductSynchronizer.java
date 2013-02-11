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

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
import org.dhus.store.ParallelProductSetter;
import org.dhus.store.StoreException;
import org.dhus.store.StoreService;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreFactory.InvalidConfigurationException;
import org.dhus.store.datastore.DataStoreManager;
import org.dhus.store.datastore.ProductOfflineReference;
import org.dhus.store.datastore.ProductReference;
import org.dhus.store.datastore.config.DataStoreManager.UnavailableNameException;
import org.dhus.store.datastore.config.DataStoreRestriction;
import org.dhus.store.datastore.config.HfsDataStoreConf;
import org.dhus.store.derived.DerivedProductStoreService;
import org.dhus.store.ingestion.IngestibleODataProduct;
import org.dhus.store.ingestion.IngestibleODataProduct.MissingProductsException;
import org.dhus.store.metadatastore.MetadataStoreService;
import org.hibernate.exception.LockAcquisitionException;
import org.quartz.SchedulerException;
import org.springframework.dao.CannotAcquireLockException;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.database.object.config.productsource.ProductSource;
import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer.Sources;
import fr.gael.dhus.database.object.config.synchronizer.Source;
import fr.gael.dhus.factory.MetadataFactory;
import fr.gael.dhus.olingo.ODataClient;
import fr.gael.dhus.olingo.v1.Model;
import fr.gael.dhus.service.DataStoreService;
import fr.gael.dhus.service.IProductSourceService;
import fr.gael.dhus.service.ISynchronizerService;
import fr.gael.dhus.service.MetadataTypeService;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.service.exception.InvokeSynchronizerException;
import fr.gael.dhus.service.metadata.MetadataType;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.sync.ProductSynchronizerUtils;
import fr.gael.dhus.sync.SyncException;
import fr.gael.dhus.sync.Synchronizer;
import fr.gael.dhus.sync.intelligent.ProductSynchronizerScheduler;
import fr.gael.dhus.util.JTSGeometryValidator;
import fr.gael.dhus.util.http.DownloadableProduct;
import fr.gael.dhus.util.http.HttpAsyncClientProducer;
import fr.gael.dhus.util.http.InterruptibleHttpClient;
import fr.gael.dhus.util.http.Timeouts;

/**
 * A synchronizer using the OData API of another DHuS.
 */
public class ODataProductSynchronizer extends Synchronizer
{
   private static final String FAILURE_SUFFIX = ".failure";

   private static final String SUCCESS_SUFFIX = ".success";

   /** Log. */
   private static final Logger LOGGER =
      LogManager.getLogger(ODataProductSynchronizer.class);

   /** Number of download attempts (-1 for infinite, must be at least 1). */
   private static final int DL_TRIES =
      Integer.getInteger("dhus.sync.download_attempts", 10);

   /* Number of turns before process the skipped products */
   private static final int MAX_TURNS_SKIPPED_PRODUCTS =
      Integer.getInteger("dhus.sync.skipped_turns", 10);

   /** Synchronizer Service, for this sync to save its own settings. */
   private static final ISynchronizerService SYNC_SERVICE =
      ApplicationContextProvider.getBean(ISynchronizerService.class);

   /** Product service, to store Products in the database. */
   private static final ProductService PRODUCT_SERVICE =
      ApplicationContextProvider.getBean(ProductService.class);

   /** Metadata Type Service, MetadataIndex name to Queryable. */
   private static final MetadataTypeService METADATA_TYPE_SERVICE =
      ApplicationContextProvider.getBean(MetadataTypeService.class);

   /** Metadata service that manages metadata **/
   private static final MetadataStoreService METADATA_SERVICE =
      ApplicationContextProvider.getBean(MetadataStoreService.class);

   /** Service that manages all Stores. */
   private static final StoreService STORE_SERVICE =
      ApplicationContextProvider.getBean(StoreService.class);

   /** Derived Store service */
   private static final DerivedProductStoreService DERIVED_PRODUCT_STORE_SERVICE =
      ApplicationContextProvider.getBean(DerivedProductStoreService.class);

   /** Service that manages DataStores. */
   private static final DataStoreManager DATA_STORE_MANAGER =
      ApplicationContextProvider.getBean(DataStoreManager.class);

   /** Service to store persistent DataStores. */
   private static final DataStoreService DSC_SERVICE =
      ApplicationContextProvider.getBean(DataStoreService.class);

   /** Metric Registry, for monitoring purposes. */
   private static final MetricRegistry METRIC_REGISTRY =
      ApplicationContextProvider.getBean(MetricRegistry.class);

   /**
    * Global set of all queued downloads, to avoid simultaneous downloads of the
    * same product.
    */
   private static final Set<DownloadTask> RUNNING_DOWNLOADS = new HashSet<>();

   /** Prefix for all generated datastore by a product synchronizer */
   private static final String DATASTORE_SYNC_PREFIX_NAME = "datastore-sync#";

   /** Parallelised product setter (if CopyProducts is set to true). */
   private final ParallelProductSetter productSetter;

   /**
    * A set of queued downloads managed by this synchroniser. An implementation
    * of SortedSet is required to avoid skipping failing downloads.
    */
   private final Set<DownloadTask> runningDownloads = new TreeSet<>();

   /** Every new product is added to these collections. */
   private final List<String> targetCollectionName;

   /** True if this synchronizer must download a local copy of the product. */
   private final boolean copyProduct;

   /** Custom $filter parameter, to be added to the query URI. */
   private final String filterParam;

   /** GeoSync filter operator. */
   private final JTSGeometryValidator.WKTAdapter validator;

   /** Skip product on error. */
   private final boolean skipOnError;

   /** Set to true whenever one of the three date fields is modified. */
   private boolean dateChanged = false;

   /** Size of a Page (count of products to retrieve at once). */
   private int pageSize;

   /** Controls whether we are updating LastCreationDate or not. */
   private boolean update_created = true;

   /** Allows to synchronize offline products. By default, it is disabled */
   private boolean syncOfflineProduct = false;

   /** Number of retries for each skipped product */
   private int retriesSkippedProduct;

   /** Timeout for skipped products */
   private long skippedProductsTimeout;

   private int processSkippedTurns = 0;

   private static Map<String, Integer> skippedProductsMap;
   
   /** Manage Synchronizer Sources. */
   private final IProductSourceService productSourceService;
   
   ProductSynchronizer productSynchronizer;

   ProductSource usedSource;

   Source referencedSource;
   
   private ProductSynchronizerScheduler scheduler;
   
   private boolean rankSourcesInit = false;

   List<Long> productSourceIds;

   Map<Long, Boolean> referencedSources =  new HashMap<>();

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
    * @throws IllegalStateException if the c2730002060000000448284E84onfiguration doe not contains the required fields, or those fields are malformed
    * @throws IOException           when the OdataClient fails to contact the server at {@code url}
    * @throws ODataException        when no OData service have been found at the given url
    * @throws NumberFormatException if the value of the `target_collection` configuration field is not a number
    * @throws ParseException        Should not occur as the WKT in geofilter_shape is validated early
    */
   public ODataProductSynchronizer(ProductSynchronizer productSynchronizer, IProductSourceService sourceService)
         throws IOException, ODataException, ParseException
   {
      super(productSynchronizer);
      this.productSynchronizer = productSynchronizer;

      this.productSourceService = sourceService;
    
      syncOfflineProduct = productSynchronizer.isSyncOfflineProducts();
      skippedProductsMap = new HashMap<String, Integer>();

      pageSize = productSynchronizer.getPageSize();

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

      Boolean copy_product = productSynchronizer.isCopyProduct();
      this.copyProduct = copy_product != null ? copy_product : false;

      if (this.copyProduct)
      {
         this.productSetter = new ParallelProductSetter("sync-ingestion", this.pageSize, this.pageSize, 0, TimeUnit.SECONDS);
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
      setRetriesSkippedProduct(productSynchronizer.getRetriesForSkippedProducts());
      setSkippedProductsTimeout(productSynchronizer.getTimeoutSkippedProducts());
      
      if (productSynchronizer.isCopyProduct() && (!SYNC_SERVICE.isUniqueSource(productSynchronizer)))
      {
         if (productSourceService.getListableSources(productSynchronizer).size() == 0)
         {
            throw new RuntimeException("Cannot start the Synchronizer "+ getId()+ " there are no listable sources");
         }
         else
         {
            try
            {
               scheduler =new ProductSynchronizerScheduler(productSynchronizer, this);
               scheduler.start(productSynchronizer.getId());

               waitForRanking();
            }
            catch (SchedulerException e)
            {
               LOGGER.error("An error occured when traying to start the scheduler", e);
            }
         }
      }
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
    * @param skippedProducts if `true`, we will check only products skipped
    * @throws URISyntaxException 
    */
   private ODataFeed getPage(Integer optional_skip, boolean expand_navlinks, boolean skippedProducts)
         throws ODataException, IOException, InterruptedException, URISyntaxException
   {
      // Makes the query parameters
      Map<String, String> query_param = new HashMap<>();
      String filter = null;

      if (skippedProducts)
      {
         filter = buildQueryFilterForSkippedProducts(optional_skip);
      }
      else
      {
         String lup_s = DateFormatter.format(referencedSource.getLastCreationDate().toGregorianCalendar());
         // 'GreaterEqual' because of products with the same CreationDate
         filter = "CreationDate ge " + lup_s;

         // Appends custom $filter parameter
         if (filterParam != null)
         {
            filter += " and (" + filterParam + ")";
         }
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
      String sourceCollection = referencedSource.getSourceCollection();
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
      ODataClient client = new ODataClient(usedSource.getUrl(), usedSource.getLogin(), usedSource.getPassword());
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
   
   /**
    * Creates and returns a new Product from the given entry. 
    * @param productEntry
    * @param getDownloads
    * @param online
    * @return
    * @throws ODataException
    * @throws IOException
    * @throws InterruptedException
    * @throws MissingProductsException
    * @throws URISyntaxException 
    */
   private IngestibleODataProduct entryToProducts(ODataEntry productEntry, boolean getDownloads, boolean onlineProduct)
         throws ODataException, IOException, InterruptedException, MissingProductsException, URISyntaxException
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
         
         ODataClient client = new ODataClient(usedSource.getUrl(), usedSource.getLogin(), usedSource.getPassword());
         derivedProductFeed = client.readFeed(odataProductResource + "/Products", null);
         logODataPerf(odataProductResource, "/Products", null, System.currentTimeMillis() - delta);
      }

      //Perform specific behavior when the product is offline
      Map<String, ? extends Product> productAndDerived;
      if (onlineProduct)
      {
         productAndDerived = processOnlineProducts(productEntry, derivedProductFeed, odataProductResource, origin, checksumValue, identifier, getDownloads);
      }
      else
      {
         productAndDerived = performOfflineProducts(productEntry, derivedProductFeed);
      }

      IngestibleODataProduct ingestibleODataProduct =
            IngestibleODataProduct.fromODataEntry(productEntry, origin, itemClass, metadataIndexList, productAndDerived, referencedSource.getReferenceId());

      ingestibleODataProduct.setProperty(ProductConstants.CHECKSUM_PREFIX + "." + checksumAlgorithm, checksumValue);

      return ingestibleODataProduct;
   }

   /** 
    * @throws ODataException 
    * @throws InterruptedException 
    * @throws IOException 
    * @throws URISyntaxException */
   private Map<String, ? extends Product> processOnlineProducts(ODataEntry productEntry, ODataFeed derivedProductFeed, String odataProductResource,
         String origin,  String checksumValue , String identifier,boolean getDownloads) throws IOException, InterruptedException, ODataException, URISyntaxException
        
   {
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
      return productAndDerived;
   }
   
   private Map<String, ? extends Product> performOfflineProducts(ODataEntry productEntry, ODataFeed derivedProductFeed)
   {
      Map<String, ProductOfflineReference> productReferences = new HashMap<>();

      // handle unaltered product reference
      ProductOfflineReference unalteredProductReference = new ProductOfflineReference(getResourceLocation(productEntry));
      unalteredProductReference.setProperty(ProductConstants.DATA_SIZE, (Long) productEntry.getProperties().get("ContentLength"));
      productReferences.put(IngestibleODataProduct.UNALTERED, unalteredProductReference);

      // make derived product references
      for (ODataEntry derivedProductEntry: derivedProductFeed.getEntries())
      {
         String id = (String) derivedProductEntry.getProperties().get("Id");
         String path = (String) derivedProductEntry.getProperties().get("LocalPath");
         
         checkAccess(path);

         // Retrieves the Quicklook
         if (id.equals("Quicklook"))
         {
            ProductOfflineReference quicklookReference = new ProductOfflineReference(path);
            quicklookReference.setProperty(ProductConstants.DATA_SIZE, (Long) derivedProductEntry.getProperties().get("ContentLength"));
            productReferences.put(IngestibleODataProduct.QUICKLOOK, quicklookReference);
         }

         // Retrieves the Thumbnail
         else if (id.equals("Thumbnail"))
         {
            ProductOfflineReference thumbnailReference = new ProductOfflineReference(path);
            thumbnailReference.setProperty(ProductConstants.DATA_SIZE, (Long) derivedProductEntry.getProperties().get("ContentLength"));
            productReferences.put(IngestibleODataProduct.THUMBNAIL, thumbnailReference);
         }
      }
      return productReferences;
   }

   private void checkAccess(String resource_location)
   {
      if (!DATA_STORE_MANAGER.canAccess(resource_location))
      {
         String remoteIncoming = usedSource.getRemoteIncoming();
         // If remoteIncoming set, tries to create DS and check it can access
         // product
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
                  DataStore liveDataStore =
                     DSC_SERVICE.getDataStoreByName(dataStoreName);
                  if (liveDataStore.canAccess(resource_location))
                  {
                     DATA_STORE_MANAGER.add(liveDataStore);
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
      }
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
         throws IOException, InterruptedException, ODataException, URISyntaxException
   {
      // initialize result map
      Map<String, DownloadableProduct> resultDownloadableProducts = new HashMap<>();

      if(SYNC_SERVICE.isUniqueSource(productSynchronizer))
      {
         // create http client to handle downloadable data
         InterruptibleHttpClient httpUniqueClient = new InterruptibleHttpClient(new BasicAuthHttpClientProducer(usedSource.getLogin(),usedSource.getPassword()));
         DownloadableProduct uniqueDownloadableProduct = new DownloadableProduct(httpUniqueClient, DL_TRIES, origin, checksumValue, identifier);
         uniqueDownloadableProduct.setProductSource(usedSource);
         resultDownloadableProducts.put(IngestibleODataProduct.UNALTERED, uniqueDownloadableProduct);
         
         // make downloadable derived products
         for (ODataEntry derivedProductEntry: derivedProductFeed.getEntries())
         {
            String id = (String) derivedProductEntry.getProperties().get("Id");
            ODataClient client = new ODataClient(usedSource.getUrl(), usedSource.getLogin(), usedSource.getPassword());
            String path = client.getServiceRoot() + '/' + odataProductResource
                  + "/Products('" + derivedProductEntry.getProperties().get("Id") + "')/$value";

            // Retrieves the Quicklook
            if (id.equals("Quicklook"))
            {
               DownloadableProduct quicklook = new DownloadableProduct(httpUniqueClient, DL_TRIES, path, null, "quicklook");
               resultDownloadableProducts.put(IngestibleODataProduct.QUICKLOOK, quicklook);
            }

            // Retrieves the Thumbnail
            else if (id.equals("Thumbnail"))
            {
               DownloadableProduct thumbnail = new DownloadableProduct(httpUniqueClient, DL_TRIES, path, null, "thumbnail");
               resultDownloadableProducts.put(IngestibleODataProduct.THUMBNAIL, thumbnail);
            }
         }
      }
      else
      {
         for (int i = 0; i <= productSourceIds.size(); i++)
         {
            long id = productSourceIds.get(i);
            ProductSource bestSource = productSourceService.getProductSource(id);
            ODataClient client = new ODataClient(bestSource.getUrl(), bestSource.getLogin(), bestSource.getPassword());
            String newOrigin = client.getServiceRoot() + '/' + odataProductResource + "/$value";

            InterruptibleHttpClient bestHttpClient = new InterruptibleHttpClient(new BasicAuthHttpClientProducer(bestSource.getLogin(), bestSource.getPassword()));
            HttpResponse headrsp = bestHttpClient.interruptibleHead(newOrigin);

            if (headrsp.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
            {
               LOGGER.warn("Product {} not available in Source {} - Source skipped ", identifier, bestSource.getUrl());
               LOGGER.warn("Change the download source...");
            }
            else
            {             
               // unaltered physical product
               DownloadableProduct bestDownloadableProduct = new DownloadableProduct(bestHttpClient, DL_TRIES, newOrigin, checksumValue, identifier);
               bestDownloadableProduct.setProductSource(bestSource);
               
               //Update last used date for the source
               Sources sources = productSynchronizer.getSources();
               List<Source> listSources = sources.getSource();
               if (listSources != null)
               {
                  for(Source s : listSources)
                  {
                     if (s.getReferenceId() == id) {
                        s.setLastDateSourceUsed(getLastUsedDate());
                        break;
                     }
                  }
                  sources.setSource(listSources);
                  productSynchronizer.setSources(sources);
                  SYNC_SERVICE.saveSynchronizer(this);
               }
               
               resultDownloadableProducts.put(IngestibleODataProduct.UNALTERED, bestDownloadableProduct);

               for (ODataEntry derivedProductEntry: derivedProductFeed.getEntries())
               {
                  String derivedId = (String) derivedProductEntry.getProperties().get("Id");
                  String path = client.getServiceRoot() + '/' + odataProductResource
                        + "/Products('" + derivedProductEntry.getProperties().get("Id") + "')/$value";

                  // Retrieves the Quicklook
                  if (derivedId.equals("Quicklook"))
                  {
                     DownloadableProduct quicklook = new DownloadableProduct(bestHttpClient, DL_TRIES, path, null, "quicklook");
                     resultDownloadableProducts.put(IngestibleODataProduct.QUICKLOOK, quicklook);
                  }

                  // Retrieves the Thumbnail
                  else if (derivedId.equals("Thumbnail"))
                  {
                     DownloadableProduct thumbnail = new DownloadableProduct(bestHttpClient, DL_TRIES, path, null, "thumbnail");
                     resultDownloadableProducts.put(IngestibleODataProduct.THUMBNAIL, thumbnail);
                  }
               }
               break;
            }
         }
      }
      return resultDownloadableProducts;
   }
   
   private XMLGregorianCalendar getLastUsedDate()
   {      
      Timestamp time = Timestamp.from(Instant.now());
      LocalDateTime ldt = time.toLocalDateTime();

      XMLGregorianCalendar cal;
      try
      {
         cal = DatatypeFactory.newInstance().newXMLGregorianCalendar();
         cal.setYear(ldt.getYear());
         cal.setMonth(ldt.getMonthValue());
         cal.setDay(ldt.getDayOfMonth());
         cal.setHour(ldt.getHour());
         cal.setMinute(ldt.getMinute());
         cal.setSecond(ldt.getSecond());
         String nanos = "0." + String.valueOf(ldt.getNano()).substring(0, 3);         
         cal.setFractionalSecond(new BigDecimal(nanos));
         cal.setTimezone(0);
         return cal;
      }
      catch (DatatypeConfigurationException e)
      {
         LOGGER.error("Error while updating LastCreationDate.", e);
      }
      return null;
   }   

   private List<MetadataIndex> getMetadataIndexFromEntry(ODataEntry productEntry, String odataProductResource, String productClass)
         throws IOException, ODataException, InterruptedException, URISyntaxException
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
         ODataClient client = new ODataClient(usedSource.getUrl(), usedSource.getLogin(), usedSource.getPassword());
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
         throws URISyntaxException, IOException, ODataException
   {
      ODataClient client = new ODataClient(usedSource.getUrl(),
            usedSource.getLogin(), usedSource.getPassword());
      return client.getServiceRoot() + '/' + odataProductResource + "/$value";
   }

   private String getClassFromEntry(ODataEntry productEntry, String odataProductRessource)
         throws IOException, ODataException, InterruptedException, URISyntaxException
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
         ODataClient client = new ODataClient(usedSource.getUrl(), usedSource.getLogin(), usedSource.getPassword()); 
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
      String prefix = "prod_sync.sync" + getId() + ".source"+ usedSource.getId()+".timeliness";
      METRIC_REGISTRY.histogram(prefix + ".creation")
            .update(now - product.getCreationDate().getTime());
      METRIC_REGISTRY.histogram(prefix + ".ingestion")
            .update(now - product.getIngestionDate().getTime());
      LOGGER.debug("Timeliness - CreationDate: {} - IngestionDate: {}",  product.getCreationDate(),product.getIngestionDate() );
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
         
         //Report successful metrics in ParallelProductSetter.AddTask         
         IngestibleODataProduct product = dltask.product;
         long sourceId = product.getSourceId();
        
         Source refSource = SYNC_SERVICE.getRefercedSource(productSynchronizer, sourceId);
         String genericMetricPrefix = ProductInformationUtils.parseGeneralMetricNameFromEntry(product);
         String perSyncPerProductCountersMetricPrefix = ProductInformationUtils.parsePerSourceMetricNameFromEntry(product, getId(), usedSource.getId()
               , "counters", true);
         String perSyncCountersMetricPrefix = ProductInformationUtils.parsePerSourceMetricNameFromEntry(product, getId(), usedSource.getId()
               , "counters", false);

         if (product.getBestSource() != null)
         {
            perSyncPerProductCountersMetricPrefix = ProductInformationUtils.parsePerSourceMetricNameFromEntry(product, getId(), product.getBestSource().getId()
                  , "counters", true);
            perSyncCountersMetricPrefix = ProductInformationUtils.parsePerSourceMetricNameFromEntry(product, getId(),  product.getBestSource().getId()
                  , "counters", false);
         }
         try
         { 
            dltask.productDownload.get();
            RUNNING_DOWNLOADS.remove(dltask);
            itdl.remove();
            count++;
            if (update_created)
            {
               XMLGregorianCalendar lastCreationDate = lastCreated(product.getCreationDate());
               if (checkLastCreatedDate(lastCreationDate))
               {
                  refSource.setLastCreationDate(lastCreationDate);
                  this.dateChanged = true;
               }
            }            
            LOGGER.debug("Synchronizer#{} Product {} was successfully synchronized", getId(), product.getIdentifier());  
         }
         catch (ExecutionException ex)
         {
            if (this.skipOnError)
            {
               // force lastCreated update in case of skip
               if (update_created)
               {
                  XMLGregorianCalendar lastCreationDate = lastCreated(product.getCreationDate());
                  if (checkLastCreatedDate(lastCreationDate))
                  {
                     refSource.setLastCreationDate(lastCreationDate);
                     this.dateChanged = true;
                  }
               }
               LOGGER.warn("Synchronizer#{} PRODUCT SKIPPED from '{}', creationDate: {}", getId(), product.getOrigin(), product.getCreationDate());
               skip++;
               if(!skippedProductsMap.containsKey(product.getUuid()))
               {
                  skippedProductsMap.put(product.getUuid(), 0);               
               }
            }
            else
            {
               update_created = false;
            }
            LOGGER.error("Synchronizer#{} Product {} failed to download", getId(), product.getIdentifier(), ex);
            RUNNING_DOWNLOADS.remove(dltask);
            METRIC_REGISTRY.meter(genericMetricPrefix + FAILURE_SUFFIX).mark(1L);
            METRIC_REGISTRY.meter(perSyncPerProductCountersMetricPrefix + FAILURE_SUFFIX).mark(1L);
            METRIC_REGISTRY.meter(perSyncCountersMetricPrefix + FAILURE_SUFFIX).mark(1L);
            itdl.remove();
         }
         catch (CancellationException ex)
         {
            LOGGER.debug("Synchronizer#{} download of Product {} cancelled", getId(), product.getIdentifier());
            update_created = false;
            RUNNING_DOWNLOADS.remove(dltask);
            itdl.remove();
         }
         finally
         {
            // Writes the database only if there is a modification
            if (this.dateChanged)
            {
               ProductSynchronizer.Sources synchronizerSources = productSynchronizer.getSources();
               List<Source> sources = synchronizerSources.getSource();
               for(Source source : sources)
               {
                  if(source.getReferenceId() == refSource.getReferenceId())
                  {
                     source.setLastCreationDate(refSource.getLastCreationDate());
                     synchronizerSources.setSource(sources);
                     productSynchronizer.setSources(synchronizerSources);
                     SYNC_SERVICE.saveSynchronizer(this);
                     this.dateChanged = false;
                     // resetting all sources status to look for products from them
                     for (Source src : sources)
                     { 
                        referencedSources.put(src.getReferenceId(), true);            
                     }
                     break;
                  }
               }
            }
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
      //Add information about synchronizer and source
      ProductSource bestSource = product.getBestSource();
      Future<?> result = null;
      if (bestSource != null)
      {
         LOGGER.debug("Using best source. Source #{} URL {} chosen for product {}", bestSource.getId(), bestSource.getUrl(), product.getIdentifier());
         result = productSetter.submitProduct(STORE_SERVICE, product, targetCollectionName, false
               , getId(), bestSource.getUrl(), bestSource.getId(), METRIC_REGISTRY);
      }
      else
      {
         LOGGER.debug("Using defined source. Source #{} URL {} chosen for product {}", usedSource.getId(), usedSource.getUrl(), product.getIdentifier());
         result = productSetter.submitProduct(STORE_SERVICE, product, targetCollectionName, false
            , getId(), usedSource.getUrl(), usedSource.getId(), METRIC_REGISTRY);
      }
      DownloadTask downloadTask = new DownloadTask(product, result);

      RUNNING_DOWNLOADS.add(downloadTask); // To avoid simultaneaous download of same product
      this.runningDownloads.add(downloadTask); // For ordered processing
   }

   /** Retrieves and download new products, downloads are parallelized. 
    * @throws URISyntaxException */
   private int getAndCopyNewProduct(boolean skippedProducts) throws InterruptedException, URISyntaxException
   {
      int res = 0;
      int count = this.pageSize - runningDownloads.size();
      int skip = 0;
      Date updatedLastCreationDate = referencedSource.getLastCreationDate().toGregorianCalendar().getTime();
      boolean updateLastCreated = true;

      // Downloads are done asynchronously in another threads
      try
      {
         // Downloads at least `pageSize` products
         while (count > 0)
         {
            ODataFeed pdf = getPage(skip, true, skippedProducts);
            this.update_created = true;
            if (pdf.getEntries().isEmpty()) // No more products
            {
               break;
            }

            skip += pdf.getEntries().size();

            for (ODataEntry pdt : pdf.getEntries())
            {
               String uuid = (String) pdt.getProperties().get("Id");

               if (skippedProducts)
               {
                  // Get the number of trials
                  int cpt = skippedProductsMap.get(uuid);
         
                  // In this case, we retry too many times. We remove the product from the map
                  if (cpt >= this.retriesSkippedProduct)
                  {
                     LOGGER.info("Synchronizer#{} Product of UUID {} abandonned - Too many trials ", getId(), uuid);
                     skippedProductsMap.remove(uuid);
                     continue;
                  }

                  // Increment the trials for this product
                  skippedProductsMap.put(uuid, ++cpt);
               }

               if (exists(uuid))
               {
                  LOGGER.info("Synchronizer#{} Product of UUID {} already exists, skipping", getId(), uuid);
                  if (updateLastCreated && !isDownloading(uuid))
                  {
                     updatedLastCreationDate = getCreationDate(pdt);
                     if (LOGGER.isDebugEnabled())
                     {
                        LOGGER.debug("Synchronizer#{} Product {} safely skipped, new LCD={}", getId(), uuid,
                              DateFormatter.format(updatedLastCreationDate));
                     }
                  }
                  removeProductsFromSkippedMap(skippedProducts, uuid);
                  continue;
               }
               if (!ProductSynchronizerUtils.isOnline(pdt))
               {
                  LOGGER.info("Synchronizer#{} Product {} is not online", getId(), pdt.getProperties().get("Name"));
                  if (updateLastCreated)
                  {
                     updatedLastCreationDate = getCreationDate(pdt);
                  }
                  if (syncOfflineProduct)
                  {
                     LOGGER.info("Synchronizer#{} Offline product UUID {} will be synchronized", getId(), uuid);
                     int nbOfflineProductsSync = synchronizeOfflineProducts(pdt);
                     res += nbOfflineProductsSync;
                  }
                  removeProductsFromSkippedMap(skippedProducts, uuid);
                  continue;
               }

               if (!validateFootprint(pdt))
               {
                  LOGGER.info("Synchronizer#{} Product of UUID {} does not match the geographic filter", getId(),
                        (String) pdt.getProperties().get("Id"));
                  if (updateLastCreated)
                  {
                     updatedLastCreationDate = getCreationDate(pdt);
                  }
                  removeProductsFromSkippedMap(skippedProducts, uuid);
                  continue;
               }
               updateLastCreated = false;

               IngestibleODataProduct product = entryToProducts(pdt, true, true);

               // Avoid downloading of the same product several times
               // and run post-filters of the product to download
               if (isAlreadyQueued(product))
               {
                  removeProductsFromSkippedMap(skippedProducts, product.getUuid());
                  continue;
               }
               count--;
               res++;

               queue(product);
               LOGGER.info("Synchronizer#{} added product {} to the queue ", getId(), product.getIdentifier());
            }
         }
         XMLGregorianCalendar lastCreationDate = lastCreated(updatedLastCreationDate);
         if (checkLastCreatedDate(lastCreationDate))
         {
            referencedSource.setLastCreationDate(lastCreationDate);
            this.dateChanged = true;
         }
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

   private void removeProductsFromSkippedMap(boolean skippedProducts, String uuid)
   {
      if (skippedProducts)
      {
         skippedProductsMap.remove(uuid);
      }
   }

   /**
    * Retrieve new/updated products.
    *
    * @return how many products have been retrieved
    */
   private int getNewProducts(boolean skippedProducts) throws InterruptedException
   {
      int res = 0;
      int skip = 0;
      Meter successCounter = METRIC_REGISTRY.meter("prod_sync.global.counters.success");
      Meter failureCounter = METRIC_REGISTRY.meter("prod_sync.global.counters.failure");

      try
      {
         ODataFeed pdf = getPage(null, true, skippedProducts);

         // For each entry, creates a DataBase Object
         for (ODataEntry pdt: pdf.getEntries())
         {
            String uuid = (String) pdt.getProperties().get("Id");
            
            String genericMetricCounterPrefix = parseMetricNameFromEntry(pdt);
            String perProductPerSourceMetricCounterPrefix = parseMetricNameBySourceFromEntry(pdt, true);
            String perSourceMetricCounterPrefix = parseMetricNameBySourceFromEntry(pdt, false);
            //Get the counter in metrics for each product
            Meter successPerType = METRIC_REGISTRY.meter(genericMetricCounterPrefix + SUCCESS_SUFFIX);
            Meter failurePerType = METRIC_REGISTRY.meter(genericMetricCounterPrefix + FAILURE_SUFFIX);
            Meter successPerSource = METRIC_REGISTRY.meter(perProductPerSourceMetricCounterPrefix + SUCCESS_SUFFIX);
            Meter failurePerSource = METRIC_REGISTRY.meter(perProductPerSourceMetricCounterPrefix + FAILURE_SUFFIX);
            Meter successPerSourcePerProduct = METRIC_REGISTRY.meter(perSourceMetricCounterPrefix + SUCCESS_SUFFIX);
            Meter failurePerSourcePerProduct = METRIC_REGISTRY.meter(perSourceMetricCounterPrefix + FAILURE_SUFFIX);
            
            if (skippedProducts)
            {
             //Get the number of trials
               int cpt = skippedProductsMap.get(uuid);
               
               //In this case, we retry too many times. We remove the product from the map
               if (cpt >= this.retriesSkippedProduct)
               {
                  LOGGER.info("Synchronizer#{} Product of UUID {} abandonned - Too many trials ", getId(), uuid);
                  skippedProductsMap.remove(uuid);
                  continue;
               }
               
               //Increment the trials for this product
               skippedProductsMap.put(uuid, ++cpt);
            }
            
            if (exists(uuid))
            {
               LOGGER.info("Synchronizer#{} Product of UUID {} already exists", getId(), uuid);
               if (skippedProducts)
               {
                  //remove the product from skippedMap
                  skippedProductsMap.remove(uuid);
               }
               else
               {
                  XMLGregorianCalendar lastCreationDate = lastCreated(getCreationDate(pdt));
                  if (checkLastCreatedDate(lastCreationDate))
                  {
                     referencedSource.setLastCreationDate(lastCreationDate);
                     this.dateChanged = true;
                  }
               }
               continue;
            }
            //Case product is offline
            if (!ProductSynchronizerUtils.isOnline(pdt))
            {
               LOGGER.debug("Synchronizer#{} Product of UUID {} is not online", getId(), uuid);
               if (!skippedProducts)
               {
                  XMLGregorianCalendar lastCreationDate = lastCreated(getCreationDate(pdt));
                  if (checkLastCreatedDate(lastCreationDate))
                  {
                     referencedSource.setLastCreationDate(lastCreationDate);
                     this.dateChanged = true;
                  }
               }
               if (syncOfflineProduct)
               {
                  LOGGER.debug("Synchronizer#{} Offline product UUID {} will be synchronized", getId(), uuid);
                  int nbOfflineProducstSync = synchronizeOfflineProducts(pdt);
                  res += nbOfflineProducstSync;
               }
               else
               {
                  removeProductsFromSkippedMap(skippedProducts, uuid);
               }
               continue;
            }

            if (!validateFootprint(pdt))
            {
               LOGGER.debug("Synchronizer#{} Product of UUID {} does not match the geographic filter",
                     getId(), (String) pdt.getProperties().get("Id"));
               if (skippedProducts)
               {
                  skippedProductsMap.remove(uuid);
               }
               else
               {
                  XMLGregorianCalendar lastCreationDate = lastCreated(getCreationDate(pdt));
                  if (checkLastCreatedDate(lastCreationDate))
                  {
                     referencedSource.setLastCreationDate(lastCreationDate);
                     this.dateChanged = true;
                  }
               }
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
                     getId(), identifier, uuid, usedSource.getLogin());
               failureCounter.mark();
               failurePerType.mark();
               failurePerSource.mark();
               failurePerSourcePerProduct.mark();
               if (this.skipOnError)
               {
                  LOGGER.warn("Synchronizer#{} PRODUCT SKIPPED from '{}', creationDate: {}",
                        getId(), origin, creationDate);
                  skip++;
                  skippedProductsMap.put(uuid, 0);
                  
                  if (!skippedProducts)
                  {
                     // force lastCreated update in case of skip
                     XMLGregorianCalendar lastCreationDate = lastCreated(getCreationDate(pdt));
                     if (checkLastCreatedDate(lastCreationDate))
                     {
                        referencedSource.setLastCreationDate(lastCreationDate);
                        this.dateChanged = true;
                     }
                  }
               }
               continue;
            }

            IngestibleODataProduct product = entryToProducts(pdt, false, true);

            String productUuid = product.getUuid();
            // check product accessibility
            if (!DATA_STORE_MANAGER.canAccess(product_resource_location) && !DATA_STORE_MANAGER.hasProduct(productUuid))
            {
               boolean success = false;
               // If remoteIncoming set, tries to create DS and check it can access product
               String remoteIncoming = usedSource.getRemoteIncoming();
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
                        usedSource.getUrl(), productUuid);
                  LOGGER.error("Synchronizer#{} {}", getId(), err);
                  failureCounter.mark();
                  failurePerType.mark();
                  failurePerSource.mark();
                  failurePerSourcePerProduct.mark();
                  if (!this.skipOnError)
                  {
                     throw new SyncException(err);
                  }
                  LOGGER.warn("Synchronizer#{} PRODUCT SKIPPED from '{}', creationDate: {}",
                        getId(), product.getOrigin(), product.getCreationDate());
                  skip++;
                  skippedProductsMap.put(productUuid,0);
                  if (!skippedProducts)
                  {
                     // force lastCreated update in case of skip
                     XMLGregorianCalendar lastCreationDate = lastCreated(getCreationDate(pdt));
                     if (checkLastCreatedDate(lastCreationDate))
                     {
                        referencedSource.setLastCreationDate(lastCreationDate);
                        this.dateChanged = true;
                     }
                  }
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
               failureCounter.mark();
               failurePerType.mark();
               failurePerSource.mark();
               failurePerSourcePerProduct.mark();
               if (!this.skipOnError)
               {
                  throw new SyncException(e);
               }
               LOGGER.warn("Synchronizer#{} PRODUCT SKIPPED from '{}', creationDate: {}",
                     getId(), product.getOrigin(), product.getCreationDate());
               skip++;
               skippedProductsMap.put(productUuid, 0);
               if (!skippedProducts)
               {
                  // force lastCreated update in case of skip
                  XMLGregorianCalendar lastCreationDate = lastCreated(getCreationDate(pdt));
                  if (checkLastCreatedDate(lastCreationDate))
                  {
                     referencedSource.setLastCreationDate(lastCreationDate);
                     this.dateChanged = true;
                  }
               }
               continue;
            }

            LOGGER.info("Synchronizer#{} Product {} ({} bytes compressed) successfully synchronized from {}",
                  getId(), product.getIdentifier(), product.getProperty(ProductConstants.DATA_SIZE), usedSource.getUrl());

            res++;
            successCounter.mark();
            successPerType.mark();
            successPerSource.mark();
            successPerSourcePerProduct.mark();
            if (skippedProducts)
            {
               skippedProductsMap.remove(product.getUuid());
            }
            else
            {
               XMLGregorianCalendar lastCreationDate = lastCreated(getCreationDate(pdt));
               if (checkLastCreatedDate(lastCreationDate))
               {
                  referencedSource.setLastCreationDate(lastCreationDate);
                  this.dateChanged = true;
               }
            }

            reportTimelinessMetrics(product);

            // Checks if we have to abandon the current pass
            if (Thread.interrupted())
            {
               throw new InterruptedException();
            }
         }
      }
      catch (IOException | ODataException | MissingProductsException | URISyntaxException ex)
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

   private String parseMetricNameFromEntry(ODataEntry pdt)
   {
      List<MetadataIndex> metadataIndexes = retrieveMetadataIndexesForMetrics(pdt);
      if (metadataIndexes != null)
      {
         return ProductInformationUtils.parseGeneralMetricNameFromEntry(metadataIndexes);
      }
      return "prod_sync.global.counters.";
   }
   
   private String parseMetricNameBySourceFromEntry(ODataEntry pdt, boolean addProductPartName)
   {
      List<MetadataIndex> metadataIndexes = retrieveMetadataIndexesForMetrics(pdt);
      if (metadataIndexes != null)
      {
         return ProductInformationUtils.parsePerSourceMetricNameFromEntry(metadataIndexes, getId(), usedSource.getId(),
               "counters", addProductPartName);
      }

      return "prod_sync.sync" + getId() + ".source.counters.";
   }
   
   private List<MetadataIndex> retrieveMetadataIndexesForMetrics(ODataEntry pdt)
   {
      Map<String, Object> properties = pdt.getProperties();
      String uuid = (String) properties.get("Id");

      // Makes the product resource path
      String odataProductResource = "Products('" + uuid + "')";

      // Retrieves Metadata Indexes (aka Attributes on odata) if not inlined
      try
      {
         // Retrieves the Product Class if not inlined
         String itemClass = getClassFromEntry(pdt, odataProductResource);
         List<MetadataIndex> metadataIndexList = getMetadataIndexFromEntry(pdt, odataProductResource, itemClass);
         return metadataIndexList;
      }
      catch (IOException | ODataException | InterruptedException | URISyntaxException e)
      {
         LOGGER.warn("An exception occured", e);
      }
      return null;
   }
   
   private int synchronizeOfflineProducts(ODataEntry pdt)
   {
      try
      {
         int res = 0;
         IngestibleODataProduct product = entryToProducts(pdt, false, false);
         String productUuid = product.getUuid();
         
       //Get the counter in metrics for each product
         String genericMetricPrefix = ProductInformationUtils.parseGeneralMetricNameFromEntry(product);
         String perSyncPerProductCountersMetricPrefix = ProductInformationUtils.parsePerSourceMetricNameFromEntry(product, getId(), usedSource.getId(), "counters", true);
         Counter successPerType = METRIC_REGISTRY.counter(genericMetricPrefix +SUCCESS_SUFFIX);
         Counter failurePerType = METRIC_REGISTRY.counter(genericMetricPrefix +FAILURE_SUFFIX);
         Counter successPerSource = METRIC_REGISTRY.counter(perSyncPerProductCountersMetricPrefix +SUCCESS_SUFFIX);
         Counter failurePerSource = METRIC_REGISTRY.counter(perSyncPerProductCountersMetricPrefix +FAILURE_SUFFIX);
         Counter successCounter = METRIC_REGISTRY.counter("prod_sync.global.counters.success");
         Counter failureCounter = METRIC_REGISTRY.counter("prod_sync.global.counters.failure");
         try
         {
            // adding product and derived products
            // If there is a problem to access the QL or the thumbnail(resource not available) the process stopped
            // In fact, no information about the product is added (metadata and derived product references)
            DERIVED_PRODUCT_STORE_SERVICE.addDefaultDerivedProductReferences(product);
            METADATA_SERVICE.addProduct(product, targetCollectionName);
         }
         catch (StoreException e)
         {
            LOGGER.error("Synchronizer#{} Cannot synchronise product '{}'", getId(), productUuid, e);
            if (failureCounter != null)
            {
               failureCounter.inc();
            }
            if (failurePerType != null)
            {
               failurePerType.inc();
            }
            if (failurePerSource != null)
            {
               failurePerSource.inc();
            }
            if (!this.skipOnError)
            {
               throw new SyncException(e);
            }
            LOGGER.warn("Synchronizer#{} PRODUCT SKIPPED from '{}', creationDate: {}", getId(), product.getOrigin(),
                  product.getCreationDate());
            skippedProductsMap.put(productUuid, 0);
            // force lastCreated update in case of skip
            XMLGregorianCalendar lastCreationDate = lastCreated(getCreationDate(pdt));
            if (checkLastCreatedDate(lastCreationDate))
            {
               referencedSource.setLastCreationDate(lastCreationDate);
               this.dateChanged = true;
            }
            return res;
         }

         LOGGER.info("Synchronizer#{} Offline Product {} ({} bytes compressed) successfully synchronized from {}",
               getId(), product.getIdentifier(), product.getProperty(ProductConstants.DATA_SIZE),
               usedSource.getUrl());

         res++;
         if (successCounter != null)
         {
            successCounter.inc();
         }
         if(successPerType != null)
         {
            successPerType.inc();
         }
         if (successPerSource != null)
         {
            successPerSource.inc();
         }
         XMLGregorianCalendar lastCreationDate = lastCreated(getCreationDate(pdt));
         if (checkLastCreatedDate(lastCreationDate))
         {
            referencedSource.setLastCreationDate(lastCreationDate);
            this.dateChanged = true;
         }
         reportTimelinessMetrics(product);

         // Checks if we have to abandon the current pass
         if (Thread.interrupted())
         {
            throw new InterruptedException();
         }
         return res;
      }
      catch (ODataException | IOException | InterruptedException
            | MissingProductsException | URISyntaxException ex)
      {
         LOGGER.error("Synchronizer#{} OData failure", getId(), ex);
         throw new SyncException(ex);
      }
   }

   private synchronized void waitForRanking()
   {
      if (!rankSourcesInit)
      {
         try
         {
            this.wait();
         }
         catch (InterruptedException e)
         {
            Thread.currentThread().interrupt();
            LOGGER.error("Thread interrupted", e);
         }
      }
   }

   public synchronized void setRankedSourceIds(List<Long> productSources)
   {
      productSourceIds = productSources;
      rankSourcesInit = true;
      this.notify();
   }

   private void selectListableSource()
   {
      if(SYNC_SERVICE.isUniqueSource(productSynchronizer))
      {
         usedSource = productSourceService.getReferencedSources(productSynchronizer).get(0);
         referencedSource = SYNC_SERVICE.getRefercedSource(productSynchronizer, usedSource.getId());
      }
      else
      {
         referencedSource = null;
         List<Source> sources = productSourceService.getListableSources(productSynchronizer);
         for (Source source : sources)
         {
            Boolean updatedDate = referencedSources.get(source.getReferenceId());
            if (updatedDate == null || updatedDate == true)
            {
               referencedSource = source;
               referencedSources.put(source.getReferenceId(), false);        
               break;
            }            
         }
         if (referencedSource == null)
         {
            for (Source source : sources)
            { 
               referencedSources.put(source.getReferenceId(), true);            
            }
            referencedSource = sources.get(0);
            referencedSources.put(referencedSource.getReferenceId(), false); 
         }

         usedSource = productSourceService.getProductSource(referencedSource.getReferenceId());
         LOGGER.info("The source used to list the products is {} - {}", usedSource.getUrl(), referencedSource.getLastCreationDate());
      }
   }
   
   @Override
   public boolean synchronize() throws InterruptedException
   {
      int retrieved = 0;
      int retrievedSkipped = 0;

      LOGGER.info("Synchronizer#{} started", getId());
      
      try (Timer.Context ctx = METRIC_REGISTRY.timer("prod_sync.sync" + getId() + ".timer").time())
      {
         if (processSkippedTurns >= MAX_TURNS_SKIPPED_PRODUCTS)
         {
            checkSkippedProducts();
         }
         else
         {
            incrementProcessSkippedTurns();
         }

         if (this.copyProduct)
         {
            // synchronization with copy
            if (checkDownloadTasks())
            {
               selectListableSource();
               retrieved = getAndCopyNewProduct(false);
            }
         }
         else
         {
            // synchronization without copy
            selectListableSource();
            retrieved = getNewProducts(false);
         }
         if (Thread.interrupted())
         {
            throw new InterruptedException();
         }
      }
      catch (LockAcquisitionException | CannotAcquireLockException | URISyntaxException e)
      {
         LOGGER.warn("Synchronizer#{} - An exception occured {} Skipped products found", getId(), e);
         throw new InterruptedException(e.getMessage());
      }
      finally
      {
         // Writes the database only if there is a modification
         if (this.dateChanged)
         {
            ProductSynchronizer.Sources synchronizerSources = productSynchronizer.getSources();
            List<Source> sources = synchronizerSources.getSource();
            for(Source source : sources)
            {
               if(source.getReferenceId() == referencedSource.getReferenceId())
               {
                  source.setLastCreationDate(referencedSource.getLastCreationDate());
                  synchronizerSources.setSource(sources);
                  productSynchronizer.setSources(synchronizerSources);
                  SYNC_SERVICE.saveSynchronizer(this);
                  this.dateChanged = false;
                  // resetting all sources status to look for products from them
                  for (Source src : sources)
                  { 
                     referencedSources.put(src.getReferenceId(), true);            
                  }
                  break;
               }
            }
         }
      }
      return retrieved + retrievedSkipped > 0;
   }

   private void checkSkippedProducts() throws InterruptedException
   {
      //First check skipped product before each sync
      if (skippedProductsMap != null && !skippedProductsMap.isEmpty())
      {
         LOGGER.debug("Synchronizer#{} - Execute the thread for skipped products", getId());
         ExecutorService executor = Executors.newSingleThreadExecutor();
         @SuppressWarnings("unchecked")
         Future<String> future = executor.submit(new Callable()
         {
            public String call() throws Exception
            {
               return String.valueOf(processSkippedProducts());
            }
         });
         try
         {
            future.get(this.getSkippedProductsTimeout(), TimeUnit.MILLISECONDS);  //timeout in milliseconds
         }
         catch (TimeoutException | ExecutionException e)
         {
            LOGGER.warn("Synchronizer#{} - skipped products - Exception {} }", getId(), e);
         }
         executor.shutdownNow();
      }
      setProcessSkippedTurns(0);
   }

   private String buildQueryFilterForSkippedProducts(Integer optionalSkip)
   {
      // If we do not have skipped products, no need to continue
      if (skippedProductsMap == null || skippedProductsMap.isEmpty())
      {
         return null;
      }

      StringBuilder filterBuilder = new StringBuilder("(");
      Object[] tabUuid = skippedProductsMap.keySet().toArray();
      int length =  tabUuid.length;

      if (optionalSkip != null && optionalSkip > 0)
      {
         length = optionalSkip;
      }
      
      for (int i=0; i<length; i++)
      {
         if (i == 0)
          {
             filterBuilder.append("Id eq '").append(tabUuid[i].toString()).append("' ");
          }
          else
          {
             filterBuilder.append("or Id eq '").append(tabUuid[i].toString()).append("' ");
          }
      }

      filterBuilder.append(")");
      LOGGER.debug("Synchronizer#{} - Filter for skipped products: {}", getId(), filterBuilder.toString());
      return filterBuilder.toString();
   }
   
   /**
    * Process skipped products which was found during previous launching
    * @return
    * @throws InterruptedException 
    */
   private int processSkippedProducts() throws InterruptedException
   {
      int retrieved = 0;
      try
      {
         if (this.copyProduct)
         {
            // synchronization with copy
            if (checkDownloadTasks())
            {
               retrieved = getAndCopyNewProduct(true);
            }
         }
         else
         {
            // synchronization without copy
            retrieved = getNewProducts(true);
         }
         LOGGER.info("Synchronizer#{} - {} skipped products copied", getId(), retrieved);
      }
      catch (LockAcquisitionException | CannotAcquireLockException | URISyntaxException e)
      {
         throw new InterruptedException(e.getMessage());
      }
//      String metricNamePrefix = parseMetricNameFromEntry
//      METRIC_REGISTRY.counter("prod_sync.counter.sync" + getId() +".skipped.ingested" );
      return retrieved;
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

   public ProductSynchronizerScheduler getScheduler()
   {
      return scheduler;
   }

   @Override
   public String toString()
   {
      return "OData Product Synchronizer on " ;//+ syncConf.getServiceUrl();
   }

   public int getRetriesSkippedProduct()
   {
      return retriesSkippedProduct;
   }

   public void setRetriesSkippedProduct(int retriesSkippedProduct)
   {
      this.retriesSkippedProduct = retriesSkippedProduct;
   }

   public long getSkippedProductsTimeout()
   {
      return skippedProductsTimeout;
   }

   public void setSkippedProductsTimeout(long skippedProductsTimeout)
   {
      this.skippedProductsTimeout = skippedProductsTimeout;
   }
   
   public int getProcessSkippedTurns()
   {
      return processSkippedTurns;
   }

   private void incrementProcessSkippedTurns()
   {
      ++processSkippedTurns;
   }
   
   private void setProcessSkippedTurns(int processSkippedTurn)
   {
      this.processSkippedTurns = processSkippedTurn;
   }

   private XMLGregorianCalendar lastCreated(Date date)
   {
      try
      {
         GregorianCalendar gregorianCalendar = new GregorianCalendar();         
         gregorianCalendar.setTime(date);
         return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
      }
      catch (DatatypeConfigurationException e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
         return null;
      }
   }
   
   private boolean checkLastCreatedDate(XMLGregorianCalendar lastCreated)
   {
      Source source = SYNC_SERVICE.getRefercedSource(productSynchronizer, referencedSource.getReferenceId());
      
      XMLGregorianCalendar configuredDate = source.getLastCreationDate();

      int result = lastCreated.toGregorianCalendar().compareTo(configuredDate.toGregorianCalendar());
      if(result > 0)
      {
         return true;
      }
      return false;
   }

   /** Creates a client producer that produces HTTP Basic auth aware clients. */
   public class BasicAuthHttpClientProducer implements HttpAsyncClientProducer
   {
      private String username;
      private String password;
      
      public BasicAuthHttpClientProducer(String username, String password)
      {
         this.username = username;
         this.password = password;
      }

      @Override
      public CloseableHttpAsyncClient generateClient()
      {
         CredentialsProvider credsProvider = new BasicCredentialsProvider();       
         credsProvider.setCredentials(new AuthScope(AuthScope.ANY),
              new UsernamePasswordCredentials(username, password));
     
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

      public String getUsername()
      {
         return username;
      }

      public String getPassword()
      {
         return password;
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
