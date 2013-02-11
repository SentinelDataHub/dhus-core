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
package fr.gael.dhus.server.http.valve.processings;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ProcessingValveTest
{
   @DataProvider(name = "urlChecks")
   public Object[][] urlChecks()
   {
      String pattern_odata  = ".*/odata/v1/.*(?<!(\\$value))";
      String pattern_search = ".*/search\\?(q|rows|start|format|orderby).*";

      // Same pattern as ProcessingValve.DEFAULT_PATTERN
      String combined = "(.*/odata/v1/.*(?<!(\\$value)))|" // includes odata $values only
                      + "(.*/search\\?(q|rows|start|format|orderby).*)"; // or search query only

      String top100max = ".*(rows|\\$top)=0*[1-9]\\d{2,}.*";

      String sd_2004_1 = ".*(rows|\\$top)=\\b([0-9]|1[0-9]|2[0-5])\\b.*";
      String sd_2004_2 = ".*(rows|\\$top)=0*[1-9]\\d{2,}.*";

      return new Object[][]
      {
         { "http://schihub.copernicus.eu/dhus/odata/v1/Products('aazz')/$value", pattern_odata, false },
         { "http://schihub.copernicus.eu/dhus/odata/v1/Products('aazz')/$toto", pattern_odata, true },
         { "http://schihub.copernicus.eu/dhus/odata/v1/Products", pattern_odata, true },
         { "http://schihub.copernicus.eu/dhus/odata/v1/Products", pattern_odata, true },
         { "http://schihub.copernicus.eu/dhus/home", pattern_odata, false },
         { "http://schihub.copernicus.eu/dhus/home/value", pattern_odata, false },
         { "http://schihub.copernicus.eu/dhus/search?q=*", pattern_odata, false },
         { "http://schihub.copernicus.eu/dhus/search?q=*", pattern_search, true },
         { "http://schihub.copernicus.eu/dhus/search?q=*&rows=125&start=123&orderby=tot ee", pattern_search, true },

         { "http://schihub.copernicus.eu/dhus/odata/v1/Products('aazz')/$value", combined, false  },
         { "http://schihub.copernicus.eu/dhus/odata/v1/Products('aazz')/$toto", combined, true },
         { "http://schihub.copernicus.eu/dhus/odata/v1/Products", combined, true },
         { "http://schihub.copernicus.eu/dhus/home", combined, false },
         { "http://schihub.copernicus.eu/dhus/home/value", combined, false },
         { "http://schihub.copernicus.eu/dhus/search?q=*", combined, true },
         { "http://schihub.copernicus.eu/dhus/search?q=*&rows=125&start=123&orderby=tot ee", combined, true },

         { "http://schihub.copernicus.eu/dhus/odata/v1/Products('aazz')/$value", top100max, false },
         { "http://schihub.copernicus.eu/dhus/odata/v1/Products('aazz')/$toto", top100max, false },
         { "http://schihub.copernicus.eu/dhus/odata/v1/Products", top100max, false },
         { "http://schihub.copernicus.eu/dhus/home", top100max, false },
         { "http://schihub.copernicus.eu/dhus/home/value", top100max, false },
         { "http://schihub.copernicus.eu/dhus/search?q=*", top100max, false },
         { "http://schihub.copernicus.eu/dhus/search?q=*&rows=125&start=123&orderby=tot ee", top100max, true },
         { "http://schihub.copernicus.eu/dhus/search?q=*&rows=99&start=123&orderby=tot ee", top100max, false },
         { "http://schihub.copernicus.eu/dhus/search?q=*&rows=100&start=123&orderby=tot ee", top100max, true },
         { "http://schihub.copernicus.eu/dhus/search?q=*&rows=010&start=123&orderby=tot ee", top100max, false },
         { "http://schihub.copernicus.eu/dhus/search?q=*&rows=0100&start=123&orderby=tot ee", top100max, true },
         { "http://schihub.copernicus.eu/dhus/search?q=*&rows=09999&start=123&orderby=tot ee", top100max, true },
         { "http://schihub.copernicus.eu/dhus/search?q=*&rows=00012&start=123&orderby=tot ee", top100max, false },

         { "http://schihub.copernicus.eu/dhus/odata/v1/Products('aazz')?$top=10", top100max, false },
         { "http://schihub.copernicus.eu/dhus/odata/v1/Products('aazz')?$top=99", top100max, false },
         { "http://schihub.copernicus.eu/dhus/odata/v1/Products('aazz')?$top=100", top100max, true },
         { "http://schihub.copernicus.eu/dhus/odata/v1/Products('aazz')?$top=00100", top100max, true },
         { "http://schihub.copernicus.eu/dhus/odata/v1/Products('aazz')?$top=101", top100max, true },
         { "http://schihub.copernicus.eu/dhus/odata/v1/Products('aazz')?$top=01000", top100max, true },

         // SD-2004 [0-25]
         { "http://131.176.236.25/odata/v1/Products?$top=2", sd_2004_1, true },
         { "http://131.176.236.25/odata/v1/Products?$top=10", sd_2004_1, true },
         { "http://131.176.236.25/odata/v1/Products?$top=15", sd_2004_1, true },
         { "http://131.176.236.25/odata/v1/Products?$top=17", sd_2004_1, true },
         { "http://131.176.236.25/odata/v1/Products?$top=20", sd_2004_1, true },
         { "http://131.176.236.25/odata/v1/Products?$top=22", sd_2004_1, true },
         { "http://131.176.236.25/odata/v1/Products?$top=25", sd_2004_1, true },
         { "http://131.176.236.25/odata/v1/Products?$top=30", sd_2004_1, false },
         { "http://131.176.236.25/odata/v1/Products?$top=100", sd_2004_1, false },
         { "http://131.176.236.25/odata/v1/Products?$top=200", sd_2004_1, false },
         { "http://131.176.236.25/odata/v1/Products?$top=50", sd_2004_1, false },
         { "http://131.176.236.25/odata/v1/Products?$top=5", sd_2004_1, true },
         { "http://131.176.236.25/odata/v1/Products?$top=7", sd_2004_1, true },

         // SD-2004 comment #2 >99
         { "http://131.176.236.25/odata/v1/Products?$top=120", sd_2004_2, true },
         { "http://131.176.236.25/odata/v1/Products?$top=200", sd_2004_2, true },
         { "http://131.176.236.25/odata/v1/Products?$top=010", sd_2004_2, false },
         { "http://131.176.236.25/odata/v1/Products?$top=99", sd_2004_2, false },
         { "http://131.176.236.25/odata/v1/Products?$top=100", sd_2004_2, true },

         { "http://131.176.236.25/odata/v1/Products?$top=100", "", false },  // empty string shall match nothing
         { "http://131.176.236.25/odata/v1/Products?$top=100", " ", false }, // empty string shall match nothing

      };
   }

   @Test(dataProvider="urlChecks")
   public void checkUrlPatterns(String url, String pattern, boolean expectedResult)
   {
      Assert.assertEquals(url.matches(pattern), expectedResult);
   }
}
