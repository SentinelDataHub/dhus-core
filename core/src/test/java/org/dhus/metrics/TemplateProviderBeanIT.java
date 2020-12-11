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
package org.dhus.metrics;

import static org.testng.Assert.assertTrue;

import eu.metrics.spring.TemplateProviderBean;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import org.testng.annotations.Test;

/**
 * Test all the patterns defined in the `dhus-core-monitoring.xml` bean declaration file.
 * Please keep the ordering of the methods, so it matches the ordering in the XML document.
 * Method names here do not follow the standard naming conventions to better match metric names from
 * the XML doc.
 */
@ContextConfiguration(locations = { "classpath:fr/gael/dhus/spring/dhus-core-monitoring.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TemplateProviderBeanIT extends AbstractTestNGSpringContextTests
{
   private static final Logger LOGGER = LogManager.getLogger();

   @Autowired
   private TemplateProviderBean templateProvider;

   private final Map<String, Pattern> compiledPattern = new HashMap<>();

   // Array of all known product ItemClasses
   private static final String[] ITEM_CLS =
   {
      "unknown",
      // COARSE precision
      "s1",
      "s2",
      "s3",
      "s5p",
      "sentinel-aux",
      // FINE precision
      "s1_EWlevel0sdualproduct",
      "s1_EWlevel1sS1Aproduct",
      "s1_IWlevel0sdualproduct",
      "s1_IWlevel1sS1Aproduct",
      "s1_SMlevel0dualsproduct",
      "s1_WVlevel2product",
      "s2_userLevel1CProduct",
      "s2_userLevel2AProduct",
      "s3_ipf_olci_level_1",
      "s3_ipf_olci_level_2",
      "s3_ipf_slstr_level_1",
      "s3_ipf_slstr_level_2",
      "s3_ipf_sral_level_1_2_no_cal",
      "s3_ipf_sral_level_2",
      "s3_ipf_syn_level_2_syn",
      "s3_ipf_syn_level_2_vgp",
      "s5p_auxiliaryCH4",
      "s5p_auxiliaryCO",
      "s5p_auxiliaryMet2D",
      "s5p_auxiliaryMetQp",
      "s5p_auxiliaryMetTp",
      "s5p_auxiliaryNise",
      "s5p_productLevel1BIrSir",
      "s5p_productLevel1BIrUvn",
      "s5p_productLevel1BRaBd1",
      "s5p_productLevel1BRaBd2",
      "s5p_productLevel1BRaBd3",
      "s5p_productLevel1BRaBd4",
      "s5p_productLevel1BRaBd5",
      "s5p_productLevel1BRaBd6",
      "s5p_productLevel1BRaBd7",
      "s5p_productLevel1BRaBd8",
      "s5p_productLevel2",
      "s5p_productLevel2AerAI",
      "s5p_productLevel2AerLH",
      "s5p_productLevel2CH4",
      "s5p_productLevel2CO",
      "s5p_productLevel2Fresco",
      "s5p_productLevel2HCHO",
      "s5p_productLevel2O3",
      "s5p_productLevel2O3Pr",
      "s5p_productLevel2O3Tpr",
      "s5p_productLevel2SO2",
      "sentinel-aux_dailyGnssProduct"
   };

   // The two connectors that are defined by default
   private static final String[] CONNECTORS =
   {
      "8081", "33000"
   };

   // A few well known names plus random names validating the USERNAME_PATTERN in the UserService
   private static final String[] USER_NAMES =
   {
      "root",
      "~default", "~none",
      "a",
      "foo.bar", "foo.", ".bar",
      "foo2bar", "foo2", "2bar",
      "foo_bar", "foo_", "_bar",
      "foo-bar", "foo-", "-bar",
      "-_._-"
   };

   // A few known context (servlet) names
   private static final String[] CTX_NAMES =
   {
      "solr", "odata", "search", "none"
   };

   // All known cache name in DHuS core
   private static final String[] CACHE_NAMES =
   {
      "product",
      "product_count",
      "product_eviction_date",
      "user_requests",
      "current_quotas",
      "user_connections",
      "products",
      "network_download_count",
      "userByName",
      "indexes",
      "network_download_size",
      "user",
      "security_context"
   };

   /** Must not throw a PatternSyntaxException. */
   @Test
   public void testPatternsDoCompile()
   {
      templateProvider.getTemplates().forEach((name, pattern) -> compiledPattern.put(name, Pattern.compile(pattern.pattern())));
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_prod_sync_total_queued_downloads()
   {
      assertTrue(testPatternAndTags("prod_sync_total_queued_downloads", null, "prod_sync.global.gauges"));
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_prod_sync_timer()
   {
      assertTrue(testPatternAndTags("prod_sync_timer", null, "prod_sync.sync314.timer"));
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_prod_sync_counters()
   {
      for (String itemClass: ITEM_CLS)
      {
         assertTrue(testPatternAndTags("prod_sync_counters", null, "prod_sync.sync0.counters." + itemClass));
      }
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_prod_sync_gauges()
   {
      assertTrue(testPatternAndTags("prod_sync_gauges", null, "prod_sync.sync3.gauges"));
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_prod_sync_timeliness_creation()
   {
      assertTrue(testPatternAndTags("prod_sync_timeliness_creation", null, "prod_sync.sync1.timeliness.creation"));
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_prod_sync_timeliness_ingestion()
   {
      assertTrue(testPatternAndTags("prod_sync_timeliness_ingestion", null, "prod_sync.sync1.timeliness.ingestion"));
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_access_timer()
   {
      for (String connector: CONNECTORS)
      {
         for (String ctx: CTX_NAMES)
         {
            assertTrue(testPatternAndTags("access_timer", new String[] { "connector", "context" }, "access." + connector + "." + ctx + ".timer"));
            for (String username: USER_NAMES)
            {
               assertTrue(testPatternAndTags("access_timer", null, "access." + connector + "." + ctx + "." + username + ".timer"));
            }
         }
      }
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_access_counters()
   {
      for (String connector: CONNECTORS)
      {
         for (String ctx: CTX_NAMES)
         {
            assertTrue(testPatternAndTags("access_counters", new String[] { "connector", "context" }, "access." + connector + "." + ctx + ".counters"));
            for (String username: USER_NAMES)
            {
               assertTrue(testPatternAndTags("access_counters", null, "access." + connector + "." + ctx + "." + username + ".counters"));
            }
         }
      }
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_ingestion_timer()
   {
     for (String itemClass: ITEM_CLS)
      {
         assertTrue(testPatternAndTags("ingestion_timer", null, "ingestion." + itemClass + ".timer"));
      }
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_ingestion_counters()
   {
     for (String itemClass: ITEM_CLS)
      {
         assertTrue(testPatternAndTags("ingestion_counters", null, "ingestion." + itemClass + ".counters"));
      }
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_downloads_start_meter()
   {
     assertTrue(testPatternAndTags("downloads_start_meter", new String[] {}, "downloads.start.meter"));
     for (String itemClass: ITEM_CLS)
      {
         assertTrue(testPatternAndTags("downloads_start_meter", new String[] { "itemclass" }, "downloads.start." + itemClass + ".meter"));
         for (String username: USER_NAMES)
         {
            assertTrue(testPatternAndTags("downloads_start_meter", null, "downloads.start." + itemClass + ".username:" + username + ".meter"));
         }
      }
     for (String username: USER_NAMES)
      {
         assertTrue(testPatternAndTags("downloads_start_meter", new String[] { "username" }, "downloads.start.username:" + username + ".meter"));
      }
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_downloads_end_meter()
   {
      assertTrue(testPatternAndTags("downloads_end_meter", new String[] { "status" }, "downloads.end.success.meter"));
      assertTrue(testPatternAndTags("downloads_end_meter", new String[] { "status" }, "downloads.end.failure.meter"));
      for (String itemClass: ITEM_CLS)
      {
         assertTrue(testPatternAndTags("downloads_end_meter", new String[] { "itemclass", "status" }, "downloads.end." + itemClass + ".success.meter"));
         assertTrue(testPatternAndTags("downloads_end_meter", new String[] { "itemclass", "status" }, "downloads.end." + itemClass + ".failure.meter"));
         for (String username: USER_NAMES)
         {
            assertTrue(testPatternAndTags("downloads_end_meter", null, "downloads.end." + itemClass + ".username:" + username + ".success.meter"));
            assertTrue(testPatternAndTags("downloads_end_meter", null, "downloads.end." + itemClass + ".username:" + username + ".failure.meter"));
         }
      }
      for (String username: USER_NAMES)
      {
         assertTrue(testPatternAndTags("downloads_end_meter", new String[] { "username", "status" }, "downloads.end.username:" + username + ".success.meter"));
         assertTrue(testPatternAndTags("downloads_end_meter", new String[] { "username", "status" }, "downloads.end.username:" + username + ".failure.meter"));
      }
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_downloads_volume_rate()
   {
      assertTrue(testPatternAndTags("downloads_volume_rate", new String[] {}, "downloads.bytes"));
      for (String itemClass: ITEM_CLS)
      {
         assertTrue(testPatternAndTags("downloads_volume_rate", new String[] { "itemclass" }, "downloads." + itemClass + ".bytes"));
         for (String username: USER_NAMES)
         {
            assertTrue(testPatternAndTags("downloads_volume_rate", null, "downloads." + itemClass + ".username:" + username + ".bytes"));
         }
      }
      for (String username: USER_NAMES)
      {
         assertTrue(testPatternAndTags("downloads_volume_rate", new String[] { "username" }, "downloads.username:" + username + ".bytes"));
      }
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_eviction_timer()
   {
      assertTrue(testPatternAndTags("eviction_timer", null, "eviction.evictionName.DSname.soft.safe.timer"));

      assertTrue(testPatternAndTags("eviction_timer", new String[] { "evictionname", "issoft", "issafe" }, "eviction.evictionName.soft.safe.timer"));
      assertTrue(testPatternAndTags("eviction_timer", new String[] { "evictionname", "datastorename", "issafe" }, "eviction.evictionName.DSname.safe.timer"));
      assertTrue(testPatternAndTags("eviction_timer", new String[] { "evictionname", "datastorename", "issoft" }, "eviction.evictionName.DSname.soft.timer"));
      assertTrue(testPatternAndTags("eviction_timer", new String[] { "evictionname", "datastorename" }, "eviction.evictionName.DSname.timer"));
      assertTrue(testPatternAndTags("eviction_timer", new String[] { "evictionname", "issafe" }, "eviction.evictionName.safe.timer"));
      assertTrue(testPatternAndTags("eviction_timer", new String[] { "evictionname", "issoft" }, "eviction.evictionName.soft.timer"));
      assertTrue(testPatternAndTags("eviction_timer", new String[] { "evictionname" }, "eviction.evictionName.timer"));
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_async_get_rate()
   {
      assertTrue(testPatternAndTags("async_get_rate", null, "datastore.async.DSname.gets"));
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_async_restore_rate()
   {
      assertTrue(testPatternAndTags("async_restore_rate", null, "datastore.async.DSname.restores"));
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_async_cache_hits()
   {
      assertTrue(testPatternAndTags("async_cache_hits", null, "datastore.async.DSname.cache.hits"));
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_async_cache_size()
   {
      assertTrue(testPatternAndTags("async_cache_size", null, "datastore.async.DSname.cache"));
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_async_queue_size()
   {
      assertTrue(testPatternAndTags("async_queue_size", null, "datastore.async.DSname.queue"));
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_async_accepted_fetches()
   {
      assertTrue(testPatternAndTags("async_accepted_fetches", null, "datastore.async.DSname.fetches.accepted"));
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_async_refused_fetches()
   {
      assertTrue(testPatternAndTags("async_refused_fetches", null, "datastore.async.DSname.fetches.refused"));
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_cache()
   {
      for (String cachename: CACHE_NAMES)
      {
         assertTrue(testPatternAndTags("cache", null, "dhus.cache." + cachename));
      }
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_cache_gets()
   {
      for (String cachename: CACHE_NAMES)
      {
         assertTrue(testPatternAndTags("cache_gets", null, "dhus.cache." + cachename + ".gets"));
      }
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_cache_puts()
   {
      for (String cachename: CACHE_NAMES)
      {
         assertTrue(testPatternAndTags("cache_puts", null, "dhus.cache." + cachename + ".puts"));
      }
   }

   @Test(dependsOnMethods = { "testPatternsDoCompile" })
   public void test_product_repair()
   {
      for (String itemClass: ITEM_CLS)
      {
         assertTrue(testPatternAndTags("product_repair", null, "repair." + itemClass));
      }
   }

   /** Returns true if all the tests pass. If tags is null, uses the tags defined in the templateProvider. */
   private boolean testPatternAndTags(String name, String[] tags, String toTest)
   {
      Pattern pattern = compiledPattern.get(name);
      if (pattern == null)
      {
         LOGGER.error("Pattern {} does not exist");
         return false;
      }

      Boolean othersDontMatch = compiledPattern.entrySet().stream()
            .filter((entry) -> ! entry.getKey().equals(name))
            .<Boolean>reduce(
                  Boolean.TRUE,
                  (bool, entry) ->
                  {
                     boolean matches = entry.getValue().matcher(toTest).matches();
                     if (matches)
                     {
                        LOGGER.error("Pattern {} should not match input string '{}' matched by {}", entry.getKey(), toTest, name);
                     }
                     return bool && !matches;
                  },
                  (b1, b2) -> b1 && b2
            );
      if (!othersDontMatch)
      {
         return false;
      }

      Matcher matcher = pattern.matcher(toTest);
      if (!matcher.matches())
      {
         LOGGER.error("Pattern {} does not match input string '{}'", name, toTest);
         return false;
      }

      if (tags == null)
      {
         tags = templateProvider.getTemplates().get(name).tagKeys().toArray(new String[0]);
         if (tags == null || tags.length == 0)
         {
            return true;
         }
      }

      for (String tag: tags)
      {
         String tagMatched = matcher.group(tag);
         if (tagMatched == null || tagMatched.isEmpty())
         {
            LOGGER.error("Pattern {} does not have matching group {}", name, tag);
            return false;
         }
      }
      return true;
   }
}
