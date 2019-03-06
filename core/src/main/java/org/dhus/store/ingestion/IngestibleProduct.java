/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
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
import java.util.Date;
import java.util.List;

import org.dhus.Product;

/**
 * Product that can be ingested in any Store, it is identified by a UUID.
 */
// TODO document methods
public interface IngestibleProduct extends Product, Closeable
{
   public String getUuid();

   public String getOrigin();

   public String getItemClass() throws MetadataExtractionException;

   public String getIdentifier();

   public List<MetadataIndex> getMetadataIndexes() throws MetadataExtractionException;

   public Date getContentStart() throws MetadataExtractionException;

   public Date getContentEnd() throws MetadataExtractionException;

   /**
    * May return null.
    *
    * @return footprint in WKT format, or null
    * @throws MetadataExtractionException could not extract metadata indexes from product
    */
   public String getFootprint() throws MetadataExtractionException;

   public Date getIngestionDate();

   /**
    * May return null.
    *
    * @return Quicklook derived product, or null
    */
   public Product getQuicklook();

   /**
    * May return null.
    *
    * @return Thumbnail derived product, or null
    */
   public Product getThumbnail();

   public boolean removeSource();
}

