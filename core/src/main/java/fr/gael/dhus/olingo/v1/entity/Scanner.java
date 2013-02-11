/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2018 GAEL Systems
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
package fr.gael.dhus.olingo.v1.entity;

import fr.gael.dhus.database.object.config.scanner.FileScannerConf;
import fr.gael.dhus.database.object.config.scanner.ScannerConfiguration.Collections;
import fr.gael.dhus.datastore.scanner.ScannerException;
import fr.gael.dhus.datastore.scanner.ScannerStatus;
import fr.gael.dhus.olingo.v1.Expander;
import fr.gael.dhus.olingo.v1.ExpectedException;
import fr.gael.dhus.olingo.v1.ExpectedException.ConflictException;
import fr.gael.dhus.olingo.v1.ExpectedException.IncompleteDocException;
import fr.gael.dhus.olingo.v1.ExpectedException.InvalidKeyException;
import fr.gael.dhus.olingo.v1.ExpectedException.InvalidTargetException;
import fr.gael.dhus.olingo.v1.Model;
import fr.gael.dhus.olingo.v1.entityset.ScannerEntitySet;
import fr.gael.dhus.olingo.v1.map.FunctionalMap;
import fr.gael.dhus.olingo.v1.visitor.CollectionFunctionalVisitor;
import fr.gael.dhus.service.CollectionService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.uri.NavigationSegment;
import org.apache.olingo.odata2.api.uri.UriInfo;
import org.apache.olingo.odata2.api.uri.info.DeleteUriInfo;

import org.dhus.scanner.ScannerContainer;
import org.dhus.scanner.config.ScannerInfo;
import org.dhus.scanner.service.ScannerService;

public class Scanner extends AbstractEntity
{
   private static final ScannerService SCANNER_SERVICE =
         ApplicationContextProvider.getBean(ScannerService.class);

   private static final CollectionService COLLECTION_SERVICE =
         ApplicationContextProvider.getBean(CollectionService.class);

   private final fr.gael.dhus.datastore.scanner.Scanner scanner;

   public Scanner(fr.gael.dhus.datastore.scanner.Scanner scanner)
   {
      this.scanner = scanner;
   }

   public static Scanner get(Long id) throws InvalidKeyException
   {
      fr.gael.dhus.datastore.scanner.Scanner scanner = SCANNER_SERVICE.getScanner(id);
      if (scanner == null)
      {
         throw new ExpectedException.InvalidKeyException(id.toString(), ScannerEntitySet.ENTITY_NAME);
      }
      return new Scanner(scanner);
   }

   /**
    * Creates an new Scanner from the given ODataEntry.
    *
    * @param odataEntry created by a POST request on the OData interface
    * @return new instance
    * @throws ODataException if the given entry is malformed (eg: missing required properties)
    */
   public static Scanner create(ODataEntry odataEntry) throws ODataException
   {
      Map<String, Object> properties = odataEntry.getProperties();
      String url = (String) properties.get(ScannerEntitySet.URL);
      Boolean active = (Boolean) properties.get(ScannerEntitySet.ACTIVE);
      String pattern = (String) properties.get(ScannerEntitySet.PATTERN);

      if (url == null || url.isEmpty())
      {
         throw new IncompleteDocException();
      }
      if (active == null)
      {
         active = Boolean.FALSE;
      }

      String statusMessage = "Added on " + ScannerContainer.SDF.format(new Date());
      String status = ScannerStatus.STATUS_ADDED;

      ScannerInfo config = new FileScannerConf();
      config.setId(-1);
      config.setUrl(url);
      config.setPattern(pattern);
      config.setActive(active);

      try
      {
         fr.gael.dhus.datastore.scanner.Scanner scanner = SCANNER_SERVICE.createScanner(config);
         scanner.getStatus().addStatusMessage(statusMessage);
         scanner.getStatus().setStatus(status);

         return new Scanner(scanner);
      }
      catch (ScannerException e)
      {
         throw new ODataException("Cannot create scanner: " + e.getMessage());
      }
   }

   public static void delete(long id) throws ODataException
   {
      try
      {
         SCANNER_SERVICE.deleteScanner(id);
      }
      catch (ScannerException e)
      {
         throw new ODataException("Cannot delete scanner: " + e.getMessage());
      }
   }

   @Override
   public Map<String, Object> toEntityResponse(String selfUrl)
   {
      Map<String, Object> entityResponse = new HashMap<>();

      entityResponse.put(ScannerEntitySet.ID, scanner.getConfig().getId());
      entityResponse.put(ScannerEntitySet.URL, scanner.getConfig().getUrl());
      entityResponse.put(ScannerEntitySet.STATUS, scanner.getStatus().getStatus());
      entityResponse.put(ScannerEntitySet.STATUS_MESSAGE, scanner.getStatus().getStatusMessage());
      entityResponse.put(ScannerEntitySet.ACTIVE, scanner.getConfig().isActive());
      entityResponse.put(ScannerEntitySet.USERNAME, scanner.getConfig().getUsername());
      entityResponse.put(ScannerEntitySet.PASSWORD, "******"); // hidden password
      entityResponse.put(ScannerEntitySet.PATTERN, scanner.getConfig().getPattern());

      return entityResponse;
   }

   @Override
   public Object getProperty(String propName) throws ODataException
   {
      switch (propName)
      {
         case ScannerEntitySet.ID:
            return scanner.getConfig().getId();
         case ScannerEntitySet.URL:
            return scanner.getConfig().getUrl();
         case ScannerEntitySet.STATUS:
            return scanner.getStatus().getStatus();
         case ScannerEntitySet.STATUS_MESSAGE:
            return scanner.getStatus().getStatusMessage();
         case ScannerEntitySet.ACTIVE:
            return scanner.getConfig().isActive();
         case ScannerEntitySet.USERNAME:
            return scanner.getConfig().getUsername();
         case ScannerEntitySet.PASSWORD:
            return "******"; // hidden password
         case ScannerEntitySet.PATTERN:
            return scanner.getConfig().getPattern();
         default:
            throw new ODataException("Property '" + propName + "' not found");
      }
   }

   @Override
   public void updateFromEntry(ODataEntry entry) throws ODataException
   {
      if (scanner.getStatus().getStatus().equalsIgnoreCase(ScannerStatus.STATUS_RUNNING))
      {
         throw new ConflictException("Scanner is currently running, please stop it before doing any update");
      }

      // olingo handles illegal null values for us
      Map<String, Object> properties = entry.getProperties();

      ScannerInfo config = scanner.getConfig();

      if (properties.containsKey(ScannerEntitySet.ID))
      {
         config.setId((Long) properties.get(ScannerEntitySet.ID));
      }
      if (properties.containsKey(ScannerEntitySet.URL))
      {
         config.setUrl((String) properties.get(ScannerEntitySet.URL));
      }
      if (properties.containsKey(ScannerEntitySet.ACTIVE))
      {
         config.setActive((Boolean) properties.get(ScannerEntitySet.ACTIVE));
      }
      if (properties.containsKey(ScannerEntitySet.USERNAME))
      {
         config.setUsername((String) properties.get(ScannerEntitySet.USERNAME));
      }
      if (properties.containsKey(ScannerEntitySet.PASSWORD))
      {
         config.setPassword((String) properties.get(ScannerEntitySet.PASSWORD));
      }
      if (properties.containsKey(ScannerEntitySet.PATTERN))
      {
         config.setPattern((String) properties.get(ScannerEntitySet.PATTERN));
      }

      try
      {
         SCANNER_SERVICE.updateScanner(config);
         scanner.getStatus().addStatusMessage("Updated on " + ScannerContainer.SDF.format(new Date()));
      }
      catch (ScannerException e)
      {
         throw new ODataException("Cannot create update scanner: " + e.getMessage());
      }
   }

   @Override
   public Object navigate(NavigationSegment navigationSegment) throws ODataException
   {
      EdmEntitySet entitySet = navigationSegment.getEntitySet();

      if (entitySet.getName().equals(Model.COLLECTION.getName()))
      {
         FunctionalMap<String, Collection> collectionMap = makeCollectionMap();
         // single entity case
         if (!navigationSegment.getKeyPredicates().isEmpty())
         {
            return collectionMap.get(navigationSegment.getKeyPredicates().get(0).getLiteral());
         }
         // multiple entity case
         else
         {
            return collectionMap;
         }
      }
      else
      {
         throw new InvalidTargetException(Model.SCANNER.getName(), navigationSegment.getEntitySet().getName());
      }
   }

   @Override
   public void createLink(UriInfo link) throws ODataException
   {
      EdmEntitySet targetEntitySet = link.getTargetEntitySet();
      if (targetEntitySet.getName().equals(Model.COLLECTION.getName()))
      {
         // retrieve collection to be linked
         String collectionName = link.getTargetKeyPredicates().get(0).getLiteral();
         String collectionUuid = COLLECTION_SERVICE.getCollectionUUIDByName(collectionName);
         fr.gael.dhus.database.object.Collection collection =
               COLLECTION_SERVICE.getCollection(collectionUuid);

         // retrieve existing list of collections for this scanner
         ScannerInfo config = scanner.getConfig();
         List<String> existingCollections = config.getCollectionList();

         // create list of existing + new collection
         List<String> collectionList = new ArrayList<>();
         collectionList.addAll(existingCollections);
         collectionList.add(collection.getName());

         // set the new collection set to the scanner an update it
         Collections collections = new Collections();
         collections.setCollection(collectionList);
         config.setCollections(collections);
         try
         {
            SCANNER_SERVICE.updateScanner(config);
         }
         catch (ScannerException e)
         {
            throw new ODataException("Cannot create link from scanner " + e.getMessage());
         }
      }
      else
      {
         throw new ODataException(
               String.format("Cannot create link from %s to %s",
                     Model.SCANNER.getName(), targetEntitySet.getName()));
      }
   }

   @Override
   public void deleteLink(DeleteUriInfo link) throws ODataException
   {
      EdmEntitySet targetEntitySet = link.getTargetEntitySet();
      if (targetEntitySet.getName().equals(Model.COLLECTION.getName()))
      {
         String collectionToDelete = link.getTargetKeyPredicates().get(0).getLiteral();

         // retrieve existing list of collections for this scanner
         ScannerInfo config = scanner.getConfig();
         List<String> existingCollections = config.getCollectionList();

         List<String> collectionList = new ArrayList<>();
         for (String collectionName: existingCollections)
         {
            // add all but the collection to delete
            if (!collectionToDelete.equals(collectionName))
            {
               collectionList.add(collectionName);
            }
         }
         // set the new collection set to the scanner an update it
         Collections collections = new Collections();
         collections.setCollection(collectionList);
         config.setCollections(collections);
         try
         {
            SCANNER_SERVICE.updateScanner(config);
         }
         catch (ScannerException e)
         {
            throw new ODataException("Cannot delete link for scanner " + e.getMessage());
         }
      }
      else
      {
         throw new ODataException(String.format("Cannot delete link from %s to %s",
               Model.SCANNER.getName(), targetEntitySet.getName()));
      }
   }

   @Override
   public List<String> getExpandableNavLinkNames()
   {
      return Arrays.asList("Collections");
   }

   @Override
   public List<Map<String, Object>> expand(String navlinkName, String selfUrl) throws ODataException
   {
      switch (navlinkName)
      {
         case "Collections":
            return Expander.mapToData(makeCollectionMap(), selfUrl);
         default:
            return super.expand(navlinkName, selfUrl);
      }
   }

   private FunctionalMap<String, Collection> makeCollectionMap()
   {
      // TODO is creating this map Scanner's role?
      Map<String, Collection> collectionMap = new HashMap<>();
      for (String collectionName: scanner.getConfig().getCollectionList())
      {
         collectionMap.put(collectionName, new Collection(COLLECTION_SERVICE.getCollectionByName(collectionName)));
      }
      return new FunctionalMap<>(collectionMap, new CollectionFunctionalVisitor());
   }
}
