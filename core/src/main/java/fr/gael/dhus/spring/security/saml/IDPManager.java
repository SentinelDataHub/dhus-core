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
package fr.gael.dhus.spring.security.saml;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensaml.saml2.metadata.provider.AbstractMetadataProvider;
import org.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.parse.ParserPool;

import org.springframework.stereotype.Component;

@Component(value = "idpManager")
public class IDPManager
{
   private static final Logger LOGGER = LogManager.getLogger();

   public List<MetadataProvider> getMetadataProviders(ParserPool parser)
         throws MetadataProviderException
   {
      String idpName = System.getProperty("dhus.saml.idp.name", null);
      String idpFile = System.getProperty("dhus.saml.idp.file", null);
      String idpURL = System.getProperty("dhus.saml.idp.url", null);

      if (idpName == null || (idpURL == null && idpFile == null))
      {
         LOGGER.warn("No idp configured.");
         return Collections.emptyList();
      }

      AbstractMetadataProvider provider;

      if (idpURL != null && !idpURL.isEmpty())
      {
         provider = new HTTPMetadataProvider(idpURL, 5000);
      }
      else
      {
         provider = new FilesystemMetadataProvider(new File(idpFile));
      }
      provider.setParserPool(parser);

      return Collections.<MetadataProvider>singletonList(provider);
   }

   public String getIdpName()
   {
      return System.getProperty("dhus.saml.idp.name");
   }
}
