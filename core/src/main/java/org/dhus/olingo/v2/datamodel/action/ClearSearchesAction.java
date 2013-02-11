package org.dhus.olingo.v2.datamodel.action;

import java.util.Arrays;

import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;
import org.dhus.olingo.v2.datamodel.SearchModel;
import org.dhus.olingo.v2.datamodel.UserModel;
import org.dhus.olingo.v2.web.DHuSODataServlet;

import fr.gael.odata.engine.model.ActionModel;

public class ClearSearchesAction implements ActionModel
{

   public static final String ACTION_CLEAR_SEARCHES_NAME = "ClearSearches";
   public static final FullQualifiedName ACTION_CLEAR_SEARCHES_FNQ =
         new FullQualifiedName(DHuSODataServlet.NAMESPACE, ACTION_CLEAR_SEARCHES_NAME);
   
   public static final String PARAM_USER = "User";
   
   @Override
   public String getName()
   {
      return ACTION_CLEAR_SEARCHES_NAME;
   }

   @Override
   public FullQualifiedName getFQN()
   {
      return ACTION_CLEAR_SEARCHES_FNQ;
   }

   

   @Override
   public CsdlAction getAction()
   {
      CsdlParameter userParameter = new CsdlParameter()
            .setName(PARAM_USER)
            .setNullable(false)
            .setType(UserModel.FULL_QUALIFIED_NAME);
      
      return new CsdlAction()
            .setName(ACTION_CLEAR_SEARCHES_NAME)
            .setBound(true)
            .setParameters(Arrays.asList(userParameter))
            .setReturnType(new CsdlReturnType().setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()));
   }

}
