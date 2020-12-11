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
package org.dhus.olingo.v2.visitor;

import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.primitivetype.EdmBoolean;
import org.apache.olingo.commons.core.edm.primitivetype.EdmByte;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDate;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDateTimeOffset;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDecimal;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDouble;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDuration;
import org.apache.olingo.commons.core.edm.primitivetype.EdmInt16;
import org.apache.olingo.commons.core.edm.primitivetype.EdmInt32;
import org.apache.olingo.commons.core.edm.primitivetype.EdmInt64;
import org.apache.olingo.commons.core.edm.primitivetype.EdmSByte;
import org.apache.olingo.commons.core.edm.primitivetype.EdmString;
import org.apache.olingo.commons.core.edm.primitivetype.EdmTimeOfDay;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;
import org.apache.olingo.server.api.uri.queryoption.expression.UnaryOperatorKind;

/**
 * An SQL visitor, if you want the HQL visitor, see {@link SQLVisitor}.
 */
public abstract class HQLVisitor implements ExpressionVisitor<Object>
{
   protected static final Logger LOGGER = LogManager.getLogger();

   private static final DateTimeFormatter SQL_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

   private static final int DEFAULT_TOP_VALUE = ApplicationContextProvider.getBean(ConfigurationManager.class)
         .getOdataConfiguration().getDefaultTop();

   private final FilterOption filterOption;
   private final OrderByOption orderByOption;
   private final TopOption topOption;
   private final SkipOption skipOption;

   private String sqlFilter;
   private String sqlOrder;
   private int skip;
   private int top;

   protected HQLVisitor(FilterOption filter, OrderByOption order, TopOption topOption, SkipOption skipOption)
   {
      this.filterOption = filter;
      this.orderByOption = order;
      this.topOption = topOption;
      this.skipOption = skipOption;
   }

   @Override
   public Object visitAlias(String arg0) throws ExpressionVisitException, ODataApplicationException
   {
      return null;
   }

   @Override
   public Object visitBinaryOperator(BinaryOperatorKind operator, Object left, Object right)
         throws ExpressionVisitException, ODataApplicationException
   {

      StringBuilder sb = new StringBuilder().append(left);

      if (right == null)
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
               throw new UnsupportedOperationException("Unsupported operator for null values: " + operator.name());
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
               sb.append(" <= ");
               break;
            case AND:
               sb.append(" AND ");
               break;
            case OR:
               sb.append(" OR ");
               break;
            case ADD:
               sb.append(" + ");
               break;
            case SUB:
               sb.append(" - ");
               break;
            case MUL:
               sb.append(" * ");
               break;
            case DIV:
               sb.append(" / ");
               break;
            case MOD:
               sb.append(" % "); // HSQLDB does not support the SQL mod (%) operator
               break;
            default:
               // Other operators are not supported for SQL Statements
               throw new UnsupportedOperationException("Unsupported operator: " + operator.name());
         }
         // return the binary statement
         sb.append(right);
      }
      return sb.toString();
   }

   @Override
   public Object visitEnum(EdmEnumType type, List<String> enumValues)
         throws ExpressionVisitException, ODataApplicationException
   {
      throw new ODataApplicationException("Not implemented",
            HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
   }

   @Override
   public Object visitLambdaExpression(String lambdaFunction, String lambdaVariable, Expression expression)
         throws ExpressionVisitException, ODataApplicationException
   {
      throw new ODataApplicationException("Not implemented",
            HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
   }

   @Override
   public Object visitLambdaReference(String variableName) throws ExpressionVisitException, ODataApplicationException
   {
      throw new ODataApplicationException("Not implemented",
            HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
   }

   @Override
   public abstract Object visitMember(Member member) throws ExpressionVisitException, ODataApplicationException;

   @Override
   public Object visitMethodCall(MethodKind methodCall, List<Object> parameters)
         throws ExpressionVisitException, ODataApplicationException
   {
      String result;
      switch (methodCall)
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
            sb.append(parameters.get(1)).append(", ");
            sb.append(parameters.get(0)).append(") ");
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
            String valueString = unquote(parameters.get(1).toString());
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
            String valueString = unquote(parameters.get(1).toString());
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
         case CONTAINS:
         {
            StringBuilder sb = new StringBuilder();
            sb.append(parameters.get(0));
            String valueString = unquote(parameters.get(1).toString());
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
            result = "FLOOR(SECOND(" + parameters.get(0) + ")) ";
            break;
         }
         case FRACTIONALSECONDS:
         {
            result = "SECOND(" + parameters.get(0) + ") ";
            break;
         }
         case YEAR:
         {
            result = "YEAR(" + parameters.get(0) + ") ";
            break;
         }
         case DATE:
         {
            result = "CAST(" + parameters.get(0) + " AS DATE) ";
            break;
         }
         case TIME:
         {
            result = "CAST(" + parameters.get(0) + " AS TIME) ";
            break;
         }
         case NOW:
         {
            result = "LOCALTIMESTAMP(3) ";
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
            throw new UnsupportedOperationException("Unsupported method: " + methodCall.toString());
      }

      return result;
   }

   private static String unquote(String literal)
   {
      if (literal != null && literal.length() > 2)
      {
         return literal.substring(1, literal.length() - 1);
      }
      return literal;
   }

   @Override
   public Object visitUnaryOperator(UnaryOperatorKind operator, Object operand)
         throws ExpressionVisitException, ODataApplicationException
   {
      // OData allows two different unary operators. We have to check that the type of the operand fits the operator

      if (operator == UnaryOperatorKind.NOT)
      {
         if (operand instanceof Boolean)
         {
            // 1.1) boolean negation
            return !(Boolean) operand;
         }
         else if (operand instanceof String)
         {
            // 1.2) negation on a method (startswith, endswith, substringof, ...) or expression
            return " NOT (" + operand + ") ";
         }
      }
      else if (operator == UnaryOperatorKind.MINUS && operand instanceof Integer)
      {
         // 2.) arithmetic minus
         return -(Integer) operand;
      }
      else if (operator == UnaryOperatorKind.MINUS && operand instanceof Double)
      {
         // 2.) arithmetic minus
         return -(Double) operand;
      }

      // Operation not processed, throw an exception
      throw new ODataApplicationException("Invalid type for unary operator",
            HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
   }

   @Override
   public Object visitTypeLiteral(EdmType type) throws ExpressionVisitException, ODataApplicationException
   {
      throw new ODataApplicationException("Type literals are not implemented",
            HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
   }

   @Override
   public Object visitLiteral(Literal literal) throws ExpressionVisitException, ODataApplicationException
   {
      // String literals start and end with an single quotation mark
      String literalAsString = literal.getText();
      if (literal.getType() instanceof EdmString)
      {
         return literalAsString;
      }
      else if (literal.getType() instanceof EdmInt16 || literal.getType() instanceof EdmInt32
            || literal.getType() instanceof EdmSByte || literal.getType() instanceof EdmByte)
      {
         Integer intValue = Integer.parseInt(literalAsString);
         return intValue;
      }
      else if (literal.getType() instanceof EdmInt64)
      {
         Integer intValue = Integer.parseInt(literalAsString);
         return intValue;
      }
      else if (literal.getType() instanceof EdmBoolean)
      {
         Boolean bValue = Boolean.getBoolean(literalAsString);
         return bValue;
      }
      else if (literal.getType() instanceof EdmDecimal || literal.getType() instanceof EdmDouble)
      {
         Double doubleValue = Double.parseDouble(literalAsString);
         return doubleValue;
      }
      else if (literal.getType() instanceof EdmDateTimeOffset)
      {
         String dateTime = OffsetDateTime.parse(literalAsString) // In OData4 DateTimeOffset literals may have an offset
               .withOffsetSameInstant(ZoneOffset.UTC) // Translate to the UTC timezone
               .toLocalDateTime() // Remove the timezone information
               .format(SQL_TIMESTAMP_FORMATTER);
         return "TIMESTAMP'"+dateTime+"'";
      }
      else if (literal.getType() instanceof EdmDate)
      {
         return "DATE'" + literalAsString + "'";
      }
      else if (literal.getType() instanceof EdmTimeOfDay)
      {
         return "TIME'" + literalAsString + "'";
      }
      else if (literal.getType() instanceof EdmDuration)
      {
         Duration duration = Duration.parse(literalAsString.substring(9, literalAsString.length() - 1));
         return "INTERVAL'" + duration.getSeconds() + '.' + (int)(duration.getNano()/1_000_000) + "' SECOND";
      }
      else if (literal.getType() == null && literalAsString.equals("null"))
      {
         return null;
      }
      throw new ODataApplicationException(
            "Unsupported literal type: " + literal.getType(),
            HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
   }

   /**
    * Returns the SQL query.
    *
    * @param prefix the FROM clause
    * @return the SQL query generated by this visitor
    */
   public String getSqlQuery(String prefix)
   {
      StringBuilder sb = new StringBuilder(prefix);
      if (filterOption != null)
      {
         sb.append("WHERE ").append(getSqlFilter());
         LOGGER.debug("Query filter: {}", sqlFilter);
      }
      if (orderByOption != null)
      {
         sb.append(" ORDER BY ").append(getSqlOrder());
         LOGGER.debug("Query order by: {}", sqlOrder);
      }
      return sb.toString();
   }

   /**
    * Returns the generated SQL clause condition.
    *
    * @return a string representation of the SQL 'WHERE' request
    */
   public String getSqlFilter()
   {
      if(sqlFilter == null && filterOption != null)
      {
         try
         {
            String visitorResult = (String) filterOption.getExpression().accept(this);
            sqlFilter = visitorResult;
         }
         catch (ExpressionVisitException | ODataApplicationException e)
         {
            LOGGER.error("Cannot evaluate filter expression: {}", e.getMessage());
         }
      }

      return sqlFilter;
   }

   /**
    * Returns the generated SQL order.
    *
    * @return a string representation of the SQL 'ORDER BY' request
    */
   public String getSqlOrder()
   {
      if(sqlOrder == null)
      {
         sqlOrder = generateOrderByClause();
      }
      return sqlOrder;
   }

   private String generateOrderByClause()
   {
      if (orderByOption == null)
      {
         return null;
      }
      LOGGER.debug("OrderBy is: {}", sqlOrder);
      StringBuilder orderByStringBuilder = new StringBuilder();

      if (sqlOrder != null)
      {
         orderByStringBuilder.append(sqlOrder).append(" ");
      }

      List<OrderByItem> listOrders = orderByOption.getOrders();
      int count = 0;
      for (OrderByItem orderByItem: listOrders)
      {
         if (count > 0 && count < listOrders.size())
         {
            // We add a comma to the orderBy String
            orderByStringBuilder.append(",");
         }
         Expression expression = orderByItem.getExpression();
         LOGGER.debug("Expression: {}", expression.toString());
         LOGGER.debug("orderByItem isDescending: {}", orderByItem.isDescending());
         try
         {
            String val = (String) expression.accept(this);
            LOGGER.debug("Val : " + val);
            if (val != null)
            {
               orderByStringBuilder.append(val).append(" ");
               if (orderByItem.isDescending())
               {
                  orderByStringBuilder.append("DESC");
               }
               else
               {
                  orderByStringBuilder.append("ASC");
               }
            }
         }
         catch (ExpressionVisitException | ODataApplicationException e)
         {
            LOGGER.error("An error occured during orderBy validation: {}", expression.toString());
         }
         count++;
      }
      LOGGER.debug("StringBuilder result: {}", orderByStringBuilder.toString());
      return orderByStringBuilder.toString();
   }

   /**
    * Provides the {@link #getTop()} a value to return when the topOption is null.
    * Override to change the default $top value.
    * The default behaviour is to return the value set in the config file (dhus.xml).
    *
    * @return the default $top value
    */
   protected int getDefaultTop()
   {
      return DEFAULT_TOP_VALUE;
   }

   /**
    * The $skip URI parameter value, returns 0 if $skip is not set.
    *
    * @return the value of $skip, or 0
    */
   public int getSkip()
   {
      if (skipOption == null)
      {
         return 0;
      }
      skip = skipOption.getValue();
      return skip;
   }

   /**
    * The $top URI parameter value, return a default value if $top is not set
    *
    * @return the value of $top, or the value returned by {@link #getDefaultTop()}
    */
   public int getTop()
   {
      if (topOption == null || topOption.getValue() <= 0)
      {
         return getDefaultTop();
      }
      top = topOption.getValue();
      return top;
   }
}
