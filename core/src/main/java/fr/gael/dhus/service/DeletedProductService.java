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
package fr.gael.dhus.service;

import fr.gael.dhus.database.dao.DeletedProductDao;
import fr.gael.dhus.database.object.DeletedProduct;
import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.datastore.processing.ProcessingUtils;
import fr.gael.dhus.olingo.v1.visitor.DeletedProductSQLVisitor;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product Service provides connected clients with a set of method
 * to interact with it.
 */
@Service
public class DeletedProductService extends WebService
{
   private static final Logger LOGGER = LogManager.getLogger();

   @Autowired
   private DeletedProductDao deletedProductDao;

   /**
    * OData dedicated Services.
    *
    * @param visitor OData expression visitor
    * @param skip $skip parameter
    * @param top $top parameter
    * @return a list of results
    */
   @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
   public List<DeletedProduct> getProducts(DeletedProductSQLVisitor visitor, int skip, int top)
   {
      return deletedProductDao.executeHQLQuery(visitor.getHqlQuery(), visitor.getHqlParameters(), skip, top);
   }

   /**
    * OData dedicated Services.
    *
    * @param visitor OData expression visitor
    * @return count of results
    */
   @Transactional(readOnly = true)
   public int countProducts(DeletedProductSQLVisitor visitor)
   {
      return deletedProductDao.countHQLQuery(visitor.getHqlQuery(), visitor.getHqlParameters());
   }

   public void storeProduct(Product product, List<MetadataIndex> indexes, String deletionCause)
   {
      DeletedProduct dproduct = new DeletedProduct();
      dproduct.setId(product.getId());
      dproduct.setUuid(product.getUuid());
      dproduct.setCreated(product.getCreated());
      dproduct.setUpdated(product.getUpdated());
      dproduct.setIdentifier(product.getIdentifier());
      dproduct.setFootPrint(product.getFootPrint());
      dproduct.setOrigin(product.getOrigin());
      dproduct.setDownloadSize(product.getDownloadableSize());
      dproduct.setSize(product.getSize());
      dproduct.setIngestionDate(product.getIngestionDate());
      dproduct.setContentStart(product.getContentStart());
      dproduct.setContentEnd(product.getContentEnd());

      // Case of ingestion performed before DHuS 0.4.4
      String clazz = product.getItemClass();
      if (clazz == null)
      {
         try
         {
            clazz = ProcessingUtils.getItemClassUri(ProcessingUtils.getClassFromProduct(product));
         }
         catch (IOException e)
         {
            LOGGER.warn("Cannot find item class for product '" + product.getIdentifier() + "'.", e);
         }
      }
      dproduct.setItemClass(clazz);

      dproduct.setDeletionDate(new Date());
      dproduct.setDeletionCause(deletionCause);
      try
      {
         dproduct.setChecksums(product.getDownload().getChecksums());
      }
      catch (IOException e)
      {
         LOGGER.error("There was an error while saving checksums of deleted product.", e);
      }

      try
      {
         dproduct.setMetadataIndexes(indexes);
      }
      catch (IOException e)
      {
         LOGGER.error("There was an error while saving metadataIndexes of deleted product.", e);
      }
      deletedProductDao.create(dproduct);
   }

   @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
   public int count()
   {
      return deletedProductDao.count();
   }

   @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
   public DeletedProduct getProduct(String uuid)
   {
      return deletedProductDao.getProductByUuid(uuid);
   }

   @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
   public void delete(DeletedProduct product)
   {
      deletedProductDao.delete(product);
   }

}
