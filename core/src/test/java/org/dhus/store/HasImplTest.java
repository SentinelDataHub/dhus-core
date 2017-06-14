/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016 GAEL Systems
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
package org.dhus.store;

import fr.gael.drb.DrbFactory;
import fr.gael.drb.DrbItem;
import fr.gael.drb.DrbNode;
import fr.gael.drb.impl.spi.DrbNodeSpi;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.testng.Assert;
import org.testng.annotations.Test;

public class HasImplTest
{
   AbstractHasImpl impls = new AbstractHasImpl()
   {
      Class<?>[] impls =
      {
         File.class,
         InputStream.class,
         DrbNode.class,
         URL.class
      };

      @Override
      protected Class<?>[] implsTypes()
      {
         return impls;
      }

      @Override
      @SuppressWarnings("unchecked")
      public <T> T getImpl(Class<? extends T> cl)
      {
         try
         {
            if (cl.isAssignableFrom(File.class))
            {
               return (T) new File(".");
            }
            if (cl.isAssignableFrom(InputStream.class))
            {
               return (T) new FileInputStream(".");
            }
            if (cl.isAssignableFrom(DrbNode.class))
            {
               return (T) DrbFactory.openURI(".");
            }
            if (cl.isAssignableFrom(URL.class))
            {
               return (T) new URL(".");
            }
         }
         catch (Exception e)
         {
            // No importance...
         }

         throw new UnsupportedOperationException("Not implemented.");

      }
   };

   @Test
   public void getImpl()
   {
      File file = impls.getImpl(File.class);
      Assert.assertNotNull(file);

      DrbItem item = impls.getImpl(DrbItem.class);
      Assert.assertNotNull(item);

   }

   @Test
   public void hasImpl()
   {
      Assert.assertTrue(impls.hasImpl(File.class));
      Assert.assertTrue(impls.hasImpl(InputStream.class));
      Assert.assertTrue(impls.hasImpl(DrbNode.class));
      Assert.assertTrue(impls.hasImpl(URL.class));
      Assert.assertTrue(impls.hasImpl(DrbItem.class));

      Assert.assertFalse(impls.hasImpl(DrbNodeSpi.class));
      Assert.assertFalse(impls.hasImpl(OutputStream.class));
   }

}
