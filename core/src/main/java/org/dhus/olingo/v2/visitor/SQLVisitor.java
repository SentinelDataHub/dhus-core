/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018-2020 GAEL Systems
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

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.primitivetype.EdmBoolean;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDate;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDateTimeOffset;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDecimal;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDouble;
import org.apache.olingo.commons.core.edm.primitivetype.EdmInt16;
import org.apache.olingo.commons.core.edm.primitivetype.EdmInt32;
import org.apache.olingo.commons.core.edm.primitivetype.EdmInt64;
import org.apache.olingo.commons.core.edm.primitivetype.EdmString;
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

import org.hibernate.type.StandardBasicTypes;

public abstract class SQLVisitor implements Serializable, ExpressionVisitor<Object>
{
   private static final long serialVersionUID = 1L;
   protected static final Logger LOGGER = LogManager.getLogger(SQLVisitor.class);

   /** Date formatter for Edm.Date. */
   protected static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
   /** Date formatter for Edm.DateTimeOffset. */
   protected static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSS");

   /** JPA positional parameter token. */
   protected static final String HQL_REQUEST_PARAM = "?";

   private static final int DEFAULT_SKIP_VALUE = 0;

   private static final int DEFAULT_TOP_VALUE = ApplicationContextProvider.getBean(ConfigurationManager.class)
         .getOdataConfiguration().getDefaultTop();

   private transient final FilterOption filterOption;
   private final OrderByOption orderByOption;
   private final TopOption topOption;
   private final SkipOption skipOption;
   /** Stores values of every JPA positional parameters. */
   protected final List<SQLVisitorParameter> hqlParameters;

   private final String hqlPrefix;
   private final String selectClause;
   private final String fromClause;
   private String hqlFilter;
   private String hqlOrder;

   /** Position suffix for JPA parameter tokens, increment at every use. */
   protected int positionToUse = 1;

   private int skip;
   private int top;

   protected SQLVisitor(String selectClause, String fromClause, FilterOption filter, OrderByOption order,
         TopOption topOption, SkipOption skipOption)
   {
      this.hqlPrefix = selectClause + ' ' + fromClause + ' ';
      this.selectClause = selectClause;
      this.fromClause = fromClause;
      this.filterOption = filter;
      this.orderByOption = order;
      this.hqlParameters = new LinkedList<>();
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

      StringBuilder sb = new StringBuilder().append('(').append(left);

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
            case HAS:
               sb.append(" IN ");
               break;
            default:
               // Other operators are not supported for SQL Statements
               throw new UnsupportedOperationException("Unsupported operator: " + operator.name());
         }
         // return the binary statement
         sb.append(right).append(')');
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

   // FIXME duplicated code from fr.gael.dhus.olingo.v1.SQLVisitor
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
         case CONTAINS:
         {
            StringBuilder sb = new StringBuilder();
            sb.append(parameters.get(0));
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
            throw new UnsupportedOperationException("Unsupported method: " + methodCall.toString());
      }

      return result;
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
         String stringLiteral = "";
         if (literal.getText().length() > 2)
         {
            stringLiteral = literalAsString.substring(1, literalAsString.length() - 1);
         }
         LOGGER.debug("stringLiteral {}", stringLiteral);
         hqlParameters.add(new SQLVisitorParameter(positionToUse, stringLiteral, StandardBasicTypes.STRING));
         return HQL_REQUEST_PARAM + positionToUse++;
      }
      else if (literal.getType() instanceof EdmInt16 || literal.getType() instanceof EdmInt32)
      {
         int intValue = Integer.parseInt(literalAsString);
         LOGGER.debug("Integer {}", intValue);
         hqlParameters.add(new SQLVisitorParameter(positionToUse, intValue, StandardBasicTypes.INTEGER));
         return HQL_REQUEST_PARAM + positionToUse++;
      }
      else if (literal.getType() instanceof EdmInt64)
      {
         int intValue = Integer.parseInt(literalAsString);
         LOGGER.debug("Integer {}", intValue);
         hqlParameters.add(new SQLVisitorParameter(positionToUse, intValue, StandardBasicTypes.BIG_INTEGER));
         return HQL_REQUEST_PARAM + positionToUse++;
      }
      else if (literal.getType() instanceof EdmBoolean)
      {
         boolean bValue = Boolean.parseBoolean(literalAsString);
         LOGGER.debug("Boolean {}", bValue);
         hqlParameters.add(new SQLVisitorParameter(positionToUse, bValue, StandardBasicTypes.BOOLEAN));
         return HQL_REQUEST_PARAM + positionToUse++;
      }
      else if (literal.getType() instanceof EdmDecimal || literal.getType() instanceof EdmDouble)
      {
         double doubleValue = Double.parseDouble(literalAsString);
         LOGGER.debug("Double {}", doubleValue);
         hqlParameters.add(new SQLVisitorParameter(positionToUse, doubleValue, StandardBasicTypes.DOUBLE));
         return HQL_REQUEST_PARAM + positionToUse++;
      }
      else if (literal.getType() instanceof EdmDateTimeOffset)
      {
         return parseDate(literalAsString, DATE_TIME_FORMATTER);
      }
      else if (literal.getType() instanceof EdmDate)
      {
         return parseDate(literalAsString, DATE_FORMATTER);
      }
      else if (literal.getType() == null && literalAsString.equals("null"))
      {
         return null;
      }
      throw new ODataApplicationException(
            "Only Edm.IntXX, Edm.String, EdmBoolean, EdmDecimal, EdmDouble, EdmDateTimeOffset, EdmDate literals are implemented",
            HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
   }

   private Object parseDate(String literalAsString, SimpleDateFormat dateFormat) throws ODataApplicationException
   {
      Date parsedTimeStamp;
      try
      {
         LOGGER.debug("dateformat {}", dateFormat.toPattern());
         parsedTimeStamp = dateFormat.parse(literalAsString);
         Timestamp timestamp = new Timestamp(parsedTimeStamp.getTime());
         LOGGER.debug("Timestamp " + timestamp);
         hqlParameters.add(new SQLVisitorParameter(positionToUse, timestamp, StandardBasicTypes.TIMESTAMP));
         return HQL_REQUEST_PARAM + positionToUse++;
      }
      catch (ParseException e)
      {
         LOGGER.debug("An exception occured while parsing date: '{}'", e.toString());
         throw new ODataApplicationException("Could not parse date " + literalAsString,
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
   }

   /**
    * Returns the HQL query.
    *
    * @return the HQL query generated by this visitor
    * @throws ODataApplicationException could not evaluate filter expression
    */
   public String getHqlQuery() throws ODataApplicationException
   {
      StringBuilder sb = new StringBuilder(hqlPrefix);
      if (filterOption != null)
      {
         sb.append("WHERE ").append(getHqlFilter());
         LOGGER.debug("Query filter: {}", hqlFilter);
      }
      if (orderByOption != null)
      {
         sb.append(" ORDER BY ").append(getHqlOrder());
         LOGGER.debug("Query order by: {}", hqlOrder);
      }
      return sb.toString();
   }

   /**
    * Returns a list of all parameters of the HQL query.
    *
    * @return an unmodifiable ordered list containing all parameters of the HQL query generated by this visitor
    */
   public List<SQLVisitorParameter> getHqlParameters()
   {
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
    * @throws ODataApplicationException could not evaluate filter expression
    */
   public String getHqlFilter() throws ODataApplicationException
   {
      if(hqlFilter == null && filterOption != null)
      {
         try
         {
            String visitorResult = (String) filterOption.getExpression().accept(this);
            hqlFilter = visitorResult;
         }
         catch (ExpressionVisitException | ODataApplicationException e)
         {
            LOGGER.error("Cannot evaluate filter expression: {}", e.getMessage());
            throw new ODataApplicationException("Cannot evaluate filter expression: " + e.getMessage(),
                  HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH, e);
         }
      }

      return hqlFilter;
   }

   /**
    * Returns the generated HQL order.
    *
    * @return a string representation of the HQL 'ORDER BY' request
    */
   public String getHqlOrder()
   {
      if(hqlOrder == null)
      {
         hqlOrder = generateOrderByClause();
      }
      return hqlOrder;
   }

   // TODO review
   private String generateOrderByClause()
   {
      if (orderByOption == null)
      {
         return null;
      }
      LOGGER.debug("OrderBy is: {}", hqlOrder);
      StringBuilder orderByStringBuilder = new StringBuilder();

      if (hqlOrder != null)
      {
         orderByStringBuilder.append(hqlOrder).append(" ");
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

   public int getSkip()
   {
      if (skipOption == null)
      {
         return DEFAULT_SKIP_VALUE;
      }
      skip = skipOption.getValue();
      return skip;
   }

   public int getTop()
   {
      if (topOption == null || topOption.getValue() <= 0)
      {
         return getDefaultTop();
      }
      top = topOption.getValue();
      return top;
   }

   /**
    * Provides the {@link #getTop()} method a value to return when the topOption is null.
    * Override to change the default $top value.
    * The default behaviour is to return the value set in the config file (dhus.xml).
    *
    * @return the default $top value
    */
   protected int getDefaultTop()
   {
      return DEFAULT_TOP_VALUE;
   }

   public String getSelectClause()
   {
      return selectClause;
   }

   public String getFromClause()
   {
      return fromClause;
   }
}
