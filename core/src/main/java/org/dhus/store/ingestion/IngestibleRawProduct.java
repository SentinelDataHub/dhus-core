/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2020 GAEL Systems
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
package org.dhus.store.ingestion;

import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.database.object.config.product.ProductConfiguration;
import fr.gael.dhus.datastore.processing.InconsistentImageScale;
import fr.gael.dhus.datastore.processing.ProcessingUtils;
import fr.gael.dhus.factory.MetadataFactory;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;
import fr.gael.dhus.system.init.WorkingDirectory;
import fr.gael.dhus.util.DrbChildren;
import fr.gael.dhus.util.WKTFootprintParser;

import fr.gael.drb.DrbAttribute;
import fr.gael.drb.DrbNode;
import fr.gael.drb.DrbSequence;
import fr.gael.drb.impl.DrbNodeImpl;
import fr.gael.drb.impl.xml.XmlWriter;
import fr.gael.drb.query.Query;
import fr.gael.drb.value.Value;
import fr.gael.drbx.cortex.DrbCortexItemClass;
import fr.gael.drbx.image.ImageFactory;
import fr.gael.drbx.image.impl.sdi.SdiImageFactory;
import fr.gael.drbx.image.jai.RenderingFactory;

import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.imageio.ImageIO;
import javax.media.jai.RenderedImageList;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.Product;
import org.dhus.ProductFactory;
import org.dhus.store.datastore.DataStoreProduct;
import org.dhus.store.datastore.hfs.HfsProduct;

import org.geotools.gml2.GMLConfiguration;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Lazy raw product used during the ingestion process, it is identified by a UUID.
 * <p>
 * This product extracts, stores, and provide its own metadata on demand according to a preset dictionary.
 */
public class IngestibleRawProduct implements IngestibleProduct
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final String METADATA_NAMESPACE = "http://www.gael.fr/dhus#";
   private static final String PROPERTY_IDENTIFIER = "identifier";
   private static final String PROPERTY_METADATA_EXTRACTOR = "metadataExtractor";
   private static final String MIME_PLAIN_TEXT = "plain/text";
   private static final String MIME_APPLICATION_GML = "application/gml+xml";

   private static final DateTimeFormatter DATETIME_FORMAT =
      new DateTimeFormatterBuilder ().appendPattern ("yyyy-MM-dd'T'HH:mm:ss")
         .appendFraction (ChronoField.NANO_OF_SECOND, 0, 9, true).appendPattern ("'Z'").toFormatter();

   // base fields
   private final String uuid;
   private final String productUrl; // FIXME should be URL
   private final DrbNode productNode;
   private final Product physicalProduct;
   private Boolean onDemand;

   // set when called
   private Date ingestionDate = null;

   // public extracted data
   private String itemClass = null;
   private String identifier = null;
   private Date contentStart = null;
   private Date contentEnd = null;
   private String footprint = null;
   private List<MetadataIndex> metadataIndexes = null;

   // private extracted data
   private DrbCortexItemClass drbCortexItemClass = null;

   // physical data
   private File quicklook = null;
   private File thumbnail = null;
   private HfsProduct quicklookProduct = null;
   private HfsProduct thumbnailProduct = null;

   // public externally added properties (mutable)
   private final Map<String, Object> publicProperties;

   // used to avoid removing DataStore products
   private final boolean readOnlySource;

   // timer
   private long timerStartMillis;
   private long timerStopMillis;

   public static IngestibleRawProduct fromURL(URL productUrl)
   {
      return fromURL(productUrl, false);
   }

   public static IngestibleRawProduct fromURL(URL productUrl, boolean onDemand)
   {
      DrbNode nodeFromPath = ProcessingUtils.getNodeFromPath(productUrl.getPath());
      Product physicalProduct = ProductFactory.generateProduct(productUrl);

      return new IngestibleRawProduct(
            UUID.randomUUID().toString(),
            productUrl.toString(),
            nodeFromPath,
            physicalProduct,
            false,
            onDemand);
   }

   public static IngestibleRawProduct fromDataStoreProduct(String uuid, DataStoreProduct dataStoreProduct,
      fr.gael.dhus.database.object.Product databaseProduct)
   {
      DrbNode node = dataStoreProduct.getImpl(DrbNode.class);
      String resourceLocation = dataStoreProduct.getResourceLocation();

      // getFirstChild required for zips, but crashes extraction for TGZ
      // something is wrong with DRB's Tar implementation (or Zip)
      // similar issue with DhusODataV1Node and SMOS
      // TODO fix DRB implementations and remove this
      if (DrbChildren.shouldIngestionUseFirstChild(resourceLocation, node))
      {
         LOGGER.debug("Skipping first child of DrbNode from product at {}", resourceLocation);
         node = node.getFirstChild();
      }

      return new IngestibleRawProduct(uuid, resourceLocation, node, dataStoreProduct, true, databaseProduct.getIngestionDate(), false);
   }

   private IngestibleRawProduct(String uuid, String productUrl, DrbNode productNode, Product physicalProduct, boolean readOnlySource, Date ingestionDate, boolean onDemand)
   {
      this(uuid,productUrl,productNode,physicalProduct,readOnlySource, onDemand);
      this.ingestionDate = ingestionDate;
   }

   private IngestibleRawProduct(String uuid, String productUrl, DrbNode productNode, Product physicalProduct, boolean readOnlySource, boolean onDemand)
   {
      this.uuid = uuid;
      this.productUrl = productUrl;
      this.productNode = productNode;
      this.physicalProduct = physicalProduct;
      this.onDemand = onDemand;

      this.readOnlySource = readOnlySource;

      this.publicProperties = new HashMap<>();
   }

   @Override
   public String getUuid()
   {
      return uuid;
   }

   @Override
   public String getOrigin()
   {
      return productUrl.toString();
   }

   @Override
   public String getItemClass() throws MetadataExtractionException
   {
      if (itemClass == null)
      {
         itemClass = getDrbCortexItemClass().getOntClass().getURI();
      }
      return itemClass;
   }

   @Override
   public String getIdentifier()
   {
      if (identifier == null)
      {
         try
         {
            identifier = extractIdentifier(getProductNode(), getDrbCortexItemClass());
         }
         catch (MetadataExtractionException | RuntimeException e)
         {
            LOGGER.warn(e.getMessage());
         }
         if (identifier != null)
         {
            LOGGER.debug("Found product identifier {}", identifier);
         }
         else
         {
            LOGGER.warn("No defined identifier - using default name");
            identifier = getName();
         }
      }
      return identifier;
   }

   @Override
   public List<MetadataIndex> getMetadataIndexes() throws MetadataExtractionException
   {
      if (metadataIndexes == null)
      {
         metadataIndexes = extractIndexes(getProductNode(), getDrbCortexItemClass());

         if (metadataIndexes == null || metadataIndexes.isEmpty())
         {
            LOGGER.warn("No index processed for product {}", productUrl);
         }
      }
      return metadataIndexes;
   }

   @Override
   public Date getContentStart() throws MetadataExtractionException
   {
      if (contentStart == null)
      {
         for (MetadataIndex index: getMetadataIndexes())
         {
            if (index.getQueryable() != null && index.getQueryable().equalsIgnoreCase("beginposition"))
            {
               TemporalAccessor parsedDate = DATETIME_FORMAT.parse (index.getValue ());

               LocalDateTime localDateTime = LocalDateTime.from (parsedDate);
               ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
               Instant instant = Instant.from(zonedDateTime);
               contentStart = java.util.Date.from (instant);
            }
         }
      }

      // mandatory metadataindex
      if (contentStart == null)
      {
         throw new MetadataExtractionException("Failed to extract ContentDate Start");
      }

      return contentStart;
   }

   @Override
   public Date getContentEnd() throws MetadataExtractionException
   {
      if (contentEnd == null)
      {
         for (MetadataIndex index: getMetadataIndexes())
         {
            if (index.getQueryable() != null
                  && index.getQueryable().equalsIgnoreCase("endposition"))
            {
               TemporalAccessor parsedDate = DATETIME_FORMAT.parse (index.getValue ());

               LocalDateTime localDateTime = LocalDateTime.from (parsedDate);
               ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
               Instant instant = Instant.from(zonedDateTime);
               contentEnd = java.util.Date.from (instant);
            }
         }
      }

      // mandatory metadataindex
      if (contentEnd == null)
      {
         throw new MetadataExtractionException("Failed to extract ContentDate End");
      }

      return contentEnd;
   }

   @Override
   public String getFootprint() throws MetadataExtractionException
   {
      if (footprint == null)
      {
         for (MetadataIndex index: getMetadataIndexes())
         {
            if (index.getType() != null && index.getType().equalsIgnoreCase(MIME_APPLICATION_GML))
            {
               String gmlFootprint = index.getValue();
               if ((gmlFootprint != null) && checkGMLFootprint(gmlFootprint))
               {
                  footprint = gmlFootprint;
                  return footprint;
               }
               else
               {
                  LOGGER.error("Incorrect or empty footprint for product {}", productUrl);
               }
            }
         }
      }
      return footprint;
   }

   /**
    * @return the ingestionDate
    */
   @Override
   public Date getIngestionDate()
   {
      if (ingestionDate == null)
      {
         ingestionDate = new Date();
      }
      return ingestionDate;
   }

   @Override
   public Product getQuicklook()
   {
      // check existence of image file
      if (quicklook == null || !quicklook.exists())
      {
         extractImages(getProductNode());
      }

      // make product from image file if possible
      if (quicklookProduct == null && quicklook != null)
      {
         quicklookProduct = new HfsProduct(quicklook);
      }

      // may be null
      return quicklookProduct;
   }

   @Override
   public Product getThumbnail()
   {
      // check existence of image file
      if (thumbnail == null || !thumbnail.exists())
      {
         extractImages(getProductNode());
      }

      // make product from image file if possible
      if(thumbnailProduct == null && thumbnail != null)
      {
         thumbnailProduct = new HfsProduct(thumbnail);
      }

      // may be null
      return thumbnailProduct;
   }

   @Override
   public Set<String> getPropertyNames()
   {
      return publicProperties.keySet();
   }

   @Override
   public Object setProperty(String key, Object value)
   {
      return publicProperties.put(key, value);
   }

   @Override
   public Object getProperty(String key)
   {
      return publicProperties.get(key);
   }

   @Override
   public void close() throws IOException
   {
      if (productNode != null)
      {
         closeNode(productNode);
      }

      if (quicklook != null && quicklook.exists())
      {
         quicklook.delete();
         quicklook = null;
      }

      if (thumbnail != null && thumbnail.exists())
      {
         thumbnail.delete();
         thumbnail = null;
      }
   }

   private DrbCortexItemClass getDrbCortexItemClass() throws MetadataExtractionException
   {
      if (drbCortexItemClass == null)
      {
         try
         {
            drbCortexItemClass = ProcessingUtils.getClassFromNode(getProductNode());
         }
         catch (IOException e)
         {
            throw new MetadataExtractionException("Cannot compute item class", e);
         }
      }
      return drbCortexItemClass;
   }

   private DrbNode getProductNode()
   {
      return productNode;
   }

   private void closeNode(DrbNode node)
   {
      if (node instanceof DrbNodeImpl)
      {
         DrbNodeImpl.class.cast(node).close(false);
      }
   }

   /**
    * Check GML Footprint validity
    */
   private boolean checkGMLFootprint(String footprint)
   {
      try
      {
         Configuration configuration = new GMLConfiguration();
         Parser parser = new Parser(configuration);
         parser.parse(new InputSource(new StringReader(footprint)));
         return true;
      }
      catch (IOException | ParserConfigurationException | SAXException | RuntimeException e)
      {
         LOGGER.error("Error in extracted footprint: {}", e.getMessage());
         return false;
      }
   }

   /**
    * Retrieve product identifier using its Drb node and class.
    */
   private String extractIdentifier(DrbNode productNode, DrbCortexItemClass productClass)
   {
      Collection<String> properties;

      // Get all values of the metadata properties attached to the item
      // class or any of its super-classes
      properties = productClass.listPropertyStrings(METADATA_NAMESPACE + PROPERTY_IDENTIFIER, false);

      // Return immediately if no property value were found
      if (properties == null)
      {
         LOGGER.warn("Item \"{}\" has no identifier defined", productClass.getLabel());
         return null;
      }

      // retrieve the first extractor
      String property = properties.iterator().next();

      // Filter possible XML markup brackets that could have been encoded
      // in a CDATA section
      property = property.replaceAll("&lt;", "<");
      property = property.replaceAll("&gt;", ">");

      // Create a query for the current metadata extractor
      Query query = new Query(property);

      // Evaluate the XQuery
      DrbSequence sequence = query.evaluate(productNode);

      // Check that something results from the evaluation: jump to next
      // value otherwise
      if ((sequence == null) || (sequence.getLength() < 1))
      {
         return null;
      }

      return sequence.getItem(0).toString();
   }

   /**
    * Loads product images from Drb node and stores information inside the
    * product before returning it
    */
   private void extractImages(DrbNode productNode)
   {
      long start = System.currentTimeMillis();
      LOGGER.info(" - Product images extraction started");

      if (ImageIO.getUseCache())
      {
         ImageIO.setUseCache(false);
      }

      if (!ImageFactory.isImage(productNode))
      {
         LOGGER.debug("No Image");
         return;
      }

      RenderedImageList input_list = null;
      RenderedImage input_image = null;
      try
      {
         input_list = ImageFactory.createImage(productNode);
         input_image = RenderingFactory.createDefaultRendering(input_list);
      }
      catch (Exception e)
      {
         LOGGER.warn("Cannot retrieve default rendering");
         if (input_list == null)
         {
            LOGGER.error("No image can be rendered", e);
            return;
         }
         LOGGER.debug("Cannot retrieve default rendering",e);
         input_image = input_list;
      }

      if (input_image == null)
      {
         return;
      }

      ProductConfiguration productConfiguration = ApplicationContextProvider
            .getBean(ConfigurationManager.class).getProductConfiguration();

      // Generate Quicklook
      int quicklook_width = productConfiguration.getQuicklookConfiguration().getWidth();
      int quicklook_height = productConfiguration.getQuicklookConfiguration().getHeight();

      // Deprecated code: raise warn.
      boolean quicklook_cutting = productConfiguration.getQuicklookConfiguration().isCutting();
      if (quicklook_cutting)
      {
         LOGGER.warn("Quicklook \"cutting\" parameter is deprecated, will be ignored");
      }

      LOGGER.info("Generating Quicklook {}x{} from {}x{} image",
            quicklook_width, quicklook_height,
            input_image.getWidth(), input_image.getHeight());

      RenderedImage image = null;
      try
      {
         image = ProcessingUtils.resizeImage(input_image, quicklook_width, quicklook_height);
      }
      catch (InconsistentImageScale e)
      {
         LOGGER.error("Cannot resize image: {}", e.getMessage());
         SdiImageFactory.close(input_list);
         return;
      }

      // Manages the quicklook output
      File image_directory = WorkingDirectory.getTempDirectoryFile();

      String identifier = getIdentifier();
      File quicklook = new File(image_directory, identifier + "-ql.jpg");
      try
      {
         if (ImageIO.write(image, "jpg", quicklook))
         {
            this.quicklook = quicklook;
         }
      }
      catch (IOException e)
      {
         LOGGER.error("Cannot save quicklook.", e);
      }

      // Generate Thumbnail
      int thumbnail_width = productConfiguration.getThumbnailConfiguration().getWidth();
      int thumbnail_height = productConfiguration.getThumbnailConfiguration().getHeight();

      LOGGER.info("Generating Thumbnail {}x{} from {}x{} image",
            thumbnail_width, thumbnail_height,
            input_image.getWidth(), input_image.getHeight());

      try
      {
         image = ProcessingUtils.resizeImage(input_image, thumbnail_width, thumbnail_height);
      }
      catch (InconsistentImageScale e)
      {
         LOGGER.error("Cannot resize image: {}", e.getMessage());
         SdiImageFactory.close(input_list);
         return;
      }

      // Manages the thumbnail output
      File thumbnail = new File(image_directory, identifier + "-th.jpg");
      try
      {
         if (ImageIO.write(image, "jpg", thumbnail))
         {
            this.thumbnail = thumbnail;
         }
      }
      catch (IOException e)
      {
         LOGGER.error("Cannot save thumbnail", e);
      }
      SdiImageFactory.close(input_list);

      LOGGER.info(" - Product images extraction done in {} ms", (System.currentTimeMillis() - start));
   }

   /** Retrieve product indexes using its Drb node and class. */
   private List<MetadataIndex> extractIndexes(DrbNode productNode, DrbCortexItemClass productClass)
         throws MetadataExtractionException
   {
      long start = System.currentTimeMillis();
      LOGGER.info(" - Product indexes and footprint extraction started");

      // Get all values of the metadata properties attached to the item
      // class or any of its super-classes
      Collection<String> properties =
            productClass.listPropertyStrings(METADATA_NAMESPACE + PROPERTY_METADATA_EXTRACTOR, false);

      // Return immediately if no property value were found
      if (properties == null)
      {
         LOGGER.warn("Item \"{}\" has no metadata defined", productClass.getLabel());
         return null;
      }

      // Prepare the index structure.
      List<MetadataIndex> indexes = new ArrayList<>();

      // Loop among retrieved property values
      for (String property: properties)
      {
         // Filter possible XML markup brackets that could have been encoded
         // in a CDATA section
         property = property.replaceAll("&lt;", "<");
         property = property.replaceAll("&gt;", ">");

         // Create a query for the current metadata extractor
         Query metadataQuery = null;
         try
         {
            metadataQuery = new Query(property);
         }
         catch (RuntimeException e)
         {
            LOGGER.error("Cannot compile metadata extractor (set debug mode to see details)", e);
            if (LOGGER.isDebugEnabled())
            {
               LOGGER.debug(property);
            }
            throw new MetadataExtractionException("Cannot compile metadata extractor", e);
         }

         // Evaluate the XQuery
         DrbSequence metadataSequence = metadataQuery.evaluate(productNode);

         // Check that something results from the evaluation: jump to next
         // value otherwise
         if ((metadataSequence == null) || (metadataSequence.getLength() < 1))
         {
            continue;
         }

         // Loop among results
         for (int iitem = 0; iitem < metadataSequence.getLength(); iitem++)
         {
            // Get current metadata node
            DrbNode n = (DrbNode) metadataSequence.getItem(iitem);

            // Get name
            DrbAttribute name_att = n.getAttribute("name");
            Value name_v = null;
            if (name_att != null)
            {
               name_v = name_att.getValue();
            }
            String name = null;
            if (name_v != null)
            {
               name = name_v.convertTo(Value.STRING_ID).toString();
            }

            // get type
            DrbAttribute type_att = n.getAttribute("type");
            Value type_v = null;
            if (type_att != null)
            {
               type_v = type_att.getValue();
            }
            else
            {
               type_v = new fr.gael.drb.value.String(MIME_PLAIN_TEXT);
            }
            String type = type_v.convertTo(Value.STRING_ID).toString();

            // get category
            DrbAttribute cat_att = n.getAttribute("category");
            Value cat_v = null;
            if (cat_att != null)
            {
               cat_v = cat_att.getValue();
            }
            else
            {
               cat_v = new fr.gael.drb.value.String("product");
            }
            String category = cat_v.convertTo(Value.STRING_ID).toString();

            // get category
            DrbAttribute qry_att = n.getAttribute("queryable");
            String queryable = null;
            if (qry_att != null)
            {
               Value qry_v = qry_att.getValue();
               if (qry_v != null)
               {
                  queryable = qry_v.convertTo(Value.STRING_ID).toString();
               }
            }

            // Get value
            String value = null;
            if (MIME_APPLICATION_GML.equals(type) && n.hasChild())
            {
               ByteArrayOutputStream out = new ByteArrayOutputStream();
               XmlWriter.writeXML(n.getFirstChild(), out);
               value = out.toString();
               try
               {
                  out.close();
               }
               catch (IOException e)
               {
                  LOGGER.warn("Cannot close stream !", e);
               }
            }
            else
            // Case of "text/plain"
            {
               Value value_v = n.getValue();
               if (value_v != null)
               {
                  value = value_v.convertTo(Value.STRING_ID).toString();
                  value = value.trim();
               }
            }

            if ((name != null) && (value != null))
            {
               String metadateType = null;
               try
               {
                  metadateType = new MimeType(type).toString();
               }
               catch (MimeTypeParseException e)
               {
                  LOGGER.warn("Wrong metatdata extractor mime type in class \"{}, args \" for metadata called \"{}\"",
                              productClass.getLabel(), name, e);
               }
               MetadataIndex index = MetadataFactory.createMetadataIndex(name, metadateType, category, queryable, value);
               indexes.add(index);
            }
            else
            {
               String field_name = "";
               if (name != null)
               {
                  field_name = name;
               }
               else if (queryable != null)
               {
                  field_name = queryable;
               }
               else if (category != null)
               {
                  field_name = "of category " + category;
               }

               LOGGER.warn("Nothing extracted for field {}", field_name);
            }
         }
      }
      finalizeIndexes(indexes);

      LOGGER.info(" - Product indexes and footprint extraction done in {} ms", (System.currentTimeMillis() - start));
      return Collections.unmodifiableList(indexes);
   }

   private void finalizeIndexes(List<MetadataIndex> indexes)
   {
      boolean invalidWkt = false;
      Iterator<MetadataIndex> iterator = indexes.iterator();
      while (iterator.hasNext())
      {
         MetadataIndex index = iterator.next();
         // Extract the footprints according to its types (GML or WKT)
         if (index.getType() != null)
         {
            // Should not have been application/wkt ?
            if (index.getType().equalsIgnoreCase("application/jts")
                  || index.getType().equalsIgnoreCase("application/wkt"))
            {
               String wktFootprint = index.getValue();
               String parsedWktFootprint = WKTFootprintParser.reformatWKTFootprint(wktFootprint);
               if (parsedWktFootprint != null)
               {
                  index.setValue(parsedWktFootprint);
               }
               else if (wktFootprint != null)
               {
                  // Invalid WKT footprint; remove the corrupted footprint
                  invalidWkt = true;
                  LOGGER.error("Incorrect or empty footprint for product {}", productUrl);
                  iterator.remove();
               }
            }
         }
      }

      // remove GML footprint as well if WKT is invalid
      if (invalidWkt)
      {
         LOGGER.error("WKT footprint not existing or not valid, removing GML footprint on {}", productUrl);
         iterator = indexes.iterator();
         while (iterator.hasNext())
         {
            MetadataIndex index = iterator.next();
            if (index.getType().equalsIgnoreCase(MIME_APPLICATION_GML))
            {
               iterator.remove();
            }
         }
      }

      indexes.add(MetadataFactory.IngestionDate.create(getIngestionDate()));
      indexes.add(MetadataFactory.Identifier.create(getIdentifier()));
   }

   @Override
   public boolean hasImpl(Class<?> cl)
   {
      return toPhysicalProduct().hasImpl(cl);
   }

   @Override
   public <T> T getImpl(Class<? extends T> cl)
   {
      return toPhysicalProduct().getImpl(cl);
   }

   private Product toPhysicalProduct()
   {
      return physicalProduct;
   }

   // TODO define or remove
   @Override
   public String getName()
   {
      return toPhysicalProduct().getName();
   }

   // TODO define or remove
   @Override
   public void setName(String name)
   {
      throw new UnsupportedOperationException("Cannot change the name of an IngestibleProduct");
   }

   @Override
   public boolean removeSource()
   {
      if (!readOnlySource)
      {
         LOGGER.info("Deleting source of product {} at: {}", uuid, productUrl);
         try
         {
            Path productPath = Paths.get(new URL(productUrl).getPath());
            return Files.deleteIfExists(productPath);
         }
         catch (IOException | RuntimeException e)
         {
            LOGGER.warn("Cannot delete source of product {} at: {} ({})", uuid, productUrl, e.getMessage());
            return false;
         }
      }
      return false;
   }

   @Override
   public Boolean isOnDemand()
   {
      return onDemand;
   }

   public void setOnDemand(Boolean onDemand)
   {
      this.onDemand = onDemand;
   }

   @Override
   public void startTimer()
   {
      timerStartMillis = System.currentTimeMillis();
   }

   @Override
   public void stopTimer()
   {
      timerStopMillis = System.currentTimeMillis();
   }

   @Override
   public long getIngestionTimeMillis()
   {
      return timerStopMillis - timerStartMillis;
   }
}
