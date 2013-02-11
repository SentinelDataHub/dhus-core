package org.dhus.metrics.external.influx.database;

import org.influxdb.annotation.Column;

public class ProdSyncCounterPOJO extends InfluxCounter
{
   @Column(name = "platformSerialId", tag = true)
   private String platformSerialId;
   
   @Column(name = "productType", tag = true)
   private String productType;
   
   @Column(name = "platformShortName", tag = true)
   private String platformShortName;

   public String getPlatformSerialId()
   {
      return platformSerialId;
   }

   public void setPlatformSerialId(String platformSerialId)
   {
      this.platformSerialId = platformSerialId;
   }

   public String getPlatformShortName()
   {
      return platformShortName;
   }

   public void setPlatformShortName(String platformShortName)
   {
      this.platformShortName = platformShortName;
   }

   public String getProductType()
   {
      return productType;
   }

   public void setProductType(String productType)
   {
      this.productType = productType;
   }

}
