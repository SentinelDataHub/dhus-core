/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2020 GAEL Systems
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
package org.dhus.store.datastore.async.lta;

import java.util.Date;

import org.apache.olingo.client.api.domain.ClientEntity;

import org.dhus.store.datastore.async.AbstractHttpJob;

public class LTAJob extends AbstractHttpJob
{
   private final Date submissionDate;
   private final Date estimatedDate;

   public static final String PROPERTY_SUBMISSION_DATE = "SubmissionDate";
   public static final String PROPERTY_ESTIMATED_DATE = "EstimatedDate";

   public LTAJob(ClientEntity orderEntity, String productUuid, String productName)
   {
      super(orderEntity, productUuid, productName);
      this.submissionDate = parseDate(getPropertyValue(orderEntity, PROPERTY_SUBMISSION_DATE).toString());
      this.estimatedDate = parseDate(getPropertyValue(orderEntity, PROPERTY_ESTIMATED_DATE).toString());
   }

   public Date getSubmissionDate()
   {
      return submissionDate;
   }

   public Date getEstimatedDate()
   {
      return estimatedDate;
   }
}