package org.dhus.olingo.v2.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.dhus.olingo.v2.datamodel.SearchModel;
import org.dhus.olingo.v2.datamodel.UserModel;
import org.dhus.olingo.v2.datamodel.complex.AdvancedPropertyComplexType;

import fr.gael.dhus.database.object.Search;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.service.UserService;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.odata.engine.data.DataHandlerUtil;
import fr.gael.odata.engine.data.DatabaseDataHandler;

public class SearchesDataHandler implements DatabaseDataHandler
{
   private static final UserService USER_SERVICE = ApplicationContextProvider.getBean(UserService.class);

   @Override
   public EntityCollection getEntityCollectionData() throws ODataApplicationException
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Entity getEntityData(List<UriParameter> keyParameters) throws ODataApplicationException
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public EntityCollection getEntityCollectionData(FilterOption filterOption, OrderByOption orderByOption,
         TopOption topOption, SkipOption skipOption, CountOption countOption) throws ODataApplicationException
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Integer countEntities(FilterOption filterOption) throws ODataApplicationException
   {
      // TODO Auto-generated method stub
      return null;
   }
   
   private Entity toOlingoEntity(Search search)
   {  
      Entity searchEntity = new Entity();
      searchEntity.addProperty(new Property(null,
            SearchModel.PROPERTY_ID,
            ValueType.PRIMITIVE,
            UUID.fromString(search.getUUID())));
      
      searchEntity.addProperty(new Property(null,
            SearchModel.PROPERTY_NOTIFY,
            ValueType.PRIMITIVE,
            search.isNotify()));
      
      searchEntity.addProperty(new Property(null,
            SearchModel.PROPERTY_FOOTPRINT,
            ValueType.PRIMITIVE,
            search.getFootprint()));
      
      searchEntity.addProperty(new Property(null,
            SearchModel.PROPERTY_COMPLETE,
            ValueType.PRIMITIVE,
            search.getComplete()));
      
      searchEntity.addProperty(new Property(null,
            SearchModel.PROPERTY_VALUE,
            ValueType.PRIMITIVE,
            search.getValue()));
      
      Map<String, String> advanced = search.getAdvanced();

      List<ComplexValue> advancedComplexCollection = new ArrayList<>();
      
      for (String key : advanced.keySet())
      {
         String value = advanced.get(key);      
         ComplexValue advancedValue = new ComplexValue();
         advancedValue.getValue().add(new Property(
               null,
               AdvancedPropertyComplexType.PROPERTY_NAME,
               ValueType.PRIMITIVE,
               key));

         advancedValue.getValue().add(new Property(
               null,
               AdvancedPropertyComplexType.PROPERTY_VALUE,
               ValueType.PRIMITIVE,
               value));

         advancedComplexCollection.add(advancedValue);
      }
      
      searchEntity.addProperty(new Property(null,
            AdvancedPropertyComplexType.COMPLEX_TYPE_NAME,
            ValueType.COLLECTION_COMPLEX,
            advancedComplexCollection));
      return searchEntity;
   }
   
   @Override
   public EntityCollection getRelatedEntityCollectionData(Entity sourceEntity,
         EdmNavigationProperty edmNavigationProperty) throws ODataApplicationException
   {
      //Coming from User
      if (sourceEntity.getType().equals(UserModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
         EntityCollection navigationTargetEntityCollection = new EntityCollection();
         String username = (String) sourceEntity.getProperty(UserModel.PROPERTY_USERNAME).getValue();
         User user = UserDataHandler.getUser(username);
         if (user == null)
         {
            throw new ODataApplicationException("Unable to get the user",
                  HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
         }
         List<Search> searches = USER_SERVICE.getAllUserSearches(user.getUUID());
         if (searches != null)
         {
            searches.forEach(search ->
            {
               navigationTargetEntityCollection.getEntities().add(toOlingoEntity(search));
            });
            return navigationTargetEntityCollection;
         }
      }
      return null;
   }

   @Override
   public Entity getRelatedEntityData(Entity entity, List<UriParameter> navigationKeyParameters, EdmNavigationProperty edmNavigationProperty)
         throws ODataApplicationException
   {
      if (entity.getType().equals(UserModel.FULL_QUALIFIED_NAME.getFullQualifiedNameAsString()))
      {
            String username = (String) entity.getProperty(UserModel.PROPERTY_USERNAME).getValue();
            User user = UserDataHandler.getUser(username);
            if (user == null)
            {
               throw new ODataApplicationException("Unable to get the user",
                     HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
            }
            List<Search> searches = USER_SERVICE.getAllUserSearches(user.getUUID());
            if (searches != null)
            {
               String key = null;
               for (UriParameter uriParameter : navigationKeyParameters)
               {
                  if (uriParameter.getName().equals(SearchModel.PROPERTY_ID))
                  {
                     key = DataHandlerUtil.trimStringKeyParameter(uriParameter);
                     break;
                  }
               }
               for (Search search : searches)
               {
                  if (search.getUUID().equals(key))
                  {
                     return toOlingoEntity(search);
                  }
               }
            }
      }
      return null;
   }
   
}
