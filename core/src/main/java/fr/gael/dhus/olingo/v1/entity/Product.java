/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2014-2020 GAEL Systems
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
package fr.gael.dhus.olingo.v1.entity;

import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.database.object.Order;
import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.datastore.Destination;
import fr.gael.dhus.network.RegulatedInputStream;
import fr.gael.dhus.network.RegulationException;
import fr.gael.dhus.network.TrafficDirection;
import fr.gael.dhus.olingo.Security;
import fr.gael.dhus.olingo.v1.Expander;
import fr.gael.dhus.olingo.v1.ExpectedException;
import fr.gael.dhus.olingo.v1.ExpectedException.InvalidKeyException;
import fr.gael.dhus.olingo.v1.ExpectedException.InvalidMediaException;
import fr.gael.dhus.olingo.v1.ExpectedException.InvalidTargetException;
import fr.gael.dhus.olingo.v1.ExpectedException.MediaRegulationException;
import fr.gael.dhus.olingo.v1.ExpectedException.NotAllowedException;
import fr.gael.dhus.olingo.v1.MediaResponseBuilder;
import fr.gael.dhus.olingo.v1.Model;
import fr.gael.dhus.olingo.v1.entityset.ItemEntitySet;
import fr.gael.dhus.olingo.v1.entityset.NodeEntitySet;
import fr.gael.dhus.olingo.v1.entityset.ProductEntitySet;
import fr.gael.dhus.service.EvictionService;
import fr.gael.dhus.service.OrderService;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.util.DownloadActionRecordListener;
import fr.gael.dhus.util.DownloadStreamCloserListener;
import fr.gael.dhus.util.DrbChildren;
import fr.gael.dhus.util.stream.ListenableStream;

import fr.gael.drb.DrbNode;
import fr.gael.drb.impl.DrbNodeImpl;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.commons.net.io.CopyStreamAdapter;
import org.apache.commons.net.io.CopyStreamListener;

import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.processor.ODataSingleProcessor;
import org.apache.olingo.odata2.api.uri.NavigationSegment;

import org.dhus.store.datastore.async.AsyncDataStoreException;
import org.dhus.ProductConstants;
import org.dhus.metrics.DownloadMetrics;
import org.dhus.store.datastore.async.AsyncProduct;
import org.dhus.store.datastore.openstack.OpenStackProduct;
import org.dhus.store.derived.DerivedProductStore;
import org.dhus.store.derived.DerivedProductStoreService;
import org.dhus.store.StoreException;
import org.dhus.store.StoreService;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.DataStoreProduct;
import org.dhus.store.quota.QuotaException;

/**
 * A OData representation of a DHuS Product.
 */
public class Product extends Node implements Closeable
{
   private static final EvictionService EVICTION_SERVICE =
         ApplicationContextProvider.getBean(EvictionService.class);

   private static final ProductService PRODUCT_SERVICE =
         ApplicationContextProvider.getBean(ProductService.class);

   private static final StoreService STORE_SERVICE =
         ApplicationContextProvider.getBean(StoreService.class);

   private static final DerivedProductStoreService DERIVED_PRODUCT =
         ApplicationContextProvider.getBean(DerivedProductStoreService.class);

   private static final OrderService ORDER_SERVICE =
         ApplicationContextProvider.getBean(OrderService.class);

   private static final Logger LOGGER = LogManager.getLogger();

   private DataStoreProduct physical;
   private Map<String, Product> products;

   protected final fr.gael.dhus.database.object.Product product;
   protected Map<String, Node> nodes;
   protected Map<String, Attribute> attributes;

   public static void delete(String uuid, String cause) throws ODataException
   {
      delete(uuid, cause, false);
   }

   public static void delete(String uuid, String cause, boolean purge) throws ODataException
   {
      if (Security.currentUserHasRole(Role.DATA_MANAGER))
      {
         fr.gael.dhus.database.object.Product p = PRODUCT_SERVICE.systemGetProduct(uuid);
         if (p == null)
         {
            throw new InvalidKeyException(uuid, Product.class.getSimpleName());
         }
         try
         {
            long start = System.currentTimeMillis();
            STORE_SERVICE.deleteProduct(uuid, Destination.TRASH, !purge, cause);

            long totalTime = System.currentTimeMillis() - start;
            // sysma logs
            LOGGER.info("Deletion of product '{}' ({} bytes) successful spent {}ms",
                  p.getIdentifier(),
                  p.getSize(),
                  totalTime);
         }
         catch (StoreException e)
         {
            LOGGER.warn("Cannot delete product {}: {}", uuid, e.getMessage());
         }
      }
      else
      {
         throw new NotAllowedException();
      }
   }

   /**
    * Factory method to create a Product entity from a database entry.
    * Will either call {@link #Product(fr.gael.dhus.database.object.Product)}
    * or {@link #Product(fr.gael.dhus.database.object.Product, DataStoreProduct)}
    * depending on the situation (product is online).
    *
    * @param product a non null instance
    * @return a Product entity
    */
   public static Product generateProduct(fr.gael.dhus.database.object.Product product)
   {
      try
      {
         return new Product(product, STORE_SERVICE.getPhysicalProduct(product.getUuid()));
      }
      catch (DataStoreException e)
      {
         return new Product(product);
      }
   }

   protected Product(fr.gael.dhus.database.object.Product product)
   {
      super(product.getIdentifier() + ".zip");
      this.product = product;
   }

   protected Product(fr.gael.dhus.database.object.Product product, DataStoreProduct data)
   {
      super(data.getName());
      this.product = product;
      this.physical = data;
   }

   /**
    * Retrieve the Class from this product entity.
    *
    * @return the DrbCortex class name.
    * @throws UnsupportedOperationException if the model cannot be computed.
    * @throws NullPointerException          if this product does not related any class.
    */
   @Override
   public fr.gael.dhus.olingo.v1.entity.Class getItemClass()
   {
      return new fr.gael.dhus.olingo.v1.entity.Class(product.getItemClass());
   }

   @Override
   public String getId()
   {
      return product.getUuid();
   }

   @Override
   public String getName()
   {
      return product.getIdentifier();
   }

   @Override
   public String getContentType()
   {
      return fr.gael.dhus.database.object.Product.DEFAULT_CONTENT_TYPE;
   }

   @Override
   public Long getContentLength()
   {
      return product.getSize();
   }

   @Override
   public Integer getChildrenNumber()
   {
      int number = 0;
      if (this.product != null)
      {
         String uuid = this.product.getUuid();
         if (DERIVED_PRODUCT.hasDerivedProduct(uuid, DerivedProductStore.QUICKLOOK_TAG))
         {
            number++;
         }
         if (DERIVED_PRODUCT.hasDerivedProduct(uuid, DerivedProductStore.THUMBNAIL_TAG))
         {
            number++;
         }
      }
      return number;
   }

   @Override
   public Object getValue()
   {
      return null;
   }

   public Date getCreationDate()
   {
      return product.getCreated();
   }

   public Date getIngestionDate()
   {
      return product.getIngestionDate();
   }

   public Date getModificationDate()
   {
      return product.getUpdated();
   }

   public Date getEvictionDate()
   {
      // dynamic date
      return EVICTION_SERVICE.getEvictionDate(product);
   }

   public String getGeometry()
   {
      return product.getFootPrint();
   }

   public Date getContentStart()
   {
      return product.getContentStart();
   }

   public Date getContentEnd()
   {
      return product.getContentEnd();
   }

   public boolean hasChecksum()
   {
      return !(product.getDownload().getChecksums().isEmpty());
   }

   public boolean isOnline()
   {
      return product.isOnline();
   }

   public boolean isOnDemand()
   {
      return product.isOnDemand();
   }

   public String getChecksumAlgorithm()
   {
      if (!(hasChecksum()))
      {
         return null;
      }

      Map<String, String> checksum = product.getDownload().getChecksums();
      String algorithm = "MD5";
      if (checksum.get(algorithm) != null)
      {
         return algorithm;
      }
      return checksum.keySet().iterator().next();
   }

   public String getChecksumValue()
   {
      if (!(hasChecksum()))
      {
         return null;
      }
      return product.getDownload().getChecksums()
            .get(getChecksumAlgorithm());
   }

   /**
    * This product requires system controls (statistics/quotas)
    *
    * @return true is control is required, false otherwise.
    */
   public boolean requiresControl()
   {
      // TODO This method shall be replaced by RABAC mechanism
      return true;
   }

   // Getters
   public Map<String, Product> getProducts()
   {
      if (this.products == null)
      {
         Map<String, Product> products = new LinkedHashMap<>();
         String uuid = this.product.getUuid();

         if (DERIVED_PRODUCT.hasDerivedProduct(uuid, DerivedProductStore.QUICKLOOK_TAG))
         {
            products.put("Quicklook", QuicklookProduct.generateQuickLookProduct(product));
         }

         if (DERIVED_PRODUCT.hasDerivedProduct(uuid, DerivedProductStore.THUMBNAIL_TAG))
         {
            products.put("Thumbnail", ThumbnailProduct.generateThumbnailProduct(product));
         }
         this.products = products;
      }
      return products;
   }

   @Override
   public Map<String, Node> getNodes()
   {
      if (this.nodes == null)
      {
         try
         {
            DataStoreProduct data = getPhysicalProduct();
            if (data.hasImpl(DrbNode.class))
            {
               this.drbNode = data.getImpl(DrbNode.class);
            }

            DrbNode node = this.drbNode;
            if (DrbChildren.shouldODataUseFirstChild(data.getName(), node))
            {
               // skip product zip node or remote odata container
               node = this.drbNode.getFirstChild();
            }

            this.nodes = Collections.singletonMap(node.getName(), new Node(node));
         }
         catch (DataStoreException | NullPointerException e)
         {
            this.nodes = Collections.emptyMap();
         }
      }
      return this.nodes;
   }

   @Override
   public Map<String, Attribute> getAttributes()
   {
      if (this.attributes == null)
      {
         this.attributes = new LinkedHashMap<>();
         for (MetadataIndex index: PRODUCT_SERVICE.getIndexes(this.product.getUuid()))
         {
            Attribute attr = new Attribute(index.getName(), index.getValue(), index.getCategory());
            this.attributes.put(attr.getName(), attr);
         }
      }
      return this.attributes;
   }

   public InputStream getInputStream() throws IOException
   {
      try
      {
         DataStoreProduct data = getPhysicalProduct();
         if (data.hasImpl(InputStream.class))
         {
            return data.getImpl(InputStream.class);
         }
         return null;
      }
      catch (DataStoreException e)
      {
         return null;
      }
   }

   @Override
   public Map<String, Object> toEntityResponse(String root_url)
   {
      // superclass node response is not required. Only Item response is
      // necessary.
      Map<String, Object> res = super.itemToEntityResponse(root_url);

      res.put(NodeEntitySet.CHILDREN_NUMBER, getChildrenNumber());

      LinkedHashMap<String, Date> dates = new LinkedHashMap<String, Date>();
      dates.put(Model.TIME_RANGE_START, getContentStart());
      dates.put(Model.TIME_RANGE_END, getContentEnd());
      res.put(ProductEntitySet.CONTENT_DATE, dates);

      HashMap<String, String> checksum = new LinkedHashMap<String, String>();
      checksum.put(Model.ALGORITHM, getChecksumAlgorithm());
      checksum.put(Model.VALUE, getChecksumValue());
      res.put(ProductEntitySet.CHECKSUM, checksum);

      res.put(ProductEntitySet.CREATION_DATE, getCreationDate());
      res.put(ProductEntitySet.INGESTION_DATE, getIngestionDate());
      res.put(ProductEntitySet.MODIFICATION_DATE, getModificationDate());
      res.put(ProductEntitySet.EVICTION_DATE, getEvictionDate());
      res.put(ProductEntitySet.CONTENT_GEOMETRY, getGeometry());
      res.put(ProductEntitySet.ONLINE, isOnline());
      res.put(ProductEntitySet.ON_DEMAND, isOnDemand());

      try
      {
         res.put(ProductEntitySet.LOCAL_PATH, getPhysicalProduct().getResourceLocation());
      }
      catch (DataStoreException e)
      {
         res.put(ProductEntitySet.LOCAL_PATH, null);
      }

      return res;
   }

   @Override
   public Object getProperty(String prop_name) throws ODataException
   {
      // item and node properties are fetched directly from Product
      // to avoid initializing a DrbNode
      switch (prop_name)
      {
         // item properties
         case ItemEntitySet.ID: return getId();
         case ItemEntitySet.NAME: return getName();
         case ItemEntitySet.CONTENT_TYPE: return getContentType();
         case ItemEntitySet.CONTENT_LENGTH: return getContentLength();

         // node properties
         case NodeEntitySet.CHILDREN_NUMBER: return getChildrenNumber();
         case NodeEntitySet.VALUE: return getValue();

         // product properties
         case ProductEntitySet.CREATION_DATE: return getCreationDate();
         case ProductEntitySet.INGESTION_DATE: return getIngestionDate();
         case ProductEntitySet.MODIFICATION_DATE: return getModificationDate();
         case ProductEntitySet.EVICTION_DATE: return getEvictionDate();
         case ProductEntitySet.CONTENT_GEOMETRY: return getGeometry();
         case ProductEntitySet.ONLINE: return isOnline();
         case ProductEntitySet.ON_DEMAND: return isOnDemand();
         case ProductEntitySet.LOCAL_PATH:
            try
            {
               return getPhysicalProduct().getResourceLocation();
            }
            catch (DataStoreException ex)
            {
               return null;
            }

         // will throw "Property X not found" ODataException
         default: return super.getProperty(prop_name);
      }
   }

   @Override
   public Map<String, Object> getComplexProperty(String prop_name)
         throws ODataException
   {
      if (prop_name.equals(ProductEntitySet.CONTENT_DATE))
      {
         Map<String, Object> values = new HashMap<>();
         values.put(Model.TIME_RANGE_START, getContentStart());
         values.put(Model.TIME_RANGE_END, getContentEnd());
         return values;
      }
      if (prop_name.equals(ProductEntitySet.CHECKSUM))
      {
         Map<String, Object> values = new HashMap<>();
         values.put(Model.ALGORITHM, getChecksumAlgorithm());
         values.put(Model.VALUE, getChecksumValue());
         return values;
      }
      throw new ODataException("Complex property '" + prop_name
            + "' not found.");
   }

   @Override
   public ODataResponse getEntityMedia(ODataSingleProcessor processor, boolean attach_stream)
         throws ODataException
   {
      try
      {
         if (getPhysicalProduct() instanceof AsyncProduct)
         {
            if (attach_stream)
            {
               Order order = AsyncProduct.class.cast(getPhysicalProduct()).asyncFetchData();
               ORDER_SERVICE.addOwner(order, Security.getCurrentUser());
            }
            return MediaResponseBuilder.prepareAsyncMediaResponse();
         }
      }
      catch (AsyncDataStoreException e)
      {
         throw new ExpectedException(e.getUserMessage(), HttpStatusCodes.fromStatusCode(e.getHttpStatusCode()));
      }
      catch (DataStoreException ex)
      {
         Throwable th = ex.getCause();
         if (th != null
               && QuotaException.ParallelFetchResquestQuotaException.class.isAssignableFrom(th.getClass()))
         {
            throw new ExpectedException(th.getMessage(), HttpStatusCodes.FORBIDDEN);
         }
         throw new ExpectedException(ex.getMessage(), HttpStatusCodes.SERVICE_UNAVAILABLE);
      }

      ODataResponse rsp;
      InputStream input = null;
      try
      {
         input = attach_stream? getInputStream(): null;
         User u = Security.getCurrentUser();
         String user_name = (u == null ? null : u.getUsername());
         DownloadMetrics.DownloadActionListener metrics = null;

         long length = getContentLength();
         DataStoreProduct data = getPhysicalProduct();
         // particular case of MOST scenario (product no more available in DHuS format but in uuid.zip format)
         if (data instanceof OpenStackProduct && ((OpenStackProduct)data).getUUID() != null)
         {
            ((OpenStackProduct)data).prepareDownloadInformation();
            length = (Long) data.getProperty(ProductConstants.DATA_SIZE);
         }
         
         InputStream is = null;
         if (attach_stream)
         {
            is = new BufferedInputStream(input);
            if (requiresControl())
            {
               CopyStreamAdapter adapter = new CopyStreamAdapter();
               CopyStreamListener recorder =
                     new DownloadActionRecordListener(product.getUuid(), product.getIdentifier(), u);

               CopyStreamListener closer = new DownloadStreamCloserListener(is);
               adapter.addCopyStreamListener(recorder);
               adapter.addCopyStreamListener(closer);

               RegulatedInputStream.Builder builder =
                     new RegulatedInputStream.Builder(is, TrafficDirection.OUTBOUND);
               builder.userName(user_name);
               builder.copyStreamListener(adapter);
               builder.streamSize(length);

               is = builder.build();
            }
            if (DL_METRICS.isPresent())
            {
               metrics = new DownloadMetrics.DownloadActionListener(DL_METRICS.get(), getItemClass().getUri(), user_name);
               is = new ListenableStream(is, metrics);
            }
         }

         // Computes ETag
         String etag = getChecksumValue();
         if (etag == null)
         {
            etag = getId();
         }
         
         // Prepare the HTTP header for stream transfer.
         rsp = MediaResponseBuilder.prepareMediaResponse(
               etag, getPhysicalProduct().getName(),
               getContentType(),
               getCreationDate().getTime(),
               length,
               processor.getContext(),
               is);
         if (metrics != null)
         {
            DL_METRICS.get().recordDownloadStart(getItemClass().getUri(), user_name);
            String contentLength = rsp.getHeader("Content-Length");
            metrics.setRequestedBytes(contentLength != null ? Long.decode(contentLength) : length);
         }
      }
      // RegulationException must be handled separately as they are
      // user generated errors and not internal problems
      catch (RegulationException e)
      {
         forceClose(input);
         throw new MediaRegulationException(e.getMessage());
      }
      catch (IOException | DataStoreException e)
      {
         forceClose(input);
         throw new InvalidMediaException(e.getMessage());
      }
      return rsp;
   }

   @Override
   public Object navigate(NavigationSegment ns) throws ODataException
   {
      Object res;

      if (ns.getEntitySet().getName().equals(Model.NODE.getName()))
      {
         res = getNodes();
      }
      else if (ns.getEntitySet().getName().equals(Model.ATTRIBUTE.getName()))
      {
         res = getAttributes();
      }
      else if (ns.getEntitySet().getName().equals(Model.CLASS.getName()))
      {
         res = getItemClass();
      }
      else if (ns.getEntitySet().getName().equals(Model.PRODUCT.getName()))
      {
         res = getProducts();
      }
      else
      {
         throw new InvalidTargetException(this.getClass().getSimpleName(), ns.getEntitySet().getName());
      }

      if (!ns.getKeyPredicates().isEmpty())
      {
         res = Map.class.cast(res).get(
               ns.getKeyPredicates().get(0).getLiteral());
      }

      return res;
   }

   @Override
   public void close() throws IOException
   {
      if (this.drbNode == null)
      {
         return;
      }

      if (this.drbNode instanceof DrbNodeImpl)
      {
         DrbNodeImpl.class.cast(this.drbNode).close(true);
      }
   }

   @Override
   public List<String> getExpandableNavLinkNames()
   {
      // Product inherits from Node
      List<String> res = new ArrayList<>(super.getExpandableNavLinkNames());
      res.add("Products");
      res.add("Class");
      res.add("Attributes");
      res.add("Nodes");
      return res;
   }

   @Override
   public List<Map<String, Object>> expand(String navlink_name, String self_url) throws ODataException
   {
      switch (navlink_name)
      {
         case "Products":
            return Expander.mapToData(getProducts(), self_url);
         case "Class":
            return Expander.entityToData(getItemClass(), self_url);
         default:
            return super.expand(navlink_name, self_url);
      }
   }

   protected org.dhus.store.datastore.DataStoreProduct getPhysicalProduct() throws DataStoreException
   {
      if (null == this.physical)
      {
         this.physical = STORE_SERVICE.getPhysicalProduct(product.getUuid());
      }
      return this.physical;
   }

   /**
    * Returns product media filename.
    *
    * @return data filename or null if media is not found
    */
   public String getMediaName()
   {
      try
      {
         return getPhysicalProduct().getName();
      }
      catch (DataStoreException ex)
      {
         return null;
      }
   }
}
