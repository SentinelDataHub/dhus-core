/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018,2019 GAEL Systems
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

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.datastore.processing.ProcessingUtils;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.drb.DrbNode;
import fr.gael.drbx.cortex.DrbCortexItemClass;
import fr.gael.drbx.cortex.DrbCortexModel;

import fr.gael.odata.engine.data.DataHandler;
import fr.gael.odata.engine.data.DataHandlerUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;

import org.dhus.olingo.v2.datamodel.ClassModel;
import org.dhus.olingo.v2.datamodel.ItemModel;
import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.store.StoreException;
import org.dhus.store.datastore.DataStoreProduct;
import org.dhus.store.derived.DerivedProductStore;
import org.dhus.store.derived.DerivedProductStoreService;

public class ClassDataHandler implements DataHandler
{
   private static final ProductService PRODUCT_SERVICE =
         ApplicationContextProvider.getBean(ProductService.class);
   private final DerivedProductStoreService derivedProductStoreService =
         ApplicationContextProvider.getBean(DerivedProductStoreService.class);

   private Entity toOlingoEntity(String uri)
   {
      Entity classEntity = new Entity();

      // Uri
      classEntity.addProperty(new Property(
            null,
            ClassModel.PROPERTY_URI,
            ValueType.PRIMITIVE,
            uri));

      // Id
      try
      {
         classEntity.addProperty(new Property(
               null,
               ClassModel.PROPERTY_ID,
               ValueType.PRIMITIVE,
               UUID.nameUUIDFromBytes(uri.
                     getBytes("UTF-8")).toString()));
         // Set Id
         classEntity.setId(DataHandlerUtil.createEntityId(
               ClassModel.PROPERTY_ID,
               UUID.nameUUIDFromBytes(uri.
                     getBytes("UTF-8")).toString()));
      }
      catch (UnsupportedEncodingException e)
      {
         throw new UnsupportedOperationException("Cannot compute Class Id for URI " + uri, e);
      }
      classEntity.setType(ClassModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());
      return classEntity;
   }

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      EntityCollection entityCollection = new EntityCollection();
      try
      {
         DrbCortexModel model = DrbCortexModel.getDefaultModel();
         ExtendedIterator it = model.getCortexModel().getOntModel().listClasses();
         while (it.hasNext())
         {
            OntClass cl = (OntClass) it.next();
            // FIXME cl.getURI() must not be null !!
            if (cl.getURI() != null)
            {
               entityCollection.getEntities().add(toOlingoEntity(cl.getURI()));
            }
         }
      }
      catch (IOException e)
      {
         throw new UnsupportedOperationException("Cannot compute Class Id", e);
      }
      return entityCollection;
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      for (UriParameter uriParameter: keyParameters)
      {
         if (ItemModel.PROPERTY_ID.equals(uriParameter.getName()))
         {
            String classId = DataHandlerUtil.trimStringKeyParameter(uriParameter);
            try
            {
               DrbCortexModel model = DrbCortexModel.getDefaultModel();
               ExtendedIterator it = model.getCortexModel().getOntModel().listClasses();
               while (it.hasNext())
               {
                  // FIXME cl.getURI() must not be null !!
                  OntClass cl = (OntClass) it.next();

                  String id = UUID.nameUUIDFromBytes(cl.getURI().getBytes("UTF-8")).toString();
                  if (cl.getURI() != null && classId.equals(id))
                  {
                     return toOlingoEntity(cl.getURI());
                  }
               }
            }
            catch (IOException e)
            {
               throw new UnsupportedOperationException("Cannot compute Class Id", e);
            }
         }
      }
      return null;
   }

   @Override
   public Entity getRelatedEntityData(Entity sourceEntity, EdmNavigationProperty edmNavigationProperty)
         throws ODataApplicationException
   {
      // Derived Product to Attributes Navigation
      if (sourceEntity instanceof DerivedProductEntity)
      {
         String tag = (String) sourceEntity.getProperty(ItemModel.PROPERTY_ID).getValue();
         String uuid = ((DerivedProductEntity) sourceEntity).getParentUuid();
         DataStoreProduct dp = null;
         try
         {
            switch (tag)
            {
               case ProductDataHandler.QUICKLOOK_ID:
               {
                  dp = derivedProductStoreService.getDerivedProduct(uuid, DerivedProductStore.QUICKLOOK_TAG);
                  break;
               }
               case ProductDataHandler.THUMBNAIL_ID:
               {
                  dp = derivedProductStoreService.getDerivedProduct(uuid, DerivedProductStore.THUMBNAIL_TAG);
                  break;
               }
            }
         }
         catch (StoreException e)
         {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
         }
         if (dp == null)
         {
            throw new ODataApplicationException("Derived product " + tag + " not found", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
         }
         DrbNode node = dp.getImpl(DrbNode.class);
         try
         {
            return toOlingoEntity(ProcessingUtils.getClassFromNode(node).getOntClass().getURI());
         }
         catch (IOException e)
         {
            throw new UnsupportedOperationException("Cannot compute Class Id", e);
         }
      }
      else if (sourceEntity.getType().equals(ProductModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         String productUuid = (String) sourceEntity.getProperty(ItemModel.PROPERTY_ID).getValue();
         Product product = PRODUCT_SERVICE.getProduct(productUuid);
         return toOlingoEntity(product.getItemClass());
      }
      else if (sourceEntity instanceof NodeEntity)
      {
         NodeEntity nodeEntity = (NodeEntity) sourceEntity;
         DrbNode node = nodeEntity.getDrbNode();
         try
         {
            return toOlingoEntity(ProcessingUtils.getClassFromNode(node).getOntClass().getURI());
         }
         catch (IOException e)
         {
            throw new UnsupportedOperationException("Cannot compute Class Id", e);
         }
      }
      return null;
   }

   @Override
   public Entity getRelatedEntityData(Entity sourceEntity, List<UriParameter> navigationKeyParameters, EdmNavigationProperty edmNavigationProperty)
         throws ODataApplicationException
   {
      if (sourceEntity.getType().equals(ClassModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         String uri = (String) sourceEntity.getProperty(ClassModel.PROPERTY_URI).getValue();
         DrbCortexItemClass cl = DrbCortexItemClass.getCortexItemClassByName(uri);
         ExtendedIterator it = cl.getOntClass().listSubClasses(true);
         String keyParameterValue = DataHandlerUtil.getSingleStringKeyParameterValue(navigationKeyParameters, ClassModel.PROPERTY_ID);

         while (it.hasNext())
         {
            String classuri = ((OntClass) it.next()).getURI();
            try
            {
               if (classuri != null)
               {
                  String classId = UUID.nameUUIDFromBytes(classuri.getBytes("UTF-8")).toString();
                  if (keyParameterValue.equals(classId))
                  {
                     return toOlingoEntity(classuri);
                  }
               }
            }
            catch (UnsupportedEncodingException e)
            {
               throw new UnsupportedOperationException(
                     "Cannot compute Class Id for URI " + classuri, e);
            }
         }
      }
      return null;
   }

   @Override
   public EntityCollection getRelatedEntityCollectionData(Entity sourceEntity, EdmNavigationProperty edmNavigationProperty)
         throws ODataApplicationException
   {
      EntityCollection entityCollection = new EntityCollection();

      if (sourceEntity.getType().equals(ClassModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         String uri = (String) sourceEntity.getProperty(ClassModel.PROPERTY_URI).getValue();
         DrbCortexItemClass cl = DrbCortexItemClass.getCortexItemClassByName(uri);
         ExtendedIterator it = cl.getOntClass().listSubClasses(true);
         while (it.hasNext())
         {
            String classuri = ((OntClass) it.next()).getURI();
            if (classuri != null)
            {
               entityCollection.getEntities().add(toOlingoEntity(classuri));
            }
         }
      }
      return entityCollection;
   }
}
