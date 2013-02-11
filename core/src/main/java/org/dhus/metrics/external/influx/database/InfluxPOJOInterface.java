package org.dhus.metrics.external.influx.database;

import java.time.Instant;

public interface InfluxPOJOInterface
{
   public Instant getTime();
   
   public void setTime(Instant time);

   public String getDhusInst();

   public void setDhusInst(String dhusInst);
}
