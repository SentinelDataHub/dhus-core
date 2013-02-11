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
package fr.gael.dhus.sync.smart.download;

import fr.gael.dhus.util.http.CountingIWC;
import fr.gael.dhus.util.http.InterruptibleHttpClient;

import java.io.IOException;
import java.nio.channels.Pipe;
import java.util.Formatter;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Performs a download from a remote DHuS, and forward transferred bytes in the
 * associated {@link java.nio.channels.Pipe}.
 */
class ProductDownloadTask implements Runnable
{
   private static final Logger LOGGER = LogManager.getLogger();

   private static final String HTTP_HEADER_CAUSE_MESSAGE = "cause-message";

   private final InterruptibleHttpClient httpClient;
   private final Pipe pipe;
   private final String url;
   private final String eTag;
   private final long skip;
   private final long contentLength;
   private final boolean useRange;

   private int attempts;

   /**
    * Constructs a <em>ProductDownloadTask</em> with the specific arguments.
    *
    * @param client        HTTP client
    * @param url           resource location of data
    * @param pipe          pipe to buffered the download
    * @param eTag          HTTP ETag of data
    * @param skip          offset
    * @param contentLength total size of data
    * @param attempts      max attempts number
    * @param useRange      define if download can use HTTP Range
    */
   ProductDownloadTask(final InterruptibleHttpClient client,
         final String url, final Pipe pipe, final String eTag, final long skip,
         final long contentLength, final int attempts, boolean useRange)
   {
      this.httpClient = client;
      this.pipe = pipe;
      this.url = url;
      this.eTag = eTag;
      this.attempts = attempts;
      this.skip = skip;
      this.contentLength = contentLength;
      this.useRange = useRange;
   }

   /**
    * raise an IOException with the given StatusLine and cause Header (cause may be null).
    */
   private void raiseFailure(StatusLine stl, Header cause) throws IOException
   {
      Formatter ff = new Formatter();
      ff.format("Cannot download %s, Reason='%s' (HTTP%d)",
            this.url,
            stl.getReasonPhrase(),
            stl.getStatusCode());
      if (cause != null)
      {
         String cause_msg = cause.getValue();
         if (cause_msg != null && !cause_msg.isEmpty())
         {
            ff.format(" Cause='%s'", cause_msg);
         }
      }

      IOException exception = new IOException(ff.out().toString());
      ff.close();
      throw exception;
   }

   /**
    * Performs a download range (with multiple attempts if necessary) and
    * transfers downloaded bytes in the given channel.
    *
    * @param output output channel
    * @return the number of bytes transferred
    * @throws IOException          if a I/O error occurred
    * @throws InterruptedException if the download is interrupted
    */
   private long downloadRange(Pipe.SinkChannel output)
         throws IOException, InterruptedException
   {
      long start = skip;
      CountingIWC counter = new CountingIWC<>(output);

      for (int time = 1; (skip + counter.currentCount()) < contentLength && time < attempts; time++)
      {
         try
         {
            httpClient.interruptibleGetRange(url, counter, eTag, start, contentLength);
         }
         catch (IOException e)
         {
            LOGGER.debug("Download of {} interrupted cause by {}:{}", url, e.getClass(), e.getMessage());
            start = skip + counter.currentCount();
         }
      }

      return counter.currentCount();
   }

   @Override
   public void run()
   {
      Pipe.SinkChannel output = pipe.sink();
      try
      {
         if (useRange)
         {
            long received = downloadRange(output);
            if (received != (contentLength - skip))
            {
               LOGGER.trace("Download range [{}-{}] of {} failure", skip, contentLength, url);
               throw new IOException("Incomplete download from " + url);
            }
            LOGGER.trace("Download range [{}-{}] complete from {}", skip, contentLength, url);
         }
         else
         {
            HttpResponse response = httpClient.interruptibleGet(url, output);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
            {
               raiseFailure(response.getStatusLine(), response.getFirstHeader(HTTP_HEADER_CAUSE_MESSAGE));
            }
         }
      }
      catch (IOException | InterruptedException e)
      {
         try
         {
            pipe.source().close(); // Will generate an IOException on the reader side
         }
         catch (IOException suppressed) {}
         try
         {
            pipe.sink().close();
         }
         catch (IOException suppressed) {}
      }
      finally
      {
         try
         {
            output.close();
         }
         catch (IOException e)
         {
            LOGGER.warn("Cannot close pipe");
         }
      }
   }
}
