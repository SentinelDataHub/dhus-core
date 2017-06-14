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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.HibernateException;
import org.hibernate.ScrollableResults;
import org.hibernate.type.Type;

/**
 * An empty ScrollIterator used when result is null.
 */
public class EmptyScrollableResult implements ScrollableResults
{
   @Override
   public boolean next() throws HibernateException
   {
      return false;
   }

   @Override
   public boolean previous() throws HibernateException
   {
      return false;
   }

   @Override
   public boolean scroll(int i) throws HibernateException
   {
      return false;
   }

   @Override
   public boolean last() throws HibernateException
   {
      return false;
   }

   @Override
   public boolean first() throws HibernateException
   {
      return false;
   }

   @Override
   public void beforeFirst() throws HibernateException
   {
   }

   @Override
   public void afterLast() throws HibernateException
   {
   }

   @Override
   public boolean isFirst() throws HibernateException
   {
      return false;
   }

   @Override
   public boolean isLast() throws HibernateException
   {
      return false;
   }

   @Override
   public void close() throws HibernateException
   {

   }

   @Override
   public Object[] get() throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Object get(int i) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Type getType(int i)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Integer getInteger(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Long getLong(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Float getFloat(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Boolean getBoolean(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Double getDouble(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Short getShort(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Byte getByte(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Character getCharacter(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public byte[] getBinary(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public String getText(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Blob getBlob(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Clob getClob(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public String getString(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public BigDecimal getBigDecimal(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public BigInteger getBigInteger(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Date getDate(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Locale getLocale(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Calendar getCalendar(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public TimeZone getTimeZone(int col) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public int getRowNumber() throws HibernateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean setRowNumber(int rowNumber) throws HibernateException
   {
      throw new UnsupportedOperationException();
   }
}
