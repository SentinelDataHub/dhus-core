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
package org.dhus.store.datastore.remotedhus;

import fr.gael.dhus.olingo.ODataClient;
import fr.gael.dhus.util.http.Timeouts;
import fr.gael.drb.DrbAttribute;
import fr.gael.drb.DrbAttributeList;
import fr.gael.drb.DrbDefaultAttribute;
import fr.gael.drb.DrbDefaultAttributeList;
import fr.gael.drb.DrbDefaultNodeList;
import fr.gael.drb.DrbNode;
import fr.gael.drb.DrbNodeList;
import fr.gael.drb.impl.DrbNodeImpl;
import fr.gael.drb.value.Value;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataException;

public class DhusODataV1Node extends DrbNodeImpl
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final String ATTRIBUTES = "/Attributes";
   private static final String NODES = "/Nodes";
   private static final String GET_STREAM = "/$value";
   private static final String OPEN_KEY_PREDICATE = "('";
   private static final String CLOSE_KEY_PREDICATE = "')";

   private static final String PROPERTY_ID = "Id";
   private static final String PROPERTY_NAME = "Name";
   private static final String PROPERTY_CHILDREN_NUM = "ChildrenNumber";
   private static final String PROPERTY_CONTENT_LENGTH = "ContentLength";
   private static final String PROPERTY_CONTENT_TYPE = "ContentType";
   private static final String PROPERTY_VALUE = "Value";

   private final String login;
   private final String password;

   private final String serviceUrl;
   private final String nodeResourceParts;
   private final ODataClient client;
   private final DhusODataV1Node parent;

   private ODataEntry odataEntry = null;

   private DhusODataV1Node(DhusODataV1Node parent, ODataEntry entry, String part)
   {
      this.odataEntry = entry;
      this.nodeResourceParts = parent.nodeResourceParts + part;
      this.parent = parent;
      this.client = parent.client;
      this.serviceUrl = parent.serviceUrl;
      this.login = parent.login;
      this.password = parent.password;
   }

   private DhusODataV1Node(DhusODataV1Node parent, String nodeUrl)
   {
      this(parent, null, nodeUrl);
   }

   DhusODataV1Node(String nodeUrl, String login, String password, ODataClient odataClient)
   {
      this.serviceUrl = nodeUrl.substring(0, nodeUrl.indexOf("Products"));
      this.client = odataClient;
      this.nodeResourceParts = nodeUrl.substring(nodeUrl.indexOf("Products"));
      this.login = login;
      this.password = password;
      this.parent = null; // root node, no parent
   }

   @Override
   public DrbAttributeList getAttributes()
   {
      DrbDefaultAttributeList attributeList = new DrbDefaultAttributeList();
      String path = nodeResourceParts + ATTRIBUTES;
      try
      {
         client.readFeed(path, Collections.emptyMap()).getEntries().stream().map(
               entry -> {
                  Map<String, Object> properties = entry.getProperties();
                  return new DrbDefaultAttribute(
                        (String) properties.get(PROPERTY_NAME),
                        new fr.gael.drb.value.String((String) properties.get(PROPERTY_VALUE)));
               })
               .forEach(attributeList::addItem);
         return attributeList;
      }
      catch (IOException | ODataException | InterruptedException e)
      {
         if (InterruptedException.class.isAssignableFrom(e.getClass()))
         {
            Thread.currentThread().interrupt(); // reset interrupt flag to true
         }
         throw new IllegalStateException(
               "Cannot generate remote attributes from " + serviceUrl + path, e);
      }
   }

   @Override
   public DrbAttribute getAttribute(String name)
   {
      if ("size".equals(name))
      {
         Long size = (Long) getODataEntry().getProperties().get(PROPERTY_CONTENT_LENGTH);
         return new DrbDefaultAttribute(name, new fr.gael.drb.value.String(size.toString()));
      }

      String path = nodeResourceParts + ATTRIBUTES + stringKeyPredicate(name);
      try
      {
         ODataEntry entry = client.readEntry(path, Collections.emptyMap());
         return new DrbDefaultAttribute(name,
               new fr.gael.drb.value.String((String) entry.getProperties().get(PROPERTY_VALUE)));
      }
      catch (IOException | ODataException | InterruptedException e)
      {
         if (InterruptedException.class.isAssignableFrom(e.getClass()))
         {
            Thread.currentThread().interrupt(); // reset interrupt flag to true
         }
         throw new IllegalStateException("Cannot generate remote attribute from: " + serviceUrl + path, e);
      }
   }

   @Override
   public DrbNode getParent()
   {
      return parent;
   }

   @Override
   public int getChildrenCount()
   {
      return ((Long) getODataEntry().getProperties().get(PROPERTY_CHILDREN_NUM)).intValue();
   }

   @Override
   public boolean hasChild()
   {
      return getChildrenCount() > 0;
   }

   @Override
   public DrbNodeList getChildren()
   {
      DrbDefaultNodeList nodeList = new DrbDefaultNodeList();
      final String url = nodeResourceParts + NODES;
      try
      {
         client.readFeed(url, Collections.emptyMap()).getEntries().stream().map(
               entry -> new DhusODataV1Node(
                     this,
                     entry,
                     NODES + stringKeyPredicate((String) entry.getProperties().get(PROPERTY_ID))))
               .forEach(nodeList::addItem);
         return nodeList;
      }
      catch (IOException | ODataException | InterruptedException e)
      {
         if (InterruptedException.class.isAssignableFrom(e.getClass()))
         {
            Thread.currentThread().interrupt(); // reset interrupt flag to true
         }
         throw new IllegalStateException("Cannot generate remote nodes from " + url, e);
      }
   }

   @Override
   public DrbNode getChildAt(int index)
   {
      String part = nodeResourceParts + NODES;
      try
      {
         ODataEntry entry = client.readFeed(part, Collections.emptyMap()).getEntries().get(index);
         part = NODES + stringKeyPredicate((String) entry.getProperties().get(PROPERTY_ID));
         return new DhusODataV1Node(this, entry, part);
      }
      catch (IndexOutOfBoundsException e)
      {
         return null;
      }
      catch (IOException | ODataException | InterruptedException e)
      {
         if (InterruptedException.class.isAssignableFrom(e.getClass()))
         {
            Thread.currentThread().interrupt(); // reset interrupt flag to true
         }
         throw new IllegalStateException("Cannot generate remote node: " + serviceUrl + part, e);
      }
   }

   @Override
   public DrbNode getNamedChild(String name, int occurrence)
   {
      // FIXME: In multiple occurrence case, this will not work...
      if (occurrence > 1)
      {
         return null;
      }
      return new DhusODataV1Node(this,NODES + stringKeyPredicate(name));
   }

   @Override
   public String getPath()
   {
      String[] splittedResourceParts = nodeResourceParts.split("/");
      if (splittedResourceParts.length == 1)
      {
         return "/";
      }

      StringBuilder path = new StringBuilder();
      int i = 1;
      do
      {
         path.append("/").append(getStringKeyPredicate(splittedResourceParts[i]));
         i++;
      }
      while (i < splittedResourceParts.length);
      return path.toString();
   }

   @Override
   public String getName()
   {
      return (String) getODataEntry().getProperties().get(PROPERTY_NAME);
   }

   @Override
   public int getOccurrence()
   {
      return 1;
   }

   @Override
   public boolean hasImpl(@SuppressWarnings("rawtypes") Class api)
   {
      return InputStream.class.isAssignableFrom(api) || super.hasImpl(api);
   }

   @Override
   public Value getValue()
   {
      String value = (String) getODataEntry().getProperties().get(PROPERTY_VALUE);
      return value == null ? null : new fr.gael.drb.value.String(value);
   }

   public String getContentType()
   {
      return (String) getODataEntry().getProperties().get(PROPERTY_CONTENT_TYPE);
   }

   @Override
   public Object getImpl(@SuppressWarnings("rawtypes") Class api)
   {
      if (InputStream.class.isAssignableFrom(api))
      {
         String url = serviceUrl + nodeResourceParts + GET_STREAM;
         try
         {
            URLConnection connection = new URL(url).openConnection();
            connection.setConnectTimeout(Timeouts.CONNECTION_TIMEOUT);
            connection.setReadTimeout(Timeouts.SOCKET_TIMEOUT);
            String basicAuth = "Basic " + new String(
                  Base64.getEncoder().encode((login + ":" + password).getBytes()));
            connection.setRequestProperty("Authorization", basicAuth);
            return connection.getInputStream();
         }
         catch (IOException e)
         {
            LOGGER.error("Cannot access remote stream from: {}", url, e);
            return null;
         }
      }
      return super.getImpl(api);
   }

   private String stringKeyPredicate(final String name)
   {
      return OPEN_KEY_PREDICATE + name + CLOSE_KEY_PREDICATE;
   }

   private String getStringKeyPredicate(final String segment)
   {
      return segment.substring(
            segment.indexOf(OPEN_KEY_PREDICATE) + OPEN_KEY_PREDICATE.length(),
            segment.lastIndexOf(CLOSE_KEY_PREDICATE)
      );
   }

   private ODataEntry getODataEntry()
   {
      if (odataEntry == null)
      {
         try
         {
            this.odataEntry = client.readEntry(nodeResourceParts, Collections.emptyMap());
         }
         catch (IOException | ODataException | InterruptedException e)
         {
            if (InterruptedException.class.isAssignableFrom(e.getClass()))
            {
               Thread.currentThread().interrupt(); // reset interrupt flag to true
            }
         }
      }
      return odataEntry;
   }
}
