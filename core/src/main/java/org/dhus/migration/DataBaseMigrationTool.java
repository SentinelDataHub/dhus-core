/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018,2019 GAEL Systems
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
package org.dhus.migration;

import fr.gael.dhus.util.UncaughtExceptionHandler;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * DataBase Migration Tool.
 */
public class DataBaseMigrationTool
{
   private static final Logger LOGGER;

   // Migration context location
   private static final String MIGR_CTX = "classpath:fr/gael/dhus/spring/dhus-core-migration.xml";

   private static String JDBCDriver = "";
   private static String JDBCUrl = "";
   private static String username = "";
   private static String password = "";

   // Getters used by the migration context (SpEL expressions)

   public static String getJDBCDriver()
   {
      return JDBCDriver;
   }

   public static String getJDBCUrl()
   {
      return JDBCUrl;
   }

   public static String getUsername()
   {
      return username;
   }

   public static String getPassword()
   {
      return password;
   }

   static
   {
      // Sets up the JUL --> Log4J brigde
      System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

      // Sets version property
      System.setProperty("fr.gael.dhus.version", "DataBaseMigrationTool");

      LOGGER = LogManager.getLogger();

      // Transfer System.err in logger
      IoBuilder iob = IoBuilder.forLogger(LOGGER).setAutoFlush(true).setLevel(Level.ERROR);
      System.setErr(iob.buildPrintStream());
   }

   /**
    * Entry point for the DataBase Migration Tool.
    *
    * @param args JDBCDriver JDBCUrl username password
    */
   public static void main(String[] args)
   {
      Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler.INSTANCE);
      Thread.currentThread().setUncaughtExceptionHandler(UncaughtExceptionHandler.INSTANCE);

      LOGGER.info("DataBase Migration Tool");

      if (args.length < 2)
      {
         LOGGER.fatal("This tool expects 4 arguments: <JDBCDriver> <JDBCUrl> [username] [password]");
         System.exit(1);
      }

      JDBCDriver = args[0];
      JDBCUrl    = args[1];
      if (args.length > 2) username = args[2];
      if (args.length > 3) password = args[3];

      LOGGER.info("Configuration:");
      LOGGER.info("  JDBCDriver: '{}'", JDBCDriver);
      LOGGER.info("  JDBCUrl   : '{}'", JDBCUrl);
      LOGGER.info("  username  : '{}'", username);
      LOGGER.info("  password  : '{}'", password);

      try (ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(MIGR_CTX))
      {
         LOGGER.info("DataBase migration finished!");
      }

      LOGGER.info("Success!");
   }

}
