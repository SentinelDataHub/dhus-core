package fr.gael.dhus.database.liquibase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class CleanUsersForGDPR implements CustomTaskChange
{
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String MD5_PATTERN = "^[a-fA-F0-9]{32}$";
   private static final int BATCH_COUNT = 10_000;

   private static final String UUID = "UUID";
   private static final String PREFERENCES_UUID = "PREFERENCES_UUID";
   private static final String SEARCHES_UUID = "SEARCHES_UUID";
   private static final String LOGIN = "LOGIN";
   private static final String TOTAL = "TOTAL";

   // User
   private static final String USERS_SEL_QUERY = "select UUID,PREFERENCES_UUID,LOGIN from USERS where UUID > ? ORDER BY UUID LIMIT ?";
   private static final String USERS_SEL_COUNT_QUERY = "select count(*) as TOTAL from USERS";

   private static final String USER_ROLES_DEL_QUERY = "delete from USER_ROLES where USER_UUID = ?";
   private static final String USER_RESTRICTIONS_DEL_QUERY = "delete from USER_RESTRICTIONS where USER_UUID = ?";
   private static final String USER_TRANSFORMATIONS_DEL_QUERY = "delete from USER_TRANSFORMATIONS where USER_UUID = ?";
   private static final String USERS_DEL_QUERY = "delete from USERS where UUID = ?";

   private static final String ORDER_OWNERS_DEL_QUERY = "delete from ORDER_OWNERS where USER_UUID = ?";
   private static final String STORE_QUOTAS_DEL_QUERY = "delete from STORE_QUOTAS where USER_UUID = ?";
   private static final String NETWORK_USAGE_DEL_QUERY = "delete from NETWORK_USAGE where USER_UUID = ?";

   // Cart products
   private static final String PRODUCTCARTS_SEL_QUERY = "select UUID from PRODUCTCARTS where USER_UUID = ?";
   private static final String CART_PRODUCTS_DEL_QUERY = "delete from CART_PRODUCTS where CART_UUID = ?";
   private static final String PRODUCTCARTS_DEL_QUERY = "delete from PRODUCTCARTS where USER_UUID = ?";

   // Search Preferences
   private static final String SEARCH_PREFERENCES_SEL_QUERY = "select SEARCHES_UUID from SEARCH_PREFERENCES where PREFERENCE_UUID = ?";
   private static final String SEARCH_ADVANCED_DEL_QUERY = "delete from SEARCH_ADVANCED where SEARCH_UUID = ?";
   private static final String SEARCHES_DEL_QUERY = "delete from SEARCHES where UUID = ?";
   private static final String SEARCH_PREFERENCES_DEL_QUERY = "delete from SEARCH_PREFERENCES where PREFERENCE_UUID = ?";
   private static final String PREFERENCES_DEL_QUERY = "delete from PREFERENCES where UUID = ?";

   private PreparedStatement usersStmt, delUserRolesStmt, delUserRestrictionsStmt, delUserTransformationsStmt, delUserStmt,
         delOrderOwnersStmt, delStoreQuotasStmt, delNetworkUsageStmt, selProductCartsStmt, delCartProductsStmt,
         delProductCartsStmt, selSearchPreferencesStmt, delSearchAdvancedStmt, delSearchesStmt,
         delSearchPreferencesStmt, delPreferencesStmt;
   private boolean delSearchAdvancedStmtExec, delSearchesStmtExec, delCartProductsStmtExec;
   private int batchCount = 0;
   private int totalUsers = 0;

   @Override
   public String getConfirmationMessage()
   {
      return "Users and all their data are purged for GDPR use context";
   }

   @Override
   public void setUp() throws SetupException
   {
   }

   @Override
   public void setFileOpener(ResourceAccessor resourceAccessor)
   {
   }

   @Override
   public ValidationErrors validate(Database database)
   {
      return null;
   }

   @Override
   public void execute(Database database) throws CustomChangeException
   {
      JdbcConnection con = (JdbcConnection) database.getConnection();
      LOGGER.info("Start cleaning users for GDPR...");
      boolean statementsInit = false;
      try (final PreparedStatement usersCountStmt = con.prepareStatement(USERS_SEL_COUNT_QUERY))
      {
         final ResultSet rsUsersCount = usersCountStmt.executeQuery();
         if(rsUsersCount.next())
         {
            totalUsers = rsUsersCount.getInt(TOTAL);
         }
         rsUsersCount.close();
         if(totalUsers <= 0)
         {
            LOGGER.info("No users found in the database, Exit.");
            return;
         }
         usersStmt = con.prepareStatement(USERS_SEL_QUERY);
         delUserRolesStmt = con.prepareStatement(USER_ROLES_DEL_QUERY);
         delUserRestrictionsStmt = con.prepareStatement(USER_RESTRICTIONS_DEL_QUERY);
         delUserTransformationsStmt = con.prepareStatement(USER_TRANSFORMATIONS_DEL_QUERY);
         delUserStmt = con.prepareStatement(USERS_DEL_QUERY);

         delOrderOwnersStmt = con.prepareStatement(ORDER_OWNERS_DEL_QUERY);
         delStoreQuotasStmt = con.prepareStatement(STORE_QUOTAS_DEL_QUERY);
         delNetworkUsageStmt = con.prepareStatement(NETWORK_USAGE_DEL_QUERY);

         selProductCartsStmt = con.prepareStatement(PRODUCTCARTS_SEL_QUERY);
         delCartProductsStmt = con.prepareStatement(CART_PRODUCTS_DEL_QUERY);
         delProductCartsStmt = con.prepareStatement(PRODUCTCARTS_DEL_QUERY);

         selSearchPreferencesStmt = con.prepareStatement(SEARCH_PREFERENCES_SEL_QUERY);
         delSearchAdvancedStmt = con.prepareStatement(SEARCH_ADVANCED_DEL_QUERY);
         delSearchesStmt = con.prepareStatement(SEARCHES_DEL_QUERY);
         delSearchPreferencesStmt = con.prepareStatement(SEARCH_PREFERENCES_DEL_QUERY);
         delPreferencesStmt = con.prepareStatement(PREFERENCES_DEL_QUERY);
         statementsInit = true;
         boolean loop = true;
         String userId = "0";
         while(loop)
         {
            usersStmt.setString(1, userId);
            usersStmt.setInt(2, BATCH_COUNT);
            delSearchAdvancedStmtExec = delSearchesStmtExec = delCartProductsStmtExec = false;

            String prefId, login;
            ResultSet rsSearchPreferences, rsProductCarts;
            // USERS
            final ResultSet rsUsers = usersStmt.executeQuery();
            int counter = 0;
            int lastBatchCount = batchCount;
            while (rsUsers.next())
            {
               counter++;
               userId = rsUsers.getString(UUID);
               prefId = rsUsers.getString(PREFERENCES_UUID);
               login = rsUsers.getString(LOGIN);

               // if MD5 login then it is in GDPR format, go next, no processing needed
               if(login.matches(MD5_PATTERN))
               {
                  continue;
               }

               // SEARCHES
               selSearchPreferencesStmt.setString(1, prefId);
               rsSearchPreferences = selSearchPreferencesStmt.executeQuery();
               while(rsSearchPreferences.next())
               {
                  final String searchId = rsSearchPreferences.getString(SEARCHES_UUID);
                  delSearchAdvancedStmt.setString(1, searchId);
                  delSearchAdvancedStmt.addBatch();
                  delSearchAdvancedStmtExec = true;
                  delSearchesStmt.setString(1, searchId);
                  delSearchesStmt.addBatch();
                  delSearchesStmtExec = true;
               }

               delSearchPreferencesStmt.setString(1, prefId);
               delSearchPreferencesStmt.addBatch();

               delPreferencesStmt.setString(1, prefId);
               delPreferencesStmt.addBatch();

               // PRODUCT CARTS
               selProductCartsStmt.setString(1, userId);
               rsProductCarts = selProductCartsStmt.executeQuery();
               while(rsProductCarts.next())
               {
                  final String productCartId = rsProductCarts.getString(UUID);
                  delCartProductsStmt.setString(1, productCartId);
                  delCartProductsStmt.addBatch();
                  delCartProductsStmtExec = true;
               }

               delProductCartsStmt.setString(1, userId);
               delProductCartsStmt.addBatch();

               // USER ROLES
               delUserRolesStmt.setString(1, userId);
               delUserRolesStmt.addBatch();

               // USER RESTRICTIONS
               delUserRestrictionsStmt.setString(1, userId);
               delUserRestrictionsStmt.addBatch();

               // USER TRANSFORMATIONS
               delUserTransformationsStmt.setString(1, userId);
               delUserTransformationsStmt.addBatch();

               // ORDER OWNERS
               delOrderOwnersStmt.setString(1, userId);
               delOrderOwnersStmt.addBatch();

               // STORE QUOTAS
               delStoreQuotasStmt.setString(1, userId);
               delStoreQuotasStmt.addBatch();

               // NETWORK USAGE
               delNetworkUsageStmt.setString(1, userId);
               delNetworkUsageStmt.addBatch();

               // --- USER ---
               delUserStmt.setString(1, userId);
               delUserStmt.addBatch();

               batchCount++;
            }
            if(lastBatchCount < batchCount)
            {
               executeBatch();
               con.commit();
            }
            loop = counter == BATCH_COUNT;
         }
      }
      catch (DatabaseException | SQLException e)
      {
          throw new CustomChangeException(e);
      }
      finally
      {
         if(statementsInit)
         {
            closeStatements();
         }
      }
      LOGGER.info("End cleaning users for GDPR : deleted={} / total={}", batchCount, totalUsers);
   }

   private void executeBatch() throws SQLException
   {
      // DEL BATCH_COUNT x USER DATA
      if (delSearchAdvancedStmtExec)
      {
         delSearchAdvancedStmt.executeBatch();
      }
      delSearchPreferencesStmt.executeBatch();
      if (delSearchesStmtExec)
      {
         delSearchesStmt.executeBatch();
      }
      if (delCartProductsStmtExec)
      {
         delCartProductsStmt.executeBatch();
      }
      delProductCartsStmt.executeBatch();

      delUserRolesStmt.executeBatch();
      delUserRestrictionsStmt.executeBatch();
      delUserTransformationsStmt.executeBatch();
      delOrderOwnersStmt.executeBatch();
      delStoreQuotasStmt.executeBatch();
      delNetworkUsageStmt.executeBatch();

      // DEL BATCH_COUNT x USER
      delUserStmt.executeBatch();
      delPreferencesStmt.executeBatch();
      LOGGER.info("{} Users deleted from database (GDPR) / Total Users : {}", batchCount, totalUsers);
      // DEL USER

      // CLEAN
      delSearchAdvancedStmtExec = delSearchesStmtExec = delCartProductsStmtExec = false;
      delUserRolesStmt.clearBatch();
      delUserRestrictionsStmt.clearBatch();
      delUserTransformationsStmt.clearBatch();
      delOrderOwnersStmt.clearBatch();
      delStoreQuotasStmt.clearBatch();
      delNetworkUsageStmt.clearBatch();
      delUserStmt.clearBatch();
      delSearchAdvancedStmt.clearBatch();
      delSearchPreferencesStmt.clearBatch();
      delSearchesStmt.clearBatch();
      delPreferencesStmt.clearBatch();
      delCartProductsStmt.clearBatch();
      delProductCartsStmt.clearBatch();
   }

   private void closeStatements()
   {
      try
      {
         usersStmt.close();
         selProductCartsStmt.close();
         selSearchPreferencesStmt.close();
         delUserRolesStmt.close();
         delUserRestrictionsStmt.close();
         delUserTransformationsStmt.close();
         delOrderOwnersStmt.close();
         delStoreQuotasStmt.close();
         delNetworkUsageStmt.close();
         delUserStmt.close();
         delSearchAdvancedStmt.close();
         delSearchPreferencesStmt.close();
         delSearchesStmt.close();
         delPreferencesStmt.close();
         delCartProductsStmt.close();
         delProductCartsStmt.close();
      }
      catch (SQLException e)
      {
         LOGGER.error("Error closing the prepared statements after deletion.", e);
      }
   }
}
