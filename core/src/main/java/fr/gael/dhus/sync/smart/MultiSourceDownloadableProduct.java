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
package fr.gael.dhus.sync.smart;

import fr.gael.dhus.database.object.config.synchronizer.SmartProductSynchronizer;
import fr.gael.dhus.util.http.InterruptibleHttpClient;
import fr.gael.dhus.util.stream.MultiSourceInputStream;
import fr.gael.dhus.util.stream.MultiSourceInputStreamFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.dhus.AbstractProduct;
import org.dhus.ProductConstants;

/**
 * A Product that supports the InputStream implementation.
 * <p>
 * Every time getImpl(InputStream.class) is invoked, a new download starts using multiple sources to
 * resume the download in case of error.
 * <p>
 * Download happens in a thread, there is no thread pool.
 * Uses the {@link InterruptibleHttpClient} and a {@link java.nio.channels.Pipe}.
 * Both the Sink and Source channels of a pipe implement.
 * <p>
 * {@link java.nio.channels.InterruptibleChannel}, making this class suitable for syncing tasks.
 */
public class MultiSourceDownloadableProduct extends AbstractProduct implements Closeable
{
   private final List<InputStream> streamList = new ArrayList<>();
   private final SmartProductSynchronizer synchronizer;
   private final MultiSourceInputStreamFactory multiSourceStreamFactory;
   private final String productUuid;
   private final String filename;
   private final String subProduct;
   private final String algorithm;
   private final String checksum;
   private final long contentLength;

   /**
    * Constructs a <em>MultiSourceDownloadableProduct</em>.
    *
    * @param synchronizer  synchronizer asking the product
    * @param streamFactory input stream factory
    * @param productUuid   product UUID
    * @param filename      product filename
    * @param subProduct    sub-product (optional)
    * @param algorithm     checksum algorithm
    * @param checksum      product checksum
    * @param contentLength product size
    */
   public MultiSourceDownloadableProduct(MultiSourceInputStreamFactory streamFactory,
         SmartProductSynchronizer synchronizer, String productUuid, String filename,
         String subProduct, String algorithm, String checksum, long contentLength)
   {
      this.synchronizer = synchronizer;
      this.multiSourceStreamFactory = streamFactory;
      this.productUuid = productUuid;
      this.filename = filename;
      this.subProduct = subProduct;
      this.algorithm = algorithm;
      this.checksum = checksum;
      this.contentLength = contentLength;
      setProperty(ProductConstants.DATA_SIZE, contentLength);
   }

   @Override
   public String getName()
   {
      return this.filename;
   }

   @Override
   public boolean hasImpl(Class<?> cl)
   {
      boolean hasimpl = false;
      if (cl.equals(InputStream.class) || cl.equals(MultiSourceInputStream.class))
      {
         hasimpl = true;
      }
      return hasimpl;
   }

   @Override
   @SuppressWarnings("unchecked")
   public <T> T getImpl(Class<? extends T> cl)
   {
      if (cl.equals(InputStream.class) || cl.equals(MultiSourceInputStream.class))
      {
         ProductInfo productInfo = new ProductInfo(productUuid, subProduct, contentLength, algorithm, checksum);
         InputStream stream = multiSourceStreamFactory.generateDownloadInputStream(synchronizer, productInfo);
         streamList.add(stream);
         return (T) stream;
      }
      throw new UnsupportedOperationException(cl + " is not supported");
   }

   @Override
   public void close() throws IOException
   {
      for (InputStream stream: streamList)
      {
         stream.close();
      }
   }
}
