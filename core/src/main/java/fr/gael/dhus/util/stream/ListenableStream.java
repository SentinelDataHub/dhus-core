/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019 GAEL Systems
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.commons.net.io.CopyStreamEvent;

/**
 * A stream decorator that is listenable.
 */
public class ListenableStream extends InputStream
{
   private final InputStream decorated;
   private final StreamListener listener;
   private final long streamSize;

   private long total = 0;

   /**
    * Creates instance.
    *
    * @param decorated not null
    * @param listener not null
    * @param streamSize length of the stream
    */
   public ListenableStream(InputStream decorated, StreamListener listener, long streamSize)
   {
      Objects.requireNonNull(decorated, "Decorated input stream parameter cannot be null");
      Objects.requireNonNull(listener, "Listener parameter cannot be null");
      this.decorated = decorated;
      this.listener = listener;
      this.streamSize = streamSize;
   }

   public ListenableStream(InputStream decorated, StreamListener listener)
   {
      this(decorated, listener, CopyStreamEvent.UNKNOWN_STREAM_SIZE);
   }

   @Override
   public int available() throws IOException
   {
      return this.decorated.available();
   }

   @Override
   public void close() throws IOException
   {
      this.listener.closed();
      this.decorated.close();
   }

   @Override
   public synchronized void mark(int readlimit)
   {
      this.decorated.mark(readlimit);
   }

   @Override
   public boolean markSupported()
   {
      return this.decorated.markSupported();
   }

   @Override
   public int read() throws IOException
   {
      int read = decorated.read();
      if (read > 0)
      {
         this.listener.bytesRead(++this.total, 1, this.streamSize);
      }
      return read;
   }

   @Override
   public int read(byte[] b) throws IOException
   {
      int read = this.decorated.read(b);
      if (read > 0)
      {
         this.total += read;
         this.listener.bytesRead(this.total, read, this.streamSize);
      }
      return read;
   }

   @Override
   public int read(byte[] b, int off, int len) throws IOException
   {
      int read = this.decorated.read(b, off, len);
      if (read > 0)
      {
         this.total += read;
         this.listener.bytesRead(this.total, read, this.streamSize);
      }
      return read;
   }

   @Override
   public synchronized void reset() throws IOException
   {
      this.decorated.reset();
   }

   @Override
   public long skip(long n) throws IOException
   {
      return decorated.skip(n);
   }

   public static interface StreamListener
   {
      /**
       * Stream closed event.
       */
      void closed();

      /**
       * Stream read event.
       *
       * @param totalBytesRead The total number of bytes read so far
       * @param bytesRead The number of bytes read by the last read call
       * @param streamSize The number of bytes in the stream being read
       */
      void bytesRead(long totalBytesRead, int bytesRead, long streamSize);
   }
}
