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
package org.dhus.scanner.schedule;

import fr.gael.dhus.database.object.config.scanner.ScannerConfiguration;

import javax.annotation.PostConstruct;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import org.springframework.stereotype.Component;

@Component
public class ScannerScheduler
{

   private Scheduler scheduler;

   @PostConstruct
   public void init() throws SchedulerException
   {
      scheduler = StdSchedulerFactory.getDefaultScheduler();
   }

   public void scheduleScanner(ScannerConfiguration sc) throws SchedulerException
   {
      JobDetail jobDetail = JobBuilder.newJob(ScannerJob.class)
            .withIdentity(String.valueOf(sc.getId()), "Scanner")
            .build();

      Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(sc.getId() + "Trigger", "ScannerTrigger")
            .withSchedule(CronScheduleBuilder.cronSchedule(sc.getCron().getSchedule()))
            .build();

      // remove existing job if exists
      if (scheduler.checkExists(jobDetail.getKey()))
      {
         scheduler.deleteJob(jobDetail.getKey());
      }
      // schedule job
      scheduler.scheduleJob(jobDetail, trigger);
   }

   public void unscheduleScanner(ScannerConfiguration sc) throws SchedulerException
   {
      JobDetail jobDetail = JobBuilder.newJob(ScannerJob.class)
            .withIdentity(String.valueOf(sc.getId()), "Scanner")
            .build();

      // remove existing job if exists
      if (scheduler.checkExists(jobDetail.getKey()))
      {
         scheduler.deleteJob(jobDetail.getKey());
      }
   }

   public void start() throws SchedulerException
   {
      scheduler.start();
   }
}
