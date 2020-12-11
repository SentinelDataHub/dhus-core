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
package org.dhus.store.datastore.async.pdgs;

import fr.gael.dhus.database.object.Order;
import fr.gael.dhus.util.http.Timeouts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.api.JobStatus;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.StreamableProduct;
import org.dhus.store.datastore.async.AbstractAsyncCachedDataStore;
import org.dhus.store.datastore.async.AsyncDataStoreException;
import org.dhus.store.datastore.async.pdgs.PDGSJob.JSONParseException;
import org.dhus.store.datastore.config.PatternReplace;

/**
 * A store backed by a PDGS.
 */
public class PdgsDataStore extends AbstractAsyncCachedDataStore
{
   /* Log. */
   private static final Logger LOGGER = LogManager.getLogger();

   /* HTTP methods. */
   private static final String HTTP_METHOD_GET = "GET";
   private static final String HTTP_METHOD_DELETE = "DELETE";

   private static final String CONTENT_TYPE_JSON = "application/json";

   private static final String STATUS_COMPLETED = "completed";
   private static final String STATUS_IN_PROGRESS = "in_progress";
   private static final String STATUS_DONE = "done";
   private static final String STATUS_FAILED = "failed";

   private static final String PRODUCTS = "products";
   private static final String JOBS = "jobs";
   private static final String JOB = "job";
   protected static final String SEPARATOR = "/";

   /* To connect to the PDGS Service */
   protected final String urlService;
   private final String login;
   private final String password;

   /* Ingest Job. */
   private final Timer timer;

   private final ParallelProductDownloadManager parallelProductDownloadManager;

   /**
    * Create a PDGS DateStore.
    *
    * @param name                    of this DataStore
    * @param priority                DataStores are ordered
    * @param isManager               true to enable the ingest job in this instance of the DHuS (only one instance per cluster)
    * @param hfsLocation             path to the local HFS cache
    * @param patternReplaceIn        transform PDGS identifiers to DHuS identifiers
    * @param patternReplaceOut       transform DHuS identifiers to PDGS identifiers
    * @param maxPendingRequests      maximum number of pending orders at the same time
    * @param maxRunningRequests      maximum number of running orders at the same time
    * @param maximumSize             maximum size in bytes of the local HFS cache DataStore
    * @param currentSize             overall size of the local HFS cache DataStore (disk usage)
    * @param autoEviction            true to activate auto-eviction based on disk usage on the local HFS cache DataStore
    * @param urlService              URL to connect to the PDGS Service
    * @param login                   user to log to PDGS Service
    * @param password                password to log to PDGS Service
    * @param interval                interval
    * @param maxConcurrentsDownloads maximum number of product download occurring in parallel
    * @param hashAlgorithms          to compute on restore
    * @throws URISyntaxException could not create PDGSDataStore
    * @throws IOException        could not create PDGS repo location directory
    */
public PdgsDataStore(String name, int priority, boolean isManager, String hfsLocation,
      PatternReplace patternReplaceIn, PatternReplace patternReplaceOut, Integer maxPendingRequests, Integer maxRunningRequests,
      long maximumSize, long currentSize, boolean autoEviction, String urlService, String login, String password,
      long interval, int maxConcurrentsDownloads, String[] hashAlgorithms)
            throws URISyntaxException, IOException
{
   super(name, priority, hfsLocation, patternReplaceIn, patternReplaceOut, maxPendingRequests, maxRunningRequests,
         maximumSize, currentSize, autoEviction, hashAlgorithms);

      Objects.requireNonNull(urlService);

      // FIXME these two should be optional
      Objects.requireNonNull(login);
      Objects.requireNonNull(password);

      this.urlService = urlService;
      this.login = login;
      this.password = password;

      LOGGER.info("New PDGS DataStore, name={} url={} hfsLocation={}", getName(), urlService, hfsLocation);

      LOGGER.info("This DHuS instance {} the PDGS manager", isManager ? "is" : "isn't");

      // FIXME the handling of manager/timer shouldn't be duplicated between PdgsDS and GmpDS
      if (isManager)
      {
         timer = new Timer("Run PDGS Job", true);
         timer.schedule(new RunJob(), 0, interval);
      }
      else
      {
         timer = null;
      }

      parallelProductDownloadManager = new ParallelProductDownloadManager(maxConcurrentsDownloads);
   }

   @Override
   public void close() throws Exception
   {
      if (this.timer != null)
      {
         try
         {
            this.timer.cancel();
         }
         catch (RuntimeException suppressed) {}
      }

      this.parallelProductDownloadManager.shutdownNow();
   }

   @Override
   protected Order internalFetch(String localIdentifier, String remoteIdentifier, String uuid, Long size) throws AsyncDataStoreException
   {
      // job not found, create it
      PDGSJob createdpdgsJob = createNewJob(remoteIdentifier, uuid);

      // make sure that local identifier matches renamed PDGS product
      String jobProductRename = getLocalIdentifierFromRemoteProductName(createdpdgsJob.getProductName());
      if (!localIdentifier.equals(jobProductRename))
      {
         throw new AsyncDataStoreException(
               "Cannot match database product " + localIdentifier + " to PDGS product " + createdpdgsJob.getProductName(),
               "Cannot retrieve remote product", 502);
      }

      // create associated order for user feedback
      return new Order(
            getName(),
            uuid,
            createdpdgsJob.getJobId(),
            getJobStatus(createdpdgsJob.getStatusCode()),
            new Date(),
            createdpdgsJob.getEstimatedTime(),
            createdpdgsJob.getStatusMessage());
   }

   protected PDGSJob createNewJob(String remoteIdentifier, String uuid) throws AsyncDataStoreException
   {
      String url = urlService + SEPARATOR + PRODUCTS + SEPARATOR + remoteIdentifier;

      // get job_uri to check the product's availability
      return getJobForProductAt(url);
   }

   private JobStatus getJobStatus(String status)
   {
      if (STATUS_IN_PROGRESS.equals(status))
      {
         return JobStatus.RUNNING;
      }
      else if (STATUS_COMPLETED.equals(status))
      {
         return JobStatus.COMPLETED;
      }
      else
      {
         return JobStatus.FAILED;
      }
   }

   // Get all jobs
   private List<PDGSJob> getAllJobs() throws IOException
   {
      try
      {
         URL url = new URL(urlService + SEPARATOR + JOBS);
         HttpURLConnection connection = openConnection(url, HTTP_METHOD_GET, CONTENT_TYPE_JSON);
         int statusCode = connection.getResponseCode();

         if (statusCode >= 400)
         {
            LOGGER.error("Failed to get all jobs: {}", statusCode);
            throw new IOException();
         }

         String response = connectionResponse(connection, statusCode);
         return PDGSJob.listfromJSON(response);
      }
      catch (IOException | JSONParseException e)
      {
         throw new IOException("Cannot retrieve Jobs", e);
      }
   }

   /**
    * get job_uri from json response to check the product's availability.
    *
    * @param getProductUrl url used to get the response for GET product request
    * @return a non null instance
    * @throws AsyncDataStoreException the job is invalid or not found
    */
   protected PDGSJob getJobForProductAt(String getProductUrl) throws AsyncDataStoreException
   {
      LOGGER.debug("Requesting LTA Broker URL: " + getProductUrl);
      try
      {
         // perform "GET Product" request
         HttpURLConnection prodConnection = openConnection(new URL(getProductUrl), HTTP_METHOD_GET, CONTENT_TYPE_JSON);
         int prodStatusCode = prodConnection.getResponseCode();

         // status for error management
         switch (prodStatusCode)
         {
            case 404:
               throw new AsyncDataStoreException.ProductNotFoundException(
                     "Product not found in LTA Broker: " + prodStatusCode);
            case 429:
               throw new AsyncDataStoreException.TooManyRequestsException(
                     "The quota on LTA Broker side has been reached: " + prodStatusCode);
            case 500:
               throw new AsyncDataStoreException.BadGatewayException(
                     "The LTA Broker service is not available: " + prodStatusCode);
         }

         // body
         String prodResponse = connectionResponse(prodConnection, prodStatusCode);
         PDGSJob pdgsJob = PDGSJob.fromJSON(prodResponse); // this isn't actually a job
         String jobUri = pdgsJob.getJobUri();

         // perform "GET Job" request
         HttpURLConnection jobConnection = openConnection(new URL(jobUri), HTTP_METHOD_GET, CONTENT_TYPE_JSON);

         // status
         int jobStatusCode = jobConnection.getResponseCode();
         switch (jobStatusCode)
         {
            case 404:
               throw new AsyncDataStoreException.ProductNotFoundException(
                     "Job not found in LTA Broker: " + jobStatusCode);
            case 500:
               throw new AsyncDataStoreException.BadGatewayException(
                     "The LTA Broker service is not available: " + jobStatusCode);
         }

         // body
         String jobResponse = connectionResponse(jobConnection, jobStatusCode);
         PDGSJob createdpdgsJob = PDGSJob.fromJSON(jobResponse);
         return createdpdgsJob;
      }
      catch (IOException | JSONParseException e)
      {
         LOGGER.error("Cannot fetch async product from datastore {}", getName(), e);
         throw new AsyncDataStoreException(e.getMessage(), "Cannot fetch offline product", 500);
      }
   }

   /**
    * open client http connection to the PDGS Service and get the json response
    *
    * @param url        to connect to the PDGS Service
    * @param httpMethod use GET or DELETE http methods
    * @return
    * @throws IOException
    */
   private HttpURLConnection openConnection(URL url, String httpMethod, String contentType) throws IOException
   {
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setDoInput(true);
      connection.setRequestMethod(httpMethod);

      if (contentType != null)
      {
         connection.setRequestProperty("Content-Type", contentType);
      }
      connection.setRequestProperty("Accept", "*/*");

      connection.setDoOutput(true);
      connection.setReadTimeout(Timeouts.CONNECTION_TIMEOUT);
      connection.setConnectTimeout(Timeouts.CONNECTION_TIMEOUT);

      String basicAuth = "Basic " + new String(Base64.getEncoder().encode((login + ":" + password).getBytes()));
      connection.setRequestProperty("Authorization", basicAuth);
      return connection;
   }

   private String connectionResponse(HttpURLConnection connection, int statusCode)
         throws IOException
   {
      BufferedReader Reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      StringBuilder response = new StringBuilder();

      if (statusCode == 200 | statusCode == 202)
      {
         String inputLine = Reader.readLine();
         response.append(inputLine);
      }

      return response.toString();
   }

   private void downloadProduct(PDGSJob pdgsJob, String fileName, String uuid) throws IOException
   {
      HttpURLConnection connection;
      InputStream inputStream;
      String downloadUrl = pdgsJob.getProductUrl();

      // normal case
      if (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://"))
      {
         connection = openConnection(new URL(downloadUrl), HTTP_METHOD_GET, null);
         connection.setReadTimeout(Timeouts.CONNECTION_TIMEOUT);
         connection.setConnectTimeout(Timeouts.CONNECTION_TIMEOUT);
         inputStream = connection.getInputStream();
      }
      // special case for special lta brokers
      else if (downloadUrl.startsWith("file://"))
      {
         Path productPath = Paths.get(new URL(downloadUrl).getPath());
         inputStream = Files.newInputStream(productPath);
         connection = null;
      }
      else
      {
         throw new IOException("Unsupported protocol in URL: " + downloadUrl);
      }

      // make simple stream product
      StreamableProduct product = new StreamableProduct(inputStream);

      // don't forget to call that very convenient setter for product name
      product.setName(fileName);

      // put the product in a queue to download it
      parallelProductDownloadManager.submit(product, () ->
      {
         try
         {
            moveProductToCache(product, uuid);

            // refresh database order
            refreshOrder(uuid, getName(), pdgsJob.getJobId(), JobStatus.COMPLETED, pdgsJob.getEstimatedTime(), pdgsJob.getStatusMessage());
         }
         catch (DataStoreException ex)
         {
            // refresh database order
            refreshOrder(uuid, getName(), pdgsJob.getJobId(), JobStatus.FAILED, pdgsJob.getEstimatedTime(), pdgsJob.getStatusMessage());
         }
         finally
         {
            silentlyClose(connection, inputStream);
            deleteJob(pdgsJob);
         }
      });

      LOGGER.info("Product download {} successfully queued", product.getName());
   }

   private void silentlyClose(HttpURLConnection connection, InputStream inputStream)
   {
      if (connection != null)
      {
         connection.disconnect();
      }
      if (inputStream != null)
      {
         try
         {
            inputStream.close();
         }
         catch (IOException suppressed) {}
      }
   }

   private void deleteJob(PDGSJob pdgsJob)
   {
      String jobId = pdgsJob.getJobId();

      HttpURLConnection connection;
      try
      {
         connection = openConnection(
               new URL(urlService + SEPARATOR + JOBS + SEPARATOR + jobId), // /jobs/ID
               HTTP_METHOD_DELETE, CONTENT_TYPE_JSON);

         if (connection.getResponseCode() >= 400)
         {
            LOGGER.warn("Cannot delete job {} ({} {})", jobId, connection.getResponseCode(), connection.getResponseMessage());

            // LTA Broker ICD seems to be wrong? try /job/ID
            LOGGER.info("Trying alternative URL...");
            connection = openConnection(
                  new URL(urlService + SEPARATOR + JOB + SEPARATOR + jobId), // /job/ID
                  HTTP_METHOD_DELETE, CONTENT_TYPE_JSON);

            if (connection.getResponseCode() >= 400)
            {
               LOGGER.warn("Cannot delete job {} ({} {})", jobId, connection.getResponseCode(), connection.getResponseMessage());
               return;
            }
         }

         String jobResponse = connectionResponse(connection, connection.getResponseCode());
         PDGSJob pdgs = PDGSJob.fromJSON(jobResponse);
         if (STATUS_DONE.equals(pdgs.getStatusCode()))
         {
            LOGGER.info("Job {} has been successfully deleted", jobId);
         }
         else if (STATUS_FAILED.equals(pdgs.getStatusCode()))
         {
            LOGGER.error("The deletion of job {} has failed", jobId);
         }
         else
         {
            LOGGER.warn("Unknown status for deletion of job {}: {}", jobId, pdgs.getStatusCode());
         }
      }
      catch (IOException | JSONParseException e)
      {
         LOGGER.error("Failed to delete job {}", pdgsJob.getJobId(), e);
      }
   }

   protected String getLocalIdentifierFromRemoteProductName(String productName) throws AsyncDataStoreException
   {
      return doPatternReplaceIn(productName);
   }

   /**
    * put all completed downloads in the cache DataStore.
    */
   private final class RunJob extends IngestTask
   {
      @Override
      protected int ingestCompletedFetches()
      {
         // check jobs and start restores
         LOGGER.debug("Checking PDGS jobs in {}...", getName());
         int res = 0;
         try
         {
            // Get all jobs
            List<PDGSJob> pdgsJobsList = getAllJobs();
            for (PDGSJob pdgsJob: pdgsJobsList)
            {
               try
               {
                  if (restoreProduct(pdgsJob))
                  {
                     res++;
                  }
               }
               catch (IOException | RuntimeException | JSONParseException | AsyncDataStoreException ex)
               {
                  LOGGER.error("Failed to restore a product in {}", getName(), ex);
               }
            }
            LOGGER.debug("Refreshed information of {} orders in {}", pdgsJobsList.size(), getName());
         }
         catch (IOException | RuntimeException ex) // catch runtime to prevent the timer from crashing
         {
            LOGGER.error("Failed to retrieve new state of Jobs in {}", getName(), ex);
         }

         LOGGER.debug("Started restore of {} products in {}", res, getName());

         // check running downloads
         parallelProductDownloadManager.checkProductDownloads();

         return res;
      }

      private boolean restoreProduct(PDGSJob pdgsJob) throws IOException, JSONParseException, AsyncDataStoreException
      {
         String remoteIdentifier = pdgsJob.getProductName();
         String localIdentifier = getLocalIdentifierFromRemoteProductName(remoteIdentifier);
         String productUUID = getProductUUID(localIdentifier);
         if (productUUID == null)
         {
            LOGGER.error("Cannot match database product {} to PDGS product {}", localIdentifier, remoteIdentifier);
            LOGGER.error("Could not refresh Order for product {} ({} in Job)", localIdentifier, remoteIdentifier);
            return false;
         }

         // start product download of completed jobs
         if (STATUS_COMPLETED.equals(pdgsJob.getStatusCode()))
         {
            // handle product file name
            String productName = localIdentifier;
            // FIXME what if some LTAs send other file formats?
            // make configurable in the future if necessary
            if (!productName.toLowerCase().endsWith(".zip"))
            {
               productName = productName + ".zip";
            }

            // make sure the product is not already in the cache or already queued for download
            if (!existsInCache(productUUID) && !parallelProductDownloadManager.isAlreadyQueued(productName))
            {
               // download product
               downloadProduct(pdgsJob, productName, productUUID);
            }
            else
            {
               LOGGER.warn("Job for product {} ({}) found, but product already downloaded or downloading, skipping",
                     productUUID, localIdentifier);
               return false; // we are not starting the restore. Cannot consider the result as true.
            }

            return true;
         }
         else
         {
            // refresh database order
            refreshOrder(
                  productUUID,
                  getName(),
                  pdgsJob.getJobId(),
                  getJobStatus(pdgsJob.getStatusCode()),
                  pdgsJob.getEstimatedTime(),
                  pdgsJob.getStatusMessage());
            if (STATUS_IN_PROGRESS.equals(pdgsJob.getStatusCode()))
            {
               LOGGER.debug("the requested product {} is being processed", localIdentifier);
            }
            else
            {
               LOGGER.error("product {} retrieval has failed", localIdentifier);
            }
         }
         return false;
      }

   }
}
