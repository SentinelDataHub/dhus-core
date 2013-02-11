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

public class ProductInfo
{
   private static final String DOWNLOAD_URL_PRODUCT_PATTERN    = "%s/Products('%s')/$value";
   private static final String DOWNLOAD_URL_SUBPRODUCT_PATTERN = "%s/Products('%s')/Products('%s')/$value";

   private final String uuid;
   private final String subProduct;
   private final Long size;
   private final String checksumAlgorithm;
   private final String checksum;

   public ProductInfo(String uuid, String subProduct, Long size, String checksumAlgorithm, String checksum)
   {
      this.uuid = uuid;
      this.subProduct = subProduct;
      this.size = size;
      this.checksumAlgorithm = checksumAlgorithm;
      this.checksum = checksum;
   }

   public String getUuid()
   {
      return uuid;
   }

   public String getSubProduct()
   {
      return subProduct;
   }

   public Long getSize()
   {
      return size;
   }

   public String getChecksumAlgorithm()
   {
      return checksumAlgorithm;
   }

   public String getChecksum()
   {
      return checksum;
   }

   public String getDownloadUrl(String url)
   {
      if (subProduct == null || subProduct.isEmpty())
      {
         return String.format(DOWNLOAD_URL_PRODUCT_PATTERN, url, uuid);
      }
      return String.format(DOWNLOAD_URL_SUBPRODUCT_PATTERN, url, uuid, subProduct);
   }
}
