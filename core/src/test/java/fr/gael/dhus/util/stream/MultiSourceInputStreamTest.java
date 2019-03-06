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
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerSource;
import fr.gael.dhus.service.ISourceService;
import fr.gael.dhus.sync.smart.ProductInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.easymock.EasyMock;

import org.springframework.security.crypto.codec.Hex;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MultiSourceInputStreamTest
{
   private byte[] data;
   private String checksum;
   private String algorithm;
   private MessageDigest md;

   @BeforeClass
   public void setUp() throws NoSuchAlgorithmException, MalformedURLException
   {
      // initialize data
      data = new byte[4096];
      Random rand = new Random();
      rand.nextBytes(data);

      // compute checksum data
      algorithm = "MD5";
      md = MessageDigest.getInstance(algorithm);
      checksum = String.valueOf(Hex.encode(md.digest(data)));
   }

   @SuppressWarnings("unchecked")
   @Test(expectedExceptions = {IOException.class})
   public void unitaryReadTest() throws IOException
   {
      // prepare test
      ISourceService sourceService = EasyMock.createMock(ISourceService.class);
      EasyMock.expect(sourceService.getSource(EasyMock.anyObject(List.class)))
            .andStubReturn(Collections.emptyList());

      ProductStreamFactory streamFactory = EasyMock.createMock(ProductStreamFactory.class);

      SmartProductSynchronizer synchronizer = EasyMock.createMock(SmartProductSynchronizer.class);
      EasyMock.expect(synchronizer.getSources())
            .andStubReturn(new SmartProductSynchronizer.Sources());
      EasyMock.expect(synchronizer.getThreshold()).andStubReturn(100);
      EasyMock.expect(synchronizer.getTimeout()).andStubReturn(300_000L);
      EasyMock.expect(synchronizer.getAttempts()).andStubReturn(1);

      ProductInfo info = EasyMock.createMock(ProductInfo.class);
      EasyMock.expect(info.getChecksum()).andStubReturn(null);

      EasyMock.replay(sourceService, streamFactory, synchronizer, info);

      MultiSourceInputStreamImpl stream =
            new MultiSourceInputStreamImpl(sourceService, streamFactory, synchronizer, info);

      // test
      stream.read();
   }

   @Test
   @SuppressWarnings("unchecked")
   public void completeReadStreamTest() throws IOException
   {
      // prepare variable
      String uuid = UUID.randomUUID().toString();

      ProductInfo info = new ProductInfo(uuid, null, (long) data.length, algorithm, checksum);

      SynchronizerSource ss1 = new SynchronizerSource();
      ss1.setSourceId(0);
      SynchronizerSource ss2 = new SynchronizerSource();
      ss2.setSourceId(1);
      List<SynchronizerSource> synchronizerSourceList = Arrays.asList(ss1, ss2);
      SmartProductSynchronizer.Sources syncSources = new SmartProductSynchronizer.Sources();
      syncSources.setSource(synchronizerSourceList);

      Source source1 = EasyMock.createNiceMock(Source.class);
      EasyMock.expect(source1.getId()).andStubReturn(0);
      EasyMock.expect(source1.getBandwidth()).andStubReturn(1L);
      EasyMock.expect(source1.getMaxDownload()).andStubReturn(5);
      EasyMock.expect(source1.generateBandwidthCalculator(EasyMock.anyString()))
            .andStubReturn(true);
      Source source2 = EasyMock.createNiceMock(Source.class);
      EasyMock.expect(source2.getId()).andStubReturn(1);
      EasyMock.expect(source2.getMaxDownload()).andStubReturn(5);
      EasyMock.expect(source2.generateBandwidthCalculator(EasyMock.anyString()))
            .andStubReturn(true);

      List<Source> sourceList = Arrays.asList(source1, source2);

      ISourceService sourceService = EasyMock.createMock(ISourceService.class);
      EasyMock.expect(sourceService.getSource(synchronizerSourceList)).andStubReturn(sourceList);
      EasyMock.expect(sourceService.getBestSource(EasyMock.anyObject()))
            .andStubAnswer(() -> (new Random().nextBoolean()) ? source1 : source2);

      ProductStreamFactory streamFactory = EasyMock.createMock(ProductStreamFactory.class);
      EasyMock.expect(streamFactory.generateInputStream(
            EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyLong()))
            .andStubAnswer(() ->
            {
               Object[] params = EasyMock.getCurrentArguments();
               return new ByteArrayInputStream(data, ((Long) params[3]).intValue(), data.length);
            });

      SmartProductSynchronizer synchronizer = EasyMock.createMock(SmartProductSynchronizer.class);
      EasyMock.expect(synchronizer.getSources())
            .andStubReturn(syncSources);
      EasyMock.expect(synchronizer.getThreshold()).andStubReturn(100);
      EasyMock.expect(synchronizer.getTimeout()).andStubReturn(300_000L);
      EasyMock.expect(synchronizer.getAttempts()).andStubReturn(1);

      EasyMock.replay(source1, source2, sourceService, streamFactory, synchronizer);

      byte[] buffer = new byte[1024];
      try (MultiSourceInputStreamImpl stream =
            new MultiSourceInputStreamImpl(sourceService, streamFactory, synchronizer, info))
      {
         stream.read(buffer);
      }

   }

   @Test(expectedExceptions = {IOException.class})
   @SuppressWarnings("unchecked")
   public void readStreamFailureTest() throws IOException
   {
      SmartProductSynchronizer sync = new SmartProductSynchronizer();
      sync.setSources(new SmartProductSynchronizer.Sources());
      sync.setThreshold(50);

      ProductInfo info = EasyMock.createNiceMock(ProductInfo.class);
      EasyMock.expect(info.getChecksumAlgorithm()).andStubReturn(algorithm);
      EasyMock.expect(info.getChecksum()).andStubReturn(checksum);
      EasyMock.expect(info.getSize()).andStubReturn((long) data.length);

      Source source = EasyMock.createNiceMock(Source.class);
      EasyMock.expect(source.getMaxDownload()).andStubReturn(5);
      EasyMock.expect(source.getBandwidth()).andStubReturn(500L);
      List<Source> sourceList = Collections.singletonList(source);
      ISourceService sourceService = EasyMock.createMock(ISourceService.class);
      EasyMock.expect(sourceService.getSource(EasyMock.anyObject(List.class)))
            .andStubReturn(sourceList);
      EasyMock.expect(sourceService.getBestSource(sourceList)).andStubReturn(source);

      ProductStreamFactory streamFactory = EasyMock.createNiceMock(ProductStreamFactory.class);
      EasyMock.expect(streamFactory.generateInputStream(EasyMock.anyObject(), EasyMock.anyObject(),
            EasyMock.anyObject(), EasyMock.anyLong()))
            .andStubAnswer(() -> new ByteArrayInputStream(data, 0, data.length / 2));

      EasyMock.replay(info, source, sourceService, streamFactory);

      byte[] buffer = new byte[1024];
      try (MultiSourceInputStreamImpl stream =
            new MultiSourceInputStreamImpl(sourceService, streamFactory, sync, info))
      {
         int read;
         do
         {
            read = stream.read(buffer);
         }
         while (read != -1);
      }
   }
}
