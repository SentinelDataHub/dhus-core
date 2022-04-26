package fr.gael.dhus.spring.security.saml;

import org.springframework.security.core.Authentication;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.security.saml.log.SAMLDefaultLogger;

public class SAMLLogger extends SAMLDefaultLogger
{
   @Override
   public void log(String operation, String result, SAMLMessageContext context, Authentication a, Exception e)
   {
      // GDPR : remove user information
      super.log(operation, result, context, null, e);
   }
}
