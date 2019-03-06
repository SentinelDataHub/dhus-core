/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2014,2015,2017 GAEL Systems
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
package fr.gael.dhus.util;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class TestContextLoader extends AbstractContextLoader
{

   @Override
   protected String getResourceSuffix()
   {
      return "-test.xml";
   }

   @Override
   public ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception
   {
      return loadContext(mergedConfig.getLocations());
   }

   @Override
   public ApplicationContext loadContext(String... locations) throws Exception
   {
      XmlWebApplicationContext ctx = new XmlWebApplicationContext();
      ctx.setConfigLocations(locations);
      ctx.getEnvironment().setActiveProfiles("test");
      ctx.refresh();
      AnnotationConfigUtils.registerAnnotationConfigProcessors((BeanDefinitionRegistry) ctx.getBeanFactory());
      ctx.registerShutdownHook();
      return ctx;
   }

}
