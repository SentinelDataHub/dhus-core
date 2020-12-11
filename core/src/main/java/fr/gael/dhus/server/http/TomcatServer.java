/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013-2017,2019 GAEL Systems
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
package fr.gael.dhus.server.http;

import fr.gael.dhus.server.http.webapp.WebApplication;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import org.apache.catalina.Container;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.startup.Constants;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat.DefaultWebXmlListener;
import org.apache.catalina.valves.RemoteAddrValve;
import org.apache.catalina.valves.RemoteIpValve;

import org.apache.commons.io.FileUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.scan.StandardJarScanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TomcatServer
{
   private static final Logger LOGGER = LogManager.getLogger();

   @Autowired
   private ConfigurationManager configurationManager;

   private String tomcatpath;

   private Catalina cat;

   /**
    * Initialises the Catalina servlet engine.
    *
    * @throws TomcatException encapsulate any exception thrown by the init procedure
    */
   public void init() throws TomcatException
   {
      tomcatpath = configurationManager.getTomcatConfiguration().getPath();
      final Path extractDirectory = Paths.get(tomcatpath);

      LOGGER.info("Starting tomcat in {}", extractDirectory.toAbsolutePath());

      try
      {
         // delete tomcat's conf directory structure
         if (Files.exists(extractDirectory))
         {
            LOGGER.debug("Clean extractDirectory");
            FileUtils.deleteDirectory(extractDirectory.toFile());
         }

         // re-create tomcat's conf directory structure
         Files.createDirectories(extractDirectory);

         Path confDir = extractDirectory.resolve("conf");
         Files.createDirectory(confDir);
         Files.createDirectory(extractDirectory.resolve("logs"));
         Files.createDirectory(extractDirectory.resolve("webapps"));
         Files.createDirectory(extractDirectory.resolve("work"));
         Path tmpDir = extractDirectory.resolve("temp");
         Files.createDirectory(tmpDir);

         System.setProperty("java.io.tmpdir", tmpDir.toAbsolutePath().toString());
         System.setProperty("catalina.base", extractDirectory.toAbsolutePath().toString());
         System.setProperty("catalina.home", extractDirectory.toAbsolutePath().toString());

         try (InputStream in = ClassLoader.getSystemResource("server.xml").openStream())
         {
            Files.copy(in, confDir.resolve("server.xml"), StandardCopyOption.REPLACE_EXISTING);
         }

         // The default web.xml extracted in tomcat's conf dir would be overridden by the following instruction in install(WebApplication) below:
         // ctxCfg.setDefaultWebXml("fr/gael/dhus/server/http/global-web.xml");
         try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("conf/web.xml"))
         {
            if (in != null)
            {
               Files.copy(in, confDir.resolve("web.xml"), StandardCopyOption.REPLACE_EXISTING);
            }
         }

         cat = new Catalina();
      }
      catch (IOException | RuntimeException ex)
      {
         throw new TomcatException("Cannot initalize Tomcat environment", ex);
      }

      Runtime.getRuntime().addShutdownHook(new TomcatShutdownHook());
   }

   /**
    * This method Starts the Tomcat server.
    *
    * @throws TomcatException encapsulate any exception thrown by the init procedure
    */
   public void start() throws TomcatException
   {
      if (cat == null)
      {
         init();
      }
      cat.start();
   }

   /**
    * This method Stops the Tomcat server.
    *
    * @throws TomcatException encapsulate any exception thrown by the stop procedure
    */
   public void stop() throws TomcatException
   {
      // Stop the embedded server
      cat.stop();
      cat = null;
   }

   /**
    * Is the tomcat server started.
    *
    * @return true if tomcat is running
    */
   public boolean isRunning()
   {
      return cat != null;
   }

   protected class TomcatShutdownHook extends Thread
   {
      protected TomcatShutdownHook() {}

      @Override
      public void run()
      {
         try
         {
            TomcatServer.this.stop();
         }
         catch (Throwable ex)
         {
            ExceptionUtils.handleThrowable(ex);
            LOGGER.error("Fail to properly shutdown Tomcat: {}", ex.getMessage());
         }
      }
   }

   /**
    * Installs the given web application in the Tomcat server.
    *
    * @param web_application to install (must not be null)
    * @throws TomcatException installation failed
    */
   public void install(WebApplication web_application)
         throws TomcatException
   {
      if (!web_application.isActive())
      {
         LOGGER.info("Skipping '{}', because it is disabled", web_application);
         return;
      }
      LOGGER.info("Installing webapp {}", web_application);
      String appName = web_application.getName();
      String folder;

      if (appName.trim().isEmpty())
      {
         folder = "ROOT";
      }
      else
      {
         folder = appName;
      }

      try
      {
         Path webappDestFolder = Paths.get(tomcatpath, "webapps", folder);
         if (web_application.hasWarStream())
         {
            InputStream stream = web_application.getWarStream();
            if (stream == null)
            {
               throw new TomcatException("Cannot install webApplication " + appName
                     + ", the referenced war file does not exist");
            }
            try (JarInputStream jis = new JarInputStream(stream))
            {
               JarEntry file;
               while ((file = jis.getNextJarEntry()) != null)
               {
                  Path exEntry = webappDestFolder.resolve(file.getName());
                  if (file.isDirectory())
                  { // if it is a directory, create it
                     Files.createDirectories(exEntry);
                     continue;
                  }
                  if (!Files.exists(exEntry.getParent()))
                  {
                     Files.createDirectories(exEntry.getParent());
                  }
                  Files.copy(jis, exEntry, StandardCopyOption.REPLACE_EXISTING);
               }
            }
         }
         web_application.configure(webappDestFolder.toAbsolutePath().toString());

         StandardEngine engine = (StandardEngine) cat.getServer().findServices()[0].getContainer();
         Container container = engine.findChild(engine.getDefaultHost());

         StandardContext ctx = new StandardContext();
         String url = (appName.isEmpty() ? "" : "/") + appName;
         ctx.setName(appName);
         ctx.setPath(url);
         ctx.setDocBase(webappDestFolder.toString());

         ctx.addLifecycleListener(new DefaultWebXmlListener());
         ctx.setConfigFile(getWebappConfigFile(webappDestFolder, url));

         String extPath = configurationManager.getServerConfiguration().getExternalPath();
         if (extPath != null && !extPath.isEmpty())
         {
            ctx.setSessionCookiePath(extPath);
         }
         else
         {
            ctx.setSessionCookiePath("/");
         }

         ContextConfig ctxCfg = new ContextConfig();
         ctx.addLifecycleListener(ctxCfg);

         ctxCfg.setDefaultWebXml("fr/gael/dhus/server/http/global-web.xml");

         StandardJarScanner.class.cast(ctx.getJarScanner()).setScanClassPath(false);
         container.addChild(ctx);

         List<String> welcomeFiles = web_application.getWelcomeFiles();

         for (String welcomeFile: welcomeFiles)
         {
            ctx.addWelcomeFile(welcomeFile);
         }

         if (web_application.getAllow() != null
          || web_application.getDeny()  != null)
         {
            RemoteIpValve valve = new RemoteIpValve();
            valve.setRemoteIpHeader("x-forwarded-for");
            valve.setProxiesHeader("x-forwarded-by");
            valve.setProtocolHeader("x-forwarded-proto");
            ctx.addValve(valve);

            RemoteAddrValve valve_addr = new RemoteAddrValve();
            valve_addr.setAllow(web_application.getAllow());
            valve_addr.setDeny(web_application.getDeny());
            ctx.addValve(valve_addr);
         }

         web_application.checkInstallation();
      }
      catch (Exception e)
      {
         throw new TomcatException("Cannot install webApplication " + appName, e);
      }
   }

   /**
    * Blocks until the Tomcat server dies.
    */
   public void await()
   {
      cat.getServer().await();
   }

   /**
    * @return port of this running Tomcat server
    */
   public int getPort()
   {
      Connector connector = cat.getServer().findServices()[0].findConnectors()[0];
      return connector.getPort();
   }

   /**
    * If this Tomcat server has more than one Connector, returns the port of the second Connector,
    * otherwise behaves like {@link #getPort()}.
    *
    * @return alternative port of this running Tomcat server
    */
   public int getAltPort()
   {
      Connector[] connectors = cat.getServer().findServices()[0].findConnectors();
      if (connectors.length >= 2)
      {
         return connectors[1].getPort();
      }
      return connectors[0].getPort();
   }

   public String getPath()
   {
      return this.tomcatpath;
   }

   protected URL getWebappConfigFile(Path docBase, String url)
   {
      if (Files.isDirectory(docBase))
      {
         return getWebappConfigFileFromDirectory(docBase);
      }
      else
      {
         return getWebappConfigFileFromJar(docBase);
      }
   }

   private URL getWebappConfigFileFromDirectory(Path docBase)
   {
      Path webAppContextXml = docBase.resolve(Constants.ApplicationContextXml);
      if (Files.exists(webAppContextXml))
      {
         try
         {
            return webAppContextXml.toUri().toURL();
         }
         catch (MalformedURLException e)
         {
            LOGGER.warn("Unable to determine web application context.xml {}", docBase, e);
         }
      }
      return null;
   }

   private URL getWebappConfigFileFromJar(Path docBase)
   {
      URL result = null;
      JarFile jar = null;
      try
      {
         jar = new JarFile(docBase.toFile());
         JarEntry entry = jar.getJarEntry(Constants.ApplicationContextXml);
         if (entry != null)
         {
            result = new URL("jar:" + docBase.toUri().toString() + "!/" + Constants.ApplicationContextXml);
         }
      }
      catch (IOException e)
      {
         LOGGER.warn("Unable to determine web application context.xml {}", docBase, e);
      }
      finally
      {
         if (jar != null)
         {
            try
            {
               jar.close();
            }
            catch (IOException suppressed) {}
         }
      }
      return result;
   }
}
