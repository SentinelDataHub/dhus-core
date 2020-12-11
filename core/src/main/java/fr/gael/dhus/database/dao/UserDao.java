/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013-2020 GAEL Systems
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
package fr.gael.dhus.database.dao;

import fr.gael.dhus.database.dao.interfaces.DaoEvent;
import fr.gael.dhus.database.dao.interfaces.DaoListener;
import fr.gael.dhus.database.dao.interfaces.DaoUtils;
import fr.gael.dhus.database.dao.interfaces.HibernateDao;
import fr.gael.dhus.database.dao.interfaces.UserListener;
import fr.gael.dhus.database.object.Preference;
import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.Search;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.database.object.restriction.AccessRestriction;
import fr.gael.dhus.database.object.restriction.LockedAccessRestriction;
import fr.gael.dhus.database.object.restriction.TmpUserLockedAccessRestriction;
import fr.gael.dhus.service.exception.UserAlreadyExistingException;
import fr.gael.dhus.system.config.ConfigurationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.hibernate.HibernateException;
import org.hibernate.query.Query;
import org.hibernate.query.NativeQuery;
import org.hibernate.Session;

import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserDao extends HibernateDao<User, String>
{
   @Autowired
   private ConfigurationManager cfgManager;

   @Autowired
   private ProductCartDao productCartDao;

   @Autowired
   private SearchDao searchDao;

   @Autowired
   private AccessRestrictionDao accessRestrictionDao;

   /* Log. */
   private final Logger LOGGER = LogManager.getLogger();

   public User getByName (final String name)
   {
      User user = getHibernateTemplate().execute(session -> {
         Query query = session.createQuery("From User u where u.username=?1");
         query.setParameter(1, name, StandardBasicTypes.STRING);
         return (User) query.uniqueResult();
      });

      // Optimization user extraction: most of the users uses case-sensitive
      // match for the login. A Requirement of the project asked for non-case
      // sensitive match. The extraction of non-case sensitive login from
      // database requires conversions and forbid the usage of indexes, so it
      // is much more slow.
      // This Fix aims to first try the extraction of the user with exact match
      // equals operator, then if not match use the toLower conversion.
      if (user==null)
      {
         user = getHibernateTemplate().execute(session -> {
            Query query = session.createQuery("From User u where LOWER(u.username)=?1");
            query.setParameter(1, name.toLowerCase(), StandardBasicTypes.STRING);
            return (User) query.uniqueResult();
         });
      }
      return user;
   }

   @Override
   public void delete (final User user)
   {
      if (user == null) return;

      // remove user external references
      final String uid = user.getUUID ();
      productCartDao.deleteCartOfUser (user);
      getHibernateTemplate ().execute (new HibernateCallback<Void> ()
      {
         @Override
         public Void doInHibernate (Session session)
               throws HibernateException
         {
            String sql = "DELETE FROM NETWORK_USAGE WHERE USER_UUID = :uid";
            NativeQuery query = session.createNativeQuery (sql);
            query.setParameter("uid", uid, StandardBasicTypes.STRING);
            query.executeUpdate ();
            return null;
         }
      });

      super.delete (user);
   }
   
   private void forceDelete (User user)
   {
      super.delete (read (user.getUUID ()));
   }

   @SuppressWarnings ("unchecked")
   public List<User> readNotAdmin ()
   {
      return (List<User>)find (
         "FROM " + entityClass.getName () + " u WHERE not u.username='" +
            cfgManager.getAdministratorConfiguration ().getName () + "' " +
            "order by username");
   }

   public Iterator<User> iterate (final String filter, int skip)
   {
      StringBuilder query = new StringBuilder ();
      query.append ("FROM ").append (entityClass.getName ()).append (" ");
      query.append ("WHERE username LIKE'%").append (filter).append ("%' AND ");
      query.append ("not username='").append (getRootUser ().getUsername ())
            .append ("' ");
      query.append ("ORDER BY username");
      return new PagedIterator<> (this, query.toString (), skip, 3);
   }
   
   public Iterator<User> scrollAll (String filter, int skip)
   {
      StringBuilder query = new StringBuilder ();
      query.append ("FROM ").append (entityClass.getName ()).append (" ");
      query.append ("WHERE username LIKE '%").append (filter).append ("%' ");
      query.append ("ORDER BY username");
      return new PagedIterator<> (this, query.toString (), skip);
   }
   
   public int countNotAdmin (String filter)
   {
      return DataAccessUtils.intResult (find (
         "select count(*) FROM " + entityClass.getName () +
            " u WHERE u.username LIKE '%" + filter +
            "%' and " + "not u.username='" +
            cfgManager.getAdministratorConfiguration ().getName () + "'"));
   }
   
   public int countAll (String filter)
   {
      return DataAccessUtils.intResult (find (
         "select count(*) FROM " + entityClass.getName () +
            " u WHERE u.username LIKE '%" + filter + "%'"));
   }

   public String computeUserCode (User user)
   {
      if (user == null) throw new NullPointerException ("Null user.");

      if (user.getUUID () == null)
         throw new IllegalArgumentException ("User " + user.getUsername () +
            " must be created in the DB to compute its code.");

      String digest = user.hash ();

      String code = user.getUUID () + digest;

      return code;
   }

   public User getUserFromUserCode (String code)
   {
      if (code == null) throw new NullPointerException ("Null code.");

      String id = code.substring (0, 36);

      // Retrieve the user
      User user = read (id);

      if (user == null)
         throw new NullPointerException ("User cannot be retrieved for id " +
            id);

      // Check the Id
      String hash = user.hash ();
      String user_hash = code.substring (36);

      if ( !hash.equals (user_hash))
         throw new SecurityException ("Wrong hash code \"" + user_hash + "\".");

      return user;
   }

   public void lockUser (User user, String reason)
   {
      LockedAccessRestriction ar = new LockedAccessRestriction ();
      ar.setBlockingReason (reason);

      user.addRestriction (ar);
      update (user);
   }

   public void unlockUser (User user, Class<? extends AccessRestriction> car)
   {
      if (user.getRestrictions () == null) return;

      Iterator<AccessRestriction> iter = user.getRestrictions ().iterator ();
      HashSet<AccessRestriction> toDelete = new HashSet<AccessRestriction> ();
      while (iter.hasNext ())
      {
         AccessRestriction lar = iter.next ();
         if (lar.getClass ().equals (car))
         {
            iter.remove ();
            toDelete.add (lar);
         }
      }
      update (user);

      for (AccessRestriction restriction : toDelete)
      {
         accessRestrictionDao.delete (restriction);
      }
   }

   /**
    * Create a temporary user.
    * 
    * @param temporary user.
    * @return the updated user.
    */
   public void createTmpUser (User user)
   {
      TmpUserLockedAccessRestriction tuar =
         new TmpUserLockedAccessRestriction ();
      user.addRestriction (tuar);
      create (user);
   }

   @Override
   public User create (User u)
   {
      User user = getByName (u.getUsername ());
      if (user != null)
      {
         throw new UserAlreadyExistingException (
            "An user is already registered with name '" + u.getUsername () +
               "'.");
      }
      // Default new user come with at least search access role.
      if (u.getRoles ().isEmpty ())
      {
         u.addRole (Role.SEARCH);
         u.addRole (Role.DOWNLOAD);
      }
      return super.create (u);
   }

   /**
    * Create a row in database for the given user, do not fail if user has no email.
    *
    * @param user a User to store (non null)
    * @return stored user
    */
   public User createWithoutMail(User user)
   {
      User test_exists = getByName(user.getUsername());
      if (test_exists != null)
      {
         throw new UserAlreadyExistingException("An user is already registered with name '"
               + user.getUsername() + "'.");
      }
      // Default new user come with at least search access role.
      if (user.getRoles().isEmpty())
      {
         user.addRole(Role.SEARCH);
         user.addRole(Role.DOWNLOAD);
      }

      long start = System.currentTimeMillis();
      String id = (String) getHibernateTemplate().save(user);
      user = getHibernateTemplate().<User>get(User.class, id);
      long end = System.currentTimeMillis();
      LOGGER.info("Create/save {} ({}) spent {}ms", entityClass.getSimpleName(), id, (end - start));

      return user;
   }

   public void registerTmpUser (User u)
   {
      unlockUser (u, TmpUserLockedAccessRestriction.class);
      fireUserRegister (new DaoEvent<User> (u));
   }

   public boolean isTmpUser (User u)
   {
      if (u.getRestrictions () == null)
      {
         return false;
      }
      for (AccessRestriction ar : u.getRestrictions ())
      {
         if (ar instanceof TmpUserLockedAccessRestriction)
         {
            return true;
         }
      }
      return false;
   }

   public void cleanupTmpUser (int max_days)
   {
      int skip = 0;
      final int top = DaoUtils.DEFAULT_ELEMENTS_PER_PAGE;
      long MILLISECONDS_PER_DAY = 1000 * 60 * 60 * 24;
      long runtime = System.currentTimeMillis ();
      final String hql = "SELECT u, r FROM User u LEFT OUTER JOIN " +
         "u.restrictions r WHERE r.discriminator = 'temporary'";
      List<Object[]> result;
      HibernateTemplate template = getHibernateTemplate ();
      do
      {
         final int start = skip;
         result = template.execute (new HibernateCallback<List<Object[]>>()
         {
            @Override
            @SuppressWarnings ("unchecked")
            public List<Object[]> doInHibernate (Session session)
               throws HibernateException
            {
               Query query = session.createQuery (hql).setReadOnly (true);
               query.setFirstResult (start);
               query.setMaxResults (top);
               return (List<Object[]>) query.list ();
            }
         });
         for (Object[] objects : result)
         {
            if (objects.length != 2) continue;
            
            User user = User.class.cast (objects[0]);
            TmpUserLockedAccessRestriction restriction = 
                     TmpUserLockedAccessRestriction.class.cast (objects[1]);
            
            long date = runtime - restriction.getLockDate ().getTime ();
            if ((date / MILLISECONDS_PER_DAY) >= max_days)
            {
               logger.info("Remove unregistered User " + user.getUsername ());
               forceDelete (user);
            }
         }
         skip = skip + top;
      }
      while (result.size () == top);
   }

   public User getRootUser()
   {
      return getByName (cfgManager.getAdministratorConfiguration ().getName ());
   }
   
   public boolean isRootUser (User user)
   {
      if (user.getUsername ().equals (
         cfgManager.getAdministratorConfiguration ().getName ())) return true;
      return false;
   }

   void fireUserRegister (DaoEvent<User> e)
   {
      for (DaoListener<?> listener : getListeners ())
      {
         if (listener instanceof UserListener)
            ((UserListener) listener).register (e);
      }
   }

   // Preference settings
   private void updateUserPreference (User user)
   {
      getHibernateTemplate ().update (user);
   }

   public void storeUserSearch (User user, String request, String footprint,
         HashMap<String, String> advanced, String complete)
   {
      Preference pref = user.getPreferences ();
      Search search = new Search();
      search.setValue (request);
      search.setFootprint (footprint);
      search.setAdvanced (advanced);
      search.setComplete (complete);
      search.setNotify (false);
      search = searchDao.create (search);
      pref.getSearches ().add (search);
      updateUserPreference (user);
   }

   public void removeUserSearch (User user, String uuid)
   {
      Search search = searchDao.read(uuid);
      if (search != null)
      {
         Preference pref = user.getPreferences ();
         Set<Search> s = pref.getSearches ();
         Iterator<Search> iterator = s.iterator ();
         while (iterator.hasNext ())
         {
            if (iterator.next ().equals (search))
            {
               iterator.remove ();
            }
         }
         updateUserPreference (user);
      }
      searchDao.delete (search);
   }

   public void activateUserSearchNotification (String uuid, boolean notify)
   {
      Search search = searchDao.read (uuid);
      search.setNotify (notify);
      searchDao.update (search);
   }
   
   public void clearUserSearches (User user)
   {
      Preference pref = user.getPreferences ();
      pref.getSearches ().clear ();
      updateUserPreference (user);
   }

   public List<Search> getUserSearches (User user)
   {
      Set<Search> searches = read (user.getUUID ()).getPreferences ().
            getSearches ();
      List<Search> list = new ArrayList<Search> (searches);
      Collections.sort (list, new Comparator<Search> ()
      {
         @Override
         public int compare (Search arg0, Search arg1)
         {
            return arg0.getValue ().compareTo (arg1.getValue ());
         }
      });      
      return list;
   }

   /**
    *  Get users for the given filter, offset and limit
    * @param filter
    * @param offset
    * @param limit
    * @return
    */
   public Iterator<User> getUsersByFilter (String filter, int skip)
   {
      String s = filter.toLowerCase ();
      StringBuilder sb = new StringBuilder ();
      sb.append ("FROM ").append (entityClass.getName ()).append (" ");
      sb.append ("WHERE (username LIKE '%").append (s).append ("%' ")
            .append ("OR lower(firstname) LIKE '%").append (s).append ("%' ")
            .append ("OR lower(lastname) LIKE '%").append (s).append ("%' ")
            .append ("OR lower(email) LIKE '%").append (s).append ("%') ");
      sb.append ("AND not username='")
            .append (cfgManager.getAdministratorConfiguration ().getName ())
            .append ("' ");
      sb.append ("ORDER BY username");
      return new PagedIterator<> (this, sb.toString (), skip);
   }
   
   public int countByFilter (String filter)
   {
      return DataAccessUtils.intResult (find (
         "select count(*) FROM " + entityClass.getName () +
            " u WHERE (u.username LIKE '%" + filter +
         "%'  OR lower(u.firstname) LIKE '%"+filter.toLowerCase()+ "%'  OR lower(u.lastname) LIKE '%"+filter.toLowerCase()+
         "%'  OR lower(u.email) LIKE '%"+filter.toLowerCase()+ "%') and not u.username='" +
            cfgManager.getAdministratorConfiguration ().getName () + "'"));
   }

   public Iterator<User> getAllUsers ()
   {
      return new PagedIterator<> (this, "FROM " + entityClass.getName ());
   }
}

