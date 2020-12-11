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

import fr.gael.dhus.datastore.processing.ProcessingUtils;

import fr.gael.drb.DrbAttribute;
import fr.gael.drb.DrbNode;
import fr.gael.drb.impl.spi.DrbNodeSpi;
import fr.gael.drb.value.Value;

import fr.gael.odata.engine.data.DataHandlerUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;

import org.dhus.olingo.v2.datamodel.ItemModel;
import org.dhus.olingo.v2.datamodel.NodeModel;

public class NodeEntity extends Entity
{
   private final DrbNode drbNode;

   public static NodeEntity initialize(DrbNode drbNode)
   {
      Objects.requireNonNull(drbNode, "Node must be non-null");

      NodeEntity nodeEntity = new NodeEntity(drbNode);

      // ID
      nodeEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_ID,
            ValueType.PRIMITIVE,
            drbNode.getName()));

      // Name
      nodeEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_NAME,
            ValueType.PRIMITIVE,
            drbNode.getName()));

      // ContentType
      nodeEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_CONTENTTYPE,
            ValueType.PRIMITIVE,
            getContentType(drbNode)));

      // ContentLength
      nodeEntity.addProperty(new Property(
            null,
            ItemModel.PROPERTY_CONTENTLENGTH,
            ValueType.PRIMITIVE,
            getContentLength(drbNode)));

      // ChildrenCount
      nodeEntity.addProperty(new Property(
            null,
            NodeModel.PROPERTY_CHILDREN_COUNT,
            ValueType.PRIMITIVE,
            getChildrenCount(drbNode)));

      // Value
      nodeEntity.addProperty(new Property(
            null,
            NodeModel.PROPERTY_VALUE,
            ValueType.PRIMITIVE,
            getValue(drbNode)));

      // Set Id
      nodeEntity.setId(DataHandlerUtil.createEntityId(
            NodeModel.ENTITY_SET_NAME,
            drbNode.getName()));

      nodeEntity.setType(NodeModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());
      nodeEntity.setMediaContentType(getContentType(drbNode));

      return nodeEntity;
   }

   private NodeEntity(DrbNode drbNode)
   {
      this.drbNode = drbNode;
   }

   public DrbNode getDrbNode()
   {
      return drbNode;
   }

   private static String getContentType(DrbNode drbNode)
   {
      return ProcessingUtils.getClassLabelFromNode(drbNode);
   }

   private static Long getContentLength(DrbNode drbNode)
   {
      Long contentLength = -1L;
      if (hasStream(drbNode))
      {
         InputStream stream = getStream(drbNode);
         if (contentLength == -1)
         {
            DrbAttribute attr = drbNode.getAttribute("size");
            if (attr != null)
            {
               try
               {
                  contentLength = Long.decode(attr.getValue().toString());
               }
               catch (NumberFormatException nfe)
               {
                  // Error in attribute...
               }
            }
         }
         // Still not initialized ?
         if (stream instanceof FileInputStream)
         {
            try
            {
               contentLength = ((FileInputStream) stream).getChannel().size();
            }
            catch (IOException e)
            {
               // Error while accessing file size: using -1L
            }
         }
         // Stil not iniitalized, forcing to 0
         if (contentLength == -1)
         {
            contentLength = 0L;
         }
      }
      else
      {
         contentLength = 0L;
      }

      return contentLength;
   }

   public static boolean hasStream(DrbNode drbNode)
   {
      if (drbNode instanceof DrbNodeSpi)
      {
         return ((DrbNodeSpi) drbNode).hasImpl(InputStream.class);
      }
      return false;
   }

   public static InputStream getStream(DrbNode drbNode)
   {
      if (drbNode instanceof DrbNodeSpi)
      {
         return (InputStream) ((DrbNodeSpi) drbNode).getImpl(InputStream.class);
      }
      return null;
   }

   private static Integer getChildrenCount(DrbNode drbNode)
   {
      return drbNode.getChildrenCount();
   }

   private static Object getValue(DrbNode drbNode)
   {
      Value val = drbNode.getValue();
      String s_value = null;
      if (val != null)
      {
         s_value = cleanInvalidXmlChars(val.toString(), "");
      }
      return s_value;
   }

   private static final Pattern RE = Pattern.compile("[^\\x09\\x0A\\x0D\\x20-\\xD7FF\\xE000-\\xFFFD\\x10000-x10FFFF]");
   private static String cleanInvalidXmlChars(String text, String replacement)
   {
      return RE.matcher(text).replaceAll(replacement);
   }
}
