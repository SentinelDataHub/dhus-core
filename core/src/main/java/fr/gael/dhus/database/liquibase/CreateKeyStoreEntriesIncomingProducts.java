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
package fr.gael.dhus.database.liquibase;

import fr.gael.dhus.ConfigurationConverter;
import fr.gael.dhus.system.config.ConfigurationException;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Unbox;

import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.config.DataStoreRestriction;
import org.dhus.store.datastore.config.HfsDataStoreConf;
import org.dhus.store.datastore.config.NamedDataStoreConf;
import org.dhus.store.derived.DerivedProductStore;

/**
 * Migrate existing path, quicklookPath and thumbnailPath into OldIncomingAdapter.
 */
public final class CreateKeyStoreEntriesIncomingProducts implements CustomTaskChange
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int LIMIT = 10_000;
   private static final String NEW_DS_NAME_PREFIX = "_MigratedDataStore_";
   private static final String HFS_REGEX = "file:(.+?)" // path to datastore
         + "((/\\d+)+/dhus_entry(/product)?/"           // antediluvian HFS path segment format (pre 0.4.1 (c61188b92))
         + "|"
         + "(/x[0-9A-F]+)+(/dhus_entry/product)?/"      // newer HFS path segment format
         + "|"
         + "/dhus_entry/product/)"                      // no HFS path segments (how?)
         + "[^/]+"                                      // product file
         + "/?";                                        // exploded product
   private static final Pattern HFS_PATTERN = Pattern.compile(HFS_REGEX);

   private ConfigurationManager cfgMgr;
   private List<HfsDataStoreConf> HFSCfgList;
   private int numberOfNewDataStores = 0;

   private long countProductsToMigrate(JdbcConnection dbConn)
   {
      long res = 0L;
      try (PreparedStatement maxProductsIDs = dbConn.prepareStatement("SELECT COUNT (*) FROM PRODUCTS WHERE PATH IS NOT NULL"))
      {
         ResultSet maxProducts_IDs = maxProductsIDs.executeQuery();
         maxProducts_IDs.next();
         res = maxProducts_IDs.getLong(1);
         LOGGER.info("OldIncoming products to migrate: {}", Unbox.box(res));
      }
      catch (DatabaseException | SQLException e)
      {
         LOGGER.error("An exception occured", e);
      }
      return res;
   }

   private void createAndCheckConfiguration() throws CustomChangeException
   {
      try
      {
          cfgMgr = new ConfigurationManager();

          NamedDataStoreConf ds = cfgMgr.getDataStoreManager().getNamed(ConfigurationConverter.INCOMING_NAME);
          if (ds == null)
          {
             LOGGER.fatal("DataStore {} not found in configuration", ConfigurationConverter.INCOMING_NAME);
             throw new CustomChangeException();
          }
          if (!HfsDataStoreConf.class.isAssignableFrom(ds.getClass()))
          {
             LOGGER.fatal("DataStore {} is not an HFS, cannot migrate incoming products", ConfigurationConverter.INCOMING_NAME);
             throw new CustomChangeException();
          }
      }
      catch (ConfigurationException ex)
      {
         LOGGER.fatal("could not load configuration", ex);
         throw new CustomChangeException();
      }
   }

   private static HfsDataStoreConf logPath(HfsDataStoreConf cfg)
   {
      LOGGER.debug("Known path prefix to remove: '{}'", cfg.getPath());
      return cfg;
   }

   private void gatherAllPaths()
   {
      // Gets all paths from all HFS DataStores declared in the conf
      this.HFSCfgList = this.cfgMgr.getDataStoresConf().stream()
            .filter((namesCfg) -> HfsDataStoreConf.class.isAssignableFrom(namesCfg.getClass()))
            .<HfsDataStoreConf>map(HfsDataStoreConf.class::cast)
            .<HfsDataStoreConf>map(CreateKeyStoreEntriesIncomingProducts::logPath)
            .collect(Collectors.toList());
   }

   private HfsDataStoreConf findOrCreateDataStore(String dbPath)
   {
      for (HfsDataStoreConf cfg: this.HFSCfgList)
      {
         String path = cfg.getPath();
         if (!path.endsWith("/"))
         {
            path = path + '/';
         }
         if (dbPath.contains(path))
         {
            return cfg;
         }
      }
      LOGGER.debug("Orphan product detected at '{}', creating a new DataStore ...", dbPath);
      Matcher matcher = HFS_PATTERN.matcher(dbPath);
      String path;
      if (matcher.matches())
      {
         path = matcher.group(1);
      }
      else // Not a HFS, therefore it must be the now deprecated LocalArchive!!
      {
         path = Paths.get(dbPath.substring(5)).getParent().toString();
      }
      HfsDataStoreConf newCfg = new HfsDataStoreConf();
      newCfg.setRestriction(DataStoreRestriction.REFERENCES_ONLY);
      newCfg.setName(NEW_DS_NAME_PREFIX + ++numberOfNewDataStores);
      newCfg.setPath(path);
      cfgMgr.getDataStoresConf().add(newCfg);
      this.HFSCfgList.add(newCfg);
      LOGGER.info("New DataStore '{}' at '{}'", newCfg.getName(), newCfg.getPath());
      return newCfg;
   }

   private static String toAbsPath(String path)
   {
      String res = Paths.get(path).toAbsolutePath().toString();
      return res.endsWith("/") ? res: res + '/';
   }

   private String removePathPrefix(String prefix, String path)
   {
      if (path == null)
      {
         return null;
      }
      return path.substring(prefix.length());
   }

   private int migrateProducts(JdbcConnection dbConn) throws CustomChangeException
   {
      int count = 0;

      try (PreparedStatement getPaths = dbConn.prepareStatement(
                  "SELECT PATH, QUICKLOOK_PATH, THUMBNAIL_PATH, UUID, ID, DOWNLOAD_SIZE FROM PRODUCTS"
                  + " WHERE ID > ? AND PATH IS NOT NULL ORDER BY ID LIMIT ?");
           PreparedStatement keyStoreEntries = dbConn.prepareStatement(
                  "INSERT INTO KEYSTOREENTRIES (KEYSTORE, ENTRYKEY, TAG, VALUE) VALUES (?, ?, ?, ?)"))
      {
         boolean loop = true;
         long productIDs = -1;
         String dbPath, path, altPathPrefix, pathPrefix;
         HfsDataStoreConf dsCfg;
         while (loop)
         {
            getPaths.setLong(1, productIDs);
            getPaths.setLong(2, this.LIMIT);

            ResultSet get_paths = getPaths.executeQuery();
            if (!get_paths.isBeforeFirst())
            {
               break;
            }
            int counter = 0;
            while (get_paths.next())
            {
               counter++;
               dbPath = get_paths.getString("PATH");

               if ((dsCfg = findOrCreateDataStore(dbPath)) == null)
               {
                  LOGGER.error("Cannot migrate product at '{}', reason: DataStore is null", dbPath);
                  continue;
               }
               altPathPrefix = toAbsPath(dsCfg.getPath());
               pathPrefix = "file:" + altPathPrefix;
               if ((path = removePathPrefix(pathPrefix, dbPath)) == null)
               {
                  LOGGER.error("Cannot migrate product at '{}', reason: path is null", dbPath);
                  continue;
               }
               LOGGER.trace("Product at '{}' to be inserted into datastore '{}'", dbPath, dsCfg.getName());

               productIDs = get_paths.getLong("ID");
               dsCfg.setCurrentSize(dsCfg.getCurrentSize() + get_paths.getLong("DOWNLOAD_SIZE"));

               keyStoreEntries.setString(1, dsCfg.getName());
               keyStoreEntries.setString(2, get_paths.getString("UUID"));
               keyStoreEntries.setString(3, DataStore.UNALTERED_PRODUCT_TAG);
               keyStoreEntries.setString(4, path);
               keyStoreEntries.addBatch();

               if ((path = removePathPrefix(altPathPrefix, get_paths.getString("QUICKLOOK_PATH"))) != null)
               {
                  keyStoreEntries.setString(3, DerivedProductStore.QUICKLOOK_TAG);
                  keyStoreEntries.setString(4, path);
                  keyStoreEntries.addBatch();
               }
               if ((path = removePathPrefix(altPathPrefix, get_paths.getString("THUMBNAIL_PATH"))) != null)
               {
                  keyStoreEntries.setString(3, DerivedProductStore.THUMBNAIL_TAG);
                  keyStoreEntries.setString(4, path);
                  keyStoreEntries.addBatch();
               }
            }

            int[] results = keyStoreEntries.executeBatch();
            int total = 0;
            for (int result: results)
            {
               if (result < 0)
               {
                  LOGGER.warn("A KeyStoreEntry could not be inserted!");
                  continue;
               }
               total += result;
            }
            count += counter;
            LOGGER.debug("Successfully created {} KeyStoreEntries for {} products", Unbox.box(total), Unbox.box(counter));

            loop = counter == this.LIMIT;
         }
      }
      catch (DatabaseException | SQLException ex)
      {
         throw new CustomChangeException(ex);
      }

      return count;
   }

   @Override
   public void execute(Database database) throws CustomChangeException
   {
      JdbcConnection databaseConnection = (JdbcConnection) database.getConnection();

      long count = countProductsToMigrate(databaseConnection);
      if (count <= 0L)
      {
         return; // Nothing to do
      }

      // Creates a ConfigurationManager and gets the target HFS DataStore to store incoming product
      createAndCheckConfiguration();
      gatherAllPaths();

      long migrated = migrateProducts(databaseConnection);
      LOGGER.info("Migrated {}/{} old incoming products to their datastores", Unbox.box(migrated), Unbox.box(count));

      try
      {
         this.cfgMgr.saveConfiguration();
      }
      catch (ConfigurationException ex)
      {
         LOGGER.warn("Could not save updated datastores");
      }
   }

   @Override
   public String getConfirmationMessage()
   {
      return null;
   }

   @Override
   public void setUp() throws SetupException {}

   @Override
   public void setFileOpener(ResourceAccessor resourceAccessor) {}

   @Override
   public ValidationErrors validate(Database database)
   {
      return null;
   }
}
