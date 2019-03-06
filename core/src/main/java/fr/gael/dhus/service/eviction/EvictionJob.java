/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2018 GAEL Systems
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
package fr.gael.dhus.service.eviction;

import fr.gael.dhus.service.EvictionService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class EvictionJob implements Job
{
   private static final Logger LOGGER = LogManager.getLogger();

   @Override
   public void execute(JobExecutionContext context) throws JobExecutionException
   {
      LOGGER.info("Scheduled eviction job started");
      String evictionName = context.getJobDetail().getKey().getName();
      ApplicationContextProvider.getBean(EvictionService.class).doEvict(evictionName);
   }
}
