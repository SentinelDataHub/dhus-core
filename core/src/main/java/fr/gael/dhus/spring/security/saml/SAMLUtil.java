package fr.gael.dhus.spring.security.saml;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import fr.gael.dhus.spring.context.ApplicationContextProvider;

public class SAMLUtil
{
   private static IDPManager idpManager = ApplicationContextProvider.getBean(IDPManager.class);
   
   public static String hash(String username)
   {
      String key = idpManager.getIdpName() + "~" + username;
      String myHash = key;
      
      try
      {
         MessageDigest md = MessageDigest.getInstance("MD5");
         md.update(key.getBytes());
         byte[] digest = md.digest();
         // forced toLowerCase since User username are stored in LowerCase.
         myHash = DatatypeConverter.printHexBinary(digest).toLowerCase();
      }
      catch (NoSuchAlgorithmException e) {}
      
      return myHash;
   }
}
