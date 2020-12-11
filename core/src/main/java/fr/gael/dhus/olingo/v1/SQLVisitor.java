/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2014-2019 GAEL Systems
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
package fr.gael.dhus.olingo.v1;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.apache.olingo.odata2.api.edm.EdmLiteral;
import org.apache.olingo.odata2.api.edm.EdmTyped;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.uri.expression.BinaryExpression;
import org.apache.olingo.odata2.api.uri.expression.BinaryOperator;
import org.apache.olingo.odata2.api.uri.expression.ExceptionVisitExpression;
import org.apache.olingo.odata2.api.uri.expression.ExpressionVisitor;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.LiteralExpression;
import org.apache.olingo.odata2.api.uri.expression.MemberExpression;
import org.apache.olingo.odata2.api.uri.expression.MethodExpression;
import org.apache.olingo.odata2.api.uri.expression.MethodOperator;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderExpression;
import org.apache.olingo.odata2.api.uri.expression.PropertyExpression;
import org.apache.olingo.odata2.api.uri.expression.SortOrder;
import org.apache.olingo.odata2.api.uri.expression.UnaryExpression;
import org.apache.olingo.odata2.api.uri.expression.UnaryOperator;
import org.apache.olingo.odata2.api.uri.expression.Visitable;
import org.dhus.olingo.v2.visitor.SQLVisitorParameter;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Implements the ExpressionVisitor interface to build HQL expressions from Olingo expression trees.
 * visitFilterExpression builds the WHERE clause (not prefixed with the WHERE statement).
 * visitOrderByExpression builds the ORDER BY clause (not prefixed with the ORDER BY statement).
 * You must implement the <code>visitProperty</code> method which highly depends on the EDM.
 *
 * @see http://olingo.apache.org/doc/odata2/tutorials/Olingo_Tutorial_AdvancedRead_FilterVisitor.html
 */
public abstract class SQLVisitor implements ExpressionVisitor, Serializable
{
   private static final long serialVersionUID = 1L;
   private static final String HQL_PREFIX = "FROM ";

   private final String hqlPrefix;
   private transient final FilterExpression filterExpression;
   private final OrderByExpression orderExpression;
   private final List<SQLVisitorParameter> hqlParameters;

   private String hqlFilter;
   private String hqlOrder;

   private int positionToUse = 1;

   protected SQLVisitor(Class<?> entity, FilterExpression filter, OrderByExpression order)
         throws ExceptionVisitExpression, ODataApplicationException
   {
      this.hqlPrefix = new StringBuilder(HQL_PREFIX).append(entity.getName()).append(" ").toString();
      this.filterExpression = filter;
      this.orderExpression = order;
      this.hqlParameters = new LinkedList<>();
      compute();
   }

   /**
    * Calls accept method on the visitable expressions `filter` and `order by`.
    *
    * @throws ExceptionVisitExpression Exception occurred the OData library while traversing the tree
    * @throws ODataApplicationException Exception thrown by the application who implemented the visitor
    * @see Visitable#accept(ExpressionVisitor)
    */
   private void compute() throws ExceptionVisitExpression, ODataApplicationException
   {
      if (filterExpression != null)
      {
         filterExpression.accept(this);
      }
      if (orderExpression != null)
      {
         orderExpression.accept(this);
      }
   }

   /* Builds the WHERE clause (not prefixed with the WHERE statement). */
   @Override
   public Object visitFilterExpression(FilterExpression filter_expression,
         String expression_string, Object expression)
   {
      if (hqlFilter == null)
      {
         this.hqlFilter = (String) expression;
      }
      return hqlFilter;
   }

   /* Builds the ORDER BY clause (not prefixed with the ORDER BY statement). */
   @Override
   public Object visitOrderByExpression(OrderByExpression order_expression,
         String expression_string, List<Object> orders)
   {
      if (hqlOrder == null)
      {
         StringBuilder sb = new StringBuilder();
         Iterator<Object> it = orders.iterator();
         while (it.hasNext())
         {
            String order = (String) it.next();
            sb.append(order);
            if (it.hasNext())
            {
               sb.append(", ");
            }
         }
         hqlOrder = sb.toString();
      }
      return hqlOrder;
   }

   /* Called for each fields in the $orderby param. */
   @Override
   public Object visitOrder(OrderExpression order_expression, Object filter_result, SortOrder sort_order)
   {
      String property = filter_result.toString();
      switch (sort_order)
      {
         case asc:
         {
            return new StringBuilder(property).toString();
         }
         case desc:
         {
            return new StringBuilder(property).append(" ").append("desc").toString();
         }
         default:
         {
            throw new UnsupportedOperationException("Unsupported hqlOrder: " + sort_order);
         }
      }
   }

   /* Binary Operators. */
   @Override
   public Object visitBinary(BinaryExpression binary_expression, BinaryOperator operator,
         Object left_side, Object right_side)
   {
      StringBuilder sb = new StringBuilder().append('(').append(left_side);

      if (right_side.equals(""))
      {
         switch (operator)
         {
            case EQ:
               sb.append(" IS NULL ");
               break;
            case NE:
               sb.append(" IS NOT NULL ");
               break;
            default:
               throw new UnsupportedOperationException("Unsupported operator for null values: " + operator.toUriLiteral());
         }
      }
      else
      {
        switch (operator)
       {
         case EQ:
            sb.append(" = ");
            break;
         case NE:
            sb.append(" != ");
            break;
         case GT:
            sb.append(" > ");
            break;
         case GE:
            sb.append(" >= ");
            break;
         case LT:
            sb.append(" < ");
            break;
         case LE:
         {
            sb.append(" <= ");
            break;
         }
         case AND:
         {
            sb.append(" AND ");
            break;
         }
         case OR:
         {
            sb.append(" OR ");
            break;
         }
         default:
            // Other operators are not supported for SQL Statements
            throw new UnsupportedOperationException("Unsupported operator: " + operator.toUriLiteral());
         }
         // return the binary statement
         sb.append(right_side).append(')');
      }
      return sb.toString();
   }

   /* Unary operator. */
   @Override
   public Object visitUnary(UnaryExpression unary_expression, UnaryOperator operator, Object operand)
   {
      switch (operator)
      {
         case MINUS:
         {
            if (operand instanceof Long)
            {
               return -((Long) operand);
            }
            else if (operand instanceof Double)
            {
               return -((Double) operand);
            }
            else
            {
               throw new UnsupportedOperationException("Invalid expression: " + unary_expression.getUriLiteral());
            }
         }
         case NOT:
         {
            return "not " + operand;
         }
         default:
            break;
      }
      throw new UnsupportedOperationException("Unsupported operator: " + operator.toUriLiteral());
   }

   /* A constant. */
   @Override
   public Object visitLiteral(LiteralExpression literal, EdmLiteral edm_literal)
   {
      Object result;
      Type resultType;
      Class<?> type = edm_literal.getType().getDefaultType();

      if (type == null)
      {
         return "";
      }
      else if (type.equals(Boolean.class))
      {
         result = Boolean.valueOf(edm_literal.getLiteral());
         resultType = StandardBasicTypes.BOOLEAN;
      }
      else if (type.equals(Byte.class) || type.equals(Short.class) ||
               type.equals(Integer.class) || type.equals(Long.class))
      {
         result = Long.valueOf(edm_literal.getLiteral());
         resultType = StandardBasicTypes.LONG;
      }
      else if (type.equals(Double.class) || type.equals(BigDecimal.class))
      {
         result = Double.valueOf(edm_literal.getLiteral());
         resultType = StandardBasicTypes.DOUBLE;
      }
      else if (type.equals(String.class))
      {
         result = edm_literal.getLiteral();
         resultType = StandardBasicTypes.STRING;
      }
      else if (type.equals(Calendar.class))
      {
         String datestr = edm_literal.getLiteral();
         SimpleDateFormat sdf;
         // Workarounds a datetime issue in the OData API
         int idx = datestr.lastIndexOf('.');
         if (idx != -1)
         {
            sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            String millis = datestr.substring(idx + 1);
            switch (millis.length())
            {
               case 1:
                  datestr = datestr + "00";
                  break;
               case 2:
                  datestr = datestr + "0";
                  break;
            }
         }
         else
         {
            sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
         }
         try
         {
            result = sdf.parse(datestr);
            resultType = StandardBasicTypes.TIMESTAMP;
         }
         catch (ParseException ex)
         {
            throw new IllegalArgumentException("Invalid date format");
         }
      }
      else
      {
         throw new IllegalArgumentException("Type " + edm_literal.getType() + " is not supported by the service");
      }

      if (result instanceof Member)
      {
         return result;
      }

      hqlParameters.add(new SQLVisitorParameter(positionToUse, result, resultType));
      return "?" + positionToUse++;
   }

   /* Translates to an SQL function. */
   @Override
   public Object visitMethod(MethodExpression method_expression, MethodOperator method, List<Object> parameters)
   {
      String result;
      switch (method)
      {
         // String functions
         case CONCAT:
         {
            StringBuilder sb = new StringBuilder("CONCAT(");
            sb.append(parameters.get(0)).append(", ");
            sb.append(parameters.get(1)).append(") ");
            result = sb.toString();
            break;
         }
         case INDEXOF:
         {
            StringBuilder sb = new StringBuilder("LOCATE(");
            sb.append(parameters.get(0)).append(", ");
            sb.append(parameters.get(1)).append(") ");
            result = sb.toString();
            break;
         }
         case LENGTH:
         {
            StringBuilder sb = new StringBuilder("LENGTH(");
            sb.append(parameters.get(0)).append(")");
            result = sb.toString();
            break;
         }
         case SUBSTRING:
         {
            StringBuilder sb = new StringBuilder("SUBSTRING(");
            sb.append(parameters.get(0)).append(", ");
            sb.append(parameters.get(1));
            if (parameters.size() == 3)
            {
               sb.append(", ").append(parameters.get(2));
            }
            result = sb.append(")").toString();
            break;
         }
         case TOUPPER:
         {
            StringBuilder sb = new StringBuilder("UPPER(");
            sb.append(parameters.get(0)).append(")");
            result = sb.toString();
            break;
         }
         case TOLOWER:
         {
            StringBuilder sb = new StringBuilder("LOWER(");
            sb.append(parameters.get(0)).append(")");
            result = sb.toString();
            break;
         }
         case TRIM:
         {
            StringBuilder sb = new StringBuilder("TRIM(");
            sb.append(parameters.get(0)).append(")");
            result = sb.toString();
            break;
         }
         case ENDSWITH:
         {
            StringBuilder sb = new StringBuilder();
            sb.append(parameters.get(0));
            Object value = hqlParameters.remove(hqlParameters.size() - 1).getValue();
            positionToUse--;
            String valueString = value.toString();
            if (valueString.contains("_"))
            {
               sb.append(" LIKE '%").append(valueString.replace("_", "\\_")).append("' ESCAPE '\\' ");
            }
            else
            {
               sb.append(" LIKE '%").append(valueString).append("' ");
            }
            result = sb.toString();
            break;
         }
         case STARTSWITH:
         {
            StringBuilder sb = new StringBuilder();
            sb.append(parameters.get(0));
            Object value = hqlParameters.remove(hqlParameters.size() - 1).getValue();
            positionToUse--;
            String valueString = value.toString();
            if (valueString.contains("_"))
            {
               sb.append(" LIKE '").append(valueString.replace("_", "\\_")).append("%' ESCAPE '\\' ");
            }
            else
            {
               sb.append(" LIKE '").append(valueString).append("%' ");
            }
            result = sb.toString();
            break;
         }
         case SUBSTRINGOF:
         {
            StringBuilder sb = new StringBuilder();
            sb.append(parameters.get(1));
            Object value = hqlParameters.remove(hqlParameters.size() - 1).getValue();
            positionToUse--;
            String valueString = value.toString();
            if (valueString.contains("_"))
            {
               sb.append(" LIKE '%").append(valueString.replace("_", "\\_")).append("%' ESCAPE '\\' ");
            }
            else
            {
               sb.append(" LIKE '%").append(valueString).append("%' ");
            }
            result = sb.toString();
            break;
         }

         // Date functions
         case DAY:
         {
            result = "DAY(" + parameters.get(0) + ") ";
            break;
         }
         case HOUR:
         {
            result = "HOUR(" + parameters.get(0) + ") ";
            break;
         }
         case MINUTE:
         {
            result = "MINUTE(" + parameters.get(0) + ") ";
            break;
         }
         case MONTH:
         {
            result = "MONTH(" + parameters.get(0) + ") ";
            break;
         }
         case SECOND:
         {
            result = "SECOND(" + parameters.get(0) + ") ";
            break;
         }
         case YEAR:
         {
            result = "YEAR(" + parameters.get(0) + ") ";
            break;
         }

         // Math functions
         case CEILING:
         {
            result = "CEILING(" + parameters.get(0) + ") ";
            break;
         }
         case FLOOR:
         {
            result = "FLOOR(" + parameters.get(0) + ") ";
            break;
         }
         case ROUND:
         {
            result = "ROUND(" + parameters.get(0) + ") ";
            break;
         }

         default:
            throw new UnsupportedOperationException("Unsupported method: " + method.toUriLiteral());
      }

      return result;
   }

   @Override
   public Object visitMember(MemberExpression member_expression, Object path, Object property)
   {
      /* The property shall be handled inside visitProperty method in
       * ODataSQLVisitor implementation */
      return property;
   }

   /* Returns the field name corresponding to the given EDM type. */
   @Override
   public abstract Object visitProperty(PropertyExpression property_expression, String uri_literal, EdmTyped edm_property);

   /**
    * Returns the HQL query.
    *
    * @return the HQL query generated by this visitor
    */
   public String getHqlQuery()
   {
      StringBuilder sb = new StringBuilder(hqlPrefix);
      if (filterExpression != null)
      {
         sb.append("WHERE ").append(hqlFilter);
      }
      if (orderExpression != null)
      {
         sb.append(" ORDER BY ").append(hqlOrder);
      }
      return sb.toString();
   }

   /**
    * Returns a list of all parameters of the HQL query.
    *
    * @return an ordered list containing all parameters of the HQL query generated by this visitor
    */
   public List<SQLVisitorParameter> getHqlParameters()
   {
      // protected copy
      return Collections.unmodifiableList(hqlParameters);
   }

   public String getHqlPrefix()
   {
      return hqlPrefix;
   }

   /**
    * Returns the generated HQL clause condition.
    *
    * @return a string representation of the HQL 'WHERE' request
    */
   public String getHqlFilter()
   {
      return hqlFilter;
   }

   /**
    * Returns the generated HQL order.
    *
    * @return a string representation of the HQL 'ORDER BY' request
    */
   public String getHqlOrder()
   {
      return hqlOrder;
   }

   protected static class Member
   {
      private final String name;

      public Member(String name)
      {
         this.name = Objects.requireNonNull(name);
      }

      public String getName()
      {
         return name;
      }

      @Override
      public String toString()
      {
         return this.name;
      }
   }
}
