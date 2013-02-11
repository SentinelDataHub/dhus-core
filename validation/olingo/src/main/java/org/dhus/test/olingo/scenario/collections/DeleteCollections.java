package org.dhus.test.olingo.scenario.collections;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.dhus.test.olingo.OlingoTestException;
import org.dhus.test.olingo.Utils;
import org.dhus.test.olingo.operations.ODataOperator;
import org.dhus.test.olingo.scenario.TestScenario;

public class DeleteCollections implements TestScenario
{

   private final static Logger LOGGER = LogManager.getLogger(DeleteCollections.class.getName());

   private final ODataOperator odataOperator;

   public DeleteCollections(ODataOperator odataOperator)
   {

      this.odataOperator = odataOperator;
   }

   public void execute() throws OlingoTestException, Exception
   {

      boolean pass = true;
      Map<String, Object> data = new HashMap<String, Object>();
      data.put("Id", "'Collection3'");
      data.put("Name", "Collection3");
      data.put("Description", "OK ");

      ODataEntry createdCollectionEntry = odataOperator.createEntry("Collections", data);

      if (createdCollectionEntry == null)
      {
         throw new OlingoTestException("Collection not created");
      }
      LOGGER.info("Created Collection Entry:\n"
            + odataOperator.prettyPrint(createdCollectionEntry.getProperties()));

      String id = "Collection3";
      String formattedCollectionId = "\'" + id + "\'";

      int statusCode = odataOperator.deleteEntry("Collections", formattedCollectionId);
      LOGGER.info("Deletion of Entry successful: " + statusCode);

      Utils.loggerResult(LOGGER, pass, this.getClass());
   }

}
