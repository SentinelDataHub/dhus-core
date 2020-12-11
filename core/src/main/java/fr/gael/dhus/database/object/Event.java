/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017,2019 GAEL Systems
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "EVENT")
public class Event implements Serializable
{
   private static final long serialVersionUID = 1474091135291618625L;

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   @Column(name = "ID")
   private Long id;

   @Column(name = "CATEGORY", nullable = false)
   @Enumerated(EnumType.STRING)
   private EventCategory category;

   @Column(name = "SUBCATEGORY", nullable = true, length = 128)
   private String subcategory;

   @Column(name = "TITLE", nullable = false, length = 255)
   private String title;

   @Column(name = "DESCRIPTION", nullable = false, length = 1024)
   private String description;

   @Temporal(TemporalType.TIMESTAMP)
   @Column(name = "START_DATE", nullable = false)
   private Date startDate;

   @Temporal(TemporalType.TIMESTAMP)
   @Column(name = "STOP_DATE", nullable = true)
   private Date stopDate;

   @Temporal(TemporalType.TIMESTAMP)
   @Column(name = "PUBLICATION_DATE", nullable = true)
   private Date publicationDate = new Date();

   @Column(name = "ICON", nullable = true, length = 1024)
   private String icon;

   @Column(name = "LOCAL_EVENT", columnDefinition = "BOOLEAN", nullable = false)
   private boolean localEvent = true;

   @Column(name = "PUBLIC_EVENT", columnDefinition = "BOOLEAN", nullable = false)
   private boolean publicEvent = true;

   @Column(name = "ORIGINATOR", nullable = true, length = 128)
   private String originator;

   @Column(name = "HUB_TAG", nullable = true, length = 255)
   private String hubTag;

   @Column(name = "MISSION_TAG", nullable = true, length = 255)
   private String missionTag;

   @Column(name = "INSTRUMENT_TAG", nullable = true, length = 255)
   private String instrumentTag;

   @Column(name = "EXTERNAL_URL", nullable = true, length = 1024)
   private String externalUrl;

   public Event() {}

   public Long getId()
   {
      return id;
   }

   public void setId(Long id)
   {
      this.id = id;
   }

   public EventCategory getCategory()
   {
      return category;
   }

   public void setCategory(EventCategory category)
   {
      this.category = category;
   }

   public String getSubcategory()
   {
      return subcategory;
   }

   public void setSubcategory(String subcategory)
   {
      this.subcategory = subcategory;
   }

   public String getTitle()
   {
      return title;
   }

   public void setTitle(String title)
   {
      this.title = title;
   }

   public String getDescription()
   {
      return description;
   }

   public void setDescription(String description)
   {
      this.description = description;
   }

   public Date getStartDate()
   {
      return startDate;
   }

   public void setStartDate(Date startDate)
   {
      this.startDate = startDate;
   }

   public Date getStopDate()
   {
      return stopDate;
   }

   public void setStopDate(Date stopDate)
   {
      this.stopDate = stopDate;
   }

   public Date getPublicationDate()
   {
      return publicationDate;
   }

   public void setPublicationDate(Date publicationDate)
   {
      this.publicationDate = publicationDate;
   }

   public String getIcon()
   {
      return icon;
   }

   public void setIcon(String icon)
   {
      this.icon = icon;
   }

   public boolean isLocalEvent()
   {
      return localEvent;
   }

   public void setLocalEvent(boolean localEvent)
   {
      this.localEvent = localEvent;
   }

   public boolean isPublicEvent()
   {
      return publicEvent;
   }

   public void setPublicEvent(boolean publicEvent)
   {
      this.publicEvent = publicEvent;
   }

   public String getOriginator()
   {
      return originator;
   }

   public void setOriginator(String originator)
   {
      this.originator = originator;
   }

   public String getHubTag()
   {
      return hubTag;
   }

   public void setHubTag(String hubTag)
   {
      this.hubTag = hubTag;
   }

   public String getMissionTag()
   {
      return missionTag;
   }

   public void setMissionTag(String missionTag)
   {
      this.missionTag = missionTag;
   }

   public String getInstrumentTag()
   {
      return instrumentTag;
   }

   public void setInstrumentTag(String instrumentTag)
   {
      this.instrumentTag = instrumentTag;
   }

   public String getExternalUrl()
   {
      return externalUrl;
   }

   public void setExternalUrl(String externalUrl)
   {
      this.externalUrl = externalUrl;
   }

   public static enum EventCategory implements Serializable
   {
      Satellite,
      GroundSegment,
      Product,
      Other;
   }
}
