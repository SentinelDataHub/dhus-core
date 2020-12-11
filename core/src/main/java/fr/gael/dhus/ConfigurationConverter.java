/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2020 GAEL Systems
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
package fr.gael.dhus;

import fr.gael.dhus.database.object.config.Configuration;
import fr.gael.dhus.database.object.config.cron.Cron;
import fr.gael.dhus.database.object.config.scanner.FileScannerConf;
import fr.gael.dhus.database.object.config.scanner.FtpScannerConf;
import fr.gael.dhus.database.object.config.scanner.ScannerConfiguration;
import fr.gael.dhus.database.object.config.system.DatabaseConfiguration;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dhus.scanner.config.ScannerInfo;
import org.dhus.store.datastore.config.AsyncDataStoreConf;
import org.dhus.store.datastore.config.DataStoreConf;
import org.dhus.store.datastore.config.DataStoreRestriction;
import org.dhus.store.datastore.config.GmpDataStoreConf;
import org.dhus.store.datastore.config.HfsDataStoreConf;
import org.dhus.store.datastore.config.NamedDataStoreConf;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Command line tool to clean and make valid (according the the XML schema) a DHuS config file
 * dhus.xml.
 */
public class ConfigurationConverter
{
   /**
    * Converts XML config file from previous releases.
    *
    * @param input  input config file (usually etc/dhus.xml)
    * @param backup optional backup location (input will be moved at that location), may be null
    *
    * @throws JAXBException when input is wrong
    * @throws IOException   when input cannot be accessed
    * @throws SAXException  XML cannot be parsed
    * @throws ParserConfigurationException should not be thrown
    */
   public static void convert(Path input, Path backup)
         throws IOException, JAXBException, SAXException, ParserConfigurationException
   {
      Objects.requireNonNull(input, "input file cannot be null");
      System.out.println("Loading configuration from " + input);

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      DocumentBuilder docBuilder = dbf.newDocumentBuilder();
      docBuilder.setErrorHandler(new BasicErrorHandler());
      Document doc = docBuilder.parse((input.toUri().toString()));

      JAXBContext context = JAXBContext.newInstance("fr.gael.dhus.database.object.config");
      Unmarshaller unmarshaller = context.createUnmarshaller();

      Configuration loadedConfig = unmarshaller.unmarshal(doc, Configuration.class).getValue();

      ConfigurationManager.postLoad(loadedConfig);
      convertDatabaseConfiguration(loadedConfig.getSystemConfiguration().getDatabaseConfiguration(), doc);
      convertOldIncomingToHFS(loadedConfig, doc);
      convertOldScannerConf(loadedConfig, doc);
      convertOldGmpDataStoresConf(loadedConfig, doc);
      convertOldAsyncDataStoreMaxQueuedRequest(loadedConfig, doc);
      convertAsyncQuotas(loadedConfig, doc);
      convertDataStoreReadOnly(loadedConfig, doc);

      if (backup != null)
      {
         if (Files.exists(backup))
         {
            Files.delete(backup);
         }
         Files.move(input, backup);
      }

      // Marshall conf to XML
      Marshaller marshaller = context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      try (OutputStream stream = new FileOutputStream(input.toFile()))
      {
         marshaller.marshal(loadedConfig, stream);
         stream.flush();
         stream.close();
      }
      System.out.println("Configuration converted in " + input);
   }

   private static void convertOldScannerConf(Configuration loadedConfig, Document doc)
   {
      Objects.requireNonNull(loadedConfig);
      Objects.requireNonNull(doc);
      boolean sourceRemove = false;
      boolean active = false;
      String schedule = "0 0 22 ? * *";

      // Move sourceRemove to archive conf
      Node node = doc.getElementsByTagNameNS("*", "fileScanners").item(0);
      if (node != null && Element.class.isAssignableFrom(node.getClass()))
      {
         Element element = Element.class.cast(node);
         sourceRemove = Boolean.parseBoolean(element.getAttribute("sourceRemove"));
         active = Boolean.valueOf(element.getAttribute("active"));
         schedule = element.getAttribute("schedule");
      }

      // Is there old-style scanner conf to convert?
      NodeList scannerNodes = doc.getElementsByTagNameNS("fr.gael.dhus.database.object.config.scanner", "scanner");
      if (scannerNodes.getLength() <= 0)
      {
         return;
      }
      if (scannerNodes.item(0).getAttributes().getNamedItemNS("http://www.w3.org/2001/XMLSchema-instance", "type") != null)
      {
         return;
      }

      // Convert old scanner conf
      List<ScannerConfiguration> newScanners = new ArrayList<>();
      for (int scannersIter = 0; scannersIter < scannerNodes.getLength(); scannersIter++)
      {
         Node scannerNode = scannerNodes.item(scannersIter);

         NodeList nl = scannerNode.getChildNodes();
         ScannerInfo si = null;
         // Initialize correct type based on URL
         for (int elemIter = 0; elemIter < nl.getLength(); elemIter++)
         {
            Node child = nl.item(elemIter);
            if ("url".equalsIgnoreCase(child.getLocalName()))
            {
               String url = child.getTextContent();
               if (url.startsWith("ftp") || url.startsWith("sftp:"))
               {
                  si = new FtpScannerConf();
               }
               else
               {
                  si = new FileScannerConf();
               }

               break;
            }
         }

         // Copy old value
         if (si == null)
         {
            si = new FileScannerConf();
         }
         for (int elemIter = 0; elemIter < nl.getLength(); elemIter++)
         {
            Node child = nl.item(elemIter);
            if (child.getNodeType() != Node.ELEMENT_NODE)
            {
               continue;
            }

            String nodeName = child.getLocalName();
            switch (nodeName.toLowerCase())
            {
               case "id":
                  si.setId(Long.parseLong(child.getTextContent()));
                  break;
               case "url":
                  si.setUrl(child.getTextContent());
                  break;
               case "pattern":
                  si.setPattern(child.getTextContent());
                  break;
               case "collections":
               {
                  NodeList collectionNL = child.getChildNodes();
                  ScannerConfiguration.Collections collections = new ScannerConfiguration.Collections();
                  if (collectionNL.getLength() > 0)
                  {
                     for (int collecIter = 0; collecIter < collectionNL.getLength(); collecIter++)
                     {
                        Node collection = collectionNL.item(collecIter);
                        if ("collection".equalsIgnoreCase(collection.getLocalName()))
                        {
                           String collectionName = collection.getTextContent();
                           collections.getCollection().add(collectionName);
                        }
                     }
                     si.setCollections(collections);
                  }
                  break;
               }
               case "username":
               {
                  if (si instanceof FtpScannerConf)
                  {
                     si.setUsername(child.getTextContent());
                  }
                  break;
               }
               case "password":
               {
                  if (si instanceof FtpScannerConf)
                  {
                     si.setPassword(child.getTextContent());
                  }
                  break;
               }
               default:
                  break;
            }
         }

         Cron cron = new Cron();
         cron.setActive(active);
         cron.setSchedule(schedule);
         si.setCron(cron);

         si.setSourceRemove(sourceRemove);
         newScanners.add(si);
      }

      loadedConfig.getScanners().setScanner(newScanners);
   }

   /**
    * Converts pre-externalisation database configuration
    *
    * @param dbConf database configuration to update
    * @param doc    source document containing the pre-ext database configuration
    */
   public static void convertDatabaseConfiguration(DatabaseConfiguration dbConf, Document doc)
   {
      Objects.requireNonNull(dbConf);
      Objects.requireNonNull(doc);
      Node node = doc.getElementsByTagNameNS("*", "database").item(0);
      if (node != null && Element.class.isAssignableFrom(node.getClass()))
      {
         Element element = Element.class.cast(node);
         String path = element.getAttribute("path");
         String settings = element.getAttribute("settings");
         String cryptType = element.getAttribute("cryptType");
         String cryptKey = element.getAttribute("cryptKey");

         if (path != null && !path.trim().isEmpty())
         {
            StringBuilder jdbcUrl = new StringBuilder("jdbc:hsqldb:file:");
            jdbcUrl.append(path);
            if (settings != null && !settings.trim().isEmpty())
            {
               jdbcUrl.append(';').append(settings);
            }
            if (cryptType != null && !cryptType.trim().isEmpty())
            {
               jdbcUrl.append(';').append("crypt_type=").append(cryptType);
            }
            if (cryptKey != null && !cryptKey.trim().isEmpty())
            {
               jdbcUrl.append(';').append("crypt_key=").append(cryptKey);
            }
            dbConf.setJDBCUrl(jdbcUrl.toString());
         }
      }
      // JDBCDriver, hibernateDialect, login and password are handled via default values
   }

   /** Name of the HFS DataStore created by {@link #convertOldIncomingToHFS(Configuration, Document)}. */
   public static final String INCOMING_NAME = "OldIncomingAdapter";

   /**
    * Converts system>incoming configuration to an HFS DataStore.
    *
    * @param conf configuration to update
    * @param doc  source document containing the deprecated configuration
    */
   public static void convertOldIncomingToHFS(Configuration conf, Document doc)
   {
      Objects.requireNonNull(conf);
      Objects.requireNonNull(doc);

      Node node = doc.getElementsByTagNameNS("*", "incoming").item(0);
      if (node != null && Element.class.isAssignableFrom(node.getClass()))
      {
         Element element = Element.class.cast(node);
         String path = element.getAttribute("path");
         String maxFileNo = element.getAttribute("maxFileNo");
         String maxItems = element.getAttribute("maxItems");

         // TODO: check if an HFS datastore named "OldIncomingAdapter" exists
         if (path != null && !path.trim().isEmpty())
         {
            // create an HFS datastore with the properties of the incoming
            HfsDataStoreConf hfsDsc = new HfsDataStoreConf();
            hfsDsc.setName(INCOMING_NAME);
            if (maxFileNo != null && !maxFileNo.trim().isEmpty())
            {
               try
               {
                  hfsDsc.setMaxFileNo(Integer.parseInt(maxFileNo));
               }
               catch (NumberFormatException suppressed) {}
            }
            if (maxItems != null && !maxItems.trim().isEmpty())
            {
               try
               {
                  hfsDsc.setMaxItems(Integer.parseInt(maxItems));
               }
               catch (NumberFormatException suppressed) {}
            }
            hfsDsc.setPath(path);
            hfsDsc.setPriority(-1);
            hfsDsc.setRestriction(DataStoreRestriction.NONE);

            conf.getDataStores().getDataStore().add(hfsDsc);
         }

         String errorPath = element.getAttribute("errorPath");
         conf.getSystemConfiguration().getArchiveConfiguration().setErrorPath(errorPath);
      }
   }

   /**
    * Handles the renaming of some attributes related to async DataStores:
    * - gmpRepoLocation attribute renamed to repoLocation.
    * - isMaster attribute to become a child element.
    *
    * Also handles the renaming new optional configuration options that need to be present
    * in GMPDataStore in order to not break previous behaviors:
    * - PatternReplaceIn
    * - PatternReplaceOut
    *
    * @param conf configuration to update
    * @param doc  source document containing the deprecated configuration
    */
   public static void convertOldGmpDataStoresConf(Configuration conf, Document doc)
   {
      Objects.requireNonNull(conf);
      Objects.requireNonNull(doc);

      NodeList nodes = doc.getElementsByTagNameNS("*", "dataStore");
      List<DataStoreConf> dataStoreConfList = conf.getDataStores().getDataStore();

      for (int i = 0; i < nodes.getLength(); i++)
      {
         Node node = nodes.item(i);
         if (node != null && Element.class.isAssignableFrom(node.getClass()))
         {
            Element element = Element.class.cast(node);
            String name = element.getAttribute("name");

            for (DataStoreConf dataStoreConf: dataStoreConfList)
            {
               if (dataStoreConf instanceof GmpDataStoreConf)
               {
                  GmpDataStoreConf gmpDataStoreConf = (GmpDataStoreConf) dataStoreConf;

                  if (name.equals(gmpDataStoreConf.getName()))
                  {
                     NodeList children = element.getChildNodes();
                     boolean hasIsMaster = false;
                     for (int it=0; it < children.getLength(); it++)
                     {
                        Node child = children.item(it);
                        if ("gmpRepoLocation".equals(child.getLocalName()))
                        {
                           String gmpRepoLocation = child.getTextContent();
                           gmpDataStoreConf.setRepoLocation(gmpRepoLocation);
                           continue;
                        }

                        if ("isMaster".equals(child.getLocalName()))
                        {
                           hasIsMaster = true;
                           continue;
                        }
                     }

                     if (!hasIsMaster)
                     {
                        gmpDataStoreConf.setIsMaster(Boolean.valueOf(element.getAttribute("isMaster")));
                     }
                  }
               }
            }
         }
      }
   }

   public static void convertAsyncQuotas(Configuration conf, Document doc)
   {
      Objects.requireNonNull(conf);
      Objects.requireNonNull(doc);

      NodeList nodes = doc.getElementsByTagNameNS("*", "dataStore");
      List<DataStoreConf> dataStoreConfList = conf.getDataStores().getDataStore();

      for (DataStoreConf dataStoreConf: dataStoreConfList)
      {
         if (dataStoreConf instanceof AsyncDataStoreConf)
         {
            AsyncDataStoreConf asyncDSconf = (AsyncDataStoreConf) dataStoreConf;
            Optional<Element> element = streamableElementList(nodes)
                  .filter((e) -> e.getAttribute("name").equals(asyncDSconf.getName()))
                  .findFirst();

            if (element.isPresent())
            {
               Element child = firstElementByName(element.get().getChildNodes(), "quotas");
               if (child != null)
               {
                  String maxQPU = child.getAttribute("maxQueryPerUser");
                  if (maxQPU != null && !maxQPU.isEmpty())
                  {
                     try
                     {
                        int maxQueryInParallel = Integer.parseInt(maxQPU);
                        asyncDSconf.setMaxParallelFetchRequestsPerUser(maxQueryInParallel);
                     }
                     catch (NumberFormatException suppressed) {}
                  }
               }
            }
         }
      }
   }

   /**
    * Converts old property of AsyncDataStores "maxQueuedRequest" into new properties
    * "maxPendingRequests" and "maxRunningRequests", using the old value for both.
    *
    * @param conf configuration to update
    * @param doc  source document containing the deprecated configuration
    */
   public static void convertOldAsyncDataStoreMaxQueuedRequest(Configuration conf, Document doc)
   {
      Objects.requireNonNull(conf);
      Objects.requireNonNull(doc);

      NodeList nodes = doc.getElementsByTagNameNS("*", "dataStore");
      List<DataStoreConf> dataStoreConfList = conf.getDataStores().getDataStore();

      streamableElementList(nodes).forEach((Element element) ->
      {
         String name = element.getAttribute("name");

         AsyncDataStoreConf asyncDSConf = dataStoreConfList.stream()
               .filter((dsc) -> AsyncDataStoreConf.class.isAssignableFrom(dsc.getClass()))
               .<AsyncDataStoreConf>map(AsyncDataStoreConf.class::cast)
               .filter((dsc) -> dsc.getName().equals(name))
               .findFirst().orElse(null);

         if (asyncDSConf != null)
         {
            Element child = firstElementByName(element.getChildNodes(), "maxQueuedRequest");
            if (child != null)
            {
               String maxQueuedRequest = child.getTextContent();
               try
               {
                  asyncDSConf.setMaxPendingRequests(Integer.decode(maxQueuedRequest));
                  asyncDSConf.setMaxRunningRequests(Integer.decode(maxQueuedRequest));
               }
               catch (NumberFormatException suppressed) {}
            }
         }
      });
   }

   /**
    * Handles the renaming of restriction parameter related to DataStores:
    *
    * @param conf configuration to update
    * @param doc  source document containing the deprecated configuration
    */
   public static void convertDataStoreReadOnly(Configuration conf, Document doc)
   {
      Objects.requireNonNull(conf);
      Objects.requireNonNull(doc);

      NodeList nodes = doc.getElementsByTagNameNS("*", "dataStore");
      List<DataStoreConf> dataStoreConfList = conf.getDataStores().getDataStore();

      streamableElementList(nodes).forEach((Element element) ->
      {
         // do not override existing restriction
         if (element.getAttribute("restriction").isEmpty())
         {
            String name = element.getAttribute("name");
            String readOnly = element.getAttribute("readOnly");

            dataStoreConfList.stream()
               .filter((dsc) -> NamedDataStoreConf.class.isAssignableFrom(dsc.getClass()))
               .<NamedDataStoreConf>map(NamedDataStoreConf.class::cast)
               .filter((dsc) -> dsc.getName().equals(name))
               .findFirst()
               .ifPresent(dsc -> dsc.setRestriction(Boolean.valueOf(readOnly) ? DataStoreRestriction.REFERENCES_ONLY : DataStoreRestriction.NONE));

         }
      });
   }

   /* Returns a stream of Elements from the given NodeList. */
   private static Stream<Element> streamableElementList(final NodeList nodes)
   {
      if (nodes == null)
      {
         return Stream.empty();
      }
      List<Node> nodeList = new AbstractList<Node>()
      {
         @Override
         public Node get(int index)
         {
            return nodes.item(index);
         }

         @Override
         public int size()
         {
            return nodes.getLength();
         }
      };
      return nodeList.stream()
            .filter((e) -> Element.class.isAssignableFrom(e.getClass()))
            .<Element>map(Element.class::cast);
   }

   /* Returns the first element whose name equals `name`. */
   private static Element firstElementByName(NodeList nodes, String name)
   {
      return streamableElementList(nodes)
            .filter((e) -> e.getLocalName().equals(name))
            .findFirst()
            .orElse(null);
   }

   public static void main(String[] args)
         throws IOException, JAXBException, SAXException, ParserConfigurationException
   {
      if (args.length != 1)
      {
         System.err.println("ConfigurationConverter must have a dhus configuration file path as input (dhus.xml)");
         return;
      }
      Path input = Paths.get(args[0]);
      Path bakup = Paths.get(args[0] + "_old");

      convert(input, bakup);
   }

   /** ErrorHandler used to load configuration XML document. */
   private static class BasicErrorHandler implements ErrorHandler
   {
      private void logError(SAXParseException ex) throws SAXParseException
      {
         System.err.printf("Configuration parsing failure at line %d, column %d: %s",
               ex.getLineNumber(), ex.getColumnNumber(), ex.getMessage());
         throw ex;
      }

      @Override
      public void warning(SAXParseException exception) throws SAXException
      {
         logError(exception);
      }

      @Override
      public void error(SAXParseException exception) throws SAXException
      {
         logError(exception);
      }

      @Override
      public void fatalError(SAXParseException exception) throws SAXException
      {
         logError(exception);
      }
   }

}
