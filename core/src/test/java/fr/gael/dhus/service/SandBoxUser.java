/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2015,2019 GAEL Systems
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

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class SandBoxUser extends User
{
   private static final long serialVersionUID = 4203811510444372440L;
   private Integer sandBox;

   public SandBoxUser(String username, String password, boolean enabled,
         Integer sandBox, Collection<? extends GrantedAuthority> authorities)
   {
      super(username, password, enabled, true, true, true, authorities);
      this.sandBox = sandBox;
   }

   public Integer getSandBox()
   {
      return sandBox;
   }
}
