package org.dhus.test.olingo.scenario.collections;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.dhus.test.olingo.OlingoTestException;
import org.dhus.test.olingo.operations.ODataOperator;
import org.dhus.test.olingo.scenario.TestScenario;

public class UpdateCollections implements TestScenario
{

   private final static Logger LOGGER = LogManager.getLogger(UpdateCollections.class.getName());

   private final ODataOperator odataOperator;

   public UpdateCollections(ODataOperator odataOperator)
   {

      this.odataOperator = odataOperator;
   }

   public void execute() throws OlingoTestException, Exception
   {
      ODataEntry entry = this.odataOperator.readEntry("Collections", "'Collection2'");

      if (entry == null)
      {
         LOGGER.error("Collection not read ");
         throw new OlingoTestException("Collection not read");
      }
      LOGGER.info("Collection Entry:\n" + odataOperator.prettyPrint(entry.getProperties()));

      Map<String, Object> propertiesOData = entry.getProperties();
      propertiesOData.put("Description", "Ceci est une collection 2 modifiée ");

      odataOperator.updateEntry("Collections", "'Collection2'", propertiesOData);
      ODataEntry updatedEntry = odataOperator.readEntry("Collections", "'Collection2'");
      LOGGER.info("Updated Collections Entry:\n"
            + odataOperator.prettyPrint(updatedEntry.getProperties()));
   }

}
