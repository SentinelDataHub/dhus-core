package org.dhus.test.olingo.scenario.eventsynchronizers;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

public class DeleteEventSynchronizers implements TestScenario
{

   private final static Logger LOGGER =
      LogManager.getLogger(DeleteEventSynchronizers.class.getName());
   private DateFormat df = new SimpleDateFormat(Utils.DATE_FORMAT_STRING);

   private final ODataOperator odataOperator;
   private int statusCode2 = -1;

   private Map<String, Object> propsRef = null;

   /* PROPERTIES */
   private static final String ID = "Id";
   private static final String LABEL = "Label";
   private static final String CREATIONDATE = "CreationDate";
   private static final String MODIFICATIONDATE = "ModificationDate";
   private static final String REQUEST = "Request";
   private static final String SCHEDULE = "Schedule";
   private static final String STATUS = "Status";
   private static final String STATUSDATE = "StatusDate";
   private static final String STATUSMESSAGE = "StatusMessage";
   private static final String SERVICEURL = "ServiceUrl";
   private static final String SERVICELOGIN = "ServiceLogin";
   private static final String SERVICEPASSWORD = "ServicePassword";
   private static final String FILTERPARAM = "FilterParam";

   /* VALUES OF PROPERTIES */
   private static final Long ID_EVENT_SYNC_VALUE = 1L;
   private static final String LABEL_EVENT_SYNC_VALUE = "Events synchronizer 1";
   private static final String REQUEST_EVENT_SYNC_VALUE = "start";
   private static final String SCHEDULE_EVENT_SYNC_VALUE = "0 0/1 * * * ?";
   private static final String STATUS_EVENT_SYNC_VALUE = "STOPPED";
   private static final String SERVICEURL_EVENT_SYNC_VALUE = "http://localhost:8081/odata/v1/Events";
   private static final String SERVICELOGIN_EVENT_SYNC_VALUE = "root";
   private static final String SERVICEPASSWORD_EVENT_SYNC_VALUE = "password";
   private static final String FILTERPARAM_EVENT_SYNC_VALUE =
      "PublicationDate gt datetime'2017-04-25T00:00:00'";

   public DeleteEventSynchronizers(ODataOperator odataOperator)
   {
      this.odataOperator = odataOperator;

      df.setTimeZone(TimeZone.getTimeZone(Utils.GMT_TIMEZONE));

      try
      {
         initRef();
      }
      catch (ParseException e)
      {
         e.printStackTrace();
      }

   }

   private void initRef() throws ParseException
   {

      this.propsRef = new HashMap<String, Object>();
      this.propsRef.put(ID, ID_EVENT_SYNC_VALUE);
      this.propsRef.put(LABEL, LABEL_EVENT_SYNC_VALUE);
      this.propsRef.put(CREATIONDATE, new Date());
      this.propsRef.put(MODIFICATIONDATE, new Date());
      this.propsRef.put(REQUEST, REQUEST_EVENT_SYNC_VALUE);
      this.propsRef.put(SCHEDULE, SCHEDULE_EVENT_SYNC_VALUE);
      this.propsRef.put(STATUS, STATUS_EVENT_SYNC_VALUE);
      this.propsRef.put(STATUSDATE, null);
      this.propsRef.put(STATUSMESSAGE, null);
      this.propsRef.put(SERVICEURL, SERVICEURL_EVENT_SYNC_VALUE);
      this.propsRef.put(SERVICELOGIN, SERVICELOGIN_EVENT_SYNC_VALUE);
      this.propsRef.put(SERVICEPASSWORD, SERVICEPASSWORD_EVENT_SYNC_VALUE);
      this.propsRef.put(FILTERPARAM, FILTERPARAM_EVENT_SYNC_VALUE);

   }

   public void execute() throws OlingoTestException, Exception
   {
      Long id = 0L;
      ODataEntry entryCreate =
         this.odataOperator.createEntry(Utils.EVENTSYNCHRONIZERS_NAME, this.propsRef);

      if (entryCreate == null)
      {
         LOGGER.error("EventSynchronizer not created");
         throw new OlingoTestException("EventSynchronizer not created");
      }

      id = (Long) entryCreate.getProperties().get(ID);

      try
      {
         statusCode2 =
            this.odataOperator.deleteEntry(Utils.EVENTSYNCHRONIZERS_NAME, String.valueOf(id));
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
   }

}
