package org.dhus.olingo.v2.visitor;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

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
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.dhus.olingo.v2.datamodel.MetricModel;

public class InfluxMetricsVisitor extends MetricSQLVisitor
{
   private static final DateTimeFormatter SQL_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

   /**
    * 
    */
   private static final long serialVersionUID = 9105182751130677463L;
   
   public InfluxMetricsVisitor(FilterOption filter, OrderByOption order, TopOption topOption, SkipOption skipOption)
   {
      super(filter, order, topOption, skipOption);
   }

   
   @Override
   public Object visitMember(Member member) throws ExpressionVisitException, ODataApplicationException
   {

      final List<UriResource> uriResourceParts = member.getResourcePath().getUriResourceParts();

      if (uriResourceParts.size() == 1 && uriResourceParts.get(0) instanceof UriResourcePrimitiveProperty)
      {
         String segmentVal = ((UriResourcePrimitiveProperty) uriResourceParts.get(0)).getSegmentValue();

         switch (segmentVal)
         {
            case MetricModel.TIMESTAMP: return "time";
            default:
               throw new ODataApplicationException("Property not supported: " + segmentVal,
                     HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
         }
      }
      throw new ODataApplicationException("Non-primitive properties are not supported in filter expressions",
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
         return "'"+dateTime+"'";
      }
      else if (literal.getType() instanceof EdmDate)
      {
         return "'" + literalAsString + "'";
      }
      else if (literal.getType() instanceof EdmTimeOfDay)
      {
         return "'" + literalAsString + "'";
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

}
