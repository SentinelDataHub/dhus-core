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
package fr.gael.dhus.database.object;

import java.net.URL;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("openstack")
public class OpenstackDataStoreConf extends DataStoreConfiguration
{
   @Column(name = "PROVIDER", nullable = false, length = 32)
   private String provider;

   @Column(name = "IDENTITY", nullable = false, length = 32)
   private String identity;

   @Column(name = "CREDENTIAL", nullable = false, length = 64)
   private String credential;

   @Column(name = "URL", nullable = false, length = 512)
   private URL url;

   @Column(name = "REGION", nullable = false, length = 64)
   private String region;

   @Column(name = "CONTAINER", nullable = false, length = 64)
   private String container;

   public String getProvider()
   {
      return provider;
   }

   public void setProvider(String provider)
   {
      this.provider = provider;
   }

   public String getIdentity()
   {
      return identity;
   }

   public void setIdentity(String identity)
   {
      this.identity = identity;
   }

   public String getCredential()
   {
      return credential;
   }

   public void setCredential(String credential)
   {
      this.credential = credential;
   }

   public URL getUrl()
   {
      return url;
   }

   public void setUrl(URL url)
   {
      this.url = url;
   }

   public String getRegion()
   {
      return region;
   }

   public void setRegion(String region)
   {
      this.region = region;
   }

   public String getContainer()
   {
      return container;
   }

   public void setContainer(String container)
   {
      this.container = container;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null)
      {
         return false;
      }
      if (this == obj)
      {
         return true;
      }
      if (obj instanceof OpenstackDataStoreConf)
      {
         OpenstackDataStoreConf other = (OpenstackDataStoreConf) obj;
         return getName().equals(other.getName())
               || (provider.equals(other.provider) && (url.equals(other.url))
               && region.equals(other.region)
               && container.equals(other.container));
      }
      return false;
   }

   @Override
   public int hashCode()
   {
      return getName().hashCode() + provider.hashCode() + url.hashCode()
            + region.hashCode() + container.hashCode();
   }
}
