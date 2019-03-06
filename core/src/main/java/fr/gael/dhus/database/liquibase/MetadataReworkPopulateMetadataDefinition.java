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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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

public class MetadataReworkPopulateMetadataDefinition implements CustomTaskChange
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Map<String, Integer> PROCESSED_DEFINITIONS = new HashMap<>();

   @Override
   public void execute(Database database) throws CustomChangeException
   {
      JdbcConnection connection = (JdbcConnection) database.getConnection();
      try
      {
         String select = "SELECT NAME, TYPE, CATEGORY, QUERYABLE FROM METADATA_INDEXES GROUP BY NAME, TYPE, CATEGORY, QUERYABLE";
         String insertDefinition = "INSERT INTO METADATA_DEFINITION (NAME, TYPE, CATEGORY, QUERYABLE) VALUES (?,?,?,?)";
         try (PreparedStatement selectStatement = connection.prepareStatement(select);
              PreparedStatement insertDefinitionStatement = connection.prepareStatement(insertDefinition);
              ResultSet result = selectStatement.executeQuery();)
         {
            int definition = 0;
            while (result.next())
            {
               String name = result.getString(1);
               String type = result.getString(2);
               String category = result.getString(3);
               String queryable = result.getString(4);
               String key = name + type + category + queryable;
               if (!PROCESSED_DEFINITIONS.containsKey(key))
               {
                  PROCESSED_DEFINITIONS.put(key, definition);
                  definition++;
                  insertDefinitionStatement.setString(1, name);
                  insertDefinitionStatement.setString(2, type);
                  insertDefinitionStatement.setString(3, category);
                  insertDefinitionStatement.setString(4, queryable);
                  insertDefinitionStatement.execute();
                  insertDefinitionStatement.clearParameters();
                  LOGGER.info("Inserting new metadata definition {}", definition);
               }
            }
         }
      }
      catch (DatabaseException | SQLException e)
      {
         throw new CustomChangeException(e);
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
