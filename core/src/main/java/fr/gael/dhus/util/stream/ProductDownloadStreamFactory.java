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
package fr.gael.dhus.util.stream;

import fr.gael.dhus.database.object.config.source.Source;
import fr.gael.dhus.sync.smart.ProductInfo;
import fr.gael.dhus.sync.smart.SynchronizerRules;
import fr.gael.dhus.util.http.BasicAuthHttpClientProducer;
import fr.gael.dhus.util.http.CountingIWC;
import fr.gael.dhus.util.http.InterruptibleHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProductDownloadStreamFactory implements ProductStreamFactory
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String THREAD_NAME = "ProductDownloadStream";
   private static final String HTTP_HEADER_ETAG = "ETag";
   private static final String HTTP_HEADER_ACCEPT_RANGE = "Accept-Ranges";

   @Override
   public synchronized InputStream generateInputStream(ProductInfo productInfo, Source source,
         SynchronizerRules rules, long skip)
   {
      // generate HTTP client
      int timeout = rules.getTimeout().intValue();
      InterruptibleHttpClient client = new InterruptibleHttpClient(
            new BasicAuthHttpClientProducer(source.getUsername(), source.getPassword(), timeout));

      // generate download url
      String url = productInfo.getDownloadUrl(source.getUrl());

      // generate stream
      boolean canResume;
      String etag;
      HttpResponse headers;
      Thread downloadThread = null;
      try
      {
         headers = client.interruptibleHead(url);
         // check availability of content
         if (headers.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
         {
            return null;
         }
         // retrieve content information
         if (headers.containsHeader(HTTP_HEADER_ETAG))
         {
            etag = headers.getFirstHeader(HTTP_HEADER_ETAG).getValue();
            canResume = headers.containsHeader(HTTP_HEADER_ACCEPT_RANGE);
         }
         else
         {
            etag = null;
            canResume = false;
         }

         Pipe pipe = Pipe.open();
         DownloadTask downloadTask = new DownloadTask(pipe, client, url, etag, skip,
               productInfo.getSize(), rules.getAttempts(), canResume);
         downloadThread = new Thread(downloadTask, THREAD_NAME);
         downloadThread.start();

         return Channels.newInputStream(pipe.source());
      }
      catch (InterruptedException | IOException e)
      {
         if (downloadThread != null)
         {
            downloadThread.interrupt();
         }
         return null;
      }
   }

   private static class DownloadTask implements Runnable
   {
      private static final long RETRY_DELAY = 1_000;

      private final Pipe pipe;
      private final InterruptibleHttpClient client;
      private final String url;
      private final String eTag;
      private final long skip;
      private final long length;
      private final int maxAttempts;
      private final boolean useRange;

      private DownloadTask(Pipe pipe, InterruptibleHttpClient client, String url, String eTag,
            long skip, long length, int maxAttempts, boolean useRange)
      {
         this.pipe = pipe;
         this.client = client;
         this.url = url;
         this.eTag = eTag;
         this.skip = skip;
         this.length = length;
         this.maxAttempts = maxAttempts;
         this.useRange = useRange;
      }

      @Override
      public void run()
      {
         int attempts = 0;
         long expectedTransferredBytes = length - skip;

         Pipe.SinkChannel ouput = pipe.sink();
         CountingIWC counter = new CountingIWC<>(ouput);
         if (useRange)
         {
            while (attempts < maxAttempts && counter.currentCount() < expectedTransferredBytes)
            {
               long downloaded = skip + counter.currentCount();
               try
               {
                  long rangeEnd = length - 1;
                  LOGGER.debug("Downloading {} : Range [{}, {}]", url, downloaded, rangeEnd);
                  client.interruptibleGetRange(url, counter, eTag, downloaded, rangeEnd);
               }
               catch (IOException e)
               {
                  LOGGER.debug("Download {} failed caused by {}: {}", url, e.getClass(), e.getMessage());
                  if (pipe.source().isOpen())
                  {
                     // waiting a few before resume
                     try
                     {
                        Thread.sleep(RETRY_DELAY);
                     }
                     catch (InterruptedException ex)
                     {
                        Thread.currentThread().interrupt();
                     }
                     attempts = attempts + 1;
                  }
                  else
                  {
                     LOGGER.debug("Download stream closed");
                     break;
                  }
               }
               catch (InterruptedException e)
               {
                  LOGGER.debug("Product download transfer of {} interrupted", url);
                  Thread.currentThread().interrupt();
               }
            }
         }
         else if (skip != 0)
         {
            try
            {
               LOGGER.debug("Downloading {}", url);
               client.interruptibleGet(url, counter);
            }
            catch (IOException | InterruptedException e)
            {
               LOGGER.debug("Download {} failed caused by {}: {}", url, e.getClass(), e.getMessage());
            }
         }

         if (counter.currentCount() == expectedTransferredBytes)
         {
            LOGGER.debug("Download {} completed successfully", url);
         }
         else
         {
            LOGGER.debug("Incomplete download of {}", url);
         }

         try
         {
            pipe.sink().close();
         }
         catch (IOException ex)
         {
            LOGGER.warn("Cannot close pipe: {}", ex.getMessage());
         }
      }
   }
}
