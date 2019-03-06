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
package fr.gael.dhus.datastore.processing.fair;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

/**
 * An implementation of {@link RunnableFuture} adding a key to be handled by {@link FairQueue}.
 *
 * @param <T> return type
 */
public class FairFutureTask<T> extends FutureTask<T> implements FairQueueEntry
{
   private final Object listKey;

   public FairFutureTask(Callable<T> callable, Object listKey)
   {
      super(callable);
      this.listKey = listKey;
   }

   public FairFutureTask(Runnable runnable, T result, Object listKey)
   {
      super(runnable, result);
      this.listKey = listKey;
   }

   @Override
   public Object getListKey()
   {
      return this.listKey;
   }

}
