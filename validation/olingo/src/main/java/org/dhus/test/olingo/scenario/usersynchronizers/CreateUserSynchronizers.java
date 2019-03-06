package org.dhus.test.olingo.scenario.usersynchronizers;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.dhus.test.olingo.OlingoTestException;
import org.dhus.test.olingo.Utils;
import org.dhus.test.olingo.operations.ODataOperator;
import org.dhus.test.olingo.scenario.TestScenario;

public class CreateUserSynchronizers implements TestScenario
{

   private final static Logger LOGGER = LogManager.getLogger(CreateUserSynchronizers.class.getName());

   private final ODataOperator odataOperator;
   private int statusCode2;

   private Map<String, Object> propsRef = null;

   /* PROPERTIES */
   public static final String ID = "Id";
   public static final String LABEL = "Label";
   public static final String SCHEDULE = "Schedule";
   public static final String REQUEST = "Request";
   public static final String STATUS = "Status";
   public static final String STATUS_DATE = "StatusDate";
   public static final String STATUS_MESSAGE = "StatusMessage";
   public static final String CREATION_DATE = "CreationDate";
   public static final String MODIFICATION_DATE = "ModificationDate";
   public static final String SERVICE_URL = "ServiceUrl";
   public static final String SERVICE_LOGIN = "ServiceLogin";
   public static final String SERVICE_PASSWORD = "ServicePassword";
   public static final String CURSOR = "Cursor";
   public static final String PAGE_SIZE = "PageSize";
   public static final String FORCE = "Force";

   /* VALUES OF PROPERTIES */
   private static final String LABEL_USERSYNC_VALUE = "my_user_syncer";
   private static final String REQUEST_USERSYNC_VALUE = "stop";
   private static final String SCHEDULE_USERSYNC_VALUE = "0 */3 * * * ?";
   private static final String STATUS_USERSYNC_VALUE = "PENDING";
   private static final String STATUSMESSAGE_USERSYNC_VALUE =
      "Next activation: Tue Sep 05 14:51:00 UTC 2017";
   private static final String SERVICEURL_USERSYNC_VALUE = "http://217.182.157.231:8081/odata/v1";
   private static final String SERVICELOGIN_USERSYNC_VALUE = "root";
   private static final String SERVICEPASSWORD_USERSYNC_VALUE = "password";
   private static final int CURSOR_USERSYNC_VALUE = 0;
   private static final int PAGESIZE_USERSYNC_VALUE = 500;
   private static final boolean FORCE_USERSYNC_VALUE = false;

   public CreateUserSynchronizers(ODataOperator odataOperator)
   {
      this.odataOperator = odataOperator;

      initRef();
   }

   private void initRef()
   {
      this.propsRef = new HashMap<String, Object>();

      this.propsRef.put(ID, 1L);
      this.propsRef.put(LABEL, LABEL_USERSYNC_VALUE);
      this.propsRef.put(CREATION_DATE, (new Date()));
      this.propsRef.put(MODIFICATION_DATE, (new Date()));
      this.propsRef.put(REQUEST, REQUEST_USERSYNC_VALUE);
      this.propsRef.put(SCHEDULE, SCHEDULE_USERSYNC_VALUE);
      this.propsRef.put(STATUS, STATUS_USERSYNC_VALUE);
      // this.propsRef.put(STATUS_DATE, (new Date()));
      // this.propsRef.put(STATUS_MESSAGE, STATUSMESSAGE_USERSYNC_VALUE);
      this.propsRef.put(SERVICE_URL, SERVICEURL_USERSYNC_VALUE);
      this.propsRef.put(SERVICE_LOGIN, SERVICELOGIN_USERSYNC_VALUE);
      this.propsRef.put(SERVICE_PASSWORD, SERVICEPASSWORD_USERSYNC_VALUE);
      this.propsRef.put(CURSOR, CURSOR_USERSYNC_VALUE);
      this.propsRef.put(PAGE_SIZE, PAGESIZE_USERSYNC_VALUE);
      this.propsRef.put(FORCE, FORCE_USERSYNC_VALUE);

   }

   public void execute() throws OlingoTestException, Exception
   {
      Long id = 0L;

      boolean pass = true;

      ODataEntry entryCreate =
         this.odataOperator.createEntry(Utils.USERSYNCHRONIZERS_NAME, this.propsRef);

      if (entryCreate == null)
      {
         LOGGER.error("UserSynchronizer not created");
         throw new OlingoTestException("UserSynchronizer not created");
      }
      else
      {
         LOGGER.info("UserSynchronizer created");
      }

      id = (Long) entryCreate.getProperties().get(ID);

      ODataEntry entryRead =
         this.odataOperator.readEntry(Utils.USERSYNCHRONIZERS_NAME, String.valueOf(id));

      if (entryRead == null)
      {
         LOGGER.error("UserSynchronizer not read ");
         throw new OlingoTestException("UserSynchronizer not read");
      }
      else
      {
         LOGGER.info("UserSynchronizer read");
      }

      Map<String, Object> propsOdata = entryRead.getProperties();

      if (propsOdata == null)
      {
         pass = false;
         LOGGER.error("No properties read");
         throw new OlingoTestException("No properties read");
      }
      else
      {
         LOGGER.info("Properties read");
      }

      for (Map.Entry<String, Object> ent : this.propsRef.entrySet())
      {

         String keyRef = ent.getKey();
         Object valRef = ent.getValue();

         if (propsOdata.containsKey(keyRef))
         {

            if (!keyRef.equals(ID))
            {

               if (valRef != null && propsOdata.get(keyRef) != null)
               {
                  if (!valRef.equals(propsOdata.get(keyRef)))
                  {
                     pass = false;
                     LOGGER.error("Bad value for " + keyRef + " (" + valRef + ", "
                           + propsOdata.get(keyRef) + ")");
                  }
               }
               else if (Utils.logicalXOR(valRef == null, propsOdata.get(keyRef) == null))
               {
                  LOGGER.error(
                        "Error of value of " + keyRef + " : one value is null, but not other\n");
               }
            }
         }
      }

      statusCode2 = this.odataOperator.deleteEntry(Utils.USERSYNCHRONIZERS_NAME, String.valueOf(id));
      LOGGER.info("Deletion of Entry successful: " + statusCode2);

      Utils.loggerResult(LOGGER, pass, this.getClass());
   }
}
