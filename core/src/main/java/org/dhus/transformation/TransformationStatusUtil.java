/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019,2020 GAEL Systems
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
package org.dhus.transformation;

import org.dhus.api.JobStatus;

public class TransformationStatusUtil
{
   public static final String FAILED = "FAILED";
   public static final String INGESTED = "INGESTED";
   public static final String PAUSED = "PAUSED";
   public static final String INGESTING = "INGESTING";
   public static final String COMPLETED = "COMPLETED";
   public static final String RUNNING = "RUNNING";
   public static final String PENDING = "PENDING";

   public static JobStatus fromString(String status)
   {
      status = status.toUpperCase();
      switch (status)
      {
         case PENDING:
            return JobStatus.PENDING;
         case RUNNING:
         case COMPLETED:
         case INGESTING:
            return JobStatus.RUNNING;
         case PAUSED:
            return JobStatus.PAUSED;
         case INGESTED:
            return JobStatus.COMPLETED;
         case FAILED:
            return JobStatus.FAILED;
         default:
            return JobStatus.UNKNOWN;
      }
   }

   public static String fromJobStatus(JobStatus jobStatus)
   {
      return fromJobStatusString(jobStatus.toString());
   }

   public static String fromJobStatusString(String jobStatus)
   {
      jobStatus = jobStatus.toUpperCase();
      if (JobStatus.PENDING.toString().equals(jobStatus))
      {
         return "'" + PENDING + "'";
      }
      else if (JobStatus.RUNNING.toString().equals(jobStatus))
      {
         return "'" + RUNNING + "','" + COMPLETED + "','" + INGESTING + "'";
      }
      else if (JobStatus.PAUSED.toString().equals(jobStatus))
      {
         return "'" + PAUSED + "'";
      }
      else if (JobStatus.COMPLETED.toString().equals(jobStatus))
      {
         return "'" + INGESTED + "'";
      }
      else if (JobStatus.FAILED.toString().equals(jobStatus))
      {
         return "'" + FAILED + "'";
      }
      return "";
   }
}
