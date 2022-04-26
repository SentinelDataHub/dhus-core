package org.dhus.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import fr.gael.dhus.util.UncaughtExceptionHandler;

/**
 * GDPR users migration to clean users data 
 */
public class GDPRUsersMigration
{
   private static final Logger LOGGER;

   // Migration context location
   private static final String MIGR_CTX = "classpath:fr/gael/dhus/spring/dhus-core-gdpr-users-migration.xml";

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
      System.setProperty("fr.gael.dhus.version", "GDPRUsersMigration");

      LOGGER = LogManager.getLogger();

      // Transfer System.err in logger
      IoBuilder iob = IoBuilder.forLogger(LOGGER).setAutoFlush(true).setLevel(Level.ERROR);
      System.setErr(iob.buildPrintStream());
   }

   /**
    * Entry point for the GDPR users migration 
    *
    * @param args JDBCDriver JDBCUrl username password
    */
   public static void main(String[] args)
   {
      Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler.INSTANCE);
      Thread.currentThread().setUncaughtExceptionHandler(UncaughtExceptionHandler.INSTANCE);

      LOGGER.info("GDPR Users Migration");

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
         LOGGER.info("GDPR Users Migration finished!");
      }

      // Need to clean last changeset record from databasechangelog because it blocks dhus from starting in case of postgres db use.
      // The reason is that this changeset/changelog is not added in the changelog-master.
      // It is a manually executed script run when needed.
      final String errorMessage = "Failed cleaning databasechangelog table after users migration. Please do it manually by executing the following sql query : " +
            "delete from databasechangelog where id = 'cleanUsersForGDPR';";

      try (Connection connection = DriverManager.getConnection(JDBCUrl, username, password);
            PreparedStatement preparedStatement = connection
                  .prepareStatement("delete from databasechangelog where id = 'cleanUsersForGDPR';");)
      {
         if(preparedStatement.executeUpdate() == 1)
         {
            LOGGER.info("Cleaning databasechangelog table successful!");
         }
         else
         {
            LOGGER.error(errorMessage);
         }
      }
      catch (SQLException e)
      {
         LOGGER.error(errorMessage);
         LOGGER.debug(e);
      }

      LOGGER.info("Success!");
   }
}
