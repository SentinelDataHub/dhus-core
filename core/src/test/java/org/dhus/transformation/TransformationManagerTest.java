/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018 GAEL Systems
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

import fr.gael.dhus.database.object.config.system.TransformationConfiguration;
import fr.gael.dhus.service.TransformationService;

import java.util.List;

import org.dhus.api.transformation.TransformationException;
import org.dhus.api.transformation.TransformationParameter;
import org.dhus.api.transformation.Transformer;

import org.easymock.EasyMock;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TransformationManagerTest
{
   private TransformationManager getDefaultTransformationManager()
   {
      TransformationService mockTrfDao = EasyMock.createMock(TransformationService.class);
      EasyMock.replay();
      TransformationManager manager = new TransformationManager();
      manager.setTransformationService(mockTrfDao);
      manager.setTransformationConfiguration(new TransformationConfiguration());
      manager.init();
      manager.setTransformationService(null);
      return manager;
   }

   @Test
   public void testGetTransformations() throws TransformationException
   {
      TransformationManager manager = getDefaultTransformationManager();
      List<Transformer> list = manager.getTransformers();

      Assert.assertNotNull(list);
      Assert.assertFalse(list.isEmpty());
      Assert.assertEquals(list.size(), 2);
   }

   @Test
   public void testGetTransformation() throws TransformationException
   {
      TransformationManager manager = getDefaultTransformationManager();
      Transformer transformer = manager.getTransformer(FakeTransformer2.NAME);

      Assert.assertEquals(transformer.getName(), FakeTransformer2.NAME);
      Assert.assertEquals(transformer.getDescription(), FakeTransformer2.DESCRIPTION);
      List<TransformationParameter> parameters = transformer.getParameters();
      Assert.assertNotNull(parameters);
      Assert.assertFalse(parameters.isEmpty());
      Assert.assertEquals(parameters.size(), 1);
      TransformationParameter parameter = parameters.get(0);
      Assert.assertNotNull(parameter);
      Assert.assertEquals(parameter.getName(), FakeTransformer2.PARAM_FOO_NAME);
      Assert.assertEquals(parameter.getDescription(), FakeTransformer2.PARAM_FOO_DESCRIPTION);
   }
}
