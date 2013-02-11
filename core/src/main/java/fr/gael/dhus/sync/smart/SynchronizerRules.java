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
package fr.gael.dhus.sync.smart;

import fr.gael.dhus.database.object.config.synchronizer.SmartProductSynchronizer;

public class SynchronizerRules
{
   private final Integer attempts;
   private final int threshold;
   private final Long timeout;

   public SynchronizerRules(SmartProductSynchronizer synchronizer)
   {
      this.threshold = synchronizer.getThreshold();
      this.attempts = synchronizer.getAttempts();
      this.timeout = synchronizer.getTimeout();
   }

   public Integer getAttempts()
   {
      return attempts;
   }

   public int getThreshold()
   {
      return threshold;
   }

   public Long getTimeout()
   {
      return timeout;
   }
}
