/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2015,2016,2018 GAEL Systems
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
package fr.gael.dhus.olingo.v1.map.impl;

import fr.gael.dhus.olingo.v1.entity.Connection;
import fr.gael.dhus.olingo.v1.map.FunctionalMap;
import fr.gael.dhus.olingo.v1.visitor.ConnectionFunctionalVisitor;
import fr.gael.dhus.server.http.valve.AccessValve;


/**
 * A map view on Connections.
 */
public class ConnectionMap extends FunctionalMap<String, Connection>
{
   /**
    * Creates a new ConnectionMap.
    * @param username prints connections from this user only.
    */
   public ConnectionMap(String username) {
      super(AccessValve.getConnections (username), new ConnectionFunctionalVisitor());
   }

   /**
    * Creates a new ConnectionMap.
    */
   public ConnectionMap() {
      super(AccessValve.getConnections (null), new ConnectionFunctionalVisitor());
   }
}
