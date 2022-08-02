/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2015,2016,2017 GAEL Systems
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
package fr.gael.dhus.sync;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer;

import org.testng.annotations.Test;

/** Tests for {@link ExecutorImpl}. */
public class ExecutorTest
{
   final ExecutorImpl r = new ExecutorImpl();

   /** {@link Executor#addSynchronizer(Synchronizer)} must not accept a null parameter. */
   @Test(expectedExceptions={NullPointerException.class})
   public void testNullAddSynchronizer ()
   {
      r.addSynchronizer (null);
   }

   /** Tests {@link Executor#isBatchModeEnabled()} and {@link Executor#enableBatchMode(boolean)}. */
   @Test
   public void testBatchModeFlag ()
   {
      assertFalse (r.isBatchModeEnabled ());
      r.enableBatchMode (true);
      assertTrue (r.isBatchModeEnabled ());
      r.enableBatchMode (false);
   }

   /** Implementation of {@link Synchronizer} for testing purposes. */
   private static class TestSync extends Synchronizer
   {
      boolean hasBeenCalled = false;
      int id;
      public TestSync (int id)
      {
         super (new ProductSynchronizer());
         this.id = id;
         this.syncConf.setSchedule ("* * * * * ?");
      }
      @Override
      public boolean synchronize () throws InterruptedException
      {
         if (Thread.interrupted ())
            throw new InterruptedException ();
         hasBeenCalled = true;
         return true;
      }
      @Override
      public long getId ()
      {
         return id;
      }
   }

   /** Tests {@link Executor#isRunning()}. */
   @Test
   public void testIsRunning () throws InterruptedException
   {
      r.start (true);
      Thread.sleep (200);
      assertTrue (r.isRunning ());
      r.terminate ();
      Thread.sleep (200);
      assertFalse (r.isRunning ());
   }

   /** Tests if the {@link Executor} calls {@link Synchronizer#Synchronize()}. */
   @Test (priority = 10)
   public void testSync () throws InterruptedException
   {
      TestSync s = new TestSync (0);
      r.addSynchronizer (s);
      r.start (true);
      Thread.sleep (1200);
      assertTrue (r.isRunning ());
      assertTrue (s.hasBeenCalled);
      r.terminate ();
   }

   /** Tests {@link Executor#addSynchronizer(Synchronizer)}. */
   @Test (priority = 12)
   public void testAddSynchronizer () throws InterruptedException
   {
      TestSync a = new TestSync (1);
      TestSync b = new TestSync (2);
      TestSync c = new TestSync (3);
      r.addSynchronizer (a);
      r.start (true);
      Thread.sleep (1200);
      r.addSynchronizer (b);
      Thread.sleep (1200);
      r.addSynchronizer (c);
      Thread.sleep (1200);
      r.terminate ();
      assertTrue (a.hasBeenCalled);
      assertTrue (b.hasBeenCalled);
      assertTrue (c.hasBeenCalled);
   }

   /** Tests {@link Executor#terminate()}. */
   @Test (priority = 20)
   public void testTerminate () throws InterruptedException
   {
      TestSync s = new TestSync(20);
      r.addSynchronizer (s);
      r.start (true);
      r.terminate ();
      Thread.sleep (1200);
      assertFalse (r.isRunning ());
   }

   /** Tests {@link Executor#stop()}. */
   @Test (priority = 22)
   public void testStop () throws InterruptedException
   {
      r.start (true);
      r.stop ();
      Thread.sleep (1200);
      assertFalse (r.isRunning ());
   }
}
