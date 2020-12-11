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
package org.dhus.olingo.v2.data;

import static fr.gael.odata.engine.data.DataHandlerUtil.containsProperty;
import static fr.gael.odata.engine.data.DataHandlerUtil.getPropertyValue;

import static org.apache.olingo.commons.api.data.ValueType.COMPLEX;
import static org.apache.olingo.commons.api.data.ValueType.PRIMITIVE;

import static org.dhus.olingo.v2.datamodel.ScannerModel.ENTITY_SET_NAME;
import static org.dhus.olingo.v2.datamodel.ScannerModel.PROPERTY_ID;
import static org.dhus.olingo.v2.datamodel.ScannerModel.PROPERTY_SOURCE_REMOVE;
import static org.dhus.olingo.v2.datamodel.ScannerModel.PROPERTY_URL;

import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.config.cron.Cron;
import fr.gael.dhus.database.object.config.scanner.FileScannerConf;
import fr.gael.dhus.database.object.config.scanner.FtpScannerConf;
import fr.gael.dhus.database.object.config.scanner.ScannerConfiguration.Collections;
import fr.gael.dhus.datastore.scanner.Scanner;
import fr.gael.dhus.datastore.scanner.ScannerException;
import fr.gael.dhus.datastore.scanner.ScannerStatus;
import fr.gael.dhus.service.CollectionService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.odata.engine.data.DataHandler;
import fr.gael.odata.engine.data.DataHandlerUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;

import org.dhus.olingo.v2.ODataSecurityManager;
import org.dhus.olingo.v2.datamodel.CollectionModel;
import org.dhus.olingo.v2.datamodel.FileScannerModel;
import org.dhus.olingo.v2.datamodel.FtpScannerModel;
import org.dhus.olingo.v2.datamodel.ScannerModel;
import org.dhus.olingo.v2.datamodel.action.StartScannerAction;
import org.dhus.olingo.v2.datamodel.action.StopScannerAction;
import org.dhus.olingo.v2.datamodel.complex.CronComplexType;
import org.dhus.olingo.v2.datamodel.complex.ScannerStatusComplexType;
import org.dhus.scanner.config.ScannerInfo;
import org.dhus.scanner.service.ScannerServiceImpl;

public class ScannerDataHandler implements DataHandler
{
   private static final ScannerServiceImpl SCANNER_SERVICE = ApplicationContextProvider.getBean(ScannerServiceImpl.class);
   private static final CollectionService COLLECTION_SVC = ApplicationContextProvider.getBean(CollectionService.class);

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);
      EntityCollection entityCollection = new EntityCollection();
      List<Scanner> scanners = SCANNER_SERVICE.listScanners();

      if (scanners != null && scanners.size() > 0)
      {
         scanners.stream()
               .map(this::toOlingoEntity)
               .forEach(entityCollection.getEntities()::add);
      }

      return entityCollection;
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);
      return toOlingoEntity(getScanner(keyParameters));
   }

   @Override
   public Entity createEntityData(Entity entity) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);
      ScannerInfo scannerInfo;

      if (entity.getType().equalsIgnoreCase(FtpScannerModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         scannerInfo = buildFtpScannerConfig(entity);
      }
      else if ((entity.getType().equalsIgnoreCase(FileScannerModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString())))
      {
         scannerInfo = buildFileScannerConfig(entity);
      }
      else
      {
         throw new ODataApplicationException("Scanner type not provided", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }

      try
      {
         scannerInfo.setId(-1L); // set un negative ID when creating scanner to avoid unintended update
         Scanner scanner = SCANNER_SERVICE.createScanner(scannerInfo);
         if (scanner == null)
         {
            throw new ODataApplicationException("Exception when creating scanner",
                  HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
         }
         return toOlingoEntity(scanner);
      }
      catch (ScannerException.BadScannerConfigException e)
      {
         throw new ODataApplicationException("Exception when creating scanner", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
      catch (ScannerException e)
      {
         throw new ODataApplicationException("Exception when creating scanner", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
      }
   }

   @Override
   public void updateEntityData(List<UriParameter> keyParameters, Entity updatedEntity, HttpMethod httpMethod) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      ScannerInfo scannerInfo = getScanner(keyParameters).getConfig();

      if (containsProperty(updatedEntity, ScannerModel.PROPERTY_URL))
      {
         scannerInfo.setUrl((String) getPropertyValue(updatedEntity, ScannerModel.PROPERTY_URL));
      }

      if (containsProperty(updatedEntity, ScannerModel.PROPERTY_PATTERN))
      {
         scannerInfo.setPattern((String) getPropertyValue(updatedEntity, ScannerModel.PROPERTY_PATTERN));
      }

      if (containsProperty(updatedEntity, ScannerModel.PROPERTY_SOURCE_REMOVE))
      {
         scannerInfo.setSourceRemove((Boolean) getPropertyValue(updatedEntity, ScannerModel.PROPERTY_SOURCE_REMOVE));
      }

      if (containsProperty(updatedEntity, ScannerModel.PROPERTY_CRON))
      {
         scannerInfo.setCron(extractCron(updatedEntity.getProperty(ScannerModel.PROPERTY_CRON), new Cron()));
      }

      if (scannerInfo instanceof FtpScannerConf)
      {
         if (containsProperty(updatedEntity, FtpScannerModel.USERNAME))
         {
            scannerInfo.setUsername((String) getPropertyValue(updatedEntity, FtpScannerModel.USERNAME));
         }
         if (containsProperty(updatedEntity, FtpScannerModel.PASSWORD))
         {
            scannerInfo.setPassword((String) getPropertyValue(updatedEntity, FtpScannerModel.PASSWORD));
         }
      }

      try
      {
         SCANNER_SERVICE.updateScanner(scannerInfo);
      }
      catch (ScannerException e)
      {
         throw new ODataApplicationException("Exception when updating scanner: " + e.getMessage(),
               HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
               Locale.ENGLISH);
      }
   }

   @Override
   public void deleteEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      ScannerInfo scannerInfo = getScanner(keyParameters).getConfig();
      try
      {
         SCANNER_SERVICE.deleteScanner(scannerInfo.getId());
      }
      catch (ScannerException.ScannerNotFoundException e)
      {
         throw new ODataApplicationException("Scanner not found " + e.getMessage(),
               HttpStatusCode.NOT_MODIFIED.getStatusCode(),
               Locale.ENGLISH);
      }
      catch (ScannerException e)
      {
         throw new ODataApplicationException("Fail to delete scanner " + e.getMessage(),
               HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
               Locale.ENGLISH);
      }
   }

   @Override
   public Object performBoundAction(List<UriParameter> keyPredicates, EdmAction action, Map<String, Parameter> parameters)
         throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      if (action.getFullQualifiedName().equals(StartScannerAction.START_SCANNER_FQN))
      {
         long scannerId = Long.decode(keyPredicates.get(0).getText());

         SCANNER_SERVICE.startScanner(scannerId);

         return new Property(
               null,
               StartScannerAction.START_SCANNER,
               ValueType.PRIMITIVE,
               "Scanner " + scannerId + " has been successfully started");
      }
      else if (action.getFullQualifiedName().equals(StopScannerAction.STOP_SCANNER_FQN))
      {
         long scannerId = Long.decode(keyPredicates.get(0).getText());
         String result = SCANNER_SERVICE.stopScanner(scannerId);

         return new Property(
               null,
               StopScannerAction.STOP_SCANNER,
               ValueType.PRIMITIVE,
               result);
      }
      else
      {
         throw new ODataApplicationException("Action not found",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }
   }

   @Override
   public void createReferenceInCollection(List<UriParameter> sourceKeyPredicates,
         EdmNavigationProperty navigationProperty, List<UriParameter> targetKeyPredicates)
         throws ODataApplicationException
   {
      if (navigationProperty.getName().equals(ScannerModel.NAVIGATION_COLLECTIONS))
      {
         // get scanner configuration
         ScannerInfo scannerConf = getScanner(sourceKeyPredicates).getConfig();

         // get collection name
         String collectionName = DataHandlerUtil.getSingleStringKeyParameterValue(
               targetKeyPredicates, CollectionModel.PROPERTY_NAME);

         if (COLLECTION_SVC.getCollectionByName(collectionName) == null)
         {
            throw new ODataApplicationException("Target collection does not exist: " + collectionName,
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
         }

         // add to existing collection names
         scannerConf.addCollection(collectionName);

         try
         {
            SCANNER_SERVICE.updateScanner(scannerConf);
         }
         catch (ScannerException e)
         {
            throw new ODataApplicationException("Exception when updating scanner: " + e.getMessage(),
                  HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
         }
      }
      else
      {
         throw new ODataApplicationException("Cannot update such reference: " + navigationProperty,
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
   }

   @Override
   public void deleteReference(List<UriParameter> sourceKeyPredicates,
         EdmNavigationProperty navigationProperty, List<UriParameter> targetKeyPredicates) throws ODataApplicationException
   {
      if (navigationProperty.getName().equals(ScannerModel.NAVIGATION_COLLECTIONS))
      {
         // get scanner configuration
         ScannerInfo scannerConf = getScanner(sourceKeyPredicates).getConfig();

         // get collection name
         String collectionName = DataHandlerUtil.getSingleStringKeyParameterValue(
               targetKeyPredicates, CollectionModel.PROPERTY_NAME);

         // remove existing collection names
         Collections collections = scannerConf.getCollections();

         boolean removed = false;
         if (collections != null)
         {
            removed = collections.getCollection().removeIf(collName -> collName.equals(collectionName));
            try
            {
               SCANNER_SERVICE.updateScanner(scannerConf);
            }
            catch (ScannerException e)
            {
               throw new ODataApplicationException("Exception when updating scanner: " + e.getMessage(),
                     HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
                     Locale.ENGLISH);
            }
         }

         // could not remove collection because it was not listed
         if (!removed)
         {
            throw new ODataApplicationException("Unknown Collection name: " + collectionName,
                  HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
         }
      }
      else
      {
         throw new ODataApplicationException("Cannot update such reference: " + navigationProperty,
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
   }

   private ComplexValue toComplexValue(ScannerStatus scannerStatus)
   {
      ComplexValue value = new ComplexValue();
      value.setTypeName(ScannerStatusComplexType.COMPLEX_TYPE_NAME);
      value.getValue().addAll(Arrays.asList(
            new Property(
                  null,
                  ScannerStatusComplexType.PROPERTY_STATUS,
                  ValueType.PRIMITIVE,
                  scannerStatus.getStatus()
            ),
            new Property(
                  null,
                  ScannerStatusComplexType.PROPERTY_STATUS_MESSAGES,
                  ValueType.COLLECTION_PRIMITIVE,
                  scannerStatus.getStatusMessages()
            ),
            new Property(
                  null,
                  ScannerStatusComplexType.PROPERTY_INBOX,
                  ValueType.PRIMITIVE,
                  scannerStatus.getInbox()
            ),
            new Property(
                  null,
                  ScannerStatusComplexType.PROPERTY_PROCESSED,
                  ValueType.PRIMITIVE,
                  scannerStatus.getProcessed()
            ),
            new Property(
                  null,
                  ScannerStatusComplexType.PROPERTY_CANCELLED,
                  ValueType.PRIMITIVE,
                  scannerStatus.getCancelled()
            ),
            new Property(
                  null,
                  ScannerStatusComplexType.PROPERTY_ERROR,
                  ValueType.PRIMITIVE,
                  scannerStatus.getError()
            ),
            new Property(
                  null,
                  ScannerStatusComplexType.PROPERTY_TOTAL,
                  ValueType.PRIMITIVE,
                  scannerStatus.getTotal()
            )
      ));

      return value;
   }

   private Scanner getScanner(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      long id = getScannerIdFromParam(keyParameters);

      if (id < 0)
      {
         throw new ODataApplicationException("No Id found in request",
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }

      Scanner scanner = SCANNER_SERVICE.getScanner(id);
      if (null == scanner)
      {
         throw new ODataApplicationException("No scanner exists with this id",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }

      return scanner;
   }

   private long getScannerIdFromParam(List<UriParameter> keyParameters)
   {
      return keyParameters.stream()
            .filter(param -> param.getName().equalsIgnoreCase(PROPERTY_ID))
            .map(UriParameter::getText)
            .mapToLong(Long::parseLong)
            .findFirst()
            .orElse(-1);
   }

   private Cron extractCron(Property cronProperty, Cron cron)
   {
      if (cronProperty != null && cronProperty.getValue() != null)
      {
         List<Property> cronProperties = ((ComplexValue) cronProperty.getValue()).getValue();
         for (Property property: cronProperties)
         {
            switch (property.getName())
            {
               case CronComplexType.PROPERTY_ACTIVE:
                  cron.setActive((Boolean) property.getValue());
                  break;

               case CronComplexType.PROPERTY_SCHEDULE:
                  cron.setSchedule((String) property.getValue());
                  break;
            }
         }
         return cron;
      }
      return null;
   }

   private Entity toOlingoEntity(Scanner scanner)
   {
      ScannerInfo scannerConf = scanner.getConfig();
      if (scannerConf != null)
      {
         Entity entity = new Entity()
               .addProperty(new Property(null, PROPERTY_ID, PRIMITIVE, scannerConf.getId()))
               .addProperty(new Property(null, PROPERTY_URL, PRIMITIVE, scannerConf.getUrl()))
               .addProperty(new Property(null, PROPERTY_SOURCE_REMOVE, PRIMITIVE, scannerConf.isSourceRemove()))
               .addProperty(new Property(null, ScannerStatusComplexType.COMPLEX_TYPE_NAME, COMPLEX, toComplexValue(scanner.getStatus())));

         if (scannerConf.getPattern() != null)
         {
            entity.addProperty(new Property(null, ScannerModel.PROPERTY_PATTERN, PRIMITIVE, scannerConf.getPattern()));
         }
         if (scannerConf.getCron() != null)
         {
            entity.addProperty(makeCronProperty(scannerConf.getCron()));
         }

         if (scannerConf instanceof FileScannerConf)
         {
            entity.setType(FileScannerModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());
         }

         if (scannerConf instanceof FtpScannerConf)
         {
            FtpScannerConf ftpSC = (FtpScannerConf) scannerConf;
            if (ftpSC.getUsername() != null)
            {
               entity.addProperty(new Property(null, FtpScannerModel.USERNAME, PRIMITIVE, ftpSC.getUsername()));
            }
            if (ftpSC.getPassword() != null)
            {
               entity.addProperty(new Property(null, FtpScannerModel.PASSWORD, PRIMITIVE, ftpSC.getPassword()));
            }

            entity.setType(FtpScannerModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());
         }

         try
         {
            entity.setId(new URI(ENTITY_SET_NAME + "(" + scannerConf.getId() + ")"));
         }
         catch (URISyntaxException e)
         {
            throw new ODataRuntimeException("Unable to create id for entity: " + ENTITY_SET_NAME, e);
         }

         return entity;
      }
      else
      {
         return null;
      }
   }

   private Property makeCronProperty(Cron cron)
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
            ScannerModel.PROPERTY_CRON,
            ValueType.COMPLEX,
            complexValue);
   }

   private FileScannerConf buildFileScannerConfig(Entity entity)
   {
      FileScannerConf fsi = new FileScannerConf();
      if (containsProperty(entity, ScannerModel.PROPERTY_URL))
      {
         fsi.setUrl((String) getPropertyValue(entity, ScannerModel.PROPERTY_URL));
      }
      if (containsProperty(entity, ScannerModel.PROPERTY_PATTERN))
      {
         fsi.setPattern((String) getPropertyValue(entity, ScannerModel.PROPERTY_PATTERN));
      }
      if (containsProperty(entity, ScannerModel.PROPERTY_CRON))
      {
         fsi.setCron(extractCron(entity.getProperty(ScannerModel.PROPERTY_CRON), new Cron()));
      }
      if (containsProperty(entity, FtpScannerModel.PROPERTY_SOURCE_REMOVE))
      {
         fsi.setSourceRemove((Boolean) getPropertyValue(entity, FtpScannerModel.PROPERTY_SOURCE_REMOVE));
      }
      return fsi;
   }

   private FtpScannerConf buildFtpScannerConfig(Entity entity)
   {
      FtpScannerConf fsi = new FtpScannerConf();
      if (containsProperty(entity, ScannerModel.PROPERTY_URL))
      {
         fsi.setUrl((String) getPropertyValue(entity, ScannerModel.PROPERTY_URL));
      }
      if (containsProperty(entity, ScannerModel.PROPERTY_PATTERN))
      {
         fsi.setPattern((String) getPropertyValue(entity, ScannerModel.PROPERTY_PATTERN));
      }
      if (containsProperty(entity, ScannerModel.PROPERTY_CRON))
      {
         fsi.setCron(extractCron(entity.getProperty(ScannerModel.PROPERTY_CRON), new Cron()));
      }
      if (containsProperty(entity, FtpScannerModel.USERNAME))
      {
         fsi.setUsername((String) getPropertyValue(entity, FtpScannerModel.USERNAME));
      }
      if (containsProperty(entity, FtpScannerModel.PASSWORD))
      {
         fsi.setPassword((String) getPropertyValue(entity, FtpScannerModel.PASSWORD));
      }
      if (containsProperty(entity, FtpScannerModel.PROPERTY_SOURCE_REMOVE))
      {
         fsi.setSourceRemove((Boolean) getPropertyValue(entity, FtpScannerModel.PROPERTY_SOURCE_REMOVE));
      }
      return fsi;
   }
}
