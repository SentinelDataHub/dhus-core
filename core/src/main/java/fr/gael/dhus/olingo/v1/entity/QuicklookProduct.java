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
package fr.gael.dhus.olingo.v1.entity;

import fr.gael.dhus.olingo.v1.entityset.ProductEntitySet;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;
import fr.gael.drb.DrbFactory;
import fr.gael.drb.DrbNode;
import fr.gael.drb.DrbSequence;
import fr.gael.drb.query.Query;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.DataStoreProduct;
import org.dhus.store.datastore.DataStoreService;
import org.dhus.store.datastore.hfs.HfsProduct;

/**
 * Quicklook product must contains a quicklook
 */
public class QuicklookProduct extends Product
{
   // xpath_attributes is a set of xpath returning nodes (ClasscastException
   // overwise)
   private static String[] xpath_attributes = { "image/FormatName",
         "image/directory/Width", "image/directory/Height",
         "image/directory/NumBands" };

   private final DataStoreService DATASTORE_SERVICE = ApplicationContextProvider.getBean(DataStoreService.class);
   private final ConfigurationManager CFG_MGR = ApplicationContextProvider.getBean(ConfigurationManager.class);
   private DataStoreProduct physical;

   /**
    * Build this Quicklook instance.
    *
    * @param product
    * @throws NullPointerException is product contains no Quicklook.
    * @throws DataStoreException
    */
   public QuicklookProduct(fr.gael.dhus.database.object.Product product) throws DataStoreException
   {
      super (product);
      try
      {
         physical = DATASTORE_SERVICE.get(product.getQuicklookPath()).getImpl(DataStoreProduct.class);
      }
      catch (DataStoreException e)
      {
         physical = null;
      }
   }

   @Override
   public String getId ()
   {
      return "Quicklook";
   }

   @Override
   public String getName ()
   {
      return "QL-" + product.getIdentifier ();
   }

   @Override
   public String getContentType ()
   {
      if (product.getQuicklookPath ().toLowerCase ().endsWith ("gif"))
         return "image/gif";
      else
         return "image/jpeg";
   }

   @Override
   public Long getContentLength ()
   {
      return product.getQuicklookSize ();
   }

   @Override
   public boolean requiresControl ()
   {
      return false;
   }

   @Override
   public Map<String, Product> getProducts ()
   {
      return Collections.emptyMap();
   }

   @Override
   public Map<String, Node> getNodes ()
   {
      if (this.nodes == null)
      {
         Map<String, Node> nodes = new LinkedHashMap<>();
         DrbNode parent;
         if (physical != null)
         {
            parent = physical.getImpl(DrbNode.class);
         }
         else
         {
            parent = DrbFactory.openURI(getDownloadablePath());
         }
         if (parent != null) nodes.put (parent.getName (), new Node (parent));
         this.nodes = nodes;
      }
      return this.nodes;
   }

   /**
    * The returned list is immutable.
    */
   @Override
   public Map<String, Attribute> getAttributes ()
   {
      if (this.attributes == null)
      {
         Map<String, Attribute> attributes = new LinkedHashMap<>();
         DrbNode node;
         if (physical != null)
         {
            node = physical.getImpl(DrbNode.class);
         }
         else
         {
            node = DrbFactory.openURI(getDownloadablePath());
         }

         for (String xpath : xpath_attributes)
         {
            Query query = new Query (xpath);
            DrbSequence results = query.evaluate (node);
            if ( (results != null) && (results.getLength () > 0))
            {
               DrbNode result = (DrbNode) results.getItem (0);
               if (result.getValue () != null)
                  attributes.put (result.getName (),
                     new Attribute(result.getName(), result.getValue().toString(), null));
            }
         }
         this.attributes = attributes;
      }
      return this.attributes;
   }

   @Override
   public String getDownloadablePath ()
   {
      return product.getQuicklookPath ();
   }

   @Override
   public InputStream getInputStream () throws IOException
   {
      try
      {
         if (physical != null)
         {
            return physical.getImpl(InputStream.class);
         }
         // old storage
         return new FileInputStream (product.getQuicklookPath ());
      }
      catch (Exception e)
      {
         throw new IOException ("Cannot get quicklook from product", e);
      }
   }

   @Override
   public Map<String, Object> toEntityResponse(String root_url)
   {
      Map<String, Object> response = super.toEntityResponse(root_url);
      if (physical != null)
      {
         response.put(ProductEntitySet.LOCAL_PATH, physical.getResourceLocation());
      }
      else
      {
         String incPath = CFG_MGR.getArchiveConfiguration().getIncomingConfiguration().getPath();
         String qlPath  = product.getQuicklookPath();
         String relQlPath = Paths.get(incPath).relativize(Paths.get(qlPath)).toString();
         response.put(ProductEntitySet.LOCAL_PATH, relQlPath);
      }
      return response;
   }

   @Override
   protected DataStoreProduct getPhysicalProduct() throws DataStoreException
   {
      if (physical == null)
      {
         return new HfsProduct(new File(getDownloadablePath()));
      }
      return physical;
   }
}
