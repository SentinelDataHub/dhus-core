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
import java.util.Objects;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationException;
import fr.gael.dhus.system.config.ConfigurationManager;

public class ProductSourceManagerImpl extends ProductSources implements ProductSourceManager
{  
   private static final Logger LOGGER = LogManager.getLogger(ProductSourceManagerImpl.class);

   @XmlTransient
   private long maxId = -1;

   @Override
   public ProductSource create(String url, String login, String password,String remoteIncoming,
         String sourceCollection, boolean listable, XMLGregorianCalendar lastCreationDate)
   {
     ProductSource productSource = new ProductSource();

     productSource.setId(nextProductSourceId());
     productSource.setUrl(Objects.requireNonNull(url));
     productSource.setLogin(Objects.requireNonNull(login));
     productSource.setPassword(Objects.requireNonNull(password));
     productSource.setListable(listable);

     if (remoteIncoming != null)
     {
        productSource.setRemoteIncoming(remoteIncoming);
     }

     super.getProductSource().add(productSource);

     save();
     return productSource;
  }

  @Override
  public void update(ProductSource source)
  {
     List<ProductSourceConfiguration> sources = super.getProductSource();
     int index = sources.indexOf(source);
     if (index != -1)
     {
        ProductSourceConfiguration syncSource = sources.get(index);
        syncSource.setUrl(source.getUrl());
        syncSource.setLogin(source.getLogin());
        syncSource.setPassword(source.getPassword());
        syncSource.setRemoteIncoming(source.getRemoteIncoming());
        syncSource.setListable(source.isListable());

        save();
     }
  }

   @Override
   public void remove(Long sourceId)
   {
      if (sourceId != null)
      {
         ProductSource productSource = getProductSource(sourceId);
         if (productSource != null
               && super.getProductSource().remove(productSource))
         {
            save();
         }
      }
   }

   @Override
   public ProductSource getProductSource(long sourceId)
   {
      for (ProductSourceConfiguration productSource : super.getProductSource())
      {
         if (productSource.getId() == sourceId)
         {
            return (ProductSource) productSource;
         }
      }
      return null;
   }

   @SuppressWarnings("unchecked")
   @Override
   public List<ProductSource> getProductSources()
   {
      List<?> list = super.getProductSource();
      return (List<ProductSource>) list;
   }

   private synchronized Long nextProductSourceId()
   {
      if (maxId == -1)
      {
         for (ProductSourceConfiguration productSource : super.getProductSource())
         {
            long id = productSource.getId();
            if (maxId < id)
            {
               maxId = id;
            }
         }
      }
      maxId = maxId + 1;
      return maxId;
   }
   
   private void save()
   {
      ConfigurationManager cfg = ApplicationContextProvider.getBean(ConfigurationManager.class);
      try
      {
         cfg.saveConfiguration();
      }
      catch (ConfigurationException e)
      {
         LOGGER.error("There was an error while saving configuration", e);
      }
   }
}
