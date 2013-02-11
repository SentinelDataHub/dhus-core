/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019 GAEL Systems
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
package org.dhus.olingo.v2.entity;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;

import org.dhus.api.olingo.v2.TypeInfo;
import org.dhus.olingo.v2.datamodel.GmpDataStoreModel;
import org.dhus.olingo.v2.datamodel.complex.GMPConfigurationComplexType;
import org.dhus.olingo.v2.datamodel.complex.MySQLConnectionInfoComplexType;
import org.dhus.store.datastore.config.GmpDataStoreConf;
import org.dhus.store.datastore.config.NamedDataStoreConf;

/**
 * Transforms GmpDataStoreConf configuration objects to OData entities.
 */
@TypeInfo(type = GmpDataStoreConf.class, baseType = NamedDataStoreConf.class)
public class GMPDataStoreEntityProducer extends AsyncDataStoreEntityProducer<GmpDataStoreConf>
{
   @Override
   public Entity transform(GmpDataStoreConf gmpDataStore)
   {
      Entity dataStoreEntity = super.transform(gmpDataStore);

      dataStoreEntity.setType(GmpDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());

      dataStoreEntity
            .addProperty(new Property(
                  null,
                  GmpDataStoreModel.PROPERTY_REPO_LOCATION,
                  ValueType.PRIMITIVE,
                  gmpDataStore.getRepoLocation()))
            .addProperty(makeMySQLConnectionInfoProperty(gmpDataStore.getMysqlConnectionInfo()));

      if (gmpDataStore.getConfiguration() != null)
      {
         dataStoreEntity.addProperty(makeConfigurationProperty(gmpDataStore.getConfiguration()));
      }

      return dataStoreEntity;
   }

   private static Property makeMySQLConnectionInfoProperty(GmpDataStoreConf.MysqlConnectionInfo mysql)
   {
      ComplexValue complexValue = new ComplexValue();
      complexValue.getValue().add(new Property(
            null,
            MySQLConnectionInfoComplexType.PROPERTY_DATABASE_URL,
            ValueType.PRIMITIVE,
            mysql.getValue()));
      complexValue.getValue().add(new Property(
            null,
            MySQLConnectionInfoComplexType.PROPERTY_USER,
            ValueType.PRIMITIVE,
            mysql.getUser()));
      complexValue.getValue().add(new Property(
            null,
            MySQLConnectionInfoComplexType.PROPERTY_PASSWORD,
            ValueType.PRIMITIVE,
            mysql.getPassword()));

      return new Property(
            null,
            GmpDataStoreModel.PROPERTY_MYSQLCONNECTIONINFO,
            ValueType.COMPLEX,
            complexValue);
   }

   private static Property makeConfigurationProperty(GmpDataStoreConf.Configuration configuration)
   {
      ComplexValue configurationValue = new ComplexValue();
      configurationValue.getValue().add(new Property(
            null,
            GMPConfigurationComplexType.PROPERTY_AGENTID,
            ValueType.PRIMITIVE,
            configuration.getAgentid()));

      configurationValue.getValue().add(new Property(
            null,
            GMPConfigurationComplexType.PROPERTY_TARGETID,
            ValueType.PRIMITIVE,
            configuration.getTargetid()));

      return new Property(
            null,
            GmpDataStoreModel.PROPERTY_CONFIGURATION,
            ValueType.COMPLEX,
            configurationValue);
   }

}
