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

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer;
import fr.gael.dhus.sync.impl.ODataProductSynchronizer;

public class ProductSynchronizerScheduler
{
   private final Scheduler scheduler;

   public ProductSynchronizerScheduler(ProductSynchronizer productSync, ODataProductSynchronizer odataProdSync) throws SchedulerException
   {
      scheduler = StdSchedulerFactory.getDefaultScheduler();
      JobDataMap jobData = new JobDataMap();
      jobData.put(String.valueOf(productSync.getId()), odataProdSync);

      JobDetail jobDetail = JobBuilder.newJob(RankSourcesJob.class).setJobData(jobData)
            .withIdentity(String.valueOf(productSync.getId()), "ProductSynchronizer")
            .build();

      Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(String.valueOf(productSync.getId()) + "Trigger", "ProductSynchronizerTrigger")
            .withSchedule(CronScheduleBuilder.cronSchedule(productSync.getRankingSchedule()))
            .build();

      // remove existing job if exists
      if (scheduler.checkExists(jobDetail.getKey()))
      {
         scheduler.deleteJob(jobDetail.getKey());
      }
      // schedule job
      scheduler.scheduleJob(jobDetail, trigger);
   }

   public void unscheduleSource(ProductSynchronizer productSync) throws SchedulerException
   {
      JobDetail jobDetail = JobBuilder.newJob(RankSourcesJob.class)
            .withIdentity(String.valueOf(productSync.getId()), "ProductSynchronizer")
            .build();

      // remove existing job if exists
      if (scheduler.checkExists(jobDetail.getKey()))
      {
         scheduler.deleteJob(jobDetail.getKey());
      }
   }   

   public void start(long id) throws SchedulerException
   {
      scheduler.start();
      JobDetail jobDetail = JobBuilder.newJob(RankSourcesJob.class)
            .withIdentity(String.valueOf(id), "ProductSynchronizer")
            .build();
      scheduler.triggerJob(jobDetail.getKey());
   }
}