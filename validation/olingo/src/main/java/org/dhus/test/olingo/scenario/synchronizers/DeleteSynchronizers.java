package org.dhus.test.olingo.scenario.synchronizers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dhus.test.olingo.OlingoTestException;
import org.dhus.test.olingo.Utils;
import org.dhus.test.olingo.operations.ODataOperator;
import org.dhus.test.olingo.scenario.TestScenario;

public class DeleteSynchronizers implements TestScenario
{

   private final static Logger LOGGER = LogManager.getLogger(DeleteSynchronizers.class.getName());

   private DateFormat df = new SimpleDateFormat(Utils.DATE_FORMAT_STRING);

   private final ODataOperator odataOperator;

   private Map<String, Object> propsRef = null;

   private static final Long ID_SCANNERS_VALUE = 1L;

   public DeleteSynchronizers(ODataOperator odataOperator)
   {
      this.odataOperator = odataOperator;

      df.setTimeZone(TimeZone.getTimeZone(Utils.GMT_TIMEZONE));

      initRef();

   }

   private void initRef()
   {
      this.propsRef = new HashMap<String, Object>();
      this.propsRef.put("Id", ID_SCANNERS_VALUE);
   }

   public void execute() throws OlingoTestException, Exception
   {
      // System.out.println("CreateSynchronizers.execute()");
      //
      // for (Map.Entry<String, Object> ent : this.propsRef.entrySet()) {
      // String key = ent.getKey();
      // Object value = ent.getValue();
      // System.out.println(key + " : " + value);
      // }
   }

}