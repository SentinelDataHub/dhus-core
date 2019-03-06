/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013-2018 GAEL Systems
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
package fr.gael.dhus;

import fr.gael.dhus.database.DatabasePostInit;
import fr.gael.dhus.search.SolrDao;
import fr.gael.dhus.server.http.TomcatException;
import fr.gael.dhus.server.http.TomcatServer;
import fr.gael.dhus.server.http.webapp.WebApplication;
import fr.gael.dhus.service.ISynchronizerService;
import fr.gael.dhus.service.SystemService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationException;
import fr.gael.dhus.system.config.ConfigurationManager;
import fr.gael.dhus.util.UncaughtExceptionHandler;

import fr.gael.drb.DrbItem;
import fr.gael.drb.impl.DrbFactoryResolver;
import fr.gael.drbx.cortex.DrbCortexMetadataResolver;
import fr.gael.drbx.cortex.DrbCortexModel;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;

import org.dhus.store.datastore.DataStoreFactory;
import org.dhus.store.datastore.DataStoreManager;
import org.dhus.store.datastore.config.DataStoreConf;

import org.springframework.context.ApplicationListener;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * DHuS Main class.
 *
 * Spawns servers, creates the Spring application context, starts background process.
 */
public class DHuS
{
   /** Logger. */
   private static final Logger LOGGER;

   /** {@code true} if the DHuS is started. */
   private static boolean started = false;

   /** Apache Tomcat: HTTP server, servlet container, JSP processor, ... */
   private static TomcatServer server;

   static
   {
      // Overriding default BusFactory to remove the ClientAbortException stacktrace
      System.setProperty("org.apache.cxf.bus.factory","fr.gael.dhus.util.log.ClientAbortRemovalBusFactory");

      // Sets up the JUL --> Log4J brigde
      System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
      LOGGER = LogManager.getFormatterLogger(DHuS.class);

      // Sets version property
      String version = DHuS.class.getPackage().getImplementationVersion();
      System.setProperty("fr.gael.dhus.version", version == null ? "dev" : version);

      // Transfer System.err in logger
      IoBuilder iob = IoBuilder.forLogger(LOGGER).setAutoFlush(true).setLevel(Level.ERROR);
      System.setErr(iob.buildPrintStream());
   }

   /** Hide utility class constructor. */
   private DHuS () {}

   /**
    * Returns true if DHuS is already started.
    * @return true if DHuS is already started, otherwise false.
    */
   public static boolean isStarted ()
   {
      return started;
   }

   /** Starts the DHuS (starts Tomcat, creates the Spring application context. */
   public static void start ()
   {
      // Force ehcache not to call home
      System.setProperty ("net.sf.ehcache.skipUpdateCheck", "true");
      System.setProperty ("org.terracotta.quartz.skipUpdateCheck", "true");
      System.setProperty ("user.timezone", "UTC");
      TimeZone.setDefault (TimeZone.getTimeZone ("UTC"));

      if (!SystemService.restore ())
      {
         LOGGER.error ("Cannot run system restoration.");
         LOGGER.error ("Check the restoration file \"" + 
            SystemService.RESTORATION_PROPERTIES + 
            "\" from the current directory.");
         System.exit (1);
      }

      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            ConfigurationManager cfg = ApplicationContextProvider.getBean(ConfigurationManager.class);
            if (cfg != null)
            {
               try
               {
                  cfg.saveConfiguration();
               }
               catch (ConfigurationException e) {}
            }
            if ((server != null) && server.isRunning())
            {
               try
               {
                  server.stop();
               }
               catch (TomcatException e) {}
            }
         }
      }));

      // Always add JMSAppender
      //Logger rootLogger = LogManager.getRootLogger ();
      //org.apache.logging.log4j.core.Logger coreLogger =
      //(org.apache.logging.log4j.core.Logger)rootLogger;
      //JMSAppender jmsAppender = JMSAppender.createAppender ();
      //coreLogger.addAppender (jmsAppender);
      try
      {
         // Activates the resolver for Drb
         DrbFactoryResolver.setMetadataResolver (new DrbCortexMetadataResolver (
               DrbCortexModel.getDefaultModel ()));
      }
      catch (IOException e)
      {
         LOGGER.error ("Resolver cannot be handled.");
         //logger.error (new Message(MessageType.SYSTEM,
         //"Resolver cannot be handled."));
      }

      LOGGER.info ("Launching Data Hub Service...");
      //logger.info (new Message(MessageType.SYSTEM,
      //"Loading Data Hub Service..."));

      ClassPathXmlApplicationContext migrationContext =
            new ClassPathXmlApplicationContext("classpath:fr/gael/dhus/spring/dhus-core-migration.xml");
      migrationContext.close();

      ClassPathXmlApplicationContext context
            = new ClassPathXmlApplicationContext (
                  "classpath:fr/gael/dhus/spring/dhus-core-context.xml");
      context.registerShutdownHook ();

      ConfigurationManager config = context.getBean(ConfigurationManager.class);

      try
      {
         // Adding DataStores to the manager
         DataStoreManager ds_manager = context.getBean(DataStoreManager.class);
         for (DataStoreConf conf: config.getDataStoreManager().getAllDataStoreConfigurations())
         {
            ds_manager.add(DataStoreFactory.createDataStore(conf));
         }

         // Registers ContextClosedEvent listeners to properly save states before
         // the Spring context is destroyed.
         ApplicationListener sync_sv = context.getBean (ISynchronizerService.class);
         context.addApplicationListener (sync_sv);

         // Initialize DHuS loggers
         //jmsAppender.cleanWaitingLogs ();
         //logger.info (new Message(MessageType.SYSTEM, "DHuS Started"));

         server = ApplicationContextProvider.getBean (TomcatServer.class);
         server.init ();

         LOGGER.info ("Starting server " + server.getClass () + "...");
         //logger.info (new Message(MessageType.SYSTEM, "Starting server..."));
         server.start ();
         //logger.info (new Message(MessageType.SYSTEM, "Server started."));

         LOGGER.info ("Server started.");

         // Initialises SolrDAO
         SolrDao solr_dao = (SolrDao) context.getBean("solrDao");
         solr_dao.initServerStarted();

         Map<String, WebApplication> webapps =
               context.getBeansOfType (WebApplication.class);
         for (String beanName: webapps.keySet ())
         {
            server.install(webapps.get(beanName));
         }

         context.getBean(DatabasePostInit.class).init();
         context.getBean(ISynchronizerService.class).init();

         LOGGER.info ("Server is ready...");
         started = true;

         //InitializableComponent.initializeAll ();
         //logger.info (new Message(MessageType.SYSTEM, "Server is ready..."));
         server.await ();
         context.close ();
      }
      catch (Exception e)
      {
         LOGGER.error ("Cannot start system.", e);
         //logger.error (new Message(MessageType.SYSTEM, "Cannot start DHuS."), e);
         if (context != null) context.close ();
         System.exit (1);
      }
   }

   /**
    * Allows to exit program.
    * @param exit_code return code value.
    */
   public static void stop (int exit_code)
   {
      //logger.info (new Message(MessageType.SYSTEM, "DHuS Shutdown "));
      System.exit (exit_code);
   }

   private static void initializeVersion()
   {
      String version;
      String src_path = DHuS.class.getProtectionDomain().getCodeSource().getLocation().getPath();
      File ver_file = new File(src_path.substring(0, src_path.lastIndexOf("/") + 1) + ".version");
      try (Scanner scanner = new Scanner(ver_file))
      {
         if (scanner != null && scanner.hasNextLine())
         {
            version = scanner.nextLine();
            System.setProperty("fr.gael.dhus.version", version);
         }
      }
      catch (IOException e)
      {
         LOGGER.info("Cannot retrieve DHuS-software version.");
      }
   }

   private static void printVersions()
   {
      LOGGER.info("Data Hub Service initialization...");
      LOGGER.info("%-35s%s", "dhus-distribution", System.getProperty("fr.gael.dhus.version"));
      LOGGER.info("%-35s%s", "dhus-core", DHuS.class.getPackage().getImplementationVersion());
      LOGGER.info("%-35s%s", "DRB", DrbItem.class.getPackage().getImplementationVersion());
   }

   /**
    * Main method.
    *
    * @param args String table of arguments (command line).
    */
   public static void main(String[] args)
   {
      Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler.INSTANCE);
      Thread.currentThread().setUncaughtExceptionHandler(UncaughtExceptionHandler.INSTANCE);
      boolean printVersion = false;
      if (args.length != 0)
      {
         for (String arg: args)
         {
            if ("--version".equals(arg) || "-v".equals(arg))
            {
               printVersion = true;
               break;
            }
         }
      }

      initializeVersion();
      printVersions();
      if (!printVersion)
      {
         start();
      }
   }
}
