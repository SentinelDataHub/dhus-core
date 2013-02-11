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
package org.dhus.olingo.v2;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Objects;

import org.apache.http.client.methods.HttpUriRequest;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.request.cud.ODataDeleteRequest;
import org.apache.olingo.client.api.communication.request.invoke.ODataInvokeRequest;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntityRequest;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntitySetIteratorRequest;
import org.apache.olingo.client.api.communication.request.retrieve.ODataMediaRequest;
import org.apache.olingo.client.api.communication.response.ODataDeleteResponse;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.domain.ClientEntitySetIterator;
import org.apache.olingo.client.api.http.HttpUriRequestFactory;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.commons.api.http.HttpMethod;

/**
 * OData4 client.
 */
public class OData4Client
{
   private final String serviceRootURI;
   private final String basicAuthHeader;
   private final ODataClient client;

   /**
    * Creates an ODataClient for the given service URL and credentials (HTTP Basic authentication).
    *
    * @param serviceRootURL an URL to an OData service (root document). syntax: {@code http://hostname:port/path/...}
    * @param username       HTTP basic username
    * @param password       HTTP basic password
    * @throws URISyntaxException param serviceRootURL cannot be parsed into a URI
    */
   public OData4Client(String serviceRootURL, String username, String password)
         throws URISyntaxException
   {
      Objects.requireNonNull(serviceRootURL, "Parameter serviceRootURL must be set");
      serviceRootURI = new URI(serviceRootURL).toString();

      // Cannot use an EdmEnabledODataClient due to https://issues.apache.org/jira/browse/OLINGO-1440
      // client = ODataClientFactory.getEdmEnabledClient(this.serviceRootURI);
      client = ODataClientFactory.getClient();

      if (username != null && password != null)
      {
         basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
         // Decorator to add the HTTP basic Authorization header
         final HttpUriRequestFactory clientFactory = client.getConfiguration().getHttpUriRequestFactory();
         client.getConfiguration().setHttpUriRequestFactory((HttpMethod method, URI uri) ->
         {
            HttpUriRequest res = clientFactory.create(method, uri);
            res.addHeader("Authorization", basicAuthHeader);
            return res;
         });
      }
      else
      {
         basicAuthHeader = null;
      }
   }

   /**
    * Perform bound action.
    *
    * @param key           entity key
    * @param entitySetName Name of the entitySet
    * @param actionName    action name
    *
    * @return the entity result of the action
    */
   public ClientEntity performAction(Object key, String entitySetName, String actionName)
   {
      final URI actionUri = getAbsoluteUri(entitySetName)
            .appendKeySegment(key)
            .appendActionCallSegment(actionName).build();

      ODataInvokeRequest<ClientEntity> request = client.getInvokeRequestFactory()
            .getActionInvokeRequest(actionUri, ClientEntity.class);
      request.setAccept("application/json");

      return request.execute().getBody();
   }

   /**
    * Get a navigation link entity (ex: /odata/Orders(uuid)/Product).
    *
    * @param key           entity key
    * @param entitySetName entityset name
    * @param navLinkName   the name of the navigation link entity
    *
    * @return the navigation link entity
    */
   public ClientEntity navigationLinkEntity(Object key, String entitySetName, String navLinkName)
   {
      final URI navigationUri = getAbsoluteUri(entitySetName)
            .appendKeySegment(key)
            .appendNavigationSegment(navLinkName).build();

      ODataEntityRequest<ClientEntity> request =
            client.getRetrieveRequestFactory().getEntityRequest(navigationUri);
      request.setAccept("application/json");

      return request.execute().getBody();
   }

   /**
    * Get filtered entities
    *
    * @param entitySetName the name of the entitySet to filter (may be null)
    * @param filter        the filter expression (search must be null)
    * @param search        the $search parameter (filter must be null)
    *
    * @return the filtered entities
    */
   public ClientEntitySetIterator<ClientEntitySet, ClientEntity> readEntitySet(String entitySetName,
         String filter, String search)
   {
      URIBuilder uriBuilder = getAbsoluteUri(entitySetName);
      if (filter != null)
      {
         uriBuilder.filter(filter);
      }
      else if (search != null)
      {
         uriBuilder.search(search);
      }
      URI absoluteUri = uriBuilder.build();

      ODataEntitySetIteratorRequest<ClientEntitySet, ClientEntity> request =
            client.getRetrieveRequestFactory().getEntitySetIteratorRequest(absoluteUri);
      request.setAccept("application/json");
      ODataRetrieveResponse<ClientEntitySetIterator<ClientEntitySet, ClientEntity>> response = request.execute();

      return response.getBody();
   }

   /**
    * Read single entity
    *
    * @param entitySetName the name of the entitySet to filter (may be null)
    * @param key           entity key
    *
    * @return the single entity
    */
   public ClientEntity readSingleEntity(String entitySetName, Object key)
   {
      URI absoluteUri = getAbsoluteUri(entitySetName).appendKeySegment(key).build();
      ODataEntityRequest<ClientEntity> request = client.getRetrieveRequestFactory().getEntityRequest(absoluteUri);
      request.setAccept("application/json");
      ODataRetrieveResponse<ClientEntity> response = request.execute();

      return response.getBody();
   }

   /**
    * Download the media attached to an entity.
    *
    * @param entitySetName the name of the entitySet
    * @param key           the entity key
    *
    * @return an inputStream
    */
   public InputStream downloadEntityMedia(String entitySetName, Object key)
   {
      URI absoluteUri = getAbsoluteUri(entitySetName).appendKeySegment(key).build();

      final ODataEntityRequest<ClientEntity> entityRequest = client.getRetrieveRequestFactory().getEntityRequest(absoluteUri);
      final ODataMediaRequest streamRequest = client.getRetrieveRequestFactory().getMediaEntityRequest(absoluteUri);

      entityRequest.setAccept("application/octet-stream");
      final ODataRetrieveResponse<InputStream> streamResponse = streamRequest.execute();
      return streamResponse.getBody();
   }

   /**
    * Delete an entity.
    *
    * @param entitySetName the name of the entity to delete
    * @param key           the entity key
    * @return an http status code
    */
   public int deleteEntity(String entitySetName, Object key)
   {
      URI absoluteUri = getAbsoluteUri(entitySetName).appendKeySegment(key).build();
      ODataDeleteRequest request = client.getCUDRequestFactory().getDeleteRequest(absoluteUri);
      request.setAccept("application/json;odata.metadata=minimal");
      ODataDeleteResponse response = request.execute();
      return response.getStatusCode();
   }

   private URIBuilder getAbsoluteUri(String entitySetName)
   {
      return client.newURIBuilder(this.serviceRootURI).appendEntitySetSegment(entitySetName);
   }

   /**
    * Returns the service root URI passed to the constructor.
    *
    * @return service root URI
    */
   public String getServiceRoot()
   {
      return serviceRootURI;
   }

}
