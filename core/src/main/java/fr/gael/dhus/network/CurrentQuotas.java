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
package fr.gael.dhus.network;

import java.io.Serializable;

/**
 * Holds the number of downloads happening in parallel.
 */
public class CurrentQuotas implements Serializable, Cloneable
{
   private static final long serialVersionUID = 1L;

   /** Downloads in parallel. */
   private int currentDownloads;

   /**
    * Creates a new CurrentQuotas with currentDownloads set to 0.
    */
   public CurrentQuotas() {}

   /**
    * Creates a new CurrentQuotas with given currentDownloads.
    *
    * @param currentDownloads downloads happening in parallel
    */
   public CurrentQuotas(int currentDownloads)
   {
      this.currentDownloads = currentDownloads;
   }

   /**
    * @return the number of downloads happening in parallel
    */
   public int getCurrentDownloads()
   {
      return currentDownloads;
   }

   /**
    * @param currentDownloads downloads happening in parallel
    */
   public void setCurrentDownloads(int currentDownloads)
   {
      this.currentDownloads = currentDownloads;
   }

}
