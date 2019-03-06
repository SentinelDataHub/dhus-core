/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2018 GAEL Systems
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

import fr.gael.dhus.database.object.config.eviction.Eviction;
import fr.gael.dhus.database.object.config.eviction.EvictionConfiguration;
import fr.gael.dhus.database.object.config.scanner.ScannerInfo;
import fr.gael.dhus.database.object.config.scanner.ScannerManager;
import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerConfiguration;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerManager;
import fr.gael.dhus.database.object.config.synchronizer.UserSynchronizer;
import fr.gael.dhus.system.config.ConfigurationException;
import fr.gael.dhus.system.config.ConfigurationManager;
import fr.gael.dhus.util.XmlProvider;
import fr.gael.dhus.util.functional.tuple.Duo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

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

import org.dhus.store.datastore.config.DataStoreConf;
import org.dhus.store.datastore.config.DataStoreManager;
import org.dhus.store.datastore.config.HfsDataStoreConf;
import org.dhus.store.datastore.config.NamedDataStoreConf;
import org.dhus.store.datastore.config.OpenStackDataStoreConf;

/**
 * A Liquibase custom task to write into the XML configuration file values in every column of the
 * CONFIGURATION table before this table is dropped by the next change.
 */
public class WriteBackConfigurationEmbed implements CustomTaskChange
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String COMPAT_EVICTION_NAME = "_OldDbEviction";
   private ConfigurationManager cfgMgr;

   /**
    * From table CONFIGURATION to dhus.xml.
    *
    * @param connection an open connection to the database
    * @throws CustomChangeException in case of error that should interrupt start of DHuS
    */
   public void saveConfiguration(JdbcConnection connection) throws CustomChangeException
   {
      try
      {
         String sql = "SELECT * FROM CONFIGURATION";
         try (PreparedStatement statement = connection.prepareStatement(sql))
         {
            statement.execute();
            ResultSet resultSet = statement.getResultSet();
            if (!resultSet.next())
            {
               // No row? then nothing to do!
               return;
            }

            // Get each column value (only values that are editable at runtime are written back here)
            String eviction_schedule = resultSet.getString("EVICTION_SCHEDULE");
            Boolean mail_onusercreate = resultSet.getObject("MAIL_ONUSERCREATE", Boolean.class);
            Boolean mail_onuserupdate = resultSet.getObject("MAIL_ONUSERUPDATE", Boolean.class);
            Boolean mail_onuserdelete = resultSet.getObject("MAIL_ONUSERDELETE", Boolean.class);
            String mailserver_smtp = resultSet.getString("MAILSERVER_SMTP");
            Integer mailserver_port = resultSet.getObject("MAILSERVER_PORT", Integer.class);
            Boolean mailserver_tls = resultSet.getObject("MAILSERVER_TLS", Boolean.class);
            String mailserver_username = resultSet.getString("MAILSERVER_USERNAME");
            String mailserver_password = resultSet.getString("MAILSERVER_PASSWORD");
            String mailserver_replyto = resultSet.getString("MAILSERVER_REPLYTO");
            String mailserver_fromname = resultSet.getString("MAILSERVER_FROMNAME");
            String mailserver_fromaddress = resultSet.getString("MAILSERVER_FROMADDRESS");
            String system_supportname = resultSet.getString("SYSTEM_SUPPORTNAME");
            String system_supportmail = resultSet.getString("SYSTEM_SUPPORTMAIL");
            String system_registrationmail = resultSet.getString("SYSTEM_REGISTRATIONMAIL");

            // Set config is column value is not null
            if (cfgMgr.getEvictionManager().getEvictionByName(COMPAT_EVICTION_NAME) == null)
            {
               Eviction oldEviction = new Eviction();
               oldEviction.setName(COMPAT_EVICTION_NAME);
               oldEviction.setCron(new EvictionConfiguration.Cron());
               oldEviction.getCron().setActive(false);
               if (eviction_schedule != null)
               {
                  oldEviction.getCron().setSchedule(eviction_schedule);
               }
               cfgMgr.getEvictionManager().getEviction().add(oldEviction);
            }
            if (mail_onusercreate != null)
            {
               cfgMgr.getMailConfiguration().setOnUserCreate(mail_onusercreate);
            }
            if (mail_onuserupdate != null)
            {
               cfgMgr.getMailConfiguration().setOnUserUpdate(mail_onuserupdate);
            }
            if (mail_onuserdelete != null)
            {
               cfgMgr.getMailConfiguration().setOnUserDelete(mail_onuserdelete);
            }
            if (mailserver_smtp != null)
            {
               cfgMgr.getMailConfiguration().getServerConfiguration().setSmtp(mailserver_smtp);
            }
            if (mailserver_port != null)
            {
               cfgMgr.getMailConfiguration().getServerConfiguration().setPort(mailserver_port);
            }
            if (mailserver_tls != null)
            {
               cfgMgr.getMailConfiguration().getServerConfiguration().setTls(mailserver_tls);
            }
            if (mailserver_username != null)
            {
               cfgMgr.getMailConfiguration().getServerConfiguration().setUsername(mailserver_username);
            }
            if (mailserver_password != null)
            {
               cfgMgr.getMailConfiguration().getServerConfiguration().setPassword(mailserver_password);
            }
            if (mailserver_replyto != null)
            {
               cfgMgr.getMailConfiguration().getServerConfiguration().setReplyTo(mailserver_replyto);
            }
            if (mailserver_fromname != null)
            {
               cfgMgr.getMailConfiguration().getServerConfiguration().getMailFromConfiguration().setName(mailserver_fromname);
            }
            if (mailserver_fromaddress != null)
            {
               cfgMgr.getMailConfiguration().getServerConfiguration().getMailFromConfiguration().setAddress(mailserver_fromaddress);
            }
            if (system_supportname != null)
            {
               cfgMgr.getSupportConfiguration().setName(system_supportname);
            }
            if (system_supportmail != null)
            {
               cfgMgr.getSupportConfiguration().setMail(system_supportmail);
            }
            if (system_registrationmail != null)
            {
               cfgMgr.getSupportConfiguration().setRegistrationMail(system_registrationmail);
            }
         }
      }
      catch (DatabaseException | SQLException ex)
      {
         throw new CustomChangeException(ex);
      }
   }

   /**
    * From tables SYNCHRONIZERS & SYNCHRONIZERS_CONFIG to dhus.xml.
    *
    * @param connection an open connection to the database
    * @throws CustomChangeException in case of error that should interrupt start of DHuS
    */
   public void saveSynchronizers(JdbcConnection connection) throws CustomChangeException
   {
      try
      {
         List<Duo<Long, Long>> coordinates = new LinkedList<>();
         SynchronizerManager syncMgr = cfgMgr.getSynchronizerManager();
         SynchronizerConfiguration sc = null;

         String sql = "SELECT * FROM SYNCHRONIZERS";
         try (PreparedStatement statement = connection.prepareStatement(sql))
         {
            statement.execute();
            ResultSet resultSet = statement.getResultSet();


            while (resultSet.next())
            {
               Long sync_id = resultSet.getObject("ID", Long.class);
               if (sync_id == null)
               {
                  continue;
               }

               String type = resultSet.getString("TYPE");
               switch (type)
               {
                  case "ODataProductSynchronizer":
                     sc = new ProductSynchronizer();
                     break;
                  case "ODataUserSynchronizer":
                     sc = new UserSynchronizer();
                     break;
                  // Event synchronisers did not exist before the externalisation
                  default:
                        throw new CustomChangeException("Sync type unknown " + type);
               }
               String label = resultSet.getString("LABEL");
               String schedule = resultSet.getString("CRON_EXP");
               Boolean active = resultSet.getObject("ACTIVE", Boolean.class);
               Timestamp created = resultSet.getObject("CREATED", Timestamp.class);
               Timestamp modified = resultSet.getObject("MODIFIED", Timestamp.class);

               if (label != null)
               {
                  sc.setLabel(label);
               }
               if (schedule != null)
               {
                  sc.setSchedule(schedule);
               }
               if (active != null)
               {
                  sc.setActive(active);
               }
               if (created != null)
               {
                  sc.setCreated(XmlProvider.getCalendar(created.getTime()));
               }
               if (modified != null)
               {
                  sc.setModified(XmlProvider.getCalendar(modified.getTime()));
               }
               sc = syncMgr.create(sc, false);
               coordinates.add(new Duo<>(sync_id, sc.getId()));
            }
         }

         // Sync config
         sql = "SELECT * FROM SYNCHRONIZERS_CONFIG WHERE SYNC_ID=?";
         try (PreparedStatement statement = connection.prepareStatement(sql))
         {
            for (Duo<Long, Long> coord: coordinates)
            {
               sc = syncMgr.get(coord.getB());
               statement.setLong(1, coord.getA());
               statement.execute();
               try (ResultSet resultSet = statement.getResultSet())
               {
                  while (resultSet.next())
                  {
                     // Key/Value of sync_config
                     String key = resultSet.getString("CONFIG_KEY");
                     String val = resultSet.getString("CONFIG_VALUE");
                     if (key == null || val == null)
                     {
                        continue;
                     }

                     // Set config value using correct subClass
                     if (ProductSynchronizer.class.isAssignableFrom(sc.getClass()))
                     {
                        ProductSynchronizer prodSync = ProductSynchronizer.class.cast(sc);
                        switch (key)
                        {
                           case "service_uri":
                              prodSync.setServiceUrl(val);
                              break;
                           case "service_username":
                              prodSync.setServiceLogin(val);
                              break;
                           case "service_password":
                              prodSync.setServicePassword(val);
                              break;
                           case "last_created":
                              prodSync.setLastCreated(XmlProvider.getCalendar(Long.decode(val)));
                              break;
                           case "page_size":
                              prodSync.setPageSize(Integer.parseInt(val));
                              break;
                           case "remote_incoming_path":
                              prodSync.setRemoteIncoming(val);
                              break;
                           case "target_collection":
                              String colSql = "SELECT NAME FROM COLLECTIONS WHERE UUID='" + val + "'";
                              try (PreparedStatement getQueryables = connection.prepareStatement(colSql);
                                   ResultSet res = getQueryables.executeQuery())
                              {
                                 if (res.next())
                                 {
                                    String name = (String) res.getObject("NAME");
                                    if (name != null)
                                    {
                                       prodSync.setTargetCollection(name);
                                    }
                                 }
                              }
                              break;
                           case "filter_param":
                              prodSync.setFilterParam(val);
                              break;
                           case "source_collection":
                              prodSync.setSourceCollection(val);
                              break;
                           case "copy_product":
                              prodSync.setCopyProduct(Boolean.parseBoolean(val));
                              break;
                           case "geofilter_op":
                              prodSync.setGeofilterOp(val);
                              break;
                           case "geofilter_shape":
                              prodSync.setGeofilterShape(val);
                              break;
                           default:
                              LOGGER.warn("Unknown sync config key: '{}'", key);
                        }
                     }
                     else
                     {
                        UserSynchronizer userSync = UserSynchronizer.class.cast(sc);
                        switch (key)
                        {
                           case "service_uri":
                              userSync.setServiceUrl(val);
                              break;
                           case "service_username":
                              userSync.setServiceLogin(val);
                              break;
                           case "service_password":
                              userSync.setServicePassword(val);
                              break;
                           case "skip":
                              userSync.setSkip(Integer.parseInt(val));
                              break;
                           case "page_size":
                              userSync.setPageSize(Integer.parseInt(val));
                              break;
                           case "force":
                              userSync.setForce(Boolean.parseBoolean(val));
                              break;
                           default:
                              LOGGER.warn("Unknown sync config key: '{}'", key);
                        }
                     }
                  }
               }
               // Fix values
               if (sc.getPageSize() <= 0)
               {
                  sc.setPageSize(30);
               }
            }
         }
      }
      catch (DatabaseException | SQLException ex)
      {
         throw new CustomChangeException(ex);
      }
   }

   /**
    * From tables FILE_SCANNER & FILE_SCANNER_PREFERENCES & FILESCANNER_COLLECTIONS to dhus.xml.
    *
    * @param connection an open connection to the database
    * @throws CustomChangeException in case of error that should interrupt start of DHuS
    */
   public void saveFileScanners(JdbcConnection connection) throws CustomChangeException
   {
      try
      {
         ScannerManager scMgr = cfgMgr.getScannerManager();
         List<Duo<Long, Long>> coordinates = new LinkedList<>();

         // FileScanners
         String sql = "SELECT * FROM FILE_SCANNER";
         try (PreparedStatement statement = connection.prepareStatement(sql))
         {
            statement.execute();
            ResultSet resultSet = statement.getResultSet();
            while (resultSet.next())
            {
               Long id = resultSet.getObject("ID", Long.class);
               Boolean active = resultSet.getObject("ACTIVE", Boolean.class);
               String password = resultSet.getString("PASSWORD");
               String pattern = resultSet.getString("PATTERN");
               String status = resultSet.getString("STATUS");
               String status_message = resultSet.getString("STATUS_MESSAGE");
               String url = resultSet.getString("URL");
               String username = resultSet.getString("USERNAME");

               ScannerInfo scInfo = scMgr.create(url, status, status_message, active, username, password, pattern, false);

               coordinates.add(new Duo<>(id, scInfo.getId()));
            }
         }

         // FileScanner <>---> Collections
         sql = "SELECT * FROM FILESCANNER_COLLECTIONS AS fs LEFT OUTER JOIN COLLECTIONS AS col ON fs.COLLECTIONS_UUID=col.UUID WHERE FILE_SCANNER_ID=?";
         try (PreparedStatement statement = connection.prepareStatement(sql))
         {
            for (Duo<Long, Long> coord: coordinates)
            {
               ScannerInfo scInfo = scMgr.get(coord.getB());
               statement.setLong(1, coord.getA());
               statement.execute();
               try (ResultSet resultSet = statement.getResultSet())
               {
                  List<String> collections = new LinkedList<>();
                  while (resultSet.next())
                  {
                     String collectionName = resultSet.getString("NAME");
                     if (collectionName != null && !collectionName.isEmpty())
                     {
                        collections.add(collectionName);
                     }
                  }
                  if (!collections.isEmpty())
                  {
                     ScannerInfo.Collections scCollections = scInfo.getCollections();
                     if (scCollections == null)
                     {
                        scCollections = new ScannerInfo.Collections();
                     }
                     scCollections.setCollection(collections);
                     scMgr.update(scInfo, false);
                  }
               }
            }
         }
      }
      catch (DatabaseException | SQLException ex)
      {
         throw new CustomChangeException(ex);
      }
   }

   /**
    * From table DATASTORE_CONF to dhus.xml.
    *
    * @param connection an open connection to the database
    * @throws CustomChangeException in case of error that should interrupt start of DHuS
    */
   public void saveDataStores(JdbcConnection connection) throws CustomChangeException
   {
      try
      {
         String sql = "SELECT * FROM DATASTORE_CONF";
         try (PreparedStatement statement = connection.prepareStatement(sql))
         {
            statement.execute();
            ResultSet resultSet = statement.getResultSet();
            DataStoreManager dsMgr = cfgMgr.getDataStoreManager();
            while (resultSet.next())
            {
               String name = resultSet.getString("NAME");
               DataStoreConf dsCfg = dsMgr.get(name);
               if (dsCfg == null)
               {
                  String type = resultSet.getString("TYPE");
                  switch (type)
                  {
                     case "hfs":
                        dsCfg = new HfsDataStoreConf();
                        break;
                     case "openstack":
                        dsCfg = new OpenStackDataStoreConf();
                        break;
                     default:
                        LOGGER.warn("Cannot migrate datastore '{}' of unknown type '{}'", name, type);
                        continue;
                  }
                  NamedDataStoreConf.class.cast(dsCfg).setName(name);
                  dsMgr.create(dsCfg, false);
               }

               Boolean read_only = resultSet.getObject("READ_ONLY", Boolean.class);
               if (read_only != null)
               {
                  dsCfg.setReadOnly(read_only);
               }

               if (HfsDataStoreConf.class.isAssignableFrom(dsCfg.getClass()))
               {
                  HfsDataStoreConf hfsDsCfg = HfsDataStoreConf.class.cast(dsCfg);
                  String path = resultSet.getString("PATH");
                  Integer max_file_depth = resultSet.getObject("MAX_FILE_DEPTH", Integer.class);
                  if (path != null)
                  {
                     hfsDsCfg.setPath(path);
                  }
                  if (max_file_depth != null && max_file_depth > 4)
                  {
                     hfsDsCfg.setMaxFileNo(max_file_depth);
                  }
               }
               else
               {
                  OpenStackDataStoreConf openStackDsCfg = OpenStackDataStoreConf.class.cast(dsCfg);
                  String provider = resultSet.getString("PROVIDER");
                  String identity = resultSet.getString("IDENTITY");
                  String credential = resultSet.getString("CREDENTIAL");
                  String url = resultSet.getString("URL");
                  String region = resultSet.getString("REGION");
                  String container = resultSet.getString("CONTAINER");
                  if (provider != null)
                  {
                     openStackDsCfg.setProvider(provider);
                  }
                  if (identity != null)
                  {
                     openStackDsCfg.setIdentity(identity);
                  }
                  if (credential != null)
                  {
                     openStackDsCfg.setCredential(credential);
                  }
                  if (url != null)
                  {
                     openStackDsCfg.setUrl(url);
                  }
                  if (region != null)
                  {
                     openStackDsCfg.setRegion(region);
                  }
                  if (container != null)
                  {
                     openStackDsCfg.setContainer(container);
                  }
               }
               LOGGER.debug("Successfully migrated datastore '{}'", name);
            }
         }
      }
      catch (DatabaseException | SQLException ex)
      {
         throw new CustomChangeException(ex);
      }
   }

   /**
    * From table EVICTION to dhus.xml.
    *
    * @param connection an open connection to the database
    * @throws CustomChangeException in case of error that should interrupt start of DHuS
    */
   public void saveEviction(JdbcConnection connection) throws CustomChangeException
   {
      Eviction oldEviction = cfgMgr.getEvictionManager().getEvictionByName(COMPAT_EVICTION_NAME);
      if (oldEviction == null)
      {
         return;
      }
      try
      {
         String sql = "SELECT * FROM EVICTION";
         try (PreparedStatement statement = connection.prepareStatement(sql))
         {
            statement.execute();
            ResultSet resultSet = statement.getResultSet();
            if (!resultSet.next())
            {
               // No row? then nothing to do!
               return;
            }

            Integer keep_period = resultSet.getObject("KEEP_PERIOD", Integer.class);
            Integer max_product_number = resultSet.getObject("EVICTION_MAX_PRODUCT_NUMBER", Integer.class);

            if (keep_period != null)
            {
               oldEviction.setKeepPeriod(keep_period);
            }
            if (max_product_number != null)
            {
               oldEviction.setMaxEvictedProducts(max_product_number);
            }
         }
      }
      catch (DatabaseException | SQLException ex)
      {
         throw new CustomChangeException(ex);
      }
   }

   @Override
   public void execute(Database database) throws CustomChangeException
   {
      try
      {
         this.cfgMgr = new ConfigurationManager();
      }
      catch (ConfigurationException ex)
      {
         throw new CustomChangeException("ConfigurationManager bean not available");
      }

      JdbcConnection connection = (JdbcConnection) database.getConnection();
      saveConfiguration(connection);
      saveSynchronizers(connection);
      saveFileScanners(connection);
      saveDataStores(connection);
      saveEviction(connection);

      try
      {
         cfgMgr.saveConfiguration();
      }
      catch (ConfigurationException ex)
      {
         throw new CustomChangeException(ex);
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
   public void setFileOpener(ResourceAccessor ra) {}

   @Override
   public ValidationErrors validate(Database dtbs)
   {
      return null;
   }

}
