/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018 GAEL Systems
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
package org.dhus.olingo.v2.web;

import fr.gael.odata.engine.data.DataHandler;
import fr.gael.odata.engine.model.EdmProvider;
import fr.gael.odata.engine.servlet.AbstractODataServlet;

import java.util.HashMap;
import java.util.Map;

import org.apache.olingo.commons.api.edm.FullQualifiedName;

import org.dhus.olingo.v2.data.CollectionDataHandler;
import org.dhus.olingo.v2.data.DataStoreDataHandler;
import org.dhus.olingo.v2.data.EvictionDataHandler;
import org.dhus.olingo.v2.data.ProductDataHandler;
import org.dhus.olingo.v2.data.SmartSynchronizerDataHandler;
import org.dhus.olingo.v2.data.SourceDataHandler;
import org.dhus.olingo.v2.data.SynchronizerDataHandler;
import org.dhus.olingo.v2.datamodel.CollectionModel;
import org.dhus.olingo.v2.datamodel.DataStoreModel;
import org.dhus.olingo.v2.datamodel.RemoteDhusDataStoreModel;
import org.dhus.olingo.v2.datamodel.EvictionModel;
import org.dhus.olingo.v2.datamodel.GmpDataStoreModel;
import org.dhus.olingo.v2.datamodel.HfsDataStoreModel;
import org.dhus.olingo.v2.datamodel.OpenstackDataStoreModel;
import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.olingo.v2.datamodel.SmartSynchronizerModel;
import org.dhus.olingo.v2.datamodel.SourceModel;
import org.dhus.olingo.v2.datamodel.SynchronizerModel;
import org.dhus.olingo.v2.datamodel.action.CancelEvictionAction;
import org.dhus.olingo.v2.datamodel.action.QueueEvictionAction;
import org.dhus.olingo.v2.datamodel.action.StopEvictionAction;
import org.dhus.olingo.v2.datamodel.complex.CronComplexType;
import org.dhus.olingo.v2.datamodel.complex.GMPConfigurationComplexType;
import org.dhus.olingo.v2.datamodel.complex.GMPQuotasComplexType;
import org.dhus.olingo.v2.datamodel.complex.MySQLConnectionInfoComplexType;
import org.dhus.olingo.v2.datamodel.complex.SynchronizerSourceComplexType;
import org.dhus.olingo.v2.entity.EntityProducer;
import org.dhus.olingo.v2.entity.TypeInfo;
import org.dhus.olingo.v2.entity.TypeStore;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;


public class DHuSODataServlet extends AbstractODataServlet
{
   public static final String NAMESPACE = "OData.DHuS";
   public static final String CONTAINER_NAME = "Container";

   private static final long serialVersionUID = 1L;

   public DHuSODataServlet()
   {
      super(buildEdmProvider(), getDataHandlers());
   }

   private static Map<FullQualifiedName, DataHandler> getDataHandlers()
   {
      // Create TypeStore from all available EntityProducers
      TypeStore typeStore = new TypeStore();
      ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
      scanner.addIncludeFilter(new AnnotationTypeFilter(TypeInfo.class));
      for (BeanDefinition bd: scanner.findCandidateComponents("org.dhus.olingo.v2.entity"))
      {
         if (!bd.isAbstract())
         {
            try
            {
               Class<?> clazz = Class.forName(bd.getBeanClassName());
               if (EntityProducer.class.isAssignableFrom(clazz))
               {
                  Class<? extends EntityProducer> klazz = clazz.asSubclass(EntityProducer.class);
                  typeStore.insert(klazz.newInstance());
               }
            }
            catch (ClassNotFoundException | InstantiationException | IllegalAccessException suppressed) {}
         }
      }

      HashMap<FullQualifiedName, DataHandler> dataHandlers = new HashMap<>();

      // storage
      DataStoreDataHandler dataStoreDataHandler = new DataStoreDataHandler();
      dataHandlers.put(DataStoreModel.ABSTRACT_FULL_QUALIFIED_NAME, dataStoreDataHandler);
      dataHandlers.put(HfsDataStoreModel.FULL_QUALIFIED_NAME, dataStoreDataHandler);
      dataHandlers.put(OpenstackDataStoreModel.FULL_QUALIFIED_NAME, dataStoreDataHandler);
      dataHandlers.put(GmpDataStoreModel.FULL_QUALIFIED_NAME, dataStoreDataHandler);
      dataHandlers.put(RemoteDhusDataStoreModel.FULL_QUALIFIED_NAME, dataStoreDataHandler);

      // operative
      dataHandlers.put(EvictionModel.FULL_QUALIFIED_NAME, new EvictionDataHandler());
      dataHandlers.put(SourceModel.FULL_QUALIFIED_NAME, new SourceDataHandler());
      dataHandlers.put(SynchronizerModel.ABSTRACT_FULL_QUALIFIED_NAME, new SynchronizerDataHandler(typeStore));
      dataHandlers.put(SmartSynchronizerModel.FULL_QUALIFIED_NAME, new SmartSynchronizerDataHandler(typeStore));

      // data
      dataHandlers.put(ProductModel.FULL_QUALIFIED_NAME, new ProductDataHandler());
      dataHandlers.put(CollectionModel.FULL_QUALIFIED_NAME, new CollectionDataHandler());
      return dataHandlers;
   }

   private static EdmProvider buildEdmProvider()
   {
      return new EdmProvider.Builder(NAMESPACE, CONTAINER_NAME)
            .addModels(

                  // storage
                  new DataStoreModel(),
                  new HfsDataStoreModel(),
                  new OpenstackDataStoreModel(),
                  new GmpDataStoreModel(),
                  new RemoteDhusDataStoreModel(),

                  // operative
                  new EvictionModel(),
                  new SourceModel(),
                  new SynchronizerModel(),
                  new SmartSynchronizerModel(),

                  // data
                  new ProductModel(),
                  new CollectionModel())

            .addComplexTypes(
                  new MySQLConnectionInfoComplexType(),
                  new GMPQuotasComplexType(),
                  new GMPConfigurationComplexType(),
                  new CronComplexType(),
                  new SynchronizerSourceComplexType())
            .addActions(
                  new QueueEvictionAction(),
                  new CancelEvictionAction(),
                  new StopEvictionAction())
            .build();
   }

}
