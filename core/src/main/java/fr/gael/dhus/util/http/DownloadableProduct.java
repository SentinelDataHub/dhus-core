/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2019 GAEL Systems
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

import fr.gael.dhus.database.object.Product;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.InterruptibleChannel;
import java.nio.channels.Pipe;
import java.nio.channels.WritableByteChannel;
import java.security.DigestException;
import java.security.MessageDigest;
import java.util.Formatter;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.AbstractProduct;
import org.dhus.ProductConstants;

import org.springframework.security.crypto.codec.Hex;

/**
 * A Product that supports the InputStream implementation.
 * Every time getImpl(InputStream.class) is invoked, a new download starts.
 * Download happens in a thread, there is no thread pool.
 * Uses the {@link InterruptibleHttpClient} and a {@link Pipe}.
 * Both the Sink and Source channels of a pipe implement
 * {@link java.nio.channels.InterruptibleChannel}, making this class suitable for syncing tasks.
 */
public class DownloadableProduct extends AbstractProduct implements Closeable
{
   /** Log. */
   private static final Logger LOGGER = LogManager.getLogger();

   /** Pattern for the filename property in the Content-Disposition HTTP Header field. */
   private final Pattern pattern = Pattern.compile("filename=\"(.+?)\"", Pattern.CASE_INSENSITIVE);

   /** One HTTP client can be used by many concurrent threads. */
   private final InterruptibleHttpClient httpClient;

   /** How many time an interrupted download will be resumed. */
   public final int downloadAttempts;

   /** Target product as a database object. */
   public final String url;

   /** Expected MD5 of data to download. */
   public final String md5;

   /** Content-Length as reported in the HTTP headers (HTTP payload). */
   public final long contentLength;

   /** Content-Type as reported in the HTTP headers. */
   public final String contentType;

   /** ETag HTTP header used for resuming interrupted downloads. */
   public final String ETag;

   /** `true` if remote supports HTTP ranges. */
   public final boolean canResume;

   /** filename as reported in the Content-Disposition. */
   public final String filename;

   /** Downloading thread. */
   private volatile Thread downloadThread = null;
   
   private Pipe pipe = null;

   /** Downloads the given product (the URL returned by {@link Product#getOrigin()}) using the
    * given HTTP client.
    *
    * @param http_client A well configured HTTP client to download the product (not null)
    * @param download_attempts How many time an interrupted download will be resumed
    * @param to_download A product whose Origin is set (not null)
    * @param md5 hash of data to download, for verification purposes (not null)
    * @param default_name the default name of this product (not null)
    * @throws IOException IO Error (network unavailable)
    * @throws DownloadableProductException if the HTTP client could not HEAD the origin of the product
    * @throws InterruptedException if current thread is interrupted
    */
   public DownloadableProduct(InterruptibleHttpClient http_client, int download_attempts,
         String to_download, String md5, String default_name)
         throws IOException, DownloadableProductException, InterruptedException
   {
      Objects.requireNonNull(http_client);
      Objects.requireNonNull(to_download);
      Objects.requireNonNull(default_name);
      this.url = to_download;
      this.httpClient = http_client;
      this.md5 = md5;
      this.downloadAttempts = download_attempts;

      // Sets the MD5 sum property so it is not recomputed by the target datastore
      if (md5 != null && !md5.isEmpty())
      {
         setProperty(ProductConstants.CHECKSUM_MD5, md5);
      }

      // HEADs the target to check availability and get its properties
      HttpResponse headrsp = http_client.interruptibleHead(to_download);
      if (headrsp.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
      {
         raiseFailure(headrsp.getStatusLine(), headrsp.getFirstHeader("cause-message"));
      }
      // Gets the size of the payload, its ETag and its Accept-Ranges
      this.contentLength = Long.parseLong(headrsp.getFirstHeader("Content-Length").getValue());
      setProperty(ProductConstants.DATA_SIZE, this.contentLength);
      this.contentType = headrsp.getFirstHeader("Content-Type").getValue();
      Header etag = headrsp.getFirstHeader("ETag");
      if (etag != null)
      {
         this.canResume = headrsp.containsHeader("Accept-Ranges");
         this.ETag = etag.getValue();
      }
      else
      {
         this.ETag = null;
         canResume = false;
      }

      // Gets the filename from the HTTP header field `Content-Disposition'
      String contdis = headrsp.getFirstHeader("Content-Disposition").getValue();
      if (contdis != null && !contdis.isEmpty())
      {
         Matcher m = pattern.matcher(contdis);
         if (m.find())
         {
            this.filename = m.group(1);
         }
         else
         {
            this.filename = default_name;
         }
      }
      else
      {
         this.filename = default_name;
      }
   }

   @Override
   protected Class<?>[] implsTypes()
   {
      return new Class<?>[]{ InputStream.class };
   }

   @Override
   public String getName()
   {
      return this.filename;
   }

   @Override
   public boolean hasImpl(Class<?> cl)
   {
      return InputStream.class.isAssignableFrom(cl);
   }

   @SuppressWarnings("rawtypes")
   @Override
   public <T> T getImpl(Class<? extends T> cl)
   {
      if (InputStream.class.isAssignableFrom(cl))
      {
         try
         {
            Pipe pipe = Pipe.open();
            this.pipe = pipe;
            DownloadTask dltask = new DownloadTask(pipe);
            downloadThread = new Thread(dltask, "Product Download");
            downloadThread.start();

            InputStream is = Channels.newInputStream(pipe.source());
            return cl.cast(is);
         }
         catch (IOException ex)
         {
            LOGGER.error("could not create pipe", ex);
         }
      }
      return null;
   }

   /**
    * Interrupts the downloading thread.
    */
   @Override
   public void close() throws IOException
   {
      if (downloadThread != null)
      {
         downloadThread.interrupt();
         closeProduct();
      }
   }
   
   @Override
   public void closeProduct() throws IOException
   {
      if (pipe != null)
      {
         pipe.source().close();
         pipe.sink().close();
      }
   }

   /** raise a DownloadableProductException with the given StatusLine and cause Header (cause may be null). */
   private void raiseFailure(StatusLine stl, Header cause) throws DownloadableProductException
   {
      try (Formatter ff = new Formatter())
      {
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
         throw new DownloadableProductException(ff.out().toString(), stl.getStatusCode());
      }
   }

   /** Download this.product, streams the data to the InputStreap implementation using a Pipe. */
   private class DownloadTask<IWC extends InterruptibleChannel & WritableByteChannel> implements Runnable
   {
      private final Pipe pipe;
      private int attempts;

      /** Create a new DownloadTask with an URL to download. */
      public DownloadTask(Pipe pipe)
      {
         this.pipe = pipe;
         this.attempts = downloadAttempts;
      }

      /**
       * In-thread code.
       * @return path to the downloaded data.
       */
      @SuppressWarnings({ "unchecked", "rawtypes" })
      @Override
      public void run()
      {
         try
         {
            IWC output = (IWC) pipe.sink();

            // Computes the data's md5 sum on the fly
            MessageDigest md = null;
            if (md5 != null)
            {
               md = MessageDigest.getInstance("MD5");
               output = (IWC)new DigestIWC(md, pipe.sink());
            }

            // Download
            long delta = System.currentTimeMillis();
            if (canResume)
            {
               // Range download
               // Counts written bytes (FIXME: is it done by the HTTP client?)
               CountingIWC decorator = new CountingIWC(output);
               for (; this.attempts != 0 && decorator.currentCount() < contentLength; this.attempts--)
               {
                  long downloaded = decorator.currentCount();
                  LOGGER.debug("Download of {} : Range [{}, {}]", url, downloaded, contentLength);
                  try
                  {
                     httpClient.interruptibleGetRange(url, decorator, ETag, downloaded, contentLength);
                  }
                  catch (IOException ex)
                  {
                     LOGGER.debug("Download of {} interrupted ({}, {})",
                           url, ex.getClass().getName(), ex.getMessage());
                  }
               }

               // Check a condition that should always be false
               if (decorator.currentCount() != contentLength)
               {
                  throw new IllegalStateException("Content-Legnth does not match downloaded bytes count");
               }
            }
            else
            {
               // Classic download
               HttpResponse response = httpClient.interruptibleGet(url, output);

               // If the response's status code is not 200, something wrong happened
               if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
               {
                  raiseFailure(response.getStatusLine(), response.getFirstHeader("cause-message"));
               }
            }

            // Check MD5SUM
            if (md5 != null)
            {
               String data_md5 = String.valueOf(Hex.encode(md.digest()));
               if (!md5.equalsIgnoreCase(data_md5))
               {
                  throw new DigestException(data_md5 + " != " + md5);
               }
            }

            LOGGER.info("Product '{}' ({} bytes compressed) successfully downloaded from {} in {} ms", filename, contentLength, url, System.currentTimeMillis() - delta);
            pipe.sink().close();
         }
         catch (Exception e)
         {
            if (InterruptedException.class.isAssignableFrom(e.getClass()))
            {
               LOGGER.debug("Thread downloading {} from {} interrupted", filename, url);
            }
            else
            {
               LOGGER.error("Download of {} from {} failed", filename, url, e);
            }
            try
            {
               LOGGER.debug("Try to close source for product {}", filename);
               pipe.source().close(); // Will generate an IOException on the reader side
            }
            catch (IOException ex) {}
            try
            {
               LOGGER.debug("Try to close pipe for product {}", filename);
               pipe.sink().close();
            }
            catch (IOException ex) {}
         }
      }
   }

   public static class DownloadableProductException extends IOException
   {
      private static final long serialVersionUID = 1L;

      private final int httpStatusCode;

      public DownloadableProductException(String message, int httpStatusCode)
      {
         super(message);
         this.httpStatusCode = httpStatusCode;
      }

      public int getHttpStatusCode()
      {
         return httpStatusCode;
      }
   }
}
