package org.dhus.olingo.v2.entity;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.dhus.api.olingo.v2.TypeInfo;
import org.dhus.olingo.v2.datamodel.ProductSynchronizerModel;
import org.dhus.olingo.v2.datamodel.SmartSynchronizerModel;

import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerConfiguration;

@TypeInfo(type = ProductSynchronizer.class, baseType = SynchronizerConfiguration.class)
public class ProductSynchronizerEntityProducer<I extends ProductSynchronizer> extends SynchronizerEntityProducer<I>
{

   @Override
   public Entity transform(I prodSync)
   {
      Entity sync = super.transform(prodSync);

      sync.setType(ProductSynchronizerModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());

      sync.addProperty(new Property(
            null, 
            ProductSynchronizerModel.PROPERTY_SYNC_OFFLINE, 
            ValueType.PRIMITIVE,
            prodSync.isSyncOfflineProducts()));

      sync.addProperty(new Property(
            null, 
            ProductSynchronizerModel.PROPERTY_COPY_PRODUCT, 
            ValueType.PRIMITIVE,
            prodSync.isCopyProduct()));

      sync.addProperty(new Property(null, 
            ProductSynchronizerModel.PROPERTY_SKIP_ON_ERROR, 
            ValueType.PRIMITIVE,
            prodSync.isSkipOnError()));

      String geofilter = null;
      String operation = prodSync.getGeofilterOp();
      String shape = prodSync.getGeofilterShape();
      if (operation != null && !operation.isEmpty() && shape != null && !shape.isEmpty())
      {
         geofilter = String.format("%s %s", operation, shape);
      }
      sync.addProperty(new Property(
            null,
            SmartSynchronizerModel.PROPERTY_FILTER_GEO,
            ValueType.PRIMITIVE,
            geofilter));

      sync.addProperty(new Property(null, 
            ProductSynchronizerModel.PROPERTY_FILTER_PARAM, 
            ValueType.PRIMITIVE,
            prodSync.getFilterParam()));

      sync.addProperty(new Property(null, 
            ProductSynchronizerModel.PROPERTY_RETRIES_SKIPPED_PRODUCTS, 
            ValueType.PRIMITIVE,
            prodSync.getRetriesForSkippedProducts()));
      
      sync.addProperty(new Property(null, 
            ProductSynchronizerModel.PROPERTY_TIMEOUT_SKIPPED_PRODUCTS, 
            ValueType.PRIMITIVE,
            prodSync.getTimeoutSkippedProducts()));
     
      sync.addProperty(new Property(
            null,
            ProductSynchronizerModel.PROPERTY_RANKING_SCHEDULE,
            ValueType.PRIMITIVE,
            prodSync.getRankingSchedule()));
      
      sync.addProperty(new Property(
            null,
            ProductSynchronizerModel.PROPERTY_RETRYING_SOURCE_DELAY,
            ValueType.PRIMITIVE,
            prodSync.getRetryingSourceDelay()));

      return sync;
   }
}
