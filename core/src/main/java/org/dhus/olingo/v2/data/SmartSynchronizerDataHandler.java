/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2018 GAEL Systems
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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.config.synchronizer.SmartProductSynchronizer;
import fr.gael.dhus.database.object.config.synchronizer.SynchronizerSource;
import fr.gael.dhus.service.exception.InvokeSynchronizerException;
import fr.gael.dhus.util.XmlProvider;
import fr.gael.odata.engine.data.DataHandlerUtil;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.dhus.olingo.v2.ODataSecurityManager;
import org.dhus.olingo.v2.datamodel.SmartSynchronizerModel;
import org.dhus.olingo.v2.datamodel.complex.CronComplexType;
import org.dhus.olingo.v2.datamodel.complex.SynchronizerSourceComplexType;
import org.dhus.olingo.v2.entity.TypeStore;

public class SmartSynchronizerDataHandler extends SynchronizerDataHandler
{
   private static final String DEPRECATED = "_deprecated_";
   private static final int DEFAULT_PAGE_SIZE = 30;
   private static final int DEFAULT_ATTEMPTS = 10;
   private static final long DEFAULT_TIMEOUT = 300_000;
   private static final int DEFAULT_THRESHOLD = 0;

   private static final java.util.regex.Pattern GEO_FILTER_PATTERN =
         Pattern.compile("(disjoint|within|contains|intersects) (.+)");

   public SmartSynchronizerDataHandler(TypeStore typeStore)
   {
      super(typeStore);
   }

   /**
    * Retrieves a smart synchronizer configuration.
    * <p>
    * The given argument must contains a parameter which refers to a
    * {@link SmartProductSynchronizer} id.
    *
    * @param keyParameters URI parameters
    * @return a smart synchronizer configuration, or {@code null} if it not found
    */
   private SmartProductSynchronizer getSmartSyncFromParameters(List<UriParameter> keyParameters)
   {
      SmartProductSynchronizer config = null;
      for (UriParameter parameter: keyParameters)
      {
         if (parameter.getName().equals(SmartSynchronizerModel.PROPERTY_ID))
         {
            config = SYNCHRONIZER_SERVICE.getSynchronizerConfById(
                  Integer.valueOf(parameter.getText()), SmartProductSynchronizer.class);
            break;
         }
      }
      return config;
   }

   /**
    * Extracts geometry operation and shape.
    *
    * @param geoFilter geometry filter
    * @param syncConf  synchronizer configuration to update
    * @throws IllegalArgumentException if the <em>geoFilter</em> is not valid
    */
   private void updateGeoFilter(String geoFilter, SmartProductSynchronizer syncConf)
   {
      if (geoFilter == null || geoFilter.isEmpty())
      {
         syncConf.setGeofilterOp(null);
         syncConf.setGeofilterShape(null);
         return;
      }
      String baseErrorMessage = "Invalid parameter " + SmartSynchronizerModel.PROPERTY_FILTER_GEO;
      Matcher matcher = GEO_FILTER_PATTERN.matcher(geoFilter);
      if (!matcher.matches())
      {
         throw new IllegalArgumentException(baseErrorMessage);
      }

      String operation = matcher.group(1);
      String shape = matcher.group(2);
      WKTReader wktReader = new WKTReader();

      try
      {
         Geometry geometry = wktReader.read(shape);
         if (!geometry.isValid())
         {
            throw new IllegalArgumentException(baseErrorMessage + ": invalid shape");
         }
      }
      catch (com.vividsolutions.jts.io.ParseException e)
      {
         throw new IllegalArgumentException(baseErrorMessage + ": cannot parse shape");
      }

      syncConf.setGeofilterOp(operation);
      syncConf.setGeofilterShape(shape);
   }

   /**
    * Set required default value.
    *
    * @param syncConf synchronizer configuration to update
    */
   private void setDefaultValues(SmartProductSynchronizer syncConf)
   {
      // force deprecated value
      syncConf.setServiceUrl(DEPRECATED);
      syncConf.setServiceLogin(DEPRECATED);
      syncConf.setServicePassword(DEPRECATED);
      // set default values
      syncConf.setPageSize(DEFAULT_PAGE_SIZE);
      syncConf.setTimeout(DEFAULT_TIMEOUT);
      syncConf.setAttempts(DEFAULT_ATTEMPTS);
      syncConf.setThreshold(DEFAULT_THRESHOLD);
      SmartProductSynchronizer.Sources syncSources = new SmartProductSynchronizer.Sources();
      syncSources.setSource(new LinkedList<>());
      syncConf.setSources(syncSources);
   }

   /**
    * Updates a synchronizer configuration from a entity.
    *
    * @param syncConf      synchronizer configuration to update
    * @param updatedEntity updated source
    */
   private void updateSmartSynchronizer(SmartProductSynchronizer syncConf, Entity updatedEntity)
   {
      if (DataHandlerUtil.containsProperty(
            updatedEntity, SmartSynchronizerModel.PROPERTY_PAGE_SIZE))
      {
         Integer pageSize = (Integer) DataHandlerUtil.getPropertyValue(
               updatedEntity, SmartSynchronizerModel.PROPERTY_PAGE_SIZE);
         syncConf.setPageSize(pageSize);
      }

      if (DataHandlerUtil.containsProperty(
            updatedEntity, SmartSynchronizerModel.PROPERTY_FILTER_PARAM))
      {
         syncConf.setFilterParam((String) DataHandlerUtil.getPropertyValue(
               updatedEntity, SmartSynchronizerModel.PROPERTY_FILTER_PARAM));
      }

      if (DataHandlerUtil.containsProperty(
            updatedEntity, SmartSynchronizerModel.PROPERTY_FILTER_GEO))
      {
         String geoFilter = (String) DataHandlerUtil.getPropertyValue(
               updatedEntity, SmartSynchronizerModel.PROPERTY_FILTER_GEO);
         updateGeoFilter(geoFilter, syncConf);
      }

      if (DataHandlerUtil.containsProperty(updatedEntity, SmartSynchronizerModel.PROPERTY_ATTEMPTS))
      {
         syncConf.setAttempts((Integer) DataHandlerUtil.getPropertyValue(
               updatedEntity, SmartSynchronizerModel.PROPERTY_ATTEMPTS));
      }

      if (DataHandlerUtil.containsProperty(updatedEntity, SmartSynchronizerModel.PROPERTY_TIMEOUT))
      {
         syncConf.setTimeout((Long) DataHandlerUtil.getPropertyValue(
               updatedEntity, SmartSynchronizerModel.PROPERTY_TIMEOUT));
      }

      if (DataHandlerUtil
            .containsProperty(updatedEntity, SmartSynchronizerModel.PROPERTY_THRESHOLD))
      {
         syncConf.setThreshold((Integer) DataHandlerUtil.getPropertyValue(
               updatedEntity, SmartSynchronizerModel.PROPERTY_THRESHOLD));
      }

      if (DataHandlerUtil.containsProperty(updatedEntity, SmartSynchronizerModel.PROPERTY_SOURCES))
      {
         @SuppressWarnings("unchecked")
         List<ComplexValue> complexValueList = (List<ComplexValue>) DataHandlerUtil.getPropertyValue(
               updatedEntity, SmartSynchronizerModel.PROPERTY_SOURCES);

         ArrayList<SynchronizerSource> synchronizerSourceList = new ArrayList<>();
         complexValueList.forEach(
               complexValue -> synchronizerSourceList.add(toSynchronizerSource(complexValue)));

         SmartProductSynchronizer.Sources sources = new SmartProductSynchronizer.Sources();
         sources.setSource(synchronizerSourceList);
         syncConf.setSources(sources);
      }

      if (DataHandlerUtil.containsProperty(updatedEntity, SmartSynchronizerModel.PROPERTY_CRON))
      {
         extractCron(updatedEntity, syncConf);
      }
   }

   private void extractCron(Entity updatedEntity, SmartProductSynchronizer syncConf)
   {
      Property cronProperty = updatedEntity.getProperty(SmartSynchronizerModel.PROPERTY_CRON);

      if (cronProperty != null && cronProperty.getValue() != null)
      {
         List<Property> cronProperties = ((ComplexValue) cronProperty.getValue()).getValue();
         for (Property property: cronProperties)
         {
            switch (property.getName())
            {
               case CronComplexType.PROPERTY_ACTIVE:
                  syncConf.setActive((Boolean) property.getValue());
                  break;

               case CronComplexType.PROPERTY_SCHEDULE:
                  syncConf.setSchedule((String) property.getValue());
                  break;
            }
         }
      }
   }

   private SynchronizerSource toSynchronizerSource(ComplexValue complexValue)
   {
      if (complexValue == null)
      {
         throw new IllegalArgumentException();
      }

      List<Property> properties = complexValue.getValue();
      SynchronizerSource syncSource = new SynchronizerSource();
      boolean isValid = false;

      for (Property property: properties)
      {
         if (property.getName().equals(SynchronizerSourceComplexType.PROPERTY_SOURCE_ID))
         {
            syncSource.setSourceId((Integer) property.getValue());
            isValid = true;
         }

         if (property.getName().equals(SynchronizerSourceComplexType.PROPERTY_LAST_CREATION_DATE))
         {
            syncSource.setLastCreated(
                  XmlProvider.getCalendar((GregorianCalendar) property.getValue()));
         }
         else
         {
            syncSource.setLastCreated(XmlProvider.getCalendar(0));
         }

         if (property.getName().equals(SynchronizerSourceComplexType.PROPERTY_SOURCE_COLLECTION))
         {
            syncSource.setSourceCollection((String) property.getValue());
         }
      }

      if (!isValid)
      {
         throw new IllegalArgumentException("Missing SourceId for a SynchronizerSource");
      }

      return syncSource;
   }

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      Iterator<SmartProductSynchronizer> it =
            SYNCHRONIZER_SERVICE.getSynchronizerConfs(SmartProductSynchronizer.class);
      final EntityCollection entities = new EntityCollection();
      it.forEachRemaining(syncConf -> entities.getEntities().add(toOlingoEntity(syncConf)));
      return entities;
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      return toOlingoEntity(getSmartSyncFromParameters(keyParameters));
   }

   @Override
   public void updateEntityData(List<UriParameter> keyParameters, Entity updatedEntity,
         HttpMethod httpMethod) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      SmartProductSynchronizer syncConf = getSmartSyncFromParameters(keyParameters);

      if (syncConf == null)
      {
         throw new ODataApplicationException(
               "Cannot update synchronizer: targeted synchronizer does not exist",
               HttpStatusCode.BAD_REQUEST.getStatusCode(),
               Locale.ENGLISH);
      }

      SYNCHRONIZER_SERVICE.deactivateSynchronizer(syncConf.getId());

      try
      {
         updateSmartSynchronizer(syncConf, updatedEntity);
         SYNCHRONIZER_SERVICE.saveSynchronizerConf(syncConf);
      }
      catch (InvokeSynchronizerException e)
      {
         throw new ODataApplicationException(
               "Cannot update synchronizer: ",
               HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
               Locale.ENGLISH);
      }
      catch (IllegalArgumentException e)
      {
         throw new ODataApplicationException(
               "Cannot update synchronizer: " + e.getMessage(),
               HttpStatusCode.BAD_REQUEST.getStatusCode(),
               Locale.ENGLISH);
      }
   }

   @Override
   public void deleteEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      SmartProductSynchronizer sync = getSmartSyncFromParameters(keyParameters);
      if (sync != null)
      {
         SYNCHRONIZER_SERVICE.removeSynchronizer(sync.getId());
      }
   }

   @Override
   public Entity createEntityData(Entity entity) throws ODataApplicationException
   {
      ODataSecurityManager.checkPermission(Role.SYSTEM_MANAGER);

      String schedule = (String) DataHandlerUtil.getPropertyValueFromComplexProperty(
            entity,
            SmartSynchronizerModel.PROPERTY_CRON,
            CronComplexType.PROPERTY_SCHEDULE);

      String label = (String) DataHandlerUtil.getPropertyValue(
            entity, SmartSynchronizerModel.PROPERTY_LABEL);

      SmartProductSynchronizer syncConf = null;
      try
      {
         // create synchronizer (base)
         syncConf = SYNCHRONIZER_SERVICE.createSynchronizer(
               label, schedule, SmartProductSynchronizer.class);

         // complete synchronizer configuration
         setDefaultValues(syncConf);
         updateSmartSynchronizer(syncConf, entity);

         SYNCHRONIZER_SERVICE.saveSynchronizerConf(syncConf);
      }
      catch (ParseException | ReflectiveOperationException | InvokeSynchronizerException e)
      {
         if (syncConf != null)
         {
            SYNCHRONIZER_SERVICE.removeSynchronizer(syncConf.getId());
         }

         throw new ODataApplicationException(
               "Cannot create synchronizer",
               HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
               Locale.ENGLISH);
      }
      catch (IllegalArgumentException e)
      {
         throw new ODataApplicationException(
               "Cannot create synchronizer: " + e.getMessage(),
               HttpStatusCode.BAD_REQUEST.getStatusCode(),
               Locale.ENGLISH);
      }
      return toOlingoEntity(syncConf);
   }
}
