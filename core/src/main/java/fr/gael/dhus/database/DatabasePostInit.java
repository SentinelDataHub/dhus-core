/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013-2018 GAEL Systems
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
package fr.gael.dhus.database;

import fr.gael.dhus.database.dao.CollectionDao;
import fr.gael.dhus.database.dao.CountryDao;
import fr.gael.dhus.database.dao.NetworkUsageDao;
import fr.gael.dhus.database.dao.ProductCartDao;
import fr.gael.dhus.database.dao.ProductDao;
import fr.gael.dhus.database.dao.SearchDao;
import fr.gael.dhus.database.dao.UserDao;
import fr.gael.dhus.database.dao.interfaces.DaoUtils;
import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.database.object.config.system.AdministratorConfiguration;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.service.SearchService;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.solr.client.solrj.SolrQuery;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Initialization to be executed of database when all the service started
 */
@Component
public class DatabasePostInit implements InitializingBean
{
   private static final Logger LOGGER = LogManager.getLogger(DatabasePostInit.class);

   @Autowired
   private CountryDao countryDao;

   @Autowired
   private ProductService productService;

   @Autowired
   private ProductDao productDao;

   @Autowired
   private CollectionDao collectionDao;

   @Autowired
   private SearchService searchService;

   @Autowired
   private UserDao userDao;

   @Autowired
   private NetworkUsageDao networkUsageDao;

   @Autowired
   private SearchDao searchDao;

   @Autowired
   private ProductCartDao cartDao;

   @Autowired
   private ConfigurationManager cfgManager;


   public void init()
   {
      initDefaultArchiveSettings();
   }

   /* Creates initial object in databases */
   @Override
   public void afterPropertiesSet() throws Exception
   {
      // Because of the EmailToUserListener that want to send an email to the newly created user
      // These accounts have to be created during context initialization.

      // Root account
      AdministratorConfiguration cfg = cfgManager.getAdministratorConfiguration();
      User rootUser = userDao.getByName(cfg.getName());
      if (rootUser != null)
      {
         // If root User exists, update his roles by security
         ArrayList<Role> roles = new ArrayList<>();
         roles.addAll(Arrays.asList(Role.values()));
         rootUser.setRoles(roles);
         userDao.update(rootUser);
      }
      else
      {
         // Create it
         rootUser = new User();
         rootUser.setUsername(cfg.getName());
         rootUser.setPassword(cfg.getPassword());
         rootUser.setCreated(new Date());
         ArrayList<Role> roles = new ArrayList<>();
         roles.addAll(Arrays.asList(Role.values()));
         rootUser.setRoles(roles);
         rootUser.setDomain("Other");
         rootUser.setSubDomain("System");
         rootUser.setUsage("Other");
         rootUser.setSubUsage("System");
         userDao.create(rootUser);
      }
   }

   /**
    * Initializes archive settings: at this step, we consider only one archive
    * is configured into the system. If an archive is already present in the
    * database, methods checks if it is present. If configured archive not
    * available, it is removed and a new archive is created. If the archive path
    * was changed, is is upgraded accordingly.
    */
   private void initDefaultArchiveSettings()
   {
      // Update User table with countries synonyms
      updateUserCountries();
      // Displays database raws statistics
      printDatabaseRowCounts();

      // Check the archive on user request
      doArchiveCheck();

      // delete old search queries
      inactiveOldSearchQueries();

      doReindex();
   }

   private void printDatabaseRowCounts()
   {
      LOGGER.info("Database tables rows :");
      LOGGER.info("  Products       = " + productDao.count()      + " rows.");
      LOGGER.info("  Collections    = " + collectionDao.count()   + " rows.");
      LOGGER.info("  Users          = " + userDao.count()         + " rows.");
      LOGGER.info("  Network Usage  = " + networkUsageDao.count() + " rows.");
      LOGGER.info("  Saved searches = " + searchDao.count()       + " rows.");
      LOGGER.info("  User carts     = " + cartDao.count()         + " rows.");
   }

   private void doArchiveCheck()
   {
      boolean force_check = Boolean.getBoolean("Archive.check");

      LOGGER.info("Archives check (Archive.check) requested by user (" +force_check + ")");
      if (!force_check)
      {
         return;
      }
      // It's too dangerous to process products while performing actions on the index!
      boolean reindex = Boolean.getBoolean("dhus.solr.reindex");
      if (reindex)
      {
         LOGGER.error("Cannot do ArchiveCheck because reindex is required");
         return;
      }

      try
      {
         long time_start = System.currentTimeMillis ();
         LOGGER.info("Control of Indexes coherence...");
         long start = new Date().getTime();
         searchService.checkIndex();
         LOGGER.info("Control of Indexes coherence spent "
               + (new Date().getTime() - start) + " ms");

         LOGGER.info("Optimizing database...");
         DaoUtils.optimize();

         LOGGER.info("SCHEDULER : Check system consistency done - " +
                  (System.currentTimeMillis ()-time_start) + "ms");
      }
      catch (Exception e)
      {
         LOGGER.error("Cannot check DHus Archive.", e);
      }
   }

   private void updateUserCountries()
   {
      String synonymsFile = System.getProperty("country.synonyms");
      if (synonymsFile == null)
      {
         // TODO test it
         return;
      }
      LOGGER.info("Loading country synonyms from '" + synonymsFile + "'");
      List<String> countriesNames = countryDao.readAllNames();
      HashMap<String, List<String>> synonyms = new HashMap<>();

      try (BufferedReader br =
               new BufferedReader(
                     new InputStreamReader(
                           new FileInputStream(synonymsFile), "UTF-8")))
      {
         String sCurrentLine;
         while ((sCurrentLine = br.readLine()) != null)
         {
            if (sCurrentLine.startsWith("#"))
            {
               // comments
               continue;
            }
            String[] split1 = sCurrentLine.split(": ");
            if (split1.length > 1)
            {
               String[] split2 = split1[1].split(", ");
               List<String> syns = new ArrayList<>();
               for (String s: split2)
               {
                  syns.add(s.toLowerCase());
               }
               if (countriesNames.contains(split1[0]))
               {
                  synonyms.put(split1[0], syns);
               }
            }
         }
      }
      catch (FileNotFoundException e)
      {
         LOGGER.error("Can not load country synonyms");
         return;
      }
      catch (IOException e)
      {
         LOGGER.error("Can not load country synonyms");
         return;
      }

      Iterator<User> users = userDao.getAllUsers();
      while (users.hasNext())
      {
         User u = users.next();
         if (cfgManager.getAdministratorConfiguration().getName().equals(u.getUsername()))
         {
            continue;
         }
         if (!countriesNames.contains(u.getCountry()))
         {
            boolean found = false;
            for (String country: synonyms.keySet())
            {
               if (synonyms.get(country).contains(u.getCountry().toLowerCase()))
               {
                  u.setCountry(country);
                  userDao.update(u);
                  found = true;
                  break;
               }
            }
            if (!found)
            {
               LOGGER.warn("Unknown country for '" + u.getUsername() + "' : " + u.getCountry());
            }
         }
      }
   }

   private void inactiveOldSearchQueries()
   {
      boolean deactivate_notif = Boolean.getBoolean("users.search.notification.force.inactive");
      LOGGER.info("Deactivate all saved search notifications (users.search.notification.force.inactive)"
            + "requested by user (" + deactivate_notif + ")");

      if (deactivate_notif)
      {
         searchDao.disableAllSearchNotifications();
      }
   }

   private void doReindex()
   {
      boolean reindex = Boolean.getBoolean("dhus.solr.reindex");
      LOGGER.info("Full solr reindex (dhus.solr.reindex) requested by user ({})", reindex);

      if (reindex)
      {
         searchService.fullReindex();
      }
      else
      {
         boolean fixUUIDs = Boolean.getBoolean("dhus.solr.fixuuids");
         LOGGER.info("Solr reindex to fix UUIDs (dhus.solr.fixuuids) requested by user ({})", fixUUIDs);
         if (fixUUIDs)
         {
            SolrQuery query = new SolrQuery("-uuid:*"); // Select all not having a UUID
            query.setRows(10_000_000); // 10M products to be returned (may cause an OOM exception)
            searchService.partialReindex(query, false);
         }
      }
   }
}
