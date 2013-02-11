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
package org.dhus.api.transformation;

import java.util.List;
import java.util.Map;

/**
 * A transformer represents a transformation process and its parameters.
 */
public interface Transformer
{
   /**
    * Returns transformation name.
    *
    * @return transformation name
    */
   String getName();

   /**
    * Returns the transformation description.
    *
    * @return transformation description
    */
   String getDescription();

   /**
    * Returns all parameters and their description for this transformation.
    *
    * @return a list containing parameters
    */
   List<TransformationParameter> getParameters();


   /**
    * Submits a transformation process for a product.
    *
    * @param transformationUuid a non null transformation UUID
    * @param productInfo a non null product info about the product to transform
    * @param parameters map of parameters to configure the transformation process
    * @return a non null TransformationStatus about the submitted transformation process
    * @throws TransformationException could not submit a transformation process
    */
   TransformationStatus submitTransformation(String transformationUuid, ProductInfo productInfo, Map<String, String> parameters)
         throws TransformationException;

   /**
    * Returns the status of a Transformation.
    *
    * @param transformationUuid the UUID of a Transformation.
    * @param data custom data used by the transformer
    * @return the status of a Transformation
    * @throws TransformationException
    */
   TransformationStatus getTransformationStatus(String transformationUuid, String data) throws TransformationException;

   /**
    * Check if the given productInfo and parameters can be used by this Transformer.
    *
    * @param productInfo information about the product
    * @param parameters  transformation parameter map
    * @throws TransformationException This transformer does not accept the product and/or parameters (details in message)
    */
   void isTransformable(ProductInfo productInfo, Map<String, String> parameters) throws TransformationException;

   /**
    * Terminates a transformation process and frees any associated resource.
    *
    * @param transformationUuid a non null transformation UUID
    */
   void terminateTransformation(String transformationUuid);
}
