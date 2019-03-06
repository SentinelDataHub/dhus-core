package org.dhus.test.olingo.scenario.users;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.dhus.test.olingo.OlingoTestException;
import org.dhus.test.olingo.Utils;
import org.dhus.test.olingo.operations.ODataOperator;
import org.dhus.test.olingo.scenario.TestScenario;

public class UpdateUsers implements TestScenario
{

   private final static Logger LOGGER = LogManager.getLogger(UpdateUsers.class.getName());

   private final ODataOperator odataOperator;
   private Long id = 0L;

   private Map<String, Object> propsRef = null, propsModified = null;

   /* PROPERTIES */
   private static final String EMAIL = "Email";
   private static final String DOMAIN = "Domain";
   private static final String USAGE = "Usage";
   private static final String CREATED = "Created";
   private static final String PWD = "Password";

   /* VALUES OF PROPERTIES */
   private static final String EMAIL_USER_VALUE = "newuserb@gael.fr";
   private static final String DOMAIN_USER_VALUE = "Security";
   private static final String USAGE_USER_VALUE = "Commercial";
   private static final String PWD_USER_VALUE = "userbpwd";

   public UpdateUsers(ODataOperator odataOperator)
   {

      this.odataOperator = odataOperator;

      initRefAndModified();
   }

   private void initRefAndModified()
   {
      this.propsRef = new HashMap<String, Object>();
      this.propsModified = new HashMap<String, Object>();

      this.propsModified.put(EMAIL, EMAIL_USER_VALUE);
      this.propsModified.put(DOMAIN, DOMAIN_USER_VALUE);
      this.propsModified.put(USAGE, USAGE_USER_VALUE);
      this.propsModified.put(PWD, PWD_USER_VALUE);
   }

   public void execute() throws OlingoTestException, Exception
   {
      String id = "'userb'";
      boolean pass = true;

      ODataEntry entryRead = this.odataOperator.readEntry(Utils.USERS_NAME, String.valueOf(id));

      if (entryRead == null)
      {
         pass = false;
         LOGGER.error("User not read ");
         throw new OlingoTestException("User not read");
      }
      else
      {
         LOGGER.info("User read ");
      }

      LOGGER.info("Get properties set of User from odata");
      Map<String, Object> propsOdata = new HashMap<String, Object>(entryRead.getProperties());

      LOGGER.info("Duplicate set of properties for reference");
      for (Map.Entry<String, Object> ent : propsOdata.entrySet())
      {
         this.propsRef.put(ent.getKey(), ent.getValue());
      }

      LOGGER.info("Change values for several properties");
      for (Map.Entry<String, Object> ent : this.propsModified.entrySet())
      {
         propsOdata.put(ent.getKey(), ent.getValue());
      }

      LOGGER.info("Update User of ODATA");
      this.odataOperator.updateEntry(Utils.USERS_NAME, String.valueOf(id), propsOdata);

      LOGGER.info("New Read Users of ODATA");
      entryRead = this.odataOperator.readEntry(Utils.USERS_NAME, String.valueOf(id));

      if (entryRead == null)
      {
         pass = false;
         LOGGER.error("User not read ");
         throw new OlingoTestException("User not read");
      }
      else
      {
         LOGGER.info("User read ");
      }

      for (Map.Entry<String, Object> ent : entryRead.getProperties().entrySet())
      {
         String keyOdata = ent.getKey();
         Object valOdata = ent.getValue();

         if (this.propsModified.containsKey(keyOdata))
         {

            if (valOdata != null && this.propsModified.get(keyOdata) != null)
            {
               if (!valOdata.equals(this.propsModified.get(keyOdata)))
               {
                  LOGGER.error("Bad value for " + keyOdata + " (value from odata modified -> "
                        + valOdata + ", Value of reference modified -> "
                        + this.propsModified.get(keyOdata) + " )\n");
               }
            }
            else if (Utils.logicalXOR(valOdata == null, this.propsModified.get(keyOdata) == null))
            {
               LOGGER.error(
                     "Error of value of " + keyOdata + " : one value is null, but not other\n");
            }

         }
         else if (this.propsRef.containsKey(keyOdata))
         {

            if (valOdata != null && this.propsRef.get(keyOdata) != null)
            {
               if (!valOdata.equals(this.propsRef.get(keyOdata)))
               {
                  LOGGER.error("Bad value for " + keyOdata + " (value from odata modified -> "
                        + valOdata + ", Value of reference modified -> "
                        + this.propsRef.get(keyOdata) + " )\n");
               }
            }
            else if (Utils.logicalXOR(valOdata == null, this.propsRef.get(keyOdata) == null))
            {
               LOGGER.error(
                     "Error of value of " + keyOdata + " : one value is null, but not other\n");
            }

         }

      }

      LOGGER.info("Get properties set of User from odata");
      propsOdata = new HashMap<String, Object>(entryRead.getProperties());

      LOGGER.info("reset User of ODATA");
      this.odataOperator.updateEntry("Users", String.valueOf(id), this.propsRef);

      Utils.loggerResult(LOGGER, pass, this.getClass());
   }
}
