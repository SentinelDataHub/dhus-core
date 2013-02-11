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
package fr.gael.dhus.util;

import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * instantiates the DataTypeFactory and provides helper methods.
 */
public final class XmlProvider
{
   private static final DatatypeFactory DATA_TYPE_FACTORY;

   // Static constructor to handle a non runtime exception
   static
   {
      try
      {
         DATA_TYPE_FACTORY = DatatypeFactory.newInstance();
      }
      catch (DatatypeConfigurationException ex)
      {
         // No XML implementation in classpath (unlikely)
         throw new RuntimeException(ex);
      }
   }

   /**
    * Returns the single DataTypeFactory instance.
    *
    * @return not null instance of DataTypeFactory
    */
   public static DatatypeFactory getDataTypeFactory()
   {
      return DATA_TYPE_FACTORY;
   }

   /**
    * Returns an XMLGregorianCalendar set to the given calendar's time.
    *
    * @param cal time to set
    * @return new instance
    * @throws NullPointerException if `cal` is null
    */
   public static XMLGregorianCalendar getCalendar(GregorianCalendar cal)
   {
      return DATA_TYPE_FACTORY.newXMLGregorianCalendar(cal);
   }

   /**
    * Returns an XMLGregorianCalendar set to the given time.
    *
    * @param timeMilliSeconds time to set (in milliseconds from the posix epoch)
    * @return new instance
    */
   public static XMLGregorianCalendar getCalendar(long timeMilliSeconds)
   {
      GregorianCalendar greg = new GregorianCalendar();
      greg.setTimeInMillis(timeMilliSeconds);
      return DATA_TYPE_FACTORY.newXMLGregorianCalendar(greg);
   }

   /**
    * Returns an XMLGregorianCalendar set to NOW().
    *
    * @return new instance
    */
   public static XMLGregorianCalendar getCalendarNow()
   {
      return getCalendar(System.currentTimeMillis());
   }
}
