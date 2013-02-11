package fr.gael.dhus.sync.impl;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.dhus.store.ingestion.IngestibleODataProduct;

import fr.gael.dhus.database.object.MetadataIndex;
import fr.gael.dhus.olingo.v1.Model;

/**
 * @author rosine
 *
 */
/**
 * @author rosine
 *
 */
public class ProductInformationUtils
{
   /** Log. */
   private static final Logger LOGGER =
      LogManager.getLogger(ProductInformationUtils.class);

   public static String parseGeneralMetricNameFromEntry(IngestibleODataProduct product)
   {
      return parseGeneralMetricNameFromEntry(product.getMetadataIndexes());
   }
   
   public static String parseGeneralMetricNameFromEntry(List<MetadataIndex> metadataList)
   {
      StringBuffer commonMetricPartName = commonPartForMetricName(metadataList);
      
      StringBuffer sb = new StringBuffer("prod_sync.global.counters.");
      sb.append(commonMetricPartName);
      
      LOGGER.debug("**** Common part of metric name:  " + sb.toString());
      return sb.toString();
   }
   
   /**
    * @param product
    * @param syncId is of the sync used
    * @param sourceId id of the source used for sync
    * @param typeMetric can be "timers" or "counters"
    * @return
    */
   public static String parsePerSourceMetricNameFromEntry(List<MetadataIndex> metadataList,
         Long syncId, Long sourceId, String typeMetric, boolean addProductPart)
   {
      StringBuffer commonMetricPartName = commonPartForMetricName(metadataList);
      
      StringBuffer sb = new StringBuffer("prod_sync.sync").append(syncId).append(".source");
      if (sourceId != null)
      {
         sb.append(sourceId);
      }
      sb.append(".") .append(typeMetric);
      if (addProductPart)
      {
         sb.append(".").append(commonMetricPartName);
      }
      
      String name = sb.toString();
      LOGGER.debug("**** Metric name:  " + name);
      return name;
   }
   
   public static String parsePerSourceMetricNameFromEntry(IngestibleODataProduct product,
         Long syncId, Long sourceId, String type, boolean addProductPart)
   {
      return parsePerSourceMetricNameFromEntry(product.getMetadataIndexes(), syncId, sourceId, type, addProductPart);
   }

   private static StringBuffer commonPartForMetricName(List<MetadataIndex> metadataList)
   {
      StringBuffer commonMetricPartName = new StringBuffer();
      String productType = null;
      String platformShortName = null;
      String platformSerialId = null;
      
      for (MetadataIndex metadata : metadataList)
      {
         String name = metadata.getName();
         if ("Product type".equalsIgnoreCase(name))
         {
            productType = metadata.getValue();
         }
         else if ("Satellite".equalsIgnoreCase(name))
         {
            platformShortName = satelliteName(metadata);
         }
         else if ("Satellite number".equalsIgnoreCase(name))
         {
            platformSerialId = metadata.getValue();
         }
      }
      //Will return <productType>.<platformShortName>.<platformSerialIdentifier>
      commonMetricPartName.append(productType).append(".").append(platformShortName)
      .append(".").append(platformSerialId);
      return commonMetricPartName;
   }

   private static String satelliteName(MetadataIndex metadata)
   {
      String platformShortName;
      String value = metadata.getValue();
      if ("Sentinel-1".equalsIgnoreCase(value))
      {
         platformShortName = "S1";
      }
      else if ("Sentinel-2".equalsIgnoreCase(value))
      {
         platformShortName = "S2";
      }
      else if ("Sentinel-3".equalsIgnoreCase(value))
      {
         platformShortName = "S3";
      }
      else
      {
         platformShortName = "S5";
      }
      return platformShortName;
   }
}
