/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016,2017 GAEL Systems
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

import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerConfiguration;
import fr.gael.dhus.database.object.config.synchronizer.UserSynchronizer;
import fr.gael.dhus.sync.impl.ODataProductSynchronizer;
import fr.gael.dhus.sync.impl.ODataUserSynchronizer;

/**
 * Dispatches tasks to different executors.
 *
 * This implementation dispatches Synchronizers to different Executor according to their type:
 * ODataUserSynchronizer or ODataProductSynchronizer or other.
 */
public final class MetaExecutor implements Executor
{
   /** Unique instance. */
   private static final Executor INSTANCE = new MetaExecutor();

   /** An executor to run {@link ODataUserSynchronizer}s. */
   private final Executor userSyncExecutor = new ExecutorImpl();
   /** An executor to run {@link ODataProductSynchronizer}s. */
   private final Executor prodSyncExecutor = new ExecutorImpl();
   /** An executor to run any other type of Synchronizer. */
   private final Executor miscSyncExecutor = new ExecutorImpl();

   /** Private contructor. */
   private MetaExecutor() {}

   /**
    * MetaExecutor is a singleton.
    * @return unique instance.
    */
   public static Executor getInstance()
   {
      return INSTANCE;
   }

   @Override
   public boolean addSynchronizer(Synchronizer s)
   {
      if (s instanceof ODataUserSynchronizer)
      {
         return userSyncExecutor.addSynchronizer(s);
      }
      else if (s instanceof ODataProductSynchronizer)
      {
         return prodSyncExecutor.addSynchronizer(s);
      }
      else
      {
         return miscSyncExecutor.addSynchronizer(s);
      }
   }

   @Override
   public Synchronizer removeSynchronizer(SynchronizerConfiguration sc)
   {
      if (sc instanceof UserSynchronizer)
      {
         return userSyncExecutor.removeSynchronizer(sc);
      }
      else if (sc instanceof ProductSynchronizer)
      {
         return prodSyncExecutor.removeSynchronizer(sc);
      }
      else
      {
         return miscSyncExecutor.removeSynchronizer(sc);
      }
   }

   @Override
   public void removeAllSynchronizers()
   {
      userSyncExecutor.removeAllSynchronizers();
      prodSyncExecutor.removeAllSynchronizers();
      miscSyncExecutor.removeAllSynchronizers();
   }

   @Override
   public boolean isRunning()
   {
      return userSyncExecutor.isRunning() && miscSyncExecutor.isRunning() && prodSyncExecutor.isRunning();
   }

   @Override
   public void enableBatchMode(boolean enabled)
   {
      userSyncExecutor.enableBatchMode(enabled);
      prodSyncExecutor.enableBatchMode(enabled);
      miscSyncExecutor.enableBatchMode(enabled);
   }

   @Override
   public boolean isBatchModeEnabled()
   {
      return userSyncExecutor.isBatchModeEnabled()
          && prodSyncExecutor.isBatchModeEnabled()
          && miscSyncExecutor.isBatchModeEnabled();
   }

   @Override
   public void start(boolean start_now)
   {
      userSyncExecutor.start(start_now);
      prodSyncExecutor.start(start_now);
      miscSyncExecutor.start(start_now);
   }

   @Override
   public void stop()
   {
      userSyncExecutor.stop();
      prodSyncExecutor.stop();
      miscSyncExecutor.stop();
   }

   @Override
   public void terminate()
   {
      userSyncExecutor.terminate();
      miscSyncExecutor.terminate();
      prodSyncExecutor.terminate();
   }

   @Override
   public SynchronizerStatus getSynchronizerStatus(SynchronizerConfiguration sc)
   {
      if (sc instanceof UserSynchronizer)
      {
         return userSyncExecutor.getSynchronizerStatus(sc);
      }
      else if (sc instanceof ProductSynchronizer)
      {
         return prodSyncExecutor.getSynchronizerStatus(sc);
      }
      else
      {
         return miscSyncExecutor.getSynchronizerStatus(sc);
      }
   }

}
