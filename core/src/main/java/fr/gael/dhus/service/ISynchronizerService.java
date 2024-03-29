/*
 * Data Hub Service(DHuS) - For Space data distribution.
 * Copyright(C) 2015,2016,2017 GAEL Systems
 *
 * This file is part of DHuS software sources.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or(at your option) any later version.
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

import fr.gael.dhus.DHuS;
import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.Source;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerConfiguration;
import fr.gael.dhus.service.exception.InvokeSynchronizerException;
import fr.gael.dhus.sync.Executor;
import fr.gael.dhus.sync.Synchronizer;
import fr.gael.dhus.sync.SynchronizerStatus;

import java.text.ParseException;
import java.util.Iterator;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

public interface ISynchronizerService extends
      ApplicationListener<ContextClosedEvent>
{
   /**
    * Possible statuses for the {@link Synchronizer} executor.
    */
   enum Status
   {
      /** The executor is running. */
      RUNNING,
      /** The executor is stopped. */
      STOPPED
   }

   /**
    * Returns the status of the executor.
    * @return {@link Status#RUNNING} or {@link Status#STOPPED}.
    */
   Status getStatus ();

   /**
    * If the executor is not started yet, add all the active
    * {@link Synchronizer} in the executor and starts the
    * executor.
    */
   void startSynchronization ();

   /**
    * Stops the executor.
    */
   void stopSynchronization ();

   /**
    * Returns a {@link SynchronizerConf} by its identifier.
    *
    * @param <T>
    * @param id {@link SynchronizerConf} identifier.
    * @param requiredType runtime type for T
    * @return a {@link SynchronizerConf} or {@code null} if not found.
    */
   <T extends SynchronizerConfiguration> T getSynchronizerConfById(long id, Class<T> requiredType);

   /**
    * Returns all {@link SynchronizerConf}.
    * @return a Iterator of {@link SynchronizerConf}.
    */
   Iterator<SynchronizerConfiguration> getSynchronizerConfs();

   /**
    * Returns all {@link SynchronizerConf} of given type.
    *
    * @param <T>
    * @param type java path to an implementation of {@link Synchronizer}.
    * @return a Iterator of {@link SynchronizerConf}.
    * @see SynchronizerConf#setType(java.lang.String)
    */
   <T extends SynchronizerConfiguration> Iterator<T> getSynchronizerConfs(Class<T> type);

   /**
    * Returns how many {@link SynchronizerConf} exist in the database.
    * @return count.
    */
   int count ();

   /**
    * Creates a new {@link Synchronizer} from the given type and cron expression.
    * The newly created {@link Synchronizer} is flagged as not active and is not
    * run by the executor.
    *
    * @see SynchronizerConf#setCronExpression(String)
    *
    * @param <T>
    * @param label see {@link SynchronizerConf#getLabel()}, can be null.
    * @param type see {@link SynchronizerConf#getType()}.
    * @param cron_exp the pace of the synchronization.
    *
    * @return the newly created {@link SynchronizerConf}.
    *
    * @throws ParseException failed to parse the given cron expression.
    * @throws java.lang.InstantiationException
    * @throws java.lang.IllegalAccessException
    */
   <T extends SynchronizerConfiguration>
      T createSynchronizer(String label, String cron_exp, Class<T> type)
         throws ParseException, ReflectiveOperationException;

   /**
    * Removes a {@link Synchronizer} with the given identifier.
    * The removed {@link Synchronizer} won't be run any longer.
    * @param id {@link SynchronizerConf} identifier.
    */
   void removeSynchronizer (long id);

   /**
    * Sets a {@link Synchronizer} active and adds it in the executor.
    * @param id {@link SynchronizerConf} identifier.
    * @throws InvokeSynchronizerException Synchronizer invocation failure.
    */
   void activateSynchronizer (long id)
         throws InvokeSynchronizerException;

   /**
    * Sets a {@link Synchronizer} inactive and removes it from the executor.
    * @param id {@link SynchronizerConf} identifier.
    */
   void deactivateSynchronizer (long id);

   /**
    * Enables the batch mode of the executor.
    * @param enable {@code true} to enable the batch mode.
    */
   void enableBatchMode (boolean enable);

   /**
    * Returns {@code true} if the batch mod is enabled.
    * @return {@code true} if the batch mod is enabled.
    */
   boolean isBatchModeEnabled ();

   /**
    * Saves the configuration of a {@link Synchronizer}.
    * <p>
    * <b>You should deactivate that synchronizer first, to avoid any side effect!</b>
    * <p>
    * It will reactivate the synchronizer if its {@code active} field is set to {@code true}.
    *
    * @param sc configuration.
    * @throws InvokeSynchronizerException if the given configuration does not
    *         allow instanciation of a {@link Synchronizer}.
    */
   void saveSynchronizerConf(SynchronizerConfiguration sc) throws InvokeSynchronizerException;

   /**
    * Saves the given synchronizer's configuration back in the databse.
    * This method is intended to be used by Synchronizers themselves, this
    * method does no tests at all to avoid deadlocks.
    * <p>
    * This method is unsafe and you should probably be using
    * {@link #saveSynchronizerConf(SynchronizerConf)} instead.
    * @param s to save.
    */
   void saveSynchronizer (Synchronizer s);

   /**
    * Returns the status of the given {@link Synchronizer}.
    * @param sc an instance of SynchronizerConf that exists in the database.
    * @return status.
    */
   SynchronizerStatus getStatus(SynchronizerConfiguration sc);

   /**
    * Starts the {@link Executor} after every webapps have been installed.
    * Called by main start in {@link DHuS}
    * <b>YOU MUST NOT CALL THIS METHOD!</b>
    */
   void init ();
   
   public boolean isUniqueSource(ProductSynchronizer productSynchronizer);
   
   public Source getRefercedSource(ProductSynchronizer productSynchronizer, long id);
}
