/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2020 GAEL Systems
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
package org.dhus.olingo.v2.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.dhus.olingo.v2.ODataSecurityManager;
import org.dhus.olingo.v2.datamodel.SearchModel;
import org.dhus.olingo.v2.datamodel.UserModel;
import org.dhus.olingo.v2.datamodel.action.AddSearchAction;
import org.dhus.olingo.v2.datamodel.action.ClearSearchesAction;
import org.dhus.olingo.v2.datamodel.action.DeleteSearchAction;
import org.dhus.olingo.v2.datamodel.action.LockUserAction;
import org.dhus.olingo.v2.datamodel.action.UnlockUserAction;
import org.dhus.olingo.v2.datamodel.action.EnableSearchAction;
import org.dhus.olingo.v2.datamodel.complex.RestrictionComplexType;
import org.dhus.olingo.v2.visitor.UserSQLVisitor;

import fr.gael.dhus.database.object.Country;
import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.Search;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.database.object.restriction.AccessRestriction;
import fr.gael.dhus.olingo.Security;
import fr.gael.dhus.service.UserService;
import fr.gael.dhus.service.exception.EmailNotSentException;
import fr.gael.dhus.service.exception.GDPREnabledException;
import fr.gael.dhus.service.exception.RequiredFieldMissingException;
import fr.gael.dhus.service.exception.RootNotModifiableException;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;
import fr.gael.odata.engine.data.DataHandlerUtil;
import fr.gael.odata.engine.data.DatabaseDataHandler;

public class UserDataHandler implements DatabaseDataHandler
{
   private static final UserService USER_SERVICE = ApplicationContextProvider.getBean(UserService.class);
   
   private ConfigurationManager CONFIG_MANAGER = 
         ApplicationContextProvider.getBean(ConfigurationManager.class);


   private static final Logger LOGGER = LogManager.getLogger();

   private Entity toOlingoEntity(User user)
   {
      Entity userEntity = new Entity();

      // username
      userEntity.addProperty(new Property(
            null,
            UserModel.PROPERTY_USERNAME,
            ValueType.PRIMITIVE,
            user.getUsername()));

      if (!CONFIG_MANAGER.isGDPREnabled())
      {
         // email
         userEntity.addProperty(new Property(
            null,
            UserModel.PROPERTY_EMAIL,
            ValueType.PRIMITIVE,
            user.getEmail()));

         // firstName
         userEntity.addProperty(new Property(
            null,
            UserModel.PROPERTY_FIRSTNAME,
            ValueType.PRIMITIVE,
            user.getFirstname()));

         // lastName
         userEntity.addProperty(new Property(
            null,
            UserModel.PROPERTY_LASTNAME,
            ValueType.PRIMITIVE,
            user.getLastname()));

         // country
         userEntity.addProperty(new Property(
            null,
            UserModel.PROPERTY_COUNTRY,
            ValueType.PRIMITIVE,
            user.getCountry()));

         // phone
         userEntity.addProperty(new Property(
            null,
            UserModel.PROPERTY_PHONE,
            ValueType.PRIMITIVE,
            user.getPhone()));

         // adresse
         userEntity.addProperty(new Property(
            null,
            UserModel.PROPERTY_ADDRESS,
            ValueType.PRIMITIVE,
            user.getAddress()));

         // domain
         userEntity.addProperty(new Property(
            null,
            UserModel.PROPERTY_DOMAIN,
            ValueType.PRIMITIVE,
            user.getDomain()));

         // subDomain
         userEntity.addProperty(new Property(
            null,
            UserModel.PROPERTY_SUBDOMAIN,
            ValueType.PRIMITIVE,
            user.getSubDomain()));

         // usage
         userEntity.addProperty(new Property(
            null,
            UserModel.PROPERTY_USAGE,
            ValueType.PRIMITIVE,
            user.getUsage()));

         // subUsage
         userEntity.addProperty(new Property(
            null,
            UserModel.PROPERTY_SUBUSAGE,
            ValueType.PRIMITIVE,
            user.getSubUsage()));

         // hash
         userEntity.addProperty(new Property(
            null,
            UserModel.PROPERTY_HASH,
            ValueType.PRIMITIVE,
            user.getPasswordEncryption().getAlgorithmKey()));

         // password
         userEntity.addProperty(new Property(
            null,
            UserModel.PROPERTY_PASSWORD,
            ValueType.PRIMITIVE,
            user.getPassword()));

         // Restrictions
         userEntity.addProperty(new Property(
            null,
            UserModel.PROPERTY_RESTRICTIONS,
            ValueType.COLLECTION_COMPLEX,
            user.getRestrictions().stream().map(UserDataHandler::restrictionToComplexValue).collect(Collectors.toList())));

      }
      // created
      userEntity.addProperty(new Property(
            null,
            UserModel.PROPERTY_CREATED,
            ValueType.PRIMITIVE,
            user.getCreated()));

      // SystemRoles
      userEntity.addProperty(new Property(
            null,
            UserModel.PROPERTY_ROLES,
            ValueType.COLLECTION_ENUM,
            user.getRoles().stream().<Integer>map((r) -> r.ordinal()).collect(Collectors.toList())));

      userEntity.setId(DataHandlerUtil.createEntityId(UserModel.ENTITY_SET_NAME, user.getUsername()));
      userEntity.setType(UserModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());

      return userEntity;
   }

   private static ComplexValue restrictionToComplexValue(AccessRestriction restriction)
   {
      ComplexValue value = new ComplexValue();
      value.setTypeName(RestrictionComplexType.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString());

      // Id
      value.getValue().add(new Property(
            null,
            RestrictionComplexType.PROPERTY_ID,
            ValueType.PRIMITIVE,
            restriction.getUUID()));

      // Type
      value.getValue().add(new Property(
            null,
            RestrictionComplexType.PROPERTY_TYPE,
            ValueType.PRIMITIVE,
            restriction.getClass().getSimpleName()));

      // Reason
      value.getValue().add(new Property(
            null,
            RestrictionComplexType.PROPERTY_REASON,
            ValueType.PRIMITIVE,
            restriction.getBlockingReason()));

      return value;
   }

   private static Role enumToRole(Object object)
   {
      return Role.values()[(Number.class.cast(object).intValue())];
   }

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      return getEntityCollectionData(null, null, null, null, null);
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      String username = DataHandlerUtil.getSingleStringKeyParameterValue(keyParameters, UserModel.PROPERTY_USERNAME);
      User user = getUser(username);    
      return toOlingoEntity(user);      
   }
   
   public static User getUser(String username) throws ODataApplicationException
   {
      User user = null;
      if (Security.currentUserHasRole(Role.SYSTEM_MANAGER, Role.USER_MANAGER))
      {
         user = USER_SERVICE.getUserNoCheck(username);
      }
      else
      {
         User u = Security.getCurrentUser();      
         if (username != null && u.getUsername ().equals (username))
         {
            user = USER_SERVICE.getUserNoCheck(username);
         }
         else
         {
            throw new ODataApplicationException("You are not allowed to display another User information",
                  HttpStatusCode.FORBIDDEN.getStatusCode(), Locale.ENGLISH);
         }
      }

      if (user == null)
      {
         throw new ODataApplicationException("No user exists with this name",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }
      return user;
   }

   @Override
   public EntityCollection getEntityCollectionData(FilterOption filterOption,
         OrderByOption orderByOption, TopOption topOption, SkipOption skipOption,
         CountOption countOption) throws ODataApplicationException
   {
      EntityCollection entityCollection = new EntityCollection();

      boolean hasRole = Security.currentUserHasRole(Role.SYSTEM_MANAGER, Role.USER_MANAGER);
      // users that can see only themselves on OData
      if(!hasRole || CONFIG_MANAGER.isGDPREnabled())
      {
         User user = USER_SERVICE.getUserNoCheck(Security.getCurrentUser().getUsername());
         entityCollection.getEntities().add(toOlingoEntity(user));
         return entityCollection;
      }

      // users that can see all users of the service
      UserSQLVisitor visitor = new UserSQLVisitor(filterOption, orderByOption, topOption, skipOption);
      List <User> users = USER_SERVICE.getUsers(
            visitor.getHqlQuery(),
            visitor.getHqlParameters(),
            visitor.getSkip(),
            visitor.getTop());

      users.forEach(user -> entityCollection.getEntities().add(toOlingoEntity(user))); 
      return entityCollection;
   }

   @Override
   public Entity createEntityData(Entity entity) throws ODataApplicationException
   {
      if (CONFIG_MANAGER.isGDPREnabled())
      {
         LOGGER.warn("GDPR enabled. User management not done by DHuS. Cannot create User.");
         throw new ODataApplicationException("GDPR enabled. User management not done by DHuS. Cannot create User.",
               HttpStatusCode.FORBIDDEN.getStatusCode(), Locale.ENGLISH);
      }
      String username = (String) DataHandlerUtil.getPropertyValue(entity, UserModel.PROPERTY_USERNAME);
      if(username == null)
      {
         throw new ODataApplicationException("User's name is mandatory",
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }

      User user = new User();

      // username
      user.setUsername(username);

      // email
      if (DataHandlerUtil.containsProperty(entity, UserModel.PROPERTY_EMAIL))
      {
         user.setEmail((String) DataHandlerUtil.getPropertyValue(entity, UserModel.PROPERTY_EMAIL));
      }

      // firstname
      if (DataHandlerUtil.containsProperty(entity, UserModel.PROPERTY_FIRSTNAME))
      {
         user.setFirstname((String) DataHandlerUtil.getPropertyValue(entity, UserModel.PROPERTY_FIRSTNAME));
      }

      // lastname
      if (DataHandlerUtil.containsProperty(entity, UserModel.PROPERTY_LASTNAME))
      {
         user.setLastname((String) DataHandlerUtil.getPropertyValue(entity, UserModel.PROPERTY_LASTNAME));
      }

      // country
      if (DataHandlerUtil.containsProperty(entity, UserModel.PROPERTY_COUNTRY))
      {
         String countryProperty = (String) DataHandlerUtil.getPropertyValue(entity, UserModel.PROPERTY_COUNTRY);
         Country isoCountry = USER_SERVICE.getCountry(countryProperty);
         if(isoCountry != null)
         {
         user.setCountry(isoCountry.getName());
         }
         else
         {
            throw new ODataApplicationException("Country not found",
                  HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
         }
      }

      // phone
      if (DataHandlerUtil.containsProperty(entity, UserModel.PROPERTY_PHONE))
      {
         user.setPhone((String) DataHandlerUtil.getPropertyValue(entity, UserModel.PROPERTY_PHONE));
      }

      // adress
      if (DataHandlerUtil.containsProperty(entity, UserModel.PROPERTY_ADDRESS))
      {
         user.setAddress((String) DataHandlerUtil.getPropertyValue(entity, UserModel.PROPERTY_ADDRESS));
      }

      //domain
      if (DataHandlerUtil.containsProperty(entity, UserModel.PROPERTY_DOMAIN))
      {
         user.setDomain((String) DataHandlerUtil.getPropertyValue(entity, UserModel.PROPERTY_DOMAIN));
      }

      //subDomain
      if (DataHandlerUtil.containsProperty(entity, UserModel.PROPERTY_SUBDOMAIN))
      {
         user.setSubDomain((String) DataHandlerUtil.getPropertyValue(entity, UserModel.PROPERTY_SUBDOMAIN));
      }

      // usage
      if (DataHandlerUtil.containsProperty(entity, UserModel.PROPERTY_USAGE))
      {
         user.setUsage((String) DataHandlerUtil.getPropertyValue(entity, UserModel.PROPERTY_USAGE));
      }

      // subUsage
      if (DataHandlerUtil.containsProperty(entity, UserModel.PROPERTY_SUBUSAGE))
      {
         user.setSubUsage((String) DataHandlerUtil.getPropertyValue(entity, UserModel.PROPERTY_SUBUSAGE));
      }

      // password
      if (DataHandlerUtil.containsProperty(entity, UserModel.PROPERTY_PASSWORD))
      {
         user.setPassword((String) DataHandlerUtil.getPropertyValue(entity, UserModel.PROPERTY_PASSWORD));
      }
      else
      {
         user.generatePassword();
      }

      // roles (some defaults may apply if not set)
      if (DataHandlerUtil.containsProperty(entity, UserModel.PROPERTY_ROLES))
      {
         user.setRoles(entity.getProperty(UserModel.PROPERTY_ROLES).asCollection().stream()
               .map(UserDataHandler::enumToRole).collect(Collectors.toList()));
      }

      try
      {
         USER_SERVICE.createUser(user);
         return toOlingoEntity(USER_SERVICE.getUserByName(username));
      }
      catch (GDPREnabledException ex)
      {
         throw new ODataApplicationException(ex.getMessage(),
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
      catch (EmailNotSentException ex)
      {
         throw new ODataApplicationException("Could not send validation e-mail",
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
      catch (RequiredFieldMissingException ex)
      {
         throw new ODataApplicationException("A required field is missing to create a User: " + ex.getMessage(),
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
      catch (RootNotModifiableException ex)
      {
         throw new ODataApplicationException("Root cannot be created",
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
   }

   @Override
   public void updateEntityData(List<UriParameter> keyParameters, Entity updatedEntity, HttpMethod httpMethod)
         throws ODataApplicationException
   {
      if (CONFIG_MANAGER.isGDPREnabled())
      {
         LOGGER.warn("GDPR enabled. User management not done by DHuS. Cannot update User.");
         throw new ODataApplicationException("GDPR enabled. User management not done by DHuS. Cannot update User.",
               HttpStatusCode.FORBIDDEN.getStatusCode(), Locale.ENGLISH);
      }
      User user = getUserFromParameters(keyParameters);
      if (user == null)
      {
         throw new ODataApplicationException("No user exists with this name",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }
      String email = (String) DataHandlerUtil.getPropertyValue(updatedEntity, UserModel.PROPERTY_EMAIL);
      String firstName = (String) DataHandlerUtil.getPropertyValue(updatedEntity, UserModel.PROPERTY_FIRSTNAME);
      String lastName = (String) DataHandlerUtil.getPropertyValue(updatedEntity, UserModel.PROPERTY_LASTNAME);
      String country = (String) DataHandlerUtil.getPropertyValue(updatedEntity, UserModel.PROPERTY_COUNTRY);
      String phone = (String) DataHandlerUtil.getPropertyValue(updatedEntity, UserModel.PROPERTY_PHONE);
      String address = (String) DataHandlerUtil.getPropertyValue(updatedEntity, UserModel.PROPERTY_ADDRESS);
      String domain = (String) DataHandlerUtil.getPropertyValue(updatedEntity, UserModel.PROPERTY_DOMAIN);
      String subDomain = (String) DataHandlerUtil.getPropertyValue(updatedEntity, UserModel.PROPERTY_SUBDOMAIN);
      String usage = (String) DataHandlerUtil.getPropertyValue(updatedEntity, UserModel.PROPERTY_USAGE);
      String subUsage = (String) DataHandlerUtil.getPropertyValue(updatedEntity, UserModel.PROPERTY_SUBUSAGE);
      String password = (String) DataHandlerUtil.getPropertyValue(updatedEntity, UserModel.PROPERTY_PASSWORD);

      List<Property> updatedProperties = updatedEntity.getProperties();
      for(Property updatedProperty : updatedProperties)
      {
         String propertyName = updatedProperty.getName();

         // set email
         if (propertyName.equals(UserModel.PROPERTY_EMAIL) && email != null)
         {
            user.setEmail(email);
         }

         // set fistname
         if (propertyName.equals(UserModel.PROPERTY_FIRSTNAME))
         {
            user.setFirstname(firstName);
         }

         // set lastName
         if (propertyName.equals(UserModel.PROPERTY_LASTNAME))
         {
            user.setLastname(lastName);
         }

         // set country
         if (propertyName.equals(UserModel.PROPERTY_COUNTRY))
         {
            Country isoCountry = USER_SERVICE.getCountry(country);
            if (isoCountry != null)
            {
               user.setCountry (isoCountry.getName ());
            }
            else
            {
               throw new ODataApplicationException("Country not found",
                     HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
            }
         }

         // set lastName
         if (propertyName.equals(UserModel.PROPERTY_PHONE))
         {
            user.setPhone(phone);
         }

         // set address
         if (propertyName.equals(UserModel.PROPERTY_ADDRESS))
         {
            user.setAddress(address);
         }

         // set domain
         if (propertyName.equals(UserModel.PROPERTY_DOMAIN))
         {
            user.setDomain(domain);
         }

         // set subDomain
         if (propertyName.equals(UserModel.PROPERTY_SUBDOMAIN))
         {
            user.setSubDomain(subDomain);
         }

         // set usage
         if (propertyName.equals(UserModel.PROPERTY_USAGE))
         {
            user.setUsage(usage);
         }

         // set subUsage
         if (propertyName.equals(UserModel.PROPERTY_SUBUSAGE))
         {
            user.setSubUsage(subUsage);
         }

         // set password
         if (propertyName.equals(UserModel.PROPERTY_PASSWORD))
         {
            user.setPassword(password);
         }

         // set roles
         if (propertyName.equals(UserModel.PROPERTY_ROLES))
         {
            user.setRoles(updatedEntity.getProperty(UserModel.PROPERTY_ROLES).asCollection().stream()
                  .map(UserDataHandler::enumToRole).collect(Collectors.toList()));
         }
      }

      try
      {
         if (Security.getCurrentUser().equals(user))
         {
            USER_SERVICE.selfUpdateUser(user);
         }
         else
         {
            USER_SERVICE.updateUser(user);
         }
      }
      catch (GDPREnabledException | RootNotModifiableException | EmailNotSentException | RequiredFieldMissingException ex)
      {
         throw new ODataApplicationException(ex.getMessage(),
               HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
      }
   }

   @Override
   public void deleteEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      if (CONFIG_MANAGER.isGDPREnabled())
      {
         LOGGER.warn("GDPR enabled. User management not done by DHuS. Cannot delete User.");
         throw new ODataApplicationException("GDPR enabled. User management not done by DHuS. Cannot delete User.",
               HttpStatusCode.FORBIDDEN.getStatusCode(), Locale.ENGLISH);
      }
      String username = DataHandlerUtil.getSingleStringKeyParameterValue(keyParameters, UserModel.PROPERTY_USERNAME);
      try
      {
         User user = USER_SERVICE.getUserByName(username);
         if (user == null)
         {
            throw new ODataApplicationException("No user exists with this name",
                  HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
         }
         else
         {
            try
            {
               USER_SERVICE.deleteUser(user.getUUID());
            }
            catch (GDPREnabledException ex)
            {
               throw new ODataApplicationException(ex.getMessage(),
                     HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
            }
         }
      }
      catch (RootNotModifiableException e)
      {
         LOGGER.error("Cannot delete root user", e.getMessage());
         throw new ODataApplicationException("Cannot delete root user",
               HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
      }
   }

   @Override
   public Integer countEntities(FilterOption filterOption) throws ODataApplicationException
   {
      boolean hasRole = Security.currentUserHasRole(Role.SYSTEM_MANAGER, Role.USER_MANAGER);
      if (!hasRole || CONFIG_MANAGER.isGDPREnabled())
      {
         return 1;
      }
      UserSQLVisitor visitor = new UserSQLVisitor(filterOption, null, null, null);
      return USER_SERVICE.countUsers(visitor.getHqlQuery(), visitor.getHqlParameters());
   }

   @Override
   public Object performBoundAction(List<UriParameter> keyPredicates, EdmAction action, Map<String, Parameter> parameters)
         throws ODataApplicationException
   {
      if (action.getFullQualifiedName().equals(LockUserAction.ACTION_LOCK_USER_FQN))
      {
         ODataSecurityManager.checkPermission(Role.USER_MANAGER);
         String username = DataHandlerUtil.trimStringKeyParameter(keyPredicates.get(0));
         String reason = (String) parameters.get(LockUserAction.PARAMETER_REASON).getValue();
         try
         {
            User user = USER_SERVICE.getUserByName(username);
            AccessRestriction restriction = USER_SERVICE.lockUser(user, reason);
            return new Property(null, LockUserAction.ACTION_LOCK_USER, ValueType.COMPLEX, restrictionToComplexValue(restriction));
         }
         catch (GDPREnabledException ex)
         {
            throw new ODataApplicationException(ex.getMessage(),
                  HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
         }
         catch (RootNotModifiableException ex)
         {
            throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.FORBIDDEN.getStatusCode(), Locale.ENGLISH);
         }
      }
      else if (action.getFullQualifiedName().equals(UnlockUserAction.ACTION_UNLOCK_USER_FQN))
      {
         ODataSecurityManager.checkPermission(Role.USER_MANAGER);
         String username = DataHandlerUtil.trimStringKeyParameter(keyPredicates.get(0));
         String uuid = (String) parameters.get(UnlockUserAction.PARAMETER_ID).getValue();
         try
         {
            User user = USER_SERVICE.getUserByName(username);
            String res = (USER_SERVICE.unlockUser(user, uuid)) ? "Success" : "Restriction '" + uuid + "' not found";
            return new Property(null, UnlockUserAction.ACTION_UNLOCK_USER, ValueType.PRIMITIVE, res);
         }
         catch (GDPREnabledException ex)
         {
            throw new ODataApplicationException(ex.getMessage(),
                  HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
         }
         catch (RootNotModifiableException ex)
         {
            throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.FORBIDDEN.getStatusCode(), Locale.ENGLISH);
         }
      }
      else if (action.getFullQualifiedName().equals(AddSearchAction.ACTION_ADD_SEARCH_FNQ))
      {
            String username = DataHandlerUtil.trimStringKeyParameter(keyPredicates.get(0));
            if (!allowedUser(username))
            {
               throw new ODataApplicationException("User is not allowed to perform this action",
                     HttpStatusCode.FORBIDDEN.getStatusCode(), Locale.ENGLISH);
            }
            Entity searchEntity = (Entity) parameters.get(AddSearchAction.PARAM_SEARCH).getValue();
            if (searchEntity == null)
            {
               throw new ODataApplicationException("Cannot have a null object",
                     HttpStatusCode.FORBIDDEN.getStatusCode(), Locale.ENGLISH);
            }
            User user = USER_SERVICE.getUserNoCheck(username);
            Property advanced = searchEntity.getProperty(SearchModel.PROPERTY_ADVANCED);
            Property complete = searchEntity.getProperty(SearchModel.PROPERTY_COMPLETE);
            Property search = searchEntity.getProperty(SearchModel.PROPERTY_VALUE);
            Property footprint = searchEntity.getProperty(SearchModel.PROPERTY_FOOTPRINT);
            Search s = USER_SERVICE.storeUserSearch(user.getUUID(), (String) search.getValue(), (String) footprint.getValue(),
                  processAdvancedProperty(advanced), (String) complete.getValue());
            if (s == null)
            {
               return new Property(null, AddSearchAction.ACTION_ADD_SEARCH_NAME, ValueType.PRIMITIVE, "User search '"+complete.getValue()+"' already exists");
            }
            Property notify = searchEntity.getProperty(SearchModel.PROPERTY_NOTIFY);
            try
            {
               USER_SERVICE.activateUserSearchNotification(s.getUUID(), (Boolean) notify.getValue());
            }
            catch (GDPREnabledException ex)
            {
               throw new ODataApplicationException(ex.getMessage(),
                     HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
            }
            return new Property(null, AddSearchAction.ACTION_ADD_SEARCH_NAME, ValueType.PRIMITIVE, "User search added");
      }
      else if (action.getFullQualifiedName().equals(EnableSearchAction.ACTION_ENABLE_SEARCH_FNQ))
      {
            String username = DataHandlerUtil.trimStringKeyParameter(keyPredicates.get(0));
            if (!allowedUser(username))
            {
               throw new ODataApplicationException("User is not allowed to perform this action",
                     HttpStatusCode.FORBIDDEN.getStatusCode(), Locale.ENGLISH);
            }
            Entity searchEntity = (Entity) parameters.get(AddSearchAction.PARAM_SEARCH).getValue();
            if (searchEntity == null)
            {
               throw new ODataApplicationException("Cannot have a null object",
                     HttpStatusCode.FORBIDDEN.getStatusCode(), Locale.ENGLISH);
            }
            Property uuid = searchEntity.getProperty(SearchModel.PROPERTY_ID);
            Property notify = searchEntity.getProperty(SearchModel.PROPERTY_NOTIFY);
            try
            {
               USER_SERVICE.activateUserSearchNotification((String) uuid.getValue(), (Boolean) notify.getValue());
            }
            catch (GDPREnabledException ex)
            {
               throw new ODataApplicationException(ex.getMessage(),
                     HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
            }
            return new Property(null, EnableSearchAction.ACTION_ENABLE_SEARCH_NAME, ValueType.PRIMITIVE, "User search "+(((Boolean)notify.getValue())?"enabled":"disabled"));
      }
      else if (action.getFullQualifiedName().equals(DeleteSearchAction.ACTION_DELETE_SEARCH_FNQ))
      {
            String username = DataHandlerUtil.trimStringKeyParameter(keyPredicates.get(0));
            if (!allowedUser(username))
            {
               throw new ODataApplicationException("User is not allowed to perform this action",
                     HttpStatusCode.FORBIDDEN.getStatusCode(), Locale.ENGLISH);
            }
            String searchId = (String) parameters.get(AddSearchAction.PARAM_SEARCH).getValue();
            if (searchId == null)
            {
               throw new ODataApplicationException("Cannot have a null object",
                     HttpStatusCode.FORBIDDEN.getStatusCode(), Locale.ENGLISH);
            }
            User user = USER_SERVICE.getUserNoCheck(username);
            USER_SERVICE.removeUserSearch(user.getUUID(), searchId);
            return new Property(null, DeleteSearchAction.ACTION_DELETE_SEARCH_NAME, ValueType.PRIMITIVE, "User search deleted");
      }
      else if (action.getFullQualifiedName().equals(ClearSearchesAction.ACTION_CLEAR_SEARCHES_FNQ))
      {
            String username = DataHandlerUtil.trimStringKeyParameter(keyPredicates.get(0));
            if (!allowedUser(username))
            {
               throw new ODataApplicationException("User is not allowed to perform this action",
                     HttpStatusCode.FORBIDDEN.getStatusCode(), Locale.ENGLISH);
            }
            User user = USER_SERVICE.getUserNoCheck(username);
            USER_SERVICE.clearSavedSearches(user.getUUID());
            return new Property(null, ClearSearchesAction.ACTION_CLEAR_SEARCHES_NAME, ValueType.PRIMITIVE, "User searches cleared");
      }
      throw new ODataApplicationException("Action not found", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
   }
   
   /**
    * This method verify if the current user has correct rights to perform action
    * @param username
    * @return
    * @throws RootNotModifiableException 
    */
   private boolean allowedUser(String username)
   {
      User currentUser = USER_SERVICE.getCurrentUserInformation();
      return currentUser.getUsername().equals(username) || ODataSecurityManager.hasPermission(Role.USER_MANAGER);
   }

   private HashMap<String, String> processAdvancedProperty(Property property)
   {
      if (property == null || !property.isCollection())
         return null;
      
      HashMap<String, String> map = new HashMap<String, String>();
      ArrayList<ComplexValue> advanced = (ArrayList<ComplexValue>) property.getValue();
      advanced.forEach(complexValue -> {
         List<Property> listValue = complexValue.getValue();
         String key = (String)listValue.get(0).getValue();
         String value = (String)listValue.get(1).getValue();
         map.put(key, value);
      });
      return map;
   }

   private User getUserFromParameters(List<UriParameter> keyParameters)
         throws ODataApplicationException
   {
      for (UriParameter keyParameter: keyParameters)
      {
         if (UserModel.PROPERTY_USERNAME.equals(keyParameter.getName()))
         {
            String username = DataHandlerUtil.trimStringKeyParameter(keyParameter);

            if (!ODataSecurityManager.getCurrentUser().getUsername().equals(username)
                  && !ODataSecurityManager.hasPermission(Role.USER_MANAGER))
            {
               throw new ODataApplicationException("Not allowed to access other users",
                     HttpStatusCode.FORBIDDEN.getStatusCode(), Locale.ENGLISH);
            }
            try
            {
               return USER_SERVICE.getUserByName(username);
            }
            catch (RootNotModifiableException ex)
            {
               throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.FORBIDDEN.getStatusCode(), Locale.ENGLISH);
            }
         }
      }
      return null;
   }
}
