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
package org.dhus.transformation;

import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.database.object.Transformation;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.database.object.config.system.TransformationConfiguration;
import fr.gael.dhus.database.object.config.system.TransformerQuota;
import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.service.SecurityService;
import fr.gael.dhus.service.StoreQuotaService;
import fr.gael.dhus.service.TransfoParameterService;
import fr.gael.dhus.service.TransformationService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.ProductConstants;
import org.dhus.api.JobStatus;
import org.dhus.api.transformation.ProductInfo;
import org.dhus.api.transformation.TransformationException;
import org.dhus.api.transformation.TransformationStatus;
import org.dhus.api.transformation.Transformer;
import org.dhus.store.ParallelProductSetter;
import org.dhus.store.StoreService;
import org.dhus.store.datastore.ProductNotFoundException;
import org.dhus.store.ingestion.IngestibleRawProduct;

public class TransformationManager
{

   private static final Logger LOGGER = LogManager.getLogger();

   private final Map<String, Transformer> transformerMap = new HashMap<>();

   public  ProductService productService;
   private TransformationService transformationService;
   private TransfoParameterService transfoParameterService;
   private SecurityService securityService;
   private TransformationConfiguration transformationConfiguration;

   // quotas
   private static final String FIRST_NAME = "TRANSFORMATION";
   private static final String LAST_NAME = "RUNNING_TRANSFORMATION";
   private StoreQuotaService storeQuotaService;

   // ingestion
   private Timer ingestionTimer;
   private StoreService storeService;
   private ParallelProductSetter productSetter;
   private List<IngestTask> runningIngestions;

   /* Loads transformers defined into the classpath. */
   private void loadTransformers()
   {
      Iterator<Transformer> it = ServiceLoader.load(Transformer.class).iterator();
      while (it.hasNext())
      {
         Transformer transformer = it.next();
         String name = transformer.getName();
         if (transformerMap.containsKey(name))
         {
            LOGGER.warn("Loading transformer {} skipped: transformer name is already used", name);
         }
         else
         {
            transformerMap.put(name, transformer);
         }
      }
   }

   /* Generate a ProductInfo associated to the given product uuid. */
   private ProductInfo generateProductInfo(String uuid) throws TransformationException
   {
      Product product = productService.systemGetProduct(uuid);
      if (product == null)
      {
         throw new TransformationException("Product not found: " + uuid);
      }
      // enforce metadata loading
      product.setIndexes(productService.getIndexes(product.getUuid()));
      return new DHuSProductInfo(product);
   }

   /* Computes hash of all transformation parameters. */
   private int computeHashParameters(Map<String, Object> parameters)
   {
      return parameters.entrySet()
            .stream()
            .mapToInt(entry -> (entry.getKey().hashCode() + entry.getValue().hashCode()))
            .reduce(0, (a, b) -> a + 31 * b);
   }

   /** This bean's init-method. */
   public void init()
   {
      loadTransformers();

      if (transformationConfiguration.isIsMaster())
      {
         LOGGER.debug("Transfo: Instance is master, starting Ingestion thread pool and TransfoTask");
         int corePoolSize = transformationConfiguration.getIngestCorePoolSize();
         productSetter = new ParallelProductSetter(
               "transformation-ingestion",
               corePoolSize, corePoolSize,
               0, TimeUnit.SECONDS);
         runningIngestions = new ArrayList<>();

         ingestionTimer = new Timer("Ingest Transformations");
         ingestionTimer.schedule(new TransfoJob(), 0, transformationConfiguration.getInterval());
      }
      else
      {
         ingestionTimer = null;
      }
   }

   /** This bean's destroy-method. */
   public void cleanup()
   {
      if (transformationConfiguration.isIsMaster())
      {
          LOGGER.debug("Transfo: Instance is master, canceling TransfoTask and shuting down Ingestion thread pool");
          ingestionTimer.cancel();
          // Has a 1 hour timeout, gives time for each task to gracefully terminate: either finish ingestion
          // or properly revert all successful insertion in Stores / MetadataStores.
          productSetter.shutdownBlockingIO();

          // make sure finished ingests are properly handled
          checkRunningIngests();
      }
   }

   /**
    * Returns all loaded transformers.
    *
    * @return a non null list of all Transformers
    */
   public List<Transformer> getTransformers()
   {
      return new ArrayList<>(transformerMap.values());
   }

   /**
    * Retrieves a transformer by its name.
    *
    * @param name transformer name
    * @return a transformer if it found, otherwise null
    */
   public Transformer getTransformer(String name)
   {
      return transformerMap.get(name);
   }

   /**
    * Performs a transformation in background.
    *
    * @param transformerName transformer name to apply
    * @param productUuid     product UUID to transform
    * @param parameters      transformation parameters
    * @return a monitorable execution of the transformation
    * @throws TransformationException  Could not perform transformation
    * @throws ProductNotFoundException
    */
   public Transformation transform(String transformerName, String productUuid, Map<String, Object> parameters)
         throws TransformationException, ProductNotFoundException
   {
      LOGGER.debug("Transformation request '{}' (product {})", transformerName, productUuid);

      User currentUser = securityService.getCurrentUser();
      Product product = productService.systemGetProduct(productUuid);

      if (product == null)
      {
         // failure logged by caller
         throw new ProductNotFoundException();
      }

      // compute hash and check if already launched
      int hash = computeHashParameters(parameters);
      String transfoUuid = UUID.randomUUID().toString();
      Transformation transformation = transformationService.findTransformation(transformerName, productUuid, hash);
      if (transformation != null)
      {
         LOGGER.info("Transformation ({}) already exists for request from '{}' on product {} ({}) (~{} bytes) with transformer '{}'",
               transformation.getUuid(), currentUser.getUsername(), product.getIdentifier(), product.getUuid(), product.getSize(), transformerName);

         if (transformation.getStatus().equals(JobStatus.FAILED.toString()))
         {
            LOGGER.info("Found existing {} transformation: '{}' - Deleting it to restart transformation",
                  transformation.getStatus(), transformation.getUuid());
            transfoUuid = transformation.getUuid();
            deleteTransformation(transformation.getUuid());
         }
         else
         {
            LOGGER.debug("Found existing {} transformation: '{}'", transformation.getStatus(), transformation.getUuid());
            String productOut = transformation.getProductOut();
            if (productOut != null)
            {
               productService.updateModificationDate(productOut);
            }
            return transformation;
         }
      }

      // check service quotas
      // i.e. you cannot have more than X running transformations per transformer
      int maxPending = getMaxPending(transformerName);
      if (transformationService.countPendingTransformations() >= maxPending)
      {
         LOGGER.info("Rejected transformation request from '{}' on transformer '{}', service quota exceeded ({} max)",
               currentUser.getUsername(), transformerName, maxPending);

         // TODO better error message, handle in datahandlers
         throw new TransformationException("Too many pending transformations");
      }

      // get transformer and product ingo
      Transformer transformer = getTransformer(transformerName);
      ProductInfo productInfo = generateProductInfo(productUuid);

      // check if the product is transformable by the selected transformer
      // throws a TransformationException if not
      transformer.isTransformable(productInfo,
            parameters.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString())));

      // check user quotas
      String username = currentUser.getUsername();
      UUID userUuid = UUID.fromString(currentUser.getUUID());

      // throws exception if quota exceeded
      checkUserQuotas(userUuid, username, transformerName);

      // create transformation object
      LOGGER.debug("Assigning UUID '{}' to transformation '{}' (product {})", transfoUuid, transformerName, productUuid);
      transformation = new Transformation();
      transformation.setUuid(transfoUuid);
      transformation.setTransformer(transformerName);
      transformation.setProductIn(productUuid);
      transformation.setHash(hash);
      transformation.setStatus(JobStatus.PENDING.toString());

      // create user quota
      createQuotaEntry(userUuid, transfoUuid);

      // create transformation database object
      Transformation pendingTransfo = transformationService.create(transformation);

      // add owner
      transformationService.addUser(pendingTransfo, currentUser);

      if (transformationService.countPendingTransformations() == 1)
      {
         // no other transformation is currently pending, start right away as demanded
         tryRunTransformation(pendingTransfo);
      }

      // return possibly updated transfo object
      Transformation finalTransformation = transformationService.getTransformation(pendingTransfo.getUuid());
      LOGGER.info("Transformation ({}) successfully submitted for request from '{}' on product {} ({}) (~{} bytes) with transformer '{}'",
            transformation.getUuid(), currentUser.getUsername(), product.getIdentifier(), product.getUuid(), product.getSize(), transformerName);

      return finalTransformation;
   }

   public void deleteTransformation(String transformationUuid)
   {
      // database object
      transformationService.deleteTransformationByUuid(transformationUuid);

      // quota
      deleteQuotaEntry(transformationUuid);

      // notify transformer(s)
      transformerMap.forEach((name, transformer) -> {
         transformer.terminateTransformation(transformationUuid);
         LOGGER.debug("Terminated transformation '{}' in transformer {}", transformationUuid, name);
      });
   }

   private void checkUserQuotas(UUID userUuid, String username, String transformerName) throws TransformationException
   {
      if (transformationConfiguration.getUserQuotas() == null)
      {
         // no quotas
         return;
      }

      LOGGER.debug("Checking transformation quotas for user: {}", username);
      if (storeQuotaService.countQuotaEntries(FIRST_NAME, LAST_NAME, userUuid)
            >= transformationConfiguration.getUserQuotas().getMaxParallelTransformationsPerUser())
      {
         LOGGER.info("Rejected transformation request from '{}' on transformer '{}', user quota exceeded ({} max)",
               username, transformerName, transformationConfiguration.getUserQuotas().getMaxParallelTransformationsPerUser());
         throw new TransformationQuotasException("Too many parallel transformations for user: " + username);
      }
   }

   private void createQuotaEntry(UUID userUuid, String transfoUuid)
   {
      if (transformationConfiguration.getUserQuotas() == null)
      {
         // no quotas
         return;
      }

      storeQuotaService.insertQuotaEntry(FIRST_NAME, LAST_NAME, userUuid, transfoUuid, System.currentTimeMillis());
   }

   private void deleteQuotaEntry(String transfoUuid)
   {
      if (transformationConfiguration.getUserQuotas() == null)
      {
         // no quotas
         return;
      }

      storeQuotaService.deleteQuotaEntry(FIRST_NAME, LAST_NAME, transfoUuid);
   }

   private void failTransformation(String transfoUuid)
   {
      transformationService.onTransformationFailure(transfoUuid);

      // quota
      deleteQuotaEntry(transfoUuid);
   }

   private int getMaxPending(String transformerName)
   {
      return transformationConfiguration.getTransformerQuotas().stream()
            .filter(quota -> transformerName.equals(quota.getTransformerName()))
            .findAny().orElse(new TransformerQuota()).getMaxPendingRequests();
   }

   private int getMaxRunning(String transformerName)
   {
      return transformationConfiguration.getTransformerQuotas().stream()
            .filter(quota -> transformerName.equals(quota.getTransformerName()))
            .findAny().orElse(new TransformerQuota()).getMaxRunningRequests();
   }

   private void handlePendingTransformations()
   {
      // get all pending transformations
      List<Transformation> transfos = transformationService.getTransformationsByStatus(JobStatus.PENDING.toString());
      LOGGER.debug("Found {} pending transformations", transfos.size());

      // process pending transformations
      for (Transformation transfo: transfos)
      {
         if (JobStatus.PENDING.equals(tryRunTransformation(transfo)))
         {
            // this means max running has been reached, stop
            break;
         }
      }
   }

   /**
    * Attempts to start the given Transformation is there is room in the "running" queue.
    * Returns the resulting JobStatus.
    *
    * @param transfo to run
    * @return the resulting JobStatus of the Transformation
    */
   private synchronized JobStatus tryRunTransformation(Transformation transfo)
   {
      int maxRunning = getMaxRunning(transfo.getTransformer());
      if (transformationService.countRunningTransformations() >= maxRunning)
      {
         LOGGER.debug("Max running transformation limit reached");
         return JobStatus.PENDING;
      }

      try
      {
         // get transformer
         Transformer transformer = getTransformer(transfo.getTransformer());

         // get product to transformer
         ProductInfo productInfo = generateProductInfo(transfo.getProductIn());

         // get parameters
         Map<String, String> parameters = getTransfoParameterService().getParametersFromTransformation(transfo.getUuid());

         // submit transformation process
         TransformationStatus status = transformer.submitTransformation(transfo.getUuid(), productInfo, parameters);

         // report status
         transformationService.updateStatus(
               transfo.getUuid(),
               status.toJobStatus().toString(),
               null, // result url
               null,  // output product
               status.getData());

         LOGGER.debug("Submitted transformation of UUID '{}'", transfo.getUuid());

         return status.toJobStatus();
      }
      catch (TransformationException e)
      {
         LOGGER.error("Cannot start Transformation '{}'", transfo.getUuid(), e);
         failTransformation(transfo.getUuid());

         return JobStatus.FAILED;
      }
   }

   private void handleRunningTransformations()
   {
      // get all running transformations
      List<Transformation> transfos = transformationService.getTransformationsByStatus(JobStatus.RUNNING.toString());
      LOGGER.debug("Found {} running transformations", transfos.size());

      // process running transformations
      for (Transformation transfo: transfos)
      {
         try
         {
            // get transformer
            Transformer transformer = getTransformer(transfo.getTransformer());

            // check transformation status
            TransformationStatus status = transformer.getTransformationStatus(transfo.getUuid(), transfo.getData());

            if (JobStatus.FAILED.equals(status.toJobStatus()))
            {
               LOGGER.info("Transformation '{}' execution failed", transfo.getUuid());
               failTransformation(transfo.getUuid());
            }
            else
            {
               // report status
               transformationService.updateStatus(
                     transfo.getUuid(),
                     status.toJobStatus().toString(),
                     status.getResult() == null ? null : status.getResult().toString(),
                     null, // output product
                     status.getData());
               LOGGER.debug("Updated transformation of UUID '{}'", transfo.getUuid());
            }
         }
         catch (TransformationException e)
         {
            LOGGER.error("Cannot update status of Transformation '{}'", transfo.getUuid(), e);
            failTransformation(transfo.getUuid());
         }
      }
   }

   private void checkLostIngests()
   {
      LOGGER.debug("Checking potential lost ingestions...");

      // get list of all ingesting transformations
      List<Transformation> transfos = transformationService.getTransformationsByStatus(TransformationStatusUtil.INGESTING);

      // get transformation uuids list of active ingest tasks
      Set<String> ingestingTransfoUuids = runningIngestions.stream().map(ingestionTask -> ingestionTask.transformationUuid).collect(Collectors.toSet());

      for (Transformation transfo: transfos)
      {
         if (!ingestingTransfoUuids.contains(transfo.getUuid()))
         {
            LOGGER.warn("Ingestion for Transformation '{}' has been lost and will be re-attempted soon", transfo.getUuid());

            // this means there is no active ingest task for this transformation despite
            // its INGESTING status in the database, the ingest may have been interrupted
            // by a shutdown
            // set this transformation as COMPLETED so that the ingest is attempted again
            transformationService.updateStatus(
                  transfo.getUuid(),
                  TransformationStatusUtil.COMPLETED,
                  transfo.getResultUrl(),
                  null, // output product
                  transfo.getData());
         }
      }
   }

   private void handleCompletedTransformations()
   {
      // get all completed transformations
      List<Transformation> transfos = transformationService.getTransformationsByStatus(JobStatus.COMPLETED.toString());
      LOGGER.debug("Found {} completed transformations", transfos.size());

      // process completed transformations
      for (Transformation transfo: transfos)
      {
         try
         {
            URL resultUrl = new URL(transfo.getResultUrl());

            // make ingestible product
            IngestibleRawProduct product = IngestibleRawProduct.fromURL(resultUrl, true);

            // submit ingestion
            Future<?> ingest = productSetter.submitProduct(storeService, product,
                  transformationConfiguration.getTargetDataStore(), Collections.emptyList(), true);

            // track ingestion
            runningIngestions.add(new IngestTask(transfo.getUuid(), product, ingest));

            LOGGER.debug("Transformation '{}' complete, submitting ingest of result product (uuid: '{}', identifier: '{}')",
                  transfo.getUuid(), product.getUuid(), product.getIdentifier());

            // update status
            transformationService.updateStatus(
                  transfo.getUuid(),
                  TransformationStatusUtil.INGESTING,
                  transfo.getResultUrl(),
                  null, // output product
                  transfo.getData());
         }
         catch (MalformedURLException e)
         {
            LOGGER.error("Transformation '{}' is broken: invalid URL '{}'", transfo.getUuid(), transfo.getResultUrl(), e);
            failTransformation(transfo.getUuid());
         }
      }
   }

   private void checkRunningIngests()
   {
      // check running ingestions
      Iterator<IngestTask> ingestIterator = runningIngestions.iterator();
      LOGGER.debug("Checking {} running ingestions", runningIngestions.size());
      while (ingestIterator.hasNext())
      {
         IngestTask ingestTask = ingestIterator.next();
         if (ingestTask.isDone())
         {
            try
            {
               ingestTask.getTask().get();

               // update status in db
               transformationService.updateStatus(
                     ingestTask.getTransformationId(),
                     TransformationStatusUtil.INGESTED,
                     null, // result url, no longer needed
                     ingestTask.getProduct().getUuid(),
                     null); // no longer needed

               // quota release
               deleteQuotaEntry(ingestTask.getTransformationId());

               // transformer release
               Transformation transformation = transformationService.getTransformation(ingestTask.getTransformationId());
               if (transformation != null)
               {
                  Transformer transformer = transformerMap.get(transformation.getTransformer());
                  if (transformer != null)
                  {
                     transformer.terminateTransformation(transformation.getUuid());
                  }
               }

               IngestibleRawProduct product = ingestTask.getProduct();
               LOGGER.info("Transformed product {} ('{}') (~{} bytes) (from transformation '{}') successfully ingested in {}ms",
                     product.getIdentifier(),
                     product.getUuid(),
                     product.getProperty(ProductConstants.DATA_SIZE),
                     ingestTask.transformationUuid,
                     product.getIngestionTimeMillis());
            }
            catch (InterruptedException | ExecutionException e)
            {
               // update status in db
               failTransformation(ingestTask.getTransformationId());

               LOGGER.error("Transformed product '{}' (from transformation '{}') ingestion failed",
                     ingestTask.getProduct().getUuid(), ingestTask.transformationUuid, e);
            }

            // remove from running ingests list
            ingestIterator.remove();
         }
      }
      LOGGER.debug("TransformationManager: {} ingestions running", runningIngestions.size());
   }

   private final class TransfoJob extends TimerTask
   {
      @Override
      public void run()
      {
         try
         {
            // check pending transformations and start their execution
            handlePendingTransformations();

            // check running transformations and report their status
            handleRunningTransformations();

            // check ingests that would have been lost during a shutdown
            // this will set affected transformations to COMPLTED
            checkLostIngests();

            // check completed transformations and queue ingest tasks
            handleCompletedTransformations();

            // check running ingests and update transformation statuses
            checkRunningIngests();
         }
         catch (Exception e)
         {
            LOGGER.error("Unexpected exception during TransformationManager routine", e);
         }
      }
   }

   private static final class IngestTask
   {
      private final String transformationUuid;
      private final IngestibleRawProduct product;
      private final Future<?> task;

      public IngestTask(String transformationId, IngestibleRawProduct product, Future<?> task)
      {
         this.transformationUuid = transformationId;
         this.product = product;
         this.task = task;
      }

      public boolean isDone()
      {
         return task.isDone();
      }

      @Override
      public boolean equals(Object obj)
      {
         if (obj == null || !IngestTask.class.isAssignableFrom(obj.getClass()))
         {
            return false;
         }
         IngestTask other = IngestTask.class.cast(obj);
         return other.transformationUuid.equals(this.transformationUuid);
      }

      @Override
      public int hashCode()
      {
         return this.transformationUuid.hashCode();
      }

      public Future<?> getTask()
      {
         return task;
      }

      public String getTransformationId()
      {
         return transformationUuid;
      }

      public IngestibleRawProduct getProduct()
      {
         return product;
      }
   }

   /* | Getters and setters for configuration | */
   /* |                                       | */
   /* V                                       V */

   public StoreService getStoreService()
   {
      return storeService;
   }

   public void setStoreService(StoreService storeService)
   {
      this.storeService = storeService;
   }

   public ProductService getProductService()
   {
      return productService;
   }

   public void setProductService(ProductService productService)
   {
      this.productService = productService;
   }

   public TransformationService getTransformationService()
   {
      return transformationService;
   }

   public void setTransformationService(TransformationService transformationService)
   {
      this.transformationService = transformationService;
   }

   public SecurityService getSecurityService()
   {
      return securityService;
   }

   public void setSecurityService(SecurityService securityService)
   {
      this.securityService = securityService;
   }

   public TransformationConfiguration getTransformationConfiguration()
   {
      return transformationConfiguration;
   }

   public void setTransformationConfiguration(TransformationConfiguration transformationConfiguration)
   {
      this.transformationConfiguration = transformationConfiguration;
   }

   public TransfoParameterService getTransfoParameterService()
   {
      return transfoParameterService;
   }

   public void setTransfoParameterService(TransfoParameterService transfoParameterService)
   {
      this.transfoParameterService = transfoParameterService;
   }

   public StoreQuotaService getStoreQuotaService()
   {
      return storeQuotaService;
   }

   public void setStoreQuotaService(StoreQuotaService storeQuotaService)
   {
      this.storeQuotaService = storeQuotaService;
   }
}
