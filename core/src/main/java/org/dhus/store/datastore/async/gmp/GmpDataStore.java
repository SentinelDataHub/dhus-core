/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2019 GAEL Systems
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

import fr.gael.dhus.service.ProductService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dhus.Product;
import org.dhus.ProductConstants;
import org.dhus.store.StoreException;
import org.dhus.store.StoreService;
import org.dhus.store.datastore.DataStore;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.ReadOnlyDataStoreException;
import org.dhus.store.datastore.async.AsyncDataSource;
import org.dhus.store.datastore.async.AsyncProduct;
import org.dhus.store.datastore.config.GmpDataStoreConf;
import org.dhus.store.datastore.hfs.HfsDataStore;
import org.dhus.store.datastore.hfs.HfsManager;
import org.dhus.store.datastore.hfs.HfsProduct;
import org.dhus.store.ingestion.IngestibleProduct;

/**
 * A store backed by GMP.
 * <p>See: https://github.com/gisab/gmp
 */
public final class GmpDataStore implements DataStore, AsyncDataSource
{
   /** Log. */
   private static final Logger LOGGER = LogManager.getLogger();

   /** Data Store identifier. */
   private final String name;

   /** maxQueuedRequest until the DHuS has to refuse a new download using GMP. */
   private final int maxQueuedRequests;

   /** Where GMP downloads and zips products. */
   private final String gmpRepoLocation;

   /** Agent defined in GMP. */
   private final String agentId;

   /** Target defined in GMP. */
   private final String targetId;

   /** To get Identifier from product UUID. */
   private final ProductService productService;

   /** To "restore" fetched products. */
   private final StoreService storeService;

   /** Use a connection pool because this DS may have to handle a lot of queries. */
   private final BoneCPDataSource dataSource;

   /** Local HFS cache. */
   private final HfsDataStore cache;

   /** GMP Ingest Job. */
   private final Timer timer;

   /** Position of this datastore among all other datastores */
   private final int priority;

   /**
    * Creates a GMP data store.
    *
    * @param name              of this store (used by DHuS to identify DataStores)
    * @param priority          DataStores are ordered, this DataStore MUST have the highest priority because it is a yes-store
    * @param isManager         true to enable the ingest job in this instance of the DHuS (only one instance per cluster)
    * @param gmpRepoLocation   path to the repository configured in GMP
    * @param hfsLocation       path to the local HFS cache
    * @param maxQueuedRequests maximum number of queued product at the same time in the `queue` table of GMP
    * @param mySqlURL          URL to connect to the DataBase of GMP
    * @param mySqlUser         user to log into the DataBase of GMP
    * @param mySqlPass         password to log into the DataBase of GMP
    * @param agentId           agent to download products, must exist in table `agent`
    * @param targetId          target to download products, must exist in table `target`
    * @param maximumSize       maximum size in bytes of the local HFS cache DataStore
    * @param currentSize       overall size of the local HFS cache DataStore (disk usage)
    * @param autoEviction      true to activate auto-eviction based on disk usage on the local HFS cache DataStore
    *
    * @throws DataStoreException could not create a GMP DataStore
    */
   public GmpDataStore(String name, int priority, boolean isManager,
         String gmpRepoLocation, String hfsLocation, Integer maxQueuedRequests,
         String mySqlURL, String mySqlUser, String mySqlPass,
         String agentId, String targetId,
         long maximumSize, long currentSize, boolean autoEviction) throws DataStoreException
   {
      Objects.requireNonNull(name);
      Objects.requireNonNull(gmpRepoLocation);
      Objects.requireNonNull(hfsLocation);
      Objects.requireNonNull(name);
      Objects.requireNonNull(mySqlURL);
      Objects.requireNonNull(mySqlUser);
      Objects.requireNonNull(mySqlPass);
      Objects.requireNonNull(agentId);
      Objects.requireNonNull(targetId);

      this.name = name;
      this.priority = priority;
      this.gmpRepoLocation = gmpRepoLocation;
      this.targetId = targetId;
      this.agentId = agentId;

      /*
       * setting the HFS cache's name as this GMPDataStore's name allows the
       * cache to update this GMPDataStore's database entry (currentSize) when
       * products are inserted or evicted on a cache level
       */
      this.cache = new HfsDataStore(
            this.name,
            new HfsManager(hfsLocation, 10,1024),
            false,
            0, // unused
            maximumSize,
            currentSize,
            autoEviction);

      this.maxQueuedRequests = (maxQueuedRequests != null)? maxQueuedRequests: 4;

      LOGGER.info("New GMP DataStore, name={} url={} repo={} hfs={} max_queued_requests={}",
            getName(), mySqlURL, gmpRepoLocation, hfsLocation, maxQueuedRequests);

      // If this instance is cluster manager, it is tasked with managing the GMP
      LOGGER.info("This DHuS instance {} the GMP manager", isManager? "is": "isn't");

      this.productService = ApplicationContextProvider.getBean(ProductService.class);
      this.storeService = ApplicationContextProvider.getBean(StoreService.class);

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

      if (isManager)
      {
         timer = new Timer("GMP Ingest Job", true);
         timer.schedule(new IngestJob(), 0, 300_000);
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
    * @return a new instance of GMP DataStore (never null)
    * @throws DataStoreException could not create a GMP DataStore
    */
   public static GmpDataStore make(GmpDataStoreConf configuration) throws DataStoreException
   {
      return new GmpDataStore(
            configuration.getName(),
            configuration.getPriority(),
            configuration.isIsMaster(),
            configuration.getGmpRepoLocation(),
            configuration.getHfsLocation(),
            configuration.getMaxQueuedRequest(),
            configuration.getMysqlConnectionInfo().getValue(),
            configuration.getMysqlConnectionInfo().getUser(),
            configuration.getMysqlConnectionInfo().getPassword(),
            configuration.getConfiguration().getAgentid(),
            configuration.getConfiguration().getTargetid(),
            configuration.getMaximumSize(),
            configuration.getCurrentSize(), // datastore coming from xml configuration
            configuration.isAutoEviction());
   }

   @Override
   public String getName()
   {
      return this.name;
   }

   @Override
   public boolean hasProduct(String uuid)
   {
      return cache.hasProduct(uuid);
   }

   @Override
   public boolean hasAsyncProduct(String uuid)
   {
      return Boolean.TRUE;
   }

   @Override
   public Product get(String id) throws DataStoreException
   {
      // If is in cache ...
      if (this.cache.hasProduct(id))
      {
         LOGGER.debug("get of cached product {}", id);
         return cache.get(id);
      }
      else
      {
         fr.gael.dhus.database.object.Product prod = productService.getProduct(id);
         String gmp_id = prod.getIdentifier() + ".SAFE";
         // Return AsyncProduct is present in GMP
         LOGGER.debug("get of product {} ({})", id, gmp_id);
         AsyncProduct res = new AsyncProduct(this);
         res.setName(gmp_id);
         res.setProperty(ProductConstants.DATA_SIZE, prod.getSize());
         res.setProperty(ProductConstants.UUID, prod.getUuid());
         return res;
      }
   }

   @Override
   public void fetch(AsyncProduct to_fetch) throws DataStoreException
   {
      String identifier = to_fetch.getName();
      String uuid = (String) to_fetch.getProperty(ProductConstants.UUID);
      String username = (String) to_fetch.getProperty("username"); // Hack to satisfy SD-3153, property `username` is set by the FetchLimiter decorator
      Long   size =   (Long) to_fetch.getProperty(ProductConstants.DATA_SIZE);
      LOGGER.info("fetch request for product {}", identifier);
      try (Connection conn = this.dataSource.getConnection())
      {
         // Checks that the requested product isn't queued already
         if (isQueuedOrCompleted(conn, identifier))
         {
            LOGGER.info("Fetch request from '{}' for product {} ({}) (~{} bytes) already in queue", username, identifier, uuid, size);
            return;
         }

         // Checks that table `queue` is not full
         if (!checkQueueQuota(conn))
         {
            LOGGER.info("Table `queue` is full (more than {} rows)", this.maxQueuedRequests);
            LOGGER.info("Fetch request from '{}' for product {} ({}) (~{} bytes) failed due to max active order per instance reached", username, identifier, uuid, size);
            throw new DataStoreException("The retrieval of offline data is temporary unavailable, please try again later");
         }

         // Enqueue product to fetch
         enQueue(conn, identifier);
      }
      catch (SQLException ex)
      {
         LOGGER.error("Cannot use the database of GMP", ex);
         throw new DataStoreException("Cannot submit fetch request", ex);
      }
      LOGGER.info("Fetch request from '{}' for product {} ({}) (~{} bytes) successfully submitted", username, identifier, uuid, size);
   }

   @Override
   public int getPriority()
   {
      return priority;
   }

   @Override
   public boolean isReadOnly()
   {
      return true;
   }

   /**
    * Removes the '.SAFE' extension (if any).
    * @param identifier product identifier (should ends with '.SAFE')
    * @return identifier without the '.SAFE' extension
    */
   private String unSafe(String identifier)
   {
      if (identifier.endsWith(".SAFE"))
      {
         return identifier.substring(0, identifier.length()-5);
      }
      return identifier;
   }

   /**
    * Returns {@code true} if given product is queued or completed (if `dwnstatus` == 'Q' OR 'C').
    * @param conn connection to use
    * @param qid Identifier of product to check (with '.SAFE' extension)
    * @return true if `dwnstatus` == 'Q' OR 'C'
    * @throws SQLException in case of connectivity issues
    */
   private boolean isQueuedOrCompleted(Connection conn, String qid) throws SQLException
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

   /**
    * Checks if the queue is overquota
    * @param conn connection to use
    * @return true if calling code may queue a fetch
    * @throws SQLException in case of connectivity issues
    */
   private boolean checkQueueQuota(Connection conn) throws SQLException
   {
      try (Statement sta = conn.createStatement())
      {
         // Checks that table `queue` is not full
         try (ResultSet rs = sta.executeQuery("SELECT COUNT(*) FROM `queue` WHERE `dwnstatus`<>'C'"))
         {
            return !rs.first() || rs.getInt(1) < this.maxQueuedRequests;
         }
      }
   }

   // vvvv Not implemented because this DS is read-only vvvv

   @Override
   public boolean canAccess(String resource_location)
   {
      // Is read only, should not be target of synchronisers
      return false;
   }

   @Override
   public void set(String id, Product product) throws DataStoreException
   {
      throw new ReadOnlyDataStoreException("GMP DataStore is read-only");
   }

   @Override
   public void addProduct(IngestibleProduct inProduct) throws StoreException
   {
      throw new ReadOnlyDataStoreException("GMP DataStore is read-only");
   }

   @Override
   public void deleteProduct(String uuid) throws DataStoreException
   {
      cache.deleteProduct(uuid);
   }

   @Override
   public boolean addProductReference(String id, Product product) throws DataStoreException
   {
      return false;
   }

   @Override
   public List<String> getProductList()
   {
      return cache.getProductList();
   }

   @Override
   public List<String> getProductList(int skip, int top)
   {
      return cache.getProductList(skip, top);
   }

   /**
    * This Job put all completed downloads in the cache datastore.
    */
   private final class IngestJob extends TimerTask
   {
      @Override
      public void run()
      {
         // Select all completed downloads
         try (Connection conn = dataSource.getConnection();
               Statement sta = conn.createStatement();
               ResultSet rs = sta.executeQuery("SELECT * FROM `queue` WHERE dwnstatus='C'"))
         {
            while (rs.next())
            {
               String qid = rs.getString("id");
               String finstatus = rs.getString("finstatus");
               if (finstatus != null && finstatus.equals("NOK"))
               {
                  LOGGER.error("Fetch of product {} failed", qid);
                  enQueue(conn, qid);
               }
               else
               {
                  String identifier = unSafe(qid);
                  String uuid = productService.getProducBytIdentifier(identifier).getUuid();
                  Path productPath = getProductFile(identifier);
                  if (productPath == null)
                  {
                     LOGGER.error("Fetched file for product {} ({}) could not be found in repository", identifier, uuid);
                     continue;
                  }
                  LOGGER.info("Moving product {} ({}) from {} to cache", identifier, uuid, productPath);
                  HfsProduct prod  = new HfsProduct(productPath.toFile());
                  try
                  {
                     // FIXME: remove surrounding try-catch block when set() cleans on error as it should do
                     try
                     {
                        cache.set(uuid, prod);
                     }
                     catch (DataStoreException | RuntimeException ex)
                     {
                        cache.deleteProduct(uuid);
                        throw ex;
                     }

                     Files.deleteIfExists(productPath);

                     //Set checksum and content length in database
                     long size = (Long) prod.getProperty(ProductConstants.DATA_SIZE);
                     storeService.restoreProduct(uuid, size, ProductConstants.getChecksums(prod));
                     LOGGER.info("Product {} ({}) ({} bytes) successfully restored", identifier, uuid, size);

                     deQueue(conn, qid);
                  }
                  catch (DataStoreException ex)
                  {
                     LOGGER.error("Cannot move product {} ({}) from {} to cache", identifier, uuid, productPath, ex);
                  }
                  catch (IOException ex)
                  {
                     LOGGER.warn("Cannot delete data file ({}) of product {} ({})", productPath, identifier, uuid, ex);
                  }
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
}
