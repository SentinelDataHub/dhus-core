package org.dhus.olingo.v2.datamodel;

import java.util.Arrays;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.dhus.olingo.v2.web.DHuSODataServlet;

import fr.gael.odata.engine.model.EntityModel;

public class ProductSynchronizerModel extends SynchronizerModel
{

   public static final String ENTITY_TYPE_NAME = "ProductSynchronizer";
   public static final FullQualifiedName FULL_QUALIFIED_NAME
         = new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);
   
   public static final String PROPERTY_SYNC_OFFLINE = "SyncOfflineProducts";
   public static final String PROPERTY_COPY_PRODUCT = "CopyProduct";
   public static final String PROPERTY_FILTER_PARAM = "FilterParam";
   public static final String PROPERTY_GEO_FILTER = "GeoFilter";
   public static final String PROPERTY_SOURCE_COLLECTION = "SourceCollection";
   public static final String PROPERTY_LAST_CREATION_DATE = "LastCreationDate";
   public static final String PROPERTY_REQUEST = "Request";
   public static final String PROPERTY_SKIP_ON_ERROR = "SkipOnError";
   public static final String PROPERTY_FILTER_GEO = "GeoFilter";
   public static final String PROPERTY_REMOTE_INCOMING = "RemoteIncoming";
   public static final String PROPERTY_RETRIES_SKIPPED_PRODUCTS = "RetriesForSkippedProducts";
   public static final String PROPERTY_TIMEOUT_SKIPPED_PRODUCTS = "TimeoutSkippedProducts";
   
   @Override
   public CsdlEntityType getEntityType()
   {
      CsdlProperty syncOffline = new CsdlProperty()
            .setName(PROPERTY_SYNC_OFFLINE)
            .setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName())
            .setDefaultValue(Boolean.FALSE.toString());
      
      CsdlProperty skipOnError = new CsdlProperty()
            .setName(PROPERTY_SKIP_ON_ERROR)
            .setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName())
            .setDefaultValue(Boolean.TRUE.toString());
      
      CsdlProperty copyProduct = new CsdlProperty()
            .setName(PROPERTY_COPY_PRODUCT)
            .setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName())
            .setDefaultValue(Boolean.FALSE.toString());
      
      CsdlProperty remoteIncoming = new CsdlProperty()
            .setName(PROPERTY_REMOTE_INCOMING)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
      
      CsdlProperty filterParam = new CsdlProperty()
            .setName(PROPERTY_FILTER_PARAM)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
      
      CsdlProperty filterGeo = new CsdlProperty()
            .setName(PROPERTY_FILTER_GEO)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
      
      CsdlProperty sourceCollection = new CsdlProperty()
            .setName(PROPERTY_SOURCE_COLLECTION)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
      
      CsdlProperty retriesSkippedProducts = new CsdlProperty()
            .setName(PROPERTY_RETRIES_SKIPPED_PRODUCTS)
            .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName())
            .setDefaultValue("3");
      
      CsdlProperty timeoutSkippedProducts = new CsdlProperty()
            .setName(PROPERTY_TIMEOUT_SKIPPED_PRODUCTS)
            .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName())
            .setDefaultValue("60000");
      
      CsdlProperty lastCreationDate = new CsdlProperty()
            .setName(PROPERTY_LAST_CREATION_DATE)
            .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
            .setPrecision(3);
      
      
      return new CsdlEntityType()
            .setName(ENTITY_TYPE_NAME)
            .setBaseType(SynchronizerModel.ABSTRACT_FULL_QUALIFIED_NAME)
            .setProperties(Arrays.asList(syncOffline, skipOnError, copyProduct,
                  remoteIncoming, filterGeo, filterParam, sourceCollection,
                  lastCreationDate, retriesSkippedProducts, timeoutSkippedProducts));
   }
   
   @Override
   public String getName()
   {
      return ENTITY_TYPE_NAME;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return FULL_QUALIFIED_NAME;
   }
}
