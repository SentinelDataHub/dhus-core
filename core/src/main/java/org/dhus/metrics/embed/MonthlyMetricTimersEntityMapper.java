package org.dhus.metrics.embed;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MonthlyMetricTimersEntityMapper extends MetricTimersEntityMapper
{
   private static final Logger LOGGER = LogManager.getLogger(MetricCounterEntityMapper.class);
   
   @Override
   public String processName(String name)
   {
      //prod_sync.sync1.source1.timers.GRD.S1.A.transfertRate
      if (name != null)
      {
         StringBuilder sb = new StringBuilder();
         String[] nameParts = name.split("\\.");
         for (int i=0; i < nameParts.length; i++)
         {
            sb.append(nameParts[i]);
            if ("timers".equalsIgnoreCase(nameParts[i]))
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
