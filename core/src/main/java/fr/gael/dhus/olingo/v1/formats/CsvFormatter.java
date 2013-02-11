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
package fr.gael.dhus.olingo.v1.formats;

import fr.gael.dhus.olingo.v1.entity.AbstractEntity;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.olingo.odata2.api.edm.EdmComplexType;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.edm.EdmTypeKind;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.uri.SelectItem;

public class CsvFormatter
{
   private static final Charset CHARSET = Charset.forName("UTF-8");
   public static final String CONTENT_TYPE = "text/csv";

   private static final String SEPARATOR = ",";
   private static final String NEWLINE = "\r\n";

   public static ODataResponse writeFeed(EdmEntitySet entity_set,
         Collection<AbstractEntity> entities, List<SelectItem> selection) throws ODataException
   {
      EdmEntityType entity_type = entity_set.getEntityType();
      StringBuilder response = new StringBuilder();

      if (selection.isEmpty())
      {
         response.append(makeHeader(entity_type));
         for (AbstractEntity entity: entities)
         {
            response.append(makeLine(entity_type, entity));
         }
      }
      // use properties from select instead of from the entity type
      else
      {
         response.append(makeSelectiveHeader(selection));
         for (AbstractEntity entity: entities)
         {
            response.append(makeSelectiveLine(selection, entity));
         }
      }

      // build and return ODataResponse for the Processor
      byte[] bytes = response.toString().getBytes(CHARSET);

      return ODataResponse
            .fromResponse(EntityProvider.writeBinary(CONTENT_TYPE, bytes))
            .header("Content-Type", CONTENT_TYPE)
            .build();
   }

   private static String makeLine(EdmEntityType entity_type, AbstractEntity entity)
         throws ODataException
   {
      StringBuilder line = new StringBuilder();
      for (String property_name: entity_type.getPropertyNames())
      {
         EdmProperty property = (EdmProperty) entity_type.getProperty(property_name);
         line.append(formatProperty(entity, property));
      }
      line = endLine(line);
      return line.toString();
   }

   private static String makeHeader(EdmEntityType entity_type) throws EdmException
   {
      StringBuilder header = new StringBuilder();

      // create header from property names
      for (String propertyName: entity_type.getPropertyNames())
      {
         EdmProperty property = (EdmProperty) entity_type.getProperty(propertyName);
         header.append(formatPropertyHeader(property));
      }

      header = endLine(header);
      return header.toString();
   }

   private static String makeSelectiveLine(List<SelectItem> selection, AbstractEntity entity)
         throws ODataException
   {
      StringBuilder line = new StringBuilder();
      for (SelectItem select_item: selection)
      {
         EdmProperty property = select_item.getProperty();
         line.append(formatProperty(entity, property));
      }
      line = endLine(line);
      return line.toString();
   }

   private static String formatProperty(AbstractEntity entity, EdmProperty property)
         throws EdmException, ODataException
   {
      StringBuilder formatted_property = new StringBuilder();
      if (property.getType().getKind().equals(EdmTypeKind.COMPLEX))
      {
         EdmComplexType property_type = (EdmComplexType) property.getType();
         Map<String, Object> complex_property = entity.getComplexProperty(property.getName());
         for (String complex_property_name: property_type.getPropertyNames())
         {
            String formatted_value = formatValue(complex_property.get(complex_property_name));
            formatted_property.append(formatted_value).append(SEPARATOR);
         }
      }
      else
      {
         String formatted_value = formatValue(entity.getProperty(property.getName()));
         formatted_property.append(formatted_value).append(SEPARATOR);
      }
      return formatted_property.toString();
   }

   private static String makeSelectiveHeader(List<SelectItem> selection) throws EdmException
   {
      StringBuilder header = new StringBuilder();
      // create header from property names
      for (SelectItem select_item: selection)
      {

         EdmProperty property = select_item.getProperty();
         header.append(formatPropertyHeader(property));
      }

      header = endLine(header);
      return header.toString();
   }

   private static String formatPropertyHeader(EdmProperty property) throws EdmException
   {
      StringBuilder property_header = new StringBuilder();
      if (property.getType().getKind().equals(EdmTypeKind.COMPLEX))
      {
         EdmComplexType property_type = (EdmComplexType) property.getType();
         for (String complex_property_name: property_type.getPropertyNames())
         {
            property_header.append(property.getName())
                  .append(":")
                  .append(complex_property_name)
                  .append(SEPARATOR);
         }
      }
      else
      {
         property_header.append(property.getName()).append(SEPARATOR);
      }
      return property_header.toString();
   }

   private static String formatValue(Object value)
   {
      if (value instanceof Date)
      {
         SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
         return format.format((Date) value);
      }

      String result = Objects.toString(value);
      if (result.contains(SEPARATOR) || result.contains(NEWLINE)
            || result.contains("\n") || result.contains("\r")
            || result.contains("\""))
      {

         result = '"' + result.replaceAll("\"", "\"\"") + '"';
      }
      return result;
   }

   private static StringBuilder endLine(StringBuilder line)
   {
      line.replace(line.lastIndexOf(SEPARATOR), line.length(), "");
      line.append(NEWLINE);
      return line;
   }
}
