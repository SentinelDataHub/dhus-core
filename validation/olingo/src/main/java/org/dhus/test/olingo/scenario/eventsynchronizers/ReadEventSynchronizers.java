package org.dhus.test.olingo.scenario.eventsynchronizers;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.dhus.test.olingo.OlingoTestException;
import org.dhus.test.olingo.Utils;
import org.dhus.test.olingo.operations.ODataOperator;
import org.dhus.test.olingo.scenario.TestScenario;

public class ReadEventSynchronizers implements TestScenario
{

   private final static Logger LOGGER = LogManager.getLogger(ReadEventSynchronizers.class.getName());

   private final ODataOperator odataOperator;

   private Map<String, Object> propsRef = null;

   /* PROPERTIES */
   private static final String ID = "Id";
   private static final String LABEL = "Label";
   private static final String CREATIONDATE = "CreationDate";
   private static final String MODIFICATIONDATE = "ModificationDate";
   private static final String REQUEST = "Request";
   private static final String SCHEDULE = "Schedule";
   private static final String STATUSDATE = "StatusDate";
   private static final String SERVICEURL = "ServiceUrl";
   private static final String SERVICELOGIN = "ServiceLogin";
   private static final String SERVICEPASSWORD = "ServicePassword";
   private static final String FILTERPARAM = "FilterParam";

   /* VALUES OF PROPERTIES */
   private static final String LABEL_EVENT_SYNC_VALUE = "Events synchronizer 1";
   private static final String REQUEST_EVENT_SYNC_VALUE = "start";
   private static final String SCHEDULE_EVENT_SYNC_VALUE = "0 0/1 * * * ?";
   private static final String SERVICEURL_EVENT_SYNC_VALUE = "http://localhost:8081/odata/v1/Events";
   private static final String SERVICELOGIN_EVENT_SYNC_VALUE = "root";
   private static final String FILTERPARAM_EVENT_SYNC_VALUE =
      "PublicationDate gt datetime'2017-04-25T00:00:00'";

   public ReadEventSynchronizers(ODataOperator odataOperator)
   {
      this.odataOperator = odataOperator;

      propsRef = new HashMap<String, Object>();
      propsRef.put(LABEL, LABEL_EVENT_SYNC_VALUE);
      propsRef.put(SCHEDULE, SCHEDULE_EVENT_SYNC_VALUE);
      propsRef.put(REQUEST, REQUEST_EVENT_SYNC_VALUE);
      propsRef.put(SERVICEURL, SERVICEURL_EVENT_SYNC_VALUE);
      propsRef.put(SERVICELOGIN, SERVICELOGIN_EVENT_SYNC_VALUE);
      propsRef.put(FILTERPARAM, FILTERPARAM_EVENT_SYNC_VALUE);

   }

   public void execute() throws OlingoTestException, Exception
   {
      Long id = 0L;
      boolean pass = true;

      ODataEntry entry =
         this.odataOperator.readEntry(Utils.EVENTSYNCHRONIZERS_NAME, String.valueOf(id));

      if (entry == null)
      {
         pass = false;
         LOGGER.error("EventSynchronizer no read ");
         throw new OlingoTestException("EventSynchronizer not read");
      }
      else
      {
         LOGGER.info("EventSynchronizer read ");
      }

      Map<String, Object> propsOdata = entry.getProperties();

      if (propsOdata == null)
      {
         pass = false;
         LOGGER.error("No properties read");
         throw new OlingoTestException("No properties read");
      }
      else
      {
         LOGGER.error("properties read");
      }

      for (Map.Entry<String, Object> ent : this.propsRef.entrySet())
      {
         String keyRef = ent.getKey();
         Object valRef = ent.getValue();

         if (!keyRef.equals(ID) && !keyRef.equals(CREATIONDATE) && !keyRef.equals(MODIFICATIONDATE)
               && !keyRef.equals(STATUSDATE) && !keyRef.equals(SERVICEPASSWORD))
         {

            if (propsOdata.containsKey(keyRef))
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

      Utils.loggerResult(LOGGER, pass, this.getClass());

   }

}