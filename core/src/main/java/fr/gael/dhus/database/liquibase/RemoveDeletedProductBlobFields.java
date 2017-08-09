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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

public class RemoveDeletedProductBlobFields implements CustomTaskChange
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int PAGE_SIZE = 1;

   @Override
   public void execute(Database database) throws CustomChangeException
   {
      try
      {
         JdbcConnection connection = (JdbcConnection) database.getConnection();

         // count number of deleted products
         String sql_count = "SELECT COUNT(*) FROM DELETED_PRODUCTS";
         long max;
         try (PreparedStatement stmt = connection.prepareStatement(sql_count))
         {
            ResultSet result = stmt.executeQuery();
            if (!result.next())
            {
               throw new CustomChangeException("DELETED_PRODUCTS table update failed");
            }
            max = result.getLong(1);
         }
         LOGGER.debug("{} deleted product(s) to update");

         long index = 0;
         String get_pattern = "SELECT ID, CHECKSUMS FROM DELETED_PRODUCTS LIMIT %d,%d";
         String update_pattern = "UPDATE DELETED_PRODUCTS SET CHECKSUM_ALGORITHM='%s', CHECKSUM_VALUE='%s' WHERE ID=%d";
         while (index <= max)
         {
            // retrieve data
            sql_count = String.format(get_pattern, index, PAGE_SIZE);
            try (PreparedStatement get_stmt = connection.prepareStatement(sql_count))
            {
               ResultSet get_result = get_stmt.executeQuery();

               if (get_result.next())
               {
                  // retrieve data
                  long id = get_result.getLong("ID");
                  Blob blob = get_result.getBlob("CHECKSUMS");
                  byte[] data = blob.getBytes(1, (int) blob.length());
                  Map<String, String> checksums = (Map<String, String>) deserialize(data);

                  if (!checksums.isEmpty())
                  {
                     // fill newly fill
                     Map.Entry<String, String> checksum = checksums.entrySet().iterator().next();
                     String sql_update = String.format(update_pattern, checksum.getKey(), checksum.getValue(), id);
                     try (PreparedStatement update_stmt = connection.prepareStatement(sql_update))
                     {
                        update_stmt.executeUpdate();
                     }
                  }
               }
            }
            index = index + PAGE_SIZE;
         }
      }
      catch (DatabaseException | SQLException | IOException | ClassNotFoundException e)
      {
         throw new CustomChangeException("DELETED_PRODUCTS table update failed", e);
      }
   }

   private Object deserialize(byte[] data) throws IOException, ClassNotFoundException
   {
      try (ByteArrayInputStream bis = new ByteArrayInputStream(data))
      {
         try (ObjectInputStream ois = new ObjectInputStream(bis))
         {
            return ois.readObject();
         }
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
