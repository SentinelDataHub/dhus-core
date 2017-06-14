/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016,2017 GAEL Systems
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.InterruptibleChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.AsyncByteConsumer;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;

/**
 * An interruptible HTTP client using Apache HttpComponents' async HTTP
 * client.<br>
 * This class have interruptible methods that will return as soon as their
 * running
 * thread is interrupted.<br>
 * This class only use interruptible channels from
 * {@link java.nio.channels}.<br>
 *
 * @see
 * <a href="https://hc.apache.org/httpcomponents-asyncclient-4.1.x/">HttpComponents:
 * async client</a>.
 */
public class InterruptibleHttpClient
{

   /** An HttpClient producer. */
   private final HttpAsyncClientProducer clientProducer;

   /** An InterruptibleHttpClient using {@code HttpAsyncClients.createDefault()}
    * as HttpAsyncClientProducer. */
   public InterruptibleHttpClient ()
   {
      clientProducer = new HttpAsyncClientProducer ()
      {
         @Override
         public CloseableHttpAsyncClient generateClient ()
         {
            CloseableHttpAsyncClient res = HttpAsyncClients.createDefault ();
            res.start ();
            return res;
         }
      };
   }

   /**
    * An InterruptibleHttpClient using the given HttpAsyncClientProducer.
    *
    * @param clientProducer a custom HttpAsyncClientProducer.
    */
   public InterruptibleHttpClient (HttpAsyncClientProducer clientProducer)
   {
      this.clientProducer = clientProducer;
   }

   /**
    * Performs the given request, writes the content into the given channel.
    *
    * @param <IWC> a generic type for any class that implements
    *              InterruptibleChannel and WritableByteChannel.
    * @param request to perform.
    * @param output written with the content of the HTTP response.
    * @param val validate the HttpResponse before downloading the payload.
    *
    * @return a response (contains the HTTP Headers, the status code, ...).
    *
    * @throws IOException IO error.
    * @throws InterruptedException interrupted.
    * @throws RuntimeException containing the actual exception if it is not an
    *                          instance of IOException.
    */
   public <IWC extends InterruptibleChannel & WritableByteChannel>
         HttpResponse interruptibleRequest(HttpUriRequest request, final IWC output, final ResponseValidator val)
         throws IOException, InterruptedException
   {

      // Creates a new client for each request, because we want to close it to interrupt the request.
      try (CloseableHttpAsyncClient httpClient = clientProducer.generateClient())
      {

         HttpAsyncRequestProducer producer = HttpAsyncMethods.create(request);
         // Creates a consumer callback that is called each time bytes are received
         AsyncByteConsumer<HttpResponse> consumer = new AsyncByteConsumer<HttpResponse>()
         {

            HttpResponse response = null;

            @Override
            protected void onByteReceived(ByteBuffer buf, IOControl ioctrl) throws IOException
            {
               output.write(buf);
            }

            @Override
            protected void onResponseReceived(HttpResponse response)
                  throws HttpException, IOException
            {
               if (val != null)
               {
                  val.validate(response);
               }
               this.response = response;
            }

            @Override
            protected HttpResponse buildResult(HttpContext context) throws Exception
            {
               return response;
            }
         };
         Future<HttpResponse> future = httpClient.execute(producer, consumer, null);

         try
         {
            // Blocks until the download is done, interruptible,
            // if interrupted, will close the HttpClient, the download will be interrupted
            return future.get();
         }
         catch (ExecutionException e)
         {
            // an error occured while producing the Future<HttpResponse>
            Throwable t = e.getCause();
            // output.write throws only instances of IOException
            if (t instanceof IOException)
            {
               throw (IOException) t;
            }
            throw new RuntimeException(t);
         }
      }
   }

   /**
    * Gets the given URL, writes the content into the given channel.
    *
    * @param <IWC> a generic type for any class that implements
    *              InterruptibleChannel and WritableByteChannel.
    * @param url to get.
    * @param output written with the content of the HTTP response.
    *
    * @return a response (contains the HTTP Headers, the status code, ...).
    *
    * @throws IOException IO error.
    * @throws InterruptedException interrupted.
    * @throws RuntimeException containing the actual exception if it is not an
    *                          instance of IOException.
    */
   public <IWC extends InterruptibleChannel & WritableByteChannel>
         HttpResponse interruptibleGet(String url, final IWC output)
         throws IOException, InterruptedException
   {
      return interruptibleRequest(new HttpGet(url), output, null);
   }

   /**
    * Gets a part of the given URL, writes the content into the given channel.
    * Fails if the returned HTTP status is not "206 partial content".
    *
    * @param <IWC> a generic type for any class that implements InterruptibleChannel and WritableByteChannel
    * @param url to get
    * @param output written with the content of the HTTP response
    * @param etag value of the If-Range header
    * @param range_start range byte start (inclusive)
    * @param range_end range byte end (inclusive)
    *
    * @return a response (contains the HTTP Headers, the status code, ...)
    *
    * @throws IOException IO error
    * @throws InterruptedException interrupted
    * @throws RuntimeException containing the actual exception if it is not an instance of IOException
    */
   public <IWC extends InterruptibleChannel & WritableByteChannel>
         HttpResponse interruptibleGetRange(String url, final IWC output, String etag, long range_start, long range_end)
         throws IOException, InterruptedException
   {
      HttpGet get = new HttpGet(url);
      get.setHeader("If-Range", etag);
      get.setHeader("Range", String.format("bytes=%d-%d", range_start, range_end));
      // This validator throws an IOException if the response code is not 206 partial content
      ResponseValidator val = new ResponseValidator()
      {
         @Override
         public void validate(HttpResponse response) throws HttpException, IOException
         {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_PARTIAL_CONTENT)
            {
               throw new IOException("Range request does not return partial content");
            }
         }
      };
      return interruptibleRequest(get, output, val);
   }

   /**
    * Deletes the given URL, writes the content into the given channel.
    *
    * @param <IWC> a generic type for any class that implements
    *              InterruptibleChannel and WritableByteChannel.
    * @param url to delete.
    * @param output written with the content of the HTTP response.
    *
    * @return a response (contains the HTTP Headers, the status code, ...).
    *
    * @throws IOException IO error.
    * @throws InterruptedException interrupted.
    * @throws RuntimeException containing the actual exception if it is not an
    *                          instance of IOException.
    */
   public <IWC extends InterruptibleChannel & WritableByteChannel>
         HttpResponse interruptibleDelete(String url, final IWC output)
         throws IOException, InterruptedException
   {
      return interruptibleRequest(new HttpDelete(url), output, null);
   }

   /**
    * HEADs the given URL.
    *
    * @param url to query
    * @return a response (contains the HTTP Headers, the status code, ...)
    *
    * @throws IOException IO error
    * @throws InterruptedException interrupted
    * @throws RuntimeException containing the actual exception if it is not an instance of IOException
    */
   public HttpResponse interruptibleHead(String url) throws IOException, InterruptedException
   {
      // NullIWC because a response to a HEAD request has no payload
      return interruptibleRequest(new HttpHead(url), new NullIWC(), null);
   }

   /** A null interruptible and writable sink channel. */
   public static class NullIWC implements InterruptibleChannel, WritableByteChannel
   {
      private boolean open = true;

      @Override
      public void close() throws IOException
      {
         this.open = false;
      }

      @Override
      public boolean isOpen()
      {
         return open;
      }

      @Override
      public int write(ByteBuffer src) throws IOException
      {
         if (!open)
         {
            throw new ClosedChannelException();
         }
         // Pretends `src.remaining()` bytes have been read from src
         int res = src.remaining();
         src.position(src.limit());
         return res;
      }
   }

   /** An interruptible, writable channel that writes to an in-memory byte array. */
   public static class MemoryIWC implements InterruptibleChannel, WritableByteChannel
   {
      private boolean open = true;
      private final ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
      private final WritableByteChannel byteChannel = Channels.newChannel(byteArrayOS);

      @Override
      public void close() throws IOException
      {
         this.open = false;
         this.byteChannel.close();
         this.byteArrayOS.close();
      }

      @Override
      public boolean isOpen()
      {
         return open;
      }

      @Override
      public int write(ByteBuffer src) throws IOException
      {
         if (!open)
         {
            throw new ClosedChannelException();
         }
         return byteChannel.write(src);
      }

      /**
       * Returns a copy of the underlying byte array.
       * @return byte array of written data.
       */
      public byte[] getBytes()
      {
         return byteArrayOS.toByteArray();
      }
   }

   /**
    * Useful to interrupt GET requests that download a large payload when the response does not fit
    * some expectations.
    */
   public static interface ResponseValidator
   {
      /**
       * Throw an HttpException or an IOException if the HTTP response is not OK.
       * @param response to validate
       * @throws HttpException Bad response
       * @throws IOException Bad response
       */
      public void validate(HttpResponse response) throws HttpException, IOException;
   }
}
