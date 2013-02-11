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
package fr.gael.dhus.database.liquibase;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class RemoveUnprocessedProducts implements CustomTaskChange
{
   private static final Logger LOGGER = LogManager.getLogger();

   @Override
   public void execute(Database database) throws CustomChangeException
   {
      JdbcConnection databaseConnection = (JdbcConnection) database.getConnection();
      try (PreparedStatement products =
               databaseConnection.prepareStatement("DELETE FROM PRODUCTS WHERE PROCESSED IS FALSE"))
      {
         products.executeUpdate();
      }
      catch (DatabaseException | SQLException e)
      {
         LOGGER.error("An exception occured", e);
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
   public void setFileOpener(ResourceAccessor resourceAccessor) {}

   @Override
   public ValidationErrors validate(Database database)
   {
      return null;
   }

}
