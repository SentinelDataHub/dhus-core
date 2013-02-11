/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2021 GAEL Systems
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


package fr.gael.dhus.sync.intelligent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer;
import fr.gael.dhus.service.IProductSourceService;
import fr.gael.dhus.service.ISynchronizerService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.sync.impl.ODataProductSynchronizer;

public class RankSourcesJob implements Job 
{
   private static final ISynchronizerService SYNCHRONIZER_SERVICE =
         ApplicationContextProvider.getBean(ISynchronizerService.class);
   private static final IProductSourceService PRODUCT_SOURCE_SERVICE =
         ApplicationContextProvider.getBean(IProductSourceService.class);
   

   private static final Logger LOGGER = LogManager.getLogger();

   @Override
   public void execute(JobExecutionContext context) throws JobExecutionException
   {  
      LOGGER.info("Start ranking the sources for the synchronizer: "+ context.getJobDetail().getKey().getName());
      long syncId = Long.parseLong(context.getJobDetail().getKey().getName());
      ODataProductSynchronizer odataProdSync =(ODataProductSynchronizer) context.getJobDetail().getJobDataMap()
               .get(context.getJobDetail().getKey().getName());

      ProductSynchronizer productSync = SYNCHRONIZER_SERVICE.getSynchronizerConfById(syncId, ProductSynchronizer.class);
      PRODUCT_SOURCE_SERVICE.rankSources(productSync, odataProdSync);
   } 
}