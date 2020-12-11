/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018-2020 GAEL Systems
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
package org.dhus.transformation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.dhus.api.transformation.ProductInfo;
import org.dhus.api.transformation.TransformationException;
import org.dhus.api.transformation.TransformationParameter;
import org.dhus.api.transformation.TransformationStatus;
import org.dhus.api.transformation.Transformer;

public class FakeTransformer1 implements Transformer
{
   static final String NAME = "TransformationOne";
   static final String DESCRIPTION = "DescriptionOne";

   static final String PARAM_FOO_NAME = "foo";
   static final String PARAM_FOO_DESCRIPTION = "foo_description";

   static final String PARAM_BAR_NAME = "bar";
   static final String PARAM_BAR_DESCRIPTION = "bar_description";

   @Override
   public String getName()
   {
      return NAME;
   }

   @Override
   public String getDescription()
   {
      return DESCRIPTION;
   }

   @Override
   public List<TransformationParameter> getParameters()
   {
      return Arrays.asList(
            new TransformationParameter(PARAM_FOO_NAME, PARAM_FOO_DESCRIPTION),
            new TransformationParameter(PARAM_BAR_NAME, PARAM_BAR_DESCRIPTION)
      );
   }

   @Override
   public TransformationStatus submitTransformation(String transformationUuid,
         ProductInfo productInfo, Map<String, String> parameters)
         throws TransformationException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public TransformationStatus getTransformationStatus(String transformationUuid, String data)
         throws TransformationException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public void terminateTransformation(String transformationUuid)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public void isTransformable(ProductInfo productInfo, Map<String, String> parameters) throws TransformationException {}
}
