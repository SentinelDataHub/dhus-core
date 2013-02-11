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

import fr.gael.dhus.datastore.processing.ProcessingUtils;

import java.io.IOException;
import java.net.URL;
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
import org.apache.logging.log4j.util.Unbox;

/**
 * Update itemClass for Products.
 */
public class SetMissingItemClass implements CustomTaskChange
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String DEFAULT_ITEM_CLASS = "http://www.gael.fr/drb#item";

   @Override
   public void execute(Database database) throws CustomChangeException
   {
      int limit = 10_000;
      long max_id = 0;

      JdbcConnection databaseConnection = (JdbcConnection) database.getConnection();

      try (PreparedStatement maxProductId = databaseConnection.prepareStatement(
            "SELECT COUNT (*) FROM PRODUCTS WHERE PATH IS NOT NULL AND ITEM_CLASS IS NULL"))
      {
         ResultSet maxProduct_Id = maxProductId.executeQuery();
         maxProduct_Id.next();
         max_id = maxProduct_Id.getLong(1);
         LOGGER.info("Products to update (add missing item class): {}", Unbox.box(max_id));
      }
      catch (DatabaseException | SQLException e1)
      {
         LOGGER.error("An exception occured", e1);
      }

      try (
            PreparedStatement getProducts =
                  databaseConnection.prepareStatement("SELECT ID, PATH, ITEM_CLASS FROM PRODUCTS"
                        + " WHERE ID > ? AND PATH IS NOT NULL AND ITEM_CLASS IS NULL"
                        + " ORDER BY ID LIMIT ?");
            PreparedStatement updateItemClass =
                  databaseConnection.prepareStatement("UPDATE PRODUCTS SET ITEM_CLASS = ? WHERE ID = ?"))
      {
         long index = 0;
         long product_id = -1;
         while (index < max_id)
         {
            getProducts.setLong(1, product_id);
            getProducts.setLong(2, limit);
            ResultSet res = getProducts.executeQuery();
            if (!res.next())
            {
               break;
            }
            do
            {
               product_id = res.getLong("ID");
               String path = res.getString("PATH");
               URL url = new URL(path);
               String itemClass;
               try
               {
                  itemClass = ProcessingUtils.getItemClassUri(ProcessingUtils.getClassFromUrl(url));
               }
               catch (IOException | RuntimeException e)
               {
                  LOGGER.warn("Cannot retrieve item class of product (Id={}, path={})", Unbox.box(product_id), path);
                  itemClass = DEFAULT_ITEM_CLASS;
               }
               updateItemClass.setString(1, itemClass);
               updateItemClass.setLong(2, product_id);
               updateItemClass.addBatch();
               index++;
               LOGGER.debug("Successfully set item class ({}) of product (Id={}, path={})", itemClass, Unbox.box(product_id), path);
            }
            while (res.next());

            int[] results = updateItemClass.executeBatch();
            int total = 0;
            for (int result: results)
            {
               if (result < 0)
               {
                  total = -1;
                  break;
               }
               total += result;
            }
            LOGGER.info("Successfully set item class on {} products", Unbox.box(total));
         }
         LOGGER.info("Procedure 'set missing item class' is finished");
      }
      catch (DatabaseException | SQLException | IOException ex)
      {
         throw new CustomChangeException(ex);
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
