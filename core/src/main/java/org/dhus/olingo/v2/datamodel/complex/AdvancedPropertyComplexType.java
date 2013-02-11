package org.dhus.olingo.v2.datamodel.complex;

import java.util.Arrays;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.dhus.olingo.v2.web.DHuSODataServlet;

import fr.gael.odata.engine.model.ComplexModel;

public class AdvancedPropertyComplexType implements ComplexModel
{
   public static final String COMPLEX_TYPE_NAME = "Advanced";
   public static final String COMPLEX_SET_NAME = "Advanceds";
   public static final FullQualifiedName FULL_QUALIFIED_NAME =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, COMPLEX_TYPE_NAME);
   public static final String PROPERTY_NAME = "Name";
   public static final String PROPERTY_VALUE = "Value";

   @Override
   public String getName()
   {
      return COMPLEX_TYPE_NAME;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return FULL_QUALIFIED_NAME;
   }

   @Override
   public CsdlComplexType getComplexType()
   {
      CsdlProperty name = new CsdlProperty()
            .setName(PROPERTY_NAME)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setPrecision(6)
            .setNullable(false);

      CsdlProperty value = new CsdlProperty()
            .setName(PROPERTY_VALUE)
            .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
            .setPrecision(6)
            .setNullable(false);

      CsdlComplexType advancedProperty = new CsdlComplexType()
            .setName(COMPLEX_TYPE_NAME)
            .setProperties(Arrays.asList(name, value));

      return advancedProperty;
   }

}
