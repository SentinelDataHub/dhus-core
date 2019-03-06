package org.dhus.test.olingo.scenario.ingestion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.dhus.test.olingo.OlingoTestException;
import org.dhus.test.olingo.TestManager;
import org.dhus.test.olingo.Utils;
import org.dhus.test.olingo.operations.ODataOperator;
import org.dhus.test.olingo.scenario.FunctionalTest;
import org.dhus.test.olingo.scenario.TestScenario;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This scenario tests the OData Ingestion process
 */
@FunctionalTest
public class IngestionTestScenario implements TestScenario
{
   // entity set name
   private static final String INGESTS_ENTITYSET = "Ingests";
   // property names
   private static final String INGEST_ID = "Id";
   private static final String INGEST_STATUS = "Status";
   private static final String INGEST_FILENAME = "Filename";
   private static final String INGEST_MD5 = "MD5";
   // property values
   private static final String INGEST_STATUS_INGESTED = "INGESTED";
   
   // entity set name
   private static final String PRODUCTS_ENTITYSET = "Products";
   // property names
   private static final String PRODUCT_UUID = "Id";
   
   // entity set name
   private static final String DELETEDPRODUCTS_ENTITYSET = "DeletedProducts";

   @Autowired
   private ODataOperator odataOperator;
   
   private final String testFileDirectory = System.getProperty(TestManager.DATA_DIR);

   public void execute() throws OlingoTestException, Exception
   {
      // Recover the product
      Path productPath = Paths.get(
            testFileDirectory,
            "S1A_EW_GRDM_1SDH_20150525T143648_20150525T143700_006079_007DE9_11C5.zip");
      String productFilename = productPath.getFileName().toString();
      byte[] productData = Files.readAllBytes(productPath);

      // create Ingest entry from product data and start ingest process
      ODataEntry ingestEntry = createAndStartIngest(productFilename, productData);
      if(ingestEntry == null)
      {
         throw new OlingoTestException("Cannot create or start Ingest");
      }
      Long ingestId = (Long) ingestEntry.getProperties().get(INGEST_ID);
      TestManager.logScenarioInfo("Ingest "+ingestId+" successfully created and started");
      
      // retrieve newly created product according to its name
      String productName = productFilename.substring(0, productFilename.length() - 4);
      ODataEntry productEntry = retrieveProductEntry(productName);
      
      String productUUID = (String) productEntry.getProperties().get(PRODUCT_UUID);
      TestManager.logScenarioInfo("Product " + productName + " ingested with UUID " + productUUID);

      // delete Ingest
      if(odataOperator.deleteEntry(INGESTS_ENTITYSET, String.valueOf(ingestId)) != ODataOperator.HTTP_NO_CONTENT
            || odataOperator.readEntry(INGESTS_ENTITYSET, String.valueOf(ingestId)) != null)
      {
         throw new OlingoTestException("Failed to delete Ingest "+ingestId);
      }
      TestManager.logScenarioInfo("Deleted Ingest "+ingestId);

      // delete Product
      String formattedProductUUID = "'"+productUUID+"'";
      if(odataOperator.deleteEntry(PRODUCTS_ENTITYSET, formattedProductUUID) != ODataOperator.HTTP_NO_CONTENT
            || odataOperator.readEntry(PRODUCTS_ENTITYSET, formattedProductUUID) != null)
      {
         throw new OlingoTestException("Failed to delete Product "+productUUID);
      }
      TestManager.logScenarioInfo("Deleted Product "+productUUID);

      // check creation of delete product
      ODataEntry deletedProductEntry = odataOperator.readEntry(DELETEDPRODUCTS_ENTITYSET, formattedProductUUID);
      if (deletedProductEntry == null)
      {
         throw new OlingoTestException("DeletedProduct not found");
      }
      TestManager.logScenarioInfo("DeletedProduct "+productUUID+" generated:");

      // delete DeletedProduct
      if(odataOperator.deleteEntry(DELETEDPRODUCTS_ENTITYSET, formattedProductUUID) != ODataOperator.HTTP_NO_CONTENT
            || odataOperator.readEntry(DELETEDPRODUCTS_ENTITYSET, formattedProductUUID) != null)
      {
         throw new OlingoTestException("Failed to delete Product "+productUUID);
      }
      TestManager.logScenarioInfo("Deleted DeletedProduct "+productUUID);
   }

   private ODataEntry createAndStartIngest(String productFilename, byte[] productData)
         throws NoSuchAlgorithmException, Exception, OlingoTestException, IOException
   {
      // calculate MD5 of product data
      String clientSideMD5 = Utils.calculateMD5(productData);

      // create Ingest using product data
      ODataEntry createdIngestEntry = odataOperator.createEntryFromBinary(INGESTS_ENTITYSET, productData);
      if (createdIngestEntry == null)
      {
         throw new OlingoTestException("Ingest not created");
      }
      Long ingestId = (Long) createdIngestEntry.getProperties().get(INGEST_ID);
      TestManager.logScenarioInfo("Created Ingests Entry " + ingestId);
      
      // compare client side and server sides MD5
      Map<String, Object> ingestProperties = createdIngestEntry.getProperties();
      if (clientSideMD5.toUpperCase().equals((String) ingestProperties.get(INGEST_MD5)))
      {  
         // update filename of Ingest entry
         ingestProperties.put(INGEST_FILENAME, productFilename);
         int statusCode = odataOperator.updateEntry(
               INGESTS_ENTITYSET, String.valueOf(ingestId), ingestProperties);
         if(statusCode != 204)
         {
            throw new OlingoTestException("Ingest not updated");
         }
         TestManager.logScenarioInfo("Updated Ingests Entry " + ingestId + "with filename " + productFilename);
         
         // check if Ingest status was updated to "INGESTED"
         ODataEntry updatedEntry = odataOperator.readEntry(INGESTS_ENTITYSET, String.valueOf(ingestId));
         if (updatedEntry == null || !INGEST_STATUS_INGESTED.equals((String) updatedEntry.getProperties().get(INGEST_STATUS)))
         {
            throw new OlingoTestException("Product not ingested");
         }
         return updatedEntry;
      }
      else
      {
         TestManager.logScenarioError("Product not ingested, deleting Ingest entry " + ingestId);
         odataOperator.deleteEntry(INGESTS_ENTITYSET, ingestId.toString());
         throw new OlingoTestException("Product upload failed");
      }
   }

   private ODataEntry retrieveProductEntry(String productName)
         throws InterruptedException, IOException, ODataException, OlingoTestException
   {
      ODataEntry productEntry = null;
      
      // wait for the product to be processed
      long start = System.currentTimeMillis();
      while(productEntry == null)
      {
         // check every five second
         Thread.sleep(5000);
         productEntry = getProductByName(productName);
         
         // stop waiting after a full minute
         if(System.currentTimeMillis() - start > 300_000)
         {
            throw new OlingoTestException("Ingested Product not found");
         }
      }
      
      return productEntry;
   }

   private ODataEntry getProductByName(String productName) throws IOException, ODataException
   {
      ODataFeed feed = odataOperator.readFeed(PRODUCTS_ENTITYSET);
      if (feed == null)
      {
         return null;
      }
      
      for (ODataEntry productEntry : feed.getEntries())
      {
         if (productEntry.getProperties().get("Name").equals(productName))
         {
            return productEntry;
         }
      }
      return null;
   }

}
