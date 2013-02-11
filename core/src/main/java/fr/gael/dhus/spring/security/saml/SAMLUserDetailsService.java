package fr.gael.dhus.spring.security.saml;

import java.util.ArrayList;
import java.util.List;

import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.schema.impl.XSStringImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.stereotype.Component;

import fr.gael.dhus.database.object.Role;
import fr.gael.dhus.database.object.User;
import fr.gael.dhus.system.config.ConfigurationManager;

@Component
public class SAMLUserDetailsService implements org.springframework.security.saml.userdetails.SAMLUserDetailsService 
{

   @Autowired
   private ConfigurationManager cfg;
   
   @Override
   public Object loadUserBySAML (SAMLCredential credential) throws UsernameNotFoundException
   {
      User u = new User ();
      u.setUUID(credential.getAttributeAsString(cfg.getSAMLUserId()));

      List<Role> roles = new ArrayList<Role> ();
      List<String> extendedRoles = new ArrayList<String> ();
      
      for (Attribute attr : credential.getAttributes ())
      {
         if(!"Role".equalsIgnoreCase(attr.getName()))
         {
            continue;
         }
         String role = ((XSStringImpl)(attr.getAttributeValues ().get (0))).getValue ();
         try 
         {
            Role r = Role.valueOf (role);
            roles.add (r);
         }
         catch (IllegalArgumentException e)
         {
            extendedRoles.add(role.toUpperCase());
         }
      }
      
      u.setRoles (roles);
      u.setExtendedRoles(extendedRoles);
      return u;
   }
}
