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
package fr.gael.dhus.service;

import fr.gael.dhus.database.dao.TransfoParameterDao;
import fr.gael.dhus.database.object.TransfoParameter;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransfoParameterService
{
   @Autowired
   private TransfoParameterDao transfoParameterDao;

   @Transactional(readOnly = true)
   public Map<String, String> getParametersFromTransformation(String transformationUuid)
   {
      return transfoParameterDao.getParametersFromTransformation(transformationUuid)
            .stream()
            .collect(Collectors.toMap(param -> param.getKey().getName(), TransfoParameter::getValue));
   }
}
