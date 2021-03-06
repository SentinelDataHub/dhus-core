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
package fr.gael.dhus.olingo.v1.map.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.gael.dhus.olingo.v1.entity.Network;
import fr.gael.dhus.olingo.v1.map.AbstractDelegatingMap;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;

/**
 * A map view on Synchronizers.
 *
 * @see AbstractDelegatingMap
 */
public class NetworkMap extends AbstractDelegatingMap<Integer, Network>
{
   private final Network network;

   /**
    * Creates a new map view.
    */
   public NetworkMap ()
   {
      this.network = new Network ();
   }

   @Override
   protected Iterator<Network> serviceIterator ()
   {
      List<Network> list = new ArrayList<Network> ();
      list.add (network);
      return list.iterator ();
   }

   @Override
   protected int serviceCount ()
   {
      return 1;
   }

   @Override
   protected Network serviceGet (Integer key)
   {
      return network;
   }
}
