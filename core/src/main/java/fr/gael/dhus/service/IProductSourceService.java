/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2021 GAEL Systems
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

import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import fr.gael.dhus.database.object.config.productsource.ProductSource;
import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.Source;
import fr.gael.dhus.sync.impl.ODataProductSynchronizer;

public interface IProductSourceService
{
   /**
    * 
    * @param url
    * @param login
    * @param password
    * @param remoteIncoming
    * @param sourceCollection
    * @param listable
    * @param lastCreationDate
    * @return
    */
   ProductSource createProductSource(String url, String login, String password,
         String remoteIncoming, String sourceCollection, boolean listable, XMLGregorianCalendar lastCreationDate);
   
   /**
    * 
    * @param source
    */
   void updateProductSource(ProductSource source);
   
   /**
    * 
    * @param sourceId
    */
   boolean removeProductSource(ProductSource productSource);

   /**
    * 
    * @param sourceId
    * @return
    */
   ProductSource getProductSource(long sourceId);

   /**
    * 
    * @return
    */
   List<ProductSource> getProductSources(); 
   
   /**
    * 
    * @param productSynchronizer
    * @return
    */
   List<ProductSource> getReferencedSources (ProductSynchronizer productSynchronizer);

   /**
    * 
    * @param productSynchronizer
    * @return
    */
   List<Source> getListableSources(ProductSynchronizer productSynchronizer); 

   /**
    * 
    * @return
    */
   ProductSource getProductSource(Source source);

   /**
    * 
    * @param productSynchronizer
    */
   void rankSources(ProductSynchronizer productSynchronizer, ODataProductSynchronizer odataProdSync);

   
   Source getSourceConfiguration(ProductSynchronizer productSynchronizer, long id);
}