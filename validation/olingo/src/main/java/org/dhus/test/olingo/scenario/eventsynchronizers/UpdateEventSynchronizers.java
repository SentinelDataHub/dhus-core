package org.dhus.test.olingo.scenario.eventsynchronizers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.dhus.test.olingo.OlingoTestException;
import org.dhus.test.olingo.Utils;
import org.dhus.test.olingo.operations.ODataOperator;
import org.dhus.test.olingo.scenario.TestScenario;

public class UpdateEventSynchronizers implements TestScenario
{

   private final static Logger LOGGER =
      LogManager.getLogger(UpdateEventSynchronizers.class.getName());

   private DateFormat df = new SimpleDateFormat(Utils.DATE_FORMAT_STRING);

   private final ODataOperator odataOperator;
   private Long id = 0L;

   private Map<String, Object> propsRef = null, propsModified = null;

   /* PROPERTIES */
   private static final String ID = "Id";
   private static final String LABEL = "Label";
   private static final String CREATIONDATE = "CreationDate";
   private static final String MODIFICATIONDATE = "ModificationDate";
   private static final String SCHEDULE = "Schedule";
   private static final String STATUSDATE = "StatusDate";
   private static final String SERVICEPASSWORD = "ServicePassword";

   private static final String LABEL_EVENT_SYNC_VALUE = "Events sync 1";
   private static final String SCHEDULE_EVENT_VALUE = "0 0/2 * * * ?";

   public UpdateEventSynchronizers(ODataOperator odataOperator)
   {

      df.setTimeZone(TimeZone.getTimeZone(Utils.GMT_TIMEZONE));

      this.odataOperator = odataOperator;

      initRefAndModified();
   }

   private void initRefAndModified()
   {
      this.propsRef = new HashMap<String, Object>();
      this.propsModified = new HashMap<String, Object>();

      this.propsModified.put(LABEL, LABEL_EVENT_SYNC_VALUE);
      this.propsModified.put(SCHEDULE, SCHEDULE_EVENT_VALUE);
   }

   public void execute() throws OlingoTestException, Exception
   {

      Long id = 0L;
      boolean pass = true;

      ODataEntry entryRead =
         this.odataOperator.readEntry(Utils.EVENTSYNCHRONIZERS_NAME, String.valueOf(id));

      if (entryRead == null)
      {
         pass = false;
         LOGGER.error("EventSynchronizer not read ");
         throw new OlingoTestException("EventSynchronizer not read");
      }
      else
      {
         LOGGER.info("EventSynchronizer read ");
      }

      LOGGER.info("Get properties set of EventSynchronizer from odata");
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

      LOGGER.info("Update Events of ODATA");
      this.odataOperator.updateEntry(Utils.EVENTSYNCHRONIZERS_NAME, String.valueOf(id), propsOdata);

      LOGGER.info("New Read Events of ODATA");
      entryRead = this.odataOperator.readEntry(Utils.EVENTSYNCHRONIZERS_NAME, String.valueOf(id));

      if (entryRead == null)
      {
         pass = false;
         LOGGER.error("EventSynchronizer not read ");
         throw new OlingoTestException("EventSynchronizer not read");
      }
      else
      {
         LOGGER.info("EventSynchronizer read ");
      }

      for (Map.Entry<String, Object> ent : entryRead.getProperties().entrySet())
      {
         String keyOdata = ent.getKey();
         Object valOdata = ent.getValue();

         if (!keyOdata.equals(ID) && !keyOdata.equals(CREATIONDATE)
               && !keyOdata.equals(MODIFICATIONDATE) && !keyOdata.equals(STATUSDATE)
               && !keyOdata.equals(SERVICEPASSWORD))
         {

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
      }

      LOGGER.info("Get properties set of EventSynchronizer from odata");
      propsOdata = new HashMap<String, Object>(entryRead.getProperties());

      LOGGER.info("reset EventSynchronizer of ODATA");
      this.odataOperator.updateEntry(Utils.EVENTSYNCHRONIZERS_NAME, String.valueOf(id),
            this.propsRef);

      Utils.loggerResult(LOGGER, pass, this.getClass());

   }

}
