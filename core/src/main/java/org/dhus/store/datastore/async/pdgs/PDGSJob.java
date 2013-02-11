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
package org.dhus.store.datastore.async.pdgs;

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Holds all the information related to a specific job on the PDGS.
 */
public class PDGSJob
{
   private static final Logger LOGGER = LogManager.getLogger();

   private String status_code;
   private String status_message;
   private String job_id;
   private String job_uri;
   private String product_name;
   private Date submission_time;
   private Date estimated_time;
   private Date actual_time;
   private String product_url;

   public static PDGSJob fromJSON(String json) throws JSONParseException
   {
      LOGGER.debug("Parsing Json document: {}", json);

      try
      {
         Gson gson = new Gson();
         return gson.fromJson(json, PDGSJob.class);
      }
      catch (RuntimeException e)
      {
         throw new JSONParseException(json, e);
      }
   }

   public static List<PDGSJob> listfromJSON(String json) throws JSONParseException
   {
      try
      {
         Gson gson = new Gson();
         PDGSJob[] pdgsJob = gson.fromJson(json, PDGSJob[].class);
         return Arrays.asList(pdgsJob);
      }
      catch (RuntimeException e)
      {
         throw new JSONParseException(json, e);
      }
   }

   public String getStatusCode()
   {
      return status_code;
   }

   public String getStatusMessage()
   {
      return status_message;
   }

   public String getJobId()
   {
      return job_id;
   }

   public String getJobUri()
   {
      return job_uri;
   }

   public String getProductName()
   {
      return product_name;
   }

   public Date getSubmissionTime()
   {
      return submission_time;
   }

   public Date getEstimatedTime()
   {
      return estimated_time;
   }

   public Date getActualTime()
   {
      return actual_time;
   }

   public String getProductUrl()
   {
      return product_url;
   }

   public static class JSONParseException extends Exception
   {
      private static final long serialVersionUID = 1L;

      public JSONParseException(String json, Exception e)
      {
         super("Could not parse JSON document: " + json, e);
      }
   }
}
