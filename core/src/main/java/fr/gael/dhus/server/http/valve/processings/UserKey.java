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

import java.io.Serializable;

import fr.gael.dhus.server.http.valve.processings.ProcessingValve.UserSelection;

public class UserKey implements Serializable
{
   private static final long serialVersionUID = 1L;
   ProcessingInformation pi;
   UserSelection[] selection;

   public ProcessingInformation getPi()
   {
      return this.pi;
   }

   public UserKey(ProcessingInformation pi, UserSelection[] selection)
   {
      this.pi = pi;
      this.selection = selection;
   }

   /**
    * Redefine the equals operator to manage equality info regarding the
    * selection. Thanks to this method, UserKey can be equals if
    *    selection={} = always equals without necessary comparison,
    *    selection={LOGIN} = on username comparison,
    *    selection={EMAIL} = on email comparison*,
    *    selection={IP} = on ip address comparison*,
    * all the combinaisons of selection are possible:
    *    selection={LOGIN,IP} on username AND ip address comparison.
    * Warning: EMAIL not currently implemented (USERNAME used instead).
    *
    * @param o
    * @return
    */
   @Override
   public boolean equals(Object o)
   {
      if (this == o)
      {
         return true;
      }
      if (o instanceof UserKey)
      {
         UserKey key = (UserKey) o;
         boolean result = true;
         for (UserSelection sel: this.selection)
         {
            switch (sel)
            {
               case LOGIN:
               case EMAIL:
                  result &= key.pi.getUsername() == null ? false :
                        key.pi.getUsername().equals(pi.getUsername());
                  break;
               case IP:
                  result &= key.pi.getRemoteAddress() == null ? false :
                        key.pi.getRemoteAddress().equals(pi.getRemoteAddress());
                  break;
               default:
                  result = false;
                  break;
            }
         }
         return result;
      }
      return false;
   }

   @Override
   public String toString()
   {
      StringBuilder sb = new StringBuilder();
      for (UserSelection selection: this.selection)
      {
         sb.append(selection.name()).append(":");
         switch (selection)
         {
            case LOGIN:
            case EMAIL:
               sb.append(pi.getUsername()); // EMAIL must be extracted from db
               break;
            case IP:
               sb.append(pi.getRemoteAddress());
               break;
            default:
               break;
         }
         sb.append(";");
      }
      return sb.toString();
   }
}
