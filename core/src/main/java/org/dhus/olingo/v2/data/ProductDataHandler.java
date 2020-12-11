/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2020 GAEL Systems
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
package org.dhus.olingo.v2.data;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.gml2.GMLReader;

import fr.gael.dhus.database.object.Order;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.Product.Download;
import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.Transformation;
import fr.gael.dhus.service.EvictionService;
import fr.gael.dhus.service.OrderService;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.service.TransformationService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.init.WorkingDirectory;

import fr.gael.odata.engine.data.DataHandlerUtil;
import fr.gael.odata.engine.data.DatabaseDataHandler;
import fr.gael.odata.engine.processor.MediaResponseBuilder;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.GeoUtils;
import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.edm.EdmFunction;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.geo.Geospatial;
import org.apache.olingo.commons.api.edm.geo.LineString;
import org.apache.olingo.commons.api.edm.geo.Point;
import org.apache.olingo.commons.api.edm.geo.Polygon;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;

import org.dhus.api.transformation.TransformationException;

import org.dhus.olingo.v2.ODataSecurityManager;
import org.dhus.olingo.v2.datamodel.JobModel;
import org.dhus.olingo.v2.datamodel.DataStoreModel;
import org.dhus.olingo.v2.datamodel.ItemModel;
import org.dhus.olingo.v2.datamodel.NodeModel;
import org.dhus.olingo.v2.datamodel.OrderModel;
import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.olingo.v2.datamodel.TransformationModel;
import org.dhus.olingo.v2.datamodel.action.OrderProductAction;
import org.dhus.olingo.v2.datamodel.action.RepairProductAction;
import org.dhus.olingo.v2.datamodel.action.RepairProductsAction;
import org.dhus.olingo.v2.datamodel.action.RunTransformerAction;
import org.dhus.olingo.v2.datamodel.action.TransformProductAction;
import org.dhus.olingo.v2.datamodel.complex.ChecksumComplexType;
import org.dhus.olingo.v2.datamodel.complex.ResourceLocationComplexType;
import org.dhus.olingo.v2.datamodel.complex.TimeRangeComplexType;
import org.dhus.olingo.v2.datamodel.function.EvictionDateFunction;
import org.dhus.olingo.v2.datamodel.function.ResourceLocationFunction;
import org.dhus.olingo.v2.entity.TypeStore;
import org.dhus.olingo.v2.visitor.ProductSQLVisitor;
import org.dhus.store.StoreException;
import org.dhus.store.StoreService;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.DataStoreManager;
import org.dhus.store.datastore.DataStoreProduct;
import org.dhus.store.datastore.ProductNotFoundException;
import org.dhus.store.datastore.async.AsyncDataStoreException;
import org.dhus.store.datastore.async.AsyncProduct;
import org.dhus.store.derived.DerivedProductStore;
import org.dhus.store.derived.DerivedProductStoreService;
import org.dhus.store.ingestion.IngestibleRawProduct;
import org.dhus.store.ingestion.ProcessingManager;
import org.dhus.store.quota.QuotaException;
import org.dhus.transformation.TransformationManager;
import org.dhus.transformation.TransformationQuotasException;

import org.xml.sax.SAXException;

/**
 * Provides data for Product entities.
 */
public class ProductDataHandler implements DatabaseDataHandler
{
   private static final DataStoreManager DATASTORE_MANAGER =
         ApplicationContextProvider.getBean(DataStoreManager.class);

   private static final DerivedProductStoreService DERIVED_PS_SERVICE =
         ApplicationContextProvider.getBean(DerivedProductStoreService.class);

   private static final StoreService STORE_SERVICE =
         ApplicationContextProvider.getBean(StoreService.class);

   private static final ProductService PRODUCT_SERVICE =
         ApplicationContextProvider.getBean(ProductService.class);

   private static final EvictionService EVICTION_SERVICE =
         ApplicationContextProvider.getBean(EvictionService.class);

   private static final TransformationManager TR_MANAGER =
         ApplicationContextProvider.getBean(TransformationManager.class);

   private static final TransformationService TR_SERVICE =
         ApplicationContextProvider.getBean(TransformationService.class);

   private static final OrderService ORDER_SERVICE =
         ApplicationContextProvider.getBean(OrderService.class);

   private final TypeStore typeStore;

   private static final Logger LOGGER = LogManager.getLogger(ProductDataHandler.class);

   public static final String QUICKLOOK_ID = "Quicklook";
   public static final String THUMBNAIL_ID = "Thumbnail";
   // xpath_attributes is a set of xpath returning nodes (ClasscastException
   // overwise)
   public static String[] image_xpath_attributes =
   {
      "image/FormatName",
      "image/directory/Width",
      "image/directory/Height",
      "image/directory/NumBands"
   };

   private final GMLReader gmlReader = new GMLReader();

   public ProductDataHandler(TypeStore typeStore)
   {
      this.typeStore = typeStore;
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      for (UriParameter uriParameter: keyParameters)
      {
         if (ItemModel.PROPERTY_ID.equals(uriParameter.getName()))
         {
            String uuid = DataHandlerUtil.trimStringKeyParameter(uriParameter);
            Product product = PRODUCT_SERVICE.getProduct(uuid);
            return toProductEntity(product);
         }
      }
      return null;
   }

   /** Create a ODataEntity using the DAO Product. */
   private Entity toProductEntity(Product product)
   {
      if (product == null)
      {
         return null;
      }

      Entity productEntity = new Entity();

      // UUID
      productEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_ID,
            ValueType.PRIMITIVE,
            product.getUuid()));

      // Name
      productEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_NAME,
            ValueType.PRIMITIVE,
            product.getIdentifier()));

      // PublicationDate
      productEntity.addProperty(new Property(
            null,
            ProductModel.PROPERTY_CREATIONDATE,
            ValueType.PRIMITIVE,
            new Timestamp(product.getCreated().getTime())));

      // IngestionDate
      productEntity.addProperty(new Property(
            null,
            ProductModel.PROPERTY_INGESTIONDATE,
            ValueType.PRIMITIVE,
            new Timestamp(product.getIngestionDate().getTime())));

      // ModificationDate
      productEntity.addProperty(new Property(
            null,
            ProductModel.PROPERTY_MODIFICATIONDATE,
            ValueType.PRIMITIVE,
            new Timestamp(product.getUpdated().getTime())));

      // Value
      productEntity.addProperty(new Property(
            null,
            NodeModel.PROPERTY_VALUE,
            ValueType.PRIMITIVE,
            null));

      // Geography data
      productEntity.addProperty(new Property(
            null,
            ProductModel.PROPERTY_FOOTPRINT,
            ValueType.PRIMITIVE,
            parseFootprint(product.getFootPrint())));

      // ContentLength
      productEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_CONTENTLENGTH,
            ValueType.PRIMITIVE,
            product.getSize()));

      // ContentType
      productEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_CONTENTTYPE,
            ValueType.PRIMITIVE,
            fr.gael.dhus.database.object.Product.DEFAULT_CONTENT_TYPE));

      // ChildrenCount
      productEntity.addProperty(new Property(
            null,
            NodeModel.PROPERTY_CHILDREN_COUNT,
            ValueType.PRIMITIVE,
            DERIVED_PS_SERVICE.getDefaultDerivedProdutCount(product.getUuid())));

      // Online
      productEntity.addProperty(new Property(
            null,
            ProductModel.PROPERTY_ONLINE,
            ValueType.PRIMITIVE,
            product.isOnline()));

      // OnDemand
      productEntity.addProperty(new Property(
            null,
            ProductModel.PROPERTY_ONDEMAND,
            ValueType.PRIMITIVE,
            product.isOnDemand()));

      // Checksum
      productEntity.addProperty(checksumProperty(product.getDownload()));

      // TimeRange (Begin-End positions)
      productEntity.addProperty(timeRangeProperty(product.getContentStart(), product.getContentEnd()));

      productEntity.setId(DataHandlerUtil.createEntityId(ProductModel.ENTITY_SET_NAME, product.getUuid()));
      productEntity.setType(ProductModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());
      productEntity.setMediaContentType(fr.gael.dhus.database.object.Product.DEFAULT_CONTENT_TYPE);

      return productEntity;
   }

   private DerivedProductEntity toDerivedProductEntity(Product product)
   {
      if (product == null)
      {
         return null;
      }

      DerivedProductEntity productEntity = new DerivedProductEntity(product.getUuid());

      // Name
      productEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_NAME,
            ValueType.PRIMITIVE,
            product.getIdentifier()));

      // PublicationDate
      productEntity.addProperty(new Property(
            null,
            ProductModel.PROPERTY_CREATIONDATE,
            ValueType.PRIMITIVE,
            new Timestamp(product.getCreated().getTime())));

      // IngestionDate
      productEntity.addProperty(new Property(
            null,
            ProductModel.PROPERTY_INGESTIONDATE,
            ValueType.PRIMITIVE,
            new Timestamp(product.getIngestionDate().getTime())));

      // ModificationDate
      productEntity.addProperty(new Property(
            null,
            ProductModel.PROPERTY_MODIFICATIONDATE,
            ValueType.PRIMITIVE,
            new Timestamp(product.getUpdated().getTime())));

      // Value
      productEntity.addProperty(new Property(
            null,
            NodeModel.PROPERTY_VALUE,
            ValueType.PRIMITIVE,
            null));

      // Geography data
      productEntity.addProperty(new Property(
            null,
            ProductModel.PROPERTY_FOOTPRINT,
            ValueType.PRIMITIVE,
            parseFootprint(product.getFootPrint())));

      // ChildrenCount
      productEntity.addProperty(new Property(
            null,
            NodeModel.PROPERTY_CHILDREN_COUNT,
            ValueType.PRIMITIVE,
            0));

      // Online
      productEntity.addProperty(new Property(
            null,
            ProductModel.PROPERTY_ONLINE,
            ValueType.PRIMITIVE,
            product.isOnline()));

      // Checksum
      productEntity.addProperty(new Property(
            null,
            ProductModel.PROPERTY_CHECKSUM,
            ValueType.COMPLEX,
            null));

      // OnDemand
      productEntity.addProperty(new Property(
            null,
            ProductModel.PROPERTY_ONDEMAND,
            ValueType.PRIMITIVE,
            product.isOnDemand()));

      // TODO display GML as string as well?
      productEntity.addProperty(timeRangeProperty(product.getContentStart(), product.getContentEnd()));
      productEntity.setMediaContentType(fr.gael.dhus.database.object.Product.DEFAULT_CONTENT_TYPE);

      return productEntity;
   }

   private DerivedProductEntity toQuicklookEntity(Product product)
   {
      DerivedProductEntity productEntity = toDerivedProductEntity(product);

      // ID
      productEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_ID,
            ValueType.PRIMITIVE,
            QUICKLOOK_ID));

      // ContentType
      productEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_CONTENTTYPE,
            ValueType.PRIMITIVE,
            getDerivedProductContentType(
                  product.getUuid(),
                  DerivedProductStoreService.QUICKLOOK_TAG)));

      // ContentLength
      productEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_CONTENTLENGTH,
            ValueType.PRIMITIVE,
            product.getQuicklookSize()));

      // Set Id
      productEntity.setId(DataHandlerUtil.createEntityId(
            ProductModel.ENTITY_SET_NAME,
            QUICKLOOK_ID));

      return productEntity;
   }

   private DerivedProductEntity toThumbnailEntity(Product product)
   {
      DerivedProductEntity productEntity = toDerivedProductEntity(product);

      // ID
      productEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_ID,
            ValueType.PRIMITIVE,
            THUMBNAIL_ID));

      // ContentType
      productEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_CONTENTTYPE,
            ValueType.PRIMITIVE,
            getDerivedProductContentType(
                  product.getUuid(),
                  DerivedProductStoreService.THUMBNAIL_TAG)));

      // ContentLength
      productEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_CONTENTLENGTH,
            ValueType.PRIMITIVE,
            product.getThumbnailSize()));

      // Set Id
      productEntity.setId(DataHandlerUtil.createEntityId(
            ProductModel.ENTITY_SET_NAME,
            THUMBNAIL_ID));

      return productEntity;
   }

   private Polygon parseFootprint(String footprint)
   {
      if (footprint == null)
      {
         return null;
      }

      Geometry geometry;
      try
      {
         // since this is a GML footprint, lon/lat is inverted
         geometry = gmlReader.read(footprint, null);
      }
      catch (SAXException | IOException | ParserConfigurationException e)
      {
         LOGGER.debug("Failed to parse footprint", e);
         return null;
      }

      List<Point> exteriorPoints = Arrays.asList(geometry.getCoordinates()).stream()
            .map(coordinate ->
            {
               Point point = new Point(GeoUtils.getDimension(EdmPrimitiveTypeKind.GeographyPoint), null);

               //!\ invert lon/lat (gml to wkt)
               point.setX(coordinate.y);
               point.setY(coordinate.x);

               point.setZ(coordinate.z);
               return point;
            }).collect(Collectors.toList());

      Geospatial.Dimension dimension = GeoUtils.getDimension(EdmPrimitiveTypeKind.GeographyPolygon);

      return new Polygon(
            dimension,
            null,
            Collections.emptyList(), // no interior
            new LineString(dimension, null, exteriorPoints));
   }

   private Property checksumProperty(Download download)
   {
      List<ComplexValue> checksumComplexCollection = new ArrayList<>();
      if (download != null)
      {
         Map<String, String> checksumsMap = download.getChecksums();
         for (Entry<String, String> checksum: checksumsMap.entrySet())
         {
            ComplexValue checksumValue = new ComplexValue();
            // checksum algorithm
            checksumValue.getValue().add(new Property(
                  null,
                  ChecksumComplexType.PROPERTY_ALGORITHM,
                  ValueType.PRIMITIVE,
                  checksum.getKey()));

            // checksum value
            checksumValue.getValue().add(new Property(
                  null,
                  ChecksumComplexType.PROPERTY_VALUE,
                  ValueType.PRIMITIVE,
                  checksum.getValue()));

            checksumComplexCollection.add(checksumValue);
         }

         return new Property(
               null,
               ChecksumComplexType.COMPLEX_TYPE_NAME,
               ValueType.COLLECTION_COMPLEX,
               checksumComplexCollection);
      }
      return null;
   }

   private Property timeRangeProperty(Date contentStart, Date contentEnd)
   {
      ComplexValue complexValue = new ComplexValue();
      complexValue.getValue().add(new Property(
            null,
            TimeRangeComplexType.PROPERTY_START,
            ValueType.PRIMITIVE,
            new Timestamp(contentStart.getTime())));
      complexValue.getValue().add(new Property(
            null,
            TimeRangeComplexType.PROPERTY_END,
            ValueType.PRIMITIVE,
            new Timestamp(contentEnd.getTime())));

      return new Property(
            null,
            ProductModel.PROPERTY_CONTENTDATE,
            ValueType.COMPLEX,
            complexValue);
   }

   @Override
   public EntityCollection getRelatedEntityCollectionData(Entity sourceEntity, EdmNavigationProperty edmNavigationProperty)
         throws ODataApplicationException
   {
      String entityType = sourceEntity.getType();
      // No product available from derived product
      if (sourceEntity instanceof DerivedProductEntity)
      {
         throw new ODataApplicationException("Invalid navigation from derived product to "
               + ProductModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString(),
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
      else if (DataStoreModel.isDataStoreSubType(entityType))
      {
         return getDataStoreNavLink(sourceEntity);
      }
      else if (sourceEntity.getType().equals(ProductModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         return getProductsNavLink(sourceEntity);
      }
      return null; // ?
   }

   private EntityCollection getDataStoreNavLink(Entity sourceEntity) throws ODataApplicationException
   {
      EntityCollection navigationTargetEntityCollection = new EntityCollection();
      String dataStoreName = (String) sourceEntity.getProperty(DataStoreModel.PROPERTY_NAME).getValue();

      List<DataStore> dataStore = DATASTORE_MANAGER.list();
      for (int i = 0; i < dataStore.size(); i++)
      {
         // retrieve list of product UUIDs from datastore of given name
         if (dataStore.get(i).getName().equals(dataStoreName))
         {
            List<String> productList = dataStore.get(i).getProductList();

            // make entities
            for (String uuid: productList)
            {
               Product product = PRODUCT_SERVICE.getProduct(uuid);
               Entity productEntity = toProductEntity(product);
               navigationTargetEntityCollection.getEntities().add(productEntity);
            }
         }
      }
      return navigationTargetEntityCollection;
   }

   private EntityCollection getProductsNavLink(Entity sourceEntity)
   {
      EntityCollection navigationTargetEntityCollection = new EntityCollection();

      String productUuid = (String) sourceEntity.getProperty(ItemModel.PROPERTY_ID).getValue();
      Product product = PRODUCT_SERVICE.getProduct(productUuid);

      if (DERIVED_PS_SERVICE.hasDerivedProduct(productUuid, DerivedProductStoreService.QUICKLOOK_TAG))
      {
         navigationTargetEntityCollection.getEntities().add(toQuicklookEntity(product));
      }
      if (DERIVED_PS_SERVICE.hasDerivedProduct(productUuid, DerivedProductStoreService.THUMBNAIL_TAG))
      {
         navigationTargetEntityCollection.getEntities().add(toThumbnailEntity(product));
      }
      return navigationTargetEntityCollection;
   }

   @Override
   public Entity getRelatedEntityData(Entity entity, List<UriParameter> navigationKeyParameters, EdmNavigationProperty edmNavigationProperty)
         throws ODataApplicationException
   {
      String productUuid = (String) entity.getProperty(ItemModel.PROPERTY_ID).getValue();

      Product product = PRODUCT_SERVICE.getProduct(productUuid);
      for (UriParameter keyParameter: navigationKeyParameters)
      {
         if (keyParameter.getName().equals(ItemModel.PROPERTY_ID))
         {
            String keyParameterValue = DataHandlerUtil.trimStringKeyParameter(keyParameter);
            if (keyParameterValue.equals(QUICKLOOK_ID)
                  && DERIVED_PS_SERVICE.hasDerivedProduct(productUuid, DerivedProductStoreService.QUICKLOOK_TAG))
            {
               return toQuicklookEntity(product);
            }
            if (keyParameterValue.equals(THUMBNAIL_ID)
                  && DERIVED_PS_SERVICE.hasDerivedProduct(productUuid, DerivedProductStoreService.THUMBNAIL_TAG))
            {
               return toThumbnailEntity(product);
            }
            return null;
         }
      }
      return null;
   }

   @Override
   public Entity getRelatedEntityData(Entity entity, EdmNavigationProperty edmNavigationProperty)
         throws ODataApplicationException
   {
      String type = entity.getType();
      if (TransformationModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString().equals(type))
      {
         String transformationId = (String) entity.getProperty(JobModel.PROPERTY_ID).getValue();
         Transformation transformation = TR_SERVICE.getTransformation(transformationId);
         Product product = PRODUCT_SERVICE.systemGetProduct(transformation.getProductOut());
         if (product == null)
         {
            throw new ODataApplicationException("Transformed product not found",
                  HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
         }
         return toProductEntity(product);
      }
      else if (OrderModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString().equals(type))
      {
         String odataOrderId = (String) entity.getProperty(JobModel.PROPERTY_ID).getValue();
         String productUuid = OrderDataHandler.extractUuidFromOrderId(odataOrderId);

         Order order = ORDER_SERVICE.getOrderByProductUuid(productUuid);
         String orderId = OrderDataHandler.makeOrderId(order);

         if (odataOrderId.equals(orderId))
         {
            Product product = PRODUCT_SERVICE.getProduct(productUuid);
            return toProductEntity(product);
         }

         throw new ODataApplicationException("Product order not found",
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
      throw new ODataApplicationException("Invalid navigation from " + type + " to "
            + ProductModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString(),
            HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);

   }

   @Override
   public EntityCollection getEntityCollectionData(FilterOption filterOption, OrderByOption orderByOption,
         TopOption topOption, SkipOption skipOption, CountOption countOption)
         throws ODataApplicationException
   {
      // Retrieve data applying the different options
      ProductSQLVisitor productSqlVisitor = new ProductSQLVisitor(filterOption, orderByOption, topOption, skipOption);
      // FIXME full list in memory: paginate or stream
      List<Product> productsCollection = PRODUCT_SERVICE.getProducts(productSqlVisitor, null);

      EntityCollection entityCollection = new EntityCollection();
      for (Product product: productsCollection)
      {
         Entity entityProduct = toProductEntity(product);
         entityCollection.getEntities().add(entityProduct);
      }

      // handle count, must ignore skip & top
      if (countOption != null && countOption.getValue())
      {
         // FIXME unnecessary extra request to database?
         entityCollection.setCount(countEntities(filterOption));
      }

      return entityCollection;
   }

   @Override
   public Integer countEntities(FilterOption filterOption) throws ODataApplicationException
   {
      return PRODUCT_SERVICE.countProducts(new ProductSQLVisitor(filterOption, null, null, null), null);
   }

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      return getEntityCollectionData(null, null, null, null, null);
   }

   @Override
   public Property performBoundFunction(List<UriParameter> keyPredicates, EdmFunction function, Map<String, Parameter> parameters)
         throws ODataApplicationException
   {
      // TODO support derived products
      Product product = getProduct(keyPredicates);
      ODataSecurityManager.checkPermission(Role.AUTHED);
      if (function.getFullQualifiedName().equals(ResourceLocationFunction.FUNCTION_RESOURCE_LOCATION_FNQ))
      {
         try
         {
            Map<String, String> locations = DATASTORE_MANAGER.getResourceLocations(product.getUuid());
            return makeResourceLocationsProperty(locations);
         }
         catch (DataStoreException e)
         {
            throw new ODataApplicationException(e.getMessage(),
                  HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
         }
      }
      else if (function.getFullQualifiedName().equals(EvictionDateFunction.FUNCTION_EVICTION_DATE_FNQ))
      {
         Date evictionDate = EVICTION_SERVICE.getEvictionDate(product);
         return new Property(
               null,
               EvictionDateFunction.FUNCTION_EVICTION_DATE,
               ValueType.PRIMITIVE,
               evictionDate == null ? null : new Timestamp(evictionDate.getTime()));
      }
      return null;
   }

   @Override
   public Object performBoundAction(List<UriParameter> keyPredicates, EdmAction action, Map<String, Parameter> parameters)
         throws ODataApplicationException
   {
      // single product repair
      if (action.getFullQualifiedName().equals(RepairProductAction.ACTION_REPAIR_PRODUCT_FQN))
      {
         ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);
         return singleProductRepairAction(keyPredicates);
      }
      // multi product repair
      else if (action.getFullQualifiedName().equals(RepairProductsAction.ACTION_REPAIR_PRODUCTS_FQN))
      {
         ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);
         return multiProductRepairAction(parameters);
      }
      // product transformation
      else if (action.getFullQualifiedName().equals(TransformProductAction.FULL_QUALIFIED_NAME))
      {
         return transformProductAction(keyPredicates, parameters);
      }
      // product order
      else if (action.getFullQualifiedName().equals(OrderProductAction.ACTION_ORDER_PRODUCT_FQN))
      {
         return orderProductAction(keyPredicates);
      }
      return null;
   }

   private Property singleProductRepairAction(List<UriParameter> keyPredicates)
         throws ODataApplicationException
   {
      String productUuid = DataHandlerUtil
            .getSingleStringKeyParameterValue(keyPredicates, ItemModel.PROPERTY_ID);

      try
      {
         STORE_SERVICE.repairProduct(productUuid);
         return new Property(null, "Success", ValueType.PRIMITIVE, "Successfully repaired product " + productUuid);
      }
      catch (ProductNotFoundException e)
      {
         LOGGER.info("Product {} does not exist, cannot repair", productUuid);
         throw new ODataApplicationException("Product not found, cannot repair",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }
      catch (StoreException e)
      {
         LOGGER.error("Cannot repair product {}: ", productUuid, e);
         throw new ODataApplicationException("Cannot repair product: " + e.getMessage(),
               HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
      }

   }

   private Property multiProductRepairAction(Map<String, Parameter> parameters)
         throws ODataApplicationException
   {
      String filter = (String) parameters.get(RepairProductsAction.PARAMETER_FILTER).asPrimitive();
      String orderBy =
            (String) parameters.get(RepairProductsAction.PARAMETER_ORDERBY).asPrimitive();
      Integer maxRepaired =
            (Integer) parameters.get(RepairProductsAction.PARAMETER_MAXREP).asPrimitive();
      Integer skip =
            (Integer) parameters.get(RepairProductsAction.PARAMETER_SKIP).asPrimitive();

      int repairedProducts;
      try
      {
         repairedProducts = STORE_SERVICE.repairProducts(filter, orderBy,
               (maxRepaired == null ? Integer.MAX_VALUE : maxRepaired),
               skip == null ? 0 : skip);
      }
      catch (StoreException e)
      {
         LOGGER.error("Cannot repair products", e);
         throw new ODataApplicationException("Cannot repair products: " + e.getMessage(),
               HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
      }

      return new Property(null, "Success", ValueType.PRIMITIVE, "Repaired " + repairedProducts + " products");
   }

   private Entity transformProductAction(List<UriParameter> keyPredicates, Map<String, Parameter> parameters)
         throws ODataApplicationException
   {
      String username = ODataSecurityManager.getCurrentUser().getUsername();
      String productUuid = DataHandlerUtil.getSingleStringKeyParameterValue(keyPredicates, ItemModel.PROPERTY_ID);

      try
      {
         Transformation transformation = TR_MANAGER.transform(
               (String) parameters.get(TransformProductAction.PARAM_TRANSFORMER_NAME).getValue(),
               productUuid,
               Util.parametersToMap(parameters.get(RunTransformerAction.PARAM_PARAMETERS)));

         return typeStore.get(Transformation.class).getEntityProducer()
               .toOlingoEntity(transformation);
      }
      catch (TransformationQuotasException e)
      {
         throw new ODataApplicationException("Transformation quota exceeded: " + e.getMessage(), 429, Locale.ENGLISH);
      }
      catch (TransformationException e)
      {
         Product product = PRODUCT_SERVICE.getProduct(productUuid);
         LOGGER.info("Failed to submit transformation for request from '{}' on product {} ({}) (~{} bytes), invalid request: {}",
               username,
               product != null ? product.getIdentifier() : "UNKNOWN",
               product != null ? product.getUuid() : "UNKNOWN",
               product != null ? product.getSize() : "UNKNOWN",
               e.getMessage());

         throw new ODataApplicationException("Invalid Transformation request: " + e.getMessage(),
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
      catch (ProductNotFoundException e)
      {
         LOGGER.info("Failed to submit transformation for request from '{}', product {} not found",
               username, productUuid);
         throw new ODataApplicationException("Product not found",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }
   }

   private Entity orderProductAction(List<UriParameter> keyPredicates)
         throws ODataApplicationException
   {
      String uuid = DataHandlerUtil.trimStringKeyParameter(keyPredicates.get(0));
      try
      {
         org.dhus.Product product = DATASTORE_MANAGER.get(uuid);

         if (product instanceof AsyncProduct)
         {
            // trigger LTA order for this product
            Order order = ((AsyncProduct) product).asyncFetchData();
            ORDER_SERVICE.addOwner(order, ODataSecurityManager.getCurrentUser());
            return OrderDataHandler.toOlingoEntity(order);
         }
         else
         {
            throw new ODataApplicationException("Product already online",
                  HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
         }
      }
      catch (AsyncDataStoreException e)
      {
         throw new ODataApplicationException(e.getUserMessage(), e.getHttpStatusCode(), Locale.ENGLISH);
      }
      catch (DataStoreException e)
      {
         if (e.getCause() instanceof QuotaException)
         {
            throw new ODataApplicationException(e.getMessage(), 429, Locale.ENGLISH);
         }
         LOGGER.error("An error occured while getting the product", e);
         throw new ODataApplicationException(e.getMessage(), 500, Locale.ENGLISH);
      }
   }

   private Product getProduct(List<UriParameter> keyPredicates) throws ODataApplicationException
   {
      Product product = null;
      for (UriParameter key: keyPredicates)
      {
         if (key.getName().equals(ItemModel.PROPERTY_ID))
         {
            product = PRODUCT_SERVICE.systemGetProduct(DataHandlerUtil.trimStringKeyParameter(key));
            if (product == null)
            {
               throw new ODataApplicationException("Product not found",
                     HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
            }
         }
      }
      return product;
   }

   private Property makeResourceLocationsProperty(Map<String, String> locations)
   {
      return new Property(
            null,
            ResourceLocationComplexType.COMPLEX_TYPE_NAME,
            ValueType.COLLECTION_COMPLEX,
            locations.entrySet().stream().map(entry ->
            {
               ComplexValue complex = new ComplexValue();
               complex.getValue().add(new Property(
                     null,
                     ResourceLocationComplexType.PROPERTY_DATASTORE_NAME,
                     ValueType.PRIMITIVE,
                     entry.getKey()));
               complex.getValue().add(new Property(
                     null,
                     ResourceLocationComplexType.PROPERTY_LOCATION,
                     ValueType.PRIMITIVE,
                     entry.getValue()));

               return complex;
            }).collect(Collectors.toList()));
   }

   private String getDerivedProductContentType(String uuid, String tag)
   {
      try
      {
         String resourceLocation = DERIVED_PS_SERVICE.getDerivedProduct(uuid, tag).getResourceLocation();
         if (resourceLocation.toLowerCase().endsWith("gif"))
         {
            return "image/gif";
         }
         else
         {
            return "image/jpeg";
         }
      }
      catch (StoreException e)
      {
         return null; // ?
      }
   }

   @Override
   public void prepareResponseForDownload(ODataRequest request, ODataResponse response, Entity entity) throws ODataApplicationException
   {
      String productUuid = (String) entity.getProperty(ItemModel.PROPERTY_ID).getValue();

      // Checksum
      String checksumValue = extractChecksum(entity);

      // PublicationDate
      Date publicationDate = (Date) entity.getProperty(ProductModel.PROPERTY_CREATIONDATE).getValue();

      // ContentType
      String contentType = (String) entity.getProperty(ItemModel.PROPERTY_CONTENTTYPE).getValue();

      // ContentLength
      long contentLength = (long) entity.getProperty(ItemModel.PROPERTY_CONTENTLENGTH).getValue();

      // Download Physical Derived Products
      DataStoreProduct data = null;
      if (entity instanceof DerivedProductEntity)
      {
         DerivedProductEntity derivedProductEntity = (DerivedProductEntity) entity;
         try
         {
            String parentUuid = derivedProductEntity.getParentUuid();
            data = getPhysicalDerivedProduct(parentUuid);
            if (entity.getProperty(ItemModel.PROPERTY_ID).getValue().equals(QUICKLOOK_ID))
            {
               // QUICKLOOK Products
               MediaResponseBuilder.prepareMediaResponse(
                     null,
                     data.getName(),
                     contentType,
                     publicationDate.getTime(),
                     contentLength,
                     request,
                     response,
                     getDerivedProductInputStream(parentUuid, DerivedProductStore.QUICKLOOK_TAG));
            }
            else
            {
               // THUMBNAIL Products
               MediaResponseBuilder.prepareMediaResponse(
                     null,
                     data.getName(),
                     contentType,
                     publicationDate.getTime(),
                     contentLength,
                     request,
                     response,
                     getDerivedProductInputStream(parentUuid, DerivedProductStore.THUMBNAIL_TAG));
            }
         }
         catch (IOException | StoreException e)
         {
            LOGGER.error("No stream for derived product", e);
            throw new ODataApplicationException(
                  e.getMessage(), HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
         }
      }
      // Download Physical Products
      else
      {
         try
         {
            data = STORE_SERVICE.getPhysicalProduct(productUuid);
            MediaResponseBuilder.prepareMediaResponse(
                  checksumValue,
                  data.getName(),
                  contentType,
                  publicationDate.getTime(),
                  contentLength,
                  request,
                  response,
                  getProductInputStream(productUuid));
         }
         catch (IOException | DataStoreException e)
         {
            LOGGER.error("Product {} not found", productUuid, e);
            throw new ODataApplicationException(
                  e.getMessage(), HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
         }
      }
   }

   private String extractChecksum(Entity entity)
   {
      Property checksumProperty = entity.getProperty(ProductModel.PROPERTY_CHECKSUM);
      @SuppressWarnings("unchecked")
      List<ComplexValue> checksumList = (List<ComplexValue>) checksumProperty.getValue();

      if (checksumList == null)
      {
         return null;
      }

      for (ComplexValue checksum: checksumList)
      {
         List<Property> properties = checksum.getValue();
         for (Property property: properties)
         {
            if (property.getName().equals(ChecksumComplexType.PROPERTY_VALUE))
            {
               return (String) property.getValue();
            }
         }
      }
      return null;
   }

   private InputStream getProductInputStream(String productUuid) throws IOException, DataStoreException
   {
      DataStoreProduct data = STORE_SERVICE.getPhysicalProduct(productUuid);
      if (data.hasImpl(InputStream.class))
      {
         return data.getImpl(InputStream.class);
      }
      return null;
   }

   private InputStream getDerivedProductInputStream(String productUuid, String tag)
         throws IOException, StoreException
   {
      DataStoreProduct data = DERIVED_PS_SERVICE
            .getDerivedProduct(productUuid, tag)
            .getImpl(DataStoreProduct.class);
      if (data.hasImpl(InputStream.class))
      {
         return data.getImpl(InputStream.class);
      }
      return null;
   }

   private DataStoreProduct getPhysicalDerivedProduct(String productUuid) throws StoreException
   {
      return DERIVED_PS_SERVICE
            .getDerivedProduct(productUuid, DerivedProductStore.QUICKLOOK_TAG)
            .getImpl(DataStoreProduct.class);

   }

   @Override
   public Entity createMediaEntityData(InputStream mediaContent, ODataRequest request) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.UPLOAD);

      try
      {
         Path tempProductFile = Files.createTempFile(WorkingDirectory.getTempDirectoryFile().toPath(), null, ".ingest_data");
         LOGGER.debug("Temp Product at: {}", tempProductFile);

         try (OutputStream os = Files.newOutputStream(tempProductFile))
         {
            BufferedOutputStream bufferedOS = new BufferedOutputStream(os);

            int eightBits;

            while ((eightBits = mediaContent.read()) != -1)
            {
               bufferedOS.write(eightBits);
            }
            bufferedOS.flush();
         }

         // get filename from http headers
         String productFilename = DataHandlerUtil.parseFilenameFromHttpHeaders(request);
         Path productFile = tempProductFile.resolveSibling(productFilename);

         Files.move(tempProductFile, productFile);
         LOGGER.debug("Moved Product at: {}", productFile);

         // start ingestion
         IngestibleRawProduct inProduct = IngestibleRawProduct.fromURL(productFile.toUri().toURL());
         Object futureResult = ProcessingManager.processProduct(inProduct, Collections.emptyList(), true).get();
         LOGGER.debug("Uploaded product has been ingested: {}", futureResult);

         // return created product
         return toProductEntity(PRODUCT_SERVICE.getProduct(inProduct.getUuid()));
      }
      catch (IOException | InterruptedException | ExecutionException e)
      {
         LOGGER.error("Could not ingest uploaded product", e);
         throw new ODataApplicationException("Could not ingest uploaded product: " + e.getMessage(),
               HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
      }
   }
}
