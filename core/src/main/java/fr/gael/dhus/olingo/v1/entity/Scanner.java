/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
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

import fr.gael.dhus.database.object.config.scanner.ScannerConfiguration.Collections;
import fr.gael.dhus.database.object.config.scanner.ScannerInfo;
import fr.gael.dhus.database.object.config.scanner.ScannerManager;
import fr.gael.dhus.datastore.scanner.ScannerFactory;
import fr.gael.dhus.olingo.v1.Expander;
import fr.gael.dhus.olingo.v1.ExpectedException;
import fr.gael.dhus.olingo.v1.ExpectedException.IncompleteDocException;
import fr.gael.dhus.olingo.v1.ExpectedException.ConflictException;
import fr.gael.dhus.olingo.v1.ExpectedException.InvalidKeyException;
import fr.gael.dhus.olingo.v1.ExpectedException.InvalidTargetException;
import fr.gael.dhus.olingo.v1.Model;
import fr.gael.dhus.olingo.v1.entityset.ScannerEntitySet;
import fr.gael.dhus.olingo.v1.map.FunctionalMap;
import fr.gael.dhus.olingo.v1.visitor.CollectionFunctionalVisitor;
import fr.gael.dhus.service.CollectionService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;

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

public class Scanner extends AbstractEntity
{
   private static final ConfigurationManager CONFIGURATION_MANAGER =
         ApplicationContextProvider.getBean(ConfigurationManager.class);

   private static final CollectionService COLLECTION_SERVICE =
         ApplicationContextProvider.getBean(CollectionService.class);

   private final ScannerInfo scanner;

   public Scanner(ScannerInfo scanner)
   {
      this.scanner = scanner;
   }

   public static Scanner get(Long id) throws InvalidKeyException
   {
      ScannerInfo scannerInfo = CONFIGURATION_MANAGER.getScannerManager().get(id);
      if(scannerInfo == null)
      {
         throw new ExpectedException.InvalidKeyException(id.toString(), ScannerEntitySet.ENTITY_NAME);
      }
      return new Scanner(scannerInfo);
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
      String username = (String) properties.get(ScannerEntitySet.USERNAME);
      String password = (String) properties.get(ScannerEntitySet.PASSWORD);
      String pattern = (String) properties.get(ScannerEntitySet.PATTERN);

      if (url == null || url.isEmpty())
      {
         throw new IncompleteDocException();
      }
      if (active == null)
      {
         active = Boolean.FALSE;
      }

      String statusMessage = "Added on " + ScannerFactory.SDF.format(new Date());
      String status = ScannerManager.STATUS_ADDED;

      ScannerInfo scanner = CONFIGURATION_MANAGER.getScannerManager().
            create(url, status, statusMessage, active, username, password, pattern, true);
      return new Scanner(scanner);
   }

   public static void delete(long id)
   {
      CONFIGURATION_MANAGER.getScannerManager().delete(id);
   }

   @Override
   public Map<String, Object> toEntityResponse(String selfUrl)
   {
      Map<String, Object> entityResponse = new HashMap<>();

      entityResponse.put(ScannerEntitySet.ID, scanner.getId());
      entityResponse.put(ScannerEntitySet.URL, scanner.getUrl());
      entityResponse.put(ScannerEntitySet.STATUS, scanner.getStatus());
      entityResponse.put(ScannerEntitySet.STATUS_MESSAGE, scanner.getStatusMessage());
      entityResponse.put(ScannerEntitySet.ACTIVE, scanner.isActive());
      entityResponse.put(ScannerEntitySet.USERNAME, scanner.getUsername());
      entityResponse.put(ScannerEntitySet.PASSWORD, "******"); // hidden password
      entityResponse.put(ScannerEntitySet.PATTERN, scanner.getPattern());

      return entityResponse;
   }

   @Override
   public Object getProperty(String propName) throws ODataException
   {
      switch (propName)
      {
         case ScannerEntitySet.ID:
            return scanner.getId();
         case ScannerEntitySet.URL:
            return scanner.getUrl();
         case ScannerEntitySet.STATUS:
            return scanner.getStatus();
         case ScannerEntitySet.STATUS_MESSAGE:
            return scanner.getStatusMessage();
         case ScannerEntitySet.ACTIVE:
            return scanner.isActive();
         case ScannerEntitySet.USERNAME:
            return scanner.getUsername();
         case ScannerEntitySet.PASSWORD:
            return "******"; // hidden password
         case ScannerEntitySet.PATTERN:
            return scanner.getPattern();
         default:
            throw new ODataException("Property '" + propName + "' not found.");
      }
   }

   @Override
   public void updateFromEntry(ODataEntry entry) throws ODataException
   {
      if (scanner.getStatus().equals(ScannerManager.STATUS_RUNNING))
      {
         throw new ConflictException("Scanner is currently running, please stop it before doing any update");
      }

      // olingo handles illegal null values for us
      Map<String, Object> properties = entry.getProperties();

      if (properties.containsKey(ScannerEntitySet.ID))
      {
         scanner.setId((Long) properties.get(ScannerEntitySet.ID));
      }
      if (properties.containsKey(ScannerEntitySet.URL))
      {
         scanner.setUrl((String) properties.get(ScannerEntitySet.URL));
      }
      if (properties.containsKey(ScannerEntitySet.ACTIVE))
      {
         scanner.setActive((Boolean) properties.get(ScannerEntitySet.ACTIVE));
      }
      if (properties.containsKey(ScannerEntitySet.USERNAME))
      {
         scanner.setUsername((String) properties.get(ScannerEntitySet.USERNAME));
      }
      if (properties.containsKey(ScannerEntitySet.PASSWORD))
      {
         scanner.setPassword((String) properties.get(ScannerEntitySet.PASSWORD));
      }
      if (properties.containsKey(ScannerEntitySet.PATTERN))
      {
         scanner.setPattern((String) properties.get(ScannerEntitySet.PATTERN));
      }
      scanner.setStatusMessage(scanner.getStatusMessage() + " Updated on " + ScannerFactory.SDF.format(new Date()));

      CONFIGURATION_MANAGER.getScannerManager().update(scanner, true);
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
         throw new InvalidTargetException(Model.SCANNER.getName(),
               navigationSegment.getEntitySet().getName());
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
         List<String> existingCollections = scanner.getCollectionList();

         // create list of existing + new collection
         List<String> collectionList = new ArrayList<>();
         collectionList.addAll(existingCollections);
         collectionList.add(collection.getName());

         // set the new collection set to the scanner an update it
         Collections collections = new Collections();
         collections.setCollection(collectionList);
         scanner.setCollections(collections);
         CONFIGURATION_MANAGER.getScannerManager().update(scanner, true);
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
         List<String> existingCollections = scanner.getCollectionList();

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
         scanner.setCollections(collections);
         CONFIGURATION_MANAGER.getScannerManager().update(scanner, true);
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
   public List<Map<String, Object>> expand(String navlinkName, String selfUrl)
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
      for (String collectionName : scanner.getCollectionList())
      {
         collectionMap.put(collectionName, new Collection(COLLECTION_SERVICE.getCollectionByName(collectionName)));
      }
      return new FunctionalMap<>(collectionMap, new CollectionFunctionalVisitor());
   }
}
