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

import com.sun.management.ThreadMXBean;

import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.spring.context.SecurityContextProvider;
import fr.gael.dhus.spring.security.CookieKey;
import fr.gael.dhus.spring.security.authentication.ProxyWebAuthenticationDetails;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.management.MBeanServer;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.event.CacheEventListenerAdapter;
import net.sf.ehcache.management.ManagementService;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.codec.Base64;

/**
 * This processing valve aims to manage the user processing time concerning the
 * requests matching the provided pattern.
 * Requests older than the configured period are forgotten.
 */
@SuppressWarnings("restriction")
public class ProcessingValve extends ValveBase
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final SecurityContextProvider SEC_CTX_PROVIDER =
         ApplicationContextProvider.getBean(SecurityContextProvider.class);

   public static final String DEFAULT_PATTERN = ".*(rows|\\$top)=0*[1-9]\\d{2,}.*";

   /*
    * Odata and solr except downloads
    *
    * "(.* /odata/v1/.*(?<!(\\$value)))|" + // includes odata $values only
    * "(.* /search\\?(q|rows|start|format|orderby).*)"; // or search query only
    *
    * ODataand solr requests with expected row>100
    * ".*(rows|\\$top)=0*[1-9]\\d{2,}.*"
    */
   /**
    * Filter pattern is passed as Tomcat parameter.
    * It allows to focus on a specific path: i.e "^.*(/odata/v1/).*$"(odata only)
    * or to exclude element : "^((?!/(home|new)/).)*$" : all but web pages...
    */
   private String pattern = DEFAULT_PATTERN;

   /**
    * Parameter to activates/deactivates this valve
    */
   private boolean enable = true;

   /**
    * User selection settings
    */
   public enum UserSelection
   {
      LOGIN,
      EMAIL,
      IP
   };

   /**
    * User selection: the selection of the user can be done by UserSelection
    * Regarding is the regulation could be done according the the user login only,
    * its IP or its e-mail.
    * This setting has been introduced to avoid multiple account abusive usage.
    */
   UserSelection[] userSelection =
   {
      UserSelection.LOGIN
   };

   /**
    * White list is a list of users authorized to have no quota.
    */
   private List<String> userWhiteList = Collections.emptyList();
   /**
    * Window size: The users processing authorization is done in the range of
    * a configurable window of time. This window is configurable via this
    * setting.
    * The window here is based on seconds.
    */

   private long timeWindow = 60 * 60; // Default 1 hour
   /**
    * The idle Time before reset is the time a user has no action before its
    * connection statistics are reseted.
    */

   private long idleTimeBeforeReset = 5 * 60; // default 5mn
   /**
    * Limit: Maximum time in seconds elapsed into the configured window. This
    * elapsed is configured by users (login/IP/e-mail wrt the user selection
    * settings)
    */

   private long maxElapsedTimePerUserPerWindow = 10 * 60; // default 10 minutes
   /**
    * Limit: Maximum number of requests within the elapsed window.
    * Use -1 for unlimited.
    */

   private long maxRequestNumberPerUserPerWindow = 60 * 60; // Default 1 per second.
   /**
    * Limit: maximum memory used within the time frame window fo each user.
    * Only when supported by the JVM (MXBean)
    */

   private long maxUsedMemoryPerUserPerWindow = 1024 * 1024;

   private static final String CACHE_MANAGER_NAME = "dhus_cache";
   private static final String CACHE_NAME = "user_requests";
   private static Cache cache;

   private static ThreadMXBean threadMxBean;

   static
   {
      // Active MBean interface for this CM
      try
      {
         MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
         ManagementService.registerMBeans(getCacheManager(), mBeanServer, true, true, true, true);
      }
      catch (Error e)
      {
         LOGGER.error("Cannot register MBean services.", e);
      }
   }

   private ThreadMXBean getThreadMxBean()
   {
      if (ProcessingValve.threadMxBean == null)
      {
         ThreadMXBean tmxBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
         if (!tmxBean.isCurrentThreadCpuTimeSupported())
         {
            throw new UnsupportedOperationException("Thread CPU monitoring not supported.");
         }
         if (tmxBean.isThreadCpuTimeSupported() && !tmxBean.isThreadCpuTimeEnabled())
         {
            tmxBean.setThreadCpuTimeEnabled(true);
         }
         if (tmxBean.isThreadAllocatedMemorySupported() && !tmxBean.isThreadAllocatedMemoryEnabled())
         {
            tmxBean.setThreadAllocatedMemoryEnabled(true);
         }

         ProcessingValve.threadMxBean = tmxBean;
      }
      return ProcessingValve.threadMxBean;
   }

   private static CacheManager getCacheManager()
   {
      return CacheManager.getCacheManager(CACHE_MANAGER_NAME);
   }

   private synchronized Cache getUserCache()
   {
      if (ProcessingValve.cache == null)
      {
         CacheManager cm = getCacheManager();
         ProcessingValve.cache = cm.getCache(CACHE_NAME);

         // Register listener dedicated to remove users windows caches
         // when users are evicted after a period of inactivity.
         ProcessingValve.cache.getCacheEventNotificationService().
               registerListener(new CacheEventListenerAdapter()
               {
                  @Override
                  public void notifyRemoveAll(Ehcache cache)
                  {
                     for (Object key: cache.getKeys())
                     {
                        notifyElementRemoved(cache, cache.get(key));
                     }
                  }

                  @Override
                  public void notifyElementRemoved(Ehcache cache, Element element)
                        throws CacheException
                  {
                     UserKey user = (UserKey) element.getObjectKey();
                     CacheManager cm = getCacheManager();
                     String user_cache_name = getCacheName(user);
                     if (cm.cacheExists(user_cache_name))
                     {
                        LOGGER.debug("Remove cache \"{}\"", user_cache_name);
                        cm.removeCache(user_cache_name);
                     }
                  }

                  @Override
                  public void notifyElementEvicted(Ehcache cache, Element element)
                  {
                     notifyElementRemoved(cache, element);
                  }
               });
      }
      return ProcessingValve.cache;
   }

   private String getCacheName(UserKey user)
   {
      return new StringBuilder().append("Window(").append(user.toString()).append(")").toString();
   }

   /**
    * Each user (@see {@link UserKey} for discrimination of users) has its own
    * rotating window to store their connections settings. The Window cache
    * is not directly referenced by connected users cache, but it is retrieved
    * thanks to it rule name (@see {@link #getCacheName(UserKey)}).
    * This method retrieves or create new window dedicated to the given user.
    *
    * @param user the user to retrieve window.
    * @return the window cache.
    */
   private synchronized Cache getWindowCache(UserKey user)
   {
      Cache window = null;
      String window_cache_name = getCacheName(user);
      if (!getCacheManager().cacheExists(window_cache_name))
      {
         CacheConfiguration cacheConfiguration = new CacheConfiguration();
         cacheConfiguration.setName(window_cache_name);
         cacheConfiguration.setMemoryStoreEvictionPolicy("FIFO");
         cacheConfiguration.setMaxEntriesLocalHeap(getMaxRequestNumberPerUserPerWindow() + 1);
         cacheConfiguration.setEternal(false);
         cacheConfiguration.setTimeToIdleSeconds((int) getTimeWindow());

         window = new Cache(cacheConfiguration);
         getCacheManager().addCache(window);

         // The cache on user is only used to manage users windows evictions.
         Element user_req_elem = new Element(user, null);
         user_req_elem.setTimeToIdle((int) getIdleTimeBeforeReset());
         user_req_elem.setTimeToLive((int) getTimeWindow());
         getUserCache().put(user_req_elem);
      }
      else
      {
         window = getCacheManager().getCache(window_cache_name);
      }
      return window;
   }

   public synchronized void resetAllCache()
   {
      if (ProcessingValve.cache == null)
      {
         return;
      }
      LOGGER.debug("Reseting all caches!");

      for (Object key: ProcessingValve.cache.getKeys())
      {
         String window_cache_name = getCacheName((UserKey) key);
         if (getCacheManager().cacheExists(window_cache_name))
         {
            getCacheManager().removeCache(window_cache_name);
         }
         LOGGER.debug("-> Removed {}", window_cache_name);
      }
      ProcessingValve.cache.removeAll();
   }

   @Override
   public void invoke(Request request, Response response) throws IOException, ServletException
   {
      ProcessingInformation pi = this.createProcessing(request, response);
      // Case of Valve disabled.
      if (!isEnable()
            || getUserWhiteList().contains(pi.getUsername())
            || ((getPattern() != null) && (pi.getRequest() != null) && !pi.getRequest().matches(getPattern())))
      {
         getNext().invoke(request, response);
         return;
      }

      try
      {
         UserKey user_key = new UserKey(pi, getUserSelection());

         Cache window = getWindowCache(user_key);
         // Check if authorized: throw ProcessingQuotaException otherwise.
         checkAccess(user_key, window);

         // Run the request
         getNext().invoke(request, response);

         updateProcessingMetrics(pi);
         // Processing done: update the user window.
         Element request_element = new Element(pi.getDateNs(), pi);

         // Time expected into the cache should be at least the window time
         // No matter about the accesses.
         request_element.setTimeToLive((int) 0); // Infinite
         request_element.setTimeToIdle((int) getTimeWindow()); // Window time

         try
         {
            window.put(request_element);
         }
         catch (Exception e)
         {
            // Do nothing case of reset happen during the request invoke.
         }
      }
      catch (ProcessingQuotaException e)
      {
         // quota exceeded, set user error message and error flag
         response.setError();
         response.sendError(429, e.getMessage());
         getNext().invoke(request, response);
      }
      catch (IOException | ServletException e)
      {
         LOGGER.debug("Exception while checking processing quota.", e);
         throw e;
      }
   }

   /**
    * Logs information into temporary cache. According to the Valve
    * configuration, log will also display into the logger.
    *
    * @param request  the input user request to log.
    * @param response the response to the user to be incremented.
    *                 return the log entry.
    * @throws IOException
    * @throws ServletException
    */
   private ProcessingInformation createProcessing(Request request, Response response)
         throws IOException, ServletException
   {
      String request_string = null;
      if (request.getQueryString() != null)
      {
         request_string = request.getRequestURL().append('?').append(request.getQueryString()).toString();
      }
      else
      {
         request_string = request.getRequestURL().toString();
      }

      ProcessingInformation pi = new ProcessingInformation(request_string);

      // Retrieve cookie to obtains existing context if any.
      Cookie integrityCookie = CookieKey.getIntegrityCookie(request.getCookies());

      SecurityContext ctx = null;
      if (integrityCookie != null)
      {
         String integrity = integrityCookie.getValue();
         if (integrity != null && !integrity.isEmpty())
         {
            ctx = SEC_CTX_PROVIDER.getSecurityContext(integrity);
         }
      }
      if ((ctx != null) && (ctx.getAuthentication() != null))
      {
         pi.setUsername(ctx.getAuthentication().getName());
      }
      else
      {
         String[] basicAuth = extractAndDecodeHeader(request.getHeader("Authorization"));
         if (basicAuth != null)
         {
            pi.setUsername(basicAuth[0]);
         }
      }
      pi.setRemoteAddress(ProxyWebAuthenticationDetails.getRemoteIp(request));
      pi.setRemoteHost(ProxyWebAuthenticationDetails.getRemoteHost(request));
      return pi;
   }

   private void updateProcessingMetrics(ProcessingInformation pi)
   {
      StringBuilder sb = new StringBuilder();
      sb.append(Thread.currentThread().getName()).
            append("[").append(Thread.currentThread().getId()).append("]:").
            append(pi.getUsername() == null ? "internal" : pi.getUsername()).
            append(":");

      if (getThreadMxBean().isCurrentThreadCpuTimeSupported())
      {
         long cpu_time = getThreadMxBean().getCurrentThreadCpuTime();
         pi.setCpuTimeNs(cpu_time);

         sb.append("cpu_time=").append(formatInterval(cpu_time)).append(",");

         long user_time = getThreadMxBean().getCurrentThreadUserTime();
         pi.setUserTimeNs(user_time);
         sb.append("user_time=").append(formatInterval(user_time)).append(",");
      }

      if (getThreadMxBean().isThreadAllocatedMemoryEnabled())
      {
         long tid = Thread.currentThread().getId();
         long mem = getThreadMxBean().getThreadAllocatedBytes(tid);
         pi.setMemoryUsage(mem);
         sb.append("memory=").append(formatSize(mem));
      }
      LOGGER.debug(sb.toString());
   }

   Long sToNs(Long s)
   {
      return s * 1000000000;
   }

   double sToMn(Long s)
   {
      return s.doubleValue() / 60.0;
   }

   double nsToMn(double d)
   {
      return (d / 1000000000.0) / 60.0;
   }

   String formatMn(double mn)
   {
      return String.format("%.2f", mn);
   }

   private String formatInterval(final long ns)
   {
      long l = ns / 1000000;
      final long hr = TimeUnit.MILLISECONDS.toHours(l);
      final long min = TimeUnit.MILLISECONDS.toMinutes(l - TimeUnit.HOURS.toMillis(hr));
      final long sec = TimeUnit.MILLISECONDS.toSeconds(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
      final long ms = TimeUnit.MILLISECONDS.toMillis(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
      return String.format("%02d:%02d:%02d.%03d", hr, min, sec, ms);
   }

   public static String formatSize(long size)
   {
      if (size <= 0)
      {
         return "0";
      }
      final String[] units = new String[]
      {
         "B", "kB", "MB", "GB", "TB"
      };
      int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
      return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
   }

   protected void checkAccess(UserKey user, Cache window) throws ProcessingQuotaException
   {
      ProcessingInformation pi = user.getPi();
      if (isWhiteListed(pi) || isInternal(pi))
      {
         return;
      }

      SummaryStatistics cpu_time_stats = new SummaryStatistics();
      SummaryStatistics user_time_stats = new SummaryStatistics();
      SummaryStatistics memory_stats = new SummaryStatistics();
      long now = System.nanoTime();

      for (Object o: window.getKeysWithExpiryCheck())
      {
         Long date_ns = (Long) o;
         if ((now - date_ns) > sToNs(getTimeWindow()))
         {
            // Cache returns element out of the expected time window
            // (delta is {} ns)." (now-date_ns)
            // This can happen when the cache settings are modified
            // on the fly by JMX.
            continue;
         }
         ProcessingInformation proc = (ProcessingInformation) window.get(o).getObjectValue();

         if (proc.getCpuTimeNs() != null)
         {
            cpu_time_stats.addValue(proc.getCpuTimeNs().doubleValue());
         }
         if (proc.getUserTimeNs() != null)
         {
            user_time_stats.addValue(proc.getUserTimeNs().doubleValue());
         }
         if (proc.getMemoryUsage() != null)
         {
            memory_stats.addValue(proc.getMemoryUsage().doubleValue());
         }
      }
      String username = pi.getUsername();
      // Checks the system CPU time used in the time frame
      if (checkParameter(getMaxElapsedTimePerUserPerWindow())
            && cpu_time_stats.getSum() > sToNs(getMaxElapsedTimePerUserPerWindow()))
      {
         StringBuilder sb = new StringBuilder();
         sb.append("CPU usage quota exceeded (").
               append(formatMn(sToMn(getMaxElapsedTimePerUserPerWindow()))).
               append("mn per period of ").
               append(formatMn(sToMn(getTimeWindow()))).
               append("mn) - please wait and retry.");

         LOGGER.warn("[{}]  CPU usage quota exceeded: {} (max={})",
               username, formatInterval((long) cpu_time_stats.getSum()),
               formatInterval((long) getMaxElapsedTimePerUserPerWindow() * 1000000000));

         throw new ProcessingQuotaException(sb.toString());
      }

      // Checks the user CPU time used in the time frame
      if (checkParameter(getMaxElapsedTimePerUserPerWindow())
            && user_time_stats.getSum() >= sToNs(getMaxElapsedTimePerUserPerWindow()))
      {
         StringBuilder sb = new StringBuilder();
         sb.append("User CPU usage quota exceeded (").
               append(formatMn(sToMn(getMaxElapsedTimePerUserPerWindow()))).
               append("mn per period of ").
               append(formatMn(sToMn(getTimeWindow()))).
               append("mn) - please wait and retry.");

         LOGGER.warn("[{}] User CPU usage quota exceeded: {} (max={})",
               username, formatInterval((long) user_time_stats.getSum()),
               formatInterval((long) getMaxElapsedTimePerUserPerWindow() * 1000000000));

         throw new ProcessingQuotaException(sb.toString());
      }
      // Checks the total memory used in the time frame
      if (checkParameter(getMaxUsedMemoryPerUserPerWindow())
            && memory_stats.getSum() >= getMaxUsedMemoryPerUserPerWindow())
      {
         StringBuilder sb = new StringBuilder();
         sb.append("Memory quota exceeded (").
               append(formatSize(getMaxUsedMemoryPerUserPerWindow())).
               append(" used in a period of ").
               append(formatMn(sToMn(getTimeWindow()))).
               append("mn) - please wait and retry.");

         LOGGER.warn("[{}] Memory quota exceeded: {} (max={})",
               username, formatSize((long) memory_stats.getSum()),
               formatSize((long) getMaxUsedMemoryPerUserPerWindow()));

         throw new ProcessingQuotaException(sb.toString());
      }
      // Checks the number of request in the time frame
      if (checkParameter(getMaxRequestNumberPerUserPerWindow())
            && user_time_stats.getN() >= getMaxRequestNumberPerUserPerWindow())
      {
         StringBuilder sb = new StringBuilder();
         sb.append("Maximum number of request exceeded (").
               append(getMaxRequestNumberPerUserPerWindow()).
               append("max calls in a period of ").
               append(formatMn(sToMn(getTimeWindow()))).
               append("mn) - please wait and retry.");

         LOGGER.warn("[{}] Maximum number of request exceeded: {} (max={})",
               username, user_time_stats.getN(),
               getMaxRequestNumberPerUserPerWindow());

         throw new ProcessingQuotaException(sb.toString());
      }

      LOGGER.info("Time Window cumuls for user {}:{} cpu_time={}mn,user_time={}mn,memory={}",
            username, user_time_stats.getN(),
            formatMn(nsToMn(cpu_time_stats.getSum())),
            formatMn(nsToMn(user_time_stats.getSum())),
            formatSize((long) memory_stats.getSum()));
   }

   private boolean isInternal(ProcessingInformation pi)
   {
      return pi.getUsername() == null;
   }

   boolean checkParameter(Number param)
   {
      return !((param == null) || param.doubleValue() < 0);
   }

   private boolean isWhiteListed(ProcessingInformation pi)
   {
      return userWhiteList.contains(pi.getUsername());
   }

   private String[] extractAndDecodeHeader(String header) throws IOException
   {
      if (header == null || header.isEmpty())
      {
         return null;
      }
      byte[] base64Token = header.substring(6).getBytes("UTF-8");
      byte[] decoded;
      try
      {
         decoded = Base64.decode(base64Token);
      }
      catch (IllegalArgumentException e)
      {
         throw new BadCredentialsException("Failed to decode basic authentication token.");
      }

      String token = new String(decoded, "UTF-8");

      int delim = token.indexOf(":");

      if (delim == -1)
      {
         throw new BadCredentialsException("Invalid basic authentication token.");
      }
      return new String[]
      {
         token.substring(0, delim), token.substring(delim + 1)
      };
   }

   /**
    * Tomcat offers mechanism that automatically instantiate Valves that implements such setters.
    * This setter is used to set the pattern of request URL that will be logged.
    *
    * @param pattern the pattern to be applied to requests
    */
   public void setPattern(String pattern)
   {
      this.pattern = pattern;
   }

   /**
    * Retrieves pattern.
    *
    * @return the pattern
    */
   public String getPattern()
   {
      return pattern;
   }

   public void setEnable(boolean enable)
   {
      this.enable = enable;
   }

   public boolean isEnable()
   {
      return this.enable;
   }

   public UserSelection[] getUserSelection()
   {
      return userSelection;
   }

   public void setUserSelection(UserSelection[] userSelections)
   {
      this.userSelection = userSelections;
      resetAllCache();
   }

   public void setUserSelection(String userSelections)
   {
      setUserSelection(commaDelimitedListToUserSelectionArray(userSelections));
   }

   public long getTimeWindow()
   {
      return timeWindow;
   }

   /**
    * Sets the timeWindow setting and also update de cache configuration.
    *
    * @param timeWindow
    */
   public void setTimeWindow(long timeWindow)
   {
      this.timeWindow = timeWindow;
      resetAllCache();
   }

   public long getMaxElapsedTimePerUserPerWindow()
   {
      return maxElapsedTimePerUserPerWindow;
   }

   public void setMaxElapsedTimePerUserPerWindow(long max)
   {
      this.maxElapsedTimePerUserPerWindow = max;
      resetAllCache();
   }

   public long getMaxRequestNumberPerUserPerWindow()
   {
      return maxRequestNumberPerUserPerWindow;
   }

   public void setMaxRequestNumberPerUserPerWindow(long max)
   {
      this.maxRequestNumberPerUserPerWindow = max;
      resetAllCache();
   }

   public long getMaxUsedMemoryPerUserPerWindow()
   {
      return maxUsedMemoryPerUserPerWindow;
   }

   public void setMaxUsedMemoryPerUserPerWindow(long max)
   {
      this.maxUsedMemoryPerUserPerWindow = max;
      resetAllCache();
   }

   public long getIdleTimeBeforeReset()
   {
      return idleTimeBeforeReset;
   }

   public void setIdleTimeBeforeReset(long idleTimeBeforeReset)
   {
      this.idleTimeBeforeReset = idleTimeBeforeReset;
      resetAllCache();
   }

   public List<String> getUserWhiteList()
   {
      return userWhiteList;
   }

   public void setUserWhiteList(List<String> userWhiteList)
   {
      this.userWhiteList = userWhiteList;
   }

   public void setUserWhiteList(String userWhiteList)
   {
      setUserWhiteList(Arrays.asList(commaDelimitedListToStringArray(userWhiteList)));
   }

   @Override
   public String toString()
   {
      StringBuilder sb = new StringBuilder();
      sb.append("ProcessingValve@").append(this.hashCode()).
            append("[enable=\"").append(this.enable).append("\",").
            append(" pattern=\"").append(this.pattern).append("\",").
            append(" userSelection=\"[");

      StringBuilder us_sb = new StringBuilder();
      for (UserSelection us: this.userSelection)
      {
         us_sb.append(us.name()).append(",");
      }
      // remove the last trailing ","
      us_sb.setLength(Math.max(us_sb.length() - 1, 0));

      sb.append(us_sb).append("]\",");
      sb.append(" timeWindow=\"").
            append(formatInterval(getTimeWindow() * 1000000000)).
            append("\",").
            append(" idleTimeBeforeReset=\"").
            append(formatInterval(getIdleTimeBeforeReset() * 1000000000)).
            append("\",").
            append(" maxElapsedTimePerUserPerWindow=\"").
            append(formatInterval(getMaxElapsedTimePerUserPerWindow() * 1000000000)).
            append("\",").
            append(" maxRequestNumberPerUserPerWindow=\"").
            append(getMaxRequestNumberPerUserPerWindow()).
            append("\",").
            append(" maxUsedMemoryPerUserPerWindow=\"").
            append(formatSize(getMaxUsedMemoryPerUserPerWindow())).append("\"]");
      return sb.toString();
   }

   /**
    * {@link Pattern} for a comma delimited string that
    * support whitespace characters.
    */
   private static final Pattern CSV_PATTERN = Pattern.compile("\\s*,\\s*");

   protected static UserSelection[] commaDelimitedListToUserSelectionArray(String commaDelimitedUserSelections)
   {
      String[] selections = commaDelimitedListToStringArray(commaDelimitedUserSelections);

      List<UserSelection> usList = new ArrayList<>();
      for (String selection: selections)
      {
         try
         {
            usList.add(UserSelection.valueOf(selection));
         }
         catch (Exception e)
         {
            LOGGER.error("Wrong user selection {}, ignored.", selection);
         }
      }
      return usList.toArray(new UserSelection[0]);
   }

   /**
    * Convert a given comma delimited list of regular expressions
    * into an array of String
    *
    * @param commaDelimitedStrings
    * @return array of patterns (non <code>null</code>)
    */
   protected static String[] commaDelimitedListToStringArray(String commaDelimitedStrings)
   {
      return (commaDelimitedStrings == null || commaDelimitedStrings.length() == 0) ?
            new String[0] :
            CSV_PATTERN.split(commaDelimitedStrings);
   }
}
