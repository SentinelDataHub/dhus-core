/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018,2019 GAEL Systems
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
package org.dhus.olingo.v2.entity;

import fr.gael.odata.engine.data.DataHandlerUtil;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Operation;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;

import org.dhus.api.olingo.v2.EntityProducer;
import org.dhus.api.olingo.v2.TypeInfo;
import org.dhus.api.transformation.TransformationParameter;
import org.dhus.api.transformation.Transformer;

import org.dhus.olingo.v2.datamodel.TransformerModel;
import org.dhus.olingo.v2.datamodel.action.RunTransformerAction;
import org.dhus.olingo.v2.datamodel.complex.DescriptiveParameterComplexType;

@TypeInfo(type = Transformer.class)
public class TransformerEntityProducer implements EntityProducer<Transformer>
{
   @Override
   public Entity transform(Transformer transformer)
   {
      if (transformer != null)
      {
         Entity entity = new Entity();
         entity.setId(DataHandlerUtil.createEntityId(TransformerModel.ENTITY_SET_NAME, transformer.getName()));
         entity.setType(TransformerModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());

         Property name = new Property();
         name.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName().getFullQualifiedNameAsString());
         name.setName(TransformerModel.PROPERTY_NAME);
         name.setValue(ValueType.PRIMITIVE, transformer.getName());

         Property description = new Property();
         description.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName().getFullQualifiedNameAsString());
         description.setName(TransformerModel.PROPERTY_DESCRIPTION);
         description.setValue(ValueType.PRIMITIVE, transformer.getDescription());

         List<TransformationParameter> parameterList = transformer.getParameters();
         List<ComplexValue> complexList = new ArrayList<>(parameterList.size());
         Property parameters = new Property();
         parameterList.forEach(parameter ->
         {
            Property paramName = new Property();
            paramName.setName(DescriptiveParameterComplexType.PROPERTY_NAME);
            paramName.setValue(ValueType.PRIMITIVE, parameter.getName());

            Property paramDescription = new Property();
            paramDescription.setName(DescriptiveParameterComplexType.PROPERTY_DESCRIPTION);
            paramDescription.setValue(ValueType.PRIMITIVE, parameter.getDescription());

            ComplexValue complex = new ComplexValue();
            complex.setTypeName(DescriptiveParameterComplexType.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());
            complex.getValue().add(paramName);
            complex.getValue().add(paramDescription);
            complexList.add(complex);
         });
         parameters.setName(TransformerModel.PROPERTY_PARAMETERS);
         parameters.setValue(ValueType.COLLECTION_COMPLEX, complexList);

         entity.addProperty(name);
         entity.addProperty(description);
         entity.addProperty(parameters);

         Operation transformAction = new Operation();
         transformAction.setType(Operation.Type.ACTION);
         transformAction.setTarget(URI.create(entity.getId().toString() + "/" + RunTransformerAction.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()));
         transformAction.setMetadataAnchor("#" + RunTransformerAction.ACTION_NAME);
         transformAction.setTitle(RunTransformerAction.ACTION_NAME);
         entity.getOperations().add(transformAction);

         return entity;
      }
      return null;
   }
}
