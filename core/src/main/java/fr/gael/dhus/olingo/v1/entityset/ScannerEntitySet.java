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
package fr.gael.dhus.olingo.v1.entityset;

import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.olingo.v1.Expander;
import fr.gael.dhus.olingo.v1.Model;
import fr.gael.dhus.olingo.v1.entity.AbstractEntity;
import fr.gael.dhus.olingo.v1.entity.Scanner;
import fr.gael.dhus.olingo.v1.map.impl.ScannerMap;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.olingo.odata2.api.edm.EdmMultiplicity;
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind;
import org.apache.olingo.odata2.api.edm.FullQualifiedName;
import org.apache.olingo.odata2.api.edm.provider.Association;
import org.apache.olingo.odata2.api.edm.provider.AssociationEnd;
import org.apache.olingo.odata2.api.edm.provider.AssociationSet;
import org.apache.olingo.odata2.api.edm.provider.AssociationSetEnd;
import org.apache.olingo.odata2.api.edm.provider.EntityType;
import org.apache.olingo.odata2.api.edm.provider.Facets;
import org.apache.olingo.odata2.api.edm.provider.Key;
import org.apache.olingo.odata2.api.edm.provider.NavigationProperty;
import org.apache.olingo.odata2.api.edm.provider.Property;
import org.apache.olingo.odata2.api.edm.provider.PropertyRef;
import org.apache.olingo.odata2.api.edm.provider.SimpleProperty;
import org.apache.olingo.odata2.api.uri.KeyPredicate;

public class ScannerEntitySet extends AbstractEntitySet<Scanner>
{
   private final ConfigurationManager CONFIGURATION_MANAGER =
         ApplicationContextProvider.getBean(ConfigurationManager.class);

   public static final String ENTITY_NAME = "Scanner";

   // property names
   public static final String ID = "Id";
   public static final String URL = "Url";
   public static final String STATUS = "Status";
   public static final String STATUS_MESSAGE = "StatusMessage";
   public static final String ACTIVE = "Active";
   public static final String USERNAME = "Username";
   public static final String PASSWORD = "Password";
   public static final String PATTERN = "Pattern";

   public static final FullQualifiedName ASSO_SCANNER_COLLECTION =
         new FullQualifiedName(Model.NAMESPACE, "Scanner_Collection");
   public static final String ROLE_SCANNER_COLLECTIONS = "Scanner_Collections";
   public static final String ROLE_COLLECTION_SCANNERS = "Collection_Scanners";

   @Override
   public String getEntityName()
   {
      return ENTITY_NAME;
   }

   @Override
   public EntityType getEntityType()
   {

      Property id = new SimpleProperty()
            .setName(ID)
            .setType(EdmSimpleTypeKind.Int64)
            .setFacets(new Facets().setNullable(false));
      Property url = new SimpleProperty()
            .setName(URL)
            .setType(EdmSimpleTypeKind.String)
            .setFacets(new Facets().setNullable(false));
      Property status = new SimpleProperty()
            .setName(STATUS)
            .setType(EdmSimpleTypeKind.String)
            .setFacets(new Facets().setNullable(false));
      Property statusMessage = new SimpleProperty()
            .setName(STATUS_MESSAGE)
            .setType(EdmSimpleTypeKind.String)
            .setFacets(new Facets().setNullable(false));
      Property active = new SimpleProperty()
            .setName(ACTIVE)
            .setType(EdmSimpleTypeKind.Boolean)
            .setFacets(new Facets().setNullable(false).setDefaultValue("false"));

      Property username = new SimpleProperty()
            .setName(USERNAME)
            .setType(EdmSimpleTypeKind.String);
      Property password = new SimpleProperty()
            .setName(PASSWORD)
            .setType(EdmSimpleTypeKind.String);
      Property pattern = new SimpleProperty()
            .setName(PATTERN)
            .setType(EdmSimpleTypeKind.String);

      // navigation properties
      NavigationProperty scannerCollections = new NavigationProperty()
            .setName(Model.COLLECTION.getName())
            .setRelationship(ASSO_SCANNER_COLLECTION)
            .setFromRole(ROLE_COLLECTION_SCANNERS)
            .setToRole(ROLE_SCANNER_COLLECTIONS);

      Key key = new Key().setKeys(Collections.singletonList(
            new PropertyRef().setName(ID)));

      return new EntityType()
            .setName(ENTITY_NAME)
            .setProperties(Arrays.asList(id, url, status, statusMessage,
                  active, username, password, pattern))
            .setNavigationProperties(Arrays.asList(scannerCollections))
            .setKey(key);
   }

   @Override
   public List<AssociationSet> getAssociationSets()
   {
      AssociationSet scannerCollection = new AssociationSet()
            .setName(ASSO_SCANNER_COLLECTION.getName())
            .setAssociation(ASSO_SCANNER_COLLECTION)
            .setEnd1(new AssociationSetEnd()
                  .setRole(ROLE_SCANNER_COLLECTIONS)
                  .setEntitySet(Model.COLLECTION.getName()))
            .setEnd2(new AssociationSetEnd()
                  .setRole(ROLE_COLLECTION_SCANNERS)
                  .setEntitySet(Model.SCANNER.getName()));

      return Arrays.asList(scannerCollection);
   }

   @Override
   public List<Association> getAssociations()
   {
      Association scannerCollection = new Association()
            .setName(ASSO_SCANNER_COLLECTION.getName())
            .setEnd1(new AssociationEnd()
                  .setType(Model.COLLECTION.getFullQualifiedName())
                  .setRole(ROLE_SCANNER_COLLECTIONS)
                  .setMultiplicity(EdmMultiplicity.MANY))
            .setEnd2(new AssociationEnd()
                  .setType(Model.SCANNER.getFullQualifiedName())
                  .setRole(ROLE_COLLECTION_SCANNERS)
                  .setMultiplicity(EdmMultiplicity.MANY));

      return Arrays.asList(scannerCollection);
   }

   @Override
   public Map getEntities()
   {
      return new ScannerMap();
   }

   @Override
   public int count()
   {
      return CONFIGURATION_MANAGER.getScannerManager().count();
   }

   @Override
   public Scanner getEntity(KeyPredicate keyPredicate)
   {
      return new ScannerMap().get(Long.decode(keyPredicate.getLiteral()));
   }

   @Override
   public List<String> getExpandableNavLinkNames()
   {
      return Arrays.asList("Collections");
   }

   @Override
   public List<Map<String, Object>> expand(String navlinkName, String selfUrl,
         Map<?, AbstractEntity> entities, Map<String, Object> key)
   {
      return Expander.expandFeedSingletonKey(navlinkName, selfUrl, entities, key, ID);
   }

   @Override
   public boolean isAuthorized(User user)
   {
      return user.getRoles().contains(Role.DATA_MANAGER);
   }
}
