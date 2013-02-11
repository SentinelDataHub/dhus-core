package org.dhus.test.olingo.scenario.collections;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.dhus.test.olingo.OlingoTestException;
import org.dhus.test.olingo.operations.ODataOperator;
import org.dhus.test.olingo.scenario.TestScenario;

public class ReadCollections implements TestScenario
{
   private final static Logger LOGGER = LogManager.getLogger(ReadCollections.class.getName());

   private final ODataOperator odataOperator;

   public ReadCollections(ODataOperator odataOperator)
   {

      this.odataOperator = odataOperator;
   }

   public void execute() throws OlingoTestException, Exception
   {

      Map<String, Object> data = new HashMap<String, Object>();

      data.put("Name", "Collection2 ");
      data.put("Description", "Ceci est une collection 2");

      ODataEntry entry = this.odataOperator.readEntry("Collections", "'Collection2'");

      if (entry == null)
      {

         LOGGER.error("Collection not read ");
         throw new OlingoTestException("Collection not read");
      }
      LOGGER.info("Collection Entry:\n" + odataOperator.prettyPrint(entry.getProperties()));

      Map<String, Object> propertiesOData = entry.getProperties();
      if (propertiesOData == null)
      {

         LOGGER.error(" the properties are null ");
         throw new OlingoTestException("Collection properties not read");
      }

      for (Map.Entry<String, Object> CollectionMap : propertiesOData.entrySet())
      {
         if (CollectionMap.getKey().equals("Name")
               && !CollectionMap.getValue().equals("Collection2"))
         {
            LOGGER.error("The Name value is not correct");
         }
         if (CollectionMap.getKey().equals("Description")
               && !CollectionMap.getValue().equals("Ceci est une collection 2"))
         {

            LOGGER.error("The Description value is not correct");
         }
      }
   }
}
