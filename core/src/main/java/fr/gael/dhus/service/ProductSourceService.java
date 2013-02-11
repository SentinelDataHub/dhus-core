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

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.dhus.metrics.external.MetricsServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.gael.dhus.database.object.config.productsource.ProductSource;
import fr.gael.dhus.database.object.config.productsource.ProductSourceManager;
import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.Source;
import fr.gael.dhus.olingo.ODataClient;
import fr.gael.dhus.sync.impl.ODataProductSynchronizer;
import fr.gael.dhus.system.config.ConfigurationManager;

@Service
public class ProductSourceService implements IProductSourceService
{
   @Autowired
   private ConfigurationManager configurationManager;

   @Autowired
   private MetricsServiceInterface metricsService;
   
   private static final Logger LOGGER = LogManager.getLogger();


   @Override
   public ProductSource createProductSource(String url, String login, String password,
         String remoteIncoming, String sourceCollection, boolean listable,
         XMLGregorianCalendar lastCreationDate)
   {
      if (url == null || url.isEmpty())
      {
         throw new IllegalArgumentException("The URL " +url+ " is Invalid URL");
      }

      return getProductSourceManager().create(
            url,
            login,
            password,
            remoteIncoming,
            sourceCollection,
            listable,
            lastCreationDate);
   }

   @Override
   public void updateProductSource(ProductSource source)
   {
      getProductSourceManager().update(source);
   }

   @Override
   public boolean removeProductSource(ProductSource productSource)
   {
      long productSourceId = productSource.getId();
      if (getProductSourceManager().getProductSource(productSourceId) != null)
      {
         getProductSourceManager().remove(productSourceId);
         return getProductSource(productSourceId) == null;
      }
      return false;
   }

   @Override
   public ProductSource getProductSource(long sourceId)
   {
      return getProductSourceManager().getProductSource(sourceId);
   }

   @Override
   public List<ProductSource> getProductSources()
   {
      return getProductSourceManager().getProductSources();
   }

   private ProductSourceManager getProductSourceManager()
   {
      return configurationManager.getProductSourceManager();
   }

   @Override
   public List<ProductSource> getReferencedSources(ProductSynchronizer productSynchronizer)
   {
      List<ProductSource> result = new ArrayList<>();
      List<ProductSource> productSources = getProductSourceManager().getProductSources();
      List<Source> sources = productSynchronizer.getSources().getSource();

      for (Source source : sources)
      {
         for (ProductSource productSource : productSources)
         {
            if (source.getReferenceId() == productSource.getId())
            {
               result.add(productSource);
            }
         }
      }
      return result;
   }

   @Override
   public List<Source> getListableSources(ProductSynchronizer productSynchronizer)
   {
      List<Source> listableSources = new ArrayList<>();
      List<ProductSource> referencedSources = getReferencedSources(productSynchronizer);

      for (ProductSource referencedSource : referencedSources)
      {
         if (referencedSource.isListable())
         {
            Source source = getSource(productSynchronizer, referencedSource);
            listableSources.add(source);
         }
      }

      Comparator<Source> comparator = (ps1, ps2) -> {
         return ps1.getLastCreationDate().toGregorianCalendar().getTime()
               .compareTo(ps2.getLastCreationDate().toGregorianCalendar().getTime());
      };
      Collections.sort(listableSources, comparator);

      return listableSources;
   }

   @Override
   public ProductSource getProductSource(Source source)
   {
      return getProductSourceManager().getProductSource(source.getReferenceId());
   }
   
   @Override
   public Source getSourceConfiguration(ProductSynchronizer productSynchronizer, long id)
   {
      List<Source> sources = productSynchronizer.getSources().getSource();
      if (sources != null)
      {
         for(Source source: sources)
         {
            if (source.getReferenceId() == id)
            {
               return source;
            }
         }
      }
      return null;
   }

   @Override
   public void rankSources(ProductSynchronizer productSynchronizer, ODataProductSynchronizer odataProdSync)
   {
      List<ProductSource> syncSources = getReferencedSources(productSynchronizer);
      List <Long> productSourceIds = new ArrayList<>();
      Map<ProductSource, ProductSourceParameters> sourceMap =  new HashMap<ProductSource, ProductSourceParameters>();
      Map<Long, Double> resultMap =  new HashMap<Long, Double>();
      double maxRate = 0;
      double maxRatio= 0;
      
      for (ProductSource productSource : syncSources)
      {
         long id = productSource.getId();
         String url = productSource.getUrl();
         String login = productSource.getLogin();
         String password = productSource.getPassword();

         int code = onlineSource(url, login, password, id);
         if(code == 200)
         {
            Double result;
            Long syncId = productSynchronizer.getId();
            Double successFifteenMinutesRate = metricsService.getProdSyncSuccessFifteenMinutesRateBySync(syncId, id, new Timestamp(System.currentTimeMillis()));
            Double failureFifteenMinutesRate = metricsService.getProdSyncFailureFifteenMinutesRateBySync(syncId, id, new Timestamp(System.currentTimeMillis()));
            Double tranferSizeFifteenMinutesRate = metricsService.getProdSyncTransferSizeFiveMinutesRateBySync(syncId, id, new Timestamp(System.currentTimeMillis()));
            
            LOGGER.info("Source {} is online, successFifteenMinutesRate = {}, failureFifteenMinutesRate = {}, tranferSizeFifteenMinutesRate = {}", url, successFifteenMinutesRate, failureFifteenMinutesRate, tranferSizeFifteenMinutesRate);

            if ((successFifteenMinutesRate == null && failureFifteenMinutesRate == null) 
                  || (tranferSizeFifteenMinutesRate == null)
                  || sourceDelayIsOver(productSynchronizer, id))
            {
               LOGGER.info("No metrics or checking delay is over for source {}. Forcing ranking result to use it.", url);
               result = 1.1;
               resultMap.put(id, result);
            }
            else
            {
               if(successFifteenMinutesRate == null)
               {
                  successFifteenMinutesRate = (double) 0;
               }
               if(failureFifteenMinutesRate == null)
               {
                  failureFifteenMinutesRate = (double) 0;
               }

               Double ratio = (successFifteenMinutesRate / (successFifteenMinutesRate + failureFifteenMinutesRate));               
               ProductSourceParameters prodSourceParam = new ProductSourceParameters(tranferSizeFifteenMinutesRate, ratio);
               sourceMap.put(productSource, prodSourceParam);
               
               if (tranferSizeFifteenMinutesRate.compareTo(maxRate) > 0)
               {
                  maxRate = tranferSizeFifteenMinutesRate;
               }
               if (ratio.compareTo(maxRatio) > 0)
               {
                  maxRatio = ratio;
               }
            }
         }
         else
         {
            LOGGER.warn("The source {} is considered as offline, because receiving HTTP {} status code.", url, code);
         }
      }
      
      sourceMap.forEach((id,param)->{
         LOGGER.debug("SourceMap - Source {} - Param : [ Rate: {} - Ratio {} ]", id.getId(), param.getRate(), param.getRatio());
      });
      
      LOGGER.debug("MaxRate: {} - MaxRatio: {}", maxRate, maxRatio);

      for (Entry<ProductSource, ProductSourceParameters> entry : sourceMap.entrySet())
      {
         Double rate = entry.getValue().getRate();
         Double ratio = entry.getValue().getRatio();
         
         Double ratePercent = (maxRate == 0 ? 1 : rate / maxRate);
         Double ratioPercent = (maxRatio == 0 ? 1 : ratio / maxRatio);

         Double result = (ratePercent + ratioPercent) / 2;
         LOGGER.info("The ratio of the source '{}' is {}", entry.getKey().getUrl(), result);

         resultMap.put(entry.getKey().getId(), result);
      }
      
      Stream<Map.Entry<Long, Double>> sorted = resultMap.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()));
      sorted.forEachOrdered(s-> productSourceIds.add(s.getKey()));

      odataProdSync.setRankedSourceIds(productSourceIds);
   }

   //To check if the source delay is over
   private boolean sourceDelayIsOver(ProductSynchronizer productSynchronizer, long id)
   {
      Source source = getSourceConfiguration(productSynchronizer, id);
      if (source != null)
      {
         XMLGregorianCalendar sourceUsedDate = source.getLastDateSourceUsed();
         Instant dateCheckDelay = Instant.now().minus(productSynchronizer.getRetryingSourceDelay(), ChronoUnit.MINUTES);
         if (sourceUsedDate.compare(getGregorianCalendarFromInstant(dateCheckDelay)) < 0)
         {
            LOGGER.debug("Delay is over Source - {} : [lastDateSourceUsed {} - Date delay {}] ", id, sourceUsedDate, dateCheckDelay);
            return true;
         }
      }
      return false;
   }
   
   private XMLGregorianCalendar getGregorianCalendarFromInstant(Instant instant)
   {      
      Timestamp time = new Timestamp(instant.toEpochMilli());
      LocalDateTime ldt = time.toLocalDateTime();

      XMLGregorianCalendar cal;
      try
      {
         cal = DatatypeFactory.newInstance().newXMLGregorianCalendar();
         cal.setYear(ldt.getYear());
         cal.setMonth(ldt.getMonthValue());
         cal.setDay(ldt.getDayOfMonth());
         cal.setHour(ldt.getHour());
         cal.setMinute(ldt.getMinute());
         cal.setSecond(ldt.getSecond());
         String nanos = "0." + String.valueOf(ldt.getNano()).substring(0, 3);         
         cal.setFractionalSecond(new BigDecimal(nanos));
         cal.setTimezone(0);
         return cal;
      }
      catch (DatatypeConfigurationException e)
      {
         LOGGER.error("Error while updating LastCreationDate.", e);
      }
      return null;
   }   

   private int onlineSource(String url, String login, String password, long id)
   {
      try
      {
         ODataClient client = new ODataClient(url, login, password);
         return client.checkConnection(url);
      }
      catch (ODataException | URISyntaxException | IOException| InterruptedException e)
      {
         LOGGER.error("Unable to connect to the source {} ", url);
         return 400;
      }
   }

   private Source getSource(ProductSynchronizer productSynchronizer, ProductSource referencedSource )
   {
      List<Source> sources = productSynchronizer.getSources().getSource();
      for(Source source : sources)
      {
         if (source.getReferenceId() == referencedSource.getId())
         {
            return source;
         }
      }
      return null;
   }

   private class ProductSourceParameters
   {
      private Double rate;
      private Double ratio;
           
      public ProductSourceParameters(Double rate, Double ratio)
      {
         this.rate = rate;
         this.ratio = ratio;
      }
      public double getRate()
      {
         return rate;
      }

      public double getRatio()
      {
         return ratio;
      }    
   }  
}
