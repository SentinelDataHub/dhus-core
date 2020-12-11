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

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.olingo.odata2.api.ep.entry.ODataEntry;

import org.dhus.Product;

/**
 * An ingestible product synced from another DHuS using its OData API.
 */
public class IngestibleODataProduct implements IngestibleProduct
{
   public static final String UNALTERED = "unaltered";
   public static final String QUICKLOOK = "quicklook";
   public static final String THUMBNAIL = "thumbnail";

   // base fields
   private final String uuid;
   private final String origin;
   private final Date ingestionDate;
   private final Date creationDate;
   private final Boolean onDemand;

   // public metadata
   private final String itemClass;
   private final String identifier;
   private final Date contentStart;
   private final Date contentEnd;
   private final String footprint;
   private final List<MetadataIndex> metadataIndexes;

   // downloadable physical data
   private final Product product;
   private final Product quicklook;
   private final Product thumbnail;

   // mutable properties
   private final Map<String, Object> properties;

   // timer
   private long timerStartMillis;
   private long timerStopMillis;

   private IngestibleODataProduct(String uuid, String origin, Date ingestionDate, Date creationDate,
         String itemClass, String identifier, Date contentStart, Date contentEnd, String footprint,
         List<MetadataIndex> metadataIndexes, Product physicalProduct, Product quicklook, Product thumbnail, Boolean onDemand)
   {
      this.uuid = uuid;
      this.origin = origin;
      this.ingestionDate = ingestionDate;
      this.creationDate = creationDate;
      this.onDemand = onDemand;

      this.itemClass = itemClass;
      this.identifier = identifier;
      this.contentStart = contentStart;
      this.contentEnd = contentEnd;
      this.footprint = footprint;
      this.metadataIndexes = metadataIndexes;

      this.product = physicalProduct;
      this.quicklook = quicklook;
      this.thumbnail = thumbnail;

      this.properties = new HashMap<>();

      // get existing properties from physical product
      for (String propertyName: physicalProduct.getPropertyNames())
      {
         properties.put(propertyName, physicalProduct.getProperty(propertyName));
      }
   }

   public static IngestibleODataProduct fromODataEntry(ODataEntry productEntry, String origin, String itemClass,
         List<MetadataIndex> metadataIndexList, Map<String, ? extends Product> productAndDerived)
         throws MissingProductsException
   {
      Map<String, Object> odataProductProperties = productEntry.getProperties();

      // extract primitive properties
      String uuid = (String) odataProductProperties.get("Id");
      Date ingestionDate = ((GregorianCalendar) odataProductProperties.get("IngestionDate")).getTime();
      Date creationDate = ((GregorianCalendar) odataProductProperties.get("CreationDate")).getTime();
      String identifier = (String) odataProductProperties.get("Name");
      String footprint = (String) odataProductProperties.get("ContentGeometry");

      Boolean onDemand = (Boolean)odataProductProperties.get("OnDemand");
      // force onDemand default value (false) if field not found.
      if (onDemand == null)
      {
         onDemand = false;
      }

      // extract ContentDate complex property
      @SuppressWarnings("unchecked")
      Map<String, GregorianCalendar> contentDate = (Map<String, GregorianCalendar>) odataProductProperties.get("ContentDate");
      Date contentStart = contentDate.get("Start").getTime();
      Date contentEnd = contentDate.get("End").getTime();

      Product physicalProduct = productAndDerived.get(UNALTERED);
      Product quicklook = productAndDerived.get(QUICKLOOK);
      Product thumbnail = productAndDerived.get(THUMBNAIL);

      if (physicalProduct != null)
      {
         return new IngestibleODataProduct(uuid, origin, ingestionDate, creationDate, itemClass, identifier,
               contentStart, contentEnd, footprint, metadataIndexList, physicalProduct, quicklook, thumbnail, onDemand);
      }
      throw new MissingProductsException("Cannot instantiate without product, quicklook and thumbnail reference or downloadable");
   }

   @Override
   public String getUuid()
   {
      return uuid;
   }

   @Override
   public String getOrigin()
   {
      return origin;
   }

   @Override
   public String getItemClass()
   {
      return itemClass;
   }

   @Override
   public String getIdentifier()
   {
      return identifier;
   }

   @Override
   public List<MetadataIndex> getMetadataIndexes()
   {
      return metadataIndexes;
   }

   @Override
   public Date getContentStart()
   {
      return contentStart;
   }

   @Override
   public Date getContentEnd()
   {
      return contentEnd;
   }

   @Override
   public String getFootprint()
   {
      return footprint;
   }

   @Override
   public Date getIngestionDate()
   {
      return ingestionDate;
   }

   public Date getCreationDate()
   {
      return creationDate;
   }

   @Override
   public Product getQuicklook()
   {
      return quicklook;
   }

   @Override
   public Product getThumbnail()
   {
      return thumbnail;
   }

   @Override
   public Object getProperty(String key)
   {
      return properties.get(key);
   }

   @Override
   public Object setProperty(String key, Object value)
   {
      return properties.put(key, value);
   }

   @Override
   public Set<String> getPropertyNames()
   {
      return properties.keySet();
   }

   @Override
   public boolean hasImpl(Class<?> cl)
   {
      return product.hasImpl(cl);
   }

   @Override
   public <T> T getImpl(Class<? extends T> cl)
   {
      return product.getImpl(cl);
   }

   @Override
   public void close() throws IOException
   {
      if (product instanceof Closeable)
      {
         ((Closeable) product).close();
      }

      if (quicklook instanceof Closeable)
      {
         ((Closeable) quicklook).close();
      }

      if (thumbnail instanceof Closeable)
      {
         ((Closeable) thumbnail).close();
      }
   }

   @Override
   public String getName()
   {
      return product.getName();
   }

   @Override
   public void setName(String name)
   {
      throw new UnsupportedOperationException("Cannot change the name of an IngestibleProduct");
   }

   /**
    * Thrown by {@code fromODataEntry()} if data is missing.
    */
   public static class MissingProductsException extends Exception
   {
      private static final long serialVersionUID = 1L;

      public MissingProductsException(String msg)
      {
         super(msg);
      }
   }

   @Override
   public boolean removeSource()
   {
      return false;
   }

   @Override
   public Boolean isOnDemand()
   {
      return onDemand;
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
