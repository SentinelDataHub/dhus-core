/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
 *
 * This file is part of DHuS software sources.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.dhus.test.olingo.scenario;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.dhus.test.olingo.OlingoTestException;
import org.dhus.test.olingo.TestManager;
import org.dhus.test.olingo.Utils;
import org.dhus.test.olingo.operations.ODataOperator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
@FunctionalTest
public class ScannersCRUDScenario implements TestScenario
{
   public static final String ENTITY_SET = "Scanners";
   
   // properties
   public static final String ID = "Id";
   public static final String URL = "Url";
   public static final String STATUS = "Status";
   public static final String STATUSMESSAGE = "StatusMessage";
   public static final String ACTIVE = "Active";
   public static final String USERNAME = "Username";
   public static final String PASSWORD = "Password";
   public static final String PATTERN = "Pattern";
   
   // property values
   public static final Long ID_VALUE = 0L;
   public static final String URL_VALUE = "/path/to/scan";
   public static final String STATUS_VALUE = "created";
   public static final String STATUSMESSAGE_VALUE = "Create worked!";
   public static final Boolean ACTIVE_VALUE = false;
   public static final String USERNAME_VALUE = "user";
   public static final String PASSWORD_VALUE = "password";
   public static final String PATTERN_VALUE = "pattern";
   
   // alternate property values
   // no alternate value for scanner id
   public static final String URL_ALT_VALUE = "/other/path/to/scan";
   public static final String STATUS_ALT_VALUE = "updated";
   public static final String STATUSMESSAGE_ALT_VALUE = "Update worked!";
   // do not set active to true to avoid accidental ingestions or errors
   public static final String USERNAME_ALT_VALUE = "ElPonchoGrande";
   public static final String PASSWORD_ALT_VALUE = "Uncr@ck@bl€P@$$w0rd";
   public static final String PATTERN_ALT_VALUE = "otherPattern";
   
   @Autowired
   private ODataOperator odataOperator;
   
   private final Map<String, Object> referenceProperties;
   private final Map<String, Object> alternateProperties;
   
   public ScannersCRUDScenario()
   {
      this.referenceProperties = Utils.makeEntryMap(listReferenceKeys(), listReferenceValues());
      this.alternateProperties = Utils.makeEntryMap(listReferenceKeys(), listAlternateValues());
   }
   
   @Override
   public void execute() throws OlingoTestException, Exception
   {
      createScanner();
      readScanner();
      readScanners();
      updateScanner();
      deleteScanner();
   }

   private void createScanner() throws Exception, OlingoTestException
   {
      ODataEntry createdScanner = odataOperator.createEntry(ENTITY_SET, referenceProperties);
      
      if(createdScanner == null)
      {
         throw new OlingoTestException("Failed to create Scanner entity");
      }
      TestManager.logScenarioInfo("Scanner entity created.");
      
      // update references with generated ID
      Long scannerId = (Long) createdScanner.getProperties().get(ID);
      referenceProperties.put(ID, scannerId);
      alternateProperties.put(ID, scannerId);
      
      if(!Utils.validatePropertiesExcept(
            referenceProperties, createdScanner.getProperties (), getIgnoredProperties ()))
      {
         throw new OlingoTestException("Invalid created Scanner entity");
      }
   }

   private Set<String> getIgnoredProperties ()
   {  
      Set<String> ignored = new HashSet <String> ();
      ignored.add (STATUS);
      ignored.add (STATUSMESSAGE);
      ignored.add (PASSWORD);
      return ignored;
   }
   
   private void readScanners() throws IOException, ODataException, OlingoTestException
   {
      ODataFeed scannersFeed = odataOperator.readFeed(ENTITY_SET);
      if(scannersFeed == null || scannersFeed.getEntries().isEmpty())
      {
         throw new OlingoTestException("Failed to read Scanners entity collection");
      }
      TestManager.logScenarioInfo("Scanners entity collection read.");
   }
   
   private void readScanner() throws IOException, ODataException, OlingoTestException
   {
      // get ID retrieved at creation
      Long scannerId = (Long) referenceProperties.get(ID);
      
      // read scanner entry
      ODataEntry scannerEntry = odataOperator.readEntry(ENTITY_SET, String.valueOf(scannerId));
      
      if (scannerEntry == null)
      {
         throw new OlingoTestException("Failed to read Scanner entity.");
      }
      TestManager.logScenarioInfo("Scanner entity read.");
      
      // check property values
      if(!Utils.validatePropertiesExcept(
            referenceProperties, scannerEntry.getProperties(), getIgnoredProperties ()))
      {
         throw new OlingoTestException("Invalid read Scanner entity.");
      }
   }
   
   private void updateScanner() throws Exception
   {
      // get ID retrieved at creation
      Long scannerId = (Long) alternateProperties.get(ID);
      
      // update scanner with alternate properties
      int statusCode = odataOperator.updateEntry(ENTITY_SET, String.valueOf(scannerId), alternateProperties);
      
      if (statusCode != ODataOperator.HTTP_NO_CONTENT)
      {
         throw new OlingoTestException("Failed to update Scanner entity.");
      }
      TestManager.logScenarioInfo("Scanner entity updated.");
      
      // check updated property values
      ODataEntry updatedScannerEntry = odataOperator.readEntry(ENTITY_SET, String.valueOf(scannerId));
      if(updatedScannerEntry == null || !Utils.validatePropertiesExcept(
            alternateProperties, updatedScannerEntry.getProperties(), getIgnoredProperties ()))
      {
         throw new OlingoTestException("Invalid updated Scanner entity.");
      }
   }

   private void deleteScanner() throws IOException, OlingoTestException, ODataException
   {
      // get ID retrieved at creation
      Long scannerId = (Long) referenceProperties.get(ID);
      
      // delete scanner
      int statusCode = odataOperator.deleteEntry(ENTITY_SET, String.valueOf(scannerId));
      
      // check deletion success
      if(statusCode != ODataOperator.HTTP_NO_CONTENT 
            || odataOperator.readEntry(ENTITY_SET, String.valueOf(scannerId)) != null)
      {
         throw new OlingoTestException("Failed to delete Scanner entity.");
      }
      TestManager.logScenarioInfo("Scanner entity deleted.");
   }
   
   private static List<String> listReferenceKeys()
   {
      return Arrays.asList(ID, URL, STATUS, STATUSMESSAGE, ACTIVE, USERNAME, PASSWORD, PATTERN);
   }
   
   private static List<Object> listReferenceValues()
   {
      return Arrays.<Object>asList(
            ID_VALUE,
            URL_VALUE, 
            STATUS_VALUE, 
            STATUSMESSAGE_VALUE, 
            ACTIVE_VALUE, 
            USERNAME_VALUE, 
            PASSWORD_VALUE, 
            PATTERN_VALUE);
   }
   
   private static List<Object> listAlternateValues()
   {
      return Arrays.<Object>asList(
            ID_VALUE,
            URL_ALT_VALUE, 
            STATUS_ALT_VALUE, 
            STATUSMESSAGE_ALT_VALUE, 
            ACTIVE_VALUE, 
            USERNAME_ALT_VALUE, 
            PASSWORD_ALT_VALUE, 
            PATTERN_ALT_VALUE);
   }
}
