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
package fr.gael.dhus.database.object;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.dhus.api.JobStatus;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "ORDERS")
public class Order implements Serializable
{
   private static final long serialVersionUID = 1L;

   @EmbeddedId
   private OrderId orderId;

   @Column(name = "JOB_ID", nullable = true)
   private String jobId;

   @Enumerated(EnumType.STRING)
   private JobStatus status;

   @Temporal(TemporalType.TIMESTAMP)
   @Column(name = "SUBMISSION_TIME", nullable = false)
   private Date submissionTime = new Date();

   @Temporal(TemporalType.TIMESTAMP)
   @Column(name = "ESTIMATED_TIME", nullable = true)
   private Date estimatedTime;

   @Column(name = "STATUS_MESSAGE", nullable = true)
   private String statusMessage;

   @OneToMany(mappedBy = "key.order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
   @OnDelete(action = OnDeleteAction.CASCADE)
   private Set<OrderOwner> owners = new HashSet<>();

   public Order() {}

   /**
    * Create new order instance.
    *
    * @param dataStoreName  DataStore owning the async product
    * @param productUuid    product UUID
    * @param jobId          job ID
    * @param jobStatus      job status, it can be completed, running or failed
    * @param submissionTime order creation date
    * @param estimatedDate  estimated completion date of the job, may be null
    * @param statusMessage  message from the DataStore explaining the current status of this Order
    */
   public Order(String dataStoreName, String productUuid, String jobId, JobStatus jobStatus, Date submissionTime, Date estimatedDate, String statusMessage)
   {
      this(new OrderId(dataStoreName, productUuid), jobId, jobStatus, submissionTime, estimatedDate, statusMessage);
   }

   public Order(OrderId orderId, String jobId, JobStatus jobStatus, Date submissionTime, Date estimatedTime, String statusMessage)
   {
      this.orderId = orderId;
      this.jobId = jobId;
      this.status = jobStatus;
      this.submissionTime = submissionTime;
      this.estimatedTime = estimatedTime;
      this.statusMessage = statusMessage;
   }

   public String getJobId()
   {
      return jobId;
   }

   public void setJobId(String jobId)
   {
      this.jobId = jobId;
   }

   public JobStatus getStatus()
   {
      return status;
   }

   public void setStatus(JobStatus status)
   {
      this.status = status;
   }

   public Date getSubmissionTime()
   {
      return submissionTime;
   }

   public void setSubmissionTime(Date submissionTime)
   {
      this.submissionTime = submissionTime;
   }

   public Date getEstimatedTime()
   {
      return estimatedTime;
   }

   public void setEstimatedTime(Date estimatedTime)
   {
      this.estimatedTime = estimatedTime;
   }

   public OrderId getOrderId()
   {
      return orderId;
   }

   public String getStatusMessage()
   {
      return statusMessage;
   }

   public void setStatusMessage(String statusMessage)
   {
      this.statusMessage = statusMessage;
   }

   /**
    * Adds an order owner.
    *
    * @param user to add (can't be null)
    * @return false if already is an owner, true otherwise
    */
   public boolean addOwner(User user)
   {
      return this.owners.add(new OrderOwner(this, user));
   }

   /**
    * Checks if the given user owns this order.
    *
    * @param user to check for ownership
    * @return true if the given user is an owner
    */
   public boolean hasOwner(User user)
   {
      return this.owners.contains(new OrderOwner(this, user));
   }

   public Set<OrderOwner> getOwners()
   {
      return this.owners;
   }
   
   @Override
   public boolean equals(Object o)
   {
      if (o == null)
      {
         return false;
      }
      if (this == o)
      {
         return true;
      }
      if (!(o instanceof Order))
      {
         return false;
      }
      Order other = (Order) o;
      if (this.orderId == null)
      {
         return false;
      }
      return this.orderId.equals(other.orderId);
   }

   @Override
   public int hashCode()
   {
      return Objects.hashCode(this.orderId);
   }

   @Override
   public String toString()
   {
      return "Order: (" + orderId + ")";
   }

   @Embeddable
   public static class OrderId implements Serializable
   {
      private static final long serialVersionUID = 1L;

      @Column(name = "DATASTORE_NAME", nullable = false)
      private String dataStoreName;
      @Column(name = "PRODUCT_UUID", nullable = false)
      private String productUuid;

      public OrderId() {}

      public OrderId(String dataStoreName, String productUuid)
      {
         this.dataStoreName = dataStoreName;
         this.productUuid = productUuid;
      }

      public String getDataStoreName()
      {
         return dataStoreName;
      }

      public void setDataStoreName(String dataStoreName)
      {
         this.dataStoreName = dataStoreName;
      }

      public String getProductUuid()
      {
         return productUuid;
      }

      public void setProductUuid(String productUuid)
      {
         this.productUuid = productUuid;
      }

      @Override
      public int hashCode()
      {
         int hash = Objects.hashCode(this.dataStoreName);
         return 17 * hash + Objects.hashCode(this.productUuid);
      }

      @Override
      public boolean equals(Object obj)
      {
         if (this == obj)
         {
            return true;
         }
         if (obj == null)
         {
            return false;
         }
         if (getClass() != obj.getClass())
         {
            return false;
         }
         final OrderId other = (OrderId) obj;
         if (!Objects.equals(this.dataStoreName, other.dataStoreName))
         {
            return false;
         }
         if (!Objects.equals(this.productUuid, other.productUuid))
         {
            return false;
         }
         return true;
      }

      @Override
      public String toString()
      {
         return "ProductUuid: " + productUuid + ", DataStoreName: " + dataStoreName;
      }
   }
}
