/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2020 GAEL Systems
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
package org.dhus.store.datastore.async.gmp;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;

import fr.gael.dhus.database.object.Order;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.api.JobStatus;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.async.AbstractAsyncCachedDataStore;
import org.dhus.store.datastore.config.GmpDataStoreConf;
import org.dhus.store.datastore.config.PatternReplace;
import org.dhus.store.datastore.hfs.HfsProduct;

/**
 * A store backed by GMP.
 * <p>See: https://github.com/gisab/gmp
 */
public final class GmpDataStore extends AbstractAsyncCachedDataStore
{
   /** Log. */
   private static final Logger LOGGER = LogManager.getLogger();

   /** Where GMP downloads and zips products. */
   private final String gmpRepoLocation;

   /** Agent defined in GMP. */
   private final String agentId;

   /** Target defined in GMP. */
   private final String targetId;

   /** Use a connection pool because this DS may have to handle a lot of queries. */
   private final BoneCPDataSource dataSource;

   /** Ingest Job. */
   private final Timer timer;

   /**
    * Creates a GMP data store.
    *
    * @param name                of this store (used by DHuS to identify DataStores)
    * @param priority            DataStores are ordered, this DataStore MUST have the highest priority because it is a yes-store
    * @param isManager           true to enable the ingest job in this instance of the DHuS (only one instance per cluster)
    * @param gmpRepoLocation     path to the repository configured in GMP
    * @param hfsLocation         path to the local HFS cache
    * @param patternReplaceIn    pattern to extract the product identifier from the id column of the queue table of GMP
    * @param patternReplaceOut   pattern to transform the identifier to be used in the queue table of GMP
    * @param maxPendingRequests  maximum number of pending orders at the same time
    * @param maxRunningRequests  maximum number of running orders at the same time
    * @param mySqlURL            URL to connect to the DataBase of GMP
    * @param mySqlUser           user to log into the DataBase of GMP
    * @param mySqlPass           password to log into the DataBase of GMP
    * @param agentId             agent to download products, must exist in table `agent`
    * @param targetId            target to download products, must exist in table `target`
    * @param maximumSize         maximum size in bytes of the local HFS cache DataStore
    * @param currentSize         overall size of the local HFS cache DataStore (disk usage)
    * @param autoEviction        true to activate auto-eviction based on disk usage on the local HFS cache DataStore
    * @param hashAlgorithms      to compute on restore
    *
    * @throws DataStoreException could not create a GMP DataStore
    */
   public GmpDataStore(String name, int priority, boolean isManager, String gmpRepoLocation, String hfsLocation,
         PatternReplace patternReplaceIn, PatternReplace patternReplaceOut, Integer maxPendingRequests, Integer maxRunningRequests,
         String mySqlURL, String mySqlUser, String mySqlPass, String agentId, String targetId, long maximumSize,
         long currentSize, boolean autoEviction, String[] hashAlgorithms)
         throws DataStoreException
   {
      super(name, priority, hfsLocation, patternReplaceIn, patternReplaceOut, maxPendingRequests, maxRunningRequests,
            maximumSize, currentSize, autoEviction, hashAlgorithms);

      Objects.requireNonNull(gmpRepoLocation);
      Objects.requireNonNull(mySqlURL);
      Objects.requireNonNull(mySqlUser);
      Objects.requireNonNull(mySqlPass);
      Objects.requireNonNull(agentId);
      Objects.requireNonNull(targetId);

      this.gmpRepoLocation = gmpRepoLocation;
      this.targetId = targetId;
      this.agentId = agentId;

      LOGGER.info("New GMP DataStore, name={} url={} repo={} hfs={} max_queued_requests={}",
            getName(), mySqlURL, gmpRepoLocation, hfsLocation, maxRunningRequests);

      // If this instance is cluster manager, it is tasked with managing the GMP
      LOGGER.info("This DHuS instance {} the GMP manager", isManager? "is": "isn't");

      // Test SQL connection
      try (Connection conn = DriverManager.getConnection(mySqlURL, mySqlUser, mySqlPass)) {}
      catch (SQLException ex)
      {
         LOGGER.error("Cannot connect to / setup the GMP database '{}'", mySqlURL);
         throw new DataStoreException("Invalid mysqlConnectionUrl for GMP data store", ex);
      }

      // Configure/set up Bone CP
      BoneCPConfig config = new BoneCPConfig();
      config.setJdbcUrl(mySqlURL);
      config.setUser(mySqlUser);
      config.setPassword(mySqlPass);
      config.setMinConnectionsPerPartition(5);
      config.setMaxConnectionsPerPartition(10);
      config.setPartitionCount(1);
      this.dataSource = new BoneCPDataSource(config);

      // FIXME the handling of manager/timer shouldn't be duplicated between PdgsDS and GmpDS
      if (isManager)
      {
         this.timer = new Timer("GMP Ingest Job", true);
         this.timer.schedule(new IngestJob(), 0, 300_000);
      }
      else
      {
         timer = null;
      }
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
      this.dataSource.close();
   }

   /**
    * Factory method, creates a new GmpDatastore from given configuration.
    *
    * @param configuration for this DataStore
    * @param hashAlgorithms to compute on restore
    * @return a new instance of GMP DataStore (never null)
    * @throws DataStoreException could not create a GMP DataStore
    */
   public static GmpDataStore make(GmpDataStoreConf configuration, String[] hashAlgorithms) throws DataStoreException
   {
      // patternReplaceIn default value
      if (configuration.getPatternReplaceIn() == null)
      {
         PatternReplace patternReplaceIn = new PatternReplace();
         patternReplaceIn.setPattern("\\.SAFE$");
         patternReplaceIn.setReplacement("");
         configuration.setPatternReplaceIn(patternReplaceIn);
      }

      // patternReplaceOut default value
      if (configuration.getPatternReplaceOut() == null)
      {
         PatternReplace patternReplaceOut = new PatternReplace();
         patternReplaceOut.setPattern("$");
         patternReplaceOut.setReplacement(".SAFE");
         configuration.setPatternReplaceOut(patternReplaceOut);
      }

      return new GmpDataStore(
            configuration.getName(),
            configuration.getPriority(),
            configuration.isIsMaster(),
            configuration.getRepoLocation(),
            configuration.getHfsLocation(),
            configuration.getPatternReplaceIn(),
            configuration.getPatternReplaceOut(),
            configuration.getMaxPendingRequests(),
            configuration.getMaxRunningRequests(),
            configuration.getMysqlConnectionInfo().getValue(),
            configuration.getMysqlConnectionInfo().getUser(),
            configuration.getMysqlConnectionInfo().getPassword(),
            configuration.getConfiguration().getAgentid(),
            configuration.getConfiguration().getTargetid(),
            configuration.getMaximumSize(),
            configuration.getCurrentSize(), // datastore coming from xml configuration
            configuration.isAutoEviction(),
            hashAlgorithms);
   }

   @Override
   protected Order internalFetch(String localIdentifier, String remoteIdentifier, String uuid, Long size)
         throws DataStoreException
   {
      try (Connection conn = this.dataSource.getConnection())
      {
         // Checks that the requested product isn't queued already
         if (isQueued(conn, remoteIdentifier))
         {
            LOGGER.info("Fetch request from '{}' for product {} ({}) (~{} bytes) already in queue (GMP)",
                  "user" /* FIXME get username from order database object */, localIdentifier, uuid, size);
            return new Order(
                  getName(),
                  uuid,
                  remoteIdentifier,
                  JobStatus.RUNNING,
                  new Date(),
                  null,
                  null);
         }

         // Enqueue product to fetch
         enQueue(conn, remoteIdentifier);

         return new Order(
               getName(),
               uuid,
               remoteIdentifier,
               JobStatus.RUNNING,
               new Date(),
               null,
               null);
      }
      catch (SQLException ex)
      {
         LOGGER.error("Cannot use the database of GMP", ex);
         throw new DataStoreException("Cannot submit fetch request", ex);
      }
   }

   /**
    * Returns {@code true} if given product is queued or completed (if `dwnstatus` == 'Q' OR 'C').
    * @param conn connection to use
    * @param qid Identifier of product to check (with '.SAFE' extension)
    * @return true if `dwnstatus` == 'Q' OR 'C'
    * @throws SQLException in case of connectivity issues
    */
   private boolean isQueued(Connection conn, String qid) throws SQLException
   {
      try (Statement sta = conn.createStatement())
      {
         try (ResultSet rs = sta.executeQuery("SELECT * FROM `queue` WHERE id='" + qid + "'"))
         {
            if (rs.first())
            {
               return true;
            }
         }
      }
      return false;
   }

   /**
    * En-queue (or re-enqueue) downloads.
    * @param conn connection to use
    * @param qid Identifier of product to (re-)enqueue (with '.SAFE' extension)
    * @throws SQLException in case of connectivity issues
    */
   private void enQueue(Connection conn, String qid) throws SQLException
   {
      try (Statement sta = conn.createStatement())
      {
         sta.execute("INSERT INTO `queue` (`status`, `dwnstatus`, `finstatus`, `id`, `targetid`, `agentid`) "
                   + "VALUES ('NEW', 'N', NULL, '" + qid + "', '" + targetId + "', '" + agentId + "')");
      }
   }

   /**
    * De-queue downloads (eg: after storing successful download)
    * @param conn connection to use
    * @param qid Identifier of product to de-queue (with '.SAFE' extension)
    * @throws SQLException in case of connectivity issues
    */
   private void deQueue(Connection conn, String qid) throws SQLException
   {
      try (Statement sta = conn.createStatement())
      {
         sta.execute("DELETE FROM `queue` WHERE `id`='" + qid + "'");
      }
   }

   /** Used by the `queue` gauge. */
   private int gmpQueueSize()
   {
      try (Connection conn = this.dataSource.getConnection())
      {
         try (Statement sta = conn.createStatement())
         {
            try (ResultSet rs = sta.executeQuery("SELECT COUNT(*) FROM `queue` WHERE `dwnstatus`<>'C'"))
            {
               return (rs.first()) ? rs.getInt(1) : 0;
            }
         }
      }
      catch (SQLException suppressed) {}
      return 0;
   }

   /**
    * This Job put all completed downloads in the cache datastore.
    */
   private final class IngestJob extends IngestTask
   {
      // TODO cleanup this method and split into smaller ones
      @Override
      protected int ingestCompletedFetches()
      {
         int res = 0;
         // Select all downloads in queue
         try (Connection conn = dataSource.getConnection();
               Statement sta = conn.createStatement();
               ResultSet rs = sta.executeQuery("SELECT * FROM `queue`"))
         {
            while (rs.next())
            {
               String remoteIdentifier = rs.getString("id");
               String localIdentifier = doPatternReplaceIn(remoteIdentifier);

               String finstatus = rs.getString("finstatus");
               String dwnstatus = rs.getString("dwnstatus");
               String productUUID = getProductUUID(localIdentifier);
               if (productUUID == null)
               {
                  LOGGER.error("Cannot match database product {} to GMP product {}", localIdentifier, remoteIdentifier);
                  continue;
               }

               // Refresh existing order only if not Completed
               if (!dwnstatus.equals("C"))
               {
                  refreshOrder(productUUID, getName(), remoteIdentifier, getStatus(dwnstatus, finstatus), null, null);
               }

               // only execute the following instructions if this instance is manager
               if (finstatus != null && finstatus.equals("NOK")) // check for failures
               {
                  LOGGER.error("Fetch of product {} failed", localIdentifier);
                  enQueue(conn, remoteIdentifier); // SQL constraint error, do not modify, fits the operational scenario
               }
               else if(dwnstatus.equals("C")) // download is completed, proceed with restore
               {
                  Path productPath = getProductFile(localIdentifier);

                  if (productPath == null)
                  {
                     LOGGER.warn("Fetched file for product {} ({}) could not be found in repository", localIdentifier, productUUID);
                     // Just waiting for caronte to zip the product file, not an error, DO NOT deQueue the fetch request
                     continue;
                  }

                  // move product to cache
                  HfsProduct hfsProduct = new HfsProduct(productPath.toFile());
                  try
                  {
                     moveProductToCache(hfsProduct, productUUID);
                     refreshOrder(productUUID, getName(), remoteIdentifier, JobStatus.COMPLETED, null, null);
                  }
                  catch (DataStoreException ex)
                  {
                     refreshOrder(productUUID, getName(), remoteIdentifier, JobStatus.FAILED, null, null);
                  }

                  // cleanup
                  try
                  {
                     Files.deleteIfExists(productPath);
                  }
                  catch (IOException e)
                  {
                     LOGGER.warn("Cannot delete data file ({}) of product {} ({})",
                           productPath, hfsProduct.getName(), productUUID, e);
                  }

                  deQueue(conn, remoteIdentifier);

                  res++;
               }
            }
         }
         catch (SQLException ex)
         {
            LOGGER.error("Cannot use the database of GMP", ex);
         }
         catch (Throwable ex)
         {
            // Catch-all needed because an uncaught exception kills the timer!
            LOGGER.error("Uncaught exception", ex);
         }
         return res;
      }
   }

   /**
    * Finds a file in {@link #gmpRepoLocation} whose name starts with parameter `prefix`.
    *
    * @param prefix prefix
    * @return a Path or null if no file has been found
    */
   private Path getProductFile(final String prefix)
   {
      try
      {
         Path repo = Paths.get(gmpRepoLocation);
         Optional<Path> res =
               Files.find(repo, 1, (path, u) -> path.getFileName().toString().startsWith(prefix))
                    .findFirst();
         return res.isPresent() ? res.get() : null;
      }
      catch (IOException ex)
      {
         LOGGER.error("Could not read repository", ex);
      }
      return null;
   }

   private JobStatus getStatus(String status, String finstatus)
   {
      if (finstatus != null && finstatus.equals("NOK"))
      {
         return JobStatus.FAILED;
      }
      if (status.equals("C"))
      {
         return JobStatus.COMPLETED;
      }
      else
      {
         return JobStatus.RUNNING;
      }
   }
}
