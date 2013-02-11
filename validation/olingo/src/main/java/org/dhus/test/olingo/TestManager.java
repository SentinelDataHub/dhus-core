package org.dhus.test.olingo;

import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dhus.test.olingo.context.AppConfig;
import org.dhus.test.olingo.scenario.TestScenario;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * This class contains the main method, which allows to launch the test
 * scenarios
 */
public class TestManager
{
   private static final Logger LOGGER = LogManager.getLogger("");

   public static final String SERVICE_URL = "dhus.test.scenario.url";
   public static final String SERVICE_USERNAME = "dhus.test.scenario.username";
   public static final String SERVICE_PASSWORD = "dhus.test.scenario.password";
   public static final String DATA_DIR = "dhus.test.scenario.data.dir";

   public static void logScenarioInfo(String info)
   {
      LOGGER.info("|| " + info);
   }
   
   public static void logScenarioError(String info)
   {
      LOGGER.error("|# " + info);
   }
   
   public static void main(String[] args) throws Exception
   {
      if (args.length != 4)
      {
         LOGGER.error("Expected 4 arguments, found " + args.length);
         LOGGER.error("Required arguments: <serviceUrl> <username> <password> <testFileDirectory>");
         System.exit(-2);
      }

      System.setProperty(SERVICE_URL, args[0]);
      System.setProperty(SERVICE_USERNAME, args[1]);
      System.setProperty(SERVICE_PASSWORD, args[2]);
      System.setProperty(DATA_DIR, args[3]);
      
      AnnotationConfigApplicationContext context;
      context = new AnnotationConfigApplicationContext(AppConfig.class);
      context.registerShutdownHook();

      Set<Map.Entry<String, TestScenario>> scenarios =
            context.getBeansOfType(TestScenario.class).entrySet();

      // execute scenarios
      LOGGER.info("----------------------------------------------------------------");
      LOGGER.info("   Executing test scenarii using test directory: " + System.getProperty(DATA_DIR));
      LOGGER.info("----------------------------------------------------------------");
      for (Map.Entry<String, TestScenario> entry : scenarios)
      {
         String scenarioName = entry.getKey();
         TestScenario scenario = entry.getValue();
         try
         {
            LOGGER.info("================================================================");
            LOGGER.info(String.format("   Running test scenario '%s'", scenarioName));
            LOGGER.info("================================================================");
            scenario.execute();
         }
         catch (OlingoTestException e)
         {
            LOGGER.error("################################################################");
            LOGGER.error("   Test scenario '" + scenarioName + "' failed", e);
            LOGGER.error("################################################################");
            System.exit(1);
         }
         catch (Exception e)
         {
            LOGGER.error("################################################################");
            LOGGER.error("   Unexpected exception during '" + scenarioName+"'", e);
            LOGGER.error("################################################################");
            System.exit(-1);
         }
         LOGGER.info("****************************************************************");
         LOGGER.info(String.format("   Test scenario '%s' ran successfully", scenarioName));
         LOGGER.info("****************************************************************");
      }

      context.close();
   }
}
