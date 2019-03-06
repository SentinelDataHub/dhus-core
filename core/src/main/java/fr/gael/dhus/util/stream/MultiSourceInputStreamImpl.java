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
import fr.gael.dhus.database.object.config.synchronizer.SmartProductSynchronizer;
import fr.gael.dhus.service.ISourceService;
import fr.gael.dhus.sync.smart.ProductInfo;
import fr.gael.dhus.sync.smart.SynchronizerRules;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.security.crypto.codec.Hex;

/**
 * Download representation of a content into an {@link InputStream} coming from multiple sources.
 */
class MultiSourceInputStreamImpl extends MultiSourceInputStream
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Object LOCK_STREAM_GENERATION = new Object();

   private final ISourceService sourceService;
   private final ProductStreamFactory streamFactory;
   private final List<Source> streamSources;
   private final ProductInfo productInfo;
   private final SynchronizerRules rules;
   private final MessageDigest messageDigest;

   private Source currentSource;
   private InputStream currentStream;
   private boolean isChecked;
   private long transferredBytes;

   /**
    * Constructs a <em>MultiSourceInputStreamImpl</em>.
    *
    * @param sourceService source service
    * @param streamFactory stream factory
    * @param synchronizer  synchronizer requested the stream
    * @param productInfo   information about the product to download
    * @throws NoSuchAlgorithmException if the checksum algorithm is not supported
    * @throws NullPointerException if an argument is {@code null}
    */
   MultiSourceInputStreamImpl(ISourceService sourceService, ProductStreamFactory streamFactory,
         SmartProductSynchronizer synchronizer, ProductInfo productInfo)
   {
      Objects.requireNonNull( synchronizer,  "synchronizer must not be null");
      Objects.requireNonNull(sourceService, "sourceService must not be null");
      Objects.requireNonNull(streamFactory, "streamFactory must not be null");
      Objects.requireNonNull(  productInfo,   "productInfo must not be null");
      this.sourceService = sourceService;
      this.streamFactory = streamFactory;
      this.productInfo = productInfo;

      this.streamSources = Collections.unmodifiableList(
            sourceService.getSource(synchronizer.getSources().getSource()));

      this.rules = new SynchronizerRules(synchronizer);

      String checksum = productInfo.getChecksum();
      if (checksum == null || checksum.isEmpty())
      {
         this.messageDigest = null;
      }
      else
      {
         MessageDigest md = null;
         try
         {
            md = MessageDigest.getInstance(productInfo.getChecksumAlgorithm());
         }
         catch (NoSuchAlgorithmException e)
         {
            LOGGER.warn("Unsupported checksum algorithm '{}'", productInfo.getChecksumAlgorithm());
         }
         this.messageDigest = md;
      }

      this.currentSource = null;
      this.currentStream = null;
      this.isChecked = false;
      this.transferredBytes = 0;
   }

   private InputStream generateNewStream(Source source)
   {
      synchronized (LOCK_STREAM_GENERATION)
      {
         InputStream stream = null;
         if (source.concurrentDownload() < source.getMaxDownload())
         {
            source.generateBandwidthCalculator(getMonitorableId());
            stream = streamFactory.generateInputStream(
                  productInfo, source, rules, transferredBytes);
            if (stream == null)
            {
               source.removeBandwidthCalculator(getMonitorableId());
            }
         }
         return stream;
      }
   }

   /**
    * Initializes {@link #currentStream} and {@link #currentSource}, using the best source to
    * perform or resume the transfer.
    * <p>
    * If the previous source is the best next source the transfer should not be interrupted.
    */
   private InputStream initBestStream()
   {
      InputStream nextStream = null;
      Source nextSource = null;

      ArrayList<Source> sourceList = new ArrayList<>(streamSources.size());
      sourceList.addAll(streamSources);

      while (nextStream == null && !sourceList.isEmpty())
      {
         nextSource = sourceService.getBestSource(sourceList);

         if (nextSource.equals(currentSource))
         {
            return currentStream;
         }

         nextStream = generateNewStream(nextSource);
         if (nextStream == null)
         {
            sourceList.remove(nextSource);
         }
      }

      if (nextSource != null && !nextSource.equals(currentSource))
      {
         releaseResources();
         currentSource = nextSource;
      }

      return nextStream;
   }

   private boolean checkDownload() throws IOException
   {
      if (!isChecked)
      {
         long length = productInfo.getSize();
         if (transferredBytes == length)
         {
            String expectedChecksum = productInfo.getChecksum();

            // Case of thumbnails and quicklooks, do not check message digests
            if (productInfo.getSubProduct() != null && !productInfo.getSubProduct().isEmpty())
            {
               expectedChecksum = null;
            }

            if (expectedChecksum != null && !expectedChecksum.isEmpty())
            {
               String computedChecksum = String.valueOf(Hex.encode(messageDigest.digest()));
               if (!expectedChecksum.equalsIgnoreCase(computedChecksum))
               {
                  throw new IOException("Invalid checksum after complete download");
               }
            }
            isChecked = true;
         }
         else if (transferredBytes > length)
         {
            LOGGER.debug("### {} ###", String.valueOf(Hex.encode(messageDigest.digest())));
            isChecked = true;
            throw new IOException("Too much bytes read: " + transferredBytes + "/" + length);
         }
         else
         {
            return false;
         }
      }
      return true;
   }

   private boolean checkBandwidth()
   {
      if (currentSource != null)
      {
         long bandwidth = currentSource.getCalculatedBandwidth(getMonitorableId());
         if (bandwidth != -1)
         {
            return getBandwidth() > rules.getThreshold();
         }
      }
      return true;
   }

   @Override
   protected void releaseResources()
   {
      if (currentStream != null)
      {
         try
         {
            currentStream.close();
         }
         catch (IOException e)
         {
            LOGGER.warn("Cannot close current download input stream", e);
         }
         currentStream = null;
      }

      if (currentSource != null)
      {
         currentSource.removeBandwidthCalculator(getMonitorableId());
         currentSource = null;
      }
   }

   @Override
   public int read() throws IOException
   {
      throw new IOException("Unitary read is not supported");
   }

   @Override
   public int read(byte[] b) throws IOException
   {
      return this.read(b, 0, b.length);
   }

   @Override
   public int read(byte[] b, int off, int len) throws IOException
   {
      if (Thread.interrupted())
      {
         releaseResources();
         throw new IOException("Interrupted stream");
      }

      // check if stream is not already close
      if (isClosed())
      {
         throw new IOException("Stream is already closed");
      }

      if (currentStream == null || !checkBandwidth())
      {
         currentStream = initBestStream();
         if (currentStream == null)
         {
            throw new IOException("No source available");
         }
      }

      int read;
      try
      {
         read = currentStream.read(b, off, len);
         if (read != -1)
         {
            transferredBytes = transferredBytes + read;
            currentSource.populateBandwidthCalculator(getMonitorableId(), read);
            if (messageDigest != null)
            {
               // using a protected copy of buffer to compute checksum on the fly
               messageDigest.update(Arrays.copyOf(b, b.length), off, read);
            }
         }
      }
      catch (IOException e)
      {
         read = -1;
      }

      if (read == -1 && !checkDownload())
      {
         releaseResources();
         read = 0;
      }

      return read;
   }

   @Override
   public long getTransferredBytes()
   {
      return transferredBytes;
   }

   @Override
   public long getBandwidth()
   {
      long bandwidth = -1;
      if (currentSource != null)
      {
         bandwidth = currentSource.getCalculatedBandwidth(getMonitorableId());
      }
      return bandwidth;
   }
}
