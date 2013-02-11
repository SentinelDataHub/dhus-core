/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2018 GAEL Systems
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

import org.dhus.store.derived.DerivedProductStore;

/**
 *
 */
public class MigrateKeyStoreDerivedProducts implements CustomTaskChange
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String UUID_PATTERN = "________-____-____-____-____________";
   private static final String DEPRECATED_VALUE = "deprecated";
   private static final int MAX_PAGE_SIZE = 1_000;

   @Override
   public String getConfirmationMessage()
   {
      return "Migrated Derived Product KeyStore entries";
   }

   @Override
   public void setFileOpener(ResourceAccessor resourceAccessor) {}

   @Override
   public void setUp() throws SetupException {}

   @Override
   public ValidationErrors validate(Database database)
   {
      return null;
   }

   @Override
   public void execute(Database database) throws CustomChangeException
   {
      JdbcConnection jdbc = (JdbcConnection) database.getConnection();

      LOGGER.info("Migration initialization...");
      // count
      String query = "SELECT COUNT(*) FROM PRODUCTS WHERE QUICKLOOK_PATH LIKE ?";
      long count;
      try (PreparedStatement stmt = jdbc.prepareStatement(query))
      {
         stmt.setString(1, UUID_PATTERN);
         try (ResultSet result = stmt.executeQuery())
         {
            result.next();
            count = result.getLong(1) * 2;
         }
      }
      catch (DatabaseException | SQLException e)
      {
         throw new CustomChangeException(e);
      }
      LOGGER.info("{} derived products to migrate", count);
      if (count == 0)
      {
         return;
      }

      // index
      query = "SELECT ID FROM PRODUCTS WHERE QUICKLOOK_PATH LIKE ? ORDER BY ID LIMIT 1";
      long firstId;
      try (PreparedStatement stmt = jdbc.prepareStatement(query))
      {
         stmt.setString(1, UUID_PATTERN);
         try (ResultSet result = stmt.executeQuery())
         {
            result.next();
            firstId = result.getLong(1);
         }
      }
      catch (DatabaseException | SQLException e)
      {
         throw new CustomChangeException(e);
      }
      LOGGER.info("Start index migration found");

      // migration
      LOGGER.info("Migration execution...");
      query = "SELECT ID, UUID, QUICKLOOK_PATH, THUMBNAIL_PATH FROM PRODUCTS WHERE ID >= ? "
            + "AND QUICKLOOK_PATH LIKE ? ORDER BY ID LIMIT ?";
      String updateDerived = "UPDATE KEYSTOREENTRIES SET ENTRYKEY = ?, TAG = ? WHERE ENTRYKEY = ?";
      String updateProduct = "UPDATE PRODUCTS SET QUICKLOOK_PATH = ?, THUMBNAIL_PATH = ? "
            + "WHERE ID = ?";
      long index = firstId;
      long migrated = 0;
      while (migrated < count)
      {
         try (PreparedStatement stmt = jdbc.prepareStatement(query))
         {
            stmt.setLong(1, index);
            stmt.setString(2, UUID_PATTERN);
            stmt.setInt(3, MAX_PAGE_SIZE);
            try (ResultSet result = stmt.executeQuery())
            {
               PreparedStatement derivedSmt = jdbc.prepareStatement(updateDerived);
               PreparedStatement productStmt = jdbc.prepareStatement(updateProduct);
               while (result.next())
               {
                  long id = result.getLong(1);
                  String uuid = result.getString(2);
                  String qlUuid = result.getString(3);
                  String thUuid = result.getString(4);

                  derivedSmt.setString(1, uuid);
                  derivedSmt.setString(2, DerivedProductStore.QUICKLOOK_TAG);
                  derivedSmt.setString(3, qlUuid);
                  derivedSmt.addBatch();

                  derivedSmt.setString(1, uuid);
                  derivedSmt.setString(2, DerivedProductStore.THUMBNAIL_TAG);
                  derivedSmt.setString(3, thUuid);
                  derivedSmt.addBatch();

                  productStmt.setString(1, DEPRECATED_VALUE);
                  productStmt.setString(2, DEPRECATED_VALUE);
                  productStmt.setLong(3, id);
                  productStmt.addBatch();

                  migrated = migrated + 2;
                  index = id;
               }
               derivedSmt.executeBatch();
               productStmt.executeBatch();
               derivedSmt.close();
               productStmt.close();
               LOGGER.info("{}% derived products migrated", (migrated*100/count));
            }
         }
         catch (DatabaseException | SQLException e)
         {
            throw new CustomChangeException(e);
         }
      }
   }

}
