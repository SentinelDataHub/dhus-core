/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2018 GAEL Systems
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
package fr.gael.dhus.database.object;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Deleted Product instance implements an old product entry into the database. This product
 * reflects an old product in the archive which was deleted.
 */
@Entity
@Table(name = "DELETED_PRODUCTS")
public class DeletedProduct implements Serializable
{
   /**
    * serial id
    */
   private static final long serialVersionUID = 4421791317577048968L;
   public static final String AUTO_EVICTION = "Automatic Eviction";

   @Id
   @Column(name = "ID", nullable = false)
   private Long id;

   @Column(name = "uuid", unique = true, nullable = false)
   private String uuid;

   @Temporal(TemporalType.TIMESTAMP)
   @Column(name = "created", nullable = false)
   private Date created;

   @Temporal(TemporalType.TIMESTAMP)
   @Column(name = "updated", nullable = false)
   private Date updated;

   @Column(name = "IDENTIFIER", nullable = true)
   private String identifier;

   /**
    * GML footprint string if any
    */
   @Column(name = "FOOTPRINT", nullable = true, length = 8192)
   private String footPrint;

   @Column(name = "ORIGIN")
   private String origin;

   @Column(name = "SIZE")
   private Long size;

   @Column(name = "ingestionDate")
   private Date ingestionDate;

   @Column(name = "contentStart")
   private Date contentStart;

   @Column(name = "contentEnd")
   private Date contentEnd;

   @Column(name = "ITEM_CLASS")
   private String itemClass;

   @Column(name = "deletionDate")
   private Date deletionDate;

   @Column(name = "deletionCause")
   private String deletionCause;

   @Column(name = "CHECKSUM_ALGORITHM")
   private String checksumAlgorithm;

   @Column(name = "CHECKSUM_VALUE")
   private String checksumValue;

   /**
    * @return the productId
    */
   public Long getId()
   {
      return id;
   }

   public void setId(Long id)
   {
      this.id = id;
   }

   /**
    * @return the created
    */
   public Date getCreated()
   {
      return created;
   }

   /**
    * @param created the created to set
    */
   public void setCreated(Date created)
   {
      this.created = created;
   }

   /**
    * @return the created
    */
   public Date getUpdated()
   {
      return updated;
   }

   /**
    * @param updated the created to set
    */
   public void setUpdated(Date updated)
   {
      this.updated = updated;
   }

   /**
    * @param identifier the identifier to set
    */
   public void setIdentifier(String identifier)
   {
      this.identifier = identifier;
   }

   /**
    * @return the identifier
    */
   public String getIdentifier()
   {
      return identifier;
   }

   /**
    * @param foot_print the footPrint to set
    */
   public void setFootPrint(String foot_print)
   {
      this.footPrint = foot_print;
   }

   /**
    * @return the footPrint
    */
   public String getFootPrint()
   {
      return footPrint;
   }

   /**
    * @param origin the origin to set
    */
   public void setOrigin(String origin)
   {
      this.origin = origin;
   }

   /**
    * @return the origin
    */
   public String getOrigin()
   {
      return origin;
   }

   public Long getSize()
   {
      return size;
   }

   public void setSize(Long size)
   {
      this.size = size;
   }

   public String getUuid()
   {
      return uuid;
   }

   public void setUuid(String uuid)
   {
      this.uuid = uuid;
   }

   public Date getIngestionDate()
   {
      return ingestionDate;
   }

   public void setIngestionDate(Date ingestion_date)
   {
      this.ingestionDate = ingestion_date;
   }

   public Date getContentStart()
   {
      return contentStart;
   }

   public void setContentStart(Date content_start)
   {
      this.contentStart = content_start;
   }

   public Date getContentEnd()
   {
      return contentEnd;
   }

   public void setContentEnd(Date content_end)
   {
      this.contentEnd = content_end;
   }

   public String getItemClass()
   {
      return this.itemClass;
   }

   public void setItemClass(String item_class)
   {
      this.itemClass = item_class;
   }

   public Date getDeletionDate()
   {
      return deletionDate;
   }

   public void setDeletionDate(Date deletionDate)
   {
      this.deletionDate = deletionDate;
   }

   public String getDeletionCause()
   {
      return deletionCause;
   }

   public void setDeletionCause(String deletionCause)
   {
      this.deletionCause = deletionCause;
   }

   public String getChecksumAlgorithm()
   {
      return checksumAlgorithm;
   }

   public void setChecksumAlgorithm(String checksumAlgorithm)
   {
      this.checksumAlgorithm = checksumAlgorithm;
   }

   public String getChecksumValue()
   {
      return checksumValue;
   }

   public void setChecksumValue(String checksumValue)
   {
      this.checksumValue = checksumValue;
   }

   public void setChecksums(Map<String, String> checksums) throws IOException
   {
      if (checksums == null || checksums.isEmpty())
      {
         checksumAlgorithm = null;
         checksumValue = null;
      }
      else
      {
         Map.Entry<String, String> checksum = checksums.entrySet().iterator().next();
         checksumAlgorithm = checksum.getKey();
         checksumValue = checksum.getValue();
      }
   }

   public Map<String, String> getChecksums()
   {
      if (checksumAlgorithm == null ||checksumValue == null)
      {
         return Collections.emptyMap();
      }
      return Collections.singletonMap(getChecksumAlgorithm(), getChecksumValue ());
   }

   @Override
   public boolean equals(Object o)
   {
      if (o == null)
      {
         return false;
      }
      if (this == o)
      {
         return true;
      }
      if (!(o instanceof DeletedProduct))
      {
         return false;
      }
      DeletedProduct other = (DeletedProduct) o;
      if (this.id == null)
      {
         return false;
      }
      return this.id.equals(other.id);
   }

   @Override
   public int hashCode()
   {
      int hash = 7;
      hash = 67 * hash + (this.id != null ? this.id.hashCode() : 0);
      hash = 67 * hash + (this.uuid != null ? this.uuid.hashCode() : 0);
      return hash;
   }

}
