/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
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
package org.dhus.store.derived;

import org.dhus.Product;
import org.dhus.store.Store;
import org.dhus.store.StoreException;

/**
 * A DerivedProductStore is a means to store, access, and manage the physical data of derived products
 * such as Quicklooks and Thumbnails.
 */
public interface DerivedProductStore extends Store
{
   public static final String QUICKLOOK_TAG = "quicklook";
   public static final String THUMBNAIL_TAG = "thumbnail";

   /**
    * Adds a derived product to this DerivedProductStore.
    *
    * @param uuid    the UUID of the original product
    * @param tag     the tag under which the derived product is referenced
    * @param product the derived product
    * @throws StoreException
    */
   public void addDerivedProduct(String uuid, String tag, Product product) throws StoreException;

   /**
    * Deletes a derived product from this DerivedProductStore.
    *
    * @param uuid the UUID of the original product
    * @param tag  the tag under which the derived product is referenced
    * @throws StoreException
    */
   public void deleteDerivedProduct(String uuid, String tag) throws StoreException;

   /**
    * Deletes all the products that were derived from the original product of UUID uuid in this
    * DerivedProductStore.
    *
    * @param uuid the UUID of the original product
    * @throws StoreException
    */
   public void deleteDerivedProducts(String uuid) throws StoreException;

   /**
    * Returns a Product that was derived from an original of UUID productUuid according to a given tag.
    *
    * @param uuid the UUID of the Product from which the returned product was derived
    * @param tag  the tag under which the derived product is referenced
    * @return a derived Product
    */
   public Product getDerivedProduct(String uuid, String tag) throws StoreException;

   /**
    * Returns true if this DerivedProductStore has a derived product referenced under the tag,
    * tag that was derived from the original product of UUID {@code uuid}, false otherwise.
    *
    * @param uuid the UUID of the original product
    * @param tag  the tag under which the derived product is referenced
    * @return true if the derived product is found, false otherwise
    * @throws StoreException
    */
   public boolean hasDerivedProduct(String uuid, String tag);

   /**
    * Adds a reference to a derived product to this DerivedProductStore.
    *
    * @param uuid    the UUID of the original product
    * @param tag     the tag under which the derived product is referenced
    * @param product the derived product
    * @return true if the product is added, otherwise false
    * @throws StoreException on error
    */
   public boolean addDerivedProductReference(String uuid, String tag, Product product) throws StoreException;

}
