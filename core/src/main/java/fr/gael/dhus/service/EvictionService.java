/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2016 GAEL Systems
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.joda.time.DateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.gael.dhus.database.dao.interfaces.IEvictionDao;
import fr.gael.dhus.database.object.Eviction;
import fr.gael.dhus.database.object.Product;
import fr.gael.dhus.datastore.eviction.EvictionManager;
import fr.gael.dhus.datastore.eviction.EvictionStrategy;

@Service
public class EvictionService extends WebService
{
   private static final Logger LOGGER = LogManager.getLogger();

   @Autowired
   private EvictionManager evictionMgr;

   @Autowired
   private IEvictionDao evictionDao;

   @Autowired
   private ProductService productService;

   private static Eviction eviction;

   @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
   public Eviction getEviction()
   {
      if (EvictionService.eviction == null)
      {
         EvictionService.eviction = evictionDao.getEviction();
      }
      return EvictionService.eviction;
   }

   @PreAuthorize("hasRole('ROLE_SYSTEM_MANAGER')")
   @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
   public int getKeepPeriod()
   {
      return getEviction().getKeepPeriod();
   }

   @PreAuthorize("hasRole('ROLE_SYSTEM_MANAGER')")
   @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
   public int getMaxDiskUsage()
   {
      return getEviction().getMaxDiskUsage();
   }

   @PreAuthorize("hasRole('ROLE_SYSTEM_MANAGER')")
   @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
   public EvictionStrategy getStrategy()
   {
      return getEviction().getStrategy();
   }

   @PreAuthorize("hasRole('ROLE_SYSTEM_MANAGER')")
   @Transactional
   public void save(EvictionStrategy strategy, int keep_period, int max_disk_usage)
   {
      Eviction eviction = getEviction();

      // save/update eviction view
      eviction.setStrategy(strategy);
      eviction.setKeepPeriod(keep_period);
      eviction.setMaxDiskUsage(max_disk_usage);

      // save/update persistent eviction
      evictionDao.update(strategy, keep_period, max_disk_usage);
   }

   /**
    * Returns a defensive copy of evictable products list.
    *
    * @return an evictable products list.
    */
   @PreAuthorize("hasRole('ROLE_SYSTEM_MANAGER')")
   public List<Product> getEvictableProducts()
   {
      return new ArrayList<>(getEviction().getProducts());
   }

   @Transactional
   public void doEvict()
   {
      computeNextProducts();
      evictionMgr.doEvict(getEviction().getProducts());
   }

   /**
    * Computes the date <i>days</i> days ago.
    *
    * @param days number of days
    * @return a date representation of date <i>days</i> ago.
    */
   public Date getKeepPeriod(int days)
   {
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.DAY_OF_YEAR, -days);
      LOGGER.info("Eviction Max date : {}", cal.getTime());
      return cal.getTime();
   }

   /**
    * Compute of next evictable products
    */
   @Transactional
   public void computeNextProducts()
   {
      Eviction eviction = getEviction();
      if (eviction == null)
      {
         LOGGER.warn("No Eviction setting found");
         return;
      }

      Set<Product> evictProduct = eviction.getProducts();
      evictProduct.clear();

      Date deadline = getKeepPeriod(eviction.getKeepPeriod());
      Iterator<Product> it = productService.getProductsByIngestionDate(deadline, eviction.getMaxProductNumber());
      while (it.hasNext())
      {
         evictProduct.add(it.next());
      }

      evictionDao.setProducts(evictProduct);
      LOGGER.info("Found {} product(s) to evict", evictProduct.size());
   }

   /**
    * Returns the next evictable products.
    *
    * @return a set of {@link Product}.
    */
   public Set<Product> getProducts()
   {
      return getEviction().getProducts();
   }

   @PreAuthorize ("isAuthenticated()")
   @Transactional (readOnly=true, propagation=Propagation.REQUIRED)
   public Date getEvictionDate(Product product)
   {
      Eviction e = getEviction();
      if (e.getStrategy() == EvictionStrategy.NONE)
      {
         return null;
      }

      DateTime dt = new DateTime(product.getIngestionDate());
      DateTime res = dt.plusDays(e.getKeepPeriod());
      return res.toDate();
   }

   // Methods for unit tests
   void setEvictionDao(IEvictionDao eviction_dao)
   {
      this.evictionDao = eviction_dao;
   }

   void setEvictionMgr(EvictionManager eviction_mgr)
   {
      this.evictionMgr = eviction_mgr;
   }

   void setProductService(ProductService productService)
   {
      this.productService = productService;
   }
}
