package org.dhus.test.olingo.scenario.users;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.dhus.test.olingo.OlingoTestException;
import org.dhus.test.olingo.Utils;
import org.dhus.test.olingo.operations.ODataOperator;
import org.dhus.test.olingo.scenario.TestScenario;

public class ReadUsers implements TestScenario
{

   private final static Logger LOGGER = LogManager.getLogger(ReadUsers.class.getName());

   private final ODataOperator odataOperator;

   private Map<String, Object> propsRef = null;

   /* PROPERTIES */
   private static final String USERNAME = "Username";
   private static final String EMAIL = "Email";
   private static final String FIRSTNAME = "FirstName";
   private static final String LASTNAME = "LastName";
   private static final String COUNTRY = "Country";
   private static final String DOMAIN = "Domain";
   private static final String SUBDOMAIN = "SubDomain";
   private static final String USAGE = "Usage";
   private static final String SUBUSAGE = "SubUsage";
   private static final String PHONE = "Phone";
   private static final String ADDRESS = "Address";
   private static final String HASH = "Hash";
   private static final String PASSWORD = "Password";
   private static final String CREATED = "Created";
   private static final String CART = "Cart";

   /* VALUES OF PROPERTIES */
   private static final String USERNAME_USER_VALUE = "usera";
   private static final String EMAIL_USER_VALUE = "usera@gael.fr";
   private static final String FIRSTNAME_USER_VALUE = "firstNameA";
   private static final String LASTNAME_USER_VALUE = "lastNameA";
   private static final String COUNTRY_USER_VALUE = "France";
   private static final String PHONE_USER_VALUE = null;
   private static final String ADRESSE_USER_VALUE = null;
   private static final String DOMAINE_USER_VALUE = "Land";
   private static final String SUBDOMAINE_USER_VALUE = "";
   private static final String USAGE_USER_VALUE = "Research";
   private static final String SUBUSAGE_USER_VALUE = "";
   private static final String HASH_USER_VALUE = "MD5";
   private static final String PWD_USER_VALUE = "userapwd";

   public ReadUsers(ODataOperator odataOperator)
   {
      this.odataOperator = odataOperator;

      initRef();
   }

   private void initRef()
   {
      this.propsRef = new HashMap<String, Object>();

      this.propsRef.put(USERNAME, USERNAME_USER_VALUE);
      this.propsRef.put(EMAIL, EMAIL_USER_VALUE);
      this.propsRef.put(FIRSTNAME, FIRSTNAME_USER_VALUE);
      this.propsRef.put(LASTNAME, LASTNAME_USER_VALUE);
      this.propsRef.put(COUNTRY, COUNTRY_USER_VALUE);
      this.propsRef.put(PHONE, PHONE_USER_VALUE);
      this.propsRef.put(ADDRESS, ADRESSE_USER_VALUE);
      this.propsRef.put(DOMAIN, DOMAINE_USER_VALUE);
      this.propsRef.put(SUBDOMAIN, SUBDOMAINE_USER_VALUE);
      this.propsRef.put(USAGE, USAGE_USER_VALUE);
      this.propsRef.put(SUBUSAGE, SUBUSAGE_USER_VALUE);
      this.propsRef.put(HASH, HASH_USER_VALUE);

      try
      {
         this.propsRef.put(PASSWORD, Utils.MungPass(PWD_USER_VALUE).toLowerCase());
      }
      catch (NoSuchAlgorithmException e)
      {
         e.printStackTrace();
      }

   }

   public void execute() throws OlingoTestException, Exception
   {
      String id = "'usera'";

      boolean pass = true;

      ODataEntry entry = this.odataOperator.readEntry(Utils.USERS_NAME, id);

      if (entry == null)
      {
         pass = false;
         LOGGER.error("User not read ");
         throw new OlingoTestException("User not read");
      }
      else
      {
         LOGGER.info("User created");
      }

      Map<String, Object> propsOdata = entry.getProperties();

      if (propsOdata == null)
      {
         pass = false;
         LOGGER.error("Properties of user not read");
         throw new OlingoTestException("Properties of user " + id + " not read");
      }

      for (Map.Entry<String, Object> ent : this.propsRef.entrySet())
      {
         String keyRef = ent.getKey();
         Object valRef = ent.getValue();

         if (propsOdata.containsKey(keyRef))
         {

            if (valRef != null && propsOdata.get(keyRef) != null)
            {
               if (!propsOdata.get(keyRef).equals(valRef))
               {
                  pass = false;

                  LOGGER.error("Bad value for " + keyRef + " (Value of reference -> " + valRef
                        + ", value from odata -> " + propsOdata.get(keyRef) + " )\n");
               }
            }
            else if (Utils.logicalXOR(valRef == null, propsOdata.get(keyRef) == null))
            {
               pass = false;

               LOGGER.error("Error of value of " + keyRef + " : one value is null, but not other\n");
            }
         }
      }

      Utils.loggerResult(LOGGER, pass, this.getClass());

   }
}
