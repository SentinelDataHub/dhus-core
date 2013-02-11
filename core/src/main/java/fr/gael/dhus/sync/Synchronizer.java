/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2017 GAEL Systems
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

import java.util.Objects;

import fr.gael.dhus.database.object.config.synchronizer.SynchronizerConfiguration;

/**
 * A Data synchronizer.
 * <p>
 * Synchronizes this instance of DHuS with remote product sources.
 */
public abstract class Synchronizer implements AutoCloseable
{
   /** A reference to the database object storing our configuration. */
   protected final SynchronizerConfiguration syncConf;

   /**
    * Creates a new Synchronizer.
    * SubClasses of {@link Synchronizer} *MUST* have a public constructor with
    * one parameter of type {@link SynchronizerConf}.
    * @param sc a non null database object.
    */
   protected Synchronizer(SynchronizerConfiguration sc)
   {
      Objects.requireNonNull(sc, "Parameter must not be null");
      this.syncConf = sc;
   }

   /**
    * Executes a synchronization pass and then returns.
    * Will be called by a RoundRobin scheduler to efficiently manage several
    * Synchronizers.
    *
    * @return true if there is more to synchronize.
    *
    * @throws InterruptedException If its thread has its {@code interrupted}
    *       flag set, this method must throw an {@link InterruptedException}
    *       immediately, abandoning the current synchronization pass.
    */
   public abstract boolean synchronize() throws InterruptedException;

   /**
    * Returns the configuration of this synchronizer.
    * @return the configuration of this synchronizer.
    */
   public SynchronizerConfiguration getSynchronizerConf()
   {
      return this.syncConf;
   }

   /**
    * Returns the identifier for this synchronizer.
    * This identifier will be used to identify a synchronizer.
    * @return a (local) unique identifier for this synchronizer.
    */
   public long getId()
   {
      return syncConf.getId();
   }

   /**
    * Returns the pace of the synchronization.
    * @return a cron expression.
    */
   public String getCronExpression()
   {
      return syncConf.getSchedule();
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null || !(obj instanceof Synchronizer))
      {
         return false;
      }
      Synchronizer asy = (Synchronizer) obj;
      return this.getId() == asy.getId();
   }

   @Override
   public int hashCode()
   {
      return Long.valueOf(this.getId()).hashCode();
   }

   @Override
   public void close() throws Exception {} // Default: NOOP

}
