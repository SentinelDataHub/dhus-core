/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2017 GAEL Systems
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
package fr.gael.dhus.spring.security.filter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.PortResolver;
import org.springframework.security.web.PortResolverImpl;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.DefaultSavedRequest.Builder;
import org.springframework.web.filter.GenericFilterBean;

import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.spring.context.SecurityContextProvider;
import fr.gael.dhus.spring.security.CookieKey;
import fr.gael.dhus.spring.security.saml.SAMLSavedRequestCache;
import fr.gael.dhus.system.config.ConfigurationManager;

public class GDPRAuthFilter extends GenericFilterBean 
{
   private static final Logger LOGGER = LogManager.getLogger(GDPRAuthFilter.class);

   private final static String HEADER_CONTENT_TYPE = "Content-type";
   private final static String HEADER_CONTENT_TYPE_URL_ENCODED = "application/x-www-form-urlencoded";
   private final static String EQUAL = "=";
   private final static String SEMICOLON = ";";
   private final static String SAML_REQUEST = "SAMLRequest";
   private final static String SAML_RESPONSE = "SAMLResponse";
   private final static String FORM_ACTION = "action";
   private final static String FORM_NAME = "name";
   private final static String FORM_VALUE = "value";
   private final static String FORM_USERNAME = "username";
   private final static String FORM_PASSWORD = "password";
   private final static String HEADER_COOKIE = "Cookie";
   private final static String HEADER_SET_COOKIE = "Set-Cookie";
   private final static String HEADER_LOCATION = "Location";
   private final static String HEADER_AUTHORIZATION = "Authorization";
   private final static String HEADER_AUTHORIZATION_BASIC = "Basic";

   private static final ConfigurationManager cfg =
         ApplicationContextProvider.getBean(ConfigurationManager.class);
   private static final SecurityContextProvider SEC_CTX_PROVIDER =
         ApplicationContextProvider.getBean(SecurityContextProvider.class);

   @Autowired 
   private SAMLSavedRequestCache requestCache;

   private PortResolver portResolver = new PortResolverImpl();
   /**
    * Try to authenticate a pre-authenticated user with Spring Security if the
    * user has not yet been authenticated.
    */
   public void doFilter (ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException
   {
      if (cfg.isGDPREnabled() && authenticationIsRequired()) 
      {
         HttpServletRequest req = (HttpServletRequest)request;
         String authHeader = req.getHeader(HEADER_AUTHORIZATION);
         String extPath = cfg.getServerConfiguration().getExternalPath();
         if (authHeader != null)
         {
            final int port = req.getServerPort();
            String samlUrl = String.format("%s://%s", req.getScheme(), req.getServerName());
            if (port != 80 && port != 443)
            {
               samlUrl = String.format("%s://%s:%d", req.getScheme(), req.getServerName(), port);
            }
            if(extPath != null && !extPath.isEmpty() && !"/".contentEquals(extPath))
            {
               samlUrl += extPath.startsWith("/") ? extPath : "/" + extPath;
            }
            samlUrl += samlUrl.endsWith("/") ? "saml/" : "/saml/";
            String login, password;
            login = password = null;
            StringTokenizer st = new StringTokenizer(authHeader);
            if (st.hasMoreTokens())
            {
               if (st.nextToken().equalsIgnoreCase(HEADER_AUTHORIZATION_BASIC))
               {
                  final String credentials = new String(Base64.getDecoder().decode(st.nextToken()),
                        StandardCharsets.UTF_8);
                  int p = credentials.indexOf(":");
                  if (p != -1)
                  {
                     login = credentials.substring(0, p).trim();
                     password = credentials.substring(p + 1).trim();
                  }
               }
            }
            Map<String, String> cookies = basicToSAMLAuthAndGetCookies(samlUrl, login, password);
            if (cookies != null)
            {
               SecurityContextHolder.setContext(
                  SEC_CTX_PROVIDER.getSecurityContext(cookies.get(CookieKey.INTEGRITY_COOKIE_NAME)));
            }
            chain.doFilter (new RemoveAuthorizationRequestWrapper((HttpServletRequest) request), response);
            return;
         }
         else
         {
            if(extPath != null && !extPath.isEmpty() && extPath.endsWith("/"))
            {
               extPath = extPath.substring(0, extPath.length() - 1);
            }
            // saved request must include external path to load initial url after keycloak auth
            Builder builder = new Builder()
					.setMethod(req.getMethod())
					.setScheme(req.getScheme())
					.setServerName(req.getServerName())
					.setServerPort(portResolver.getServerPort(req))
					.setContextPath(extPath + req.getContextPath())
					.setPathInfo(req.getPathInfo())
					.setQueryString(req.getQueryString())
					.setRequestURI(extPath + req.getRequestURI())
					.setParameters(req.getParameterMap())
					.setServletPath(req.getServletPath());
            DefaultSavedRequest savedRequest = builder.build();
            requestCache.save(req.getSession(true).getId(), savedRequest);

            // redirect to SAML webapp to process to SSO login
            if(extPath == null || extPath.trim().isEmpty())
            {
               // if no external path, then resirect to root
               ((HttpServletResponse)response).sendRedirect("/../saml/");
            }
            else
            {
               // if external path, then redirect to ../external_path/saml/
               ((HttpServletResponse)response).sendRedirect(extPath + "/saml/");
            }
            return;
         }
      }
      chain.doFilter (request, response);
   }
   
   private boolean authenticationIsRequired() 
   {
      Authentication existingAuth = SecurityContextHolder.getContext()
            .getAuthentication();

      if (existingAuth == null || !existingAuth.isAuthenticated())
      {
         return true;
      }

      return false;
   }

   private static Map<String, String> basicToSAMLAuthAndGetCookies(final String url, final String user, final String pwd)
   {
      Map<String, String> cookies = null;
      HttpClient client = HttpClientBuilder.create().build();
      HttpGet getRequest = new HttpGet();
      HttpPost postRequest = new HttpPost();
      do
      {
         try
         {
            ///////////////////
            //// GET http://dhus_ip:port/odata/v1/Products (example)
            ///////////////////
            HttpResponse response = get(client, getRequest, url, null);
            if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
            {
               break;
            }
            ///////////////////
            //// POST http://keycloak_ip:port/auth/realms/dhus/protocol/saml (example)
            //// + data : SAMLRequest
            ///////////////////
            String content = EntityUtils.toString(response.getEntity());
            List <NameValuePair> nvps = new ArrayList <NameValuePair>();
            nvps.add(new BasicNameValuePair(SAML_REQUEST, getFormFieldValueByName(content, SAML_REQUEST)));
            response = post(client, postRequest, sanitizeUrl(getFormFieldValueByName(content, FORM_ACTION)), nvps);
            if(response.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY)
            {
               break;
            }
            ///////////////////
            //// GET http://keycloak_ip:port/auth/realms/dhus/login-actions/authenticate?client_id=dhus_client&tab_id=4LruigNgVCs (example)
            //// + cookies : AUTH_SESSION_ID_LEGACY + KC_RESTART + ...
            ///////////////////
            response = get(client, getRequest, response.getFirstHeader(HEADER_LOCATION).getValue(), getSetCookieHeaderAsString(response.getHeaders(HEADER_SET_COOKIE)));
            if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
            {
               break;
            }
            ///////////////////
            //// POST http://keycloak_ip:port/auth/realms/dhus/login-actions/authenticate?session_code=tC6iF4jbxNccd0SjR2a9ZMG6EA_Ln_uTT-auoK597W4&execution=ccdef0c0-2ef2-429a-acbc-1fd5152e92e9&
            //// client_id=dhus_client&tab_id=4LruigNgVCs (example)
            //// + data : username + password
            ///////////////////
            content = EntityUtils.toString(response.getEntity());
            nvps.clear();
            nvps.add(new BasicNameValuePair(FORM_USERNAME, user));
            nvps.add(new BasicNameValuePair(FORM_PASSWORD, pwd));
            response = post(client, postRequest, sanitizeUrl(getFormFieldValueByName(content, FORM_ACTION)), nvps);
            if(response.getStatusLine().getStatusCode() !=  HttpStatus.SC_OK)
            {
               break;
            }
            ///////////////////
            //// POST http://dhus_ip:port/saml/saml/SSO (example)
            //// + data : SAMLResponse
            ///////////////////
            content = EntityUtils.toString(response.getEntity());
            nvps.clear();
            nvps.add(new BasicNameValuePair(SAML_RESPONSE, getFormFieldValueByName(content, SAML_RESPONSE)));
            response = post(client, postRequest, getFormFieldValueByName(content, FORM_ACTION), nvps);
            if(response.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY)
            {
               break;
            }
            cookies = getSetCookieHeaderAsMap(response.getHeaders(HEADER_SET_COOKIE));
         }
         catch(IOException e)
         {
            LOGGER.error("Authentication error : Basic to SAML authentication processing.");
            LOGGER.error(e.getMessage());
         }
      }
      while(false);

      if (getRequest != null)
      {
         getRequest.releaseConnection();
      }
      if(postRequest != null)
      {
         postRequest.releaseConnection();
      }
      return cookies;
   }

   private static String getFormFieldValueByName(final String form, final String field)
   {
      final int nameIndex = form.indexOf(String.format("%s=\"%s\"",FORM_NAME, field));
      if (nameIndex > 0)
      {
         final int valueIndex = form.indexOf(String.format("%s=\"", FORM_VALUE), nameIndex) + 7;
         return form.substring(valueIndex, form.indexOf("\"", valueIndex));
      }
      final int fieldIndex = form.indexOf(String.format("%s=\"", field)) + field.length() + 2;
      if(fieldIndex > 0)
      {
         return form.substring(fieldIndex, form.indexOf("\"", fieldIndex));
      }
      return null;
   }

   private static String sanitizeUrl(final String url)
   {
      return url.replaceAll("&#x3a;", ":").replaceAll("&#x2f;", "/").replaceAll("&amp;", "&");
   }

   private static String getSetCookieHeaderAsString(final Header[] headers)
   {
      StringBuilder sb = new StringBuilder();
      for (Header head : headers)
      {
         for (HeaderElement el : head.getElements())
         {
            final String name = el.getName();
            sb.append(name);
            sb.append(EQUAL);
            sb.append(el.getValue());
            sb.append(SEMICOLON);
         }
      }
      return sb.toString();
   }

   private static Map<String, String> getSetCookieHeaderAsMap(final Header[] headers)
   {
      Map<String, String> result = new HashMap<String, String>();
      for (Header head : headers)
      {
         for (HeaderElement el : head.getElements())
         {
            result.put(el.getName(), el.getValue());
         }
      }

      return result;
   }

   private static HttpResponse get(final HttpClient client, final HttpGet get, final String url, final String cookie)
   {
      HttpResponse result = null;
      if(client == null || get == null || url == null || url.isEmpty())
      {
         return result;
      }
      try
      {
         get.reset();
         get.setURI(new URI(url));
         if(cookie != null && !cookie.isEmpty())
         {
            get.setHeader(HEADER_COOKIE, cookie);
         }
         result = client.execute(get);
      }
      catch (IOException | URISyntaxException e)
      {
         e.printStackTrace();
      }
      return result;
   }

   private static HttpResponse post(final HttpClient client, final HttpPost post, final String url, final List <NameValuePair> data)
   {
      HttpResponse result = null;
      if(client == null || post == null || url == null || url.isEmpty())
      {
         return result;
      }
      try
      {
         post.reset();
         post.setURI(new URI(url));
         post.setHeader(HEADER_CONTENT_TYPE, HEADER_CONTENT_TYPE_URL_ENCODED);
         post.setEntity(new UrlEncodedFormEntity(data));
         result = client.execute(post);
      }
      catch (IOException | URISyntaxException e)
      {
         e.printStackTrace();
      }
      return result;
   }
}
