package org.dhus.store.datastore.async.onda;

import java.text.ParseException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dhus.scanner.schedule.ScannerJob;
import org.dhus.store.datastore.config.OndaDataStoreConf;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class OndaDataStoreScannerScheduler
{
   private static final Logger LOGGER = LogManager.getLogger(OndaDataStoreScannerScheduler.class);

   private Scheduler scheduler;

   public OndaDataStoreScannerScheduler()
   {
      try
      {
         scheduler = StdSchedulerFactory.getDefaultScheduler();
      }
      catch (SchedulerException e)
      {
         LOGGER.error(e.getMessage());
      }
   }

   public void scheduleScanner(String scannerId, OndaDataStoreConf sc) throws SchedulerException, ParseException
   {
      JobDetail job = JobBuilder.newJob (OndaDataStoreScannerJob.class)
            .withIdentity(scannerId, "OndaDataStoreScanner")
            .usingJobData(OndaDataStoreScannerJob.DATASTORE_NAME, sc.getName())
            .build ();
         
      CronTriggerFactoryBean trigger = new CronTriggerFactoryBean ();
      trigger.setJobDetail (job);
      trigger.setCronExpression (sc.getOndaScanner().getCron().getSchedule());
      trigger.setName (scannerId + "Trigger");
      trigger.afterPropertiesSet ();
      
      CronTrigger t = trigger.getObject();

      if (scheduler.checkExists(job.getKey()))
      {
         scheduler.deleteJob(job.getKey());
      }
      scheduler.scheduleJob(job,t);
   }

   public void unscheduleScanner(String scannerId) throws SchedulerException
   {
      JobDetail jobDetail = JobBuilder.newJob(OndaDataStoreScannerJob.class)
            .withIdentity(scannerId, "OndaDataStoreScanner")
            .build();

      if (scheduler.checkExists(jobDetail.getKey()))
      {
         scheduler.deleteJob(jobDetail.getKey());
      }
   }

   public void start() throws SchedulerException
   {
      scheduler.start();
   }

   public boolean isStarted() throws SchedulerException
   {
      return scheduler.isStarted();
   }
}
