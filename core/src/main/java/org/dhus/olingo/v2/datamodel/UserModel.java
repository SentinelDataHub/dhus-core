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
package org.dhus.olingo.v2.datamodel;

import fr.gael.odata.engine.model.EntityModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.dhus.olingo.v2.datamodel.complex.RestrictionComplexType;

import org.dhus.olingo.v2.datamodel.enumeration.SystemRoleEnum;
import org.dhus.olingo.v2.web.DHuSODataServlet;

public class UserModel implements EntityModel
{
   public static final String ENTITY_TYPE_NAME = "User";
   public static final String ENTITY_SET_NAME = "Users";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);

   public static final String PROPERTY_USERNAME = "Username";
   public static final String PROPERTY_EMAIL = "Email";
   public static final String PROPERTY_FIRSTNAME = "FirstName";
   public static final String PROPERTY_LASTNAME = "LastName";
   public static final String PROPERTY_COUNTRY = "Country";
   public static final String PROPERTY_PHONE = "Phone";
   public static final String PROPERTY_ADDRESS = "Address";
   public static final String PROPERTY_DOMAIN = "Domain";
   public static final String PROPERTY_SUBDOMAIN = "SubDomain";
   public static final String PROPERTY_USAGE = "Usage";
   public static final String PROPERTY_SUBUSAGE = "SubUsage";
   public static final String PROPERTY_HASH = "Hash";
   public static final String PROPERTY_PASSWORD = "Password";
   public static final String PROPERTY_CREATED = "Created";
   public static final String PROPERTY_ROLES = "SystemRoles";
   public static final String PROPERTY_RESTRICTIONS = "Restrictions";
   public static final String NAVIGATION_CART = "Cart";
   public static final String NAVIGATION_SEARCHES = "Searches";

   @Override
   public CsdlEntityType getEntityType()
   {
      List<CsdlProperty> properties = new ArrayList<>();

      CsdlPropertyRef nameKey = new CsdlPropertyRef().setName(PROPERTY_USERNAME);

      CsdlProperty username = new CsdlProperty()
            .setName(PROPERTY_USERNAME)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false);
      properties.add(username);

      CsdlProperty email = new CsdlProperty()
            .setName(PROPERTY_EMAIL)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);
      properties.add(email);

      CsdlProperty firstName = new CsdlProperty()
            .setName(PROPERTY_FIRSTNAME)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);
      properties.add(firstName);

      CsdlProperty lastName = new CsdlProperty()
            .setName(PROPERTY_LASTNAME)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);
      properties.add(lastName);

      CsdlProperty country = new CsdlProperty()
            .setName(PROPERTY_COUNTRY)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);
      properties.add(country);

      CsdlProperty phone = new CsdlProperty()
            .setName(PROPERTY_PHONE)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);
      properties.add(phone);

      CsdlProperty adresse = new CsdlProperty()
            .setName(PROPERTY_ADDRESS)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);
      properties.add(adresse);

      CsdlProperty domain = new CsdlProperty()
            .setName(PROPERTY_DOMAIN)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);
      properties.add(domain);

      CsdlProperty subDomain = new CsdlProperty()
            .setName(PROPERTY_SUBDOMAIN)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);
      properties.add(subDomain);

      CsdlProperty usage = new CsdlProperty()
            .setName(PROPERTY_USAGE)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);
      properties.add(usage);

      CsdlProperty subUsage = new CsdlProperty()
            .setName(PROPERTY_SUBUSAGE)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);
      properties.add(subUsage);

      CsdlProperty hash = new CsdlProperty()
            .setName(PROPERTY_HASH)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);
      properties.add(hash);

      CsdlProperty password = new CsdlProperty()
            .setName(PROPERTY_PASSWORD)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(true);
      properties.add(password);

      CsdlProperty created = new CsdlProperty()
            .setName(PROPERTY_CREATED)
            .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
            .setNullable(true);
      properties.add(created);

      CsdlProperty roles = new CsdlProperty()
            .setName(PROPERTY_ROLES)
            .setType(SystemRoleEnum.FULL_QUALIFIED_NAME)
            .setNullable(true)
            .setCollection(true);
      properties.add(roles);

      CsdlProperty restrictions = new CsdlProperty()
            .setName(PROPERTY_RESTRICTIONS)
            .setType(RestrictionComplexType.FULL_QUALIFIED_NAME)
            .setNullable(true)
            .setCollection(true);
      properties.add(restrictions);
      
      // Define navigation properties
      CsdlNavigationProperty cartNavigationProperty = new CsdlNavigationProperty()
            .setName(NAVIGATION_CART)
            .setType(ProductModel.FULL_QUALIFIED_NAME)
            .setCollection(true);
      
      CsdlNavigationProperty searchesNavigationProperty = new CsdlNavigationProperty()
            .setName(NAVIGATION_SEARCHES)
            .setType(SearchModel.FULL_QUALIFIED_NAME)
            .setCollection(true);

      return new CsdlEntityType().setName(ENTITY_TYPE_NAME)
            .setProperties(properties)
            .setKey(Collections.singletonList(nameKey))
            .setNavigationProperties(Arrays.asList(cartNavigationProperty, searchesNavigationProperty));
   }

   @Override
   public CsdlEntitySet getEntitySet()
   {
      CsdlNavigationPropertyBinding cart = new CsdlNavigationPropertyBinding()
            .setPath(NAVIGATION_CART)
            .setTarget(ProductModel.ENTITY_SET_NAME);
      CsdlNavigationPropertyBinding search = new CsdlNavigationPropertyBinding()
            .setPath(NAVIGATION_SEARCHES)
            .setTarget(SearchModel.ENTITY_SET_NAME);

      return EntityModel.super.getEntitySet().setNavigationPropertyBindings(Arrays.asList(cart, search));
   }

   @Override
   public String getEntitySetName()
   {
      return ENTITY_SET_NAME;
   }

   @Override
   public String getName()
   {
      return ENTITY_TYPE_NAME;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return FULL_QUALIFIED_NAME;
   }
}
