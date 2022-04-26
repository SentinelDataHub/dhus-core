package org.dhus.olingo.v2.data;

import static fr.gael.odata.engine.data.DataHandlerUtil.containsProperty;

import java.util.List;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.dhus.olingo.v2.datamodel.SynchronizerModel;
import org.dhus.olingo.v2.datamodel.complex.CronComplexType;

import fr.gael.dhus.database.object.config.synchronizer.SynchronizerConfiguration;
import fr.gael.odata.engine.data.DataHandlerUtil;

public abstract class AbstractSynchronizerDataHandler
{
   private static final Integer DEFAULT_PAGE_SIZE = new Integer(2);

   public void updateSynchronizerProperties(Entity updatedEntity, SynchronizerConfiguration sync)
   {
      if (containsProperty(updatedEntity, SynchronizerModel.PROPERTY_LABEL))
      {
         sync.setLabel((String) DataHandlerUtil.getPropertyValue(updatedEntity,SynchronizerModel.PROPERTY_LABEL));
      }
      
      if (containsProperty(updatedEntity, SynchronizerModel.PROPERTY_SERVICE_LOGIN))
      {
         sync.setServiceLogin((String) DataHandlerUtil.getPropertyValue(updatedEntity,SynchronizerModel.PROPERTY_SERVICE_LOGIN));
      }
      
      if (containsProperty(updatedEntity, SynchronizerModel.PROPERTY_SERVICE_PASSWD))
      {
         sync.setServicePassword((String) DataHandlerUtil.getPropertyValue(updatedEntity,SynchronizerModel.PROPERTY_SERVICE_PASSWD));
      }
      
      if (containsProperty(updatedEntity, SynchronizerModel.PROPERTY_SERVICE_URL))
      {
         sync.setServiceUrl((String) DataHandlerUtil.getPropertyValue(updatedEntity,SynchronizerModel.PROPERTY_SERVICE_URL));
      }
      
      if (containsProperty(updatedEntity, SynchronizerModel.PROPERTY_PAGE_SIZE))
      {
         Long pageSize = (Long) DataHandlerUtil.getPropertyValue(updatedEntity,SynchronizerModel.PROPERTY_PAGE_SIZE);
         sync.setPageSize(pageSize == null || pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize.intValue());
      }
      
      extractCron(updatedEntity, sync);
      updateSpecificProperties(updatedEntity, sync);
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
