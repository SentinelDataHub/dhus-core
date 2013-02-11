/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2020 GAEL Systems
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
package fr.gael.dhus.database.liquibase;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 * WriteBackConfiguration custom change task.
 * If {@code fr.gael.dhus.database.liquibase.WriteBackConfigurationEmbed} is in the classpath,
 * then executes {@code WriteBackConfigurationEmbed.execute(Database)}.
 */
public class WriteBackConfiguration implements CustomTaskChange
{
   @Override
   public void execute(Database database) throws CustomChangeException
   {
      try
      {
         // Avoid direct dependency to the Spring context
         Class<?> cls = Class.forName("fr.gael.dhus.database.liquibase.WriteBackConfigurationEmbed");
         Class<? extends CustomTaskChange> contextAwareTaskClass = cls.asSubclass(CustomTaskChange.class);
         CustomTaskChange contextAwareTask = contextAwareTaskClass.getDeclaredConstructor().newInstance();
         contextAwareTask.execute(database);
      }
      catch (ReflectiveOperationException ex)
      {
         Throwable cause = ex.getCause();
         if (cause == null || !(cause instanceof ReflectiveOperationException))
         {
            throw new CustomChangeException(cause);
         }
         // Spring context not in classpath
         // This change task should be considered successful in that case,
         // because there is no config file to write to
      }
   }

   @Override
   public String getConfirmationMessage()
   {
      return null;
   }

   @Override
   public void setUp() throws SetupException {}

   @Override
   public void setFileOpener(ResourceAccessor ra) {}

   @Override
   public ValidationErrors validate(Database dtbs)
   {
      return null;
   }

}
