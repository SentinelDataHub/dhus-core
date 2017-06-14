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

import fr.gael.dhus.olingo.v1.Model;
import fr.gael.dhus.olingo.v1.entity.AbstractEntity;
import fr.gael.dhus.olingo.v1.entity.Product;
import fr.gael.dhus.olingo.v1.map.impl.DeletedProductsMap;
import fr.gael.dhus.service.DeletedProductService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.olingo.odata2.api.edm.EdmMultiplicity;
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind;
import org.apache.olingo.odata2.api.edm.EdmTargetPath;
import org.apache.olingo.odata2.api.edm.FullQualifiedName;
import org.apache.olingo.odata2.api.edm.provider.Association;
import org.apache.olingo.odata2.api.edm.provider.AssociationEnd;
import org.apache.olingo.odata2.api.edm.provider.AssociationSet;
import org.apache.olingo.odata2.api.edm.provider.AssociationSetEnd;
import org.apache.olingo.odata2.api.edm.provider.ComplexProperty;
import org.apache.olingo.odata2.api.edm.provider.CustomizableFeedMappings;
import org.apache.olingo.odata2.api.edm.provider.EntityType;
import org.apache.olingo.odata2.api.edm.provider.Facets;
import org.apache.olingo.odata2.api.edm.provider.Key;
import org.apache.olingo.odata2.api.edm.provider.NavigationProperty;
import org.apache.olingo.odata2.api.edm.provider.Property;
import org.apache.olingo.odata2.api.edm.provider.PropertyRef;
import org.apache.olingo.odata2.api.edm.provider.SimpleProperty;
import org.apache.olingo.odata2.api.uri.KeyPredicate;

public class DeletedProductEntitySet extends AbstractEntitySet<Product>
{
   public static final String ENTITY_NAME = "DeletedProduct";

   // Entity keys
   public static final String ID = "Id";
   public static final String NAME = "Name";
   public static final String CREATION_DATE = "CreationDate";
   public static final String FOOTPRINT = "FootPrint";
   public static final String SIZE = "Size";
   public static final String INGESTION_DATE = "IngestionDate";
   public static final String CONTENT_DATE = "ContentDate";
   public static final String DELETION_DATE = "DeletionDate";
   public static final String DELETION_CAUSE = "DeletionCause";
   public static final String CHECKSUM = "Checksum";

   public static final FullQualifiedName ASSO_DELPRODUCT_CLASS =
         new FullQualifiedName(Model.NAMESPACE, "DeletedProduct_Class");
   public static final String ROLE_DELPRODUCT_CLASS = "DeletedProduct_Class";
   public static final String ROLE_CLASS_DELPRODUCTS = "Class_DeletedProducts";

   private static final DeletedProductService DELETED_PRODUCT_SERVICE =
         ApplicationContextProvider.getBean(DeletedProductService.class);

   @Override
   public String getEntityName()
   {
      return ENTITY_NAME;
   }

   @Override
   public EntityType getEntityType()
   {
      // Properties
      List<Property> properties = new ArrayList<>();

      properties.add(new SimpleProperty().setName(ID)
            .setType(EdmSimpleTypeKind.String)
            .setFacets(new Facets().setNullable(false)));

      properties.add(new SimpleProperty()
            .setName(NAME)
            .setType(EdmSimpleTypeKind.String)
            .setCustomizableFeedMappings(
                  new CustomizableFeedMappings()
                        .setFcTargetPath(EdmTargetPath.SYNDICATION_TITLE)));

      properties.add(new SimpleProperty().setName(CREATION_DATE)
            .setType(EdmSimpleTypeKind.DateTime)
            .setFacets(new Facets().setNullable(false)));

      properties.add(new SimpleProperty().setName(FOOTPRINT).setType(EdmSimpleTypeKind.String));

      properties.add(new SimpleProperty().setName(SIZE).setType(EdmSimpleTypeKind.Int64));

      properties.add(new SimpleProperty()
            .setName(INGESTION_DATE)
            .setType(EdmSimpleTypeKind.DateTime));
      properties.add(new ComplexProperty().setName(CONTENT_DATE).setType(Model.TIME_RANGE));

      properties.add(new SimpleProperty().setName(DELETION_DATE).setType(EdmSimpleTypeKind.DateTime)
            .setCustomizableFeedMappings(
                  new CustomizableFeedMappings()
                        .setFcTargetPath(EdmTargetPath.SYNDICATION_UPDATED)));
      properties.add(new SimpleProperty().setName(DELETION_CAUSE).setType(EdmSimpleTypeKind.String));

      properties.add(new ComplexProperty().setName(CHECKSUM).setType(Model.CHECKSUM));

      // Navigation Properties
      List<NavigationProperty> navigationProperties = new ArrayList<>();

      navigationProperties.add(new NavigationProperty().setName("Class")
            .setRelationship(ASSO_DELPRODUCT_CLASS)
            .setFromRole(ROLE_CLASS_DELPRODUCTS).setToRole(ROLE_DELPRODUCT_CLASS));

      // Key
      Key key = new Key().setKeys(Collections.singletonList(new PropertyRef().setName(ID)));

      // TODO (OData v3) setOpenType(true) setAbstract(true)
      return new EntityType().setName(ENTITY_NAME).setProperties(properties)
            .setKey(key).setNavigationProperties(navigationProperties);
   }

   @Override
   public List<AssociationSet> getAssociationSets()
   {
      List<AssociationSet> associationSets = new ArrayList<>();

      associationSets.add(new AssociationSet()
            .setName(ASSO_DELPRODUCT_CLASS.getName())
            .setAssociation(ASSO_DELPRODUCT_CLASS)
            .setEnd1(
                  new AssociationSetEnd().setRole(ROLE_DELPRODUCT_CLASS)
                        .setEntitySet(Model.CLASS.getName()))
            .setEnd2(
                  new AssociationSetEnd().setRole(ROLE_CLASS_DELPRODUCTS)
                        .setEntitySet(getName())));
      return associationSets;
   }

   @Override
   public List<Association> getAssociations()
   {
      List<Association> associations = new ArrayList<>();

      associations.add(new Association()
            .setName(ASSO_DELPRODUCT_CLASS.getName())
            .setEnd1(
                  new AssociationEnd()
                        .setType(Model.CLASS.getFullQualifiedName())
                        .setRole(ROLE_DELPRODUCT_CLASS)
                        .setMultiplicity(EdmMultiplicity.ONE))
            .setEnd2(
                  new AssociationEnd().setType(getFullQualifiedName())
                        .setRole(ROLE_CLASS_DELPRODUCTS)
                        .setMultiplicity(EdmMultiplicity.MANY)));

      return associations;
   }

   @Override
   public List<String> getExpandableNavLinkNames()
   {
      List<String> res = new ArrayList<>(super.getExpandableNavLinkNames());
      res.add("Class");
      return res;
   }

   @Override
   public int count()
   {
      return DELETED_PRODUCT_SERVICE.count();
   }

   @Override
   public Map getEntities()
   {
      return new DeletedProductsMap();
   }

   @Override
   public AbstractEntity getEntity(KeyPredicate kp)
   {
      return (new DeletedProductsMap()).get(kp.getLiteral());
   }
}
