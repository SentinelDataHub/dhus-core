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
import java.util.ArrayList;
import java.util.List;

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

public class RemoveDeletedUser implements CustomTaskChange
{
   private static final Logger LOGGER = LogManager.getLogger();

   @Override
   public void execute(Database database) throws CustomChangeException
   {
      List<String> prefs = new ArrayList<>();

      String selectUUID = "SELECT UUID FROM USERS WHERE DELETED=true";

      String deleteRoles = "DELETE FROM USER_ROLES WHERE USER_UUID=";

      String getRestriction = "SELECT RESTRICTION_UUID FROM USER_RESTRICTIONS WHERE USER_UUID=";
      String deleteRestriction = "DELETE FROM USER_RESTRICTIONS WHERE USER_UUID=";
      String deleteRestriction2 = "DELETE FROM ACCESS_RESTRICTION WHERE UUID=";

      String getSearches = "SELECT SEARCHES_UUID FROM SEARCH_PREFERENCES WHERE PREFERENCE_UUID=";
      String deleteSearch = "DELETE FROM SEARCHES WHERE UUID=";
      String deleteSearchAdv = "DELETE FROM SEARCH_ADVANCED WHERE SEARCH_UUID=";

      String getPref = "SELECT PREFERENCES_UUID FROM USERS WHERE UUID=";
      String deleteSearchPref = "DELETE FROM SEARCH_PREFERENCES WHERE PREFERENCE_UUID=";
      String deletePref = "DELETE FROM PREFERENCES WHERE UUID=";

      String getCart = "SELECT UUID FROM PRODUCTCARTS WHERE USER_UUID=";
      String deleteCartP = "DELETE FROM CART_PRODUCTS WHERE CART_UUID=";
      String deleteCart = "DELETE FROM PRODUCTCARTS WHERE USER_UUID=";

      String deleteNetwork = "DELETE FROM NETWORK_USAGE WHERE USER_UUID=";

      String deleteUser = "DELETE FROM USERS WHERE DELETED=true";
      try
      {
         JdbcConnection jdbc = (JdbcConnection) database.getConnection();
         try (PreparedStatement getUUID = jdbc.prepareStatement(selectUUID))
         {
            ResultSet result = getUUID.executeQuery();

            while (result.next())
            {
               String u = result.getString(1);
               //Remember for delete Preferences after users deletion
               try (PreparedStatement getPrefe = jdbc.prepareStatement(getPref + "'" + u + "'"))
               {
                  ResultSet res = getPrefe.executeQuery();

                  while (res.next())
                  {
                     String uuid = res.getString(1);
                     prefs.add(uuid);
                  }
               }

               try (PreparedStatement delRoles = jdbc.prepareStatement(deleteRoles + "'" + u + "'"))
               {
                  delRoles.executeUpdate();
               }

               try (PreparedStatement getRestr = jdbc.prepareStatement(getRestriction + "'" + u + "'"))
               {
                  ResultSet res = getRestr.executeQuery();

                  try (PreparedStatement delRestriction = jdbc.prepareStatement(deleteRestriction + "'" + u + "'"))
                  {
                     delRestriction.executeUpdate();
                  }

                  while (res.next())
                  {
                     String uuid = res.getString(1);

                     try (PreparedStatement delete = jdbc.prepareStatement(deleteRestriction2 + "'" + uuid + "'"))
                     {
                        delete.executeUpdate();
                     }
                  }
               }

               try (PreparedStatement getC = jdbc.prepareStatement(getCart + "'" + u + "'"))
               {
                  ResultSet res4 = getC.executeQuery();

                  while (res4.next())
                  {
                     String uuid = res4.getString(1);

                     try (PreparedStatement delete = jdbc.prepareStatement(deleteCartP + "'" + uuid + "'"))
                     {
                        delete.executeUpdate();
                     }
                  }
               }
               try (PreparedStatement delCart = jdbc.prepareStatement(deleteCart + "'" + u + "'"))
               {
                  delCart.executeUpdate();
               }

               try (PreparedStatement delN = jdbc.prepareStatement(deleteNetwork + "'" + u + "'"))
               {
                  delN.executeUpdate();
               }
            }
            try (PreparedStatement delUser = jdbc.prepareStatement(deleteUser))
            {
               delUser.executeUpdate();
            }
         }

         for (String uuid: prefs)
         {
            try (PreparedStatement getSearch = jdbc.prepareStatement(getSearches + "'" + uuid + "'"))
            {
               ResultSet res2 = getSearch.executeQuery();

               try (PreparedStatement delete = jdbc.prepareStatement(deleteSearchPref + "'" + uuid + "'"))
               {
                  delete.executeUpdate();
               }

               while (res2.next())
               {
                  String sid = res2.getString(1);

                  try (PreparedStatement delete4 = jdbc.prepareStatement(deleteSearchAdv + "'" + sid + "'"))
                  {
                     delete4.executeUpdate();
                  }

                  try (PreparedStatement delete3 = jdbc.prepareStatement(deleteSearch + "'" + sid + "'"))
                  {
                     delete3.executeUpdate();
                  }
               }
            }
            try (PreparedStatement delete2 = jdbc.prepareStatement(deletePref + "'" + uuid + "'"))
            {
               delete2.executeUpdate();
            }
         }

      }
      catch (DatabaseException | SQLException e)
      {
         LOGGER.error("An error occurred during removeDeletedUser", e);
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
