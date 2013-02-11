/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
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
package org.dhus.test.olingov4;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.client.api.domain.ClientObjectFactory;
import org.dhus.test.olingov4.scenario.DataStoresTestScenario;
import org.dhus.test.olingov4.scenario.TestScenarioV4;

public class TestManagerV4
{
   private static final Logger LOGGER = LogManager.getLogger("");
   
   private static String serviceUrl;
   private static String username;
   private static String password;
   
   private static ODataOperatorV4 odataOperator;

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
      if (args.length < 3)
      {
         LOGGER.error("Expected 3 arguments, found " + args.length);
         LOGGER.error("Required arguments: <serviceUrl> <username> <password>");
         System.exit(-2);
      }
      
      serviceUrl = args[0];
      username = args[1];
      password = args[2];
      
      odataOperator = ODataOperatorV4.initialize(serviceUrl, username, password);
      
      ClientObjectFactory factory = odataOperator.getClient().getObjectFactory();

      EntityFactory entityFactory = new EntityFactory(factory);

      // declare scenarios
      TestScenarioV4 dataStoresTest =
         new DataStoresTestScenario(entityFactory, odataOperator);
      
      // add scenarios to list
      List<TestScenarioV4> scenarios = Arrays.asList(dataStoresTest); 
                                                                         
      // execute scenarios
      for (TestScenarioV4 scenario : scenarios)
      {
         String scenarioName = scenario.getClass().getSimpleName();
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

   }
}
