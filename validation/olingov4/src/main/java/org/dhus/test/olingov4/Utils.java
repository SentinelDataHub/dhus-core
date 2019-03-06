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

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientProperty;

/**
 *
 */
public class Utils
{
   
   public static boolean compareEntities(ClientEntity reference, ClientEntity entity)
   {
      return compareEntitiesExcept(reference, entity, Collections.<String>emptySet());
   }
   
   public static boolean compareEntitiesExcept(ClientEntity reference, ClientEntity entity,
         Set<String> excludedProperties)
   {
      // check entity types
      if(!Objects.equals(reference.getTypeName(), entity.getTypeName()))
      {
         return false;
      }
      
      // check each property value
      for(ClientProperty referenceProperty : reference.getProperties())
      {
         // skip excluded properties
         // non-primitive values not supported
         if(!excludedProperties.contains(referenceProperty.getName()) 
               && referenceProperty.getValue().isPrimitive())
         {
            // compare property values
            ClientProperty property = entity.getProperty(referenceProperty.getName());
            if(!Objects.equals(
                  referenceProperty.getPrimitiveValue().toValue(), 
                  property.getPrimitiveValue().toValue()))
            {
               TestManagerV4.logScenarioError(String.format("Invalid property value, expected [{}] ({}), but found [{}] ({})", 
                     referenceProperty.getPrimitiveValue().toValue(), referenceProperty.getPrimitiveValue().getTypeName(),
                     property.getPrimitiveValue().toValue(), property.getPrimitiveValue().getTypeName()));
               
               return false;
            }
         }
         
         // TODO complex property support
      }
      return true;
   }
}
