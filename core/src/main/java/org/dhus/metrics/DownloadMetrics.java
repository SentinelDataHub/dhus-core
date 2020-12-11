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
package org.dhus.metrics;

import com.codahale.metrics.MetricRegistry;

import fr.gael.dhus.util.stream.ListenableStream;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Bean interface to record download events.
 */
public interface DownloadMetrics
{
   static final String NAMESPACE = "downloads";
   /**
    * Marks a meter, parameters are used to build the name of the meter.
    *
    * @param itemClass item class URI, not null
    * @param username name of user that issued the download request, may be null
    */
   public void recordDownloadStart(String itemClass, String username);

   /**
    * Marks a meter, increments a counter.
    *
    * @param itemClass item class URI, not null
    * @param username name of user that issued the download request, may be null
    * @param isSuccess was download successful
    * @param downloadedLength downloaded byte count, may be null
    */
   public void recordDownloadEnd(String itemClass, String username, boolean isSuccess, Long downloadedLength);

   /**
    * Records the metric using both the ItemClass and the username.
    */
   public static class ItemAndUser implements DownloadMetrics
   {
      /** bean property: default value is COARSE */
      private Utils.ItemClass.Precision itemClassNamePrecision = Utils.ItemClass.Precision.COARSE;

      public Utils.ItemClass.Precision getItemClassNamePrecision()
      {
         return itemClassNamePrecision;
      }

      public void setItemClassNamePrecision(Utils.ItemClass.Precision itemClassNamePrecision)
      {
         this.itemClassNamePrecision = itemClassNamePrecision;
      }

      @Autowired
      private MetricRegistry metricRegistry;

      @Override
      public void recordDownloadStart(String itemClass, String username)
      {
         String name = MetricRegistry
               .name(NAMESPACE,
                     "start",
                     Utils.ItemClass.toMetricNamePart(itemClass, itemClassNamePrecision),
                     username == null ? username : "username:" + username,
                     "meter");
         metricRegistry.meter(name).mark();
      }

      @Override
      public void recordDownloadEnd(String itemClass, String username, boolean isSuccess, Long downloadedLength)
      {
         String type = Utils.ItemClass.toMetricNamePart(itemClass, itemClassNamePrecision);
         String name = MetricRegistry
               .name(NAMESPACE,
                     "end",
                     type,
                     username == null ? username : "username:" + username,
                     isSuccess ? "success" : "failure",
                     "meter");
         metricRegistry.meter(name).mark();

         metricRegistry.meter(MetricRegistry.name(NAMESPACE, type, username == null ? username : "username:" + username, "bytes"))
               .mark(downloadedLength);
      }
   }

   /**
    * Records the metric using the ItemClass only.
    */
   public static class Item extends ItemAndUser
   {
      @Override
      public void recordDownloadStart(String itemClass, String username)
      {
         super.recordDownloadStart(itemClass, null);
      }

      @Override
      public void recordDownloadEnd(String itemClass, String username, boolean isSuccess, Long downloadedLength)
      {
         super.recordDownloadEnd(itemClass, null, isSuccess, downloadedLength);
      }
   }

   /**
    * Records the metric using the username only.
    */
   public static class User extends ItemAndUser
   {
      @Override
      public void recordDownloadStart(String itemClass, String username)
      {
         super.recordDownloadStart(null, username);
      }

      @Override
      public void recordDownloadEnd(String itemClass, String username, boolean isSuccess, Long downloadedLength)
      {
         super.recordDownloadEnd(null, username, isSuccess, downloadedLength);
      }
   }

   /**
    * Records the metric using neither the username nor the itemClass.
    * "Global" metrics.
    */
   public static class Glob extends ItemAndUser
   {
      @Override
      public void recordDownloadStart(String itemClass, String username)
      {
         super.recordDownloadStart(null, null);
      }

      @Override
      public void recordDownloadEnd(String itemClass, String username, boolean isSuccess, Long downloadedLength)
      {
         super.recordDownloadEnd(null, null, isSuccess, downloadedLength);
      }
   }

   /**
    * A CopyStream Event Listener to compute download metrics.
    */
   public static class DownloadActionListener implements ListenableStream.StreamListener
   {
      private final DownloadMetrics downloadMetrics;
      private final String itemClass;
      private final String username;
      private volatile long bytesTransferred = 0L;
      private volatile long requestedBytes = 0L;

      /**
       * Creates instance.
       *
       * @param downloadMetrics download event recorder, not null
       * @param itemClass item class, not null
       * @param username username, may be null
       */
      public DownloadActionListener(DownloadMetrics downloadMetrics, String itemClass, String username)
      {
         this.downloadMetrics = downloadMetrics;
         this.itemClass = itemClass;
         this.username = username;
      }

      /**
       * Sets the requested number of bytes, used to determine if this download is successful on
       * closed events.
       *
       * @param requestedBytes requested size in bytes, computed from the Range header from request
       */
      public void setRequestedBytes(long requestedBytes)
      {
         this.requestedBytes = requestedBytes;
      }

      @Override
      public void closed()
      {
         downloadMetrics.recordDownloadEnd(itemClass, username, (bytesTransferred == requestedBytes), requestedBytes);
      }

      @Override
      public void bytesRead(long totalBytesTransferred, int bytesTransferred, long streamSize)
      {
         this.bytesTransferred = totalBytesTransferred;
      }
   }

}
