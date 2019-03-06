/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
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
import java.sql.ResultSet;
import java.sql.SQLException;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RemoveSymDSTables implements CustomTaskChange
{
   private static final Logger LOGGER = LogManager.getLogger();

   @Override
   public void execute(Database database) throws CustomChangeException
   {
      try
      {
         JdbcConnection jdbc = (JdbcConnection) database.getConnection();

         int rsType = ResultSet.TYPE_SCROLL_INSENSITIVE;
         int concur = ResultSet.CONCUR_READ_ONLY;

         String selectTriggers = "SELECT TRIGGER_NAME"
                              + " FROM INFORMATION_SCHEMA.TRIGGERS"
                              + " WHERE TRIGGER_NAME LIKE 'SYM_%';";
         try (PreparedStatement ps = jdbc.prepareStatement(selectTriggers, rsType, concur))
         {
            ResultSet rs = ps.executeQuery();
            while (rs.next())
            {
               String dropTrigger = "DROP TRIGGER "+ rs.getString("TRIGGER_NAME");
               try (PreparedStatement ps2 = jdbc.prepareStatement(dropTrigger))
               {
                  ps2.execute();
               }
            }
         }

         String selectConstraints = "SELECT CONSTRAINT_NAME, TABLE_NAME"
                                 + " FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS"
                                 + " WHERE CONSTRAINT_NAME LIKE 'SYM_%';";
         try (PreparedStatement ps = jdbc.prepareStatement(selectConstraints, rsType, concur))
         {
            ResultSet rs = ps.executeQuery();
            while (rs.next())
            {
               String alterTable = "ALTER TABLE " + rs.getString("TABLE_NAME")
                                + " DROP CONSTRAINT " + rs.getString("CONSTRAINT_NAME");
               try (PreparedStatement ps2 = jdbc.prepareStatement(alterTable))
               {
                  ps2.execute();
               }
            }
         }

         String selectTables = "SELECT TABLE_NAME"
                            + " FROM INFORMATION_SCHEMA.TABLES"
                            + " WHERE TABLE_NAME LIKE 'SYM_%';";
         try (PreparedStatement ps = jdbc.prepareStatement(selectTables, rsType, concur))
         {
            ResultSet rs = ps.executeQuery();
            while (rs.next())
            {
               String dropTable = "DROP TABLE " + rs.getString("TABLE_NAME");
               try (PreparedStatement ps2 = jdbc.prepareStatement(dropTable))
               {
                  ps2.execute();
               }
            }
         }
      }
      catch (DatabaseException | SQLException e)
      {
         LOGGER.error("An error occurred during removeSymDSTables", e);
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
