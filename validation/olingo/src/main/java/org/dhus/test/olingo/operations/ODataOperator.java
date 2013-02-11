package org.dhus.test.olingo.operations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntityContainer;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderException;
import org.apache.olingo.odata2.api.ep.EntityProviderReadProperties;
import org.apache.olingo.odata2.api.ep.EntityProviderWriteProperties;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.dhus.test.olingo.TestManager;

public class ODataOperator
{
   private static final Logger LOGGER = LogManager.getLogger("");
   
   public static final String APPLICATION_JSON = "application/json";
   public static final String APPLICATION_XML = "application/xml";

   public static final int HTTP_CREATED = HttpStatusCodes.CREATED.getStatusCode(); // 201
   public static final int HTTP_NO_CONTENT = HttpStatusCodes.NO_CONTENT.getStatusCode(); // 203
   public static final int HTTP_NOT_FOUND = HttpStatusCodes.NOT_FOUND.getStatusCode(); // 404
   
   private static final String HTTP_METHOD_PUT = "PUT";
   private static final String HTTP_METHOD_POST = "POST";
   private static final String HTTP_METHOD_GET = "GET";
   private static final String HTTP_METHOD_DELETE = "DELETE";
   private static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
   private static final String HTTP_HEADER_ACCEPT = "Accept";
   private static final String METADATA = "$metadata";
   private static final String SEPARATOR = "/";
   private static final boolean PRINT_RAW_CONTENT = false;

   private static final String KEY_STRING_LIMITER = "'";
   private static final String KEY_LONG_SUFFIX = "L";

   private final String serviceUrl;
   private final String username;
   private final String password;
   private final String contentType;

   private Edm edm = null;

   private ODataOperator(String serviceUrl, String username, String password, String contentType)
   {
      this.serviceUrl = serviceUrl;
      this.username = username;
      this.password = password;
      this.contentType = contentType;
   }

   public static ODataOperator initialize(String serviceUrl, String username, String password,
         String contentType) throws IOException, ODataException
   {
      LOGGER.info("----------------------------------------------------------------");
      LOGGER.info("   Initializing ODataOperator:");
      LOGGER.info("");
      LOGGER.info("      [ Service URL  ] " + serviceUrl);
      LOGGER.info("      [   Username   ] " + username);
      LOGGER.info("      [ Content Type ] " + contentType);
      LOGGER.info("----------------------------------------------------------------");
      
      if (!contentType.equals(APPLICATION_JSON) && !contentType.equals(APPLICATION_XML))
      {
         throw new IllegalArgumentException("Invalid content type for ODataOperator");
      }
      ODataOperator odataOperator = new ODataOperator(serviceUrl, username, password, contentType);
      Edm edm = odataOperator.readEdm();
      if (edm == null)
      {
         throw new ODataException("Cannot retrieve EDM");
      }
      odataOperator.edm = edm;
      return odataOperator;
   }

   /**
    * The Entity Data Model (EDM) defines all metadata information about the
    * provided data of the OData service. This includes all entities with their
    * type, properties and relations.
    * 
    * @return metadata
    */
   public Edm readEdm() throws IOException, ODataException
   {
      InputStream content = execute(serviceUrl + "/" + METADATA, APPLICATION_XML, HTTP_METHOD_GET);
      if(content == null)
      {
         return null;
      }
      return EntityProvider.readMetadata(content, false);
   }

   /**
    * To create an entity, an HTTP POST on the URI of the corresponding entity
    * is sent to the OData service. If the entry was created successfully an
    * HTTP Status: 201 created will be returned as well as the complete entry.
    * 
    * @param entitySetName
    * @param dataMap
    * @return
    * @throws Exception
    */
   public ODataEntry createEntry(String entitySetName, Map<String, Object> dataMap)
         throws Exception
   {
      TestManager.logScenarioInfo("-- Creating Entry on " + entitySetName);
      String absolutUri = createUri(entitySetName, null);
      HttpURLConnection connection = initializeConnection(absolutUri, contentType, HTTP_METHOD_POST);

      EntityProviderWriteProperties properties =
            EntityProviderWriteProperties.serviceRoot(new URI(entitySetName)).build();

      EdmEntitySet entitySet = getEntitySet(entitySetName);
      ODataResponse response = EntityProvider.writeEntry(contentType, entitySet, dataMap, properties);
      return checkWriteEntityResponse(absolutUri, HTTP_METHOD_POST, connection, entitySet, response);
   }

   /**
    * To create an entity, an HTTP POST on the URI of the corresponding entity
    * is sent to the OData service. If the entry was created successfully an
    * HTTP Status: 201 created will be returned as well as the complete entry.
    * 
    * @param entitySetName
    * @param dataByte
    * @return
    * @throws Exception
    */
   public ODataEntry createEntryFromBinary(String entitySetName, byte[] dataByte) throws Exception
   {
      TestManager.logScenarioInfo("-- Creating Entry from binary on " + entitySetName);
      String absolutUri = createUri(entitySetName, null);
      HttpURLConnection connection = initializeConnection(absolutUri, contentType, HTTP_METHOD_POST);

      EdmEntitySet entitySet = getEntitySet(entitySetName);
      ODataResponse response = EntityProvider.writeBinary(contentType, dataByte);      
      return checkWriteEntityResponse(absolutUri, HTTP_METHOD_POST, connection, entitySet, response);
   }
   
   private ODataEntry checkWriteEntityResponse(String absolutUri, String httpMethod,
         HttpURLConnection connection, EdmEntitySet entitySet, ODataResponse response)
         throws IOException, EntityProviderException
   {
      Object entity = response.getEntity();
      if (entity instanceof InputStream)
      {
         byte[] buffer = streamToArray((InputStream) entity);
         connection.getOutputStream().write(buffer);
      }
   
      // check response status code and return create entry
      if (HttpStatusCodes.fromStatusCode(connection.getResponseCode()) == HttpStatusCodes.CREATED)
      {
         InputStream content = connection.getInputStream();
         content = logRawContent(httpMethod + " request on uri '" + absolutUri + "' with content:\n  ", content, "\n");
         return EntityProvider.readEntry(contentType, entitySet, content,
               EntityProviderReadProperties.init().build());
      }
   
      // should we return null?
      return null;
   }

   /**
    * To read a single ODataEntry, an HTTP GET request is sent to the ODtata
    * service, with a key value for the creation of the uri
    * 
    * @param keyValue unique identifier (Id)
    * @return readEntry
    */
   public ODataEntry readEntry(String entitySetName, String keyValue)
         throws IOException, ODataException
   {
      TestManager.logScenarioInfo("-- Reading single Entry '"+keyValue+"' on " + entitySetName);
      return readEntry(entitySetName, keyValue, null);
   }

   public ODataEntry readEntry(String entitySetName, String keyValue, String expandRelationName)
         throws IOException, ODataException
   {
      EdmEntityContainer entityContainer = edm.getDefaultEntityContainer();
      String absolutUri = createUri(entitySetName, keyValue, expandRelationName);
      InputStream content = execute(absolutUri, contentType, HTTP_METHOD_GET);
      if(content == null)
      {
         return null;
      }

      return EntityProvider.readEntry(contentType, entityContainer.getEntitySet(entitySetName),
            content, EntityProviderReadProperties.init().build());
   }

   /**
    * The ODataFeed send the HTTP GET request on OData service for allows to
    * read all entries corresponding to the entitySetName
    * 
    * @return readFeed
    */

   public ODataFeed readFeed(String entitySetName) throws IOException, ODataException
   {
      TestManager.logScenarioInfo("-- Reading Multiple Entries on " + entitySetName);
      EdmEntityContainer entityContainer = edm.getDefaultEntityContainer();
      String absolutUri = createUri(entitySetName, null);
      InputStream content = execute(absolutUri, contentType, HTTP_METHOD_GET);
      if(content == null)
      {
         return null;
      }
      
      return EntityProvider.readFeed(contentType, entityContainer.getEntitySet(entitySetName),
            content, EntityProviderReadProperties.init().build());
   }

   /**
    * To update an entity, the body of the updateEntry method is sent with the
    * HTTP PUT request on the OData service
    * 
    * @param id unique identifier(key)
    * @param data Product
    * @return 
    */
   public int updateEntry(String entitySetName, String id, Map<String, Object> data)
         throws Exception
   {
      TestManager.logScenarioInfo("-- Updating Entry '"+id+"' on " + entitySetName);
      String absolutUri = createUri(entitySetName, id);
      return updateEntity(absolutUri, entitySetName, data, HTTP_METHOD_PUT);
   }

   private int updateEntity(String absolutUri, String entitySetName, Map<String, Object> data,
         String httpMethod) throws EdmException, MalformedURLException, IOException,
         EntityProviderException, URISyntaxException
   {
      HttpURLConnection connection = initializeConnection(absolutUri, contentType, httpMethod);
      EdmEntitySet entitySet = getEntitySet(entitySetName);
      URI rootUri = new URI(serviceUrl + SEPARATOR);
      EntityProviderWriteProperties properties =
         EntityProviderWriteProperties.serviceRoot(rootUri).build();
      ODataResponse response = EntityProvider.writeEntry(contentType, entitySet, data, properties);
      Object entity = response.getEntity();
      if (entity instanceof InputStream)
      {
         byte[] buffer = streamToArray((InputStream) entity);
         connection.getOutputStream().write(buffer);
      }
      int statusCode = connection.getResponseCode();
      connection.disconnect();
      return statusCode;
   }

   /**
    * To delete an entry, the body of the deleteEntry method is sent with the
    * HTTP DELETE request on the OData service
    * 
    * @param id unique identifier (key)
    * @return an HTTP Status: 204 No content, if the entry was deleted
    *         successfully.
    */
   public int deleteEntry(String entityName, String id) throws IOException
   {
      TestManager.logScenarioInfo("-- Deleting Entry '"+id+"' on " + entityName);
      String absolutUri = createUri(entityName, id);
      HttpURLConnection connection = connect(absolutUri, contentType, HTTP_METHOD_DELETE);
      int statusCode = connection.getResponseCode();
      connection.disconnect();
      return statusCode;
   }

   /* HTTP methods */
   /**
    * Executes an HTTP request, returns null if the response code is 404.
    * @param relativeUri
    * @param contentType
    * @param httpMethod
    * @return
    * @throws IOException
    */
   private InputStream execute(String relativeUri, String contentType, String httpMethod)
         throws IOException
   {
      HttpURLConnection connection = initializeConnection(relativeUri, contentType, httpMethod);
      connection.connect();
      
      // resource not found
      if(connection.getResponseCode() == HTTP_NOT_FOUND)
      {
         connection.disconnect();
         return null;
      }
      
      checkStatus(connection);
      InputStream content = connection.getInputStream();
      content = logRawContent(httpMethod + " request on uri '" + relativeUri + "' with content:\n  ",
            content, "\n");
      return content;
   }

   private HttpURLConnection connect(String relativeUri, String contentType, String httpMethod)
         throws IOException
   {
      HttpURLConnection connection = initializeConnection(relativeUri, contentType, httpMethod);
      connection.connect();
      checkStatus(connection);
      return connection;
   }

   private HttpURLConnection initializeConnection(String absolutUri, String contentType,
         String httpMethod) throws MalformedURLException, IOException
   {
      URL url = new URL(absolutUri);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod(httpMethod);
      connection.setRequestProperty(HTTP_HEADER_ACCEPT, contentType);
      String authorizationHeader = getAuthorizationHeader();
      connection.setRequestProperty("Authorization", authorizationHeader);
      if (HTTP_METHOD_POST.equals(httpMethod) || HTTP_METHOD_PUT.equals(httpMethod))
      {
         connection.setDoOutput(true);
         connection.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, contentType);
      }
      return connection;
   }
   
   /* Utility methods */
   /**
    * Converts the given object to a valid parameter for OData v2.
    * @param key object to convert.
    * @return a string representation of the parameter
    */
   public String toODataParameter(Object key)
   {
      Objects.requireNonNull(key);
      if (key instanceof String)
      {
         return KEY_STRING_LIMITER + key + KEY_STRING_LIMITER;
      }
      if (key instanceof Long)
      {
         return key.toString() + KEY_LONG_SUFFIX;
      }
   
      return key.toString();
   }

   public String prettyPrint(Map<String, Object> properties)
   {
      StringBuilder b = new StringBuilder();
      Set<Entry<String, Object>> entries = properties.entrySet();
      for (Entry<String, Object> entry : entries)
      {
         b.append(entry.getKey()).append(": ");
         Object value = entry.getValue();
         b.append(value).append("\n");
      }
      return b.toString();
   }

   private EdmEntitySet getEntitySet(String entitySetName) throws EdmException
   {
      return edm.getDefaultEntityContainer().getEntitySet(entitySetName);
   }

   private String createUri(String entitySetName, String id)
   {
      return createUri(entitySetName, id, null);
   }

   private String createUri(String entitySetName, String id, String expand)
   {
      final StringBuilder absolutUri =
         new StringBuilder(serviceUrl).append(SEPARATOR).append(entitySetName);
      if (id != null)
      {
         absolutUri.append("(").append(id).append(")");
      }
      if (expand != null)
      {
         absolutUri.append("/?$expand=").append(expand);
      }
      return absolutUri.toString();
   }

   private HttpStatusCodes checkStatus(HttpURLConnection connection) throws IOException
   {
      int statusCode = connection.getResponseCode();
      if (400 <= statusCode && statusCode <= 599)
      {
         throw new RuntimeException("Http Connection failed with status " + statusCode);
      }
      return HttpStatusCodes.fromStatusCode(statusCode);
   }

   private String getAuthorizationHeader()
   {
      String temp = new StringBuilder(username).append(":").append(password).toString();
      String result = "Basic " + new String(Base64.encodeBase64(temp.getBytes()));
      return result;
   }

   private byte[] streamToArray(InputStream stream) throws IOException
   {
      byte[] result = new byte[0];
      byte[] tmp = new byte[8192];
      int readCount = stream.read(tmp);
      while (readCount >= 0)
      {
         byte[] innerTmp = new byte[result.length + readCount];
         System.arraycopy(result, 0, innerTmp, 0, result.length);
         System.arraycopy(tmp, 0, innerTmp, result.length, readCount);
         result = innerTmp;
         readCount = stream.read(tmp);
      }
      stream.close();
      return result;
   }

   private InputStream logRawContent(String prefix, InputStream content, String postfix)
         throws IOException
   {
      if (PRINT_RAW_CONTENT)
      {
         byte[] buffer = streamToArray(content);
         System.out.println(prefix + new String(buffer) + postfix);
         return new ByteArrayInputStream(buffer);
      }
      return content;
   }
}
