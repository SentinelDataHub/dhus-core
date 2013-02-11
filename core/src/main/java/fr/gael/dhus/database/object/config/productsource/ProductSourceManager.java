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


package fr.gael.dhus.database.object.config.productsource;

import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

public interface ProductSourceManager
{
   /**
    * 
    * @param url
    * @param login
    * @param password
    * @param remoteIncoming
    * @param sourceCollection
    * @param listable
    * @param dateTime
    * @return
    */
   ProductSource create(String url, String login, String password,
         String remoteIncoming, String sourceCollection, boolean listable, XMLGregorianCalendar lastCreationDate);

   /**
    * 
    * @param source
    */
   void update(ProductSource source);
   
   /**
    * 
    * @param sourceId
    */
   void remove(Long sourceId);

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
}