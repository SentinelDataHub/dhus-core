/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016 GAEL Systems
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
package org.dhus.store.database;

import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.service.MetadataTypeService;
import fr.gael.dhus.service.metadata.MetadataType;
import fr.gael.dhus.service.metadata.SolrField;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.drbx.cortex.DrbCortexItemClass;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.solr.common.SolrInputDocument;
import org.dhus.AbstractProduct;

import java.net.URL;
import java.util.Date;
import java.util.List;

public class DatabaseProduct extends AbstractProduct
{
   private static final Logger LOGGER = LogManager.getLogger(DatabaseProduct.class);

   public static final String DEFAULT_PATH = "deprecated";

   private Long id;
   private String uuid;
   private URL path;
   private String identifier;
   private List<MetadataIndex> indexes;
   private String footprint;
   private String origin;
   private Date ingestionDate;
   private Date contentStart;
   private Date contentEnd;
   private DrbCortexItemClass itemClass;
   private User owner;
   private Long size;

   @Override
   protected Class<?>[] implsTypes()
   {
      return new Class[]
      {
         Product.class, SolrInputDocument.class
      };
   }

   @Override
   public <T> T getImpl(Class<? extends T> cl)
   {
      if (!hasImpl(cl))
      {
         return null;
      }

      if (cl.equals(Product.class))
      {
         return (T) toProduct();
      }

      if (cl.equals(SolrInputDocument.class))
      {
         return (T) toSolrInputDoc();
      }

      return null;
   }

   private Product toProduct()
   {
      Product product = new Product();
      product.setUuid(uuid);
      product.setIdentifier(identifier);
      product.setPath(path);
      product.setIndexes(indexes);
      product.setFootPrint(footprint);
      product.setOrigin(origin);
      product.setIngestionDate(ingestionDate);
      product.setContentStart(contentStart);
      product.setContentEnd(contentEnd);
      product.setItemClass(itemClass.getOntClass().getURI());
      product.setOwner(owner);
      product.setSize(size);

      return product;
   }

   private SolrInputDocument toSolrInputDoc()
   {
      SolrInputDocument doc = new SolrInputDocument();
      doc.setField("path", DEFAULT_PATH);
      doc.setField("id", id);
      doc.setField("uuid", uuid);

      // Metadata
      if (indexes != null && !indexes.isEmpty())
      {
         for (MetadataIndex index: indexes)
         {
            String type = index.getType();

            // Only textual information stored in field contents (full-text search)
            if ((type == null) || type.isEmpty() || "text/plain".equals(type))
            {
               doc.addField("contents", index.getValue());
            }

            MetadataType mt =
                  ApplicationContextProvider.getBean(MetadataTypeService.class)
                        .getMetadataTypeByName(itemClass.getOntClass().getURI(), index.getName());

            SolrField sf = (mt != null) ? mt.getSolrField() : null;

            if (sf != null || index.getQueryable() != null)
            {
               Boolean is_multivalued = (sf != null) ? sf.isMultiValued() : null;

               String field_name = (sf != null) ? sf.getName() : index.getQueryable().toLowerCase();

               if (is_multivalued != null && is_multivalued)
               {
                  doc.addField(field_name, index.getValue());
               }
               else
               {
                  doc.setField(field_name, index.getValue());
               }
               LOGGER.debug("Added {}: {}", field_name, index.getValue());
            }
         }
      }
      else
      {
         LOGGER.warn("Product '{}' contains no metadata", identifier);
      }

      return doc;
   }

   public Long getId()
   {
      return id;
   }

   public void setId(Long id)
   {
      this.id = id;
   }

   public String getUuid()
   {
      return uuid;
   }

   public void setUuid(String uuid)
   {
      this.uuid = uuid;
   }

   public URL getPath()
   {
      return path;
   }

   public void setPath(URL path)
   {
      this.path = path;
   }

   public String getIdentifier()
   {
      return identifier;
   }

   public void setIdentifier(String identifier)
   {
      this.identifier = identifier;
   }

   public List<MetadataIndex> getIndexes()
   {
      return indexes;
   }

   public void setIndexes(List<MetadataIndex> indexes)
   {
      this.indexes = indexes;
   }

   public String getFootprint()
   {
      return footprint;
   }

   public void setFootprint(String footprint)
   {
      this.footprint = footprint;
   }

   public String getOrigin()
   {
      return origin;
   }

   public void setOrigin(String origin)
   {
      this.origin = origin;
   }

   public Date getIngestionDate()
   {
      return ingestionDate;
   }

   public void setIngestionDate(Date ingestionDate)
   {
      this.ingestionDate = ingestionDate;
   }

   public Date getContentStart()
   {
      return contentStart;
   }

   public void setContentStart(Date contentStart)
   {
      this.contentStart = contentStart;
   }

   public Date getContentEnd()
   {
      return contentEnd;
   }

   public void setContentEnd(Date contentEnd)
   {
      this.contentEnd = contentEnd;
   }

   public DrbCortexItemClass getItemClass()
   {
      return itemClass;
   }

   public void setItemClass(DrbCortexItemClass itemClass)
   {
      this.itemClass = itemClass;
   }

   public User getOwner()
   {
      return owner;
   }

   public void setOwner(User owner)
   {
      this.owner = owner;
   }

   public Long getSize()
   {
      return size;
   }

   public void setSize(Long size)
   {
      this.size = size;
   }
}
