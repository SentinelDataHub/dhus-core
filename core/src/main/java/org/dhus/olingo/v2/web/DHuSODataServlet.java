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
package org.dhus.olingo.v2.web;

import fr.gael.odata.engine.data.DataHandler;
import fr.gael.odata.engine.model.EdmProvider;
import fr.gael.odata.engine.servlet.AbstractODataServlet;

import java.util.HashMap;
import java.util.Map;

import org.apache.olingo.commons.api.edm.FullQualifiedName;

import org.dhus.api.olingo.v2.EntityProducer;
import org.dhus.api.olingo.v2.TypeInfo;

import org.dhus.olingo.v2.data.AttributeDataHandler;
import org.dhus.olingo.v2.data.ClassDataHandler;
import org.dhus.olingo.v2.data.CollectionDataHandler;
import org.dhus.olingo.v2.data.DataStoreDataHandler;
import org.dhus.olingo.v2.data.EvictionDataHandler;
import org.dhus.olingo.v2.data.MetricDataHandler;
import org.dhus.olingo.v2.data.NodeDataHandler;
import org.dhus.olingo.v2.data.OrderDataHandler;
import org.dhus.olingo.v2.data.ProductDataHandler;
import org.dhus.olingo.v2.data.ScannerDataHandler;
import org.dhus.olingo.v2.data.SmartSynchronizerDataHandler;
import org.dhus.olingo.v2.data.SourceDataHandler;
import org.dhus.olingo.v2.data.SynchronizerDataHandler;
import org.dhus.olingo.v2.data.TransformationDataHandler;
import org.dhus.olingo.v2.data.TransformerDataHandler;
import org.dhus.olingo.v2.datamodel.JobModel;
import org.dhus.olingo.v2.datamodel.ParamPdgsDataStoreModel;
import org.dhus.olingo.v2.datamodel.AsyncDataStoreModel;
import org.dhus.olingo.v2.datamodel.AttributeModel;
import org.dhus.olingo.v2.datamodel.ClassModel;
import org.dhus.olingo.v2.datamodel.CollectionModel;
import org.dhus.olingo.v2.datamodel.DataStoreModel;
import org.dhus.olingo.v2.datamodel.EvictionModel;
import org.dhus.olingo.v2.datamodel.FileScannerModel;
import org.dhus.olingo.v2.datamodel.FtpScannerModel;
import org.dhus.olingo.v2.datamodel.GmpDataStoreModel;
import org.dhus.olingo.v2.datamodel.HfsDataStoreModel;
import org.dhus.olingo.v2.datamodel.ItemModel;
import org.dhus.olingo.v2.datamodel.MetricModel;
import org.dhus.olingo.v2.datamodel.NodeModel;
import org.dhus.olingo.v2.datamodel.OpenstackDataStoreModel;
import org.dhus.olingo.v2.datamodel.OrderModel;
import org.dhus.olingo.v2.datamodel.PdgsDataStoreModel;
import org.dhus.olingo.v2.datamodel.ProductModel;
import org.dhus.olingo.v2.datamodel.RemoteDhusDataStoreModel;
import org.dhus.olingo.v2.datamodel.ScannerModel;
import org.dhus.olingo.v2.datamodel.SmartSynchronizerModel;
import org.dhus.olingo.v2.datamodel.SourceModel;
import org.dhus.olingo.v2.datamodel.SynchronizerModel;
import org.dhus.olingo.v2.datamodel.TransformationModel;
import org.dhus.olingo.v2.datamodel.TransformerModel;
import org.dhus.olingo.v2.datamodel.action.CancelEvictionAction;
import org.dhus.olingo.v2.datamodel.action.OrderProductAction;
import org.dhus.olingo.v2.datamodel.action.QueueEvictionAction;
import org.dhus.olingo.v2.datamodel.action.RepairProductAction;
import org.dhus.olingo.v2.datamodel.action.RepairProductsAction;
import org.dhus.olingo.v2.datamodel.action.RunTransformerAction;
import org.dhus.olingo.v2.datamodel.action.StartScannerAction;
import org.dhus.olingo.v2.datamodel.action.StopEvictionAction;
import org.dhus.olingo.v2.datamodel.action.StopScannerAction;
import org.dhus.olingo.v2.datamodel.action.TransformProductAction;
import org.dhus.olingo.v2.datamodel.complex.ChecksumComplexType;
import org.dhus.olingo.v2.datamodel.complex.CronComplexType;
import org.dhus.olingo.v2.datamodel.complex.DescriptiveParameterComplexType;
import org.dhus.olingo.v2.datamodel.complex.GMPConfigurationComplexType;
import org.dhus.olingo.v2.datamodel.complex.MySQLConnectionInfoComplexType;
import org.dhus.olingo.v2.datamodel.complex.PatternReplaceComplexType;
import org.dhus.olingo.v2.datamodel.complex.ResourceLocationComplexType;
import org.dhus.olingo.v2.datamodel.complex.ScannerStatusComplexType;
import org.dhus.olingo.v2.datamodel.complex.SynchronizerSourceComplexType;
import org.dhus.olingo.v2.datamodel.complex.TimeRangeComplexType;
import org.dhus.olingo.v2.datamodel.complex.TransformationParametersComplexType;
import org.dhus.olingo.v2.datamodel.enumeration.MetricTypeEnum;
import org.dhus.olingo.v2.datamodel.enumeration.JobStatusEnum;
import org.dhus.olingo.v2.datamodel.function.EvictionDateFunction;
import org.dhus.olingo.v2.datamodel.function.ResourceLocationFunction;
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
                  @SuppressWarnings("rawtypes")
                  Class<? extends EntityProducer> klazz = clazz.asSubclass(EntityProducer.class);
                  typeStore.insert(klazz.getDeclaredConstructor().newInstance());
               }
            }
            catch (ReflectiveOperationException suppressed) {}
         }
      }

      HashMap<FullQualifiedName, DataHandler> dataHandlers = new HashMap<>();

      // storage
      DataStoreDataHandler dataStoreDataHandler = new DataStoreDataHandler(typeStore);
      dataHandlers.put(DataStoreModel.ABSTRACT_FULL_QUALIFIED_NAME, dataStoreDataHandler);
      dataHandlers.put(HfsDataStoreModel.FULL_QUALIFIED_NAME, dataStoreDataHandler);
      dataHandlers.put(OpenstackDataStoreModel.FULL_QUALIFIED_NAME, dataStoreDataHandler);
      dataHandlers.put(GmpDataStoreModel.FULL_QUALIFIED_NAME, dataStoreDataHandler);
      dataHandlers.put(RemoteDhusDataStoreModel.FULL_QUALIFIED_NAME, dataStoreDataHandler);
      dataHandlers.put(PdgsDataStoreModel.FULL_QUALIFIED_NAME, dataStoreDataHandler);

      // operative
      dataHandlers.put(EvictionModel.FULL_QUALIFIED_NAME, new EvictionDataHandler());
      dataHandlers.put(SourceModel.FULL_QUALIFIED_NAME, new SourceDataHandler());
      dataHandlers.put(SynchronizerModel.ABSTRACT_FULL_QUALIFIED_NAME, new SynchronizerDataHandler(typeStore));
      dataHandlers.put(SmartSynchronizerModel.FULL_QUALIFIED_NAME, new SmartSynchronizerDataHandler(typeStore));
      dataHandlers.put(TransformerModel.FULL_QUALIFIED_NAME, new TransformerDataHandler(typeStore));
      dataHandlers.put(TransformationModel.FULL_QUALIFIED_NAME, new TransformationDataHandler(typeStore));

      // scanner
      dataHandlers.put(ScannerModel.FULL_QUALIFIED_NAME, new ScannerDataHandler());
      dataHandlers.put(FileScannerModel.FULL_QUALIFIED_NAME, new ScannerDataHandler());
      dataHandlers.put(FtpScannerModel.FULL_QUALIFIED_NAME, new ScannerDataHandler());

      // data
      dataHandlers.put(ProductModel.FULL_QUALIFIED_NAME, new ProductDataHandler(typeStore));
      dataHandlers.put(CollectionModel.FULL_QUALIFIED_NAME, new CollectionDataHandler());
      dataHandlers.put(NodeModel.FULL_QUALIFIED_NAME, new NodeDataHandler());
      dataHandlers.put(AttributeModel.FULL_QUALIFIED_NAME, new AttributeDataHandler());
      dataHandlers.put(ClassModel.FULL_QUALIFIED_NAME, new ClassDataHandler());
      dataHandlers.put(OrderModel.FULL_QUALIFIED_NAME, new OrderDataHandler());

      // metrics
      if (MetricDataHandler.isEnabled())
      {
         dataHandlers.put(MetricModel.FULL_QUALIFIED_NAME, new MetricDataHandler());
      }

      return dataHandlers;
   }

   private static EdmProvider buildEdmProvider()
   {
      EdmProvider.Builder builder = new EdmProvider.Builder(NAMESPACE, CONTAINER_NAME);
      builder.addModels(
                  // storage
                  new DataStoreModel(),
                  new HfsDataStoreModel(),
                  new OpenstackDataStoreModel(),
                  new GmpDataStoreModel(),
                  new RemoteDhusDataStoreModel(),
                  new PdgsDataStoreModel(),
                  new AsyncDataStoreModel(),
                  new ParamPdgsDataStoreModel(),

                  // operative
                  new EvictionModel(),
                  new SourceModel(),
                  new SynchronizerModel(),
                  new SmartSynchronizerModel(),

                  // transformation
                  new TransformerModel(),
                  new TransformationModel(),

                  // scanner
                  new ScannerModel(),
                  new FileScannerModel(),
                  new FtpScannerModel(),

                  // data
                  new ProductModel(),
                  new CollectionModel(),
                  new ItemModel(),
                  new NodeModel(),
                  new AttributeModel(),
                  new ClassModel(),
                  new OrderModel(),
                  new JobModel())

            .addComplexTypes(
                  new MySQLConnectionInfoComplexType(),
                  new GMPConfigurationComplexType(),

                  // synchronizer
                  new SynchronizerSourceComplexType(),

                  // scanner
                  new ScannerStatusComplexType(),

                  // products
                  new ResourceLocationComplexType(),

                  // general
                  new CronComplexType(),

                  // transformation
                  new DescriptiveParameterComplexType(),
                  new TransformationParametersComplexType(),

                  // other
                  new TimeRangeComplexType(),
                  new ChecksumComplexType(),
                  new PatternReplaceComplexType())

            .addActions(
                  // eviction
                  new QueueEvictionAction(),
                  new CancelEvictionAction(),
                  new StopEvictionAction(),

                  // scanner
                  new StartScannerAction(),
                  new StopScannerAction(),

                  // transformation
                  new RunTransformerAction(),
                  new TransformProductAction(),

                  // products
                  new OrderProductAction(),
                  new RepairProductAction(),
                  new RepairProductsAction())

            .addFunctions(
                  new ResourceLocationFunction(),
                  new EvictionDateFunction())

            .addEnums(
                  new JobStatusEnum())
            .build();

      if (MetricDataHandler.isEnabled())
      {
         builder.addModels(new MetricModel());
         builder.addEnums(new MetricTypeEnum());
      }

      return builder.build();
   }

}
