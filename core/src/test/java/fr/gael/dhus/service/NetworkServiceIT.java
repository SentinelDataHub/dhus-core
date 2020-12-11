/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016,2018,2019 GAEL Systems
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
package fr.gael.dhus.service;

import fr.gael.dhus.database.dao.UserDao;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.network.RegulatedInputStream;
import fr.gael.dhus.network.RegulationException;
import fr.gael.dhus.network.TrafficDirection;
import fr.gael.dhus.service.exception.EmailNotSentException;
import fr.gael.dhus.service.exception.RequiredFieldMissingException;
import fr.gael.dhus.service.exception.RootNotModifiableException;
import fr.gael.dhus.util.TestContextLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@ContextConfiguration(locations = "classpath:fr/gael/dhus/spring/context-service-test.xml", loader = TestContextLoader.class)
@DirtiesContext (classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class NetworkServiceIT extends AbstractTransactionalTestNGSpringContextTests
{
   @Autowired
   private UserDao userDao;

   private User user;
   Authentication auth = null;

   @BeforeClass
   public void setUp ()
   {
      String name = "koko";
      this.user = userDao.getByName(name);

      SandBoxUser user = new SandBoxUser(name, name, true, 0, this.user.getAuthorities());
      auth = new UsernamePasswordAuthenticationToken(user, user.getPassword(), this.user.getAuthorities());
      SecurityContextHolder.getContext().setAuthentication(auth);
   }

   // Call of this unitary test within the profiler with 
   // (invocationCount=10000, threadPoolSize=200)
   // Manage the changes in resources/dhus.xml outbound network channel section.
   @Test 
   public void testQuota () throws EmailNotSentException, 
      RequiredFieldMissingException, RootNotModifiableException
   {
      // Because of the threads, the authentication must be reported.
      SecurityContextHolder.getContext ().setAuthentication (auth);
      
      try (RandomInputStream ris = new RandomInputStream(12345);
           InputStream is = new RegulatedInputStream.Builder(ris,TrafficDirection.OUTBOUND).userName(this.user.getUsername()).build())
      {
         byte[]b=new byte[1024*1024];
         while (true)
         {
            try
            {
               is.read(b);
            }
            catch (RegulationException e)
            {
               logger.info("Quota exceeded.", e);
               return;
            }
            catch (IOException e)
            {
               logger.error("IOException", e);
            }
         }
      }
      catch (RegulationException e)
      {
         logger.error("RegulationException", e);
      }
      catch (IOException suppressed) {}
   }
   
   public class RandomInputStream extends InputStream
   {
      private final Random random;
      private boolean closed = false;

      RandomInputStream(long seed)
      {
         this.random = new Random(seed);
      }

      @Override
      public synchronized int read() throws IOException
      {
         if (closed)
         {
            throw new IOException("Stream already closed");
         }
         return (byte) random.nextInt();
      }

      @Override
      public synchronized int read(byte[] b) throws IOException
      {
         return read(b, 0, b.length);
      }

      @Override
      public synchronized int read(byte[] b, int off, int len)
         throws IOException
      {
         for (int i = 0; i < len; ++i)
         {
            b[off + i] = (byte) read();
         }
         return len;
      }

      @Override
      public void close() throws IOException
      {
         super.close();
         closed = true;
      }
   }

}
