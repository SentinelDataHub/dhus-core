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
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.service.exception.ProductNotAddedException;
import fr.gael.dhus.service.exception.RootNotModifiableException;
import fr.gael.dhus.service.exception.UploadingException;
import fr.gael.dhus.system.init.WorkingDirectory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FilenameUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ProductUploadService extends WebService
{
   private static final Logger LOGGER = LogManager.getLogger(ProductUploadService.class);

   @Autowired
   private SecurityService securityService;
   
   @Autowired
   private CollectionService collectionService;
   
   @Autowired
   private UploadService uploadService;

   @PreAuthorize("hasRole('ROLE_UPLOAD')")
   public void upload (FileItem product,
         ArrayList<String> collection_uuids) throws UploadingException,
         RootNotModifiableException, ProductNotAddedException
   {
      User owner = securityService.getCurrentUser ();
      ArrayList<Collection> collections = new ArrayList<> ();
      for (String uuid : collection_uuids)
      {
         Collection c;
         c = collectionService.getCollection(uuid);
         collections.add (c);
      }

      String fileName = product.getName ();
      try
      {
         LOGGER.info("{} tries to upload product '{}'", owner.getUsername(), fileName);
         if (fileName != null)
         {
            fileName = FilenameUtils.getName (fileName);
         }
         File uploadedFile = new File(WorkingDirectory.getTempDirectoryFile(), fileName);
         if (uploadedFile.createNewFile ())
         {
            product.write (uploadedFile);
         }
         else
         {
            throw new IOException ("The file already exists in repository.");
         }
         uploadService.addProduct(uploadedFile.toURI().toURL(), collections);
      }
      catch (ProductNotAddedException e)
      {
         throw e;
      }
      catch (Exception e)
      {
         throw new UploadingException ("An error occured when uploading '" +
            fileName + "'", e);
      }
   }
}
