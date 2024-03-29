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
package fr.gael.dhus.server.http.webapp.saml.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class AuthController
{
   @RequestMapping(value = "/**", method = {RequestMethod.GET})
   public void slashRedirect(HttpServletRequest req, HttpServletResponse res)
         throws IOException
   {
      res.sendRedirect("../");
   }
   
   @RequestMapping(value = "/auth", method = {RequestMethod.POST})
   public void auth(String returnUrl, HttpServletRequest req, HttpServletResponse res)
         throws IOException
   {
      if (returnUrl == null || returnUrl.isEmpty())
      {
         returnUrl = "/";
      }
      res.sendRedirect(returnUrl);
   }
}
