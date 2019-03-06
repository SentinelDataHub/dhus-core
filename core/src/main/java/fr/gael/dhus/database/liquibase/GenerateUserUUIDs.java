/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2016,2017 GAEL Systems
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
import java.util.UUID;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GenerateUserUUIDs implements CustomTaskChange
{
   private static final Logger LOGGER = LogManager.getLogger();

   @Override
   public String getConfirmationMessage ()
   {
      return null;
   }

   @Override
   public void setFileOpener (ResourceAccessor resource_accessor)
   {
   }

   @Override
   public void setUp () throws SetupException
   {
   }

   @Override
   public ValidationErrors validate (Database arg0)
   {
      return null;
   }

   @Override
   public void execute (Database database) throws CustomChangeException
   {
      JdbcConnection databaseConnection =
         (JdbcConnection) database.getConnection ();
      try
      {
         // foreach user generate uuid and update associated references
         PreparedStatement getUsers =
            databaseConnection.prepareStatement ("SELECT ID FROM USERS");
         ResultSet res = getUsers.executeQuery ();
         while (res.next ())
         {
            String user_id = res.getObject("ID").toString();
            String uuid = UUID.randomUUID ().toString ();

            // update USERS table
            long start_step = System.currentTimeMillis();
            PreparedStatement updateUsers = databaseConnection.prepareStatement(
                  "UPDATE USERS SET UUID = '" + uuid + "' WHERE ID = " + user_id);
            updateUsers.execute ();
            updateUsers.close ();
            long total_step = System.currentTimeMillis() - start_step;
            LOGGER.debug("### update USERS for user #{}: {}ms", user_id, total_step);

            // update NETWORK_USAGE table
            start_step = System.currentTimeMillis();
            PreparedStatement updateNetworkUsage = databaseConnection.prepareStatement(
                  "UPDATE NETWORK_USAGE SET USER_UUID = '" + uuid + "' WHERE USER_ID = " + user_id);
            updateNetworkUsage.execute ();
            updateNetworkUsage.close ();
            total_step = System.currentTimeMillis() - start_step;
            LOGGER.debug("Update NETWORK_USAGE for user #{}: {}ms", user_id, total_step);

            // retrieve max product id
            long max_product_id;
            String sql = "SELECT MAX(ID) FROM PRODUCTS";
            PreparedStatement max_product_id_stmt = databaseConnection.prepareStatement(sql);
            ResultSet result_set = max_product_id_stmt.executeQuery();
            result_set.next();
            max_product_id = ((Number) result_set.getObject(1)).longValue();
            max_product_id_stmt.close();

            // update PRODUCTS table
            int page_size = 10_000;
            String format = "UPDATE PRODUCTS SET OWNER_UUID = '" + uuid +
                  "' WHERE OWNER_ID = " + user_id + " AND ID >= %d AND ID < %d";
            start_step = System.currentTimeMillis();
            for (int index = 0; index < max_product_id; index = index + page_size)
            {
               int limit = index + page_size;
               PreparedStatement stmt = databaseConnection.prepareStatement(
                     String.format(format, index, limit));
               stmt.execute();
               stmt.close();
               LOGGER.debug("update PRODUCT table page [{}-{}]", index, limit);
            }
            total_step = System.currentTimeMillis() - start_step;
            LOGGER.debug("Update PRODUCTS for user #{}: {}ms", user_id, total_step);

            // update COLLECTION_USER_AUTH table
            start_step = System.currentTimeMillis();
            PreparedStatement updateCollectionuser = databaseConnection.prepareStatement(
                  "UPDATE COLLECTION_USER_AUTH SET USERS_UUID = '" + uuid +
                  "' WHERE USERS_ID = " + user_id);
            updateCollectionuser.execute ();
            updateCollectionuser.close ();
            total_step = System.currentTimeMillis() - start_step;
            LOGGER.debug("Update COLLECTION_USER_AUTH for user #{}: {}ms", user_id, total_step);

            // update PRODUCTCARTS table
            start_step = System.currentTimeMillis();
            PreparedStatement updateProductCarts = databaseConnection.prepareStatement(
                  "UPDATE PRODUCTCARTS SET USER_UUID = '" + uuid + "' WHERE USER_ID = " + user_id);
            updateProductCarts.execute ();
            updateProductCarts.close ();
            total_step = System.currentTimeMillis() - start_step;
            LOGGER.debug("Update PRODUCTCARTS for user #{}: {}ms", user_id, total_step);

            // update PRODUCT_USER_AUTH table
            start_step = System.currentTimeMillis();
            format = "UPDATE PRODUCT_USER_AUTH SET USERS_UUID = '" + uuid +
                  "' WHERE USERS_ID = " + user_id +
                  " AND PRODUCTS_ID >= %d AND PRODUCTS_ID < %d";
            for (int index = 0; index < max_product_id; index = index + page_size)
            {
               int limit = index + page_size;
               PreparedStatement stmt = databaseConnection.prepareStatement(
                     String.format(format, index, limit));
               stmt.execute();
               stmt.close();
               LOGGER.debug("update PRODUCT_USER_AUTH table page [{}-{}]", index, limit);
            }
            total_step = System.currentTimeMillis() - start_step;
            LOGGER.debug("Update PRODUCT_USER_AUTH for user #{}: {}ms",
                  user_id, total_step);

            // update USER_RESTRICTIONS table
            start_step = System.currentTimeMillis();
            PreparedStatement updateRestrictions = databaseConnection.prepareStatement(
                  "UPDATE USER_RESTRICTIONS SET USER_UUID = '" + uuid + "' WHERE USER_ID = " + user_id);
            updateRestrictions.execute ();
            updateRestrictions.close ();
            total_step = System.currentTimeMillis() - start_step;
            LOGGER.debug("Update USER_RESTRICTIONS for user #{}: {}ms", user_id, total_step);

            // update USER_ROLES table
            start_step = System.currentTimeMillis();
            PreparedStatement updateRoles = databaseConnection.prepareStatement(
                  "UPDATE USER_ROLES SET USER_UUID = '" + uuid + "' WHERE USER_ID = " + user_id);
            updateRoles.execute ();
            updateRoles.close ();
            total_step = System.currentTimeMillis() - start_step;
            LOGGER.debug("Update USER_ROLES for user #{}: {}ms", user_id, total_step);
         }
         getUsers.close ();
      }
      catch (Exception e)
      {
         LOGGER.error("Exception occured while generating User UUIDs", e);
      }
   }
}