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
import java.sql.Statement;
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

public class MetadataReworkMigration implements CustomTaskChange
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int PAGE_SIZE = 100;

   private String generateKeyDefenitionMap(String name, String type, String category, String queryable)
   {
      StringBuilder sb = new StringBuilder();
      sb.append(name)
            .append(type)
            .append(category)
            .append(queryable);
      return sb.toString();
   }

   private Map<String, Integer> loadMetadataDefinitionIds(JdbcConnection connection) throws
         DatabaseException, SQLException
   {
      Map<String, Integer> definitions = new HashMap<>();

      String query = "SELECT ID, NAME, TYPE, CATEGORY, QUERYABLE FROM METADATA_DEFINITION";
      PreparedStatement stmt = connection.prepareStatement(query);
      try (ResultSet result = stmt.executeQuery())
      {
         while (result.next())
         {
            String key = generateKeyDefenitionMap(result.getString("NAME"),
                  result.getString("TYPE"),
                  result.getString("CATEGORY"),
                  result.getString("QUERYABLE"));
            definitions.putIfAbsent(key, result.getInt("ID"));
         }
      }

      return definitions;
   }

   @Override
   public void execute(Database database) throws CustomChangeException
   {
      JdbcConnection connection = (JdbcConnection) database.getConnection();
      try
      {
         Map<String, Integer> processedDefinition = loadMetadataDefinitionIds(connection);
         LOGGER.info("Counting product...");
         long max;
         try (Statement statement = connection.createStatement();
              ResultSet resultCount = statement.executeQuery("SELECT COUNT(*) FROM PRODUCTS"))
         {
            if (!resultCount.next())
            {
               throw new CustomChangeException("Cannot count product");
            }
            max = resultCount.getLong(1);
         }
         LOGGER.info("{} product(s) to migrate", max);

         String productQuery = "SELECT ID FROM PRODUCTS WHERE ID > ? LIMIT ?";
         String metadataQuery = "SELECT NAME, TYPE, CATEGORY, QUERYABLE, VALUE "
               + "FROM METADATA_INDEXES WHERE PRODUCT_ID = ?";
         String insertQuery = "INSERT INTO " +
               "METADATA_INDEXES_TMP(PRODUCT_ID, METADATA_DEFINITION_ID, VALUE) " +
               "VALUES (?, ?, ?)";
         try (PreparedStatement productStatement = connection.prepareStatement(productQuery);
              PreparedStatement metadataStatement = connection.prepareStatement(metadataQuery);
              PreparedStatement insertStatement = connection.prepareStatement(insertQuery))
         {
            long counter = 0;
            long lastProductId = -1;
            while (counter < max)
            {
               productStatement.setLong(1, lastProductId);
               productStatement.setInt(2, PAGE_SIZE);
               try (ResultSet productIdSet = productStatement.executeQuery())
               {
                  while (productIdSet.next())
                  {
                     lastProductId = productIdSet.getLong(1);
                     metadataStatement.setLong(1, productIdSet.getLong(1));
                     try (ResultSet metadataSet = metadataStatement.executeQuery())
                     {
                        while (metadataSet.next())
                        {
                           String key = generateKeyDefenitionMap(
                                 metadataSet.getString(1),
                                 metadataSet.getString(2),
                                 metadataSet.getString(3),
                                 metadataSet.getString(4));
                           int definitionId = processedDefinition.get(key);
                           insertStatement.setLong(1, lastProductId);
                           insertStatement.setInt(2, definitionId);
                           insertStatement.setString(3, metadataSet.getString(5));
                           insertStatement.addBatch();
                        }
                     }
                     counter++;
                  }
               }
               insertStatement.executeBatch();
               LOGGER.info("Product(s) migrated {}/{}", counter, max);
               connection.commit();
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
