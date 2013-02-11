package org.dhus.olingo.v2.datamodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.dhus.olingo.v2.datamodel.complex.AdvancedPropertyComplexType;
import org.dhus.olingo.v2.web.DHuSODataServlet;

import fr.gael.odata.engine.model.EntityModel;

public class SearchModel implements EntityModel
{
   public static final String ENTITY_TYPE_NAME = "Search";
   public static final String ENTITY_SET_NAME = "Searches";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ENTITY_TYPE_NAME);
   
   public static final String PROPERTY_ID = "Id";
   public static final String PROPERTY_VALUE = "Value";
   public static final String PROPERTY_FOOTPRINT = "Footprint";
   public static final String PROPERTY_ADVANCED = "Advanced";
   public static final String PROPERTY_COMPLETE = "Complete";
   public static final String PROPERTY_NOTIFY = "Notify";
   

   @Override
   public CsdlEntityType getEntityType()
   {
      CsdlPropertyRef key = new CsdlPropertyRef().setName(PROPERTY_ID);
      ArrayList<CsdlProperty> properties = new ArrayList<>();
      
      properties.add(new CsdlProperty()
            .setName(PROPERTY_ID)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false));
      
      properties.add(
            new CsdlProperty()
            .setName(PROPERTY_VALUE)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setNullable(false));
      
      properties.add(
            new CsdlProperty()
            .setName(PROPERTY_FOOTPRINT)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()));
      
      properties.add(
            new CsdlProperty()
            .setName(PROPERTY_COMPLETE)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()));
      
      properties.add(
            new CsdlProperty()
            .setName(PROPERTY_NOTIFY)
            .setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName()));
      
      properties.add(new CsdlProperty()
            .setName(PROPERTY_ADVANCED)
            .setType(AdvancedPropertyComplexType.FULL_QUALIFIED_NAME)
            .setCollection(true));
      
      return new CsdlEntityType().setName(ENTITY_TYPE_NAME)
            .setProperties(properties)
            .setKey(Collections.singletonList(key));
   }
   
   @Override
   public CsdlEntitySet getEntitySet()
   {
      CsdlEntitySet entitySet = EntityModel.super.getEntitySet();
      return entitySet.setIncludeInServiceDocument(true);
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
