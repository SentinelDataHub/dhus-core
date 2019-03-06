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

public class UpdateUserSynchronizers implements TestScenario
{

   private final static Logger LOGGER =
      LogManager.getLogger(UpdateUserSynchronizers.class.getName());

   private final ODataOperator odataOperator;
   private Long id = 0L;

   private Map<String, Object> propsRef = null, propsModified = null;

   /* PROPERTIES */
   private static final String ID = "Id";
   private static final String CREATIONDATE = "CreationDate";
   private static final String STATUSDATE = "StatusDate";
   private static final String MODIFICATIONDATE = "ModificationDate";
   private static final String LABEL = "Label";
   private static final String SCHEDULE = "Schedule";
   private static final String SERVICEPASSWORD = "ServicePassword";

   /* VALUES OF PROPERTIES */
   private static final String LABEL_USERSYNC_VALUE = "My New Label";
   private static final String SCHEDULE_USERSYNC_VALUE = "0 */4 * * * ?";
   private static final String SERVICEPASSWORD_USERSYNC_DEFAULT_VALUE = "a";

   public UpdateUserSynchronizers(ODataOperator odataOperator)
   {
      this.odataOperator = odataOperator;

      initRefAndModified();
   }

   private void initRefAndModified()
   {
      this.propsRef = new HashMap<String, Object>();
      this.propsModified = new HashMap<String, Object>();

      this.propsModified.put(LABEL, LABEL_USERSYNC_VALUE);
      this.propsModified.put(SCHEDULE, SCHEDULE_USERSYNC_VALUE);
      this.propsModified.put(SERVICEPASSWORD, SERVICEPASSWORD_USERSYNC_DEFAULT_VALUE);
   }

   public void execute() throws OlingoTestException, Exception
   {
      Long id = 0L;
      boolean pass = true;

      ODataEntry entryRead =
         this.odataOperator.readEntry(Utils.USERSYNCHRONIZERS_NAME, String.valueOf(id));

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

      LOGGER.info("Get properties set of Event from odata");
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

      for (Map.Entry<String, Object> ent : propsOdata.entrySet())
      {
         System.out.println(ent.getKey() + " : " + ent.getValue());
      }

      propsOdata.put("PageSize", ((Integer) propsOdata.get("PageSize")).intValue());

      LOGGER.info("Update UserSynchronizer of ODATA");
      this.odataOperator.updateEntry(Utils.USERSYNCHRONIZERS_NAME, String.valueOf(id), propsOdata);

      // LOGGER.info("New Read UserSynchronizer of ODATA");
      // entryRead =
      // this.odataOperator.readEntry(Utils.USERSYNCHRONIZERS_NAME,
      // String.valueOf(id));
      //
      // if (entryRead == null) {
      // pass = false;
      // LOGGER.error("UserSynchronizer not read ");
      // throw new OlingoTestException("UserSynchronizer not read");
      // } else {
      // LOGGER.info("UserSynchronizer read ");
      // }
      //
      // for (Map.Entry<String, Object> ent : entryRead.getProperties()
      // .entrySet()) {
      // String keyOdata = ent.getKey();
      // Object valOdata = ent.getValue();
      //
      // if (!keyOdata.equals(ID) && !keyOdata.equals(SERVICEPASSWORD)
      // && !keyOdata.equals(STATUSDATE)
      // && !keyOdata.equals(CREATIONDATE)
      // && !keyOdata.equals(MODIFICATIONDATE)) {
      //
      // if (this.propsModified.containsKey(keyOdata)) {
      //
      // if (valOdata != null
      // && this.propsModified.get(keyOdata) != null) {
      // if (!valOdata.equals(this.propsModified.get(keyOdata))) {
      // pass = false;
      // LOGGER.error("Bad value for " + keyOdata
      // + " (value from odata modified -> "
      // + valOdata
      // + ", Value of reference modified -> "
      // + this.propsModified.get(keyOdata) + " )\n");
      // }
      // } else if (Utils.logicalXOR(valOdata == null,
      // this.propsModified.get(keyOdata) == null)) {
      // pass = false;
      // LOGGER.error("Error of value of " + keyOdata
      // + " : one value is null, but not other\n");
      // }
      // } else if (this.propsRef.containsKey(keyOdata)) {
      //
      // if (valOdata != null && this.propsRef.get(keyOdata) != null) {
      // if (!valOdata.equals(this.propsRef.get(keyOdata))) {
      // pass = false;
      // LOGGER.error("Bad value for " + keyOdata
      // + " (value from odata modified -> "
      // + valOdata
      // + ", Value of reference modified -> "
      // + this.propsRef.get(keyOdata) + " )\n");
      // }
      // } else if (Utils.logicalXOR(valOdata == null,
      // this.propsModified.get(keyOdata) == null)) {
      // pass = false;
      // LOGGER.error("Error of value of " + keyOdata
      // + " : one value is null, but not other\n");
      // }
      // }
      // }
      // }
      //
      // LOGGER.info("Get properties set of UserSynchronizer from odata");
      // propsOdata = new HashMap<String, Object>(entryRead.getProperties());
      //
      // this.propsRef.put(SERVICEPASSWORD,
      // SERVICEPASSWORD_USERSYNC_DEFAULT_VALUE);
      //
      // LOGGER.info("reset UserSynchronizer of ODATA");
      // this.odataOperator.updateEntry(Utils.USERSYNCHRONIZERS_NAME,
      // String.valueOf(id), this.propsRef);
      //
      // Utils.loggerResult(LOGGER, pass, this.getClass());

   }
}
