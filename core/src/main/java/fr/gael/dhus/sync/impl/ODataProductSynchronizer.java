/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2016,2017 GAEL Systems
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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

import fr.gael.dhus.database.object.HfsDataStoreConf;
import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.SynchronizerConf;
import fr.gael.dhus.olingo.ODataClient;
import fr.gael.dhus.olingo.v1.Model;
import fr.gael.dhus.service.CollectionService;
import fr.gael.dhus.service.DataStoreConfService;
import fr.gael.dhus.service.ISynchronizerService;
import fr.gael.dhus.service.MetadataTypeService;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.service.SearchService;
import fr.gael.dhus.service.metadata.MetadataType;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.sync.SyncException;
import fr.gael.dhus.sync.Synchronizer;
import fr.gael.dhus.util.JTSGeometryValidator;
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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataDeltaFeed;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;

import org.apache.solr.client.solrj.SolrServerException;

import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.DataStoreService;
import org.dhus.store.datastore.ParallelProductSetter;
import org.dhus.store.datastore.ProductReference;

import org.hibernate.exception.LockAcquisitionException;

import org.springframework.dao.CannotAcquireLockException;

/**
 * A synchronizer using the OData API of another DHuS.
 */
public class ODataProductSynchronizer extends Synchronizer
{
   /** Log. */
   private static final Logger LOGGER = LogManager.getLogger(ODataProductSynchronizer.class);

   /** Number of download attempts (-1 for infinite, must be at least 1) */
   private static final int DL_TRIES = Integer.getInteger("dhus.sync.download_attempts", 10);

   /** Synchronizer Service, to save the  */
   private static final ISynchronizerService SYNC_SERVICE =
         ApplicationContextProvider.getBean (ISynchronizerService.class);

   /** Product service, to store Products in the database. */
   private static final ProductService PRODUCT_SERVICE =
         ApplicationContextProvider.getBean (ProductService.class);

   /** Metadata Type Service, MetadataIndex name to Queryable. */
   private static final MetadataTypeService METADATA_TYPE_SERVICE =
         ApplicationContextProvider.getBean (MetadataTypeService.class);

   /** Search Service, to add a new product in the index. */
   private static final SearchService SEARCH_SERVICE =
         ApplicationContextProvider.getBean (SearchService.class);

   /** Collection Service, for to add a Product in the configured targetCollection. */
   private static final CollectionService COLLECTION_SERVICE =
         ApplicationContextProvider.getBean (CollectionService.class);

   /** Service that manages DataStores. */
   private static final DataStoreService DATA_STORE_SERVICE =
         ApplicationContextProvider.getBean(DataStoreService.class);

   /** Service to store persistent DataStores. */
   private static final DataStoreConfService DBC_STORE_SERVICE =
         ApplicationContextProvider.getBean(DataStoreConfService.class);

   /** Prefix for all generated datastore by a product synchronizer */
   private static final String DATASTORE_SYNC_PREFIX_NAME = "datastore-sync#";

   /** A set of on-going downloads. */
   private static final Set<DownloadTask> RUNNING_DOWNLOADS = new LinkedHashSet<>();

   /** An {@link ODataClient} configured to query another DHuS OData service. */
   private final ODataClient client;

   /** Parallelised product setter (if CopyProducts is set to true). */
   private final ParallelProductSetter productSetter;

   /** Credentials: username. */
   private final String serviceUser;

   /** Credentials: password. */
   private final String servicePass;

   /** Path to the remote DHuS incoming directory (if accessible). */
   private final String remoteIncoming;

   /** Adds every new product in this collection. */
   private final String targetCollectionUUID;

   /** OData resource path to a remote source collection: "Collections('a')/.../Collections('z')" */
   private final String sourceCollection;

   /** True if this synchronizer must download a local copy of the product. */
   private final boolean copyProduct;

   /** Custom $filter parameter, to be added to the query URI. */
   private final String filterParam;

   /** GeoSync filter operator. */
   private final JTSGeometryValidator.WKTAdapter validator;

   /** Last created product's updated time. */
   private Date lastCreated;

   /** Last updated product's updated time. */
   private Date lastUpdated;

   /** Last deleted product's deletion time. */
   private Date lastDeleted;

   /** Set to true whenever one of the three date fields is modified. */
   private boolean dateChanged = false;

   /** Size of a Page (count of products to retrieve at once). */
   private int pageSize;

   /**
    * Creates a new ODataSynchronizer.
    *
    * @param sc configuration for this synchronizer.
    *
    * @throws IllegalStateException if the configuration doe not contains the
    *    required fields, or those fields are malformed.
    * @throws IOException when the OdataClient fails to contact the server
    *    at {@code url}.
    * @throws ODataException when no OData service have been found at the
    *    given url.
    * @throws NumberFormatException if the value of the `target_collection`
    *    configuration field is not a number.
    * @throws ParseException Should not occur as the WKT in geofilter_shape is validated early.
    */
   public ODataProductSynchronizer (SynchronizerConf sc)
         throws IOException, ODataException, ParseException
   {
      super (sc);

      // Checks if required configuration is set
      String urilit = sc.getConfig ("service_uri");
      serviceUser = sc.getConfig ("service_username");
      servicePass = sc.getConfig ("service_password");
      if (urilit == null || urilit.isEmpty ())
      {
         throw new IllegalStateException ("`service_uri` is not set");
      }

      try
      {
         client = new ODataClient (urilit, serviceUser, servicePass);
      }
      catch (URISyntaxException e)
      {
         throw new IllegalStateException ("`service_uri` is malformed");
      }

      String dec_name = client.getSchema ().getDefaultEntityContainer ().getName ();
      if (!dec_name.equals(Model.ENTITY_CONTAINER))
      {
         throw new IllegalStateException ("`service_uri` does not reference a DHuS odata service");
      }

      String last_cr = sc.getConfig ("last_created");
      if (last_cr != null && !last_cr.isEmpty ())
      {
         lastCreated = new Date (Long.decode (last_cr));
      }
      else
      {
         lastCreated = new Date (0L);
      }

      String last_up = sc.getConfig ("last_updated");
      if (last_up != null && !last_up.isEmpty ())
      {
         lastUpdated = new Date (Long.decode (last_up));
      }
      else
      {
         lastUpdated = new Date (0L);
      }

      String last_del = sc.getConfig ("last_deleted");
      if (last_del != null && !last_del.isEmpty ())
      {
         lastDeleted = new Date (Long.decode (last_del));
      }
      else
      {
         lastDeleted = new Date (0L);
      }

      String page_size = sc.getConfig ("page_size");
      if (page_size != null && !page_size.isEmpty ())
      {
         pageSize = Integer.decode (page_size);
      }
      else
      {
         pageSize = 30; // FIXME get that value from the config?
      }

      String remote_incoming = sc.getConfig("remote_incoming_path");
      if (remote_incoming != null && !remote_incoming.isEmpty())
      {
         this.remoteIncoming = remote_incoming;
      }
      else
      {
         this.remoteIncoming = null;
      }

      String target_collection = sc.getConfig ("target_collection");
      if (target_collection != null && !target_collection.isEmpty ())
      {
         this.targetCollectionUUID = target_collection;
      }
      else
      {
         this.targetCollectionUUID = null;
      }

      String filter_param = sc.getConfig ("filter_param");
      if (filter_param != null && !filter_param.isEmpty ())
      {
         filterParam = filter_param;
      }
      else
      {
         filterParam = null;
      }

      String source_collection = sc.getConfig("source_collection");
      if (source_collection != null && !source_collection.isEmpty())
      {
         sourceCollection = source_collection;
      }
      else
      {
         sourceCollection = "";
      }

      String copy_product = sc.getConfig ("copy_product");
      if (copy_product != null && !copy_product.isEmpty ())
      {
         this.copyProduct = Boolean.parseBoolean (copy_product);
      }
      else
      {
         this.copyProduct = false;
      }

      if (this.copyProduct)
      {
         this.productSetter = new ParallelProductSetter(this.pageSize, this.pageSize, 0, TimeUnit.SECONDS);
      }
      else
      {
         this.productSetter = null;
      }

      String geofilter_op = sc.getConfig("geofilter_op");
      String geofilter_shape = sc.getConfig("geofilter_shape");
      if (geofilter_shape != null && !geofilter_shape.isEmpty() &&
            geofilter_op != null && !geofilter_op.isEmpty())
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
   }

   /** Logs how much time an OData command consumed. */
   private void logODataPerf(String query, long delta_time)
   {
      LOGGER.debug("Synchronizer#{} query({}) done in {}ms", getId(), query, delta_time);
   }

   /**
    * Stores (Downloads) a product and its quicklook and thumbnail,
    * returns 3 Futures, 1st is the product, 2nd is the quicklook and 3rd is the thumbnail.
    * 2nd and 3rd Futures may be null!
    * It also sets the DownloadableType and DownloadableSize of the product, and the Path and Size
    * of the product's quicklook and thumbnail.
    */
   private DownloadTask storeRemoteProduct(Product p) throws InterruptedException
   {
      InterruptibleHttpClient http_client = new InterruptibleHttpClient(new BasicAuthHttpClientProducer());

      DownloadableProduct p_dl, t_dl = null, q_dl = null;

      try {
         String p_md5 = p.getDownload().getChecksums().get("MD5");
         p_dl = new DownloadableProduct(http_client, DL_TRIES, p.getOrigin(), p_md5, p.getIdentifier());

         // Downloads and sets the quicklook and thumbnail (if any)
         if (p.getQuicklookFlag())
         {
            q_dl = new DownloadableProduct(http_client, DL_TRIES, p.getQuicklookPath(), null, "quicklook");
         }
         if (p.getThumbnailFlag())
         {
            t_dl = new DownloadableProduct(http_client, DL_TRIES, p.getThumbnailPath(), null, "thumbnail");
         }
      }
      catch (IOException ex)
      {
         return new DownloadTask(p, new ErroneousFuture(new ExecutionException(ex)));
      }

      DownloadTask res = new DownloadTask(p, productSetter.submit(DATA_STORE_SERVICE, p.getUuid(), p_dl));
      p.setDownloadableType(p_dl.contentType);
      p.setDownloadableSize(p_dl.contentLength);
      if (q_dl != null)
      {
         String uuid = UUID.randomUUID().toString();
         res.quicklookDownload = productSetter.submit(DATA_STORE_SERVICE, uuid, q_dl);
         p.setQuicklookPath(uuid);
         p.setQuicklookSize(q_dl.contentLength);
      }
      if (t_dl != null)
      {
         String uuid = UUID.randomUUID().toString();
         res.thumbnailDownload = productSetter.submit(DATA_STORE_SERVICE, uuid, t_dl);
         p.setThumbnailPath(uuid);
         p.setThumbnailSize(t_dl.contentLength);
      }

      return res;
   }

   /**
    * Gets `pageSize` products from the data source.
    * @param optional_skip an optional $skip parameter, may be null.
    * @param expand_navlinks if `true`, the query will contain: `$expand=Class,Attributes,Products`.
    */
   private ODataFeed getPage(Integer optional_skip, boolean expand_navlinks)
         throws ODataException, IOException, InterruptedException
   {
      // Makes the query parameters
      Map<String, String> query_param = new HashMap<>();

      String lup_s = EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance()
            .valueToString(lastCreated, EdmLiteralKind.URI, null);
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
      logODataPerf("Products", System.currentTimeMillis() - delta);

      return pdf;
   }

   /** Returns the CreationDate of the given product entry. */
   private Date getCreationDate(ODataEntry entry) {
      return ((GregorianCalendar) entry.getProperties().get("CreationDate")).getTime();
   }

   /** Retrieves associated resource location from the given product entry. */
   private String getResourceLocation(ODataEntry entry)
   {
      return (String) entry.getProperties().get("LocalPath");
   }

   /** Returns the JTS footprint from the given product. */
   private String getJTSFootprint(Product p)
   {
      // See what I have to do to get a metadata index (there is ~100 indexes)
      for (MetadataIndex mi: p.getIndexes())
      {
         if (mi.getName().equals("JTS footprint"))
         {
            return mi.getValue();
         }
      }
      return null;
   }

   /** A post filter on Products, returns false is the product must not be saved. */
   private boolean postFilter(Product product)
   {
      if (validator != null)
      {
         String jts_footprint = getJTSFootprint(product);
         if (jts_footprint == null || jts_footprint.isEmpty())
         {
            return false;
         }
         try
         {
            if (!validator.validate(jts_footprint))
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
   private boolean exists(ODataEntry entry)
   {
      String uuid = (String) entry.getProperties().get("Id");
      // FIXME: might not be the same product
      return PRODUCT_SERVICE.systemGetProduct(uuid) != null;
   }

   /** Creates and returns a new Product from the given entry. */
   private Product entryToProducts(ODataEntry entry)
         throws ODataException, IOException, InterruptedException
   {
      long delta;
      Map<String, Object> props = entry.getProperties();

      // (`UUID` and `PATH` have unique constraint), PATH references the UUID
      String uuid = (String) props.get("Id");

      // Makes the product resource path
      String pdt_p = "Products('" + uuid + "')";

      Product product = new Product();
      product.setUuid(uuid);

      // Reads the properties
      product.setIdentifier((String) props.get("Name"));
      product.setIngestionDate(((GregorianCalendar) props.get("IngestionDate")).getTime());
      product.setCreated(((GregorianCalendar) props.get("CreationDate")).getTime());
      product.setFootPrint((String) props.get("ContentGeometry"));
      product.setProcessed(Boolean.TRUE);
      product.setSize((Long) props.get("ContentLength"));

      // Reads the ContentDate complex type
      Map contentDate = (Map) props.get("ContentDate");
      product.setContentStart(((GregorianCalendar) contentDate.get("Start")).getTime());
      product.setContentEnd(((GregorianCalendar) contentDate.get("End")).getTime());

      // Sets the origin to the remote URI
      product.setOrigin(client.getServiceRoot() + '/' + pdt_p + "/$value");

      Map<String, String> checksum = (Map) props.get("Checksum");
      Product.Download d = new Product.Download();
      d.setType((String) props.get("ContentType"));
      d.setChecksums(Collections.singletonMap(checksum.get(Model.ALGORITHM),checksum.get(Model.VALUE)));
      d.setSize((Long) props.get("ContentLength"));
      product.setDownload(d);

      // Retrieves the Product Class if not inlined
      ODataEntry pdt_class_e;
      if (entry.containsInlineEntry() && props.get("Class") != null)
      {
         pdt_class_e = ODataEntry.class.cast(props.get("Class"));
      }
      else
      {
         delta = System.currentTimeMillis();
         pdt_class_e = client.readEntry(pdt_p + "/Class", null);
         logODataPerf(pdt_p + "/Class", System.currentTimeMillis() - delta);
      }
      Map<String, Object> pdt_class_pm = pdt_class_e.getProperties();
      String pdt_class = String.class.cast(pdt_class_pm.get("Uri"));
      product.setItemClass(pdt_class);

      // Retrieves Metadata Indexes (aka Attributes on odata) if not inlined
      ODataFeed mif;
      if (entry.containsInlineEntry() && props.get("Attributes") != null)
      {
         mif = ODataDeltaFeed.class.cast(props.get("Attributes"));
      }
      else
      {
         delta = System.currentTimeMillis();
         mif = client.readFeed(pdt_p + "/Attributes", null);
         logODataPerf(pdt_p + "/Attributes", System.currentTimeMillis() - delta);
      }
      List<MetadataIndex> mi_l = new ArrayList<>(mif.getEntries().size());
      for (ODataEntry mie: mif.getEntries())
      {
         Map<String, Object> mi_pm = mie.getProperties();
         MetadataIndex mi = new MetadataIndex();
         String mi_name = (String) mi_pm.get("Name");
         mi.setName(mi_name);
         mi.setType((String) mi_pm.get("ContentType"));
         mi.setValue((String) mi_pm.get("Value"));
         MetadataType mt = METADATA_TYPE_SERVICE.getMetadataTypeByName(pdt_class, mi_name);
         if (mt != null)
         {
            mi.setCategory(mt.getCategory());
            if (mt.getSolrField() != null)
            {
               mi.setQueryable(mt.getSolrField().getName());
            }
         }
         else if (mi_name.equals("Identifier"))
         {
            mi.setCategory("");
            mi.setQueryable("identifier");
         }
         else if (mi_name.equals("Ingestion Date"))
         {
            mi.setCategory("product");
            mi.setQueryable("ingestionDate");
         }
         else
         {
            mi.setCategory("");
         }
         mi_l.add(mi);
      }
      product.setIndexes(mi_l);

      // Retrieves subProducts if not inlined
      ODataFeed subp;
      if (entry.containsInlineEntry() && props.get("Products") != null)
      {
         subp = ODataDeltaFeed.class.cast(props.get("Products"));
      }
      else
      {
         delta = System.currentTimeMillis();
         subp = client.readFeed(pdt_p + "/Products", null);
         logODataPerf(pdt_p + "/Products", System.currentTimeMillis() - delta);
      }
      for (ODataEntry subpe: subp.getEntries())
      {
         String id = (String) subpe.getProperties().get("Id");
         Long content_len = (Long) subpe.getProperties().get("ContentLength");

         String path = (String) subpe.getProperties().get("LocalPath");
         if (this.copyProduct)
         {
            path = client.getServiceRoot() + '/' + pdt_p +
                   "/Products('" + subpe.getProperties().get("Id") + "')/$value";
         }

         // Retrieves the Quicklook
         if (id.equals("Quicklook"))
         {
            product.setQuicklookSize(content_len);
            product.setQuicklookPath(path);
         }

         // Retrieves the Thumbnail
         else if (id.equals("Thumbnail"))
         {
            product.setThumbnailSize(content_len);
            product.setThumbnailPath(path);
         }
      }

      // `processed` must be set to TRUE
      product.setProcessed(Boolean.TRUE);

      return product;
   }

   private void save(Product product)
   {
      List<MetadataIndex> metadatas = product.getIndexes();

      // Stores `product` in the database
      product = PRODUCT_SERVICE.addProduct(product);
      product.setIndexes(metadatas); // DELME lazy loading not working atm ...

      // Stores `product` in the index
      try
      {
         long delta = System.currentTimeMillis();
         SEARCH_SERVICE.index(product);
         LOGGER.debug("Synchronizer#{} indexed product {} in {}ms",
               getId(), product.getIdentifier(), (System.currentTimeMillis() - delta));
      }
      catch (IOException | SolrServerException ex)
      {
         LOGGER.error("Synchronizer#{} Failed to index product {} in Solr's index",
               getId(), product.getIdentifier(), ex);
         throw new RuntimeException(ex);
      }

      // Sets the target collection both in the DB and Solr
      if (this.targetCollectionUUID != null)
      {
         try
         {
            COLLECTION_SERVICE.systemAddProduct(this.targetCollectionUUID, product.getId(), false);
         }
         catch (RuntimeException ex)
         {
            LOGGER.error("Synchronizer#{} Failed to set collection#{} for product {}",
                  getId(), this.targetCollectionUUID, product.getIdentifier(), ex);
            // Reverting ...
            PRODUCT_SERVICE.systemDeleteProduct(product.getId(), false, null);
            throw ex;
         }
      }
   }

   /** DownloadTask parameter must be done! */
   private void cleanupDownloadTask(DownloadTask dltask)
   {
      try
      {
         DATA_STORE_SERVICE.delete(dltask.product.getUuid());
         if (dltask.quicklookDownload != null)
         {
            DATA_STORE_SERVICE.delete(dltask.product.getQuicklookPath());
         }
         if (dltask.quicklookDownload != null)
         {
            DATA_STORE_SERVICE.delete(dltask.product.getThumbnailPath());
         }
      }
      catch (DataStoreException suppressed) {}
   }

   /**
    * Handle completed downloads.
    * @return true if there are at least one empty download slot.
    * @throws InterruptedException running thread was interrupted
    */
   private boolean checkDownloadTasks() throws InterruptedException
   {
      int count = 0;
      // Get download results from Futures, and create product entries in DB, Solr
      Iterator<DownloadTask> itdl = RUNNING_DOWNLOADS.iterator();
      boolean update_created = true; // Controls whether we are updating LastCreationDate or not
      while (itdl.hasNext())
      {
         DownloadTask dltask = itdl.next();
         if (!dltask.isDone())
         {
            update_created = false;
            continue;
         }

         Product product = dltask.product;
         try
         {
            dltask.productDownload.get();

            if (product.getQuicklookFlag() && dltask.quicklookDownload != null)
            {
               dltask.quicklookDownload.get();
            }

            if (product.getThumbnailFlag() && dltask.thumbnailDownload != null)
            {
               dltask.thumbnailDownload.get();
            }

            Date last_created = product.getCreated();
            save(product);
            itdl.remove();
            count++;
            if (update_created)
            {
               this.lastCreated = last_created;
               this.dateChanged = true;
            }

            LOGGER.info("Synchronizer#{} Product {} ({} bytes compressed) successfully synchronized from {}",
                  getId(), product.getIdentifier(), product.getSize(), this.client.getServiceRoot());
         }
         catch (ExecutionException ex)
         {
            LOGGER.error("Synchronizer#{} Product {} failed to download", getId(), product.getIdentifier(), ex);
            update_created = false;
            cleanupDownloadTask(dltask);
            itdl.remove();
         }
         catch (CancellationException ex)
         {
            LOGGER.debug("Synchronizer#{} download of Product {} cancelled", getId(), product.getIdentifier());
            update_created = false;
            cleanupDownloadTask(dltask);
            itdl.remove();
         }
      }
      if (count > 0)
      {
         LOGGER.info("Synchronizer#{} {} new Products copied", getId(), count);
      }
      return RUNNING_DOWNLOADS.size() < this.pageSize;
   }

   /** Retrieves and download new products, downloads are parallelized. */
   private int getAndCopyNewProduct() throws InterruptedException
   {
      int res = 0;
      int count = this.pageSize - RUNNING_DOWNLOADS.size();
      int skip = 0;

      // Downloads are done asynchronously in another threads
      try
      {
         // Downloads at least `pageSize` products
         while (count > 0)
         {
            ODataFeed pdf = getPage(skip, true);
            if (pdf.getEntries().isEmpty()) // No more products
            {
               break;
            }

            skip += this.pageSize;

            for (ODataEntry pdt: pdf.getEntries ())
            {
               if (exists(pdt))
               {
                  continue;
               }

               Product product = entryToProducts(pdt);

               // Avoid downloading of the same product several times
               // and run post-filters of the product to download
               if (RUNNING_DOWNLOADS.contains(new DownloadTask(product, null)) ||
                     !postFilter(product))
               {
                  continue;
               }

               RUNNING_DOWNLOADS.add(storeRemoteProduct(product));
               count--;
               res++;
            }
         }

      }
      catch (IOException | ODataException ex)
      {
         LOGGER.error ("OData failure", ex);
      }
      finally
      {
         LOGGER.info("Synchronizer#{} {} new download tasks submitted", getId(), res);
      }
      return res;
   }

   /**
    * Retrieve new/updated products.
    * @return how many products have been retrieved.
    */
   private int getNewProducts () throws InterruptedException
   {
      int res = 0;
      try
      {
         ODataFeed pdf = getPage(null, true);

         // For each entry, creates a DataBase Object
         for (ODataEntry pdt: pdf.getEntries ())
         {
            if (exists(pdt))
            {
               this.lastCreated = getCreationDate(pdt);
               this.dateChanged = true;
               continue;
            }

            Product product = entryToProducts(pdt);

            if (!postFilter(product))
            {
               this.lastCreated = getCreationDate(pdt);
               this.dateChanged = true;
               continue;
            }

            String product_uuid = product.getUuid();
            String product_resource_location = getResourceLocation(pdt);
            // check product accessibility
            if (!DATA_STORE_SERVICE.canAccess(product_resource_location))
            {
               boolean success = false;
               // If remoteIncoming set, tries to create DS and check it can access product
               if (remoteIncoming != null)
               {
                  String dataStoreName = DATASTORE_SYNC_PREFIX_NAME + String.valueOf(getId());
                  HfsDataStoreConf conf;
                  conf = new HfsDataStoreConf();
                  conf.setName(dataStoreName);
                  conf.setReadOnly(true);
                  conf.setPath(remoteIncoming);

                  if (!DBC_STORE_SERVICE.dataStoreExists(dataStoreName))
                  {
                     DBC_STORE_SERVICE.create(conf);
                     DataStore riDs = DBC_STORE_SERVICE.getDataStoreByName(dataStoreName);
                     if (riDs.canAccess(product_resource_location))
                     {
                        DATA_STORE_SERVICE.add(riDs);
                        success = true;
                     }
                     else
                     {
                        DBC_STORE_SERVICE.delete(conf);
                     }
                  }
               }

               if (!success)
               {
                  String err = String.format("Product not available in datastores and RemoteIncoming: %s/Products('%s')",
                        client.getServiceRoot(), product_uuid);
                  LOGGER.error(err);
                  throw new SyncException(err);
               }

            }

            // adding product
            try
            {
               if (!DATA_STORE_SERVICE.addProductReference(product_uuid, new ProductReference(product_resource_location)))
               {
                  String err = String.format("Cannot add product reference for '%s'", product_uuid);
                  LOGGER.error(err);
                  throw new DataStoreException(err);
               }

               // Add Quicklook
               if (product.getQuicklookFlag())
               {
                  String uuid = UUID.randomUUID().toString();
                  if (!DATA_STORE_SERVICE.addProductReference(uuid, new ProductReference(product.getQuicklookPath())))
                  {
                     String err = String.format("Cannot add quicklook reference for product '%s'", product_uuid);
                     LOGGER.error(err);
                     throw new DataStoreException(err);
                  }
                  product.setQuicklookPath(uuid);
               }

               // Add Thumbnail
               if (product.getThumbnailFlag())
               {
                  String uuid = UUID.randomUUID().toString();
                  if (!DATA_STORE_SERVICE.addProductReference(uuid, new ProductReference(product.getThumbnailPath())))
                  {
                     String err = String.format("Cannot add thumbnail reference for product '%s'", product_uuid);
                     LOGGER.error(err);
                     throw new DataStoreException(err);
                  }
                  product.setThumbnailPath(uuid);
               }
            }
            catch (DataStoreException e)
            {
               LOGGER.error("Cannot synchronise product '{}'", product_uuid, e);
               throw new SyncException(e);
            }

            Date last_created = product.getCreated();
            save(product);

            this.lastCreated = last_created;
            this.dateChanged = true;

            LOGGER.info("Synchronizer#{} Product {} ({} bytes compressed) successfully synchronized from {}",
                  getId(), product.getIdentifier(), product.getSize(), this.client.getServiceRoot());

            res++;

            // Checks if we have to abandon the current pass
            if (Thread.interrupted ())
            {
               throw new InterruptedException ();
            }
         }
      }
      catch (IOException | ODataException ex)
      {
         LOGGER.error ("OData failure", ex);
         throw new SyncException(ex);
      }
      finally
      {
         // Logs a summary of what it has been done
         LOGGER.info("Synchronizer#{} {} new Products", getId(), res);
      }

      return res;
   }

   /**
    * Retrieves updated products.
    * Not Yet Implemented.
    * @return how many products have been retrieved.
    */
   private int getUpdatedProducts ()
   {
      // NYI
      return 0;
   }

   /**
    * Retrieves deleted products.
    * Not Yet Implemented.
    * @return how many products have been retrieved.
    */
   private int getDeletedProducts ()
   {
      // NYI
      return 0;
   }

   @Override
   public boolean synchronize () throws InterruptedException
   {
      int retrieved = 0, updated = 0, deleted = 0;

      LOGGER.info("Synchronizer#{} started", getId());
      try
      {
         if (this.copyProduct)
         {
            if (checkDownloadTasks())
            {
               retrieved = getAndCopyNewProduct();
            }
         }
         else
         {
            retrieved = getNewProducts();
         }
         if (Thread.interrupted ())
         {
            throw new InterruptedException ();
         }

         updated = getUpdatedProducts ();
         if (Thread.interrupted ())
         {
            throw new InterruptedException ();
         }

         deleted = getDeletedProducts ();
      }
      catch (LockAcquisitionException | CannotAcquireLockException e)
      {
         throw new InterruptedException (e.getMessage ());
      }
      finally
      {
         // Writes the database only if there is a modification
         if (this.dateChanged)
         {
            this.syncConf.setConfig("last_created", String.valueOf(this.lastCreated.getTime()));
            SYNC_SERVICE.saveSynchronizer(this);
            this.dateChanged = false;
         }
      }

      return retrieved < pageSize && updated < pageSize && deleted < pageSize;
   }

   @Override
   public void close() throws Exception
   {
      // Dispose of running downloads
      if (this.copyProduct)
      {
         for (DownloadTask task: RUNNING_DOWNLOADS)
         {
            try
            {
               task.productDownload.cancel(true);
               task.productDownload.get();
            }
            catch (CancellationException | ExecutionException none) {}
            if (task.quicklookDownload != null)
            {
               try
               {
                  task.quicklookDownload.cancel(true);
                  task.quicklookDownload.get();
               }
               catch (CancellationException | ExecutionException none) {}
            }
            if (task.thumbnailDownload != null)
            {
               try
               {
                  task.thumbnailDownload.cancel(true);
                  task.thumbnailDownload.get();
               }
               catch (CancellationException | ExecutionException none) {}
            }
         }
         this.productSetter.shutdownNow();
      }
   }

   @Override
   public String toString ()
   {
      return "OData Product Synchronizer on " + syncConf.getConfig("service_uri");
   }

   /** Creates a client producer that produces HTTP Basic auth aware clients. */
   private class BasicAuthHttpClientProducer implements HttpAsyncClientProducer
   {
      @Override
      public CloseableHttpAsyncClient generateClient ()
      {
         CredentialsProvider credsProvider = new BasicCredentialsProvider();
         credsProvider.setCredentials(new AuthScope (AuthScope.ANY),
                 new UsernamePasswordCredentials(serviceUser, servicePass));
         RequestConfig rqconf = RequestConfig.custom()
               .setCookieSpec(CookieSpecs.DEFAULT)
               .setSocketTimeout(Timeouts.SOCKET_TIMEOUT)
               .setConnectTimeout(Timeouts.CONNECTION_TIMEOUT)
               .setConnectionRequestTimeout(Timeouts.CONNECTION_REQUEST_TIMEOUT)
               .build();
         CloseableHttpAsyncClient res = HttpAsyncClients.custom ()
               .setDefaultCredentialsProvider (credsProvider)
               .setDefaultRequestConfig(rqconf)
               .build ();
         res.start ();
         return res;
      }
   }

   /** Returned by {@link #storeRemoteProduct(fr.gael.dhus.database.object.Product)}. */
   private static class DownloadTask
   {
      /** Not null */
      public Product product;
      /** Not null */
      public Future<?> productDownload;
      /** May be null for e.g.: RAW products */
      public Future<?> quicklookDownload;
      /** May be null for e.g.: RAW products */
      public Future<?> thumbnailDownload;

      public DownloadTask(Product prod, Future<?> pdl)
      {
         this.product = prod;
         this.productDownload = pdl;
         this.quicklookDownload = null;
         this.thumbnailDownload = null;
      }

      /** Returns true of all Futures are done. */
      public boolean isDone()
      {
         return productDownload.isDone() &&
               (quicklookDownload == null || quicklookDownload.isDone()) &&
               (thumbnailDownload == null || thumbnailDownload.isDone());
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
   }

   /** A future for failed tasks. */
   private static class ErroneousFuture implements Future<Object>
   {
      public final ExecutionException ex;

      public ErroneousFuture(final ExecutionException ex)
      {
         this.ex = ex;
      }

      @Override
      public boolean cancel(boolean mayInterruptIfRunning)
      {
         return false;
      }

      @Override
      public boolean isCancelled()
      {
         return true;
      }

      @Override
      public boolean isDone()
      {
         return true;
      }

      @Override
      public Object get() throws InterruptedException, ExecutionException
      {
         throw this.ex;
      }

      @Override
      public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
      {
         throw this.ex;
      }
   }
}
