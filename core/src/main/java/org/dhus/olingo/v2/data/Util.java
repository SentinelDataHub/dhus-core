/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019 GAEL Systems
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.server.api.uri.UriParameter;

public class Util
{
   /**
    * Adapts the given parameter to a map.
    *
    * @param parameter parameter to adapt
    * @return a non null object map
    */
   public static Map<String, Object> parametersToMap(Parameter parameter)
   {
      if (parameter == null || parameter.isNull())
      {
         return Collections.emptyMap();
      }

      ComplexValue complex = (ComplexValue) parameter.getValue();
      return complex.getValue()
            .stream()
            .collect(Collectors.toMap(Property::getName, Property::getValue));
   }

   /**
    * Creates a map from a list of UriParameter whose key is the value returned by getName and the
    * value is returned by getText.
    *
    * @param parameters a list of UriParameters, e.g.: a compounded key predicate
    * @return a non null, possibly empty, instance of Map
    */
   public static Map<String, String> uriParametersToMap(List<UriParameter> parameters)
   {
      if (parameters == null || parameters.isEmpty())
      {
         return Collections.emptyMap();
      }

      return parameters.stream().collect(Collectors.toMap(UriParameter::getName, UriParameter::getText));
   }
}
