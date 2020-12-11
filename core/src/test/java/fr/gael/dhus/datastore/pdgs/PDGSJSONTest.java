/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018,2019 GAEL Systems
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
package fr.gael.dhus.datastore.pdgs;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.store.datastore.async.pdgs.PDGSJob;
import org.dhus.store.datastore.async.pdgs.PDGSJob.JSONParseException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PDGSJSONTest
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final String JSON =
         "{\"status_code\": \"completed\","
         + "\"status_message\": \"demanded product is available\","
         + "\"job_id\": \"1234\","
         + "\"job_uri\": \"https://s1-pac2dmz-oda-f-99.sentinel1.eo.esa.int/jobs/1234\","
         + "\"product_name\": \"product_name.zip\","
         + "\"submission_time\" : \"2018-10-27T03:45:10Z\","
         + "\"estimated_time\" : \"2018-10-27T04:02:51Z\","
         + "\"actual_time\" : \"2018-10-27T04:04:30Z\","
         + "\"product_url\": \"http://s1-pac1dmz-oda-f-99/product_name.zip\""
         + "}";

   @Test
   public void testJSONConversion() throws ParseException, JSONParseException
   {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
      dateFormat.setTimeZone(TimeZone.getTimeZone("Z"));

      PDGSJob pdgsJob = PDGSJob.fromJSON(JSON);

      logField("StatusCode", pdgsJob.getStatusCode());
      Assert.assertEquals(pdgsJob.getStatusCode(), "completed");

      logField("StatusMessage", pdgsJob.getStatusMessage());
      Assert.assertEquals(pdgsJob.getStatusMessage(), "demanded product is available");

      logField("JobId", pdgsJob.getJobId());
      Assert.assertEquals(pdgsJob.getJobId(), "1234");

      logField("JobUri", pdgsJob.getJobUri());
      Assert.assertEquals(pdgsJob.getJobUri(), "https://s1-pac2dmz-oda-f-99.sentinel1.eo.esa.int/jobs/1234");

      logField("ProductName", pdgsJob.getProductName());
      Assert.assertEquals(pdgsJob.getProductName(), "product_name.zip");

      logField("SubmissionTime", pdgsJob.getSubmissionTime());
      Assert.assertEquals(pdgsJob.getSubmissionTime(), dateFormat.parse("2018-10-27T03:45:10Z"));

      logField("EstimatedTime", pdgsJob.getEstimatedTime());
      Assert.assertEquals(pdgsJob.getEstimatedTime(), dateFormat.parse("2018-10-27T04:02:51Z"));

      logField("ActualTime", pdgsJob.getActualTime());
      Assert.assertEquals(pdgsJob.getActualTime(), dateFormat.parse("2018-10-27T04:04:30Z"));

      logField("ProductUrl", pdgsJob.getProductUrl());
      Assert.assertEquals(pdgsJob.getProductUrl(), "http://s1-pac1dmz-oda-f-99/product_name.zip");
   }

   private void logField(String name, Object value)
   {
      LOGGER.info("{} ({}): {}", name, value.getClass(), value);
   }

}
