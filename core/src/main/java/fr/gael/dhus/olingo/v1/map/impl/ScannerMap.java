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
package fr.gael.dhus.olingo.v1.map.impl;

import fr.gael.dhus.olingo.v1.entity.Scanner;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.gael.dhus.olingo.v1.map.FunctionalMap;
import fr.gael.dhus.olingo.v1.visitor.ScannerFunctionalVisitor;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import org.dhus.scanner.ScannerContainer;

public class ScannerMap extends FunctionalMap<Long, Scanner>
{
   public ScannerMap()
   {
      super(toMap(ApplicationContextProvider.getBean(ScannerContainer.class).getScanners()), new ScannerFunctionalVisitor());
   }

   private static Map<Long, Scanner> toMap(final List<fr.gael.dhus.datastore.scanner.Scanner> list)
   {
      HashMap<Long, Scanner> res = new HashMap<>();

      for (fr.gael.dhus.datastore.scanner.Scanner scanner: list)
      {
         res.put(scanner.getConfig().getId(), new Scanner(scanner));
      }
      return res;
   }
}
