/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2020 GAEL Systems
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
import org.dhus.olingo.v2.datamodel.OndaDataStoreModel;
import org.dhus.olingo.v2.datamodel.complex.CronComplexType;
import org.dhus.olingo.v2.datamodel.complex.ObjectStorageComplexType;
import org.dhus.olingo.v2.datamodel.complex.OndaScannerComplexType;
import org.dhus.store.datastore.config.NamedDataStoreConf;
import org.dhus.store.datastore.config.ObjectStorageCredentialConf;
import org.dhus.store.datastore.config.OndaDataStoreConf;
import org.dhus.store.datastore.config.OndaScannerConf;

import fr.gael.dhus.database.object.config.cron.Cron;

@TypeInfo(type = OndaDataStoreConf.class, baseType = NamedDataStoreConf.class)
public class OndaDataStoreEntityProducer extends HttpAsyncDataStoreEntityProducer<OndaDataStoreConf>
{
   @Override
   public Entity transform(OndaDataStoreConf ondaDataStore)
   {
      Entity dataStoreEntity = super.transform(ondaDataStore);
      dataStoreEntity.setType(OndaDataStoreModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());

      dataStoreEntity
            .addProperty(new Property(
                  null,
                  OndaDataStoreModel.PROPERTY_ORDER,
                  ValueType.PRIMITIVE,
                  ondaDataStore.isOrder()));
      
      if (ondaDataStore.getObjectStorageCredential() != null)
      {
         dataStoreEntity .addProperty(makeObjectStorageProperty(ondaDataStore.getObjectStorageCredential(), ondaDataStore.getName()));
      }
      if (ondaDataStore.getOndaScanner() != null)
      {
         dataStoreEntity.addProperty(makeOndaScannerProperty(ondaDataStore.getOndaScanner()));
      }
      return dataStoreEntity;
   }
   
   private static Property makeObjectStorageProperty(ObjectStorageCredentialConf objectStorage, String dataStoreName)
   {
      ComplexValue objectStorageValue = new ComplexValue();

      objectStorageValue.getValue().add(new Property(
            null,
            ObjectStorageComplexType.PROPERTY_PROVIDER,
            ValueType.PRIMITIVE,
            objectStorage.getProvider()));
 
      objectStorageValue.getValue().add(new Property(
            null,
            ObjectStorageComplexType.PROPERTY_IDENTITY,
            ValueType.PRIMITIVE,
            objectStorage.getIdentity()));
      
      objectStorageValue.getValue().add(new Property(
            null,
            ObjectStorageComplexType.PROPERTY_CREDENTIAL,
            ValueType.PRIMITIVE,
            objectStorage.getCredential()));
      
      objectStorageValue.getValue().add(new Property(
            null,
            ObjectStorageComplexType.PROPERTY_URL,
            ValueType.PRIMITIVE,
            objectStorage.getUrl()));
      
      objectStorageValue.getValue().add(new Property(
            null,
            ObjectStorageComplexType.PROPERTY_REGION,
            ValueType.PRIMITIVE,
            objectStorage.getRegion()));

      return new Property(
            null,
            OndaDataStoreModel.PROPERTY_OBJECT_STORAGE_CREDENTIAL,
            ValueType.COMPLEX,
            objectStorageValue);
   }
   
   private static Property makeOndaScannerProperty(OndaScannerConf ondaScanner)
   {
      ComplexValue ondaScannerValue = new ComplexValue();

      ondaScannerValue.getValue().add(new Property(
            null,
            OndaScannerComplexType.PROPERTY_OPENSEARCH_URL,
            ValueType.PRIMITIVE,
            ondaScanner.getOpensearchUrl()));
      
      ondaScannerValue.getValue().add(new Property(
            null,
            OndaScannerComplexType.PROPERTY_LAST_CREATION_DATE,
            ValueType.PRIMITIVE,
            ondaScanner.getLastCreationDate().toGregorianCalendar()));
            
      ondaScannerValue.getValue().add(new Property(
            null,
            OndaScannerComplexType.PROPERTY_FILTER,
            ValueType.PRIMITIVE,
            ondaScanner.getFilter()));
      
      ondaScannerValue.getValue().add(new Property(
            null,
            OndaScannerComplexType.PROPERTY_PAGE_SIZE,
            ValueType.PRIMITIVE,
            ondaScanner.getPageSize()));
      
      ondaScannerValue.getValue().add(
            makeCronProperty(ondaScanner.getCron()));

      return new Property(
            null,
            OndaDataStoreModel.PROPERTY_ONDA_SCANNER,
            ValueType.COMPLEX,
            ondaScannerValue); 
   } 

   private static Property makeCronProperty(Cron cron)
   {
      ComplexValue complexValue = new ComplexValue();
      complexValue.getValue().add(new Property(
            null,
            CronComplexType.PROPERTY_ACTIVE,
            ValueType.PRIMITIVE,
            cron.isActive()));

      complexValue.getValue().add(new Property(
            null,
            CronComplexType.PROPERTY_SCHEDULE,
            ValueType.PRIMITIVE,
            cron.getSchedule()));

      return new Property(
            null,
            OndaScannerComplexType.PROPERTY_CRON,
            ValueType.COMPLEX,
            complexValue);
   }
}
