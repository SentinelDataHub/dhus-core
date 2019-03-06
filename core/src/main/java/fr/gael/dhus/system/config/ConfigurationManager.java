/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2017,2018 GAEL Systems
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
package fr.gael.dhus.system.config;

import fr.gael.dhus.database.object.config.Configuration;
import fr.gael.dhus.database.object.config.cron.ArchiveSynchronizationCronConfiguration;
import fr.gael.dhus.database.object.config.cron.CleanDatabaseCronConfiguration;
import fr.gael.dhus.database.object.config.cron.CleanDatabaseDumpCronConfiguration;
import fr.gael.dhus.database.object.config.cron.CronConfiguration;
import fr.gael.dhus.database.object.config.cron.DumpDatabaseCronConfiguration;
import fr.gael.dhus.database.object.config.cron.FileScannersCronConfiguration;
import fr.gael.dhus.database.object.config.cron.SearchesCronConfiguration;
import fr.gael.dhus.database.object.config.cron.SendLogsCronConfiguration;
import fr.gael.dhus.database.object.config.cron.StatisticsConfiguration;
import fr.gael.dhus.database.object.config.cron.SystemCheckCronConfiguration;
import fr.gael.dhus.database.object.config.cron.TempUsersConfiguration;
import fr.gael.dhus.database.object.config.eviction.EvictionConfiguration;
import fr.gael.dhus.database.object.config.eviction.EvictionManager;
import fr.gael.dhus.database.object.config.eviction.EvictionStatusEnum;
import fr.gael.dhus.database.object.config.messaging.MailConfiguration;
import fr.gael.dhus.database.object.config.messaging.MailFromConfiguration;
import fr.gael.dhus.database.object.config.messaging.MailServerConfiguration;
import fr.gael.dhus.database.object.config.messaging.MessagingConfiguration;
import fr.gael.dhus.database.object.config.messaging.jms.JmsConfiguration;
import fr.gael.dhus.database.object.config.network.NetworkConfiguration;
import fr.gael.dhus.database.object.config.product.DownloadConfiguration;
import fr.gael.dhus.database.object.config.product.ProductConfiguration;
import fr.gael.dhus.database.object.config.product.QuicklookConfiguration;
import fr.gael.dhus.database.object.config.product.ThumbnailConfiguration;
import fr.gael.dhus.database.object.config.scanner.ScannerManager;
import fr.gael.dhus.database.object.config.search.GeocoderConfiguration;
import fr.gael.dhus.database.object.config.search.GeonameConfiguration;
import fr.gael.dhus.database.object.config.search.NominatimConfiguration;
import fr.gael.dhus.database.object.config.search.OdataConfiguration;
import fr.gael.dhus.database.object.config.search.SearchConfiguration;
import fr.gael.dhus.database.object.config.search.SolrCloudConfiguration;
import fr.gael.dhus.database.object.config.search.SolrConfiguration;
import fr.gael.dhus.database.object.config.search.SolrStandaloneConfiguration;
import fr.gael.dhus.database.object.config.server.AbstractServerConfiguration;
import fr.gael.dhus.database.object.config.server.ExternalServerConfiguration;
import fr.gael.dhus.database.object.config.server.ServerConfiguration;
import fr.gael.dhus.database.object.config.source.SourceManagerImpl;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerManager;
import fr.gael.dhus.database.object.config.system.AdministratorConfiguration;
import fr.gael.dhus.database.object.config.system.ArchiveConfiguration;
import fr.gael.dhus.database.object.config.system.DatabaseConfiguration;
import fr.gael.dhus.database.object.config.system.ExecutorConfiguration;
import fr.gael.dhus.database.object.config.system.NameConfiguration;
import fr.gael.dhus.database.object.config.system.ProcessingConfiguration;
import fr.gael.dhus.database.object.config.system.SupportConfiguration;
import fr.gael.dhus.database.object.config.system.SystemConfiguration;
import fr.gael.dhus.database.object.config.system.TomcatConfiguration;
import fr.gael.dhus.database.object.config.system.TrashPathConfiguration;
import fr.gael.dhus.search.SolrType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import fr.gael.dhus.sync.smart.SourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.store.datastore.config.DataStoreConf;
import org.dhus.store.datastore.config.DataStoreManager;
import org.springframework.stereotype.Service;

import org.xml.sax.SAXException;

@Service
public class ConfigurationManager
{
   private final ConfigurationLoader loader;

   /**
    * Creates the configuration manager bean and immediately loads the conf from XML files.
    *
    * @throws ConfigurationException could not load the conf
    */
   public ConfigurationManager() throws ConfigurationException
   {
      this.loader = new ConfigurationLoader();
      postLoad(loader.getConf());
      loader.save();
   }

   /**
    * Adds missing required elements (default values defined in the XSDs will be used).
    * <p>
    * Sets defaults that must be computed at runtime.
    *
    * @param conf to fix, must not be null
    */
   public static void postLoad(Configuration conf)
   {
      Objects.requireNonNull(conf);

      /// Sets all required elements

      // Crons
      setNewInstanceIfNull("cronConfiguration", conf, CronConfiguration.class);
      CronConfiguration crons = conf.getCronConfiguration();
      setNewInstanceIfNull("archiveSynchronizationConfiguration", crons, ArchiveSynchronizationCronConfiguration.class);
      setNewInstanceIfNull("cleanDatabaseConfiguration", crons, CleanDatabaseCronConfiguration.class);
      setNewInstanceIfNull("cleanDatabaseDumpConfiguration", crons, CleanDatabaseDumpCronConfiguration.class);
      setNewInstanceIfNull("dumpDatabaseConfiguration", crons, DumpDatabaseCronConfiguration.class);
      setNewInstanceIfNull("fileScannersConfiguration", crons, FileScannersCronConfiguration.class);
      setNewInstanceIfNull("searchesConfiguration", crons, SearchesCronConfiguration.class);
      setNewInstanceIfNull("sendLogsConfiguration", crons, SendLogsCronConfiguration.class);
      setNewInstanceIfNull("systemCheckConfiguration", crons, SystemCheckCronConfiguration.class);

      // CleanDatabase
      CleanDatabaseCronConfiguration cleanDB = crons.getCleanDatabaseConfiguration();
      setNewInstanceIfNull("tempUsersConfiguration", cleanDB, TempUsersConfiguration.class);
      setNewInstanceIfNull("logStatConfiguration", cleanDB, StatisticsConfiguration.class);

      // DataStores
      setNewInstanceIfNull("dataStores", conf, DataStoreManager.class);

      // Evictions
      setNewInstanceIfNull("evictions", conf, EvictionManager.class);

      // Messaging
      setNewInstanceIfNull("messagingConfiguration", conf, MessagingConfiguration.class);

      // Messaging->Mail
      setNewInstanceIfNull("mailConfiguration", conf.getMessagingConfiguration(), MailConfiguration.class);
      MailConfiguration mail = conf.getMessagingConfiguration().getMailConfiguration();
      setNewInstanceIfNull("serverConfiguration", mail, MailServerConfiguration.class);
      MailServerConfiguration mailSrv = mail.getServerConfiguration();
      setNewInstanceIfNull("mailFromConfiguration", mailSrv, MailFromConfiguration.class);

      // Network
      setNewInstanceIfNull("networkConfiguration", conf, NetworkConfiguration.class);

      // Product
      setNewInstanceIfNull("productConfiguration", conf, ProductConfiguration.class);
      ProductConfiguration prod = conf.getProductConfiguration();
      setNewInstanceIfNull("downloadConfiguration", prod, DownloadConfiguration.class);
      setNewInstanceIfNull("quicklookConfiguration", prod, QuicklookConfiguration.class);
      setNewInstanceIfNull("thumbnailConfiguration", prod, ThumbnailConfiguration.class);

      // Scanners
      setNewInstanceIfNull("scanners", conf, ScannerManager.class);

      // Search
      setNewInstanceIfNull("searchConfiguration", conf, SearchConfiguration.class);
      SearchConfiguration search = conf.getSearchConfiguration();
      setNewInstanceIfNull("geocoderConfiguration", search, GeocoderConfiguration.class);
      setNewInstanceIfNull("odataConfiguration", search, OdataConfiguration.class);

      // Search->Solr (default conf if none exist)
      if (search.getSolrConfiguration() == null
       && search.getSolrStandalone() == null
       && search.getSolrCloud() == null)
      {
         search.setSolrConfiguration(new SolrConfiguration());
      }

      // Search->Geocoder
      GeocoderConfiguration geocoder = search.getGeocoderConfiguration();
      setNewInstanceIfNull("nominatimConfiguration", geocoder, NominatimConfiguration.class);
      setNewInstanceIfNull("geonameConfiguration", geocoder, GeonameConfiguration.class);

      // Server
      setNewInstanceIfNull("serverConfiguration", conf, ServerConfiguration.class);
      AbstractServerConfiguration server = conf.getServerConfiguration();
      setNewInstanceIfNull("externalServerConfiguration", server, ExternalServerConfiguration.class);

      // Sources
      setNewInstanceIfNull("sources", conf, SourceManagerImpl.class);

      // Synchronizers
      setNewInstanceIfNull("synchronizers", conf, SynchronizerManager.class);

      // System
      setNewInstanceIfNull("systemConfiguration", conf, SystemConfiguration.class);
      SystemConfiguration sys = conf.getSystemConfiguration();
      setNewInstanceIfNull("administratorConfiguration", sys, AdministratorConfiguration.class);
      setNewInstanceIfNull("archiveConfiguration", sys, ArchiveConfiguration.class);
      setNewInstanceIfNull("databaseConfiguration", sys, DatabaseConfiguration.class);
      setNewInstanceIfNull("nameConfiguration", sys, NameConfiguration.class);
      setNewInstanceIfNull("processingConfiguration", sys, ProcessingConfiguration.class);
      setNewInstanceIfNull("supportConfiguration", sys, SupportConfiguration.class);
      setNewInstanceIfNull("tomcatConfiguration", sys, TomcatConfiguration.class);
      setNewInstanceIfNull("executorConfiguration", sys, ExecutorConfiguration.class);
      setNewInstanceIfNull("trashPathConfiguration", sys, TrashPathConfiguration.class);

      /// Sets runtime default values

      // Tomcat's `path` attribute must be absolute
      TomcatConfiguration tomcat = conf.getSystemConfiguration().getTomcatConfiguration();
      if (tomcat.getPath().startsWith("."))
      {
         tomcat.setPath(Paths.get(tomcat.getPath()).toAbsolutePath().toString());
      }

      // Set the status of all evictions to STOPPED
      List<EvictionConfiguration> evictions = conf.getEvictions().getEviction();
      evictions.forEach((evictionConf) -> evictionConf.setStatus(EvictionStatusEnum.STOPPED));

      // Set support mail as registration mail if it is empty or null
      SupportConfiguration supportConf = conf.getSystemConfiguration().getSupportConfiguration();
      String regmail = supportConf.getRegistrationMail();

      if (regmail == null || regmail.isEmpty())
      {
         supportConf.setRegistrationMail(supportConf.getMail());
      }
   }

   // Sets if non null a protected field in a class generated by XJC
   private static void setNewInstanceIfNull(String fieldName, Object toSet, Class<?> toInstanciate)
   {
      Class<?> toSetClass = toSet.getClass();
      try
      {
         Field field;
         try
         {
            field = toSetClass.getDeclaredField(fieldName);
         }
         catch (NoSuchFieldException ex)
         {
            toSetClass = toSetClass.getSuperclass();
            if (toSetClass == null)
            {
               throw ex;
            }
            field = toSetClass.getDeclaredField(fieldName);
         }
         field.setAccessible(true); // field accessibility was: protected
         if (field.get(toSet) == null)
         {
            field.set(toSet, toInstanciate.newInstance());
         }
      }
      catch (NoSuchFieldException | InstantiationException ex)
      {
         throw new RuntimeException(ex); // Programming issue
      }
      catch (IllegalArgumentException | IllegalAccessException ex)
      {
         // Ignored
      }
   }

   /**
    * Reload from XML.
    *
    * @throws ConfigurationException Could not reload
    */
   synchronized public void reloadConfiguration() throws ConfigurationException
   {
      loader.reload();
      postLoad(loader.getConf());
      loader.save();
   }

   /**
    * Returns a mutable configuration instance, valid until {@link #reloadConfiguration()} is
    * called.
    *
    * @return the currently loaded configuration
    */
   public Configuration getConfiguration()
   {
      return loader.getConf();
   }

   /**
    * Saves the current configuration back into its XML file.
    *
    * @throws ConfigurationException could not save to XML file
    */
   synchronized public void saveConfiguration() throws ConfigurationException
   {
      loader.save();
   }

   // Crons configurations
   public ArchiveSynchronizationCronConfiguration getArchiveSynchronizationCronConfiguration()
   {
      return loader.getConf().getCronConfiguration().getArchiveSynchronizationConfiguration();
   }

   public CleanDatabaseCronConfiguration getCleanDatabaseCronConfiguration()
   {
      return loader.getConf().getCronConfiguration().getCleanDatabaseConfiguration();
   }

   public DumpDatabaseCronConfiguration getDumpDatabaseCronConfiguration()
   {
      return loader.getConf().getCronConfiguration().getDumpDatabaseConfiguration();
   }

   public CleanDatabaseDumpCronConfiguration getCleanDatabaseDumpCronConfiguration()
   {
      return loader.getConf().getCronConfiguration().getCleanDatabaseDumpConfiguration();
   }

   public FileScannersCronConfiguration getFileScannersCronConfiguration()
   {
      return loader.getConf().getCronConfiguration().getFileScannersConfiguration();
   }

   public SearchesCronConfiguration getSearchesCronConfiguration()
   {
      return loader.getConf().getCronConfiguration().getSearchesConfiguration();
   }

   public SendLogsCronConfiguration getSendLogsCronConfiguration()
   {
      return loader.getConf().getCronConfiguration().getSendLogsConfiguration();
   }

   public SystemCheckCronConfiguration getSystemCheckCronConfiguration()
   {
      return loader.getConf().getCronConfiguration().getSystemCheckConfiguration();
   }

   // Messaging configurations
   public JmsConfiguration getJmsConfiguration()
   {
      return loader.getConf().getMessagingConfiguration().getJmsConfiguration();
   }

   public MailConfiguration getMailConfiguration()
   {
      return loader.getConf().getMessagingConfiguration().getMailConfiguration();
   }

   // Network configuration
   public NetworkConfiguration getNetworkConfiguration()
   {
      return loader.getConf().getNetworkConfiguration();
   }

   public DownloadConfiguration getDownloadConfiguration()
   {
      return loader.getConf().getProductConfiguration().getDownloadConfiguration();
   }

   public ProductConfiguration getProductConfiguration()
   {
      return loader.getConf().getProductConfiguration();
   }

   // Search configurations
   public SolrType getSolrType()
   {
      if (getSolrConfiguration() != null)
      {
         return SolrType.EMBED;
      }
      if (getSolrStandaloneConfiguration() != null)
      {
         return SolrType.STANDALONE;
      }
      if (getSolrCloudConfiguration() != null)
      {
         return SolrType.CLOUD;
      }
      return SolrType.NONE;
   }

   public SolrConfiguration getSolrConfiguration ()
   {
      return loader.getConf().getSearchConfiguration().getSolrConfiguration();
   }

   public SolrStandaloneConfiguration getSolrStandaloneConfiguration()
   {
      return loader.getConf().getSearchConfiguration().getSolrStandalone();
   }

   public SolrCloudConfiguration getSolrCloudConfiguration()
   {
      return loader.getConf().getSearchConfiguration().getSolrCloud();
   }

   public OdataConfiguration getOdataConfiguration()
   {
      return loader.getConf().getSearchConfiguration().getOdataConfiguration();
   }

   public GeonameConfiguration getGeonameConfiguration()
   {
      return loader.getConf()
            .getSearchConfiguration().getGeocoderConfiguration().getGeonameConfiguration();
   }

   public GeocoderConfiguration getGeocoderConfiguration()
   {
      return loader.getConf().getSearchConfiguration().getGeocoderConfiguration();
   }

   public NominatimConfiguration getNominatimConfiguration()
   {
      return loader.getConf()
            .getSearchConfiguration().getGeocoderConfiguration().getNominatimConfiguration();
   }

   // Server configurations
   public ServerConfiguration getServerConfiguration()
   {
      return (ServerConfiguration) loader.getConf().getServerConfiguration();
   }

   // System configurations
   public ArchiveConfiguration getArchiveConfiguration()
   {
      return loader.getConf().getSystemConfiguration().getArchiveConfiguration();
   }

   public TomcatConfiguration getTomcatConfiguration()
   {
      return loader.getConf().getSystemConfiguration().getTomcatConfiguration();
   }

   public SupportConfiguration getSupportConfiguration()
   {
      return loader.getConf().getSystemConfiguration().getSupportConfiguration();
   }

   public AdministratorConfiguration getAdministratorConfiguration()
   {
      return loader.getConf().getSystemConfiguration().getAdministratorConfiguration();
   }

   public NameConfiguration getNameConfiguration()
   {
      return loader.getConf().getSystemConfiguration().getNameConfiguration();
   }

   public ProcessingConfiguration getProcessingConfiguration()
   {
      return loader.getConf().getSystemConfiguration().getProcessingConfiguration();
   }

   public List<DataStoreConf> getDataStoresConf()
   {
      return loader.getConf().getDataStores().getDataStore();
   }

   public DatabaseConfiguration getDatabaseConfiguration()
   {
      return loader.getConf().getSystemConfiguration().getDatabaseConfiguration();
   }

   public ExecutorConfiguration getExecutorConfiguration()
   {
      return loader.getConf().getSystemConfiguration().getExecutorConfiguration();
   }

   public String getWorkingDirectoryPath()
   {
      return loader.getConf().getWorkingDirectory();
   }

   // Used in dhus-core-database.xml
   public String getJDBCDriver()
   {
      return loader.getConf().getSystemConfiguration().getDatabaseConfiguration().getJDBCDriver();
   }

   // Used in dhus-core-database.xml
   public String getJDBCUrl()
   {
      return loader.getConf().getSystemConfiguration().getDatabaseConfiguration().getJDBCUrl();
   }

   // Used in dhus-core-database.xml
   public String getLogin()
   {
      return loader.getConf().getSystemConfiguration().getDatabaseConfiguration().getLogin();
   }

   // Used in dhus-core-database.xml
   public String getPassword()
   {
      return loader.getConf().getSystemConfiguration().getDatabaseConfiguration().getPassword();
   }

   // Used in dhus-core-database.xml
   public String getHibernateDialect()
   {
      return loader.getConf().getSystemConfiguration().getDatabaseConfiguration().getHibernateDialect();
   }

   public DataStoreManager getDataStoreManager()
   {
      return (DataStoreManager) loader.getConf().getDataStores();
   }

   public ScannerManager getScannerManager()
   {
      return (ScannerManager) loader.getConf().getScanners();
   }

   public SourceManager getSourceManager()
   {
      return (SourceManager) loader.getConf().getSources();
   }

   public SynchronizerManager getSynchronizerManager()
   {
      return (SynchronizerManager) loader.getConf().getSynchronizers();
   }

   public EvictionManager getEvictionManager()
   {
      return (EvictionManager) loader.getConf().getEvictions();
   }

   public String getTrashPath()
   {
      TrashPathConfiguration trashPathConfiguration = loader.getConf().getSystemConfiguration().getTrashPathConfiguration();
      return (trashPathConfiguration == null) ? null : trashPathConfiguration.getPath();
   }

   public String getErrorPath()
   {
      ArchiveConfiguration archiveConf = loader.getConf().getSystemConfiguration().getArchiveConfiguration();
      if (archiveConf != null)
      {
         return archiveConf.getErrorPath();
      }
      return null;
   }

   /**
    * Loads, Reloads, Writes the configuration from/to XML files using JAXB.
    * The XML schema for this XML configuration is a resource located in fr.gael.dhus.system.config.
    */
   static class ConfigurationLoader
   {
      private static final Logger LOGGER = LogManager.getLogger();

      private Configuration loadedConfiguration;

      /**
       * Creates a new Configuration loader that immediately loads configuration from config files.
       *
       * @throws ConfigurationException Could not load configuration
       */
      public ConfigurationLoader() throws ConfigurationException
      {
         loadedConfiguration = load();
      }

      /**
       * Reloads the configuration.
       *
       * @throws ConfigurationException could not reload configuration
       */
      public void reload() throws ConfigurationException
      {
         loadedConfiguration = load();
      }

      /**
       * Saves the loaded (potentially modified) loaded configuration into the configuration file.
       * <p>Writes to the dhus.xml in the classpath, or to the dhus.xml in the working directory.
       *
       * @throws ConfigurationException in case of error
       */
      public void save() throws ConfigurationException
      {
         // Search for "dhus.xml" in classpath
         URL configLocation = ClassLoader.getSystemResource("dhus.xml");
         if (configLocation == null)
         {
            // If not found, uses "dhus.xml" in the CWD
            try
            {
               configLocation = Paths.get("dhus.xml").toUri().toURL();
            }
            catch (IOException ex)
            {
               throw new ConfigurationException(ex);
            }
         }
         try
         {
            saveConfiguration(loadedConfiguration, configLocation);
         }
         catch (JAXBException | IOException | URISyntaxException ex)
         {
            throw new ConfigurationException("Could not save configuration", ex);
         }
      }

      /**
       * Load config from dhus.xml.
       * dhus.xml can be in the classpath, or in CWD.
       *
       * @return loaded configuration
       * @throws ConfigurationException Could not load or merge config files
       */
      private static Configuration load() throws ConfigurationException
      {
         Configuration configuration = null;

         try
         {
            // Search for "dhus.xml" in classpath
            URL configLocation = ClassLoader.getSystemResource("dhus.xml");
            if (configLocation != null)
            {
               configuration = loadConfiguation(configLocation);
            }
            else
            {
               Path cwdConf = Paths.get("dhus.xml");
               if (Files.exists(cwdConf) && Files.isReadable(cwdConf))
               {
                  configLocation = cwdConf.toUri().toURL();
                  configuration = loadConfiguation(configLocation);
               }
               else
               {
                  configuration = new Configuration();
               }
            }
         }
         catch (IOException | JAXBException | SAXException ex)
         {
            throw new ConfigurationException("User configuration error", ex);
         }

         return configuration;
      }

      /**
       * Load the passed configuration URL into {@link Configuration} class.
       *
       * @param configuration the configuration to parsed
       * @return the fulfilled configuration data set
       * @throws JAXBException when input is wrong
       * @throws IOException   when input cannot be accessed
       * @throws SAXException  XML cannot be parsed
       */
      static Configuration loadConfiguation(URL configuration)
            throws JAXBException, IOException, SAXException
      {
         JAXBContext context = JAXBContext.newInstance("fr.gael.dhus.database.object.config");
         Unmarshaller unmarshaller = context.createUnmarshaller();

         SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
         URL url = ConfigurationManager.class.getClassLoader().getResource("fr/gael/dhus/system/config/dhus-configuration.xsd");
         Schema schema = schemaFactory.newSchema(url);
         unmarshaller.setSchema(schema);

         LOGGER.info("Loading configuration from {}", configuration.toExternalForm());

            unmarshaller.setEventHandler(new ValidationEventHandler()
            {
               @Override
               public boolean handleEvent(ValidationEvent event)
               {
                  switch (event.getSeverity())
                  {
                     case ValidationEvent.WARNING:
                     case ValidationEvent.ERROR:
                     case ValidationEvent.FATAL_ERROR:
                        LOGGER.error("Configuration parsing failure at line {}, column {}: {}",
                              event.getLocator().getLineNumber(),
                              event.getLocator().getColumnNumber(),
                              event.getMessage());
                        break;
                     default:
                        LOGGER.error("Invalid configuration validation event!");
                        break;
                  }
                  return false;
               }
            });

         Configuration loadedConfig = null;

         try (InputStream stream = configuration.openConnection().getInputStream())
         {
            loadedConfig = unmarshaller.unmarshal(new StreamSource(stream), Configuration.class).getValue();
         }
         return loadedConfig;
      }

      static void saveConfiguration(Configuration toSave, URL location)
            throws JAXBException, IOException, URISyntaxException
      {
         JAXBContext context = JAXBContext.newInstance("fr.gael.dhus.database.object.config");
         Marshaller marshaller = context.createMarshaller();
         marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);

         File config = new File(location.toURI());
         try (OutputStream stream = new FileOutputStream(config))
         {
            marshaller.marshal(toSave, stream);
            stream.flush();
            stream.close();
         }
      }

      /**
       * gets the currently loaded configuration.
       *
       * @return Loaded configuration
       */
      public Configuration getConf()
      {
         return loadedConfiguration;
      }
   }
}
