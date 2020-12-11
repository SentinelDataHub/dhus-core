/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016-2019 GAEL Systems
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
package fr.gael.dhus.olingo.v1.formats;

import fr.gael.dhus.olingo.v1.ExpectedException;
import fr.gael.dhus.olingo.v1.Model;
import fr.gael.dhus.olingo.v1.ServiceFactory;
import fr.gael.dhus.olingo.v1.entity.AbstractEntity;
import fr.gael.dhus.olingo.v1.entity.Product;
import fr.gael.dhus.util.MetalinkBuilder;

import java.nio.charset.Charset;
import java.util.Collection;

import javax.xml.transform.TransformerException;

import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataResponse;

/**  Creates OData response in Metalink4 format. */
public class MetalinkFormatter
{

   private static final Charset CHARSET = Charset.forName("UTF-8");
   private static final String CONTENT_DISP = "inline; filename=products" + MetalinkBuilder.FILE_EXTENSION;

   /**
    * Makes the metalink XML Document for a given list of products.
    * @param type The type of Entity Set to serialise.
    * @param data payload to serialise.
    * @param service_root the ROOT part of the URL to this service.
    * @return an non null OData response.
    * @throws ODataException on any kind of error.
    */
   public static ODataResponse writeFeed(EdmEntitySet type, Collection<AbstractEntity> data,
         String service_root) throws ODataException
   {
      // Only the Product entity type is supported
      if (!type.getEntityType().getName().equals(Model.PRODUCT.getEntityName()))
      {
         throw new MetalinkException(type.getEntityType().getName());
      }
      try
      {
         MetalinkBuilder mb = new MetalinkBuilder();

         for (AbstractEntity entity: data)
         {
            Product p = Product.class.cast(entity);

            String root = ServiceFactory.ROOT_URL;
            root = root + (root.endsWith("/") ? "" : "/");
            String url = root + Model.PRODUCT.getName() + "('" + p.getId() + "')/" + "$value";

            String filename = p.getMediaName();

            // no physical file
            if (filename == null)
            {
               // FIXME wrong for products that are not ZIPs
               filename = p.getName() + ".zip";
            }

            mb.addFile(filename)
                  .addUrl(url, null, 0)
                  .setSize(p.getContentLength())
                  .setHash(p.getChecksumAlgorithm(), p.getChecksumValue());
         }

         byte[] bin = mb.buildToString(false).getBytes(CHARSET);

         return ODataResponse
               .fromResponse(EntityProvider.writeBinary(MetalinkBuilder.CONTENT_TYPE, bin))
               .header("Content-Disposition", CONTENT_DISP)
               .build();
      }
      catch (TransformerException e)
      {
         throw new ODataException(e);
      }
   }

   public static class MetalinkException extends ExpectedException
   {
      private static final long serialVersionUID = 1L;

      public MetalinkException(String typename)
      {
         super("Entity type " + typename + " does not support Metalink formatting");
      }
   }

}
