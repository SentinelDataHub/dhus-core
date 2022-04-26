package fr.gael.dhus.spring.security.saml;

import static org.springframework.security.saml.util.SAMLUtil.isDateTimeSkewValid;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.opensaml.common.SAMLException;
import org.opensaml.common.SAMLObject;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.NameIDType;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.xml.encryption.DecryptionException;
import org.springframework.security.saml.SAMLConstants;
import org.springframework.security.saml.SAMLStatusException;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.security.saml.util.SAMLUtil;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.util.Assert;

import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.spring.context.SecurityContextProvider;

public class SAMLLogoutProcessingFilter extends org.springframework.security.saml.SAMLLogoutProcessingFilter
{
   private static final SecurityContextProvider SEC_CTX_PROVIDER = ApplicationContextProvider.getBean(SecurityContextProvider.class);

   public SAMLLogoutProcessingFilter(String logoutSuccessUrl, LogoutHandler[] handlers)
   {
      super(logoutSuccessUrl, handlers);
   }

   public SAMLLogoutProcessingFilter(LogoutSuccessHandler logoutSuccessHandler, LogoutHandler[] handlers)
   {
      super(logoutSuccessHandler, handlers);
   }

   @Override
   public void processLogout(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException
   {
      if (requiresLogout(request, response))
      {
         SAMLMessageContext context;
         try
         {
            log.debug("Processing SAML logout message");
            context = contextProvider.getLocalEntity(request, response);
            context.setCommunicationProfileId(getProfileName());
            processor.retrieveMessage(context);
            context.setLocalEntityEndpoint(SAMLUtil.getEndpoint(
                  context.getLocalEntityRoleMetadata().getEndpoints(),
                  context.getInboundSAMLBinding(),
                  context.getInboundMessageTransport(), uriComparator));

         }
         catch (SAMLException e)
         {
            log.debug("Incoming SAML message is invalid", e);
            throw new ServletException("Incoming SAML message is invalid", e);
         }
         catch (MetadataProviderException e)
         {
            log.debug("Error determining metadata contracts", e);
            throw new ServletException("Error determining metadata contracts", e);
         }
         catch (MessageDecodingException e)
         {
            log.debug("Error decoding incoming SAML message", e);
            throw new ServletException("Error decoding incoming SAML message", e);
         }
         catch (org.opensaml.xml.security.SecurityException e)
         {
            log.debug("Incoming SAML message failed security validation", e);
            throw new ServletException("Incoming SAML message failed security validation", e);
         }

         if (context.getInboundSAMLMessage() instanceof LogoutResponse)
         {
            try
            {
               logoutProfile.processLogoutResponse(context);

               log.debug("Performing local logout after receiving logout response from {}", context.getPeerEntityId());
               super.doFilter(request, response, chain);

               samlLogger.log(SAMLConstants.LOGOUT_RESPONSE, SAMLConstants.SUCCESS, context);
            }
            catch (Exception e)
            {
               log.debug("Received logout response is invalid", e);
               samlLogger.log(SAMLConstants.LOGOUT_RESPONSE, SAMLConstants.FAILURE, context, e);
            }

         }
         else if (context.getInboundSAMLMessage() instanceof LogoutRequest)
         {
            NameID nameID = null;
            LogoutRequest logoutRequest = (LogoutRequest) context.getInboundSAMLMessage();
            try
            {
               nameID = getNameID(context, logoutRequest);
            }
            catch (DecryptionException e1)
            {
               // TODO Auto-generated catch block
               e1.printStackTrace();
            }

            try
            {
               boolean doLogout;

               try
               {
                  doLogout = processLogoutRequest(context);
               }
               catch (SAMLStatusException e)
               {
                  log.debug("Received logout request is invalid, responding with error", e);
                  logoutProfile.sendLogoutResponse(context, e.getStatusCode(), e.getStatusMessage());
                  samlLogger.log(SAMLConstants.LOGOUT_REQUEST, SAMLConstants.FAILURE, context, e);
                  return;
               }

               if (doLogout)
               {
                  log.debug("Performing local logout after receiving logout request from {}", context.getPeerEntityId());
                  SEC_CTX_PROVIDER.forceLogout(fr.gael.dhus.spring.security.saml.SAMLUtil.hash(nameID.getValue()));
               }

               logoutProfile.sendLogoutResponse(context, StatusCode.SUCCESS_URI, null);
               samlLogger.log(SAMLConstants.LOGOUT_REQUEST, SAMLConstants.SUCCESS, context);
            }
            catch (Exception e)
            {
               log.debug("Error processing logout request", e);
               samlLogger.log(SAMLConstants.LOGOUT_REQUEST, SAMLConstants.FAILURE, context, e);
               throw new ServletException("Error processing logout request", e);
            }
         }
      }
      else
      {
         chain.doFilter(request, response);
      }

   }

   protected NameID getNameID(SAMLMessageContext context, LogoutRequest request) throws DecryptionException
   {
      NameID id;
      if (request.getEncryptedID() != null)
      {
         Assert.notNull(context.getLocalDecrypter(), "Can't decrypt NameID, no decrypter is set in the context");
         id = (NameID) context.getLocalDecrypter().decrypt(request.getEncryptedID());
      }
      else
      {
         id = request.getNameID();
      }
      return id;
   }

   // imported from org.springframework.security.saml.websso.SingleLogoutProfileImpl
   public boolean processLogoutRequest(SAMLMessageContext context) throws SAMLException
   {
      SAMLObject message = context.getInboundSAMLMessage();

      // Verify type
      if (message == null || !(message instanceof LogoutRequest))
      {
         throw new SAMLException("Message is not of a LogoutRequest object type");
      }

      LogoutRequest logoutRequest = (LogoutRequest) message;

      // Make sure request was authenticated if required, authentication is done
      // as part of the binding processing
      if (!context.isInboundSAMLMessageAuthenticated() && context.getLocalExtendedMetadata().isRequireLogoutRequestSigned())
      {
         throw new SAMLStatusException(StatusCode.REQUEST_DENIED_URI, "LogoutRequest is required to be signed by the entity policy");
      }

      // Verify destination
      try
      {
         verifyEndpoint(context.getLocalEntityEndpoint(), logoutRequest.getDestination());
      }
      catch (SAMLException e)
      {
         throw new SAMLStatusException(StatusCode.REQUEST_DENIED_URI, "Destination of the LogoutRequest does not match any of the single logout endpoints");
      }

      // Verify issuer
      try
      {
         if (logoutRequest.getIssuer() != null)
         {
            Issuer issuer = logoutRequest.getIssuer();
            verifyIssuer(issuer, context);
         }
      }
      catch (SAMLException e)
      {
         throw new SAMLStatusException(StatusCode.REQUEST_DENIED_URI, "Issuer of the LogoutRequest is unknown");
      }

      // Verify issue time
      DateTime time = logoutRequest.getIssueInstant();
      if (!isDateTimeSkewValid(60, time))
      {
         throw new SAMLStatusException(StatusCode.REQUESTER_URI, "LogoutRequest issue instant is either too old or with date in the future");
      }

      return true;
   }

   protected void verifyIssuer(Issuer issuer, SAMLMessageContext context) throws SAMLException
   {
      // Validate format of issuer
      if (issuer.getFormat() != null && !issuer.getFormat().equals(NameIDType.ENTITY))
      {
         throw new SAMLException( "Issuer invalidated by issuer type " + issuer.getFormat());
      }
      // Validate that issuer is expected peer entity
      if (!context.getPeerEntityMetadata().getEntityID().equals(issuer.getValue()))
      {
         throw new SAMLException("Issuer invalidated by issuer value " + issuer.getValue());
      }
   }

   protected void verifyEndpoint(Endpoint endpoint, String destination) throws SAMLException
   {
      // Verify that destination in the response matches one of the available endpoints
      if (destination != null)
      {
         if (uriComparator.compare(destination, endpoint.getLocation()))
         {
            // Expected
         }
         else if (uriComparator.compare(destination, endpoint.getResponseLocation()))
         {
            // Expected
         }
         else
         {
            throw new SAMLException("Intended destination " + destination + " doesn't match any of the endpoint URLs on endpoint "
                  + endpoint.getLocation() + " for profile " + SAMLConstants.SAML2_SLO_PROFILE_URI);
         }
      }
   }
}
