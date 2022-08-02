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
package fr.gael.dhus.database.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class KeyGen
{

   /**
    * Hide utility class constructor
    */
   private KeyGen ()
   {

   }

   public static void main(String[] args)
   {
      try
      {
         Class.forName("org.hsqldb.jdbc.JDBCDriver");
         Connection con = DriverManager.getConnection(
               "jdbc:hsqldb:file:local_dhus/database/hsqldb", "SA", "");
         Statement stmt = con.createStatement();  
         ResultSet rs = stmt.executeQuery(
               "select CRYPT_KEY('AES', null) from USERS");
         rs.next();  
         String key = rs.getString(1); 
         System.out.println(key);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }  
   }
   
}
