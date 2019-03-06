/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
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
package fr.gael.dhus.server.http.webapp.saml;

import fr.gael.dhus.server.http.webapp.WebApp;
import fr.gael.dhus.server.http.webapp.WebApplication;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.stereotype.Component;

@Component
@WebApp(name = "saml")
public class SamlWebapp extends WebApplication
{
   private static final Logger LOGGER = LogManager.getLogger(SamlWebapp.class);

   @Override
   public void configure(String dest_folder) throws IOException
   {
      String configurationFolder = "fr/gael/dhus/server/http/webapp/saml";
      URL u = Thread.currentThread().getContextClassLoader().getResource(configurationFolder);
      if (u != null && "jar".equals(u.getProtocol()))
      {
         extractJarFolder(u, configurationFolder, dest_folder);
      }
      else if (u != null)
      {
         File webAppFolder = new File(dest_folder);
         copyFolder(new File(u.getFile()), webAppFolder);
      }
   }

   @Override
   public boolean isActive()
   {
      String idPName = System.getProperty("dhus.saml.idp.name");
      if (idPName == null || idPName.isEmpty())
      {
         LOGGER.warn("SamlWebapp can not be started as the 'dhus.saml.idp.name' is missing.");
         return false;
      }

      String idpFile = System.getProperty("dhus.saml.idp.file");
      String idpUrl = System.getProperty("dhus.saml.idp.url");
      File idpF = (idpFile != null && !idpFile.isEmpty()) ? new File(idpFile) : null;
      URL idpU = null;
      try
      {
         idpU = (idpUrl != null && !idpUrl.isEmpty()) ? new URL(idpUrl) : null;
      }
      catch (MalformedURLException e)
      {
         LOGGER.warn("SamlWebapp can not be started as the 'dhus.saml.idp.url' is not well formed.");
         return false;
      }
      if ((idpF == null || !idpF.exists()) && idpU == null)
      {
         LOGGER.warn("SamlWebapp can not be started as the metadata place is not defined (dhus.saml.idp.file/url).");
         return false;
      }

      String spId = System.getProperty("dhus.saml.sp.id");
      if (spId == null || spId.isEmpty())
      {
         LOGGER.warn("SamlWebapp can not be started as the 'dhus.saml.sp.id' is missing.");
         return false;
      }

      String keystoreFile = System.getProperty("dhus.saml.keystore.file");
      File ksF = (keystoreFile != null && !keystoreFile.isEmpty()) ? new File(keystoreFile) : null;
      if (ksF == null || !ksF.exists())
      {
         LOGGER.warn("SamlWebapp can not be started as the 'dhus.saml.keystore.file' is missing.");
         return false;
      }

      String keystorePass = System.getProperty("dhus.saml.keystore.storePass");
      if (keystorePass == null || keystorePass.isEmpty())
      {
         LOGGER.warn("SamlWebapp can not be started as the 'dhus.saml.keystore.storePass' is missing.");
         return false;
      }

      String keystoreDef = System.getProperty("dhus.saml.keystore.defaultKey");
      if (keystoreDef == null || keystoreDef.isEmpty())
      {
         LOGGER.warn("SamlWebapp can not be started as the 'dhus.saml.keystore.defaultKey' is missing.");
         return false;
      }

      String keystoreDefPass = System.getProperty("dhus.saml.keystore.defaultPassword");
      if (keystoreDefPass == null || keystoreDefPass.isEmpty())
      {
         LOGGER.warn("SamlWebapp can not be started as the 'dhus.saml.keystore.defaultPassword' is missing.");
         return false;
      }

      return true;
   }

   @Override
   public boolean hasWarStream()
   {
      return false;
   }

   @Override
   public InputStream getWarStream()
   {
      return null;
   }

   @Override
   public void checkInstallation() throws Exception
   {
   }
}
