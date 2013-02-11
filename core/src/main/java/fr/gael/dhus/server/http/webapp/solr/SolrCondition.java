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
package fr.gael.dhus.server.http.webapp.solr;

import fr.gael.dhus.search.SolrType;
import fr.gael.dhus.system.config.ConfigurationManager;

import org.apache.logging.log4j.LogManager;

import org.springframework.beans.BeansException;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Determines whether the SolrWebApp beans has to be added to the Spring context.
 */
public class SolrCondition implements Condition
{
   @Override
   public boolean matches(ConditionContext cc, AnnotatedTypeMetadata atm)
   {
      try
      {
         ConfigurationManager conf = (ConfigurationManager) cc.getBeanFactory().getBean("configurationManager");
         return conf.getSolrType() == SolrType.EMBED;
      }
      catch (BeansException ex)
      {
         LogManager.getLogger().error("Cannot validate the SolrCondition without the ConfigurationManager");
         // No need to throw, the conf bean must be created for the DHuS to run
      }
      return false;
   }
}
