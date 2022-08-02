/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015 GAEL Systems
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
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;


public class DataManagerRoleCreation implements CustomTaskChange
{
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
         PreparedStatement getUsers =
               databaseConnection.prepareStatement (
                           "SELECT USER_ID,ROLES FROM USER_ROLES " +
                                 "WHERE ROLES = 'DATARIGHT_MANAGER' OR " +
                                 "ROLES = 'COLLECTION_MANAGER'");
          ResultSet res = getUsers.executeQuery ();
          Object prevId = null;
          while (res.next ())
          {
             // if we found two times the same user ID, user is DATARIGHT 
             // and COLLECTION manager. We can remove the last one to do
             // not create two times DATA MANAGER role for user.
             if (prevId == res.getObject ("USER_ID"))
             {
                PreparedStatement removeRole =
                   databaseConnection
                      .prepareStatement ("DELETE FROM USER_ROLES "+
                         "WHERE ROLES = 'COLLECTION_MANAGER' AND USER_ID = " +
                            res.getObject ("USER_ID"));
                removeRole.execute ();
                removeRole.close ();
             }
             prevId = res.getObject ("USER_ID");
          }
                  
         PreparedStatement updateRole =
            databaseConnection
               .prepareStatement ("UPDATE USER_ROLES SET ROLES = 'DATA_MANAGER"+
                  "' WHERE ROLES = 'DATARIGHT_MANAGER' OR " +
                     "ROLES = 'COLLECTION_MANAGER'");
         updateRole.execute ();
         updateRole.close ();
         getUsers.close ();
      }
      catch (Exception e)
      {
         e.printStackTrace ();
      }
   }

}
