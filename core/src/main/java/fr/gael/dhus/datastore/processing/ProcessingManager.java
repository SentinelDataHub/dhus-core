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
package fr.gael.dhus.datastore.processing;

import com.google.common.io.Closer;

import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.system.config.ConfigurationManager;
import fr.gael.dhus.system.init.WorkingDirectory;
import fr.gael.dhus.util.JTSFootprintParser;

import fr.gael.drb.DrbAttribute;
import fr.gael.drb.DrbFactory;
import fr.gael.drb.DrbNode;
import fr.gael.drb.DrbSequence;
import fr.gael.drb.impl.DrbNodeImpl;
import fr.gael.drb.impl.xml.XmlWriter;
import fr.gael.drb.query.ExternalVariable;
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
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.imageio.ImageIO;
import javax.media.jai.RenderedImageList;

import org.apache.commons.io.IOUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.ProductConstants;
import org.dhus.ProductFactory;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.DataStoreService;

import org.geotools.gml2.GMLConfiguration;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.xml.sax.InputSource;

/**
 * Manages product processing.
 */
@Component
public class ProcessingManager
{
   private static final Logger LOGGER = LogManager.getLogger(ProcessingManager.class);

   private static final String METADATA_NAMESPACE = "http://www.gael.fr/dhus#";
   private static final String PROPERTY_IDENTIFIER = "identifier";
   private static final String PROPERTY_METADATA_EXTRACTOR =
      "metadataExtractor";
   private static final String MIME_PLAIN_TEXT = "plain/text";
   private static final String MIME_APPLICATION_GML = "application/gml+xml";

   private static final String SIZE_QUERY=loadResourceFile("size.xql");

   @Autowired
   private ConfigurationManager cfgManager;

   @Autowired
   private DataStoreService dataStoreService;

   /**
    * Process product to finalize its ingestion
    */
   @Transactional
   public Product process(Product product) throws IOException, ProcessingException
   {
      LOGGER.info ("* Ingestion started.");
      long allStart = System.currentTimeMillis();

      long start = System.currentTimeMillis();
      LOGGER.info (" - Product information extraction started");
      // Force the ingestion date after transfer
      URL productUrl = product.getPath();
      DrbNode productNode = ProcessingUtils.getNodeFromPath(productUrl.getPath());
      try
      {
         DrbCortexItemClass productClass;
         try
         {
            productClass = ProcessingUtils.getClassFromNode (productNode);
         }
         catch (IOException e)
         {
            throw new UnsupportedOperationException (
                  "Cannot compute item class.", e);
         }

         // Set the product itemClass
         product.setItemClass (productClass.getOntClass ().getURI ());

         // Set the product identifier
         String identifier = extractIdentifier (productNode, productClass);
         if (identifier != null)
         {
            LOGGER.debug ("Found product identifier " + identifier);
            product.setIdentifier (identifier);
         }
         else
         {
            LOGGER.warn ("No defined identifier - using filename");
            product.setIdentifier(productUrl.getFile());
         }
         LOGGER.info (" - Product information extraction done in " +
               (System.currentTimeMillis () - start) + "ms.");

         // Extract images
         start = System.currentTimeMillis ();
         LOGGER.info (" - Product images extraction started");
         product = extractImages (productNode, product);
         LOGGER.info (" - Product images extraction done in " +
               (System.currentTimeMillis () - start) + "ms.");

         // Set the product indexes
         start = System.currentTimeMillis ();
         LOGGER.info (" - Product indexes and footprint extraction started");
         List<MetadataIndex> indexes =
               extractIndexes (productNode, productClass);
         SimpleDateFormat df =
               new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
         indexes.add (new MetadataIndex ("Identifier",
               null, "", PROPERTY_IDENTIFIER, product.getIdentifier ()));

      if (indexes == null || indexes.isEmpty ())
      {
         LOGGER.warn ("No index processed for product " + product.getPath ());
      }
      else
      {
         product.setIndexes (indexes);
         boolean jtsValid = false;
         Iterator<MetadataIndex> iterator = indexes.iterator ();
         while (iterator.hasNext ())
         {
            MetadataIndex index = iterator.next ();

            // Extracts queryable informations to be stored into database.
            if (index.getQueryable() != null)
            {
               // Begin position ("sensing start" or "validity start")
               if (index.getQueryable().equalsIgnoreCase ("beginposition"))
               {
                  try
                  {
                     product.setContentStart (df.parse (index.getValue ()));
                  }
                  catch (ParseException e)
                  {
                     LOGGER.warn ("Cannot set correctly product " +
                        "'content start' from indexes", e);
                  }
               }
               else
               // End position ("sensing stop" or "validity stop")
               if (index.getQueryable().equalsIgnoreCase ("endposition"))
               {
                  try
                  {
                     product.setContentEnd (df.parse (index.getValue ()));
                  }
                  catch (ParseException e)
                  {
                     LOGGER.warn ("Cannot set correctly product " +
                        "'content end' from indexes", e);
                  }
               }
            }
            /**
             * Extract the footprints according to its types (GML or JTS)
             */
            if (index.getType() != null)
            {
               if (index.getType().equalsIgnoreCase("application/gml+xml"))
               {
                  String gml_footprint = index.getValue ();
                  if ((gml_footprint != null) &&
                      checkGMLFootprint (gml_footprint))
                  {
                     product.setFootPrint (gml_footprint);
                  }
                  else
                  {
                     LOGGER.error ("Incorrect on empty footprint for product " +
                        product.getPath ());
                  }
               }
               // Should not have been application/wkt ?
               else if (index.getType().equalsIgnoreCase("application/jts"))
               {
                  String jts_footprint = index.getValue ();
                  String parsedFootprint = JTSFootprintParser.checkJTSFootprint (jts_footprint);
                  jtsValid = parsedFootprint != null;
                  if (jtsValid)
                  {
                     index.setValue (parsedFootprint);
                  }
                  else
                     if (jts_footprint != null)
                     {
                        // JTS footprint is wrong; remove the corrupted
                        // footprint.
                        iterator.remove ();
                     }
               }
            }
         }
         if (!jtsValid)
         {
            LOGGER.error ("JTS footprint not existing or not valid, " +
               "removing GML footprint on " + product.getPath ());
            product.setFootPrint (null);
         }
      }
         LOGGER.info(" - Product indexes and footprint extraction done in {}ms.", System.currentTimeMillis() - start);

         LOGGER.info("* Ingestion done in {}ms.", System.currentTimeMillis() - allStart);

         return product;
      }
      finally
      {
         closeNode (productNode);
      }
   }

   private void closeNode (DrbNode node)
   {
      if (node instanceof DrbNodeImpl)
      {
         DrbNodeImpl.class.cast (node).close (false);
      }
   }

   /**
    * Check GML Footprint validity
    */
   private boolean checkGMLFootprint (String footprint)
   {
      try
      {
         Configuration configuration = new GMLConfiguration ();
         Parser parser = new Parser (configuration);
         parser.parse (new InputSource (new StringReader (footprint)));
         return true;
      }
      catch (Exception e)
      {
         LOGGER.error("Error in extracted footprint: " + e.getMessage());
         return false;
      }
   }

   /**
    * Retrieve product identifier using its Drb node and class.
    */
   private String extractIdentifier (DrbNode productNode,
      DrbCortexItemClass productClass)
   {
      java.util.Collection<String> properties = null;

      // Get all values of the metadata properties attached to the item
      // class or any of its super-classes
      properties =
         productClass.listPropertyStrings (METADATA_NAMESPACE +
            PROPERTY_IDENTIFIER, false);

      // Return immediately if no property value were found
      if (properties == null)
      {
         LOGGER.warn ("Item \"" + productClass.getLabel () +
            "\" has no identifier defined.");
         return null;
      }

      // retrieve the first extractor
      String property = properties.iterator ().next ();

      // Filter possible XML markup brackets that could have been encoded
      // in a CDATA section
      property = property.replaceAll ("&lt;", "<");
      property = property.replaceAll ("&gt;", ">");

      // Create a query for the current metadata extractor
      Query query = new Query (property);

      // Evaluate the XQuery
      DrbSequence sequence = query.evaluate (productNode);

      // Check that something results from the evaluation: jump to next
      // value otherwise
      if ( (sequence == null) || (sequence.getLength () < 1))
      {
         return null;
      }

      String identifier = sequence.getItem (0).toString ();
      return identifier;
   }

   /**
    * Loads product images from Drb node and stores information inside the
    * product before returning it
    */
   private Product extractImages (DrbNode productNode, Product product)
         throws ProcessingException
   {
      if (ImageIO.getUseCache()) ImageIO.setUseCache(false);

      if (!ImageFactory.isImage (productNode))
      {
         LOGGER.debug ("No Image.");
         return product;
      }

      RenderedImageList input_list = null;
      RenderedImage input_image = null;
      try
      {
         input_list = ImageFactory.createImage (productNode);
         input_image = RenderingFactory.createDefaultRendering(input_list);
      }
      catch (Exception e)
      {
         LOGGER.debug ("Cannot retrieve default rendering");
         if (LOGGER.isDebugEnabled ())
         {
            LOGGER.debug ("Error occurs during rendered image reader", e);
         }

         if (input_list == null)
         {
            return product;
         }
         input_image = input_list;
      }

      if (input_image == null)
      {
         return product;
      }

      // Generate Quicklook
      int quicklook_width = cfgManager.getProductConfiguration ()
            .getQuicklookConfiguration ().getWidth ();
      int quicklook_height = cfgManager.getProductConfiguration ()
            .getQuicklookConfiguration ().getHeight ();

      // Deprecated code: raise warn.
      boolean quicklook_cutting = cfgManager.getProductConfiguration ()
                     .getQuicklookConfiguration ().isCutting ();
      if (quicklook_cutting)
         LOGGER.warn(
            "Quicklook \"cutting\" parameter is deprecated, will be ignored.");

      LOGGER.info ("Generating Quicklook " +
         quicklook_width + "x" + quicklook_height + " from " +
         input_image.getWidth() + "x" + input_image.getHeight ());

      RenderedImage image = null;
      try
      {
         image = ProcessingUtils.resizeImage(input_image, quicklook_width, quicklook_height);
      }
      catch (InconsistentImageScale e)
      {
         LOGGER.error("Cannot resize image: {}", e.getMessage());
         SdiImageFactory.close(input_list);
         return product;
      }


      // Manages the quicklook output
      File image_directory;
      image_directory = WorkingDirectory.getTempDirectoryFile();

      String identifier = product.getIdentifier ();
      File quicklook = new File(image_directory, identifier + "-ql.jpg");
      try
      {
         if (ImageIO.write(image, "jpg", quicklook))
         {
            String uuid = UUID.randomUUID().toString();
            org.dhus.Product p =
                  ProductFactory.generateProduct(quicklook.toURI().toURL());
            dataStoreService.set(uuid, p);
            product.setQuicklookPath(uuid);
            product.setQuicklookSize(
                  (Long) p.getProperty(ProductConstants.DATA_SIZE));
            quicklook.delete();
         }
      }
      catch (IOException | DataStoreException e)
      {
         LOGGER.error ("Cannot save quicklook.",e);
      }

      // Generate Thumbnail
      int thumbnail_width = cfgManager.getProductConfiguration ()
            .getThumbnailConfiguration ().getWidth ();
      int thumbnail_height = cfgManager.getProductConfiguration ()
            .getThumbnailConfiguration ().getHeight ();

      LOGGER.info ("Generating Thumbnail " +
         thumbnail_width + "x" + thumbnail_height + " from " +
         input_image.getWidth() + "x" + input_image.getHeight () + " image.");

      try
      {
         image = ProcessingUtils.resizeImage(input_image, thumbnail_width, thumbnail_height);
      }
      catch (InconsistentImageScale e)
      {
         LOGGER.error("Cannot resize image: {}", e.getMessage());
         SdiImageFactory.close(input_list);
         return product;
      }

      // Manages the thumbnail output
      File thumbnail = new File(image_directory, identifier + "-th.jpg");
      try
      {
         if (ImageIO.write(image, "jpg", thumbnail))
         {
            String uuid = UUID.randomUUID().toString();
            org.dhus.Product p =
                  ProductFactory.generateProduct(thumbnail.toURI().toURL());
            dataStoreService.set(uuid, p);
            product.setThumbnailPath(uuid);
            product.setThumbnailSize(
                  (Long)p.getProperty(ProductConstants.DATA_SIZE));
            thumbnail.delete();
         }
      }
      catch (IOException | DataStoreException e)
      {
         LOGGER.error ("Cannot save thumbnail.",e);
      }
      SdiImageFactory.close (input_list);

      return product;
   }

   /**
    * Retrieve product indexes using its Drb node and class.
    */
   private List<MetadataIndex> extractIndexes (DrbNode productNode,
      DrbCortexItemClass productClass)
   {
      java.util.Collection<String> properties = null;

      // Get all values of the metadata properties attached to the item
      // class or any of its super-classes
      properties =
         productClass.listPropertyStrings (METADATA_NAMESPACE +
            PROPERTY_METADATA_EXTRACTOR, false);

      // Return immediately if no property value were found
      if (properties == null)
      {
         LOGGER.warn ("Item \"" + productClass.getLabel () +
            "\" has no metadata defined.");
         return null;
      }


      // Prepare the index structure.
      List<MetadataIndex> indexes = new ArrayList<MetadataIndex> ();

      // Loop among retrieved property values
      for (String property : properties)
      {
         // Filter possible XML markup brackets that could have been encoded
         // in a CDATA section
         property = property.replaceAll ("&lt;", "<");
         property = property.replaceAll ("&gt;", ">");
         /*
          * property = property.replaceAll("\n", " "); // Replace eol by blank
          * space property = property.replaceAll(" +", " "); // Remove
          * contiguous blank spaces
          */

         // Create a query for the current metadata extractor
         Query metadataQuery =  null;
         try
         {
            metadataQuery = new Query (property);
         }
         catch (Exception e)
         {
            LOGGER.error("Cannot compile metadata extractor " +
               "(set debug mode to see details)", e);
            if (LOGGER.isDebugEnabled())
            {
               LOGGER.debug(property);
            }
            throw new RuntimeException("Cannot compile metadata extractor",e);
         }

         // Evaluate the XQuery
         DrbSequence metadataSequence = metadataQuery.evaluate (productNode);

         // Check that something results from the evaluation: jump to next
         // value otherwise
         if ( (metadataSequence == null) || (metadataSequence.getLength () < 1))
         {
            continue;
         }

         // Loop among results
         for (int iitem = 0; iitem < metadataSequence.getLength (); iitem++)
         {
            // Get current metadata node
            DrbNode n = (DrbNode) metadataSequence.getItem (iitem);

            // Get name
            DrbAttribute name_att = n.getAttribute ("name");
            Value name_v = null;
            if (name_att != null) name_v = name_att.getValue ();
            String name = null;
            if (name_v != null)
               name = name_v.convertTo (Value.STRING_ID).toString ();

            // get type
            DrbAttribute type_att = n.getAttribute ("type");
            Value type_v = null;
            if (type_att != null)
               type_v = type_att.getValue ();
            else
               type_v = new fr.gael.drb.value.String (MIME_PLAIN_TEXT);
            String type = type_v.convertTo (Value.STRING_ID).toString ();

            // get category
            DrbAttribute cat_att = n.getAttribute ("category");
            Value cat_v = null;
            if (cat_att != null)
               cat_v = cat_att.getValue ();
            else
               cat_v = new fr.gael.drb.value.String ("product");
            String category = cat_v.convertTo (Value.STRING_ID).toString ();

            // get category
            DrbAttribute qry_att = n.getAttribute ("queryable");
            String queryable = null;
            if (qry_att != null)
            {
               Value qry_v = qry_att.getValue ();
               if (qry_v != null)
                  queryable = qry_v.convertTo (Value.STRING_ID).toString ();
            }

            // Get value
            String value = null;
            if (MIME_APPLICATION_GML.equals (type) && n.hasChild ())
            {
               ByteArrayOutputStream out = new ByteArrayOutputStream ();
               XmlWriter.writeXML (n.getFirstChild (), out);
               value = out.toString ();
               try
               {
                  out.close ();
               }
               catch (IOException e)
               {
                  LOGGER.warn ("Cannot close stream !", e);
               }
            }
            else
            // Case of "text/plain"
            {
               Value value_v = n.getValue ();
               if (value_v != null)
               {
                  value = value_v.convertTo (Value.STRING_ID).toString ();
                  value = value.trim ();
               }
            }

            if ( (name != null) && (value != null))
            {
               MetadataIndex index = new MetadataIndex ();
               index.setName (name);
               try
               {
                  index.setType (new MimeType (type).toString ());
               }
               catch (MimeTypeParseException e)
               {
                  LOGGER.warn (
                     "Wrong metatdata extractor mime type in class \"" +
                      productClass.getLabel () + "\" for metadata called \"" +
                      name + "\".", e);
               }
               index.setCategory (category);
               index.setValue (value);
               index.setQueryable (queryable);
               indexes.add (index);
            }
            else
            {
               String field_name = "";
               if (name != null)
                  field_name = name;
               else
                  if (queryable != null)
                     field_name = queryable;
                  else
                     if (category != null)
                        field_name = "of category " + category;

               LOGGER.warn ("Nothing extracted for field " + field_name);
            }
         }
      }
      return indexes;
   }

   /**
    * Calculate a file or a folder size. This method recursively browse product
    * according to the supported item loaded by Drb.
    */
   long drb_size (File file)
   {
      String variable_name = "product_path";
      
      // Use Drb/XQuery to compute size.
      Query query = new Query(SIZE_QUERY);
      if (query.getEnvironment().containsExternalVariable(variable_name))
      {
         ExternalVariable[] extVariables = query.getExternalVariables();
         // Set the external variables
         for (int iext = 0; iext < extVariables.length; iext++)
         {
            ExternalVariable var = extVariables[iext];
            String varName = var.getName();
            if (varName.equals(variable_name))
            {
               // Set it a new value
               var.setValue(new
                  fr.gael.drb.value.String(file.getAbsolutePath()));
            }
         }
      }
      else
         throw new UnsupportedOperationException ("Cannot set \"" + 
            variable_name + "\" XQuery parameter.");
      
      DrbSequence sequence = query.evaluate(DrbFactory.openURI("."));
      return ((fr.gael.drb.value.UnsignedLong)sequence.getItem(0).getValue().
         convertTo(Value.UNSIGNED_LONG_ID)).longValue();
   }

   
   long system_size (File file)
   {
      long size=0;
      if (file.isDirectory())
      {
         for (File subFile : file.listFiles ())
         {
            size += system_size (subFile);
         }
      }
      else
      {
         size = file.length ();
      }
      return size;
   }
   
   long size (File file)
   {
      try
      {
         return drb_size(file);
      }
      catch (Exception e)
      {
         LOGGER.warn ("Cannot compute size via Drb API, using system API(" + 
            e.getMessage() + ").");
         return system_size(file);
      }
   }
   
   /**
    * Inner method used to load small ASCII resources that can be stored 
    * into memory. Thios resource shall be store close to this class (same 
    * package folder).
    * @param resource the resource to load.
    * @return the ASCII content of the resource.
    */
   private static String loadResourceFile (String resource)
   {
      Closer closer = Closer.create();
      String contents=null;
      try
      {
         InputStream is = closer.register (ProcessingManager.class.
            getResourceAsStream(resource));
         contents=IOUtils.toString(is);
      }
      catch (Throwable e)
      {
         throw new UnsupportedOperationException(
            "Cannot retrieve resource \"" + resource + "\".",e);
      }
      finally
      {
         try
         {
            closer.close();
         }
         catch (IOException e) { ; }
      }
      return contents;
   }
}
