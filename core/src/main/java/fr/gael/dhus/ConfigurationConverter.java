/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2018 GAEL Systems
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
import fr.gael.dhus.database.object.config.system.DatabaseConfiguration;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dhus.store.datastore.config.HfsDataStoreConf;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
            hfsDsc.setReadOnly(false);

            conf.getDataStores().getDataStore().add(hfsDsc);
         }

         String errorPath = element.getAttribute("errorPath");
         conf.getSystemConfiguration().getArchiveConfiguration().setErrorPath(errorPath);
      }
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
