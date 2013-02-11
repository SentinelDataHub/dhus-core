package org.dhus.test.olingo.scenario;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.dhus.test.olingo.OlingoTestException;
import org.dhus.test.olingo.TestManager;
import org.dhus.test.olingo.Utils;
import org.dhus.test.olingo.operations.ODataOperator;
import org.springframework.beans.factory.annotation.Autowired;

@FunctionalTest
public class ClassesScenario implements TestScenario
{
   private static final String ENTITY_SET = "Classes";
   private static final String ID_PROPERTY = "Id";

   @Autowired
   private ODataOperator operator;

   private Map<String, Object> properties;
   private String entityKey;

   @Override
   public void execute() throws OlingoTestException, Exception
   {
      testReadEntitySet();
      testReadSimpleEntity();
   }

   private void testReadEntitySet() throws OlingoTestException
   {
      try
      {
         ODataFeed feed = operator.readFeed(ENTITY_SET);
         if (feed == null)
         {
            throw new OlingoTestException(
                  "Failed to read entity set: " + ENTITY_SET);
         }
         List<ODataEntry> entries = feed.getEntries();
         if (entries == null || entries.isEmpty())
         {
            throw new OlingoTestException("Empty entity set: " + ENTITY_SET);
         }

         properties = entries.get(0).getProperties();
         entityKey = (String) properties.get(ID_PROPERTY);
      }
      catch (IOException | ODataException e)
      {
         throw new OlingoTestException(
               "Failed to read entity set " + ENTITY_SET, e);
      }
   }

   private void testReadSimpleEntity() throws OlingoTestException
   {
      String key = operator.toODataParameter(entityKey);
      TestManager.logScenarioInfo("Read entity " + key + " from entity set '" + ENTITY_SET + "'");
      try
      {
         ODataEntry entry = operator.readEntry(ENTITY_SET, key);
         if (!Utils.validateProperties(properties, entry.getProperties()))
         {
            throw new OlingoTestException("Invalid read entity");
         }
      }
      catch (IOException | ODataException e)
      {
         throw new OlingoTestException("Failed to read entity: " + ENTITY_SET);
      }
   }
}
