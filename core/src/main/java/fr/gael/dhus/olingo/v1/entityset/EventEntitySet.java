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
import fr.gael.dhus.olingo.v1.entity.AbstractEntity;
import fr.gael.dhus.olingo.v1.entity.Event;
import fr.gael.dhus.olingo.v1.map.impl.EventMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind;
import org.apache.olingo.odata2.api.edm.provider.EntityType;
import org.apache.olingo.odata2.api.edm.provider.Facets;
import org.apache.olingo.odata2.api.edm.provider.Key;
import org.apache.olingo.odata2.api.edm.provider.Property;
import org.apache.olingo.odata2.api.edm.provider.PropertyRef;
import org.apache.olingo.odata2.api.edm.provider.SimpleProperty;
import org.apache.olingo.odata2.api.uri.KeyPredicate;

public class EventEntitySet extends AbstractEntitySet<Event>
{

   private static final String ENTITY_NAME = "Event";

   public static final String ID = "Id";
   public static final String CATEGORY = "Category";
   public static final String SUBCATEGORY = "Subcategory";
   public static final String TITLE = "Title";
   public static final String DESCRIPTION = "Description";
   public static final String START_DATE = "StartDate";
   public static final String STOP_DATE = "StopDate";
   public static final String PUBLICATION_DATE = "PublicationDate";
   public static final String ICON = "Icon";
   public static final String LOCAL_EVENT = "LocalEvent";
   public static final String PUBLIC_EVENT = "PublicEvent";
   public static final String ORIGINATOR = "Originator";
   public static final String HUB_TAG = "HubTag";
   public static final String MISSION_TAG = "MissionTag";
   public static final String INSTRUMENT_TAG = "InstrumentTag";
   public static final String EXTERNAL_URL = "ExternalUrl";

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

      Property category = new SimpleProperty()
            .setName(CATEGORY)
            .setType(EdmSimpleTypeKind.String)
            .setFacets(new Facets().setNullable(false));

      Property subCategory = new SimpleProperty()
            .setName(SUBCATEGORY)
            .setType(EdmSimpleTypeKind.String)
            .setFacets(new Facets().setMaxLength(128));

      Property title = new SimpleProperty()
            .setName(TITLE)
            .setType(EdmSimpleTypeKind.String)
            .setFacets(new Facets().setNullable(false).setMaxLength(255));

      Property description = new SimpleProperty()
            .setName(DESCRIPTION)
            .setType(EdmSimpleTypeKind.String)
            .setFacets(new Facets().setNullable(false).setMaxLength(1024));

      Property startDate = new SimpleProperty()
            .setName(START_DATE)
            .setType(EdmSimpleTypeKind.DateTime)
            .setFacets(new Facets().setNullable(false).setPrecision(3));

      Property stopDate = new SimpleProperty()
            .setName(STOP_DATE)
            .setType(EdmSimpleTypeKind.DateTime)
            .setFacets(new Facets().setPrecision(3));

      Property publicationDate = new SimpleProperty()
            .setName(PUBLICATION_DATE)
            .setType(EdmSimpleTypeKind.DateTime)
            .setFacets(new Facets().setPrecision(3));

      Property icon = new SimpleProperty()
            .setName(ICON)
            .setType(EdmSimpleTypeKind.String)
            .setFacets(new Facets().setMaxLength(1024));

      Property localEvent = new SimpleProperty()
            .setName(LOCAL_EVENT)
            .setType(EdmSimpleTypeKind.Boolean)
            .setFacets(new Facets().setNullable(false));

      Property publicEvent = new SimpleProperty()
            .setName(PUBLIC_EVENT)
            .setType(EdmSimpleTypeKind.Boolean)
            .setFacets(new Facets().setNullable(false));

      Property originator = new SimpleProperty()
            .setName(ORIGINATOR)
            .setType(EdmSimpleTypeKind.String)
            .setFacets(new Facets().setMaxLength(128));

      Property hubTag = new SimpleProperty()
            .setName(HUB_TAG)
            .setType(EdmSimpleTypeKind.String)
            .setFacets(new Facets().setMaxLength(255));

      Property missionTag = new SimpleProperty()
            .setName(MISSION_TAG)
            .setType(EdmSimpleTypeKind.String)
            .setFacets(new Facets().setMaxLength(255));

      Property instrumentTag = new SimpleProperty()
            .setName(INSTRUMENT_TAG)
            .setType(EdmSimpleTypeKind.String)
            .setFacets(new Facets().setMaxLength(255));

      Property externalUrl = new SimpleProperty()
            .setName(EXTERNAL_URL)
            .setType(EdmSimpleTypeKind.String)
            .setFacets(new Facets().setMaxLength(1024));

      Key key = new Key().setKeys(Collections.singletonList(new PropertyRef().setName(ID)));

      return new EntityType()
            .setName(ENTITY_NAME)
            .setProperties(
                  Arrays.asList(id, category, subCategory, title, description, startDate, stopDate,
                        publicationDate, icon, localEvent, publicEvent, originator, hubTag,
                        missionTag, instrumentTag, externalUrl)
            )
            .setKey(key);
   }

   @Override
   public Map getEntities()
   {
      return new EventMap();
   }

   @Override
   public AbstractEntity getEntity(KeyPredicate kp)
   {
      return (Event) getEntities().get(kp.getLiteral());
   }

   public boolean hasWritePermission(User user)
   {
      return user.getRoles().contains(Role.EVENT_MANAGER);
   }

}
