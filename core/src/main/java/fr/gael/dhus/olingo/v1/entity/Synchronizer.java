/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2015-2019 GAEL Systems
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
package fr.gael.dhus.olingo.v1.entity;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

import fr.gael.dhus.database.object.config.synchronizer.ProductSynchronizer;
import fr.gael.dhus.olingo.v1.Expander;
import fr.gael.dhus.olingo.v1.ExpectedException;
import fr.gael.dhus.olingo.v1.ExpectedException.IncompleteDocException;
import fr.gael.dhus.olingo.v1.ExpectedException.InvalidTargetException;
import fr.gael.dhus.olingo.v1.ExpectedException.InvalidValueException;
import fr.gael.dhus.olingo.v1.ExpectedException.NoTargetException;
import fr.gael.dhus.olingo.v1.Model;
import fr.gael.dhus.olingo.v1.Navigator;
import fr.gael.dhus.olingo.v1.entityset.SynchronizerEntitySet;
import fr.gael.dhus.service.CollectionService;
import fr.gael.dhus.service.ISynchronizerService;
import fr.gael.dhus.service.exception.InvokeSynchronizerException;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.sync.SynchronizerStatus;
import fr.gael.dhus.util.XmlProvider;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.rt.RuntimeDelegate;
import org.apache.olingo.odata2.api.uri.KeyPredicate;
import org.apache.olingo.odata2.api.uri.NavigationSegment;
import org.apache.olingo.odata2.api.uri.PathSegment;
import org.apache.olingo.odata2.api.uri.UriInfo;
import org.apache.olingo.odata2.api.uri.UriParser;
import org.apache.olingo.odata2.api.uri.info.DeleteUriInfo;

import org.quartz.CronExpression;

/**
 * Synchronizer OData Entity.
 */
public final class Synchronizer extends AbstractEntity
{
   /** Log. */
   private static final Logger LOGGER = LogManager.getLogger(Synchronizer.class);

   /** Database Object. */
   private final ProductSynchronizer syncConf;

   /** Synchronizer Service, to create new {@link SynchronizerConf}. */
   private static final ISynchronizerService SYNCHRONIZER_SERVICE =
         ApplicationContextProvider.getBean (ISynchronizerService.class);

   /** Collection Service, for TargetCollection. */
   private static final CollectionService COLLECTION_SERVICE =
      ApplicationContextProvider.getBean (CollectionService.class);

   /**
    * Creates a new Synchronizer from its ID.
    * 
    * @param sync_id synchronizer's ID.
    * @throws NullPointerException if no synchronizer has the given ID.
    */
   public Synchronizer (long sync_id)
   {
      this(SYNCHRONIZER_SERVICE.getSynchronizerConfById(sync_id, ProductSynchronizer.class));
   }

   /**
    * Creates a new Synchronizer from a database object.
    *
    * @param productSynchronizer database object.
    */
   public Synchronizer(ProductSynchronizer productSynchronizer)
   {
      Objects.requireNonNull(productSynchronizer);
      this.syncConf = productSynchronizer;
   }

   /**
    * Creates an new Synchronizer from the given ODataEntry.
    * 
    * @param odata_entry created by a POST request on the OData interface.
    * @throws ODataException if the given entry is malformed.
    */
   public Synchronizer (ODataEntry odata_entry) throws ODataException
   {
      Map<String, Object> props = odata_entry.getProperties ();

      String label = (String) props.get(SynchronizerEntitySet.LABEL);
      String schedule = (String) props.get(SynchronizerEntitySet.SCHEDULE);
      String request = (String) props.get(SynchronizerEntitySet.REQUEST);
      String service_url = (String) props.get(SynchronizerEntitySet.SERVICE_URL);

      if (schedule == null || schedule.isEmpty () || service_url == null ||
         service_url.isEmpty ())
      {
         throw new IncompleteDocException();
      }

      if (request != null && !request.equals ("start") &&
         !request.equals ("stop"))
      {
         throw new InvalidValueException(SynchronizerEntitySet.REQUEST, request);
      }

      try
      {
         this.syncConf =
               SYNCHRONIZER_SERVICE.createSynchronizer(label, schedule, ProductSynchronizer.class);
         setDefaults(odata_entry);
         updateFromEntry (odata_entry);
      }
      catch (ParseException | ReflectiveOperationException e)
      {
         throw new ExpectedException(e.getMessage());
      }
   }

   /**
    * Called by {@link #Synchronizer(ODataEntry)}, sets the default values for null properties
    * from the create document.
    *
    * @param odataEntry create OData document
    */
   private void setDefaults(ODataEntry odataEntry)
   {
      Map<String, Object> props = odataEntry.getProperties ();

      Boolean copyProduct = (Boolean) props.get(SynchronizerEntitySet.COPY_PRODUCT);
      if (copyProduct == null)
      {
         this.syncConf.setCopyProduct(Boolean.FALSE);
      }

      Integer page_size = (Integer) props.get(SynchronizerEntitySet.PAGE_SIZE);
      if (page_size == null)
      {
         this.syncConf.setPageSize(30);
      }

      Boolean skipOnError = (Boolean) props.get(SynchronizerEntitySet.SKIP_ON_ERROR);
      if (skipOnError == null)
      {
         this.syncConf.setSkipOnError(Boolean.TRUE);
      }
   }

   /**
    * Returns the TargetCollection, or null if there is none.
    * 
    * @return the TargetCollection.
    */
   public Collection getTargetCollection () throws ODataException
   {
      String target = this.syncConf.getTargetCollection();
      if (target == null)
      {
         return null;
      }

      fr.gael.dhus.database.object.Collection c =
         COLLECTION_SERVICE.systemGetCollectionByName (target);
      if (c == null)
      {
         throw new ODataException (
            "This synchronizer references a deleted collection");
      }

      return new Collection (c);
   }

   /**
    * Deletes a synchronizer having the given id.
    * 
    * @param sync_id ID of the synchronizer to delete.
    */
   public static void delete (long sync_id)
   {
      SYNCHRONIZER_SERVICE.removeSynchronizer (sync_id);
   }

   @Override
   public void updateFromEntry (ODataEntry odata_entry) throws ODataException
   {
      Map<String, Object> props = odata_entry.getProperties ();

      String schedule = (String) props.remove(SynchronizerEntitySet.SCHEDULE);
      String request = (String) props.remove(SynchronizerEntitySet.REQUEST);
      String service_url = (String) props.remove(SynchronizerEntitySet.SERVICE_URL);
      Integer page_size = (Integer) props.remove(SynchronizerEntitySet.PAGE_SIZE);
      Boolean copy_product = (Boolean) props.remove(SynchronizerEntitySet.COPY_PRODUCT);
      Boolean skipOnError = (Boolean) props.remove(SynchronizerEntitySet.SKIP_ON_ERROR);

      // Nullable fields
      boolean has_label = props.containsKey(SynchronizerEntitySet.LABEL);
      boolean has_login = props.containsKey(SynchronizerEntitySet.SERVICE_LOGIN);
      boolean has_password = props.containsKey(SynchronizerEntitySet.SERVICE_PASSWORD);
      boolean has_incoming = props.containsKey(SynchronizerEntitySet.REMOTE_INCOMING);
      boolean has_filter = props.containsKey(SynchronizerEntitySet.FILTER_PARAM);
      boolean has_collec = props.containsKey(SynchronizerEntitySet.SOURCE_COLLECTION);
      boolean has_last_date = props.containsKey(SynchronizerEntitySet.LAST_CREATION_DATE);
      boolean has_target_col = editsTargetCollection(odata_entry);
      boolean has_geo_filter = props.containsKey(SynchronizerEntitySet.GEO_FILTER);

      String label = (String) props.remove(SynchronizerEntitySet.LABEL);
      String service_login = (String) props.remove(SynchronizerEntitySet.SERVICE_LOGIN);
      String service_password = (String) props.remove(SynchronizerEntitySet.SERVICE_PASSWORD);
      String remote_incoming = (String) props.remove(SynchronizerEntitySet.REMOTE_INCOMING);
      String filter_param = (String) props.remove(SynchronizerEntitySet.FILTER_PARAM);
      String source_collection = (String) props.remove(SynchronizerEntitySet.SOURCE_COLLECTION);
      GregorianCalendar last_ingestion_date =
            (GregorianCalendar) props.remove(SynchronizerEntitySet.LAST_CREATION_DATE);
      String geo_filter = (String) props.remove(SynchronizerEntitySet.GEO_FILTER);

      // Navigation
      Collection target_collection = getTargetCollection(odata_entry);

      for (String pname : props.keySet ())
      {
         LOGGER.debug ("Unknown or ReadOnly property: " + pname);
      }

      // To avoid any side effects
      boolean currentState = this.syncConf.isActive();
      SYNCHRONIZER_SERVICE.deactivateSynchronizer(this.syncConf.getId());
      this.syncConf.setActive(currentState);

      if (request != null)
      {
         if (request.equals ("start"))
         {
            this.syncConf.setActive (true);
         }
         else if (request.equals("stop"))
         {
            this.syncConf.setActive(false);
         }
         else
         {
            throw new InvalidValueException(SynchronizerEntitySet.SCHEDULE, request);
         }
      }

      if (schedule != null && !schedule.isEmpty ())
      {
         try
         {
             CronExpression.validateExpression(schedule);
            this.syncConf.setSchedule(schedule);
         }
         catch (ParseException ex)
         {
            throw new ExpectedException(ex.getMessage());
         }
      }

      if (has_label)
      {
         this.syncConf.setLabel (label);
      }

      if (service_url != null && !service_url.isEmpty ())
      {
         this.syncConf.setServiceUrl(service_url);
      }

      if (has_login)
      {
         this.syncConf.setServiceLogin(service_login);
      }

      if (has_password)
      {
         this.syncConf.setServicePassword(service_password);
      }

      if (page_size != null)
      {
         this.syncConf.setPageSize(page_size);
      }

      if (has_incoming)
      {
         this.syncConf.setRemoteIncoming(remote_incoming);
      }

      if (has_last_date)
      {
         if (last_ingestion_date == null)
         {
            this.syncConf.setLastCreated(null);
         }
         else
         {
            this.syncConf.setLastCreated(XmlProvider.getCalendar(last_ingestion_date));
         }
      }

      if (copy_product != null)
      {
         this.syncConf.setCopyProduct(copy_product);
      }

      if (has_target_col)
      {
         this.syncConf.setTargetCollection(target_collection == null ? null : target_collection.getName());
      }

      if (has_filter)
      {
         this.syncConf.setFilterParam(filter_param);
      }

      if (has_collec)
      {
         this.syncConf.setSourceCollection(source_collection);
      }

      if (has_geo_filter)
      {
         updateGeoFilter(geo_filter);
      }

      if (skipOnError != null)
      {
         this.syncConf.setSkipOnError(skipOnError);
      }

      try
      {
         SYNCHRONIZER_SERVICE.saveSynchronizerConf (this.syncConf);
      }
      catch (InvokeSynchronizerException e)
      {
         throw new ODataException (e);
      }
   }

   @Override
   public Map<String, Object> toEntityResponse (String root_url)
   {
      SynchronizerStatus ss = SYNCHRONIZER_SERVICE.getStatus (this.syncConf);
      Map<String, Object> res = new HashMap<> ();
      res.put(SynchronizerEntitySet.ID, this.syncConf.getId());
      res.put(SynchronizerEntitySet.LABEL, this.syncConf.getLabel());
      res.put(SynchronizerEntitySet.SCHEDULE, this.syncConf.getSchedule());
      res.put(SynchronizerEntitySet.REQUEST, this.syncConf.isActive() ? "start" : "stop");
      res.put(SynchronizerEntitySet.STATUS, ss.status.toString());
      res.put(SynchronizerEntitySet.STATUS_DATE, ss.since);
      res.put(SynchronizerEntitySet.STATUS_MESSAGE, ss.message);
      res.put(SynchronizerEntitySet.CREATION_DATE, this.syncConf.getCreated().toGregorianCalendar());
      res.put(SynchronizerEntitySet.MODIFICATION_DATE, this.syncConf.getModified().toGregorianCalendar());
      res.put(SynchronizerEntitySet.SERVICE_URL, this.syncConf.getServiceUrl());
      res.put(SynchronizerEntitySet.SERVICE_LOGIN, this.syncConf.getServiceLogin());
      res.put(SynchronizerEntitySet.SERVICE_PASSWORD, this.syncConf.getServicePassword()!= null? "***": null);
      res.put(SynchronizerEntitySet.REMOTE_INCOMING, this.syncConf.getRemoteIncoming ());
      res.put(SynchronizerEntitySet.PAGE_SIZE, this.syncConf.getPageSize ());
      res.put(SynchronizerEntitySet.COPY_PRODUCT, this.syncConf.isCopyProduct());
      res.put(SynchronizerEntitySet.FILTER_PARAM, this.syncConf.getFilterParam ());
      res.put(SynchronizerEntitySet.SOURCE_COLLECTION, this.syncConf.getSourceCollection ());
      res.put(SynchronizerEntitySet.GEO_FILTER, getGeoFilter());
      res.put(SynchronizerEntitySet.SKIP_ON_ERROR, this.syncConf.isSkipOnError());

      XMLGregorianCalendar last_created = this.syncConf.getLastCreated();
      if (last_created != null)
      {
         res.put(SynchronizerEntitySet.LAST_CREATION_DATE, last_created.toGregorianCalendar());
      }
      return res;
   }

   @Override
   public Object getProperty (String prop_name) throws ODataException
   {
      Objects.requireNonNull (prop_name);

      SynchronizerStatus ss = SYNCHRONIZER_SERVICE.getStatus (this.syncConf);

      if (prop_name.equals(SynchronizerEntitySet.ID))
      {
         return this.syncConf.getId ();
      }
      if (prop_name.equals(SynchronizerEntitySet.LABEL))
      {
         return this.syncConf.getLabel ();
      }
      if (prop_name.equals(SynchronizerEntitySet.SCHEDULE))
      {
         return this.syncConf.getSchedule();
      }
      if (prop_name.equals(SynchronizerEntitySet.REQUEST))
      {
         return this.syncConf.isActive() ? "start" : "stop";
      }
      if (prop_name.equals(SynchronizerEntitySet.STATUS))
      {
         return ss.status.toString();
      }
      if (prop_name.equals(SynchronizerEntitySet.STATUS_DATE))
      {
         return ss.since;
      }
      if (prop_name.equals(SynchronizerEntitySet.STATUS_MESSAGE))
      {
         return ss.message;
      }
      if (prop_name.equals(SynchronizerEntitySet.CREATION_DATE))
      {
         return this.syncConf.getCreated().toGregorianCalendar();
      }
      if (prop_name.equals(SynchronizerEntitySet.MODIFICATION_DATE))
      {
         return this.syncConf.getModified().toGregorianCalendar();
      }
      if (prop_name.equals(SynchronizerEntitySet.SERVICE_URL))
      {
         return this.syncConf.getServiceUrl();
      }
      if (prop_name.equals(SynchronizerEntitySet.SERVICE_LOGIN))
      {
         return this.syncConf.getServiceLogin();
      }
      if (prop_name.equals(SynchronizerEntitySet.SERVICE_PASSWORD))
      {
         return this.syncConf.getServicePassword() != null ? "***" : null;
      }
      if (prop_name.equals(SynchronizerEntitySet.REMOTE_INCOMING))
      {
         return this.syncConf.getRemoteIncoming();
      }
      if (prop_name.equals(SynchronizerEntitySet.PAGE_SIZE))
      {
         return this.syncConf.getPageSize();
      }
      if (prop_name.equals(SynchronizerEntitySet.LAST_CREATION_DATE))
      {
         return this.syncConf.getLastCreated().toGregorianCalendar().getTime();
      }
      if (prop_name.equals(SynchronizerEntitySet.COPY_PRODUCT))
      {
         Boolean val = this.syncConf.isCopyProduct();
         return val != null ? val : false;
      }
      if (prop_name.equals(SynchronizerEntitySet.FILTER_PARAM))
      {
         return this.syncConf.getFilterParam();
      }
      if (prop_name.equals(SynchronizerEntitySet.SOURCE_COLLECTION))
      {
         return this.syncConf.getSourceCollection();
      }
      if (prop_name.equals(SynchronizerEntitySet.GEO_FILTER))
      {
         return getGeoFilter();
      }
      if (prop_name.equals(SynchronizerEntitySet.SKIP_ON_ERROR))
      {
         return this.syncConf.isSkipOnError();
      }

      throw new ODataException ("Unknown property " + prop_name);
   }

   @Override
   public Object navigate(NavigationSegment ns) throws ODataException
   {
      Object res;

      if (ns.getEntitySet().getName().equals(Model.COLLECTION.getName()))
      {
         res = getTargetCollection();
         if (res == null)
         {
            throw new NoTargetException(SynchronizerEntitySet.TARGET_COLLECTION);
         }
      }
      else
      {
         throw new InvalidTargetException(this.getClass().getSimpleName(), ns.getEntitySet().getName());
      }

      return res;
   }

   @Override
   public List<String> getExpandableNavLinkNames()
   {
      return Collections.<String>singletonList(SynchronizerEntitySet.TARGET_COLLECTION);
   }

   @Override
   public List<Map<String, Object>> expand(String navlinkName, String selfUrl) throws ODataException
   {
      switch (navlinkName)
      {
         case SynchronizerEntitySet.TARGET_COLLECTION:
            return Expander.entityToData(getTargetCollection(), selfUrl);
         default:
            return super.expand(navlinkName, selfUrl);
      }
   }

   @Override
   public void createLink(UriInfo link) throws ODataException
   {
      EdmEntitySet targetEntitySet = link.getTargetEntitySet();
      if (targetEntitySet.getName().equals(Model.COLLECTION.getName()))
      {
         String collectionName = link.getTargetKeyPredicates().get(0).getLiteral();
         this.syncConf.setTargetCollection(collectionName);
         try
         {
            SYNCHRONIZER_SERVICE.saveSynchronizerConf(this.syncConf);
         }
         catch (InvokeSynchronizerException ex)
         {
            throw new ODataException("Cannot create link to TargetCollection: " + ex.getMessage());
         }
      }
      else
      {
         throw new ODataException(String.format("Cannot create link from %s to %s",
               Model.SYNCHRONIZER.getName(), targetEntitySet.getName()));
      }
   }

   @Override
   public void deleteLink(DeleteUriInfo link) throws ODataException
   {
      EdmEntitySet targetEntitySet = link.getTargetEntitySet();
      if (targetEntitySet.getName().equals(Model.COLLECTION.getName()))
      {
         this.syncConf.setTargetCollection(null);
         try
         {
            SYNCHRONIZER_SERVICE.saveSynchronizerConf(this.syncConf);
         }
         catch (InvokeSynchronizerException ex)
         {
            throw new ODataException("Cannot delete link to TargetCollection: " + ex.getMessage());
         }
      }
      else
      {
         throw new ODataException(String.format("Cannot delete link from %s to %s",
               Model.SYNCHRONIZER.getName(), targetEntitySet.getName()));
      }
   }

   private static boolean editsTargetCollection(ODataEntry entry) throws ODataException
   {
      List<String> nll = entry.getMetadata().getAssociationUris(SynchronizerEntitySet.TARGET_COLLECTION);
      return !(nll == null || nll.isEmpty());
   }

   private static Collection getTargetCollection(ODataEntry entry)
      throws ODataException
   {
      String navLinkName = SynchronizerEntitySet.TARGET_COLLECTION;
      List<String> nll = entry.getMetadata ().getAssociationUris (navLinkName);

      if (nll != null && !nll.isEmpty ())
      {
         if (nll.size () > 1)
         {
            throw new ODataException (
               "A synchronizer accepts only one collection");
         }
         String uri = nll.get(0);
         // Nullifying
         if (uri == null || uri.isEmpty())
         {
            return null;
         }

         Edm edm = RuntimeDelegate.createEdm(new Model());
         UriParser urip = RuntimeDelegate.getUriParser (edm);

         List<PathSegment> path_segments = new ArrayList<> ();

         StringTokenizer st = new StringTokenizer (uri, "/");

         while (st.hasMoreTokens ())
         {
            path_segments.add (UriParser.createPathSegment (st.nextToken (),
               null));
         }

         UriInfo uinfo =
            urip
               .parse (path_segments, Collections.<String, String> emptyMap ());

         EdmEntitySet sync_ees = uinfo.getStartEntitySet ();
         KeyPredicate kp = uinfo.getKeyPredicates ().get (0);
         List<NavigationSegment> ns_l = uinfo.getNavigationSegments ();

         Collection c = Navigator.<Collection>navigate(sync_ees, kp, ns_l, Collection.class);

         return c;
      }

      return null;
   }

   /**
    * Update the geo filter configuration from user supplied GeoFilter property.
    *
    * @param geo_filter the content of the GeoFilter property
    * @throws InvalidValueException if GeoFilter is invalid
    */
   private void updateGeoFilter(String geo_filter) throws InvalidValueException
   {
      if (geo_filter == null || geo_filter.isEmpty())
      {
         this.syncConf.setGeofilterOp(null);
         this.syncConf.setGeofilterShape(null);
      }
      else
      {
         Pattern pattern = Pattern.compile("(disjoint|within|contains|intersects) (.+)");
         Matcher matcher = pattern.matcher(geo_filter);
         if (!matcher.matches())
         {
            throw new InvalidValueException(SynchronizerEntitySet.GEO_FILTER, geo_filter);
         }
         String operator = matcher.group(1);
         String wkt = matcher.group(2);
         WKTReader wkt_reader = new WKTReader();
         try
         {
            Geometry geometry = wkt_reader.read(wkt);
            if (!geometry.isValid())
            {
               throw new InvalidValueException(SynchronizerEntitySet.GEO_FILTER, geo_filter);
            }
            this.syncConf.setGeofilterOp(operator);
            this.syncConf.setGeofilterShape(wkt);
         }
         catch (com.vividsolutions.jts.io.ParseException ex)
         {
            throw new InvalidValueException(SynchronizerEntitySet.GEO_FILTER, geo_filter);
         }
      }
   }

   /**
    * Returns the GeoFilter property or null if not set.
    *
    * @return the GeoFilter OData property
    */
   private String getGeoFilter()
   {
      String op = this.syncConf.getGeofilterOp();
      String shape = this.syncConf.getGeofilterShape();

      if (shape != null && !shape.isEmpty()   &&   op != null && !op.isEmpty())
      {
         return op + ' ' + shape;
      }
      return null;
   }

}
