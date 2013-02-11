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
package fr.gael.dhus.database.object;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "TRANSFORMATIONS")
public class Transformation implements Serializable
{
   private static final long serialVersionUID = 1L;

   @Id
   @Column(name = "UUID", nullable = false)
   private String uuid;

   @Temporal(TemporalType.TIMESTAMP)
   @Column(name = "CREATION_DATE", nullable = false)
   private Date creationDate = new Date();

   @Column(name = "TRANSFORMER", nullable = false)
   private String transformer;

   @Column(name = "PARAMETERS_HASH", nullable = false)
   private int hash;

   @Column(name = "PRODUCT_IN", nullable = false)
   private String productIn;

   @Column(name = "PRODUCT_OUT")
   private String productOut;

   @Column(name = "STATUS", nullable = false)
   private String status;

   @Column(name = "RESULT_URL")
   private String resultUrl;

   @Column(name = "DATA")
   private String data;

   @OneToMany(mappedBy="userKey.transformation", orphanRemoval=true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
   @OnDelete(action=OnDeleteAction.CASCADE)
   private Set<UserTransformation> userTransformations = new HashSet<UserTransformation>();


   public String getUuid()
   {
      return uuid;
   }

   public void setUuid(String uuid)
   {
      this.uuid = uuid;
   }

   public Date getCreationDate()
   {
      return creationDate;
   }

   public void setCreationDate(Date creationDate)
   {
      this.creationDate = creationDate;
   }

   public String getTransformer()
   {
      return transformer;
   }

   public void setTransformer(String transformer)
   {
      this.transformer = transformer;
   }

   public int getHash()
   {
      return hash;
   }

   public void setHash(int hash)
   {
      this.hash = hash;
   }

   public String getProductIn()
   {
      return productIn;
   }

   public void setProductIn(String productIn)
   {
      this.productIn = productIn;
   }

   public String getProductOut()
   {
      return productOut;
   }

   public void setProductOut(String productOut)
   {
      this.productOut = productOut;
   }

   public String getStatus()
   {
      return status;
   }

   public void setStatus(String status)
   {
      this.status = status;
   }

   public String getResultUrl()
   {
      return resultUrl;
   }

   public void setResultUrl(String resultUrl)
   {
      this.resultUrl = resultUrl;
   }

   public String getData()
   {
      return data;
   }

   public void setData(String data)
   {
      this.data = data;
   }

   public boolean addUser(User user)
   {
      return this.userTransformations.add(new UserTransformation(this, user));
   }

   public boolean removeUser(User user)
   {
      return this.userTransformations.remove(new UserTransformation(this, user));
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null)
      {
         return false;
      }
      if (!getClass().equals(obj.getClass()))
      {
         return false;
      }
      Transformation other = (Transformation) obj;
      if (uuid.equals(other.uuid))
      {
         return true;
      }
      return transformer.equals(other.transformer) && productIn.equals(other.productIn) && hash == other.hash;
   }

   @Override
   public int hashCode()
   {
      return transformer.hashCode() + (31 * productIn.hashCode()) + 31 * hash;
   }

   @Override
   public String toString()
   {
      return "Transformation: " + "(uuid: "+ uuid +" transformer: "+transformer+")";
   }
}
