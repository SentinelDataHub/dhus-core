package org.dhus.test.olingo.scenario.usersynchronizers;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.dhus.test.olingo.OlingoTestException;
import org.dhus.test.olingo.Utils;
import org.dhus.test.olingo.operations.ODataOperator;
import org.dhus.test.olingo.scenario.TestScenario;

public class ReadUserSynchronizers implements TestScenario
{

   private final static Logger LOGGER = LogManager.getLogger(ReadUserSynchronizers.class.getName());

   private final ODataOperator odataOperator;

   private Map<String, Object> propsRef = null;

   /* PROPERTIES */
   private static final String ID = "Id";
   private static final String LABEL = "Label";
   private static final String REQUEST = "Request";
   private static final String SCHEDULE = "Schedule";
   private static final String SERVICEURL = "ServiceUrl";
   private static final String SERVICELOGIN = "ServiceLogin";
   private static final String CURSOR = "Cursor";
   private static final String PAGESIZE = "PageSize";
   private static final String FORCE = "Force";

   /* VALUES OF PROPERTIES */
   private static final Long ID_USERSYNCHRONIZERS_VALUE = 0L;
   private static final String LABEL_USERSYNCHRONIZERS_VALUE = "my_user_syncer";
   private static final String REQUEST_USERSYNCHRONIZERS_VALUE = "start";
   private static final String SCHEDULE_USERSYNCHRONIZERS_VALUE = "0 */3 * * * ?";
   private static final String SERVICEURL_USERSYNCHRONIZERS_VALUE =
      "http://217.182.157.231:8081/odata/v1";
   private static final String SERVICELOGIN_USERSYNCHRONIZERS_VALUE = "root";
   private static final Long CURSOR_USERSYNCHRONIZERS_VALUE = 0L;
   private static final int PAGESIZE_USERSYNCHRONIZERS_VALUE = 500;
   private static final boolean FORCE_USERSYNCHRONIZERS_VALUE = false;

   public ReadUserSynchronizers(ODataOperator odataOperator)
   {
      this.odataOperator = odataOperator;

      initRef();
   }

   private void initRef()
   {
      this.propsRef = new HashMap<String, Object>();
      this.propsRef.put(LABEL, LABEL_USERSYNCHRONIZERS_VALUE);
      this.propsRef.put(REQUEST, REQUEST_USERSYNCHRONIZERS_VALUE);
      this.propsRef.put(SCHEDULE, SCHEDULE_USERSYNCHRONIZERS_VALUE);
      this.propsRef.put(SERVICEURL, SERVICEURL_USERSYNCHRONIZERS_VALUE);
      this.propsRef.put(SERVICELOGIN, SERVICELOGIN_USERSYNCHRONIZERS_VALUE);
      this.propsRef.put(CURSOR, CURSOR_USERSYNCHRONIZERS_VALUE);
      this.propsRef.put(PAGESIZE, PAGESIZE_USERSYNCHRONIZERS_VALUE);
      this.propsRef.put(FORCE, FORCE_USERSYNCHRONIZERS_VALUE);
   }

   public void execute() throws OlingoTestException, Exception
   {
      boolean pass = true;

      ODataEntry entryRead = this.odataOperator.readEntry(Utils.USERSYNCHRONIZERS_NAME,
            String.valueOf(ID_USERSYNCHRONIZERS_VALUE));

      if (entryRead == null)
      {
         pass = false;
         LOGGER.error("UserSynchronizer not read ");
         throw new OlingoTestException("UserSynchronizer not read");
      }
      else
      {
         LOGGER.info("UserSynchronizer read ");
      }

      Map<String, Object> propsOdata = entryRead.getProperties();

      if (propsOdata == null)
      {
         pass = false;
         LOGGER.error("UserSynchronizer properties not read");
         throw new OlingoTestException("UserSynchronizer properties not read");
      }
      else
      {
         LOGGER.info("UserSynchronizer properties read");
      }

      for (Map.Entry<String, Object> ent : this.propsRef.entrySet())
      {

         String keyRef = ent.getKey();
         Object valRef = ent.getValue();

         if (valRef != null && propsOdata.get(keyRef) != null)
         {

            if (!valRef.equals(propsOdata.get(keyRef)))
            {
               pass = false;
               LOGGER.error("Bad value for " + keyRef + " (" + valRef + ", " + propsOdata.get(keyRef)
                     + ")");
            }
         }
         else if (Utils.logicalXOR(valRef == null, propsOdata.get(keyRef) == null))
         {
            LOGGER.error("Error of value of " + keyRef + " : one value is null, but not other\n");
         }
      }

      Utils.loggerResult(LOGGER, pass, this.getClass());
   }
}
