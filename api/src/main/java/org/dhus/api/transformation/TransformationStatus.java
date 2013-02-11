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
package org.dhus.api.transformation;

import java.net.URL;

import org.dhus.api.JobStatus;

/**
 * Represents the status of a Transformation.
 * Contains transformer data and a URL to its result if the Transformation is completed.
 */
public class TransformationStatus
{
   private final JobStatus jobStatus;
   private final URL resultUrl;
   private final String data;

   /**
    * Constructor.
    * @param jobStatus status of the transformation
    * @param resultURL url of the result if completed
    * @param data custom data needed by the transformer
    */
   public TransformationStatus(JobStatus jobStatus, URL resultURL, String data)
   {
      this.jobStatus = jobStatus;
      this.resultUrl = resultURL;
      this.data = data;
   }

   public JobStatus toJobStatus()
   {
      return jobStatus;
   }

   public URL getResult()
   {
      return resultUrl;
   }

   public String getData()
   {
      return data;
   }
}
