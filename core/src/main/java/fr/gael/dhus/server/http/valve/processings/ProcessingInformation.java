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

/**
 * Gathers the informations regarding one request processing.
 */
public class ProcessingInformation
{
   private final Long dateNs;
   private String request;
   /**
    * CPU Time is the time spent by this thread:
    * cpu_time = user_time + kernel_time;
    * nanoseconds units.
    */
   private Long cpuTimeNs;
   /**
    * User time is the time spend by the CPU running user program.
    * nanoseconds units.
    */
   private Long userTimeNs;
   private Long memoryUsage;
   private String username;
   private String remoteAddress;
   private String remoteHost;

   private static synchronized long now()
   {
      return System.nanoTime();
   }

   public ProcessingInformation(String request)
   {
      this(request, now());
   }

   /**
    * Construct this class.
    *
    * @param request the request denoting this processing
    * @param date_ns in nanoseconds date of the request
    */
   public ProcessingInformation(String request, long date_ns)
   {
      this.request = request;
      this.dateNs = date_ns;
   }

   /**
    * Retrieve the elapsed time within this request.
    *
    * @return the time spent in this request in nanoseconds
    * @see #cpuTimeNs
    */
   public Long getCpuTimeNs()
   {
      return cpuTimeNs;
   }

   /**
    * Set the time elapsed into during this processing.
    *
    * @param time_ns the elapsed time in nanoseconds
    * @see #cpuTimeNs
    */
   public void setCpuTimeNs(Long time_ns)
   {
      this.cpuTimeNs = time_ns;
   }

   /**
    * Set the time elapsed into during this processing.
    *
    * @return the elapsed time in nanoseconds
    * @see #userTimeNs
    */
   public Long getUserTimeNs()
   {
      return userTimeNs;
   }

   /**
    * Set the time elapsed into during this processing.
    *
    * @param userTimeNs the elapsed time in nanoseconds
    * @see #userTimeNs
    */
   public void setUserTimeNs(Long userTimeNs)
   {
      this.userTimeNs = userTimeNs;
   }

   /**
    * Memory used by this thread execution.
    *
    * @return the used memory in byte units
    */
   public Long getMemoryUsage()
   {
      return memoryUsage;
   }

   /**
    * Memory used by this thread execution.
    *
    * @param memoryUsage the used memory in byte units
    */
   public void setMemoryUsage(Long memoryUsage)
   {
      this.memoryUsage = memoryUsage;
   }

   /**
    * The unique date in nanosecond when the processing stared.
    * This date is considered unique within this process. Its generation is thread safe.
    *
    * @return the starting date of the processing in nanoseconds
    */
   public long getDateNs()
   {
      return dateNs;
   }

   /**
    * Retrieve the registered request for this processing.
    *
    * @return the request
    */
   public String getRequest()
   {
      return request;
   }

   public String getUsername()
   {
      return username;
   }

   public void setUsername(String username)
   {
      this.username = username;
   }

   public String getRemoteAddress()
   {
      return remoteAddress;
   }

   public void setRemoteAddress(String remoteAddress)
   {
      this.remoteAddress = remoteAddress;
   }

   public String getRemoteHost()
   {
      return remoteHost;
   }

   public void setRemoteHost(String remoteHost)
   {
      this.remoteHost = remoteHost;
   }
}
