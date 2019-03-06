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
package fr.gael.dhus.sync;

import com.vividsolutions.jts.io.ParseException;

import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.database.object.config.source.Source;
import fr.gael.dhus.database.object.config.synchronizer.SmartProductSynchronizer;
import fr.gael.dhus.factory.MetadataFactory;
import fr.gael.dhus.olingo.v1.Model;
import fr.gael.dhus.service.ISynchronizerService;
import fr.gael.dhus.service.MetadataTypeService;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.service.metadata.MetadataType;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.sync.smart.MultiSourceDownloadableProduct;
import fr.gael.dhus.sync.smart.ODataSmartProductSynchronizer;
import fr.gael.dhus.util.JTSGeometryValidator;
import fr.gael.dhus.util.http.BasicAuthHttpClientProducer;
import fr.gael.dhus.util.http.InterruptibleHttpClient;
import fr.gael.dhus.util.stream.MultiSourceInputStreamFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataDeltaFeed;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;

import org.dhus.ProductConstants;
import org.dhus.store.datastore.ProductReference;
import org.dhus.store.ingestion.IngestibleODataProduct;
import org.dhus.store.ingestion.IngestibleProduct;
import org.springframework.beans.factory.annotation.Autowired;

public class ProductSynchronizerUtils
{
   @Autowired
   private ISynchronizerService synchronizerService;

   @Autowired
   private ProductService productService;

   @Autowired
   private MultiSourceInputStreamFactory multiSourceStreamFactory;

   private MultiSourceDownloadableProduct generateMultiSourceProduct(
         ProductInfoExtractor infoExtractor, SmartProductSynchronizer syncConf, String subProduct)
         throws IngestibleODataProduct.MissingProductsException
   {
      return new MultiSourceDownloadableProduct(
            multiSourceStreamFactory,
            syncConf,
            infoExtractor.uuid(),
            infoExtractor.filename(),
            subProduct,
            infoExtractor.checksumAlgorithm(),
            infoExtractor.checksumValue(),
            infoExtractor.size());
   }

   /** Returns the JTS footprint from the given product. */
   private String getJTSFootprint(IngestibleODataProduct p)
   {
      // See what I have to do to get a metadata index (there is ~100 indexes)
      for (MetadataIndex mi: p.getMetadataIndexes())
      {
         if (mi.getName().equals("JTS footprint"))
         {
            return mi.getValue();
         }
      }
      return null;
   }

   /**
    * Checks if the product is already exist into the DHuS service.
    *
    * @param product product to check
    * @return true if product exists, otherwise false
    */
   public boolean exists(IngestibleProduct product)
   {
      return productService.systemGetProduct(product.getUuid()) != null;
   }

   /**
    * Returns `true` if the given product entry is online.
    *
    * @param entry to check
    * @return the value of the entry's Online property or {@code true} if that property is null
    */
   public static boolean isOnline(ODataEntry entry)
   {
      Boolean online = (Boolean) entry.getProperties().get("Online");
      return online == null || online;
   }

   /**
    * Checks if the product is inner the region of interest.
    *
    * @param product product to check
    * @param checker ROI checker
    * @return true if it is inner the ROI, otherwise false
    */
   public boolean inROI(IngestibleODataProduct product, JTSGeometryValidator.WKTAdapter checker)
   {
      if (checker != null)
      {
         String jts_footprint = getJTSFootprint(product);
         if (jts_footprint == null || jts_footprint.isEmpty())
         {
            return false;
         }
         try
         {
            if (!checker.validate(jts_footprint))
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

   /**
    * Saves the synchronizer configuration into the persistent configuration.
    *
    * @param synchronizer config configuration to save
    */
   public void saveConfiguration(ODataSmartProductSynchronizer synchronizer)
   {
      synchronizerService.saveSynchronizer(synchronizer);
   }

   /**
    * Generates a new {@link IngestibleProduct} from a OData entry following rules defined by a
    * synchronizer to download the product.
    *
    * @param entry    OData entry
    * @param origin   product URL
    * @param syncConf synchronizer configuration
    * @return a product able to be ingested
    * @throws IngestibleODataProduct.MissingProductsException if the product cannot be generated
    */
   public IngestibleODataProduct entryToDownloadableProduct(final ODataEntry entry,
         final String origin, final SmartProductSynchronizer syncConf, Source source)
         throws IngestibleODataProduct.MissingProductsException
   {
      ProductInfoExtractor productInfo = new ProductInfoExtractor(entry, source);
      Map<String, MultiSourceDownloadableProduct> products = new HashMap<>();

      MultiSourceDownloadableProduct mainProduct = generateMultiSourceProduct(productInfo, syncConf, null);
      products.put(IngestibleODataProduct.UNALTERED, mainProduct);

      for (ProductInfoExtractor pinfo: productInfo.subProduct())
      {

         if (pinfo.id().equals("Quicklook"))
         {
            products.put(IngestibleODataProduct.QUICKLOOK, generateMultiSourceProduct(pinfo, syncConf, "Quicklook"));
         }
         if (pinfo.id().equals("Thumbnail"))
         {
            products.put(IngestibleODataProduct.THUMBNAIL, generateMultiSourceProduct(pinfo, syncConf, "Thumbnail"));
         }
      }

      return IngestibleODataProduct.fromODataEntry(entry, origin, productInfo.itemClass(), productInfo.metadata(), products);
   }

   /**
    * Generates a new {@link IngestibleProduct} from a OData entry.
    * <p>
    * This product is a reference.
    *
    * @param entry  OData entry
    * @param origin product URL
    * @return a product able to be ingested
    * @throws IngestibleODataProduct.MissingProductsException if the product cannot be generated
    */
   public IngestibleODataProduct entryToProduct(ODataEntry entry, String origin)
         throws IngestibleODataProduct.MissingProductsException
   {
      Map<String, ProductReference> products = new HashMap<>();

      // main product
      ProductInfoExtractor mainInfo = new ProductInfoExtractor(entry, null);
      ProductReference mainProduct = new ProductReference(mainInfo.location());
      mainProduct.setProperty(ProductConstants.DATA_SIZE, mainProduct.getContentLength());
      products.put(IngestibleODataProduct.UNALTERED, mainProduct);

      // sub products
      ProductReference subProduct;
      for (ProductInfoExtractor subInfo : mainInfo.subProduct())
      {
         subProduct = new ProductReference(subInfo.location());
         subProduct.setProperty(ProductConstants.DATA_SIZE, subInfo.size());

         if (subInfo.id().equals("Quicklook"))
         {
            products.put(IngestibleODataProduct.QUICKLOOK, subProduct);
         }
         if (subInfo.id().equals("Thumbnail"))
         {
            products.put(IngestibleODataProduct.THUMBNAIL, subProduct);
         }
      }

      return IngestibleODataProduct.fromODataEntry(entry, origin, mainInfo.itemClass(), mainInfo.metadata(), products);
   }

   /** This class allows to extract product information from a OData entry. */
   private static class ProductInfoExtractor
   {
      private static final Pattern PATTERN =
            Pattern.compile("filename=\"(.+?)\"", Pattern.CASE_INSENSITIVE);
      private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

      private final ODataEntry entry;
      private final Source source;
      private final InterruptibleHttpClient client;

      private String uuid;
      private String filename;

      ProductInfoExtractor(ODataEntry entry, Source source)
      {
         this.entry = entry;
         this.source = source;
         this.client = new InterruptibleHttpClient(new BasicAuthHttpClientProducer(source.getUsername(), source.getPassword()));
      }

      private ProductInfoExtractor(ODataEntry entry, Source source, InterruptibleHttpClient client, String uuid)
      {
         this.entry = entry;
         this.uuid = uuid;
         this.client = client;
         this.source = source;
      }

      @SuppressWarnings("unchecked")
      private String internalGetChecksum(String key)
      {
         String result = null;
         if (entry.getProperties().containsKey("Checksum"))
         {
            Map<String, String> checksum = (Map<String, String>) entry.getProperties().get("Checksum");
            result = checksum.get(key);
         }
         return result;
      }

      private String generateUrl()
      {
         StringBuilder sb = new StringBuilder(source.getUrl());
         sb.append("/Products('").append(uuid).append("')");
         if (!uuid().equals(id())) // is a sub-product
         {
            sb.append("/Products('").append(id()).append("')");
         }
         return sb.append("/$value").toString();
      }

      private String extractFilename(HttpResponse response)
            throws IngestibleODataProduct.MissingProductsException
      {
         String disposition = response.getFirstHeader(HEADER_CONTENT_DISPOSITION).getValue();
         Matcher m = PATTERN.matcher(disposition);
         if (!m.find())
         {
            throw new IngestibleODataProduct.MissingProductsException(
                  "Product filename extraction failure: " + generateUrl());
         }
         return m.group(1);
      }

      public String uuid()
      {
         if (uuid == null)
         {
            uuid = (String) entry.getProperties().get("Id");
         }
         return uuid;
      }

      public String id()
      {
         return (String) entry.getProperties().get("Id");
      }

      public String filename() throws IngestibleODataProduct.MissingProductsException
      {
         if (filename == null)
         {
            String url = generateUrl();
            try
            {
               HttpResponse response = client.interruptibleHead(url);
               if (response.getStatusLine().getStatusCode() != 200)
               {
                  throw new IngestibleODataProduct.MissingProductsException("Cannot retrieve product filename of " + url);
               }
               filename = extractFilename(response);
            }
            catch (IOException | InterruptedException e)
            {
               throw new IngestibleODataProduct.MissingProductsException("Cannot retrieve product filename of " + url);
            }
         }
         return filename;
      }

      public String checksumAlgorithm()
      {
         return internalGetChecksum(Model.ALGORITHM);
      }

      public String checksumValue()
      {
         return internalGetChecksum(Model.VALUE);
      }

      public long size()
      {
         long size = -1;
         Map<String, Object> properties = entry.getProperties();
         if (properties.containsKey("ContentLength"))
         {
            size = (long) properties.get("ContentLength");
         }
         return size;
      }

      public String itemClass()
      {
         String itemClass = null;
         Map<String, Object> properties = entry.getProperties();
         if (properties.containsKey("Class"))
         {
            itemClass = ((String) ((ODataEntry) properties.get("Class")).getProperties().get("Uri"));
         }
         return itemClass;
      }

      public List<MetadataIndex> metadata()
      {
         MetadataTypeService metadataService = ApplicationContextProvider.getBean(MetadataTypeService.class);

         ODataFeed attributesFeed;
         Map<String, Object> properties = entry.getProperties();
         if (!entry.containsInlineEntry() || properties.get("Attributes") == null)
         {
            return Collections.emptyList();
         }
         attributesFeed = ODataDeltaFeed.class.cast(properties.get("Attributes"));
         List<MetadataIndex> metadataIndexList = new ArrayList<>(attributesFeed.getEntries().size());
         for (ODataEntry attributeEntry: attributesFeed.getEntries())
         {
            Map<String, Object> attributeProperties = attributeEntry.getProperties();
            String name = (String) attributeProperties.get("Name");
            String type = (String) attributeProperties.get("ContentType");
            String queryable = null;
            String category;

            MetadataType mt = metadataService.getMetadataTypeByName(itemClass(), name);
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
            metadataIndexList.add(MetadataFactory.createMetadataIndex(
                  name, type, category, queryable, (String) attributeProperties.get("Value")));
         }
         return metadataIndexList;
      }

      private List<ProductInfoExtractor> subProduct()
      {
         Object subProducts = entry.getProperties().get("Products");
         if (subProducts == null)
         {
            return Collections.emptyList();

         }
         List<ODataEntry> subProductEntries = ODataDeltaFeed.class.cast(subProducts).getEntries();
         List<ProductInfoExtractor> subProductsInfo = new ArrayList<>(subProductEntries.size());
         for (ODataEntry subEntry: subProductEntries)
         {
            subProductsInfo.add(new ProductInfoExtractor(subEntry, source, client, uuid));
         }
         return subProductsInfo;
      }

      public String location()
      {
         return (String) entry.getProperties().get("LocalPath");
      }
   }
}
