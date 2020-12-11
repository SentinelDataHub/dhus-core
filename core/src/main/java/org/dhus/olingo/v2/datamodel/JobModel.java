/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019,2020 GAEL Systems
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
package org.dhus.olingo.v2.datamodel;

import fr.gael.dhus.database.object.config.search.OrderConf;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;

import org.dhus.olingo.v2.datamodel.enumeration.JobStatusEnum;
import org.dhus.olingo.v2.web.DHuSODataServlet;

import fr.gael.odata.engine.model.EntityModel;

public class JobModel implements EntityModel
{
   public static final String ENTITY_TYPE_NAME = "Job";
   public static final String ENTITY_SET_NAME = "Jobs";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);

   public static final String PROPERTY_STATUS = "Status";
   public static final String PROPERTY_ID = "Id";
   public static final String PROPERTY_SUBMISSION_TIME = "SubmissionTime";
   public static final String PROPERTY_ESTIMATED_TIME = "EstimatedTime";
   public static final String PROPERTY_STATUS_MESSAGE = "StatusMessage";

   private final boolean showEstimatedTime;

   public JobModel()
   {
      ConfigurationManager configManager = ApplicationContextProvider.getBean(ConfigurationManager.class);
      OrderConf orderConf = configManager.getOdataConfiguration().getOrder();

      this.showEstimatedTime = orderConf == null ? false : orderConf.isShowEstimatedTime();
   }

   @Override
   public CsdlEntityType getEntityType()
   {
      CsdlPropertyRef nameKey = new CsdlPropertyRef().setName(PROPERTY_ID);

      ArrayList<CsdlProperty> properties = new ArrayList<>();

      properties.add(new CsdlProperty()
            .setName(PROPERTY_ID)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false));

      properties.add(new CsdlProperty()
            .setName(PROPERTY_STATUS)
            .setType(JobStatusEnum.FULL_QUALIFIED_NAME)
            .setNullable(false));

      properties.add(new CsdlProperty()
            .setName(PROPERTY_SUBMISSION_TIME)
            .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
            .setNullable(false));

      // this property can be hidden depending on configuration
      if(showEstimatedTime)
      {
         properties.add(new CsdlProperty()
               .setName(PROPERTY_ESTIMATED_TIME)
               .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
               .setNullable(true));
      }

      properties.add(new CsdlProperty()
            .setName(PROPERTY_STATUS_MESSAGE)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true));

      // Define navigation properties
      CsdlNavigationProperty productNavigationProperty = new CsdlNavigationProperty()
            .setName(ProductModel.ENTITY_TYPE_NAME)
            .setType(ProductModel.FULL_QUALIFIED_NAME)
            .setCollection(false);

      return new CsdlEntityType().setName(ENTITY_TYPE_NAME)
            .setProperties(properties)
            .setKey(Collections.singletonList(nameKey))
            .setNavigationProperties(Arrays.asList(productNavigationProperty))
            .setAbstract(true);
   }

   @Override
   public CsdlEntitySet getEntitySet()
   {
      CsdlEntitySet entitySet = EntityModel.super.getEntitySet();
      return entitySet.setIncludeInServiceDocument(false);
   }

   @Override
   public String getEntitySetName()
   {
      return ENTITY_SET_NAME;
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
