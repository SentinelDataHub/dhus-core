/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019,2020 GAEL Systems
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

import fr.gael.dhus.database.dao.TransformationDao;
import fr.gael.dhus.database.object.Transformation;
import fr.gael.dhus.database.object.User;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.olingo.server.api.ODataApplicationException;

import org.dhus.api.JobStatus;
import org.dhus.olingo.v2.visitor.TransformationSQLVisitor;
import org.dhus.transformation.TransformationStatusUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransformationService
{
   @Autowired
   private TransformationDao transformationDao;

   /**
    * Called on success by the transformer, update the transformation's status accordingly.
    *
    * @param transformationUUID of transformation that succeed
    * @param resultUrl URL to the transformed product (transformation result)
    */
   @Transactional
   public void onTransformationSuccess(String transformationUUID, String resultUrl)
   {
      Transformation transformation = transformationDao.read(transformationUUID);
      transformation.setStatus(JobStatus.COMPLETED.toString());
      transformation.setResultUrl(resultUrl);
      transformationDao.update(transformation);
   }

   /**
    * Called on failure by the transformer, update the transformation's status accordingly.
    *
    * @param transformationUUID of transformation that failed
    */
   @Transactional
   public void onTransformationFailure(String transformationUUID)
   {
      Transformation transformation = transformationDao.read(transformationUUID);
      transformation.setStatus(JobStatus.FAILED.toString());
      transformationDao.update(transformation);
   }

   /**
    * Find a transformation by its transformer name, input product UUID and parameter hash.
    *
    * @param transformerName a non null transformer name
    * @param productUuid a non null Product UUID
    * @param hash parameter hashes
    * @return a transformation if it is found, otherwise null
    */
   @Transactional(readOnly = true)
   public Transformation findTransformation(String transformerName, String productUuid, int hash)
   {
      return transformationDao.findTransformation(transformerName, productUuid, hash);
   }

   /**
    * Retrieves a transformation execution by its id.
    *
    * @param transformationUUID UUID of transformation to get
    * @return a transformation if it is found, otherwise null
    */
   @Transactional(readOnly = true)
   public Transformation getTransformation(String transformationUUID)
   {
      return transformationDao.read(transformationUUID);
   }

   /**
    * Returns all transformations whose transformer name is set to the given name parameter.
    *
    * @param transformerName a non null transformer name
    * @return a non null, possibly empty, list of transformations
    */
   @Transactional(readOnly = true)
   public List<Transformation> getTransformationsOf(final String transformerName)
   {
      return transformationDao.findTransformationsOf(transformerName);
   }

   /**
    * Returns a transformation by its UUID, if it has the given transformer name.
    *
    * @param transformerName a non-null transformer name
    * @param transformationUUID of transformation to get
    * @return a transformation if it is found, and has the requested transformer, otherwise null
    */
   @Transactional(readOnly = true)
   public Transformation getTransformationOf(final String transformerName, String transformationUUID)
   {
      return transformationDao.findTransformationOf(transformerName, transformationUUID);
   }

   /**
    * Remove the transformation that produced the given product.
    *
    * @param productUUID non-null UUID of output product
    */
   @Transactional
   public void removeTransformationByOutputProduct(String productUUID)
   {
      Transformation trf = transformationDao.findTransformationByOutput(productUUID);
      if (trf != null)
      {
         transformationDao.delete(trf);
      }
   }

   /**
    * Persist the given transformation.
    *
    * @param transformation to create
    * @return the created transformation
    */
   @Transactional
   public Transformation create(Transformation transformation)
   {
      return transformationDao.create(transformation);
   }

   /**
    * Returns all transformations whose status is equals to the given String.
    *
    * @param status of the transformations to get
    * @return a non null, possibly empty, list of transformations
    */
   @Transactional(readOnly = true)
   public List<Transformation> getTransformationsByStatus(String status)
   {
      return transformationDao.getTransformationsByStatus(status);
   }

   /**
    * Delete the given transformation from the persistence layer.
    *
    * @param transformation to delete
    */
   @Transactional
   public void delete(Transformation transformation)
   {
      if (transformation != null)
      {
         transformationDao.delete(transformation);
      }
   }

   /**
    * Update a transformation with the given information.
    *
    * @see TransformationStatusUtil
    *
    * @param transformationUUID a non null UUID
    * @param status a non-null arbitrary string (usually a one-word status like: running, pending, done, failed, ...)
    * @param resultUrl URL to the result of the transformation process
    * @param productOutUUID UUID of the output product produced by the transformation (may be null)
    */
   @Transactional
   public void updateStatus(String transformationUUID, String status, String resultUrl, String productOutUUID, String data)
   {
      Transformation transformationInfo = transformationDao.read(transformationUUID);
      transformationInfo.setStatus(status);
      transformationInfo.setResultUrl(resultUrl);
      transformationInfo.setProductOut(productOutUUID);
      transformationInfo.setData(data);
      transformationDao.update(transformationInfo);
   }

   /**
    * Count transformations whose status is set to PENDING (TransformationStatusUtil.PENDING).
    *
    * @return number of running transformations [0, MAXINT[
    */
   @Transactional(readOnly = true)
   public int countPendingTransformations()
   {
      return transformationDao.countTransformationsByStatus(TransformationStatusUtil.PENDING);
   }

   /**
    * Count transformations whose status is set to RUNNING (TransformationStatusUtil.RUNNING).
    *
    * @return number of running transformations [0, MAXINT[
    */
   @Transactional(readOnly = true)
   public int countRunningTransformations()
   {
      return transformationDao.countTransformationsByStatus(TransformationStatusUtil.RUNNING);
   }

   /**
    * Delete a transformation by its UUID.
    *
    * @param transformationUUID UUID of transformation to delete
    */
   @Transactional
   public void deleteTransformationByUuid(String transformationUUID)
   {
      transformationDao.deleteTransformationByUuid(transformationUUID);
   }

   /**
    * Add a user to the given transformation.
    *
    * @param transformation to update
    * @param user to add
    * @throws NullPointerException if transformation or user is null
    */
   @Transactional
   public void addUser(Transformation transformation, User user)
   {
      Objects.requireNonNull(transformation);
      Objects.requireNonNull(user);
      transformation = transformationDao.findTransformationByUuid(transformation.getUuid());
      transformation.addUser(user);
      transformationDao.update(transformation);
   }

   /**
    * Remove a user from the given transformation.
    *
    * @param transformation to update
    * @param user to remove
    * @throws NullPointerException if transformation or user is null
    */
   @Transactional
   public void removeUser(Transformation transformation, User user)
   {
      Objects.requireNonNull(transformation);
      Objects.requireNonNull(user);
      transformation = transformationDao.findTransformationByUuid(transformation.getUuid());
      transformation.removeUser(user);
      transformationDao.update(transformation);
   }

   /**
    * Returns a list of Transformations matching the given OData expression.
    *
    * @param transformationSQLVisitor a non null expression visitor
    * @return a non null, possibly empty, list of Transformations
    * @throws ODataApplicationException while visiting the OData expression
    */
   @Transactional(readOnly = true)
   public List<Transformation> getTransformations(TransformationSQLVisitor transformationSQLVisitor)
         throws ODataApplicationException
   {
      return getTransformationsOfUser(transformationSQLVisitor, null);
   }

   /**
    * Returns a list of Transformations matching the given OData expression and owned by the given user.
    * Returns all Transformations if the `user` parameter is null.
    *
    * @param visitor a non null expression visitor
    * @param user the owner (may be null)
    * @return a non null, possibly empty, list of Transformations
    * @throws ODataApplicationException while visiting the OData expression
    */
   @Transactional(readOnly = true)
   public List<Transformation> getTransformationsOfUser(TransformationSQLVisitor visitor, User user)
         throws ODataApplicationException
   {
      if (visitor.getTop() <= 0)
      {
         return Collections.emptyList();
      }

      String joinClause = "";
      if (user != null)
      {
         joinClause = " inner join trf.userTransformations ut with ut.userKey.user.uuid = '" + user.getUUID() + "' ";
      }
      String hql = prepareHQLQuery(visitor.getHqlPrefix() + joinClause, visitor.getHqlFilter(), visitor.getHqlOrder());
      return transformationDao.executeHQLQuery(hql, visitor.getHqlParameters(), visitor.getSkip(), visitor.getTop());
   }

   private String prepareHQLQuery(String hqlPrefix, String hqlFilter, String hqlOrder)
   {
      StringBuilder sb = new StringBuilder(hqlPrefix);

      if (hqlFilter != null && !hqlFilter.isEmpty())
      {
         sb.append(" WHERE ").append(hqlFilter);
      }

      if (hqlOrder != null && !hqlOrder.isEmpty())
      {
         sb.append(" ORDER BY ").append(hqlOrder);
      }

      return sb.toString();
   }

   /**
    * Count Transformations matching the given OData expression.
    *
    * @param transformationSQLVisitor a non null expression visitor
    * @return the number of Transformations matching the OData expression
    * @throws ODataApplicationException while visiting the OData expression
    */
   @Transactional(readOnly = true)
   public int countTransformations(TransformationSQLVisitor transformationSQLVisitor) throws ODataApplicationException
   {
     return countTransformationsOfUser(transformationSQLVisitor, null);
   }

   /**
    * Count Transformations matching the given OData expression and owned by the given user.
    * Counts all Transformations if the `user` parameter is null.
    *
    * @param visitor a non null expression visitor
    * @param user the owner (may be null)
    * @return the number of Transformations matching the OData expression
    * @throws ODataApplicationException while visiting the OData expression
    */
   @Transactional(readOnly = true)
   public int countTransformationsOfUser(TransformationSQLVisitor visitor, User user) throws ODataApplicationException
   {
      StringBuilder sb = new StringBuilder(visitor.getFromClause());
      if (user != null)
      {
         sb.append(" inner join trf.userTransformations ut with ut.userKey.user.uuid = '").append(user.getUUID()).append("' ");
      }

      if (visitor.getHqlFilter() != null)
      {
         sb.append(" where ").append(visitor.getHqlFilter());
      }
      return transformationDao.countHQLQuery(sb.toString(), visitor.getHqlParameters());
   }
}
