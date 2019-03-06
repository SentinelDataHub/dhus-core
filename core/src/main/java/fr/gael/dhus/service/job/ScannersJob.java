/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2017 GAEL Systems
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
package fr.gael.dhus.service.job;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.gael.dhus.DHuS;
import fr.gael.dhus.database.object.config.scanner.ScannerInfo;
import fr.gael.dhus.datastore.scanner.ScannerException;
import fr.gael.dhus.datastore.scanner.ScannerFactory;
import fr.gael.dhus.system.config.ConfigurationManager;

/**
 * Autowired by {@link AutowiringJobFactory}
 */
@Component
public class ScannersJob extends AbstractJob
{
   private static final Logger LOGGER = LogManager.getLogger(ScannersJob.class);

   private static int thread_counter = 0;

   @Autowired
   private ScannerFactory scannerFactory;

   @Autowired
   private ConfigurationManager configurationManager;

   @Override
   public String getCronExpression()
   {
      return configurationManager.getFileScannersCronConfiguration().getSchedule();
   }

   @Override
   protected void executeInternal(JobExecutionContext arg0)
         throws JobExecutionException
   {
      long start = System.currentTimeMillis();

      if (!configurationManager.getFileScannersCronConfiguration().isActive())
      {
         return;
      }
      LOGGER.info("SCHEDULER : Scanners.");
      if (!DHuS.isStarted())
      {
         LOGGER.warn("SCHEDULER : Not run while system not fully initialized.");
         return;
      }

      LOGGER.info("Running Scanners Executions.");
      for (final ScannerInfo info: configurationManager.getScannerManager().getScanners())
      {
         if (!info.isActive())
         {
            LOGGER.info("Scanner #{} \"{}\" is disabled.", info.getId(), info.getUrl());
            continue;
         }

         Runnable runnable = new Runnable()
         {
            @Override
            public void run()
            {
               try
               {
                  LOGGER.info("Scanner #{} \"{}\" started.", info.getId(), info.getUrl());
                  scannerFactory.processScan(info.getId());
               }
               catch (ScannerException e)
               {
                  LOGGER.info("Scanner #{} \"{}\" not started: {}", info.getId(), info.getUrl(), e.getMessage());
               }
            }
         };
         // Asynchronously run all scanners.
         Thread thread = new Thread(runnable, "scanner-job-"+ (++thread_counter));
         if (thread_counter > 100)
         {
            thread_counter = 0;
         }
         thread.start();
      }
      LOGGER.info("SCHEDULER : Products scanners done - {}ms", System.currentTimeMillis() - start);
   }
}
