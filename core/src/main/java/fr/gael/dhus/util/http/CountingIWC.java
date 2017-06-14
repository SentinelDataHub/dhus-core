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
package fr.gael.dhus.util.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.InterruptibleChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

/**
 * An IWC decorator that counts and reports how many bytes have been written.
 * Warning: do not decorate an instance of this decorator with another decorator that adds buffering
 * or you'll lose realtime information.
 * @param <IWC> an interruptible and writable ByteChannel
 */
public class CountingIWC <IWC extends InterruptibleChannel & WritableByteChannel>
      implements InterruptibleChannel, WritableByteChannel
{
   protected final IWC iwc;
   private long count = 0L;

   public CountingIWC(IWC iwc)
   {
      Objects.requireNonNull(iwc, "param `iwc` cannot be null");
      this.iwc = iwc;
   }

   @Override
   public void close() throws IOException
   {
      this.iwc.close();
   }

   @Override
   public boolean isOpen()
   {
      return this.iwc.isOpen();
   }

   @Override
   public int write(ByteBuffer src) throws IOException
   {
      int written = this.iwc.write(src);
      this.count += written;
      return written;
   }

   /**
    * Returns the count of written bytes.
    * @return number of bytes written to this.iwc
    */
   public long currentCount()
   {
      return this.count;
   }

}
