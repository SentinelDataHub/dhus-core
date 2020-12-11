/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019 GAEL Systems
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
package fr.gael.dhus.util;

import org.dhus.store.datastore.remotedhus.DhusODataV1Node;

import fr.gael.drb.DrbNode;

//TODO fix drb and delete this class
public class DrbChildren
{
   public static boolean shouldIngestionUseFirstChild(String pathToNode, DrbNode node)
   {
      return (UnZip.supported(pathToNode) && node.getChildrenCount() == 1) // performance?
            || node instanceof DhusODataV1Node; // always true?
   }

   public static boolean shouldODataUseFirstChild(String pathToNode, DrbNode node)
   {
      return (UnZip.hasArchiveExtension(pathToNode) && node.getChildrenCount() == 1)
            || node instanceof DhusODataV1Node;
   }
}
