/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2018 GAEL Systems
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
package fr.gael.dhus.database.object.config.eviction;

import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.olingo.v1.ODataExpressionParser;
import fr.gael.dhus.olingo.v1.visitor.ExecutableExpressionTree;
import fr.gael.dhus.olingo.v1.visitor.ProductFunctionalVisitor;

import java.util.concurrent.TimeUnit;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;

/**
 *  Manages the eviction defined in configuration file (dhus.xml).
 */
public class Eviction extends EvictionConfiguration
{
   /** Log. */
   private static final Logger LOGGER = LogManager.getLogger();

   /** Used by {@link #filter(Product)}. */
   @XmlTransient
   private ExecutableExpressionTree<Product> executableFilter = null;

   /**
    * Returns the time to live of a product in milliseconds built from the KeepPeriod and
    * KeepPeriodUnit entities.
    *
    * @return keep period in milliseconds
    */
   public long computeKeepPeriod()
   {
      TimeUnit tu = TimeUnit.valueOf(getKeepPeriodUnit());
      tu = (tu != null ? tu : TimeUnit.DAYS);
      return tu.toMillis(getKeepPeriod());
   }

   /**
    * Set the filter and tries to create an executable filter expression tree from it.
    * Called by {@link #setFilter(String)} and the data handler of the Eviction entity type.
    *
    * @param value filter
    * @throws ODataException could not generate executable filter
    */
   public void checkAndSetFilter(String value) throws ODataException
   {
      if (value == null)
      {
         executableFilter = null;
         super.setFilter(null);
      }
      else
      {
         FilterExpression filterexp = ODataExpressionParser.getProductExpressionParser().parseFilterString(value);
         ProductFunctionalVisitor validator = new ProductFunctionalVisitor();
         executableFilter = ExecutableExpressionTree.class.cast(filterexp.accept(validator));
         super.setFilter(value);
      }
   }

   /**
    * Hey, you should probably use {@link #checkAndSetFilter(String)} instead !
    * This method calls {@link #checkAndSetFilter(String)} but suppresses the exception thrown if
    * the filter cannot be parsed/transformed.
    *
    * @see EvictionConfiguration#setFilter(String)
    */
   @Override
   public void setFilter(String value)
   {
      try
      {
         checkAndSetFilter(value);
      }
      catch (ODataException ex)
      {
         // Dummy executable filter tree that always return false (because filter is invalid)
         this.executableFilter = new ExecutableExpressionTree<>(ExecutableExpressionTree.Node.<Product, Boolean>createLeave(() -> Boolean.FALSE));
         LOGGER.warn("Could not make an executable filter for eviction '{}' from expression '{}'", name, value, ex);
      }
   }

   /**
    * Validates the given product against the filter defined in the filter entity.
    *
    * @param toFilter product to filter
    * @return true if given product is to be evicted by this eviction
    */
   public boolean filter(Product toFilter)
   {
      if (this.executableFilter == null)
      {
         if (this.filter == null || this.filter.isEmpty())
         {
            return true;
         }
         // generates this.executableFilter
         setFilter(this.filter);
      }
      return (boolean)this.executableFilter.exec(toFilter);
   }

   /**
    * Sets the OrderBy and tries to parse it.
    *
    * @param value OrderBy OData formatted {@code $orderby} expression
    * @throws ODataException could not generate executable OrderBy
    */
   public void checkAndSetOrderBy(String value) throws ODataException
   {
      if (value == null)
      {
         super.setOrderBy(null);
      }
      else
      {
         ODataExpressionParser.getProductExpressionParser().parseOrderByString(value);
         super.setOrderBy(value);
      }
   }

   /**
    * This method calls {@link #checkAndSetOrderBy(String)} but suppresses the exception thrown if
    * the OrderBy cannot be parsed/transformed, a warning is printed in the logs.
    *
    * @deprecated use {@link #checkAndSetOrderBy(String)} instead.
    * @see EvictionConfiguration#setOrderBy(String)
    */
   @Override
   @Deprecated
   public void setOrderBy(String value)
   {
      try
      {
         checkAndSetOrderBy(value);
      }
      catch (ODataException ex)
      {
         LOGGER.warn("Could not parse orderby parameter for eviction '{}' from expression '{}'", name, value, ex);
      }
   }

}
