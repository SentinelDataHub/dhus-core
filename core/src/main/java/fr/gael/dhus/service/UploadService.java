/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2016,2017 GAEL Systems
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
package fr.gael.dhus.service;

import fr.gael.dhus.database.object.Collection;
import fr.gael.dhus.service.job.JobScheduler;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.dhus.store.ingestion.IngestibleRawProduct;
import org.dhus.store.ingestion.ProcessingManager;

import org.quartz.SchedulerException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class UploadService extends WebService
{
   @Autowired
   private JobScheduler scheduler;

   // TODO replace List<Collection> with List<String>
   @PreAuthorize ("hasRole('ROLE_UPLOAD')")
   public boolean addProduct (URL path, final List<Collection> collections)
   {
      List<String> collectionNames = new ArrayList<>(collections.size());
      collections.forEach((collection) -> collectionNames.add(collection.getName()));
      ProcessingManager.processProduct(new IngestibleRawProduct(path), collectionNames, false);
      return true;
   }

   @PreAuthorize ("hasRole('ROLE_UPLOAD')")
   public Date getNextScheduleFileScanner() throws SchedulerException
   {
      return scheduler.getNextFileScannerJobSchedule ();
   }
}
