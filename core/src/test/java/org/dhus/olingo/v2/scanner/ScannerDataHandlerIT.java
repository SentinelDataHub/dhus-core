/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018 GAEL Systems
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
package org.dhus.olingo.v2.scanner;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotSame;

import fr.gael.dhus.datastore.scanner.ScannerStatus;

import java.io.IOException;
import javax.servlet.ServletException;

import org.apache.olingo.commons.api.http.HttpStatusCode;

import org.dhus.olingo.v2.web.DHuSODataServletIT;

import org.json.JSONException;
import org.json.JSONObject;

import org.springframework.mock.web.MockHttpServletResponse;

import org.testng.annotations.Test;

public class ScannerDataHandlerIT extends DHuSODataServletIT
{

   @Test
   public void getCollection() throws ServletException, IOException
   {
      request = new RequestBuilder()
            .withURI("Scanners")
            .withMethod("GET")
            .build();

      servlet.service(request, response);

      assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatus());
   }

   @Test
   public void createEmptyShouldReturn400() throws ServletException, IOException
   {
      request = new RequestBuilder()
            .withURI("Scanners")
            .withMethod("POST")
            .withBody("")
            .build();

      servlet.service(request, response);

      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.getStatus());
   }

   @Test
   public void createEmptyObjectShouldReturn400() throws ServletException, IOException
   {
      request = new RequestBuilder()
            .withURI("Scanners")
            .withMethod("POST")
            .withBody("{}")
            .build();

      servlet.service(request, response);

      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.getStatus());
   }

   @Test
   public void createNoTypeShouldReturn400() throws ServletException, IOException
   {
      request = new RequestBuilder()
            .withURI("Scanners")
            .withMethod("POST")
            .withBody("{"
                  + " \"@adata.type\" : \"#OData.DHuS.FileScanner\" ,"
                  + " \"Url\" : \"/tmp\" ,"
                  + " \"Pattern\" : \"zip\""
                  + "}")
            .build();

      servlet.service(request, response);

      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.getStatus());
   }

   @Test
   public void createWithNoUrlShouldReturn400() throws ServletException, IOException
   {
      request = new RequestBuilder()
            .withURI("Scanners")
            .withMethod("POST")
            .withBody("{"
                  + " \"@odata.type\" : \"#OData.DHuS.FileScanner\" ,"
                  + " \"Pattern\" : \"zip\""
                  + "}")
            .build();

      servlet.service(request, response);

      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.getStatus());
   }

   @Test
   public void createWithNoUsernameShouldReturn200() throws ServletException, IOException
   {
      request = new RequestBuilder()
            .withURI("Scanners")
            .withMethod("POST")
            .withBody("{"
                  + " \"@odata.type\" : \"#OData.DHuS.FileScanner\" ,"
                  + " \"Url\" : \"/tmp\" ,"
                  + " \"Pattern\" : \"zip\" ,"
                  + " \"Username\" : \"quan\""
                  + "}")
            .build();

      servlet.service(request, response);

      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.getStatus());
   }

   @Test
   public void createFileScannerOK() throws ServletException, IOException
   {
      request = new RequestBuilder()
            .withURI("Scanners")
            .withMethod("POST")
            .withBody("{"
                  + " \"@odata.type\" : \"#OData.DHuS.FileScanner\" ,"
                  + " \"Url\" : \"/tmp\" ,"
                  + " \"Pattern\" : \"zip\""
                  + "}")
            .build();

      servlet.service(request, response);

      assertEquals(HttpStatusCode.CREATED.getStatusCode(), response.getStatus());
   }

   @Test
   public void createFtpScannerOK() throws ServletException, IOException
   {
      request = new RequestBuilder()
            .withURI("Scanners")
            .withMethod("POST")
            .withBody("{"
                  + " \"@odata.type\" : \"#OData.DHuS.FtpScanner\","
                  + " \"Url\" : \"ftp://test.test\","
                  + " \"Pattern\" : \"zip\","
                  + " \"Username\" : \"quan\","
                  + " \"Password\" : \"secret\""
                  + "}")
            .build();

      servlet.service(request, response);

      assertEquals(HttpStatusCode.CREATED.getStatusCode(), response.getStatus());
   }

   @Test
   public void createScannerWithCronOK() throws ServletException, IOException
   {
      request = new RequestBuilder()
            .withURI("Scanners")
            .withMethod("POST")
            .withBody("{"
                  + " \"@odata.type\" : \"#OData.DHuS.FileScanner\" ,"
                  + " \"Url\" : \"/tmp\" ,"
                  + " \"Cron\" : { \"Active\" : true, \"Schedule\" : \"0 0 21 ? * * \"},"
                  + " \"Pattern\" : \"zip\""
                  + "}")
            .build();

      servlet.service(request, response);

      assertEquals(HttpStatusCode.CREATED.getStatusCode(), response.getStatus());
   }

   @Test
   public void createScannerWithInvalidDatatype() throws ServletException, IOException
   {
      request = new RequestBuilder()
            .withURI("Scanners")
            .withMethod("POST")
            .withBody("{"
                  + " \"@odata.type\" : \"#OData.DHuS.FileScanner\" ,"
                  + " \"Url\" : \"/tmp\" ,"
                  + " \"Cron\" : { \"Active\" : 123, \"Schedule\" : \"olalala\"},"
                  + " \"Pattern\" : \"zip\""
                  + "}")
            .build();

      servlet.service(request, response);

      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.getStatus());
   }

   @Test
   public void updateScannerOK() throws ServletException, IOException, JSONException
   {
      String id = createFileScannerAndReturnID();
      request = new RequestBuilder()
            .withURI("Scanners(" + id + ")")
            .withMethod("PATCH")
            .withBody("{"
                  + " \"Url\" : \"/tada\" "
                  + "}")
            .build();
      response = new MockHttpServletResponse();

      servlet.service(request, response);
      assertEquals(HttpStatusCode.NO_CONTENT.getStatusCode(), response.getStatus());
   }

   @Test
   public void updateFilesCannerWithUsernameShouldReturn400() throws ServletException, IOException, JSONException
   {
      String id = createFileScannerAndReturnID();
      request = new RequestBuilder()
            .withURI("Scanners(" + id + ")")
            .withMethod("PATCH")
            .withBody("{"
                  + " \"Username\" : \"quan\" "
                  + "}")
            .build();
      response = new MockHttpServletResponse();

      servlet.service(request, response);
      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.getStatus());
   }

   @Test
   public void updateFileCannerWithUserNameShouldHaveNoEffect() throws ServletException, IOException, JSONException
   {
      String id = createFileScannerAndReturnID();
      request = new RequestBuilder()
            .withURI("Scanners(" + id + ")")
            .withMethod("PATCH")
            .withBody("{"
                  + " \"@odata.type\" : \"#OData.DHuS.FtpScanner\" ,"
                  + " \"Username\" : \"quan\" "
                  + "}")
            .build();
      response = new MockHttpServletResponse();

      servlet.service(request, response);
      assertEquals(HttpStatusCode.NO_CONTENT.getStatusCode(), response.getStatus());
   }

   @Test
   public void updateFtpScannerWithUsernameOK() throws ServletException, IOException, JSONException
   {
      String id = createFTPScannerAndReturnID();
      request = new RequestBuilder()
            .withURI("Scanners(" + id + ")")
            .withMethod("PATCH")
            .withBody("{"
                  + " \"@odata.type\" : \"#OData.DHuS.FtpScanner\" ,"
                  + " \"Username\" : \"quan\" "
                  + "}")
            .build();
      response = new MockHttpServletResponse();

      servlet.service(request, response);
      assertEquals(HttpStatusCode.NO_CONTENT.getStatusCode(), response.getStatus());
   }

   @Test
   public void updateFtpScannerWithWrongType400() throws ServletException, IOException, JSONException
   {
      String id = createFTPScannerAndReturnID();
      request = new RequestBuilder()
            .withURI("Scanners(" + id + ")")
            .withMethod("PATCH")
            .withBody("{"
                  + " \"@odata.type\" : \"#OData.DHuS.FileScanner\" ,"
                  + " \"Username\" : \"quan\" "
                  + "}")
            .build();
      response = new MockHttpServletResponse();

      servlet.service(request, response);
      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.getStatus());
   }

   @Test
   public void updateUrlToNullShouldReturn400() throws ServletException, IOException, JSONException
   {
      String id = createFileScannerAndReturnID();
      request = new RequestBuilder()
            .withURI("Scanners(" + id + ")")
            .withMethod("PATCH")
            .withBody("{"
                  + " \"Url\" : null "
                  + "}")
            .build();
      response = new MockHttpServletResponse();

      servlet.service(request, response);
      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.getStatus());
   }

   @Test
   public void deleteScannerOK() throws ServletException, IOException, JSONException
   {
      String id = createFileScannerAndReturnID();
      request = new RequestBuilder()
            .withURI("Scanners(" + id + ")")
            .withMethod("DELETE")
            .build();
      response = new MockHttpServletResponse();

      servlet.service(request, response);
      assertEquals(HttpStatusCode.NO_CONTENT.getStatusCode(), response.getStatus());
   }

   @Test
   public void deleteInvalidId() throws ServletException, IOException, JSONException
   {
      String id = createFileScannerAndReturnID();
      request = new RequestBuilder()
            .withURI("Scanners(" + id + "lalala )")
            .withMethod("DELETE")
            .build();
      response = new MockHttpServletResponse();

      servlet.service(request, response);
      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.getStatus());
   }

   @Test
   public void startScannerOK() throws ServletException, IOException, JSONException
   {
      String id = createFileScannerAndReturnID();
      String URI = "Scanners(" + id + ")" + "/OData.DHuS.StartScanner";
      request = new RequestBuilder()
            .withURI(URI)
            .withMethod("POST")
            .withBody("{}")
            .build();
      response = new MockHttpServletResponse();

      servlet.service(request, response);

      assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatus());
   }

   @Test
   public void startScannerInvalidBodyShouldReturn200() throws ServletException, IOException, JSONException
   {
      String id = createFileScannerAndReturnID();
      String URI = "Scanners(" + id + ")" + "/OData.DHuS.StartScanner";
      request = new RequestBuilder()
            .withURI(URI)
            .withMethod("POST")
            .build();
      response = new MockHttpServletResponse();

      servlet.service(request, response);

      assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatus());
   }

   @Test
   public void stopScannerOK() throws ServletException, IOException, JSONException
   {
      String id = createFileScannerAndReturnID();
      String URI = "Scanners(" + id + ")" + "/OData.DHuS.StopScanner";
      request = new RequestBuilder()
            .withURI(URI)
            .withMethod("POST")
            .withBody("{}")
            .build();
      response = new MockHttpServletResponse();

      servlet.service(request, response);

      assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatus());
   }

   @Test
   public void stopScannerEmptyBodyShouldReturn400() throws ServletException, IOException, JSONException
   {
      String id = createFileScannerAndReturnID();
      String URI = "Scanners(" + id + ")" + "/OData.DHuS.StopScanner";
      request = new RequestBuilder()
            .withURI(URI)
            .withMethod("POST")
            .build();
      response = new MockHttpServletResponse();

      servlet.service(request, response);

      assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatus());
   }

   @Test
   public void getScannerStatusAddedOK() throws ServletException, IOException, JSONException
   {
      String id = createFileScannerAndReturnID();
      String URI = "Scanners(" + id + ")";
      request = new RequestBuilder()
            .withURI(URI)
            .withMethod("GET")
            .build();
      response = new MockHttpServletResponse();

      servlet.service(request, response);

      assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatus());
      String status = new JSONObject(response.getContentAsString()).getJSONObject("ScannerStatus").getString("Status");
      assertEquals(ScannerStatus.STATUS_ADDED, status);
   }

   @Test
   public void getScannerStatusAfterChange() throws ServletException, IOException, JSONException
   {
      // Prepare
      String id = createFileScannerAndReturnID();
      String URI = "Scanners(" + id + ")" + "/OData.DHuS.StartScanner";
      request = new RequestBuilder()
            .withURI(URI)
            .withMethod("POST")
            .withBody("{}")
            .build();
      response = new MockHttpServletResponse();
      servlet.service(request, response);
      URI = "Scanners(" + id + ")";
      request = new RequestBuilder()
            .withURI(URI)
            .withMethod("GET")
            .build();
      response = new MockHttpServletResponse();

      // Do
      servlet.service(request, response);

      // Verify
      assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatus());
      String status = new JSONObject(response.getContentAsString()).getJSONObject("ScannerStatus").getString("Status");
      assertNotSame("added", status);
   }

   private String createFileScannerAndReturnID() throws ServletException, IOException, JSONException
   {
      request = new RequestBuilder()
            .withURI("Scanners")
            .withMethod("POST")
            .withBody("{"
                  + " \"@odata.type\" : \"#OData.DHuS.FileScanner\" ,"
                  + " \"Url\" : \"/tmp\" ,"
                  + " \"Cron\" : { \"Active\" : true, \"Schedule\" : \"0 0 21 ? * *\"},"
                  + " \"Pattern\" : \"zip\""
                  + "}")
            .build();
      response = new MockHttpServletResponse();

      servlet.service(request, response);

      assertEquals(HttpStatusCode.CREATED.getStatusCode(), response.getStatus());
      String result = response.getContentAsString();
      JSONObject obj = new JSONObject(result);
      return obj.getString("Id");
   }

   private String createFTPScannerAndReturnID() throws ServletException, IOException, JSONException
   {
      request = new RequestBuilder()
            .withURI("Scanners")
            .withMethod("POST")
            .withBody("{"
                  + " \"@odata.type\" : \"#OData.DHuS.FtpScanner\" ,"
                  + " \"Url\" : \"ftp://localhost.test\" ,"
                  + " \"Cron\" : { \"Active\" : true, \"Schedule\" : \"0 0 21 ? * *\"},"
                  + " \"Pattern\" : \"zip\""
                  + "}")
            .build();
      response = new MockHttpServletResponse();

      servlet.service(request, response);

      assertEquals(HttpStatusCode.CREATED.getStatusCode(), response.getStatus());
      String result = response.getContentAsString();
      JSONObject obj = new JSONObject(result);
      return obj.getString("Id");
   }

}
