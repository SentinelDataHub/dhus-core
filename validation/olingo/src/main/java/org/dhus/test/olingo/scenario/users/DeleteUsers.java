package org.dhus.test.olingo.scenario.users;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dhus.test.olingo.OlingoTestException;
import org.dhus.test.olingo.Utils;
import org.dhus.test.olingo.operations.ODataOperator;
import org.dhus.test.olingo.scenario.TestScenario;

public class DeleteUsers implements TestScenario
{

   private final static Logger LOGGER = LogManager.getLogger(DeleteUsers.class.getName());

   private final ODataOperator odataOperator;
   private int statusCode2 = -1;

   public DeleteUsers(ODataOperator odataOperator)
   {
      this.odataOperator = odataOperator;
   }

   public void execute() throws OlingoTestException
   {
      String id = "'userb'";
      try
      {
         statusCode2 = odataOperator.deleteEntry(Utils.USERS_NAME, id);
         LOGGER.info("Deletion of Entry successful: " + statusCode2);
      }
      catch (IOException e)
      {
         e.printStackTrace();
         LOGGER.error("Test of " + this.getClass().getSimpleName() + " does not passed : "
               + e.getMessage());
      }
      catch (RuntimeException e)
      {
         e.printStackTrace();
         LOGGER.error("Test of " + this.getClass().getSimpleName() + " does not passed : "
               + e.getMessage());
      }
      catch (Exception e)
      {
         e.printStackTrace();
         LOGGER.error("Test of " + this.getClass().getSimpleName() + " does not passed : "
               + e.getMessage());
      }

   }

}
