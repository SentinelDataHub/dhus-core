package org.dhus.metrics.embed;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MonthlyMetricCounterEntityMapper extends MetricCounterEntityMapper
{
   private static final Logger LOGGER = LogManager.getLogger(MetricCounterEntityMapper.class);
   
   @Override
   public String processName(String name)
   {
      //prod_sync.sync1.source1.counters.GRD.S1.A.success
      if (name != null)
      {
         StringBuilder sb = new StringBuilder();
         String[] nameParts = name.split("\\.");
         for (int i=0; i < nameParts.length; i++)
         {
            sb.append(nameParts[i]);
            if ("counters".equalsIgnoreCase(nameParts[i]))
            {
               sb.append(".monthly");
            }
            
            if(i<nameParts.length-1)
            {
               sb.append(".");
            }
         }
         String result = sb.toString();
         LOGGER.debug("Name: {}", result);
         return result;
      }
      return null;
   }

}
