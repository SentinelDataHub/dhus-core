/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018 GAEL Systems
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
package fr.gael.dhus.util.http;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

/**
 * Creates a client producer that produces HTTP Basic auth aware clients.
 */
public class BasicAuthHttpClientProducer implements HttpAsyncClientProducer
{
   private final String username;
   private final String password;
   private final int timeout;

   public BasicAuthHttpClientProducer(final String username, final String password)
   {
      this(username, password, 0);
   }

   public BasicAuthHttpClientProducer(final String username, final String password, final int timeout)
   {
      if (timeout < 0)
      {
         throw new IllegalArgumentException("Timeout configuration error");
      }

      this.username = username;
      this.password = password;
      this.timeout = timeout;
   }

   @Override
   public CloseableHttpAsyncClient generateClient()
   {
      CredentialsProvider credsProvider = new BasicCredentialsProvider();
      credsProvider.setCredentials(new AuthScope(AuthScope.ANY),
            new UsernamePasswordCredentials(username, password));

      RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
      if (timeout == 0)
      {
         requestConfigBuilder.setSocketTimeout(Timeouts.SOCKET_TIMEOUT)
               .setConnectTimeout(Timeouts.CONNECTION_TIMEOUT)
               .setConnectionRequestTimeout(Timeouts.CONNECTION_REQUEST_TIMEOUT);
      }
      else
      {
         requestConfigBuilder.setSocketTimeout(timeout)
               .setConnectTimeout(timeout)
               .setConnectionRequestTimeout(timeout);
      }

      requestConfigBuilder.setCookieSpec(CookieSpecs.DEFAULT);

      CloseableHttpAsyncClient res = HttpAsyncClients.custom()
            .setDefaultCredentialsProvider(credsProvider)
            .setDefaultRequestConfig(requestConfigBuilder.build())
            .build();
      res.start();

      return res;
   }
}
