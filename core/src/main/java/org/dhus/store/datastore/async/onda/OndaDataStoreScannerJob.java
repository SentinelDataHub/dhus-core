package org.dhus.store.datastore.async.onda;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.config.OndaDataStoreConf;
import org.dhus.store.keystore.PersistentKeyStore;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.gael.dhus.DHuS;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationException;
import fr.gael.dhus.system.config.ConfigurationManager;


@Component
public class OndaDataStoreScannerJob implements Job
{
   public final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

   private static final Logger LOGGER = LogManager.getLogger(OndaDataStoreScannerJob.class);

   public static final String DATASTORE_NAME = "datastoreName";
   
   private static final String XML_ITEMS_PER_PAGE_SET = "count";
   private static final String XML_ENTRY = "entry";
   private static final String XML_ID = "ONDA:id";
   private static final String XML_NAME = "ONDA:name";
   private static final String XML_STORAGE = "ONDA:storage";
   private static final String XML_CREATION_DATE = "ONDA:creationDate";

   private static final String OPENSEARCH_URL_FILTER = "searchTerms=";
   private static final String OPENSEARCH_URL_SORT = "sortKeys=creationDate,,asc";
   
   private static final ProductService PRODUCT_SERVICE = ApplicationContextProvider.getBean(ProductService.class);
   private static final ConfigurationManager CONFIGURATION_MANAGER = ApplicationContextProvider.getBean(ConfigurationManager.class);

   private String name, url, region, lastCreationDateFromConf, filter;
   private int processed, referenced, pageSize;
   private String lastCreationDateUpdated;
   
   private OndaDataStoreConf ondaConf;
   
   private static List<String> runningScanners = new ArrayList<String> ();

   @Override
   public void execute(JobExecutionContext context) throws JobExecutionException
   {
      final JobDataMap jobParams = context.getJobDetail().getJobDataMap();
      name = jobParams.getString(DATASTORE_NAME);
      LOGGER.info("SCHEDULER : Onda Scanner '{}'", name);
      if (!DHuS.isStarted ())
      {
         LOGGER.warn("SCHEDULER : Not run while system not fully initialized.");
         return;
      }
      if (runningScanners.contains(name))
      {
         LOGGER.info("SCHEDULER : Onda Scanner '{}' is already running.", name);
         return;
      }
      runningScanners.add(name);
      try
      {
         long start = System.currentTimeMillis();
         processed = referenced = 0;
         ondaConf = ((OndaDataStoreConf)CONFIGURATION_MANAGER.getDataStoreManager().get(name));
         url = ondaConf.getOndaScanner().getOpensearchUrl();
         if (!url.endsWith("?"))
         {
            url += "?";
         }
         region = ondaConf.getObjectStorageCredential().getRegion();
         if(ondaConf.getOndaScanner().getLastCreationDate() == null)
         {         
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTimeInMillis(0L);
            lastCreationDateFromConf = DATE_TIME_FORMATTER.format(gc.getTime());
         }
         else
         {
            lastCreationDateFromConf = DATE_TIME_FORMATTER.format(ondaConf.getOndaScanner().getLastCreationDate().toGregorianCalendar().getTime());
         }
         pageSize = ondaConf.getOndaScanner().getPageSize();
         filter = ondaConf.getOndaScanner().getFilter();
         scan();
         LOGGER.info("SCHEDULER : Onda Scanner '{}' - {} references for {} results processed in {}ms", name, referenced, processed, (System.currentTimeMillis() - start));
      }
      finally
      {
         runningScanners.remove(name);
      }
   }

   private void scan()
   {
      LOGGER.debug("Onda Scanner last creation date : " + lastCreationDateFromConf);
      PersistentKeyStore keystore = new PersistentKeyStore(name);
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = null;
      List<OpenSearchResultEntry> openSearchResultEntries = null;
      try
      {
         builder = factory.newDocumentBuilder();
         String opensearchURI = url +"creationDate=[" + lastCreationDateFromConf +" TO NOW]&" + OPENSEARCH_URL_SORT;
         if(filter != null && !filter.trim().isEmpty())
         {
            opensearchURI += "&" + OPENSEARCH_URL_FILTER + filter;
         }
         LOGGER.debug("Onda Scanner : open search url : " + opensearchURI);
         HttpClient client = new HttpClient();
         GetMethod method = new GetMethod();
         InputStream rstream = null;
         method.setURI(new URI(opensearchURI + "&" + XML_ITEMS_PER_PAGE_SET +"=" + pageSize, false));
         if (client.executeMethod(method) != HttpStatus.SC_OK)
         {
            LOGGER.error("Error executing opensearch query : '" + opensearchURI + "'");
            return;
         }
         rstream = method.getResponseBodyAsStream();
         Document doc = builder.parse(rstream);
         openSearchResultEntries = parseXML(doc);
         rstream.close();
         processResult(openSearchResultEntries, keystore);
         processed += openSearchResultEntries.size();
         
         if(lastCreationDateUpdated != null)
         {
            LOGGER.info("Onda scanner : lastCreationDate updated to " + lastCreationDateUpdated);
            GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            gc.setTime(Date.from(Instant.parse(lastCreationDateUpdated)));
            ondaConf.getOndaScanner().setLastCreationDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(gc));
            CONFIGURATION_MANAGER.saveConfiguration();
         }
      }
      catch (DateTimeParseException e)
      {
         for (OpenSearchResultEntry opensearchResult : openSearchResultEntries)
         {
            if (lastCreationDateUpdated.contentEquals(opensearchResult.creationDate))
            {
               LOGGER.error(String.format("Error while running ONDA Scanner '%s': %s for Product (UUID/NAME): %s / %s", name, e.getMessage(), opensearchResult.id, opensearchResult.name), e);
               break;
            }
         }
      }
      catch (ParserConfigurationException | SAXException | IOException | DatatypeConfigurationException | ConfigurationException e)
      {
         LOGGER.error("Error while running ONDA Scanner '"+name+"': "+e.getMessage(),e);
      }
   }

   private static List<OpenSearchResultEntry> parseXML(final Document doc)
   {
      List<OpenSearchResultEntry> openSearchResultEntries = new ArrayList<OpenSearchResultEntry>();
      NodeList entries = doc.getElementsByTagName(XML_ENTRY);
      for(int i = 0; i < entries.getLength(); ++i)
      {
         Node entry = entries.item(i);
         OpenSearchResultEntry opensearchResult = new OpenSearchResultEntry();
         NodeList childs = entry.getChildNodes();
         for(int j = 0; j < childs.getLength(); ++j)
         {
            Node child = childs.item(j);
            if(child == null)
            {
               continue;
            }
            final String childName = child.getNodeName();
            if(childName == null)
            {
               continue;
            }
            if(XML_ID.contentEquals(childName))
            {
               opensearchResult.id = child.getTextContent();
               continue;
            }
            if(XML_NAME.contentEquals(childName))
            {
               opensearchResult.name = child.getTextContent();
               continue;
            }
            if(XML_STORAGE.contentEquals(childName))
            {
               opensearchResult.storage = child.getTextContent();
               continue;
            }
            if(XML_CREATION_DATE.contentEquals(childName))
            {
               opensearchResult.creationDate = child.getTextContent();
            }
         }
         openSearchResultEntries.add(opensearchResult);
      }
      return openSearchResultEntries;
   }

   private void processResult(final List<OpenSearchResultEntry> openSearchResultEntries, final PersistentKeyStore keystore)
   {
      for(OpenSearchResultEntry opensearchResult : openSearchResultEntries)
      {
         lastCreationDateUpdated = opensearchResult.creationDate;
         String ref = keystore.get(opensearchResult.id, DataStore.UNALTERED_PRODUCT_TAG);
         if (ref != null)
         {
            LOGGER.debug("Onda Scanner : Product '" + opensearchResult.name + "' already referenced by '"+ref+"'");
            continue;
         }
         LOGGER.debug("Onda Scanner : Processing product '" + opensearchResult.name + "'");
         final Product prod = PRODUCT_SERVICE.systemGetProduct(opensearchResult.id);
         if(prod != null)
         {
            if(prod.isOnline())
            {
               LOGGER.info("Onda Scanner : Referencing '" + opensearchResult.name + "' ("+prod.getUuid()+") to '"+region + ";" + opensearchResult.storage + ";" + opensearchResult.name+"'");
               keystore.put(opensearchResult.id, DataStore.UNALTERED_PRODUCT_TAG, region + ";" + opensearchResult.storage + ";" + opensearchResult.name);
               ++referenced;
            }
         }
      }
   }

   static class OpenSearchResultEntry
   {
      private String id;
      private String name;
      private String storage;
      private String creationDate;
   }

}
