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
package fr.gael.dhus.olingo.v1.entity;

import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.olingo.Security;
import fr.gael.dhus.olingo.v1.Expander;
import fr.gael.dhus.olingo.v1.ExpectedException.InvalidKeyException;
import fr.gael.dhus.olingo.v1.ExpectedException.InvalidTargetException;
import fr.gael.dhus.olingo.v1.ExpectedException.NotAllowedException;
import fr.gael.dhus.olingo.v1.Model;
import fr.gael.dhus.olingo.v1.entityset.DeletedProductEntitySet;
import fr.gael.dhus.service.DeletedProductService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.uri.NavigationSegment;

/**
 * A OData representation of a DHuS Product.
 */
public class DeletedProduct extends AbstractEntity
{
   private static final DeletedProductService DELETED_PRODUCT_SERVICE =
         ApplicationContextProvider.getBean(DeletedProductService.class);

   protected final fr.gael.dhus.database.object.DeletedProduct deletedProduct;
   protected String id;

   public DeletedProduct(fr.gael.dhus.database.object.DeletedProduct deletedProduct)
   {
      this.deletedProduct = deletedProduct;
      this.id = deletedProduct.getUuid();
   }

   public String getId()
   {
      return deletedProduct.getUuid();
   }

   public String getName()
   {
      return deletedProduct.getIdentifier();
   }

   public Date getCreationDate()
   {
      return deletedProduct.getCreated();
   }

   public String getFootPrint()
   {
      return deletedProduct.getFootPrint();
   }

   public Long getSize()
   {
      return deletedProduct.getDownloadSize();
   }

   public Date getIngestionDate()
   {
      return deletedProduct.getIngestionDate();
   }

   public Date getContentStart()
   {
      return deletedProduct.getContentStart();
   }

   public Date getContentEnd()
   {
      return deletedProduct.getContentEnd();
   }

   public Date getDeletionDate()
   {
      return deletedProduct.getDeletionDate();
   }

   public String getDeletionCause()
   {
      return deletedProduct.getDeletionCause();
   }

   public boolean hasChecksum()
   {
      return !(deletedProduct.getChecksums().isEmpty());
   }

   public String getChecksumAlgorithm()
   {
      if (!(hasChecksum()))
      {
         return null;
      }
      return deletedProduct.getChecksumAlgorithm();
   }

   public String getChecksumValue()
   {
      if (!(hasChecksum()))
      {
         return null;
      }
      return deletedProduct.getChecksumValue();
   }

   public fr.gael.dhus.olingo.v1.entity.Class getItemClass()
   {
      return new fr.gael.dhus.olingo.v1.entity.Class(deletedProduct.getItemClass());
   }

   @Override
   public Map<String, Object> toEntityResponse(String root_url)
   {
      Map<String, Object> res = new HashMap<>();
      res.put(DeletedProductEntitySet.ID, getId());
      res.put(DeletedProductEntitySet.NAME, getName());
      res.put(DeletedProductEntitySet.CREATION_DATE, getCreationDate());
      res.put(DeletedProductEntitySet.FOOTPRINT, getFootPrint());
      res.put(DeletedProductEntitySet.SIZE, getSize());
      res.put(DeletedProductEntitySet.INGESTION_DATE, getIngestionDate());
      LinkedHashMap<String, Date> dates = new LinkedHashMap<>();
      dates.put(Model.TIME_RANGE_START, getContentStart());
      dates.put(Model.TIME_RANGE_END, getContentEnd());
      res.put(DeletedProductEntitySet.CONTENT_DATE, dates);
      res.put(DeletedProductEntitySet.DELETION_DATE, getDeletionDate());
      res.put(DeletedProductEntitySet.DELETION_CAUSE, getDeletionCause());
      HashMap<String, String> checksum = new LinkedHashMap<>();
      checksum.put(Model.ALGORITHM, getChecksumAlgorithm());
      checksum.put(Model.VALUE, getChecksumValue());
      res.put(DeletedProductEntitySet.CHECKSUM, checksum);

      return res;
   }

   @Override
   public Object getProperty(String prop_name) throws ODataException
   {
      switch (prop_name)
      {
         case DeletedProductEntitySet.ID: return getId();
         case DeletedProductEntitySet.NAME: return getName();
         case DeletedProductEntitySet.CREATION_DATE: return getCreationDate();
         case DeletedProductEntitySet.FOOTPRINT: return getFootPrint();
         case DeletedProductEntitySet.SIZE: return getSize();
         case DeletedProductEntitySet.INGESTION_DATE: return getIngestionDate();
         case DeletedProductEntitySet.DELETION_DATE: return getDeletionDate();
         case DeletedProductEntitySet.DELETION_CAUSE: return getDeletionCause();

         default: throw new ODataException("Property '" + prop_name + "' not found.");
      }
   }

   @Override
   public Map<String, Object> getComplexProperty(String prop_name)
         throws ODataException
   {
      if (prop_name.equals(DeletedProductEntitySet.CONTENT_DATE))
      {
         Map<String, Object> values = new HashMap<>();
         values.put(Model.TIME_RANGE_START, getContentStart());
         values.put(Model.TIME_RANGE_END, getContentEnd());
         return values;
      }
      if (prop_name.equals(DeletedProductEntitySet.CHECKSUM))
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
   public Object navigate(NavigationSegment ns) throws ODataException
   {
      Object res;

      if (ns.getEntitySet().getName().equals(Model.CLASS.getName()))
      {
         res = getItemClass();
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
   public List<String> getExpandableNavLinkNames()
   {
      // Product inherits from Node
      List<String> res = new ArrayList<>(super.getExpandableNavLinkNames());
      res.add(Model.CLASS.getName());
      return res;
   }

   @Override
   public List<Map<String, Object>> expand(String navlink_name, String self_url)
   {
      if (Model.CLASS.getName().equals(navlink_name))
      {
         return Expander.entityToData(getItemClass(), self_url);
      }
      return super.expand(navlink_name, self_url);
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }
      if (obj == null)
      {
         return false;
      }
      if (!(obj instanceof DeletedProduct))
      {
         return false;
      }
      DeletedProduct other = (DeletedProduct) obj;
      return id == other.id;
   }

   @Override
   public int hashCode()
   {
      return ((id == null) ? 0 : id.hashCode());
   }

   public static void delete(String uuid) throws ODataException
   {
      if (Security.currentUserHasRole(Role.DATA_MANAGER))
      {
         fr.gael.dhus.database.object.DeletedProduct p = DELETED_PRODUCT_SERVICE.getProduct(uuid);
         if (p == null)
         {
            throw new InvalidKeyException(uuid, DeletedProduct.class.getSimpleName());
         }
         DELETED_PRODUCT_SERVICE.delete(p);
      }
      else
      {
         throw new NotAllowedException();
      }
   }
}
