/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015 GAEL Systems
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.olingo.odata2.api.edm.EdmMultiplicity;
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind;
import org.apache.olingo.odata2.api.edm.EdmTargetPath;
import org.apache.olingo.odata2.api.edm.FullQualifiedName;
import org.apache.olingo.odata2.api.edm.provider.Association;
import org.apache.olingo.odata2.api.edm.provider.AssociationEnd;
import org.apache.olingo.odata2.api.edm.provider.AssociationSet;
import org.apache.olingo.odata2.api.edm.provider.AssociationSetEnd;
import org.apache.olingo.odata2.api.edm.provider.CustomizableFeedMappings;
import org.apache.olingo.odata2.api.edm.provider.EntityType;
import org.apache.olingo.odata2.api.edm.provider.Facets;
import org.apache.olingo.odata2.api.edm.provider.Key;
import org.apache.olingo.odata2.api.edm.provider.NavigationProperty;
import org.apache.olingo.odata2.api.edm.provider.Property;
import org.apache.olingo.odata2.api.edm.provider.PropertyRef;
import org.apache.olingo.odata2.api.edm.provider.SimpleProperty;

import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.olingo.Security;
import fr.gael.dhus.olingo.v1.Model;
import fr.gael.dhus.olingo.v1.entity.Network;
import fr.gael.dhus.olingo.v1.entity.AbstractEntity;
import fr.gael.dhus.olingo.v1.map.impl.NetworkMap;
import fr.gael.dhus.server.http.valve.AccessValve;
import java.util.Map;
import org.apache.olingo.odata2.api.uri.KeyPredicate;

public class NetworkEntitySet extends AbstractEntitySet<Network>
{
   public static final String ENTITY_NAME = "Network";

   // Entity keys
   public static final String ID = "Id";

   public static final FullQualifiedName ASSO_NETWORK_NETWORKSTATISTIC =
      new FullQualifiedName(Model.NAMESPACE, "Network_NetworkStatistic");
   public static final String ROLE_NETWORK_NETWORKSTATISTIC =
      "Network_NetworkStatistic";
   public static final String ROLE_NETWORKSTATISTIC_NETWORK =
      "NetworkStatistic_Network";

   @Override
   public String getEntityName ()
   {
      return ENTITY_NAME;
   }

   @Override
   public EntityType getEntityType ()
   {
      // Properties
      List<Property> properties = new ArrayList<Property> ();

      properties.add (new SimpleProperty ()
      .setName (ID)
      .setType (EdmSimpleTypeKind.Int64)
      .setFacets (new Facets ().setNullable (false))
      .setCustomizableFeedMappings (
         new CustomizableFeedMappings ()
            .setFcTargetPath (EdmTargetPath.SYNDICATION_TITLE)));

      // Key
      Key key =
         new Key ().setKeys (Collections.singletonList (new PropertyRef ()
            .setName (ID)));

      // Navigation Properties
      List<NavigationProperty> navigationProperties =
         new ArrayList<NavigationProperty> ();

      if (Security.currentUserHasRole(Role.STATISTICS))
      {
         navigationProperties.add (new NavigationProperty ()
            .setName ("NetworkStatistic")
            .setRelationship (ASSO_NETWORK_NETWORKSTATISTIC)
            .setFromRole (ROLE_NETWORKSTATISTIC_NETWORK)
            .setToRole (ROLE_NETWORK_NETWORKSTATISTIC));
      }

      return new EntityType ().setName (ENTITY_NAME).setProperties (properties)
         .setKey (key).setNavigationProperties (navigationProperties);
   }

   @Override
   public List<AssociationSet> getAssociationSets ()
   {
      List<AssociationSet> associationSets = new ArrayList<AssociationSet> ();

      if (Security.currentUserHasRole(Role.STATISTICS))
      {
         associationSets.add (new AssociationSet ()
            .setName (ASSO_NETWORK_NETWORKSTATISTIC.getName ())
            .setAssociation (ASSO_NETWORK_NETWORKSTATISTIC)
            .setEnd1 (
               new AssociationSetEnd ().setRole (ROLE_NETWORK_NETWORKSTATISTIC)
                  .setEntitySet(Model.NETWORKSTATISTIC.getName()))
            .setEnd2 (
               new AssociationSetEnd ().setRole (ROLE_NETWORKSTATISTIC_NETWORK)
                  .setEntitySet (getName ())));
      }
      return associationSets;
   }

   @Override
   public List<Association> getAssociations ()
   {
      List<Association> associations = new ArrayList<Association> ();

      if (Security.currentUserHasRole(Role.STATISTICS))
      {
      associations.add (new Association ()
         .setName (ASSO_NETWORK_NETWORKSTATISTIC.getName ())
         .setEnd1 (
            new AssociationEnd ()
               .setType(Model.NETWORKSTATISTIC.getFullQualifiedName())
               .setRole (ROLE_NETWORK_NETWORKSTATISTIC)
               .setMultiplicity (EdmMultiplicity.ONE))
         .setEnd2 (
            new AssociationEnd ().setType (getFullQualifiedName ())
               .setRole (ROLE_NETWORKSTATISTIC_NETWORK)
               .setMultiplicity (EdmMultiplicity.MANY)));
      }
      return associations;
   }

   @Override
   public int count ()
   {
      return AccessValve.getAccessInformationMap ().size ();
   }

   @Override
   public boolean isAuthorized (User user)
   {
      return user.getRoles ().contains (Role.SYSTEM_MANAGER) ||
             user.getRoles ().contains (Role.STATISTICS);
   }

   @Override
   public Map<Integer, Network> getEntities()
   {
      return new NetworkMap();
   }

   @Override
   public AbstractEntity getEntity(KeyPredicate kp)
   {
      Integer key = Integer.parseInt(kp.getLiteral());
      return (new NetworkMap()).get(key);
   }

   @Override
   public boolean hasManyEntries()
   {
      return false;
   }
}