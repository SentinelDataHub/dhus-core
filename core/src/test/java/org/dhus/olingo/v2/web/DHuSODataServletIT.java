/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018,2019 GAEL Systems
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
package org.dhus.olingo.v2.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.util.TestContextLoader;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;

@ContextConfiguration(locations = "classpath:fr/gael/dhus/spring/context-odata-test.xml", loader = TestContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DHuSODataServletIT extends AbstractTestNGSpringContextTests
{

   protected DHuSODataServlet servlet;
   protected MockHttpServletRequest request;
   protected MockHttpServletResponse response;

   @BeforeClass
   public void setup()
   {
      authenticate();
      servlet = new DHuSODataServlet();
   }

   @BeforeTest
   public void setupRequest()
   {
      request = new MockHttpServletRequest();
      response = new MockHttpServletResponse();
   }

   private void authenticate()
   {
      String name = "user";
      String pwd = "password";
      Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
      grantedAuthorities.add(new SimpleGrantedAuthority(Role.DOWNLOAD.getAuthority()));
      grantedAuthorities.add(new SimpleGrantedAuthority(Role.SEARCH.getAuthority()));
      grantedAuthorities.add(new SimpleGrantedAuthority(Role.USER_MANAGER.getAuthority()));
      grantedAuthorities.add(new SimpleGrantedAuthority(Role.SYSTEM_MANAGER.getAuthority()));

      List<Role> roleList = Collections.singletonList(Role.SYSTEM_MANAGER);
      fr.gael.dhus.database.object.User dbUser = mock(fr.gael.dhus.database.object.User.class);
      when(dbUser.getAuthorities()).thenReturn(grantedAuthorities);
      when(dbUser.getRoles()).thenReturn(roleList);
      when(dbUser.getUsername()).thenReturn(name);
      when(dbUser.getPassword()).thenReturn(pwd);

      Authentication auth = new UsernamePasswordAuthenticationToken(dbUser, pwd, grantedAuthorities);
      SecurityContextHolder.getContext().setAuthentication(auth);
   }

   public class RequestBuilder
   {
      private MockHttpServletRequest r = new MockHttpServletRequest();

      public RequestBuilder()
      {
         r.setContextPath("/odata");
         r.setServletPath("/v2");
         r.setContentType("application/json");
      }

      public RequestBuilder withMethod(String s)
      {
         r.setMethod(s);
         return this;
      }

      public RequestBuilder withBody(String s)
      {
         try
         {
            r.setContent(s.getBytes("UTF-8"));
         }
         catch (UnsupportedEncodingException e)
         {
            e.printStackTrace();
         }
         return this;
      }

      public RequestBuilder withURI(String s)
      {
         r.setRequestURI(r.getContextPath() + r.getServletPath() + "/" + s);
         return this;
      }

      public MockHttpServletRequest build()
      {
         return r;
      }
   }

}
