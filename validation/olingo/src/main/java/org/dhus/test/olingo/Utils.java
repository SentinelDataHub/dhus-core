package org.dhus.test.olingo;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

import org.apache.logging.log4j.Logger;
import org.dhus.test.olingo.scenario.TestScenario;

public class Utils
{  
   /* Entityset names */
   // TODO move to relevant classes
   public static final String EVENTSYNCHRONIZERS_NAME = "EventSynchronizers";
   public static final String SCANNERS_NAME = "Scanners";
   public static final String USERSYNCHRONIZERS_NAME = "UserSynchronizers";
   public static final String CLASSES_NAME = "Classes";
   public static final String USERS_NAME = "Users";

   /* format and timeZone for Dates */
   public static final String DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss"; 
   
   public static final String GMT_TIMEZONE = "GMT";
   public static final String UTC_TIMEZONE = "UTC";

   public static GregorianCalendar parseToGregorianCalendar(String dateString) throws ParseException
   {
      DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING);
      GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone(GMT_TIMEZONE));
      calendar.setTime(dateFormat.parse(dateString));
      return calendar;
   }
   
   /**
    * Combines a list of keys and a list of values into a map.
    * 
    * @param keys
    * @param values
    * @return
    */
   public static Map<String, Object> makeEntryMap(List<String> keys,
         List<Object> values)
   {
      if(keys.size() != values.size())
      {
         throw new IllegalArgumentException("Lists must be of equal size");
      }
      Map<String, Object> referenceMap = new HashMap<>();
      for(int i=0; i<keys.size(); i++)
      {
         referenceMap.put(keys.get(i), values.get(i));
      }
      return referenceMap;
   }
   
   public static boolean validateProperties(Map<String, Object> reference, Map<String, Object> properties)
   {
      return validatePropertiesExcept(reference, properties, Collections.<String>emptySet());
   }
   
   public static boolean validatePropertiesExcept(Map<String, Object> reference,
         Map<String, Object> properties, Set<String> excludedProperties)
   {
      for(String propertyKey : reference.keySet())
      {
         // ignore excluded properties
         if(excludedProperties.contains(propertyKey))
         {
            continue;
         }
         
         if(!properties.containsKey(propertyKey))
         {
            TestManager.logScenarioError("Missing key "+propertyKey+" in tested entry.");
            return false;
         }
         
         if(!Objects.equals(reference.get(propertyKey), properties.get(propertyKey)))
         {
            TestManager.logScenarioError("Unexpected value for key "+propertyKey+" in tested entry: "
                  + "expected ["+reference.get(propertyKey)+"]"+", found ["+properties.get(propertyKey)+"]");
            return false;
         }
      }
      return true;
   }
   
   /**
    * Return the MD5 of a password
    * 
    * @param pass
    * @return MD5 of pass
    * @throws NoSuchAlgorithmException
    */
   public static String MungPass(String pass) throws NoSuchAlgorithmException
   {
      MessageDigest m = MessageDigest.getInstance("MD5");
      byte[] data = pass.getBytes();
      m.update(data, 0, data.length);
      BigInteger i = new BigInteger(1, m.digest());
      return String.format("%1$032X", i);
   }

   /**
    * Write into logger according to result of the test
    * 
    * @param LOGGER - Logger object
    * @param success - result of test
    * @param classResult - Class of test
    */
   public static void loggerResult(Logger LOGGER, boolean success,
         Class<? extends TestScenario> classResult)
   {
      if (success)
      {
         LOGGER.info("Test scenario '" + classResult.getSimpleName() + "' succeeded");
      }
      else
      {
         LOGGER.error("Test scenario '" + classResult.getSimpleName() + "' failed");
      }
   }

   public static boolean logicalXOR(boolean b, boolean c)
   {
      return b != c;
   }

   public static String calculateMD5(byte[] data) throws NoSuchAlgorithmException
   {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] nb = data;
      md.update(nb);
      nb = md.digest();
      StringBuffer sb = new StringBuffer();
   
      for (int i = 0; i < nb.length; i++)
      {
         sb.append(Integer.toString((nb[i] & 0xff) + 0x100, 16).substring(1));
      }
      return sb.toString();
   }
}
