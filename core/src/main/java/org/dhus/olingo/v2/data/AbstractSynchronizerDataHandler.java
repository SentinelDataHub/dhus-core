package org.dhus.olingo.v2.data;

import static fr.gael.odata.engine.data.DataHandlerUtil.containsProperty;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Property;
import org.dhus.olingo.v2.datamodel.CollectionModel;
import org.dhus.olingo.v2.datamodel.ReferencedSourceModel;
import org.dhus.olingo.v2.datamodel.SynchronizerModel;
import org.dhus.olingo.v2.datamodel.complex.CronComplexType;

import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer.Sources;
import fr.gael.dhus.database.object.config.synchronizer.Source;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerConfiguration;
import fr.gael.odata.engine.data.DataHandlerUtil;

public abstract class AbstractSynchronizerDataHandler
{
   private static final Integer DEFAULT_PAGE_SIZE = new Integer(2);
   
   private static final Logger LOGGER = LogManager.getLogger(AbstractSynchronizerDataHandler.class);

   public void updateSynchronizerProperties(Entity updatedEntity, SynchronizerConfiguration sync)
   {
      if (containsProperty(updatedEntity, SynchronizerModel.PROPERTY_LABEL))
      {
         sync.setLabel((String) DataHandlerUtil.getPropertyValue(updatedEntity,SynchronizerModel.PROPERTY_LABEL));
      }

      if (containsProperty(updatedEntity, SynchronizerModel.PROPERTY_PAGE_SIZE))
      {
         Long pageSize = (Long) DataHandlerUtil.getPropertyValue(updatedEntity,SynchronizerModel.PROPERTY_PAGE_SIZE);
         sync.setPageSize(pageSize == null || pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize.intValue());
      }
      
      extractCron(updatedEntity, sync);

      if (sync instanceof ProductSynchronizer)
      {
         ProductSynchronizer prodSync = (ProductSynchronizer) sync;
         Link sourceLink = updatedEntity.getNavigationLink(ReferencedSourceModel.ENTITY_SET_NAME);
         if (sourceLink != null)
         {
            EntityCollection sourceEntities = sourceLink.getInlineEntitySet();
            List<Entity> entities = sourceEntities.getEntities();
            List<Source> sourceList = new ArrayList<>();
            for (Entity entity : entities)
            {
               long id = (long) DataHandlerUtil.getPropertyValue(entity, ReferencedSourceModel.PROPERTY_REFERENCED_ID);
               String sourceCollection = (String) DataHandlerUtil.getPropertyValue(entity, ReferencedSourceModel.PROPERTY_SOURCE_COLLECTION);
               Object lastCreated = DataHandlerUtil.getPropertyValue(entity, ReferencedSourceModel.PROPERTY_LAST_CREATION_DATE);
               Object lastDateSourceUsed = DataHandlerUtil.getPropertyValue(entity, ReferencedSourceModel.PROPERTY_LAST_DATE_SOURCE_USED);

               // Check existing sources before add a new one
               ProductSynchronizer.Sources prodSyncSources = prodSync.getSources();
               ProductSynchronizer.Sources referencedSource = null;
               Source source = null;
               boolean alreadyPresent = false;
               if (prodSyncSources == null)
               {
                  referencedSource = new ProductSynchronizer.Sources();
               }
               else
               {
                  referencedSource = prodSyncSources;
                  // If the source is already in the list, check another source
                  List<Source> allSources = prodSyncSources.getSource();
                  for (int i = 0; i < allSources.size(); i++)
                  {
                     source = allSources.get(i);
                     if (source.getReferenceId() == id)
                     {
                        // The source is already present
                        alreadyPresent = true;
                        break;
                     }
                  }
               }
               if (!alreadyPresent)
               {
                  source = new Source();
                  source.setReferenceId(id);
               }
               
               if (sourceCollection != null)
               {
                  source.setSourceCollection(sourceCollection);
               }
               if (lastCreated != null)
               {
                  source.setLastCreationDate(getDateFromProperty(lastCreated));
               }
               if (lastDateSourceUsed != null)
               {
                  source.setLastDateSourceUsed(getDateFromProperty(lastDateSourceUsed));
               }
               sourceList.add(source);
               referencedSource.setSource(sourceList);
               prodSync.setSources(referencedSource);
            }
         }

         Link targetCollectionLink = updatedEntity.getNavigationLink(SynchronizerModel.NAVIGATION_TARGET_COLLECTION);
         if (targetCollectionLink != null)
         {
            EntityCollection targetCollection = targetCollectionLink.getInlineEntitySet();
            List<Entity> collectionEntities = targetCollection.getEntities();
            for (Entity entity : collectionEntities)
            {
            String name = (String) DataHandlerUtil.getPropertyValue(entity, CollectionModel.PROPERTY_NAME);
            prodSync.setTargetCollection(name);
            }
         } 
      }
      updateSpecificProperties(updatedEntity, sync);
   }
   
   private XMLGregorianCalendar getDateFromProperty(Object propertyValue)
   {      
      Timestamp time = (Timestamp) propertyValue;
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
         String nanos = ("0." + StringUtils.leftPad(String.valueOf(ldt.getNano()), 9, '0')).substring(0, 5);
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
   
   private void extractCron(Entity updatedEntity, SynchronizerConfiguration syncConf)
   {
      Property cronProperty = updatedEntity.getProperty(SynchronizerModel.PROPERTY_CRON);

      if (cronProperty != null && cronProperty.getValue() != null)
      {
         List<Property> cronProperties = ((ComplexValue) cronProperty.getValue()).getValue();
         for (Property property: cronProperties)
         {
            switch (property.getName())
            {
               case CronComplexType.PROPERTY_ACTIVE:
                  syncConf.setActive((Boolean) property.getValue());
                  break;

               case CronComplexType.PROPERTY_SCHEDULE:
                  syncConf.setSchedule((String) property.getValue());
                  break;
            }
         }
      }
   }

   protected abstract void updateSpecificProperties(Entity updatedEntity, SynchronizerConfiguration sync);
}
