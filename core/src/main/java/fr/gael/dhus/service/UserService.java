/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013-2019 GAEL Systems
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
package fr.gael.dhus.service;

import fr.gael.dhus.database.dao.AccessRestrictionDao;
import fr.gael.dhus.database.dao.CountryDao;
import fr.gael.dhus.database.dao.SearchDao;
import fr.gael.dhus.database.dao.UserDao;
import fr.gael.dhus.database.object.Country;
import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.Search;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.database.object.User.PasswordEncryption;
import fr.gael.dhus.database.object.config.system.AdministratorConfiguration;
import fr.gael.dhus.database.object.restriction.AccessRestriction;
import fr.gael.dhus.database.object.restriction.LockedAccessRestriction;
import fr.gael.dhus.messaging.mail.MailServer;
import fr.gael.dhus.network.CurrentQuotas;
import fr.gael.dhus.olingo.v1.visitor.UserSQLVisitor;
import fr.gael.dhus.service.exception.EmailNotSentException;
import fr.gael.dhus.service.exception.MalformedEmailException;
import fr.gael.dhus.service.exception.RequiredFieldMissingException;
import fr.gael.dhus.service.exception.RootNotModifiableException;
import fr.gael.dhus.service.exception.UserBadEncryptionException;
import fr.gael.dhus.service.exception.UserBadOldPasswordException;
import fr.gael.dhus.service.exception.UserNotExistingException;
import fr.gael.dhus.service.exception.UsernameBadCharacterException;
import fr.gael.dhus.service.job.JobScheduler;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.spring.context.SecurityContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.quartz.SchedulerException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * User Service provides connected clients with a set of method to interact with
 * it.
 */
@Service
public class UserService extends WebService
{
   private static final Logger LOGGER = LogManager.getLogger(UserService.class);

   @Autowired
   private SearchDao searchDao;

   @Autowired
   private CountryDao countryDao;

   @Autowired
   private UserDao userDao;

   @Autowired
   private AccessRestrictionDao accessRestrictionDao;

   @Autowired
   private ConfigurationManager cfgManager;

   @Autowired
   private MailServer mailer;

   @Autowired
   private JobScheduler scheduler;

   @Autowired
   private SecurityService securityService;

   @Autowired
   private CacheManager cacheManager;

   @Autowired
   private SecurityContextProvider securityContextProvider;

   /**
    * Pattern for username checking
    */
   public static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\._\\-]+$");

   /**
    * Pattern for email checking
    * Note: This pattern contains all the possible characters in an e-mail. 
    * DHuS shall restrict these mail characters to enhance mailing security...
    * As far as mail servers already avoid a large part of possible mailing 
    * hacks, no security breach is expected even if all the character are
    * authorized in DHuS...
    */
   public static final Pattern EMAIL_PATTERN = Pattern.compile(
      "^[a-zA-Z0-9!#$%\\x26'*+/=?^_`{|}~-]+" +
      "(?:\\.[a-zA-Z0-9!#$%\\x26'*+/=?^_`{|}~-]+)*" + 
      "@" +
      "(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+"  +
      "[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?$");

   /**
    * Return user corresponding to given id.
    * 
    * @param id User id.
    * @throws RootNotModifiableException
    */
   @PreAuthorize ("hasAnyRole('ROLE_USER_MANAGER','ROLE_DATA_MANAGER','ROLE_SYSTEM_MANAGER')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   @Cacheable (value = "user", key = "#id")
   public User getUser (String id) throws RootNotModifiableException
   {
      User u = userDao.read (id);
      checkRoot (u);
      return u;
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public User getUserNoCache (String id)
   {
      User u = userDao.read (id);
      return u;
   }

   /**
    * Return user corresponding to given user name.
    * 
    * @param name User name.
    * @throws RootNotModifiableException
    */
   @PreAuthorize ("hasAnyRole('ROLE_USER_MANAGER','ROLE_DATA_MANAGER','ROLE_SYSTEM_MANAGER')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   @Cacheable (value = "userByName", key = "#name?.toLowerCase()")
   public User getUserByName (String name) throws RootNotModifiableException
   {
      User u = this.getUserNoCheck (name);
      checkRoot (u);
      return u;
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   @Cacheable (value = "userByName", key = "#name?.toLowerCase()")
   public User getUserNoCheck (String name)
   {
      return userDao.getByName (name);
   }

   /**
    * Get all users corresponding to given filter.
    * 
    * @param filter
    * @return All users corresponding to given filter.
    */
   @PreAuthorize ("hasAnyRole('ROLE_USER_MANAGER','ROLE_SYSTEM_MANAGER')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public Iterator<User> getUsers (String filter, int skip)
   {
      return userDao.iterate(filter, skip);
   }

   /**
    * Retrieves corresponding users at the given criteria.
    *
    * @param visitor visitor contains filter and order of required collection.
    * @param skip    number of skipped valid results.
    * @param top     max of valid results.
    * @return a list of {@link User}
    */
   @Transactional(readOnly = true)
   public List<User> getUsers(UserSQLVisitor visitor, int skip, int top)
   {
      return userDao.executeHQLQuery(visitor.getHqlQuery(), visitor.getHqlParameters(), skip, top);
   }

   /**
    * Counts corresponding users at the given criteria.
    *
    * @param visitor visitor contains filter of required collection.
    * @return number of corresponding users.
    */
   @Transactional(readOnly = true)
   public int countUsers(UserSQLVisitor visitor)
   {
      return userDao.countHQLQuery(visitor.getHqlQuery(), visitor.getHqlParameters());
   }

   @PreAuthorize ("hasRole('ROLE_STATS')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public Iterator<User> getAllUsers (String filter, int skip)
   {
      return userDao.scrollAll (filter, skip);
   }

   /**
    * Create given User, after checking required fields.
    * 
    * @param user
    * @throws RequiredFieldMissingException
    * @throws RootNotModifiableException
    */
   @PreAuthorize ("hasRole('ROLE_USER_MANAGER')")
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   public void createUser (User user) throws RequiredFieldMissingException,
      RootNotModifiableException, EmailNotSentException
   {
      systemCreateUser(user);
   }

   /**
    * Create given User, after checking required fields.
    * No @PreAuthorize.
    *
    * @param user
    * @throws RequiredFieldMissingException
    * @throws RootNotModifiableException
    */
   @Transactional(readOnly=false)
   @CacheEvict(value = "userByName", key = "#user?.getUsername().toLowerCase()")
   public void systemCreateUser(User user) throws RequiredFieldMissingException,
      RootNotModifiableException, EmailNotSentException
   {
      checkRequiredFields(user);
      checkRoot(user);
      userDao.create(user);
   }

   /**
    * Create given user provided by the SSO service.
    *
    * @param user to create
    * @return created user
    */
   @Transactional(readOnly=false)
   @CacheEvict(value = "userByName", key = "#user?.getUsername().toLowerCase()")
   public User systemCreateSSOUser(User user)
   {
      return userDao.createWithoutMail(user);
   }

   /**
    * Create given User as temporary User, after checking required fields.
    * 
    * @param user
    * @throws RequiredFieldMissingException
    * @throws RootNotModifiableException
    */
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   public void createTmpUser (User user) throws RequiredFieldMissingException,
      RootNotModifiableException, EmailNotSentException
   {
      checkRequiredFields (user);
      checkRoot (user);
      userDao.createTmpUser (user);
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public Country getCountry (long id)
   {
      return countryDao.read (id);
   }

   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   public void validateTmpUser (String code)
   {
      User u = userDao.getUserFromUserCode (code);
      if (u != null && userDao.isTmpUser (u))
      {
         userDao.registerTmpUser (u);

         // update cache entries
         Cache cache = cacheManager.getCache("user");
         if (cache != null)
         {
            synchronized (cache)
            {
               if (cache.get(u.getUUID()) != null)
               {
                  cache.put(u.getUUID(), u);
               }
            }
         }

         cache = cacheManager.getCache("userByName");
         if (cache != null)
         {
            synchronized (cache)
            {
               if (cache.get(u.getUsername()) != null)
               {
                  cache.put(u.getUsername(), u);
               }
            }
         }
      }
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public boolean checkUserCodeForPasswordReset(String code)
   {
      return userDao.getUserFromUserCode (code) != null;
   }

   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   @Caching (evict = {
      @CacheEvict(value = "user", allEntries = true),
      @CacheEvict(value = "userByName", allEntries = true)})
   public void resetPassword(String code, String new_password)
      throws RootNotModifiableException, RequiredFieldMissingException, 
         EmailNotSentException
   {
      User u = userDao.getUserFromUserCode (code);
      if (u == null)
      {
         throw new UserNotExistingException ();
      }
      checkRoot (u);

      u.setPassword (new_password);

      checkRequiredFields (u);
      userDao.update (u);
   }

   /**
    * Update given User, after checking required fields.
    * 
    * @param user
    * @throws RootNotModifiableException
    * @throws RequiredFieldMissingException
    */
   @PreAuthorize ("hasRole('ROLE_USER_MANAGER')")
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   @Caching(evict = {
      @CacheEvict(value = "user", key = "#user?.getUUID ()"),
      @CacheEvict(value = "userByName", key = "#user?.username.toLowerCase()")})
   public void updateUser (User user) throws RootNotModifiableException,
      RequiredFieldMissingException
   {
      User u = userDao.read (user.getUUID ());
      boolean updateRoles = user.getRoles ().size () != u.getRoles ().size ();
      if (!updateRoles)
      {
         int roleFound = 0;
         for (Role r : u.getRoles ())
         {
            if (user.getRoles ().contains (r))
            {
               roleFound++;
            }
         }
         updateRoles = roleFound != user.getRoles ().size ();
      }
      checkRoot (u);
      u.setUsername (user.getUsername ());
      u.setFirstname (user.getFirstname ());
      u.setLastname (user.getLastname ());
      u.setAddress (user.getAddress ());
      u.setCountry (user.getCountry ());
      u.setEmail (user.getEmail ());
      u.setPhone (user.getPhone ());
      u.setRoles (user.getRoles ());
      u.setUsage (user.getUsage ());
      u.setSubUsage (user.getSubUsage ());
      u.setDomain (user.getDomain ());
      u.setSubDomain (user.getSubDomain ());
      if (user.getPassword() != null)
      {
         // If password is null, it means client forgot to set it up.
         // it should never been set to null.
         u.setEncryptedPassword(user.getPassword(),
            user.getPasswordEncryption());
      }

      Set<AccessRestriction> restrictions = user.getRestrictions ();
      Set<AccessRestriction> restrictionsToDelete = u.getRestrictions ();
      if (u.getRestrictions () != null && user.getRestrictions () != null)
      {
         for (AccessRestriction oldOne : u.getRestrictions ())
         {
            for (AccessRestriction newOne : user.getRestrictions ())
            {
               if (oldOne.getBlockingReason ().equals (
                  newOne.getBlockingReason ()))
               {
                  restrictions.remove (newOne);
                  restrictions.add (oldOne);
                  restrictionsToDelete.remove (oldOne);
               }
               continue;
            }
         }
      }

      u.setRestrictions (restrictions);
      checkRequiredFields (u);
      userDao.update (u);

      if ((restrictions != null && !restrictions.isEmpty ()) || updateRoles)
      {
         securityContextProvider.forceLogout(u.getUsername());
      }

      if (restrictionsToDelete != null)
      {
         for (AccessRestriction restriction : restrictionsToDelete)
         {
            accessRestrictionDao.delete (restriction);
         }
      }

      // Fix to mail user when admin updates his account
      // Temp : to move in mail class after
       LOGGER.debug("User " + u.getUsername () + 
       " Updated.");

       if (cfgManager.getMailConfiguration ().isOnUserUpdate ())
       {
          String email = u.getEmail ();
          // Do not send mail to system admin : never used
          if (cfgManager.getAdministratorConfiguration ().getName ()
                .equals (u.getUsername ()) && (email==null))
             email = "dhus@gael.fr";

          LOGGER.debug("Sending email to " + email);
          if (email == null)
             throw new UnsupportedOperationException (
                "Missing Email in configuration: " +
                 "Cannot inform modified user \"" + u.getUsername () + ".");

          String message = new String (
             "Dear " + getUserWelcome (u) + ",\n\nYour account on " +
             cfgManager.getNameConfiguration ().getShortName () +
             " has been updated by an administrator:\n" + u.toString () + "\n" +
             "For help requests please write to: " +
             cfgManager.getSupportConfiguration ().getMail () + "\n\n"+
             "Kind regards,\n" +
             cfgManager.getSupportConfiguration ().getName () + ".\n" +
             cfgManager.getServerConfiguration ().getExternalUrl ());
          String subject = new String ("Account " + u.getUsername () +
                " updated");
          try
          {
             mailer.send  (email, null, null, subject, message);
          }
          catch (Exception e)
          {
             throw new EmailNotSentException (
                "Cannot send email to " + email, e);
          }
          LOGGER.debug("email sent.");
       }

   }

   /**
    * Update given User, after checking required fields.
    * @param user
    * @throws RootNotModifiableException
    * @throws RequiredFieldMissingException
    */
   @Transactional(readOnly=false)
   public void systemUpdateUser(User user) throws RootNotModifiableException,
         RequiredFieldMissingException
   {
      checkRoot (user);
      userDao.update(user); // FIXME reproduce updateUser()?
   }

   /**
    * Delete user corresponding to given id.
    * 
    * @param uuid User id.
    * @throws RootNotModifiableException
    */
   @PreAuthorize ("hasRole('ROLE_USER_MANAGER')")
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   @Caching (evict = {
      @CacheEvict(value = "user", allEntries = true),
      @CacheEvict(value = "userByName", allEntries = true)})
   public void deleteUser (String uuid) throws RootNotModifiableException,
      EmailNotSentException
   {
      User u = userDao.read (uuid);
      checkRoot (u);
      securityContextProvider.forceLogout(u.getUsername());
      userDao.delete(u);
   }

   /**
    * Cout number of users corresponding to filter.
    * 
    * @param filter
    * @return Number of users corresponding to filter.
    */
   @PreAuthorize ("hasAnyRole('ROLE_USER_MANAGER','ROLE_DATA_MANAGER','ROLE_SYSTEM_MANAGER')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public int count (String filter)
   {
      return userDao.countNotAdmin(filter);
   }

   @PreAuthorize ("hasRole('ROLE_STATS')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public int countAll (String filter)
   {
      return userDao.countAll (filter);
   }

   @PreAuthorize ("isAuthenticated ()")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public List<AccessRestriction> getRestrictions (String user_uuid)
   {
      return new ArrayList<> (userDao.read (user_uuid).getRestrictions ());
   }

   /**
    * Update given User, after checking required fields.
    * 
    * @param user
    * @throws RootNotModifiableException
    * @throws RequiredFieldMissingException
    */
   @PreAuthorize ("isAuthenticated ()")
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   @Caching (evict = {
      @CacheEvict(value = "user", key = "#user.getUUID ()"),
      @CacheEvict(value = "userByName", key = "#user.username.toLowerCase()")})
   public void selfUpdateUser (User user) throws RootNotModifiableException,
      RequiredFieldMissingException, EmailNotSentException
   {
      User u = userDao.read (user.getUUID ());
      checkRoot (u);
      u.setEmail (user.getEmail ());
      u.setFirstname (user.getFirstname ());
      u.setLastname (user.getLastname ());
      u.setAddress (user.getAddress ());
      u.setPhone (user.getPhone ());    
      u.setCountry (user.getCountry ());
      u.setUsage (user.getUsage ());
      u.setSubUsage (user.getSubUsage ());
      u.setDomain (user.getDomain ());
      u.setSubDomain (user.getSubDomain ());  

      checkRequiredFields (u);
      userDao.update (u);
   }

   @PreAuthorize ("isAuthenticated ()")
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   @Caching (evict = {
      @CacheEvict(value = "user", allEntries = true),
      @CacheEvict(value = "userByName", allEntries = true)})
   public void selfChangePassword (String uuid, String old_password,
         String new_password) throws RootNotModifiableException,
         RequiredFieldMissingException, EmailNotSentException,
         UserBadOldPasswordException
   {
      User u = userDao.read (uuid);
      checkRoot (u);

      //encrypt old password to compare
      PasswordEncryption encryption = u.getPasswordEncryption ();
      if (encryption != PasswordEncryption.NONE) // when configurable
      {
         try
         {
            MessageDigest md =
                  MessageDigest.getInstance(encryption.getAlgorithmKey());
            old_password = new String(
                  Hex.encode(md.digest(old_password.getBytes("UTF-8"))));
         }
         catch (Exception e)
         {
            throw new UserBadEncryptionException (
                  "There was an error while encrypting password of user " +
                        u.getUsername (), e);
         }
      }      

      if (! u.getPassword ().equals(old_password))
      {
         throw new UserBadOldPasswordException("Old password is not correct.");
      }

      u.setPassword (new_password);

      checkRequiredFields (u);
      userDao.update (u);
   }

   @PreAuthorize ("hasRole('ROLE_SEARCH')")
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   public void storeUserSearch (String uuid, String search, String footprint,
         HashMap<String, String> advanced, String complete)
   {
      User u = userDao.read (uuid);
      if (u == null)
      {
         throw new UserNotExistingException ();
      }
      for (Search s : u.getPreferences ().getSearches ())
      {
         if (s.getComplete ().equals(complete))
         {
            return;
         }            
      }
      userDao.storeUserSearch (u, search, footprint, advanced, complete);
   }

   @PreAuthorize ("hasRole('ROLE_SEARCH')")
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   public void removeUserSearch (String u_uuid, String uuid)
   {
      User u = userDao.read (u_uuid);
      if (u == null)
      {
         throw new UserNotExistingException ();
      }
      userDao.removeUserSearch (u, uuid);
   }

   @PreAuthorize ("hasRole('ROLE_SEARCH')")
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   public void activateUserSearchNotification (String uuid, boolean notify)
   {
      userDao.activateUserSearchNotification (uuid, notify);
   }

   @PreAuthorize ("hasRole('ROLE_SEARCH')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public int countUserSearches (String uuid)
   {
      User u = userDao.read (uuid);
      if (u == null)
      {
         throw new UserNotExistingException ();
      }
      List<Search> searches = userDao.getUserSearches(u);
      return searches != null ? searches.size () : 0;
   }

   @PreAuthorize ("hasRole('ROLE_SEARCH')")
   @Transactional (readOnly=false, propagation=Propagation.REQUIRED)
   public void clearSavedSearches (String uuid)
   {
      User u = userDao.read (uuid);
      if (u == null)
      {
         throw new UserNotExistingException ();
      }
      userDao.clearUserSearches(u);
   }

   @PreAuthorize ("hasRole('ROLE_SEARCH')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public List<Search> getAllUserSearches (String uuid)
   {    
      User u = userDao.read (uuid);
      if (u == null)
      {
         throw new UserNotExistingException ();
      }
      return userDao.getUserSearches(u);
   }

   @PreAuthorize ("hasRole('ROLE_SEARCH')")
   public Date getNextScheduleSearch() throws SchedulerException
   {
      return scheduler.getNextSearchesJobSchedule ();
   }

   @PreAuthorize ("hasRole('ROLE_SEARCH')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public List<Search> scrollSearchesOfUser (String uuid, int skip, int top)
   {
      User u = userDao.read (uuid);
      if (u == null)
      {
         throw new UserNotExistingException ();
      }
      return searchDao.scrollSearchesOfUser (u, skip, top);
   }
   
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public void forgotPassword (User user, String baseuri) 
      throws UserNotExistingException, RootNotModifiableException,
         EmailNotSentException
   {
      checkRoot (user);
      User checked = userDao.getByName (user.getUsername ());
      if (checked == null || !checked.getEmail ().toLowerCase ().
               equals (user.getEmail ().toLowerCase ()))
      {
         throw new UserNotExistingException ("No user can be found for this " +
                        "username/mail combination");
      }

      String message = "Dear " + getUserWelcome (checked) +",\n\n" +
            "Please follow this link to set a new password in the " +
            cfgManager.getNameConfiguration ().getShortName () +" system:\n" +
            cfgManager.getServerConfiguration ().getExternalUrl () + baseuri +
            userDao.computeUserCode (checked) + "\n\n"  +
            "For help requests please write to: " +
            cfgManager.getSupportConfiguration ().getMail () + "\n\n" +
            "Kind regards.\n" +
            cfgManager.getSupportConfiguration ().getName () + ".\n" +
            cfgManager.getServerConfiguration ().getExternalUrl ();

      String subject = "User password reset";

      try
      {
         mailer.send  (checked.getEmail (), null, null, subject, message);
      }
      catch (Exception e)
      {
         throw new EmailNotSentException (
            "Cannot send email to " + checked.getEmail (), e);
      }
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   private void checkRoot (User user) throws RootNotModifiableException
   {
      if (user == null) return;
      if (userDao.isRootUser (user))
      {
         throw new RootNotModifiableException ("Root cannot be modified");
      }
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   private void checkRequiredFields (User user)
      throws RequiredFieldMissingException, UsernameBadCharacterException,
      MalformedEmailException
   {
      if (user.getUsername () == null ||
         user.getUsername ().trim ().isEmpty () ||
         user.getPassword () == null ||
         user.getPassword ().trim ().isEmpty () || user.getEmail () == null ||
         user.getEmail ().trim ().isEmpty ())
      {
         throw new RequiredFieldMissingException (
            "At least one required field is empty.");
      }
      // Test username allowed chars [a-zA-Z0-9]
      if (!USERNAME_PATTERN.matcher (user.getUsername ()).find ())
      {
         throw new UsernameBadCharacterException (
            "At least one forbidden character has been detected in username.");
      }
      // Test email field
      if (!EMAIL_PATTERN.matcher (user.getEmail ()).find ())
      {
         throw new MalformedEmailException (
            "Email is not well formed.");
      }
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   private String getUserWelcome (User u)
   {
      String firstname = u.getUsername ();
      String lastname = "";
      if (u.getFirstname () != null && !u.getFirstname().trim ().isEmpty ())
      {
         firstname = u.getFirstname ();
         if (u.getLastname () != null && !u.getLastname().trim ().isEmpty ())
            lastname = " " + u.getLastname ();
      }
      return firstname + lastname;
   }

   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public List<Country> getCountries ()
   {
      return countryDao.readAll ();
   }

   @PreAuthorize ("isAuthenticated ()")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public User getCurrentUserInformation () throws RootNotModifiableException
   {
      User u = securityService.getCurrentUser ();
      if (u == null) return null;
      return getUserByName(u.getUUID ());
   }

   /**
    * Get all users corresponding to given filter from the specified offset and limit.
    * @param filter
    * @param skip
    * @return
    */
   @PreAuthorize ("hasRole('ROLE_USER_MANAGER')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public Iterator<User> getUsersByFilter (String filter, int skip)
   {
      return userDao.getUsersByFilter(filter, skip);
   }

   /**
    * Cout number of users corresponding to filter.
    * 
    * @param filter
    * @return Number of users corresponding to filter.
    */
   @PreAuthorize ("hasAnyRole('ROLE_USER_MANAGER','ROLE_DATA_MANAGER')")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public int countByFilter (String filter)
   {
      return userDao.countByFilter(filter);
   }

   /**
    * Finds a referenced country in ISO norm.
    * @param country name, alpha2 or alpha3 of country.
    * @return true, if country name, alpha2 or alpha is referenced in ISO norme.
    */
   @Transactional (readOnly = true)
   public Country getCountry (String country)
   {
      switch (country.length ())
      {
         case 2:
            return countryDao.getCountryByAlpha2 (country);
         case 3:
            return countryDao.getCountryByAlpha3 (country);
         default:
            return countryDao.getCountryByName (country);
      }
   }

   /**
    * Returns quota informations of given user.
    *
    * @param user from whom to retrieve quota informations
    * @return quota informations
    */
   public CurrentQuotas getCurrentQuotasForUser(User user)
   {
      Objects.requireNonNull(user);
      return user.getCurrentQuotas();
   }

   /**
    * Returns the number of running downloads of given user.
    *
    * @param user from whom to retrieve number of downloads running in parallel
    * @return number of downloads running in parallel
    */
   @Cacheable(value = "current_quotas", key = "#user.getUUID()")
   public int getCurrentDownloadsForUser(User user)
   {
      int currentDownloads = getCurrentQuotasForUser(user).getCurrentDownloads();
      return currentDownloads;
   }

   /**
    * Increment the number of running downloads of given user.
    *
    * @param user whose downloads running in parallel is to be incremented
    * @return the new number of running downloads
    */
   @CachePut(value = "current_quotas", key = "#user.getUUID()")
   public int addDownloadForUser(User user)
   {
      int downloads = ApplicationContextProvider.getBean(UserService.class).getCurrentDownloadsForUser(user);
      getCurrentQuotasForUser(user).setCurrentDownloads(downloads + 1);
      return downloads + 1;
   }

   /**
    * Decrement the number of running downloads of given user.
    *
    * @param user whose downloads running in parallel is to be decremented
    * @return the new number of running downloads
    */
   @CachePut(value = "current_quotas", key = "#user.getUUID()")
   public int removeDownloadForUser(User user)
   {
      int downloads = ApplicationContextProvider.getBean(UserService.class).getCurrentDownloadsForUser(user);
      getCurrentQuotasForUser(user).setCurrentDownloads(downloads - 1);
      return downloads - 1;
   }

   @PreAuthorize("hasRole('ROLE_USER_MANAGER')")
   @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
   @Caching(evict =
   {
      @CacheEvict(value = "user", key = "#user?.getUUID()"),
      @CacheEvict(value = "userByName", key = "#user?.username.toLowerCase()")
   })
   public void lockUser(User user, String reason) throws RootNotModifiableException, UserNotExistingException
   {
      LockedAccessRestriction accessRestriction = new LockedAccessRestriction();
      accessRestriction.setBlockingReason(reason);
      user.addRestriction(accessRestriction);
      userDao.update(user);
   }

   @PreAuthorize("hasRole('ROLE_USER_MANAGER')")
   @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
   @Caching(evict =
   {
      @CacheEvict(value = "user", key = "#user?.getUUID()"),
      @CacheEvict(value = "userByName", key = "#user?.username.toLowerCase()")
   })
   public boolean unlockUser(User user, String restrictionUUID) throws RootNotModifiableException, UserNotExistingException
   {
      AccessRestriction toRemove = null;
      for (AccessRestriction accessRestriction: user.getRestrictions())
      {
         if (accessRestriction.getUUID().equals(restrictionUUID))
         {
            toRemove = accessRestriction;
         }
      }

      if (toRemove == null)
      {
         return false;
      }
      user.getRestrictions().remove(toRemove);
      userDao.update(user);
      accessRestrictionDao.delete(toRemove);
      return true;
   }

   /**
    * Called by DatabasePostInit.
    */
   @Transactional
   public void systemCreateOrUpdateRootAccount()
   {
      // Root account
      AdministratorConfiguration cfg = cfgManager.getAdministratorConfiguration();
      User rootUser = userDao.getByName(cfg.getName());
      if (rootUser != null)
      {
         // If root User exists, update his roles by security
         ArrayList<Role> roles = new ArrayList<>();
         roles.addAll(Arrays.asList(Role.values()));
         rootUser.setRoles(roles);
         userDao.update(rootUser);
      }
      else
      {
         // Create it
         rootUser = new User();
         rootUser.setUsername(cfg.getName());
         rootUser.setPassword(cfg.getPassword());
         rootUser.setCreated(new Date());
         ArrayList<Role> roles = new ArrayList<>();
         roles.addAll(Arrays.asList(Role.values()));
         rootUser.setRoles(roles);
         rootUser.setDomain("Other");
         rootUser.setSubDomain("System");
         rootUser.setUsage("Other");
         rootUser.setSubUsage("System");
         userDao.create(rootUser);
      }
   }
}
