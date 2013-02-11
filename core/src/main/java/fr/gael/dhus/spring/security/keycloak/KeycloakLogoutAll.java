package fr.gael.dhus.spring.security.keycloak;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.jboss.logging.Logger;
import org.keycloak.TokenVerifier;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.util.JsonSerialization;
import org.opensaml.xml.signature.impl.X509CertificateImpl;
import org.springframework.security.saml.metadata.CachingMetadataManager;
import org.springframework.web.filter.GenericFilterBean;

import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.spring.context.SecurityContextProvider;

public class KeycloakLogoutAll extends GenericFilterBean
{
   private static final SecurityContextProvider SEC_CTX_PROVIDER = ApplicationContextProvider.getBean(SecurityContextProvider.class);
   private static final Logger log = Logger.getLogger(KeycloakLogoutAll.class);
   private CachingMetadataManager idpMetadata;

   public KeycloakLogoutAll(CachingMetadataManager idpMetadataManager) throws Exception
   {
      idpMetadata = idpMetadataManager;
   }
   
   @Override
   public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException
   {
      try
      {
         JWSInput token = verifyAdminRequest((HttpServletRequest) req, (HttpServletResponse) res);
         if (token == null)
         {
            return;
         }
         LogoutAction action = JsonSerialization.readValue(token.getContent(), LogoutAction.class);
         if (!validateAction(action, (HttpServletResponse) res))
         {
            return;
         }
         log.info("Logging out all Users as requested by Keycloak");
         SEC_CTX_PROVIDER.forceLogoutAllUsers();
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }

   }

   protected JWSInput verifyAdminRequest(HttpServletRequest req,
         HttpServletResponse res) throws Exception
   {
      String token = StreamUtil.readString(req.getInputStream());
      if (token == null)
      {
         log.warn("Logout all users admin request failed, no token");
         res.sendError(403, "no token");
         return null;
      }

      try
      {
         // Check just signature. Other things checked in validateAction
         TokenVerifier tokenVerifier = createVerifier(token, false, JsonWebToken.class);
         tokenVerifier.verify();
         return new JWSInput(token);
      }
      catch (Exception ignore)
      {
         log.warn("Logout all users admin request failed, unable to verify token: " + ignore.getMessage());
         if (log.isDebugEnabled())
         {
            log.debug(ignore.getMessage(), ignore);
         }

         res.sendError(403, "token failed verification");
         return null;
      }
   }

   private boolean validateAction(LogoutAction action, HttpServletResponse res)
         throws IOException
   {
      if (!action.validate())
      {
         log.warn("Logout all users admin request failed, not validated" + action.getAction());
         res.sendError(400, "Not validated");
         return false;
      }
      if (action.isExpired())
      {
         log.warn("Logout all users admin request failed, expired token");
         res.sendError(400, "Expired token");
         return false;
      }
      return true;
   }

   private <T extends JsonWebToken> TokenVerifier<T> createVerifier( String tokenString, boolean withDefaultChecks, Class<T> tokenClass) throws Exception
   {
      TokenVerifier<T> tokenVerifier = TokenVerifier.create(tokenString, tokenClass);
      
      X509CertificateImpl x509Cert = (X509CertificateImpl)
            idpMetadata.getEntityDescriptor(idpMetadata.getDefaultIDP()).getIDPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
            .getKeyDescriptors().get(0).getKeyInfo().getX509Datas().get(0).getX509Certificates().get(0);

      String cert = x509Cert.getValue(); 
      byte[] encodedCert = cert.getBytes("UTF-8");
      byte[] decodedCert = Base64.decodeBase64(encodedCert);
      CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
      InputStream in = new ByteArrayInputStream(decodedCert);
      X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(in);
      
      tokenVerifier.publicKey(certificate.getPublicKey());
      return tokenVerifier;
   }
}
