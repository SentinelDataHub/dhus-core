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
package org.dhus.test.olingov4;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.request.cud.ODataDeleteRequest;
import org.apache.olingo.client.api.communication.request.cud.ODataEntityCreateRequest;
import org.apache.olingo.client.api.communication.request.cud.ODataEntityUpdateRequest;
import org.apache.olingo.client.api.communication.request.cud.UpdateType;
import org.apache.olingo.client.api.communication.request.retrieve.EdmMetadataRequest;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntityRequest;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntitySetIteratorRequest;
import org.apache.olingo.client.api.communication.response.ODataDeleteResponse;
import org.apache.olingo.client.api.communication.response.ODataEntityCreateResponse;
import org.apache.olingo.client.api.communication.response.ODataEntityUpdateResponse;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.domain.ClientEntitySetIterator;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.client.api.domain.ClientValue;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.client.core.http.BasicAuthHttpClientFactory;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmElement;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmSchema;

// TODO document or implement error cases (e.g. entity not found during read)
public class ODataOperatorV4
{
   public static final Logger LOGGER = LogManager.getLogger("");
   
   // FIXME it seems a bug in OlingoV4 causes Int64 properties to be turned
   // into Int32 properties in some cases when using JSON
   
   // private static final String APPLICATION_JSON = "application/json";
   public static final String APPLICATION_XML = "application/xml";

   private final String serviceUrl;
   private final ODataClient client;
   
   private Edm edm = null;

   private ODataOperatorV4(String serviceUrl, ODataClient client)
   {
      this.serviceUrl = serviceUrl;
      this.client = client;
   }

   public static ODataOperatorV4 initialize(String serviceUrl, String username,
         String password) throws IOException, OlingoTestException
   {
      LOGGER.info("----------------------------------------------------------------");
      LOGGER.info("   Initializing ODataOperator:");
      LOGGER.info("");
      LOGGER.info("      [ Service URL  ] " + serviceUrl);
      LOGGER.info("      [   Username   ] " + username);
      // TODO make content-type configurable
      // LOGGER.info("      [ Content Type ] " + contentType);
      LOGGER.info("----------------------------------------------------------------");
      
      ODataClient client = ODataClientFactory.getClient();
      client.getConfiguration().setHttpClientFactory(new BasicAuthHttpClientFactory(username, password));
      return new ODataOperatorV4(serviceUrl, client);
   }

   public Edm readEdm() throws IOException
   {
      if (edm == null)
      {
         EdmMetadataRequest request =
            client.getRetrieveRequestFactory().getMetadataRequest(serviceUrl);
         ODataRetrieveResponse<Edm> response = request.execute();
         edm = response.getBody();
      }
      return edm;
   }

   public void propertyEdm() throws IOException
   {
      Edm edm = readEdm();

      LOGGER.info("== DHuS ODataV4 interface metadata:");
      for (EdmSchema schema : edm.getSchemas())
      {
         for (EdmEntityType entityType : schema.getEntityTypes())
         {
            LOGGER.info("-- Entity Type: " + entityType.getFullQualifiedName());
            for (String propertyName : entityType.getPropertyNames())
            {
               EdmElement property = entityType.getProperty(propertyName);
               LOGGER.info("---- " + propertyName + " (" + property.getType().getFullQualifiedName() + ")");
            }
         }
         
         for (EdmComplexType complexType : schema.getComplexTypes())
         {
            LOGGER.info("-- Complex Type: " + complexType.getFullQualifiedName());
            for (String propertyName : complexType.getPropertyNames())
            {
               EdmElement property = complexType.getProperty(propertyName);
               LOGGER.info("---- " + propertyName + " (" + property.getType().getFullQualifiedName() + ")");
            }
         }
      }
   }

   public ClientEntitySetIterator<ClientEntitySet, ClientEntity> readEntities(
         String entitySetName)
   {
      TestManagerV4.logScenarioInfo("-- Reading Multiple Entries on " + entitySetName);
      URI absoluteUri = client.newURIBuilder(serviceUrl)
            .appendEntitySetSegment(entitySetName).build();
      return readEntities(absoluteUri);
   }

   private ClientEntitySetIterator<ClientEntitySet, ClientEntity> readEntities(
         URI absoluteUri)
   {
      ODataEntitySetIteratorRequest<ClientEntitySet, ClientEntity> request =
         client.getRetrieveRequestFactory()
               .getEntitySetIteratorRequest(absoluteUri);
      request.setAccept(APPLICATION_XML);
      ODataRetrieveResponse<ClientEntitySetIterator<ClientEntitySet, ClientEntity>> response =
         request.execute();

      return response.getBody();
   }

   public ClientEntity readEntry(String entitySetName, String keyValue)
   {
      TestManagerV4.logScenarioInfo("-- Reading single Entry '"+keyValue+"' on " + entitySetName);
      URI absoluteUri =
         client.newURIBuilder(serviceUrl).appendEntitySetSegment(entitySetName)
               .appendKeySegment(keyValue).build();
      return readEntity(absoluteUri);
   }

   private ClientEntity readEntity(URI absoluteUri)
   {
      ODataEntityRequest<ClientEntity> request =
         client.getRetrieveRequestFactory().getEntityRequest(absoluteUri);
      request.setAccept(APPLICATION_XML);
      ODataRetrieveResponse<ClientEntity> response = request.execute();
      return response.getBody();
   }

   public void createEntity(String entitySetName, ClientEntity entity)
   {
      TestManagerV4.logScenarioInfo("-- Creating Entry on " + entitySetName);
      URI URI = client.newURIBuilder(serviceUrl)
            .appendEntitySetSegment(entitySetName).build();

      ODataEntityCreateRequest<ClientEntity> req =
         client.getCUDRequestFactory().getEntityCreateRequest(URI, entity);
      ODataEntityCreateResponse<ClientEntity> res = req.execute();
      if (res.getStatusCode() == 201)
      {
         TestManagerV4.logScenarioInfo("-- Entry created on " + entitySetName);
      }

   }

   public void updateEntry(String entitySetName, String keyValue,
         ClientEntity entity)
   {
      TestManagerV4.logScenarioInfo("-- Updating Entry '"+keyValue+"' on " + entitySetName);
      URI DataStoreUpdate =
         client.newURIBuilder(serviceUrl).appendEntitySetSegment(entitySetName)
               .appendKeySegment(keyValue).build();

      ODataEntityUpdateRequest<ClientEntity> req = client.getCUDRequestFactory()
            .getEntityUpdateRequest(DataStoreUpdate, UpdateType.PATCH, entity);

      ODataEntityUpdateResponse<ClientEntity> res = req.execute();
      
      if (res.getStatusCode() == 204)
      {
         TestManagerV4.logScenarioInfo("-- Entry '"+keyValue+"' updated on " + entitySetName);
      }
   }
   
   // TODO add alternative updateEntry that uses HTTP PUT

   public void deleteEntry(String entitySetName, String keyValue)
         throws IOException
   {
      TestManagerV4.logScenarioInfo("-- Deleting Entry '"+keyValue+"' on " + entitySetName);
      URI absoluteUri =
         client.newURIBuilder(serviceUrl).appendEntitySetSegment(entitySetName)
               .appendKeySegment(keyValue).build();
      ODataDeleteRequest request =
         client.getCUDRequestFactory().getDeleteRequest(absoluteUri);
      request.setAccept(APPLICATION_XML);
      ODataDeleteResponse response = request.execute();
      
      if(response.getStatusCode() == 204)
      {
         TestManagerV4.logScenarioInfo("-- Entry '"+keyValue+"' deleted on " + entitySetName);
      }
   }

   public String prettyPrint(Collection<ClientProperty> properties)
   {
      StringBuilder b = new StringBuilder();

      for (ClientProperty entry : properties)
      {
         ClientValue value = entry.getValue();

         if (value.isComplex())
         {
            // TODO complex property support
            // ClientComplexValue cpxvalue = value.asComplex();
         }

         else if (value.isPrimitive())
         {
            b.append(entry.getName()).append(": ");
            b.append(entry.getValue()).append("\n");
         }
      }
      return b.toString();
   }

   public ODataClient getClient()
   {
      return this.client;
   }
}
